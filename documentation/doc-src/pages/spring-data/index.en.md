---
title: Spring Data
hide:
  - toc
---

**Spring Data Tarantool** is a [Spring Data](https://spring.io/projects/spring-data) module that
simplifies creating applications based on [Spring :simple-spring:](https://spring.io),
using [Tarantool](https://www.tarantool.io) as a data store.

## Features

- Implementation of CRUD methods for data model classes specific to working with the Tarantool cluster
  through the [tarantool/crud](https://github.com/tarantool/crud) library
- Working with stored procedures and Lua scripts through dynamic methods
- Ability to integrate custom repository code
- Flexible access configuration to Tarantool
    - via spring-bean
    - via configuration files (`.properties`/`.yaml`)
- Convenient integration with Spring infrastructure
- Automatic implementation of the `Repository` interface using `@EnableTarantoolRepositories`, including
  support for custom queries

## Project Status

| tarantool-java-sdk Version | tarantool-spring-data Version |                     Spring Boot Version                     |
|:--------------------------:|:-----------------------------:|:-----------------------------------------------------------:|
|           1.5.x            |             1.5.x             |         2.7.18 / 3.1.12 / 3.2.12 / 3.3.13 / 3.4.11          |
|           1.6.x            |             1.6.x             |     2.7.18 / 3.1.12 / 3.2.12 / 3.3.13 / 3.4.13 / 3.5.8      |
|           1.7.x            |             1.7.x             | 2.7.18 / 3.1.12 / 3.2.12 / 3.3.13 / 3.4.13 / 3.5.13 / 4.0.6 |

Choose the module by the Spring Boot minor line of your application. The table above lists the
Spring Boot versions each module is built and tested with; other patch versions of the same line are
not tested in CI, and newer minor lines are not tested at all.

| Spring Boot line | Module                      |
|:----------------:|:---------------------------:|
|      2.7.x       | `tarantool-spring-data-27`  |
|      3.1.x       | `tarantool-spring-data-31`  |
|      3.2.x       | `tarantool-spring-data-32`  |
|      3.3.x       | `tarantool-spring-data-33`  |
|      3.4.x       | `tarantool-spring-data-34`  |
|      3.5.x       | `tarantool-spring-data-35`  |
|      4.0.x       | `tarantool-spring-data-40`  |

### Tarantool Version and Supported Client Modules

| Tarantool Version | CRUD API (cluster) | BOX API (single instance) |
|:-----------------:|:------------------:|:-------------------------:|
|      2.11.x       |        Yes         |      Yes (limited*)       |
|        3.x        |        Yes         |      Yes (limited*)       |

???+ note "Note"

    Only the configuration and retrieval of the spring bean `TarantoolBoxClient` is supported.

| Tarantool Data Grid Version | Repository API |
|:---------------------------:|:--------------:|
|             1.x             | Yes (limited*) |
|             2.x             | Yes (limited*) |

???+ note "Note"

    Release 1.4.0 introduced an experimental version of the client for Tarantool Data Grid,
    with support for CRUD operations.

## Prerequisites Before Working with the Spring Data Tarantool Module

### Having a Tarantool Database

To work with the module, access to a running Tarantool is required. Instructions for installing and running
Tarantool can be found
here: [`github`](https://github.com/tarantool/tarantool?tab=readme-ov-file#tarantool).

???+ warning "Cluster with `crud` and `vshard` is required for `Repository`"

    The repository layer (`@EnableTarantoolRepositories`) is bound to `TarantoolCrudClient` and calls
    the `crud.*` stored procedures, which are provided by the [`crud`](https://github.com/tarantool/crud)
    module on top of `vshard`. A plain `tarantool/tarantool` image from Docker Hub has neither of them,
    so `Repository` calls fail with `Procedure 'crud.select' is not defined`.
    Run a cluster (a router with `crud` and storages with `vshard`), using one of the following ways:

    - an image of Tarantool DB (TDB), which already includes `crud` and `vshard`;
    - the [tarantool-java-sdk/testcontainers](../testcontainers/tdb/index.md) module, which starts
      a TDB cluster from a test;
    - your own image: an example based on `tarantool/tarantool` with the `crud` rock and the
      `router.lua`/`storage.lua` scripts is in the repository, directory
      `tarantool-shared-resources/vshard_cluster`.

???+ warning "Important"

    - This module is designed to work with Tarantool versions `2.11.x` and `3.x`. Differences between versions 
      can be found in the [`official documentation`](https://www.tarantool.io/en/doc/latest/).
    - Working with lower versions of Tarantool is possible but not guaranteed

???+ note "Note"

    For familiarization or reviewing examples, it's convenient to use
    [`docker`](https://www.tarantool.io/en/download/os-installation/docker-hub/) and/or
    [`testcontainers`](https://testcontainers.com) as a runtime environment for Tarantool.

???+ note "Note"

    It's also worth reviewing the 
    [tarantool-java-sdk/testcontainers](../testcontainers/index.md) module, which will reduce 
    the time spent using `Tarantool` with `testcontainers` in Java.

### Loading the Library on the Development Machine

Include the module in your project as follows:

```xml

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>4.0.6</version>
  </dependency>
  <dependency>
    <groupId>io.tarantool</groupId>
    <artifactId>tarantool-spring-data-40</artifactId>
    <version>${tarantool-spring-data.version}</version>
  </dependency>
</dependencies>
```
