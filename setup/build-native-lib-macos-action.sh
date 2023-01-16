#!/bin/sh -li
set -e
# shell w/ login & interactive, plus exit if any command fails


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

cd ./target/tokyocabinet
./configure
make -j4

# we want to force linking libjtokyocabinet against the static lib vs. the dynamic since some LD loading will look
# for the soname and versioned library, but we can only auto extract a single .so at a time
rm ./*.dylib

export TCDIR="$PWD"
# configure can't find tokyocabinet OR java/include/darwin correctly
# since this is clang, we need to setup the lib and include paths
export LIBRARY_PATH="$LIBRARY_PATH:$TCDIR"
export CPATH="$TCDIR:$JAVA_HOME/include:$JAVA_HOME/include/darwin"

cd ../tokyocabinet-java
./configure

# the Makefile needs tweaking to successfully compile
#sed -i -e "s#/include/mac#/include/darwin#" Makefile
# we need to target a MUCH higher version of java too
sed -i -e "s/-source 1.4/-source 1.8/" Makefile

make -j4

TARGET_LIB=libjtokyocabinet.dylib
OUTPUT_DIR="../../tokyocabinet-${BUILDOS}-${BUILDARCH}/src/main/resources/jne/${BUILDOS}/${BUILDARCH}"
strip -u -r ./$TARGET_LIB
cp ./$TARGET_LIB "$OUTPUT_DIR"

echo "Copied ./$TARGET_LIB to $OUTPUT_DIR"
echo "Done!"