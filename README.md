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
    <version>1.7.3</version>
</dependency>
```

**Gradle:**
```kotlin
dependencies {
    implementation("io.tarantool:tarantool-client:1.7.3")
}
```

### Quickstart Archetypes

Instead of configuring a project manually, you can generate a ready-to-use
skeleton with Maven Archetypes:

**BOX API quickstart:**
```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.tarantool \
    -DarchetypeArtifactId=tarantool-box-quickstart \
    -DarchetypeVersion=2.0.0
```

**CRUD API quickstart:**
```bash
mvn archetype:generate \
    -DarchetypeGroupId=io.tarantool \
    -DarchetypeArtifactId=tarantool-crud-quickstart \
    -DarchetypeVersion=2.0.0
```

> On Windows PowerShell, quote the version: DarchetypeVersion="2.0.0"

The BOX archetype generates a minimal example of client initialization, idempotent space creation, and basic `insert`/`select` operations.

The CRUD archetype generates a Spring Boot project with a `CrudRepository`, a `@Tuple`-mapped entity, and a `TarantoolCartridgeContainer`-based integration test. Note that the CRUD layer requires a Cartridge cluster with the `crud-router` and `crud-storage` roles enabled — a standalone Tarantool instance is not sufficient.

> **Note:** Until the first release, build and install the archetypes locally with `mvn clean install -pl tarantool-archetypes -am` and pass `-DarchetypeCatalog=local` to `mvn archetype:generate`.

## Documentation

- [Release](RELEASING.md)
- [Documentation deploy](DOC_DEPLOY.md)
