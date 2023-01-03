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

```xml
<dependency>
  <groupId>com.fizzed</groupId>
  <artifactId>tokyocabinet-linux-x64</artifactId>
  <version>VERSION-HERE</version>
</dependency>
```

## Native Libs

Zip and Bzip2 libraries must be installed for this version to run.

     sudo apt install zlib1g libbz2         # e.g. on ubuntu/debian
     sudo apk add zlib bzip2                # e.g. on alpine

On linux, please install zlib1g libbz2 packages installed

| OS Arch          | Artifact                      | Info                              |
|------------------|-------------------------------|-----------------------------------|
| Linux x64        | tokyocabinet-linux-x64        | built on ubuntu 16.04, glibc 2.23 |
| Linux arm64      | tokyocabinet-linux-arm64      | built on ubuntu 16.04, glibc 2.23 |
| Linux MUSL x64   | tokyocabinet-linux_musl-x64   | built on alpine 3.11              |
| Linux MUSL arm64 | tokyocabinet-linux_musl-arm64 | built on alpine 3.11              |
| Linux riscv64    | tokyocabinet-linux-riscv64    | built on ubuntu 20.04, glibc 2.31 |
| MacOS x64        | tokyocabinet-macos-x64        | built on macos 10.13 high sierra  |
| MacOS arm64      | tokyocabinet-macos-arm64      | built on macos 12 monterey        |

## Development

We use a simple, yet quite sophisticated build system for fast, local builds across operating system and architectures.

For linux targets, we leverage docker containers either running locally on an x86_64 host, or remotely on dedicated
build machines running on arm64, macos x64, and macos arm64.

To build containers, you'll want to edit setup/blaze.java and comment out/edit which platforms you'd like to build for,
or potentially change them running on a remote machine via SSH.  Once you're happy with what you want to build for:

     java -jar setup/blaze.jar setup/blaze.java build_containers

     java -jar setup/blaze.jar setup/blaze.java build_native_libs

For information on registering your x86_64 host to run other architectures (e.g. riscv64 or aarch64), please see
the readme for https://github.com/fizzed/jne#development