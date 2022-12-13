rm -Rf tokyocabinet-java-*
wget http://fallabs.com/tokyocabinet/javapkg/tokyocabinet-java-1.24.tar.gz
tar zxvf tokyocabinet-java-1.24.tar.gz
# remove the loader since we have our own special one
rm -f tokyocabinet-java-1.24/Loader.java
# copy everything over
cp tokyocabinet-java-1.24/*.java tokyocabinet-api/src/main/java/tokyocabinet/
# remove all tests
rm -Rf tokyocabinet-api/src/main/java/tokyocabinet/*Test.java
rm -Rf tokyocabinet-java-*
