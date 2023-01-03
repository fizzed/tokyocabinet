#!/bin/sh

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.." || exit 1
PROJECT_DIR=$PWD

mkdir -p target || exit 1
rsync -avrt --delete ./native/ ./target/ || exit 1
mkdir -p target/output

cd ./target/tokyocabinet
./configure || exit 1
make -j4 || exit 1

# we need to get rid of symlinked version
# this helps prevent java load library from looking for the specific version
#cp ./libtokyocabinet.dylib ./temp.dylib
#rm -Rf ./libtokyocabinet*dylib
#mv ./temp.dylib ./libtokyocabinet.dylib
#cp ./libtokyocabinet.dylib "$PROJECT_DIR/target/output/"
# we must force the use of the .a lib to static link
rm ./*.dylib

export TCDIR="$PWD"
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-11.jdk/Contents/Home
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

cp ./libjtokyocabinet.dylib "$PROJECT_DIR/target/output/"

#strip -u -r "$PROJECT_DIR/target/output/libtokyocabinet.dylib" || exit 1
strip -u -r "$PROJECT_DIR/target/output/libjtokyocabinet.dylib" || exit 1
chmod -R 777 "$PROJECT_DIR/target" || exit 1