#!/bin/sh -l
# Use a shell as though we logged in
# Exit when any command fails
set -e

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.."
PROJECT_DIR=$PWD

BUILDOS=$1
BUILDARCH=$2

if [ -z "${BUILDOS}" ] || [ -z "${BUILDOS}" ]; then
  echo "Usage: script [os] [arch]"
  exit 1
fi

mkdir -p target
rsync -avrt --delete ./native/ ./target/

if [ $BUILDOS = "linux" ]; then
  if [ $BUILDARCH = "x64" ]; then
    BUILDTARGET=x86_64-linux-gnu
  elif [ $BUILDARCH = "arm64" ]; then
    BUILDTARGET=aarch64-linux-gnu
  elif [ $BUILDARCH = "armhf" ]; then
    # its odd how raspbian/rpios both call their architecture "armhf" when its really not
    BUILDTARGET=arm-linux-gnueabihf
  elif [ $BUILDARCH = "armel" ]; then
    BUILDTARGET=arm-linux-gnueabi
  elif [ $BUILDARCH = "riscv64" ]; then
    BUILDTARGET=riscv64-linux-gnu
  fi
elif [ $BUILDOS = "linux_musl" ]; then
  # https://stackoverflow.com/questions/39936341/how-do-i-use-a-sysroot-with-autoconf
  if [ $BUILDARCH = "x64" ]; then
    BUILDTARGET=x86_64-linux-musl
  elif [ $BUILDARCH = "arm64" ]; then
    BUILDTARGET=aarch64-linux-musl
  fi
  export SYSROOT="/opt/${BUILDTARGET}-cross/${BUILDTARGET}"
  export CFLAGS="--sysroot=${SYSROOT} $CFLAGS"
  export CXXFLAGS="--sysroot=${SYSROOT} $CFLAGS"
  export LDFLAGS="--sysroot=${SYSROOT} -L${SYSROOT}/usr/lib $CFLAGS"
fi

if [ -z "$BUILDTARGET" ]; then
  echo "Unsupported os-arch: $BUILDOS-$BUILDARCH"
  exit 1
fi

# bzip2 dependency
cd target
curl -fsSL "https://sourceware.org/pub/bzip2/bzip2-1.0.8.tar.gz" | tar zxvf -
cd bzip2-1.0.8
sed -i 's/CC=gcc/#CC=gcc/g' Makefile-libbz2_so
CC=$BUILDTARGET-gcc make -f Makefile-libbz2_so
cp -av bzlib.h $SYSROOT/include/
cp -av libbz2.so* $SYSROOT/lib/
cd ../../

# zlib  dependency
cd target
curl -fsSL "https://zlib.net/zlib-1.2.13.tar.gz" | tar zxvf -
cd zlib-1.2.13
CC=$BUILDTARGET-gcc ./configure --prefix=$SYSROOT
make
make install
cd ../../

export CFLAGS="$CFLAGS -Wa,--noexecstack"

cd ./target/tokyocabinet
./configure --host $BUILDTARGET

# only make the static lib (we don't need anything else)
make -j4 libtokyocabinet.a

# these flags will only help the ./configure succeed for tokyocabinet-java
export TCDIR="$PWD"
export CFLAGS="$CFLAGS -I$TCDIR"
export LDFLAGS="$LDFLAGS -L$TCDIR"

cd ../tokyocabinet-java
./configure --host $BUILDTARGET

# the Makefile needs tweaking to successfully compile
# https://www.baeldung.com/linux/sed-substitution-variables
sed -i -r "s#^CPPFLAGS(.*)#CPPFLAGS\1 -I$TCDIR#" Makefile
sed -i -r "s#^LDFLAGS(.*)#LDFLAGS\1 -L$TCDIR#" Makefile
# we need to target a MUCH higher version of java too
sed -i -e "s/-source 1.4/-source 1.8/" Makefile

make -j4

TARGET_LIB=libjtokyocabinet.so
OUTPUT_DIR="../../tokyocabinet-${BUILDOS}-${BUILDARCH}/src/main/resources/jne/${BUILDOS}/${BUILDARCH}"

$BUILDTARGET-strip ./$TARGET_LIB
cp ./$TARGET_LIB "$OUTPUT_DIR"

echo "Copied ./$TARGET_LIB to $OUTPUT_DIR"
echo "Done!"