## Tarantool Testcontainers Module

This project contains the implementation of the `testcontainers` module for `Tarantool`.

Supported usage scenarios:

| Name                | Product Version                                            |
|---------------------|------------------------------------------------------------|
| Single Tarantool Node | <ul><li>[x] 3.x </li><li>[ ] 2.11.x</li></ul>              |
| TDB Cluster         | <ul><li>[x] 3.x </li><li>[x] 2.x</li><li>[ ] 1.x</li></ul> |
| TQE Cluster         | <ul><li>[x] 2.x </li><li>[ ] 3.x</li></ul>                 |

### Architecture

Information about the module architecture can be found:

* [Architecture of "Single Tarantool Node" solution](doc/001-single-node-testcontainers-arch.md)
* [Architecture of "TDB Cluster" solution](./doc/004-tdb-cluster-testcontainers-arch.md)
* [Architecture of "TQE Cluster" solution](doc/007-tqe-cluster-arch.md)

### Usage Examples

Usage examples of the module can be found:

* [Example of using testcontainers for a single Tarantool node](./doc/003-single-node-testcontainers-standard-impl-example.md)
* [Example of using testcontainers for a TDB cluster](./doc/006-tdb2-cluster-testcontainers-standart-impl-example.md)
* [Example of using testcontainers for a TQE cluster](doc/008-tqe-cluster-example.md)
