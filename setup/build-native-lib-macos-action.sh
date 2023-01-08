#!/bin/sh -l
# Use a shell as though we logged in

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.." || exit 1
PROJECT_DIR=$PWD

BUILDOS=$1
BUILDARCH=$2

if [ -z "${BUILDOS}" ] || [ -z "${BUILDOS}" ]; then
  echo "Usage: script [os] [arch]"
  exit 1
fi

mkdir -p target || exit 1
rsync -avrt --delete ./native/ ./target/ || exit 1

cd ./target/tokyocabinet
./configure || exit 1
make -j4 || exit 1

# we want to force linking libjtokyocabinet against the static lib vs. the dynamic since some LD loading will look
# for the soname and versioned library, but we can only auto extract a single .so at a time
rm ./*.dylib

export TCDIR="$PWD"
# configure can't find tokyocabinet OR java/include/darwin correctly
# since this is clang, we need to setup the lib and include paths
export LIBRARY_PATH="$LIBRARY_PATH:$TCDIR"
export CPATH="$TCDIR:$JAVA_HOME/include:$JAVA_HOME/include/darwin"

cd ../tokyocabinet-java
./configure || exit 1

# the Makefile needs tweaking to successfully compile
#sed -i -e "s#/include/mac#/include/darwin#" Makefile
# we need to target a MUCH higher version of java too
sed -i -e "s/-source 1.4/-source 1.8/" Makefile

make -j4 || exit 1

TARGET_LIB=libjtokyocabinet.dylib
OUTPUT_DIR="../../tokyocabinet-${BUILDOS}-${BUILDARCH}/src/main/resources/jne/${BUILDOS}/${BUILDARCH}"
strip -u -r ./$TARGET_LIB || exit 1
cp ./$TARGET_LIB "$OUTPUT_DIR" || exit 1

echo "Copied ./$TARGET_LIB to $OUTPUT_DIR"
echo "Done!"