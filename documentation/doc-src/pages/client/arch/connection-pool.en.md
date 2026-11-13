---
title: Connection Pool
---

## Connection Pool (IProtoClientPool)

The `IProtoClientPool` connection pool is a set of connections to Tarantool nodes grouped into
logical groups. A group is described by an [InstanceConnectionGroup](instance-connection-group.md)
object: which node to connect to, how many connections to open and which credentials to use.

When `TarantoolCrudClient` or `TarantoolBoxClient` is created, one `IProtoClientPool` instance is
created. From the passed `InstanceConnectionGroup` objects the pool creates `PoolEntry` objects,
one per connection. A `PoolEntry` manages the lifecycle of its connection: connect, disconnect,
reconnect and [heartbeat](heartbeat.md). Each `PoolEntry` belongs to one group, and all
`PoolEntry` objects of a group hold their own connections to the same node.

<figure markdown="span" id="connection-pool-1">
<figcaption>Location of <b>IProtoClientPool</b> in the Java client architecture</figcaption>
![](../../../assets/drawio/client/connection-pool.drawio)
</figure>

The pool itself does not distribute requests: it stores the connections and hands the available
ones to the [balancer](balancer.md). The balancer decides which connection a request gets.

### When connections are opened

A connection is opened when it is requested from the pool for the first time, that is, when the
balancer picks it for a request for the first time. Creating a CRUD client does not open
connections, so the first requests after the application starts wait for the connect to the node
and authentication (see [Timeouts](timeouts.md)).

The Box client loads the space schema by default (`TarantoolBoxClient.DEFAULT_FETCH_SCHEMA`): while
being created it sends the schema requests through the balancer and waits for the response, so the
connections for these requests are opened in `build()`. `withFetchSchema(false)` in
`TarantoolBoxClientBuilder` turns schema loading off.

### Warming up the pool at startup

There is no setting to open all connections when the client is created. To keep the first
requests from waiting for a connect, open the connections yourself through the public pool API:
walk over every group and every connection in the group and wait for the connect.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.tarantool.client.TarantoolClient;
import io.tarantool.pool.IProtoClientPool;

public class PoolWarmUp {

  public static void warmUp(TarantoolClient client) {
    IProtoClientPool pool = client.getPool();
    List<CompletableFuture<?>> connects = new ArrayList<>();
    for (String tag : pool.getTags()) {
      for (int i = 0; i < pool.getGroupSize(tag); i++) {
        CompletableFuture<?> connect = pool.get(tag, i);
        if (connect != null) {
          connects.add(connect);
        }
      }
    }
    CompletableFuture.allOf(connects.toArray(new CompletableFuture[0])).join();
  }
}
```

`pool.get(tag, index)` returns a `CompletableFuture` with the connection and starts the connect on
the first call. For a locked connection (see [balancer](balancer.md)) the method returns `null`, so
the example skips such connections.

If a connect fails, the future of that connection completes exceptionally, and then `join()` in the
example throws `CompletionException`: this is how `CompletableFuture.allOf` works. If a failed
warm-up must not stop the application start, catch this exception and log it.
