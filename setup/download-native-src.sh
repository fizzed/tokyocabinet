#!/bin/bash

BASEDIR=$(dirname "$0")
PROJECT_DIR=$BASEDIR/..
source $BASEDIR/versions

echo "Downloading..."
echo " project dir: $PROJECT_DIR"
echo " tokyocabinet: $TC_VERSION"
echo " tokyocabinet-java: $TC_JAVA_VERSION"

mkdir -p ${PROJECT_DIR}/native
rm -Rf ${PROJECT_DIR}/native/tokyocabinet
rm -Rf ${PROJECT_DIR}/native/tokyocabinet-java

curl -O http://fallabs.com/tokyocabinet/tokyocabinet-${TC_VERSION}.tar.gz
tar zxvf tokyocabinet-${TC_VERSION}.tar.gz
mv tokyocabinet-${TC_VERSION} ${PROJECT_DIR}/native/tokyocabinet
rm -f tokyocabinet-${TC_VERSION}.tar.gz

curl -O http://fallabs.com/tokyocabinet/javapkg/tokyocabinet-java-${TC_JAVA_VERSION}.tar.gz
tar zxvf tokyocabinet-java-${TC_JAVA_VERSION}.tar.gz
mv tokyocabinet-java-${TC_JAVA_VERSION} ${PROJECT_DIR}/native/tokyocabinet-java
rm -f tokyocabinet-java-${TC_JAVA_VERSION}.tar.gz

# now, we'll extract out the java code to our own maven source layout, along with tweaking it a bit for our customer loader
cp ${PROJECT_DIR}/native/tokyocabinet-java/*.java ${PROJECT_DIR}/tokyocabinet-api/src/main/java/tokyocabinet/
# remove all tests
rm -f ${PROJECT_DIR}/tokyocabinet-api/src/main/java/tokyocabinet/*Test.java
# replace all lines of code that loads the library with System.loadLibrary with our custom loader
sed -i 's/System.loadLibrary("jtokyocabinet");/CustomLoader.loadLibrary();/g' ${PROJECT_DIR}/tokyocabinet-api/src/main/java/tokyocabinet/*.java