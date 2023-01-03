#!/bin/sh

BASEDIR=$(dirname "$0")
cd "$BASEDIR/.." || exit 1
PROJECT_DIR=$PWD

mkdir -p target
rsync -avrt --delete ./native/ ./target/ || exit 1
mkdir -p target/output

cd ./target/tokyocabinet
./configure || exit 1

# get rid of soname with a version in it
sed -i -e 's/-Wl,-soname,libtokyocabinet.so.$(LIBVER)//' Makefile
#sed -i -e 's/-Wl,-soname,libtokyocabinet.so.$(LIBVER)/-soname,libtokyocabinet.so/' Makefile

make -j4 || exit 1

# we need to get rid of symlinked version
# this helps prevent java load library from looking for the specific version
#cp ./libtokyocabinet.so ./temp.so
#rm -Rf ./libtokyocabinet*so*
#mv ./temp.so ./libtokyocabinet.so
#cp ./libtokyocabinet.so "$PROJECT_DIR/target/output/"
# we must force the use of the .a lib to static link
rm ./*.so

# these flags will only help the ./configure succeed for tokyocabinet-java
export TCDIR="$PWD"
export CFLAGS="$CFLAGS -I$TCDIR"
export LDFLAGS="$LDFLAGS -L$TCDIR"

cd ../tokyocabinet-java
./configure || exit 1

# the Makefile needs tweaking to successfully compile
# https://www.baeldung.com/linux/sed-substitution-variables
sed -i -r "s#^CPPFLAGS(.*)#CPPFLAGS\1 -I$TCDIR#" Makefile
sed -i -r "s#^LDFLAGS(.*)#LDFLAGS\1 -L$TCDIR#" Makefile
# we need to target a MUCH higher version of java too
sed -i -e "s/-source 1.4/-source 1.8/" Makefile

make -j4 || exit 1

cp ./libjtokyocabinet.so "$PROJECT_DIR/target/output/"

#strip "$PROJECT_DIR/target/output/libtokyocabinet.so" || exit 1
strip "$PROJECT_DIR/target/output/libjtokyocabinet.so" || exit 1
chmod -R 777 "$PROJECT_DIR/target" || exit 1