[![Tests](https://github.com/tarantool/tarantool-java-sdk/actions/workflows/tests.yml/badge.svg)](https://github.com/tarantool/tarantool-java-sdk/actions/workflows/tests.yml)

# Tarantool Java SDK

A set of libraries for interacting with Tarantool from Java.
For working with Tarantool, the [Netty](https://netty.io) framework is used for asynchronous programming and the
[MessagePack](https://github.com/msgpack/msgpack-java) library for serialization and
deserialization of data.

## Getting Started

You need to download and install the library according to the [instructions](INSTALL.md).

### Using the Library

**Maven:**
```xml
<dependency>
    <groupId>io.tarantool</groupId>
    <artifactId>tarantool-client</artifactId>
    <version>1.7.0</version>
</dependency>
```

**Gradle:**
```kotlin
dependencies {
    implementation("io.tarantool:tarantool-client:1.7.0")
}
```

## Documentation

- [Release](RELEASING.md)
- [Documentation deploy](DOC_DEPLOY.md)
