---
title: Connection Group
---

## InstanceConnectionGroup

`InstanceConnectionGroup` describes a group of connections to one Tarantool node: the node
address, credentials, the number of connections and other parameters.

### Connection groups

<figure markdown="span" id="instance-connection-group-img-anchor">
<figcaption>Location of <b>InstanceConnectionGroup</b> in the Java client architecture</figcaption>
![](../../../assets/drawio/client/instance-connection-group.drawio)
</figure>

The high-level clients `TarantoolCrudClient` and `TarantoolBoxClient` work with Tarantool nodes
through connection groups. Each group is a set of connections (`IProtoClient`) to one node.

When a client is created, the [connection pool](connection-pool.md) `IProtoClientPool` creates one
group for every passed `InstanceConnectionGroup` object.

Several connections in one group let parallel requests to a node spread over different
connections; whether this improves client performance depends on the load (not measured). The [balancer](balancer.md) decides which connection a request gets.

### Parameters

| Parameter                   | Purpose                                                           | Default                     |
|-----------------------------|-------------------------------------------------------------------|-----------------------------|
| `host`                      | Name or IP address of the node                                    | `localhost`                 |
| `port`                      | Port on which the node accepts connections                        | `3301`                      |
| `user`                      | User for authentication on the node                               | `guest`                     |
| `password`                  | Password of `user`                                                | not set                     |
| `size`                      | Number of connections to the node (Netty connections, see the diagram above) | `1`              |
| `tag`                       | Group name: identifies the group in the pool and in logs          | `<user>:<host>:<port>`      |
| `authType`                  | Authentication algorithm, see below                               | `AuthType.CHAP_SHA1`        |
| `flushConsolidationHandler` | Netty [FlushConsolidationHandler](https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html) for the group's connections | not set |

`host` and `port` are set the same way as for
[InetSocketAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetSocketAddress.html).

The `guest` user logs in without a password: if `user` is not set or is `guest`, a password must
not be set, otherwise `build()` throws `IllegalArgumentException`.

`authType` must match the way the user's password is stored on the node (see the Javadoc of the
`AuthType` class and the
[Tarantool documentation](https://www.tarantool.io/en/doc/latest/reference/configuration/configuration_reference/#confval-security.auth_type)).

### Host name resolution

The host name from `host` is resolved to an IP address once, when the group is created. All
connects and reconnects of the group go to this address. If the node's IP address changes (the
container or VM is recreated, the DNS record changes), the client keeps connecting to the old
address; to connect to the new one, recreate the client.

### Usage

A group is created with `InstanceConnectionGroup.Builder` and passed to the client builder
(`TarantoolCrudClientBuilder` or `TarantoolBoxClientBuilder`):

```java
import java.util.Collections;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.core.protocol.requests.IProtoAuth.AuthType;
import io.tarantool.pool.InstanceConnectionGroup;

public class ConnectionGroupConfig {

  public static TarantoolCrudClient createClient() throws Exception {
    InstanceConnectionGroup connectionGroup =
        InstanceConnectionGroup.builder()
            .withHost("localhost")
            .withPort(3301)
            .withUser("user2581-test")
            .withPassword("pwd-1")
            .withSize(3)
            .withTag("Node-1")
            .withAuthType(AuthType.CHAP_SHA1)
            .build();

    return TarantoolFactory.crud()
        .withGroups(Collections.singletonList(connectionGroup))
        .build();
  }
}
```

If no groups are passed, `TarantoolCrudClientBuilder` creates one group from its `withHost(...)`,
`withPort(...)`, `withUser(...)` and `withPassword(...)` with one connection and the `default` tag.
If groups are passed, these builder settings are not used.

The default user of `TarantoolCrudClientBuilder` is `api_user` with the password `secret`, not
`guest`. The `secret` password stays after `withUser("guest")`, and `guest` with a password is not
allowed, so to log in as guest without your own groups reset the password explicitly:
`withUser("guest").withPassword(null)`.
