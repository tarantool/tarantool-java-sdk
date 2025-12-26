---
title: Connecting to Multiple Nodes
---

## Connecting to Multiple Tarantool Nodes

Before connecting the Java client to multiple Tarantool nodes, you need to determine the connection format.
There are two options available:

* `TarantoolCrudClient` - a high-level client for working with a Tarantool vshard cluster with
  the ability to connect to multiple routers. Uses method calls from the crud module to manipulate data. More about [crud](https://github.com/tarantool/crud) and
  [vshard](https://github.com/tarantool/vshard).
* `TarantoolBoxClient` - a high-level client for working with single Tarantool nodes with
  the ability to connect to a set of nodes regardless of topology. When using
  `crud/vshard` cluster, attempts to make changes on nodes with the `storage` role may lead to
  cluster malfunction.

???+ warning "Important"

    For a detailed description of crud and box APIs in Tarantool Java SDK, refer to the javadoc
    for `TarantoolBoxClient` and `TarantoolCrudClient`

To connect to Tarantool regardless of mode (cluster or single node), the following data is required:

* `Domain names` or `node addresses` (for example, `localhost`, `127.0.0.1`)
* `Ports` on which Tarantool listens for connections (for example, `3301`)
* `User names` on behalf of which the client interacts with Tarantool
  (for example, `admin`, `user`, etc.)
* `Passwords` corresponding to user names

### Connecting to Multiple Tarantool Nodes (Routers) via TarantoolCrudClient API

???+ warning "Important"

    In this connection format, interaction with the Tarantool cluster is performed 
    **EXCLUSIVELY** through router nodes

Let's consider an example with the following cluster topology:

<figure id="vshard-topology">
  <figcaption>Example vshard cluster topology</figcaption>
  <img src="../../../../../assets/images/client/few-nodes-crud.svg" alt="">
</figure>  

To configure `TarantoolCrudClient`, you need to use the
`TarantoolCrudClientBuilder` API:

```java
final TarantoolCrudClientBuilder crudClientBuilder = TarantoolFactory.crud();
```

Next, you need to configure connection groups to nodes. A connection group is configured
via the `InstanceConnectionGroup.Builder` API. The following code allows you to configure a connection group to
the router node `Router 1 (replicaset-1)` (see <a href="#vshard-topology">topology</a>):

```java
final InstanceConnectionGroup firstRouterConnectionGroup = InstanceConnectionGroup.builder()
    .withAuthType(AuthType.CHAP_SHA1)
    .withHost("localhost")
    .withPort(3301)
    .withUser("seller-user")
    .withPassword("pwd-1")
    .withSize(2)
    .withTag("router-1")
    .build();
```

???+ note "Note"

    For a more detailed description of the `InstanceConnectionGroup` class, you can refer to
    the corresponding [page](./instance-connection-group.md) of the documentation. Also, study the 
    `InstanceConnectionGroup` class in the Javadoc

Create an `InstanceConnectionGroup` instance for the second router `Router 2 (replicaset-2)`
(see <a href="#vshard-topology">topology</a>):

```java
final InstanceConnectionGroup secondRouterConnectionGroup = InstanceConnectionGroup.builder()
    .withPort(3302)
    .withUser("user-1182")
    .withPassword("pwd-2")
    .withSize(2)
    .withTag("router-2")
    .build();
```

Next, add the previously created `InstanceConnectionGroup` instances to the `TarantoolCrudClientBuilder`:

```java
final List<InstanceConnectionGroup> connectionGroupsList = Arrays.asList(firstRouterConnectionGroup,
    secondRouterConnectionGroup); // (1)!

final TarantoolCrudClient crudClient = crudClientBuilder.withGroups(connectionGroupsList)
    .build(); // (2)!
```  

1. Allows setting a list of previously created `InstanceConnectionGroup` instances that reflect
   the settings of connection groups to the corresponding routers
2. Create an instance of the `TarantoolCrudClient` class. The client is lazy and the connection to
   an available node occurs upon the first call to data manipulation methods

To access data manipulation methods, obtain an instance of
`TarantoolCrudSpace` through the `TarantoolCrudClient` API:

```java
// Allows getting an instance of TarantoolCrudSpace. The space has the name 'person'
final TarantoolCrudSpace personSpace = crudClient.space("person"); 
```

Using `TarantoolCrudSpace` instances, you work with data in a specific space. In
the example, this is the space named `person`.

???+ note "Note"

    Request distribution between nodes is performed according to the balancing rules. Refer to 
    [section](./balancer.md) to learn more.

### Connecting to Multiple Tarantool Nodes via TarantoolBoxClient API

In general, `TarantoolBoxClient` instances are intended for working with a single Tarantool node,
but there are scenarios where `TarantoolBoxSpace` can be used when working with multiple nodes:

<figure id="box-client-topology">
  <figcaption>Selecting data from multiple replicas of one shard</figcaption>
  <img src="../../../../../assets/images/client/few-replicas-box.svg" alt="">
</figure>  

In the example above, one of the replicas `replica-1` fails. The Java client needs to continue
data selection, switching to replica `replica-2`.

To configure the connection to two replica nodes (`replica-1`, `replica-2`), you need to use the
`TarantoolBoxClientBuilder` API:

```java
final TarantoolBoxClientBuilder boxBuilder = TarantoolFactory.box();
```

Next, you need to configure connection groups to replica nodes, similar to how it
is demonstrated in the section "[Connecting to Multiple Router Nodes via TarantoolCrudClient API](./connection-to-multiple-nodes.md):

```java
final InstanceConnectionGroup firstReplicaConnectionGroup = InstanceConnectionGroup.builder()
    .withUser("user-1298")
    .withPassword("pwd-1")
    .withPort(3301)
    .withSize(2)
    .withTag("replica-1")
    .build();

final InstanceConnectionGroup secondReplicaConnectionGroup = InstanceConnectionGroup.builder()
    .withUser("storage-user")
    .withPassword("pwd-2")
    .withPort(3302)
    .withSize(2)
    .withTag("replica-2")
    .build();
```

Add the previously created `InstanceConnectionGroup` instances to the `TarantoolBoxClientBuilder`:

```java
final List<InstanceConnectionGroup> connectionGroupsList = Arrays.asList(
    firstReplicaConnectionGroup,
    secondReplicaConnectionGroup);

final TarantoolBoxClient boxClient = boxClientBuilder.withGroups(connectionGroupsList)
    .build();
```

To access data manipulation methods, obtain an instance of
`TarantoolBoxSpace` through the `TarantoolBoxClient` API:

```java
final TarantoolBoxSpace personSpace = boxClient.space("person");
```

Using `TarantoolBoxSpace` instances, you work with data in a specific space. In
the example, this is the space named `person`.

???+ warning "Important"

    Thus, when one of the replicas fails, the Java client will switch to the second active replica 
    according to the balancing rules. Refer to [section](./balancer.md) 
    to learn more
