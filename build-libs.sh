mkdir -p ./target/libs
docker run -it -v $PWD/target:/target tokyocabinet-buildbox /root/build-tokyocabinet.sh
cp ./target/libs/* tokyocabinet-linux-x64/src/main/resources/jne/linux/x64/