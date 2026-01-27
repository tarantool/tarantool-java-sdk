---
title: Balancing
---

## Balancers

The `Balancer` is a `TarantoolClient` component designed to distribute requests
between Tarantool nodes.

### Concept

The balancer abstraction in `Tarantool Java EE` is represented by the `TarantoolBalancer` interface.
Let's look at how balancing works in the Java client:

<figure markdown="span" id="balancer">
<figcaption>How the balancer works</figcaption>
![](../../../assets/drawio/client/balancer.drawio)
</figure>  

1. The programmer executes a request through the API of high-level clients (`TarantoolClient`,
   `TarantoolCrudClient` or `TarantoolBoxClient`)
2. The high-level client requests a connection to execute the request from `TarantoolBalancer`
3. The balancer, using its logic, requests an available node from the connection pool (`IProtoClientPool`)
   Node availability is determined using the [heartbeat](heartbeat.md) mechanism
4. The connection pool returns an available connection to the balancer
5. The balancer provides the high-level client with a connection
6. The high-level client executes the request through the provided connection
7. The high-level client returns a `CompletableFuture<?>` with the result to the calling code

???+ warning "Important"

    The actual connection to the node via the selected connection occurs upon the first selection.

    If the connection is unavailable, the balancer will attempt to select the next one from the available. If there is 
    no available connection, the client will throw a `NoAvailableClientsException`.

### Available Default Implementations

By default, the following types of balancers are available:

#### TarantoolRoundRobinBalancer

<figure markdown="span" id="round-robin-balancer">
<figcaption>How the balancer works</figcaption>
![](../../../assets/drawio/client/round-robin.drawio)
</figure> 

This implementation of the `TarantoolBalancer` interface performs connection selection according to the following
algorithm:

1. Selection of the group of connections with which the balancing starts (the order is not deterministic).
2. Connection selection within the group is performed in order from first to last.
3. After the last connection in the current group, the balancer moves to connections belonging to
   another group.

#### TarantoolDistributingRoundRobinBalancer

<figure markdown="span" id="round-robin-balancer">
<figcaption>How the balancer works</figcaption>
![](../../../assets/drawio/client/distributing-round-robin.drawio)
</figure>  

This implementation of the `TarantoolBalancer` interface performs connection selection according to the following
algorithm:

1. Selection of the group of connections with which the balancing starts (the order is not deterministic).
2. Selection of the first connection in the first group.
3. Moving to the next group and selecting the first connection.
4. Repeat step 3, selecting the next connection in the group.

### Client Balancing Configuration

To configure balancing in the client, use the `TarantoolFactory` API, for example:

```java
final TarantoolCrudClient crudClient = TarantoolFactory.crud()
    .withBalancerClass(TarantoolRoundRobinBalancer.class)
    //... other settings
    .build();
```

### Custom Balancers

To use your own balancer, create a class that implements the
`TarantoolBalancer` interface.

???+ warning "Important"

    The custom class implementing `TarantoolBalancer` must have a default constructor 
    and a constructor with the signature `public <constructorName>(IProtoClientPool pool) {...}`.
