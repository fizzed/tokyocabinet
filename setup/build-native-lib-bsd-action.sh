#!/bin/sh
set -e

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.."
PROJECT_DIR=$PWD

BUILDOS=$1
BUILDARCH=$2

mkdir -p target
rsync -avrt --delete ./native/ ./target/

export CFLAGS="$CFLAGS -Wa,--noexecstack"

cd ./target/tokyocabinet
./configure

# only make the static lib (we don't need anything else)
gmake -j4 libtokyocabinet.a

# these flags will only help the ./configure succeed for tokyocabinet-java
export TCDIR="$PWD"
export CFLAGS="$CFLAGS -I$TCDIR"
export LDFLAGS="$LDFLAGS -L$TCDIR"

cd ../tokyocabinet-java
./configure

# the Makefile needs tweaking to successfully compile
# https://www.baeldung.com/linux/sed-substitution-variables
# CPPFLAGS = -I. -I$(INCLUDEDIR) -L/home/builder/include -L/usr/local/include -DNDEBUG -D_GNU_SOURCE=1 -I/usr/local/openjdk17/include -I/usr/local/openjdk17/include/freebsd
sed -i -e "s#CPPFLAGS = -I.#CPPFLAGS = -I. -I$TCDIR#" Makefile
# LDFLAGS = -L. -L$(LIBDIR) -L/home/builder/lib -L/usr/local/lib
sed -i -e "s#LDFLAGS = -L.#LDFLAGS = -L. -L$TCDIR#" Makefile
# we need to target a MUCH higher version of java too
sed -i -e "s/-source 1.4/-source 1.8/" Makefile

#cat Makefile

gmake -j4 libjtokyocabinet.so

TARGET_LIB=libjtokyocabinet.so
OUTPUT_DIR="../../tokyocabinet-${BUILDOS}-${BUILDARCH}/src/main/resources/jne/${BUILDOS}/${BUILDARCH}"

strip ./$TARGET_LIB
cp ./$TARGET_LIB "$OUTPUT_DIR"
chmod +x "$OUTPUT_DIR/$TARGET_LIB"

echo "Copied ./$TARGET_LIB to $OUTPUT_DIR"
echo "Done!"