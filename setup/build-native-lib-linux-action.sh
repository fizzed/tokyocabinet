#!/bin/sh
set -e

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.."
PROJECT_DIR=$PWD

BUILDOS=$1
BUILDARCH=$2
BUILDTARGET=$3



mkdir -p target
rsync -avrt --delete ./native/ ./target/

cd target
tar zxvf zlib-1.3.tar.gz
cd zlib-1.3
./configure --prefix=$SYSROOT
make

export ZLIBDIR="$PWD"
export CPATH="$CPATH:$ZLIBDIR"
export CXXFLAGS="$CXXFLAGS -I$ZLIBDIR"
export LDFLAGS="$LDFLAGS -L$ZLIBDIR"
export PKG_CONFIG_PATH="$PKG_CONFIG_PATH:$ZLIBDIR"

#make install
cd ../../

# zlib  dependency
#cd target
#tar zxvf /opt/zlib-1.2.13.tar.gz
#cd zlib-1.2.13
#./configure --prefix=$SYSROOT
#make
#make install
#cd ../../

# bzip2 dependency
cd target
tar zxvf bzip2-1.0.8.tar.gz
cd bzip2-1.0.8
sed -i 's/CC=gcc/#CC=gcc/g' Makefile-libbz2_so
make -f Makefile-libbz2_so
#cp -av bzlib.h $SYSROOT/include/
#cp -av libbz2.so* $SYSROOT/lib/

export BZLIBDIR="$PWD"
export CPATH="$CPATH:$BZLIBDIR"
export CXXFLAGS="$CXXFLAGS -I$BZLIBDIR"
export LDFLAGS="$LDFLAGS -L$BZLIBDIR"
export PKG_CONFIG_PATH="$PKG_CONFIG_PATH:$BZLIBDIR"

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
export LD_LIBRARY_PATH="$ZLIBDIR:$BZLIBDIR"

cd ../tokyocabinet-java
./configure --host $BUILDTARGET

# the Makefile needs tweaking to successfully compile
# https://www.baeldung.com/linux/sed-substitution-variables
sed -i -r "s#^CPPFLAGS(.*)#CPPFLAGS\1 -I$TCDIR#" Makefile
sed -i -r "s#^LDFLAGS(.*)#LDFLAGS\1 -L$TCDIR -L$ZLIBDIR#" Makefile
# we need to target a MUCH higher version of java too
sed -i -e "s/-source 1.4/-source 1.8/" Makefile

make -j4 libjtokyocabinet.so

TARGET_LIB=libjtokyocabinet.so
OUTPUT_DIR="../../tokyocabinet-${BUILDOS}-${BUILDARCH}/src/main/resources/jne/${BUILDOS}/${BUILDARCH}"

$BUILDTARGET-strip ./$TARGET_LIB
cp ./$TARGET_LIB "$OUTPUT_DIR"
chmod +x "$OUTPUT_DIR/$TARGET_LIB"

echo "Copied ./$TARGET_LIB to $OUTPUT_DIR"
echo "Done!"