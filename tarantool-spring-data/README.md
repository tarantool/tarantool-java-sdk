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

| tarantool-java-sdk Version | tarantool-spring-data Version |                Spring Boot Version                |
|:--------------------------:|:-----------------------------:|:-------------------------------------------------:|
|           1.0.0            |             1.0.0             |                      2.7.18                       |
|           1.1.x            |             1.1.x             |              2.7.18 / 3.1.10 / 3.2.4              |
|           1.2.x            |             1.2.x             |              2.7.18 / 3.1.10 / 3.2.4              |
|           1.3.x            |             1.3.x             |     2.7.18 / 3.1.10 / 3.2.4 / 3.3.11 / 3.4.5      |
|           1.4.x            |             1.4.x             |     2.7.18 / 3.1.10 / 3.2.4 / 3.3.13 / 3.4.10     |
|           1.5.x            |             1.5.x             | 2.7.18 / 3.1.10 / 3.2.4 / 3.3.13 / 3.4.13 / 3.5.8 |

### Tarantool Version and Supported Client Modules

| Tarantool Version | CRUD API (cluster) | BOX API (single instance) |
|:-----------------:|:------------------:|:-------------------------:|
|      2.11.x       |         Yes        |      Yes (limited*)       |
|       3.x         |         Yes        |      Yes (limited*)       |

*Only configuration and getting spring bean `TarantoolBoxClient` is supported.

| Tarantool Data Grid Version | Repository API |
|:---------------------------:|:--------------:|
|             1.x             |   Yes (limited*) |
|             2.x             |   Yes (limited*) |

*Experimental version of the client for Tarantool Data Grid added in release 1.4.0,
with CRUD operation support.

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
> #### Spring Data 3.4.x
> Verified operation of 
> [repository fragments SPI](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2024.1-Release-Notes#repository-fragments-spi).
> Additional information [here](https://github.com/spring-projects/spring-data-commons/pull/3093).

## Getting Started with Spring Data Tarantool

- [Prerequisites](doc/instructions/prepare.md)
- [Client Configuration](doc/instructions/configuration.md)
- [Working with Repository](./doc/instructions/tarantool_repository.md)
- [Working with Custom Query Methods](./doc/instructions/derived_methods.md)
- [Working with LUA and Stored Procedures](./doc/instructions/query_lua.md)
- [Pagination. Pageable, Page, Slice](./doc/instructions/page-slice-pageable.md)
- [Pagination. Scroll API (spring-data v3.1 and higher)](./doc/instructions/scroll-api.md)
- [Sort](./doc/instructions/sort.md)
