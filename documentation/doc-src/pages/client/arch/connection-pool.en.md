---
title: Connection Pool
---

## Connection Pool (IProtoClientPool)

In the `Tarantool Java SDK`, the connection pool represents a set of connections to Tarantool nodes,
grouped into logical groups. These groups are created using instances of the `InstanceConnectionGroup` class,
which contains the necessary metadata. More details about [InstanceConnectionGroup](instance-connection-group.md).

When creating `TarantoolCrudClient` or `TarantoolBoxClient`, one instance of
`IprotoClientPool` is created. This pool, based on the `InstanceConnectionGroup` passed to it, creates within
itself instances of `PoolEntry`, which manage the lifecycle of their connection (connection/disconnection process,
[heartbeat](heartbeat.md)). Each `PoolEntry` is associated only with one logical group. All `PoolEntry` in one logical group
contain their own connections to the same node.

<figure markdown="span" id="connection-pool-1">
<figcaption>Location of <b>IProtoClientPool</b> in the Java client architecture</figcaption>
![](../../../assets/drawio/client/connection-pool.drawio)
</figure>

`IProtoClientPool` should be considered as a container designed to store a set
of connections. When executing requests, `IProtoClientPool` acts as an object that provides
active connections to the balancer (no more than one). More detailed information about connection selection during
requests can be found in the [balancers](balancer.md) section.

???+ warning "Important"

    The actual establishment of the connection in `IProtoClientPool` occurs when the balancer first
    selects this connection from the pool.

### Locked connections

A `PoolEntry` is *locked* when its connection is considered unusable: a connection attempt failed,
or the [heartbeat](heartbeat.md) moved the connection to the invalidated state. A locked entry is not
returned to the balancer and is unlocked when the connection is restored (the reconnect succeeds or the
heartbeat reports recovery). A connection is **not** locked just because a long request is running on
it: many requests are in flight over one iproto connection at the same time.

If all entries are locked, or the balancer walked once over all connections of the pool and found no
unlocked one, the balancer does not wait or spin: the request fails with `NoAvailableClientsException`.
A server that is busy so long that it stops answering pings can lead to this, because the heartbeat
invalidates its connections.
