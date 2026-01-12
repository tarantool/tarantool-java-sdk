---
title: About the Project
hide:
  - toc
  - navigation 
---

# Tarantool Java SDK

**`Tarantool Java SDK`** is a set of libraries for interacting
with [Tarantool :simple-github:](https://github.com/tarantool/tarantool) from `Java`.  
To work with `Tarantool`, the [Netty](https://netty.io) framework is used for asynchronous network
interaction and the [MessagePack](https://github.com/msgpack/msgpack-java) library for serialization
and deserialization of data.

The libraries provide full support for the entire Tarantool protocol (IProto). They provide Box
API for working with individual Tarantool servers. Crud API[^1] for working with clusters (Tarantool
DB, Tarantool EE), as well as `Spring Data` interfaces for integration
with [Spring :simple-spring:](https://spring.io)

[^1]: CRUD API is a proxy to the [tarantool/crud](https://github.com/tarantool/crud) API

## Downloading Artifacts from Maven-central

To download artifacts from `maven-central`, you need to take the following steps:

- Install [maven :simple-apachemaven:](https://maven.apache.org)
- Create a `maven` project
- Add dependencies to the `pom.xml` file of the project:
    ```xml
    <dependency>
        <groupId>io.tarantool</groupId>
        <artifactId>tarantool-client</artifactId>
        <version>${tarantool-java-sdk.version}</version>
    </dependency>
    ```
  