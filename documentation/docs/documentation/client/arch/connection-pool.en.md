---
title: Connection Pool
---

## Connection Pool (IProtoClientPool)

In the `Tarantool Java SDK`, the connection pool represents a set of connections to Tarantool nodes,
grouped into logical groups. These groups are created using instances of the `InstanceConnectionGroup` class,
which contains the necessary metadata. More details about [InstanceConnectionGroup](./instance-connection-group.md).

When creating `TarantoolCrudClient` or `TarantoolBoxClient`, one instance of
`IprotoClientPool` is created. This pool, based on the `InstanceConnectionGroup` passed to it, creates within
itself instances of `PoolEntry`, which manage the lifecycle of their connection (connection/disconnection process,
[heartbeat](./heartbeat.md)). Each `PoolEntry` is associated only with one logical group. All `PoolEntry` in one logical group
contain their own connections to the same node.

<figure id="connection-pool-1">
  <figcaption>Location of <b>IProtoClientPool</b> in the Java client architecture</figcaption>
  <img src="../../../../../assets/drawio/client/connection-pool.drawio" alt="">
</figure>

`IProtoClientPool` should be considered as a container designed to store a set
of connections. When executing requests, `IProtoClientPool` acts as an object that provides
active connections to the balancer (no more than one). More detailed information about connection selection during
requests can be found in the [balancers](./balancer.md) section.

???+ warning "Important"

    The actual establishment of the connection in `IProtoClientPool` occurs when the balancer first
    selects this connection from the pool.
