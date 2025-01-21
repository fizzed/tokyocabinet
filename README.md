# TokyoCabinet & Native Libs for Java by Fizzed

[![Maven Central](https://img.shields.io/maven-central/v/com.fizzed/tokyocabinet?color=blue&style=flat-square)](https://mvnrepository.com/artifact/com.fizzed/tokyocabinet)

The following Java versions and platforms are tested using GitHub workflows:

[![Java 8](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/java8.yaml?branch=master&label=Java%208&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/java8.yaml)
[![Java 11](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/java11.yaml?branch=master&label=Java%2011&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/java11.yaml)
[![Java 17](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/java17.yaml?branch=master&label=Java%2017&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/java17.yaml)
[![Java 21](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/java21.yaml?branch=master&label=Java%2021&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/java21.yaml)

[![Linux x64](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/java11.yaml?branch=master&label=Linux%20x64&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/java11.yaml)
[![MacOS arm64](https://img.shields.io/github/actions/workflow/status/fizzed/tokyocabinet/macos-arm64.yaml?branch=master&label=MacOS%20arm64&style=flat-square)](https://github.com/fizzed/tokyocabinet/actions/workflows/macos-arm64.yaml)

The following platforms are tested using the [Fizzed, Inc.](http://fizzed.com) build system:

[![Linux arm64](https://img.shields.io/badge/Linux%20arm64-passing-green)](buildx-results.txt)
[![Linux armhf](https://img.shields.io/badge/Linux%20armhf-passing-green)](buildx-results.txt)
[![Linux riscv64](https://img.shields.io/badge/Linux%20riscv64-passing-green)](buildx-results.txt)
[![Linux MUSL x64](https://img.shields.io/badge/Linux%20MUSL%20x64-passing-green)](buildx-results.txt)
[![MacOS x64](https://img.shields.io/badge/MacOS%20x64-passing-green)](buildx-results.txt)
[![FreeBSD x64](https://img.shields.io/badge/FreeBSD%20x64-passing-green)](buildx-results.txt)
[![OpenBSD x64](https://img.shields.io/badge/OpenBSD%20x64-passing-green)](buildx-results.txt)

## Overview

TokyoCabinet remains a workhorse key-value store that still represents excellent performance vs. modern popular embedded
key-value stores such as LevelDB or RocksDB.  This is a published version of the library for Java 8+, along with native libs that 
are automatically extracted at runtime.

The Java library is as unmodified as possible from the original TokyoCabinet, but a few changes were made to automatically
extract the library at runtime.

## Sponsorship & Support

![](https://cdn.fizzed.com/github/fizzed-logo-100.png)

Project by [Fizzed, Inc.](http://fizzed.com) (Follow on Twitter: [@fizzed_inc](http://twitter.com/fizzed_inc))

**Developing and maintaining opensource projects requires significant time.** If you find this project useful or need
commercial support, we'd love to chat. Drop us an email at [ping@fizzed.com](mailto:ping@fizzed.com)

Project sponsors may include the following benefits:

- Priority support (outside of Github)
- Feature development & roadmap
- Priority bug fixes
- Privately hosted continuous integration tests for their unique edge or use cases

## Usage

Add the following to your maven POM file for Linux x64

```xml
<dependency>
  <groupId>com.fizzed</groupId>
  <artifactId>tokyocabinet-linux-x64</artifactId>
  <version>0.0.16</version>
</dependency>
```

Or MacOS arm64 (Apple silicon)

```xml
<dependency>
  <groupId>com.fizzed</groupId>
  <artifactId>tokyocabinet-macos-arm64</artifactId>
  <version>0.0.16</version>
</dependency>
```

Or for all operating system & arches

```xml
<dependency>
  <groupId>com.fizzed</groupId>
  <artifactId>tokyocabinet-all-natives</artifactId>
  <version>0.0.16</version>
</dependency>
```

To simplify versions, you may optionally want to import our BOM (bill of materials)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fizzed</groupId>
            <artifactId>tokyocabinet-bom</artifactId>
            <version>0.0.16</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Native Libs

Zip and Bzip2 libraries must be installed for this version to run.

     sudo apt install zlib1g libbz2         # e.g. on ubuntu/debian
     sudo apk add zlib bzip2                # e.g. on alpine

| OS Arch          | Artifact                      | Info                              |
|------------------|-------------------------------|-----------------------------------|
| Linux x64        | tokyocabinet-linux-x64        | built on ubuntu 16.04, glibc 2.23 |
| Linux arm64      | tokyocabinet-linux-arm64      | built on ubuntu 16.04, glibc 2.23 |
| Linux armhf      | tokyocabinet-linux-armhf      | built on ubuntu 16.04, glibc 2.23 |
| Linux armel      | tokyocabinet-linux-armel      | built on ubuntu 16.04, glibc 2.23 |
| Linux MUSL x64   | tokyocabinet-linux_musl-x64   | built on alpine 3.11              |
| Linux MUSL arm64 | tokyocabinet-linux_musl-arm64 | built on alpine 3.11              |
| Linux riscv64    | tokyocabinet-linux-riscv64    | built on ubuntu 18.04, glibc 2.31 |
| MacOS x64        | tokyocabinet-macos-x64        | built on macos 10.13 high sierra  |
| MacOS arm64      | tokyocabinet-macos-arm64      | built on macos 12 monterey        |
| FreeBSD x64      | tkrzw-freebsd-x64             | targets freebsd 12+               |
| OpenBSD x64      | tkrzw-openbsd-x64             | targets openbsd 7.5+              |

## Development

We use a simple, yet quite sophisticated build system for fast, local builds across operating system and architectures.

For linux targets, we leverage docker containers either running locally on an x86_64 host, or remotely on dedicated
build machines running on arm64, macos x64, and macos arm64.

To build containers, you'll want to edit setup/blaze.java and comment out/edit which platforms you'd like to build for,
or potentially change them running on a remote machine via SSH.  Once you're happy with what you want to build for:

     java -jar cross_build_containers
     java -jar cross_build_natives
     java -jar cross_tests

For information on registering your x86_64 host to run other architectures (e.g. riscv64 or aarch64), please see
the readme for https://github.com/fizzed/buildx

## License

Copyright (C) 2020+ Fizzed, Inc.

This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
