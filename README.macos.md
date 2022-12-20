# Compiling on MacOS

Download Azul JDK 8

export JAVA_HOME=$(/usr/libexec/java_home)

when compiling tokyocabinet-java

CPATH=$JAVA_HOME/include/darwin ./configure
CPATH=$JAVA_HOME/include/darwin make
