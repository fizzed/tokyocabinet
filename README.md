# TokyoCabinet & Native Libs for Java by Fizzed

[![Linux x64](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/linux-x64.yaml?branch=master&label=Linux%20x64&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/linux-x64.yaml)
[![MacOS x64](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/macos-x64.yaml?branch=master&label=MacOS%20x64&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/macos-x64.yaml)
![MacOS arm64](https://img.shields.io/badge/MacOS%20arm64-available-blue)
[![Maven Central](https://img.shields.io/maven-central/v/com.fizzed/tokyocabinet?color=blue&style=flat-square&)](https://mvnrepository.com/artifact/com.fizzed/tokyocabinet)

TokyoCabinet remains a workhorse key-value store that still represents excellent performance vs. modern popular embedded
key-value stores such as LevelDB or RocksDB.  This is a published version of the library for Java 8+, along with native libs that 
are automatically extracted at runtime.

The Java library is as unmodified as possible from the original TokyoCabinet, but a few changes were made to automatically
extract the library at runtime.

Native Libs:
 - Linux x64: compiled on Ubuntu 16.04 (for improved compatability with older linux and glibc)
 - MacOS x64: compiled on MacOS 10.13 (for improved compatability with older versions)

```xml
<dependency>
  <groupId>com.fizzed</groupId>
  <artifactId>tokyocabinet-linux-x64</artifactId>
  <version>VERSION-HERE</version>
</dependency>
```
