---
title: Interaction with Tarantool Java SDK
---

## High-Level Architecture of Interaction with Tarantool Java SDK

<figure id="high-level-arch">
  <figcaption>High-Level Architecture of Tarantool Java SDK</figcaption>
  <img src="../../../../../assets/drawio/client/high-level-arch.drawio" alt="">
</figure>

To interact with `Tarantool`, the `Tarantool Java SDK` uses the `IProto` protocol, which
is implemented in classes supporting the `IProtoClient` interface. Connection management and their
state are handled through `PoolEntry` objects, which wrap `IProtoClient`. The mechanism [heartbeat](./heartbeat.md) is used
to check connection availability.

To work with multiple connections simultaneously, `PoolEntry` objects are grouped into
logical groups within [IProtoClientPool](./connection-pool.md). These groups represent
sets of connections to a single `Tarantool` node and are configured using metadata passed
through instances of the [InstanceConnectionGroup](./instance-connection-group.md) class.

The connection pool performs the following functions:

* Creates logically separated sets of `PoolEntry` when creating a client, using metadata from
  objects of the [InstanceConnectionGroup](./instance-connection-group.md) class
* Provides [available](./heartbeat.md) connections upon request from the [balancer](./balancer.md)
* Closes connections when the client shuts down

The selection of a connection for executing a request is performed using an object of a class implementing
the [TarantoolBalancer](./balancer.md) interface.

For working with logically separated groups of connections, high-level clients are used (
`TarantoolClient`, `TarantoolCrudClient` and `TarantoolBoxClient`), which provide a convenient
API.

The high-level scheme of interaction with `Tarantool` through `Tarantool Java SDK`
looks <a href="#high-level-arch">as follows</a>:

1. The programmer executes a request through the API of high-level clients (`TarantoolClient`,
   `TarantoolCrudClient` or `TarantoolBoxClient`)
2. The high-level client requests a connection to execute the request from `TarantoolBalancer`
3. The balancer, using its logic, requests an available node from the connection pool (`IProtoClientPool`)
   Available nodes are determined using the [heartbeat](./heartbeat.md) mechanism
4. The connection pool returns an available connection to the balancer
5. The balancer provides the high-level client with a connection
6. The high-level client executes the request through the provided connection
7. The high-level client returns a `CompletableFuture<?>` with the result to the calling code
