---
title: Connection Group
---

## InstanceConnectionGroup

`InstanceConnectionGroup` is a class that aggregates parameters for configuring a group of connections to a Tarantool node.

### Connection Groups Concept

<figure id="instance-connection-group-img-anchor">
    <figcaption>Location of <b>InstanceConnectionGroup</b> in the Java client architecture</figcaption>
    <img src="../../../../../assets/drawio/client/instance-connection-group.drawio" alt="">
</figure>

High-level clients `TarantoolCrudClient`/ `TarantoolBoxClient` interact with
Tarantool nodes through logically separated groups of connections. Each group
represents a set of connections (`IProtoClient`) to one Tarantool node.

When creating a high-level client, the `IProtoClientPool` object creates connection groups based on
metadata passed in objects of the `InstanceConnectionGroup` class. One logical group of connections is created based on
metadata from one object of the `InstanceConnectionGroup` class.

Using multiple connections within one group allows you to increase the performance
of the Java client, especially in situations where data operations are performed in parallel. The selection
of a connection for executing a request is determined by the balancing rules ([more details](./balancer.md)).

### Parameter Description

<table>
    <tr align="center">
        <td>Parameter Name</td>
        <td>Description</td>
        <td>Default Value</td>
    </tr>
    <tr>
        <td id="host" align="center">host</td>
        <td>The address of the node to which the connection group should connect</td>
        <td>localhost</td>
    </tr>
    <tr>
        <td id="port" align="center">port</td>
        <td>The port on which the Tarantool node expects connections. Together with 
            <a href="#host">host</a> forms the complete address of the Tarantool node
        </td>
        <td>3301</td>
    </tr>
    <tr>
        <td id="user" align="center">user</td>
        <td>The username used for client authentication when connecting to the Tarantool node
        </td>
        <td>user</td>
    </tr>
    <tr>
        <td align="center">password</td>
        <td>Password for <a href="#user">user</a></td>
        <td>""</td>
    </tr>
    <tr>
        <td align="center">size</td>
        <td>The number of connections to one node (<a href="#instance-connection-group-img-anchor">
            Netty connection</a>)
        </td>
        <td>1</td>
    </tr>
    <tr>
        <td align="center">tag</td>
        <td>Tag name of the connection group. Required for identifying the group in the connection pool, logs</td>
        <td>user:host:port</td>
    </tr>
    <tr>
        <td align="center">flushConsolidationHandler</td>
        <td><a href="https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html">
            More details</a>
        </td>
        <td>null</td>
    </tr>
    <tr>
        <td align="center">authType</td>
        <td>The type of authentication algorithm for the Java client. See Javadoc for the 
            <code>AuthType</code> class and official 
            <a href="https://www.tarantool.io/en/doc/latest/reference/configuration/configuration_reference/#confval-security.auth_type">
            Tarantool documentation</a> for more details
        </td>
        <td>AuthType.CHAP_SHA1</td>
    </tr>
</table>

Values for <a href="#host">host</a> and <a href="#port">port</a> are specified according to
[InetSocketAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetSocketAddress.html).

### Usage

To create an instance of the `InstanceConnectionGroup` class, you need to use
`InstanceConnectionGroup.Builder`:

```java
final InstanceConnectionGroup connectionGroup = InstanceConnectionGroup.builder()
    .withHost("localhost")
    .withPort(3301)
    .withUser("user2581-test")
    .withPassword("pwd-1")
    .withSize(3)
    .withTag("Node-1")
    .withAuthType(AuthType.CHAP_SHA1)
    .build();
```

Then the group is added to the `TarantoolCrudClientBuilder` or `TarantoolBoxClientBuilder`:

```java
final TarantoolCrudClient crudClient = TarantoolFactory.crud()
    .withGroups(Arrays.asList(connectionGroup))
    .build();
```
