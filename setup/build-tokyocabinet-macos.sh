#!/bin/sh

wget http://fallabs.com/tokyocabinet/tokyocabinet-1.4.48.tar.gz
tar zxvf tokyocabinet-1.4.48.tar.gz
cd tokyocabinet-1.4.48
./configure --prefix=$PWD/target
make -j4
make install

CPPFLAGS=-I$PWD/target/include
LDFLAGS=-L$PWD/target/lib
wget http://fallabs.com/tokyocabinet/javapkg/tokyocabinet-java-1.24.tar.gz
tar zxvf tokyocabinet-java-1.24.tar.gz
cd tokyocabinet-java-1.24
./configure --prefix=$PWD/target
make -j4
make install

#mkdir -p /target/libs
#cp /usr/lib/libtokyocabinet.so /target/libs/
#cp /usr/lib/libjtokyocabinet.so /target/libs/
#chown 777 /target/libs/*
#ls -la /target/libs