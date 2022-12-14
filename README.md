Java distribution of TokyoCabinet & JNI libs by Fizzed
------------------------------------------------------

TokyoCabinet remains a workhorse key-value store that still represents excellent performance vs. modern popular embedded
key-value stores such as LevelDB or RocksDB.  This is a published version of the library for Java 8+, along with native libs that 
are automatically extracted at runtime.

The Java library is as unmodified as possible from the original TokyoCabinet, but a few changes were made to automatically
extract the library at runtime.

Linux x64 native libs are compiled on Ubuntu 16.04, so you can be assured they'll work well on various flavors of linux
going back several years in time.

```xml
<dependency>
  <groupId>com.fizzed</groupId>
  <artifactId>tokyocabinet-linux-x64</artifactId>
  <version>VERSION-HERE</version>
</dependency>
```