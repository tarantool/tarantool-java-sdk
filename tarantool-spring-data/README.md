# Spring Data Tarantool

**Spring Data Tarantool** is a [Spring Data](https://spring.io/projects/spring-data) module that
simplifies creating
applications based on [Spring](https://spring.io) using [Tarantool](https://www.tarantool.io) as a
data storage.

## Features

- Implementation of CRUD methods for data model classes specific to working with Tarantool clusters
  through the
  [tarantool/crud](https://github.com/tarantool/crud) library
- Working with stored procedures and LUA scripts via dynamic methods
- Ability to integrate custom repository code
- Flexible access configuration to Tarantool (configuration via Java objects and config files
  (.properties / .yaml))
- Convenient integration with Spring infrastructure
- Automatic implementation of the `Repository` interface using `@EnableTarantoolRepositories`, including
  support for custom queries

## Project Status

| tarantool-java-sdk Version | tarantool-spring-data Version |                     Spring Boot Version                     |
|:--------------------------:|:-----------------------------:|:-----------------------------------------------------------:|
|           1.0.0            |             1.0.0             |                           2.7.18                            |
|           1.1.x            |             1.1.x             |                   2.7.18 / 3.1.10 / 3.2.4                   |
|           1.2.x            |             1.2.x             |                   2.7.18 / 3.1.10 / 3.2.4                   |
|           1.3.x            |             1.3.x             |          2.7.18 / 3.1.10 / 3.2.4 / 3.3.11 / 3.4.5           |
|           1.4.x            |             1.4.x             |          2.7.18 / 3.1.10 / 3.2.4 / 3.3.13 / 3.4.10          |
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
|      2.11.x       |         Yes        |      Yes (limited*)       |
|       3.x         |         Yes        |      Yes (limited*)       |

*Only configuration and getting spring bean `TarantoolBoxClient` is supported.

| Tarantool Data Grid Version | TDG Repository API (`TarantoolDataGridClient`) | Spring Data `Repository` |
|:---------------------------:|:----------------------------------------------:|:------------------------:|
|             1.x             |                Yes (limited*)                  |            No            |
|             2.x             |                Yes (limited*)                  |            No            |

*Experimental version of the client for Tarantool Data Grid added in release 1.4.0. It supports the
CRUD operations of the
[TDG Repository API](https://www.tarantool.io/docs/tdg/ru/2_x/reference/sandbox-api/repository-api)
(`repository.get` / `put` / `find` / `count` / `update` / `delete`), which is not related to the
[tarantool/crud](https://github.com/tarantool/crud) module.

> [!TIP]
> - Learn more about the `crud` client:
    >
- Cluster API on the tarantool side
  ([`github.com/tarantool/crud`](https://github.com/tarantool/crud))
>   - Java proxy client to
      tarantool/crud ([`Java CRUD Client`](../tarantool-client/src/main/java/io/tarantool/client/crud)).
> - Learn more about the `box` client:
    >
- Internal box module for working in Tarantool lua
  runtime [`Submodule box.space`](https://www.tarantool.io/en/doc/2.11/reference/reference_lua/box_space/).
>   - Implementation of the box module for working with a Tarantool instance in
      Java [`Java BOX Client`](../tarantool-client/src/main/java/io/tarantool/client/box).  
      > Has a similar API to `Submodule box.space`, but is not it or a proxy to it.

> [!IMPORTANT]
> Working with BOX API (`Java BOX Client`) is currently performed **only** through the interface
> [`TarantoolBoxClient`](../tarantool-client/src/main/java/io/tarantool/client/box/TarantoolBoxClient.java)
> (you can add it as a bean and work in the Spring context). The ability to work
> through `Repository` and
> `@EnableTarantoolRepositories` is not implemented at this time.

> [!IMPORTANT]
> Working with Tarantool Data Grid is currently performed **only** through the interface
> [`TarantoolDataGridClient`](../tarantool-client/src/main/java/io/tarantool/client/tdg/TarantoolDataGridClient.java)
> (you can add it as a bean and work in the Spring context). The ability to work
> through `Repository` and `@EnableTarantoolRepositories` is not implemented at this time:
> the repository layer is bound to `TarantoolCrudClient` and calls `crud.*` stored procedures,
> which are not available in TDG. Using a `Repository` with a TDG connection fails with
> `BoxError{code=33, message='Procedure 'crud.select' is not defined'}`.

> [!IMPORTANT] 
> #### Spring Data 3.4.x
> Verified operation of 
> [repository fragments SPI](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2024.1-Release-Notes#repository-fragments-spi).
> Additional information [here](https://github.com/spring-projects/spring-data-commons/pull/3093).

## Getting Started with Spring Data Tarantool

- [Prerequisites](../documentation/doc-src/pages/spring-data/index.en.md#prerequisites-before-working-with-the-spring-data-tarantool-module)
- [Client Configuration](../documentation/doc-src/pages/spring-data/configuration.en.md)
- [Working with Repository](../documentation/doc-src/pages/spring-data/repository.en.md)
- [Working with Custom Query Methods](../documentation/doc-src/pages/spring-data/derived-methods.en.md)
- [Working with LUA and Stored Procedures](../documentation/doc-src/pages/spring-data/query-lua.en.md)
- [Pagination. Pageable, Page, Slice](../documentation/doc-src/pages/spring-data/page-slice-pageable.en.md)
- [Pagination. Scroll API (spring-data v3.1 and higher)](../documentation/doc-src/pages/spring-data/scroll-api.en.md)
- [Sort](../documentation/doc-src/pages/spring-data/sort.en.md)
