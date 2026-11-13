---
title: Balancing
---

## Balancer

The balancer is a client component that distributes requests over the connections to Tarantool
nodes. In `Tarantool Java SDK` it is represented by the `TarantoolBalancer` interface.

### How a request is executed

<figure markdown="span" id="balancer">
<figcaption>How the balancer works</figcaption>
![](../../../assets/drawio/client/balancer.drawio)
</figure>

1. The application sends a request through the API of a high-level client (`TarantoolClient`,
   `TarantoolCrudClient` or `TarantoolBoxClient`).
2. The client asks `TarantoolBalancer` for a connection for this request.
3. The balancer picks a connection in the [connection pool](connection-pool.md)
   (`IProtoClientPool`) according to its algorithm.
4. The pool returns an available connection to the balancer.
5. The balancer passes the connection to the client.
6. The client sends the request over this connection.
7. The client returns a `CompletableFuture` with the request result to the calling code.

### Available and locked connections

A connection is opened when the balancer picks it for the first time. If the picked connection is
unavailable, the balancer moves on to the next one. If there are no available connections at all,
the request's `CompletableFuture` completes with `NoAvailableClientsException`.

A connection becomes unavailable (locked) in three cases:

- the connect attempt failed;
- the node closed the connection;
- [heartbeat](heartbeat.md) moved the connection to the `INVALIDATE` state.

The lock is released when the connection connects successfully again or heartbeat moves it to the
`ACTIVATE` state. Without heartbeat configured, only the first two cases remain.

According to the SDK code, long requests do not lock a connection: many requests run over one
connection at the same time.

## Implementations

The client has two `TarantoolBalancer` implementations.

### TarantoolRoundRobinBalancer

<figure markdown="span" id="round-robin-balancer">
<figcaption>How TarantoolRoundRobinBalancer works</figcaption>
![](../../../assets/drawio/client/round-robin.drawio)
</figure>

Walks over connections group by group:

1. Picks the connection group to start from (the order of groups is not defined).
2. Picks the connections of this group in order, from the first to the last.
3. After the last connection of the group, moves on to the next group.

### TarantoolDistributingRoundRobinBalancer

<figure markdown="span" id="distributing-round-robin-balancer">
<figcaption>How TarantoolDistributingRoundRobinBalancer works</figcaption>
![](../../../assets/drawio/client/distributing-round-robin.drawio)
</figure>

Alternates groups, so consecutive requests go to different groups:

1. Picks the connection group to start from (the order of groups is not defined).
2. Picks the first connection of this group.
3. Moves on to the next group and picks its first connection.
4. After visiting all groups, repeats step 3, picking the next connection in each group.

## Choosing a balancer

The balancer class is set in the client builder:

```java
import io.tarantool.balancer.TarantoolRoundRobinBalancer;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;

public class BalancerConfig {

  public static TarantoolCrudClient createClient() throws Exception {
    return TarantoolFactory.crud()
        .withBalancerClass(TarantoolRoundRobinBalancer.class)
        // ... other settings
        .build();
  }
}
```

In Spring Data the balancer is chosen with the `spring.data.tarantool.balancer-mode` property (see
[Client Configuration](../../spring-data/configuration.md)).

## Custom balancer

To use your own algorithm, create a class that implements the `TarantoolBalancer` interface and
pass it to `withBalancerClass(...)`. The client creates the balancer through a public constructor
with the signature `public <ClassName>(IProtoClientPool pool)`, so the class must have one.
