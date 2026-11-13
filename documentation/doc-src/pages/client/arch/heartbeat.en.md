---
title: Heartbeat Architecture
---

## Heartbeat

Heartbeat is a background task that regularly sends ping requests to a Tarantool node and uses
their results to decide whether the connection is available. The task runs for every `PoolEntry`,
that is, for every connection of the [pool](connection-pool.md). The [balancer](balancer.md) does
not pick a connection that heartbeat has found unavailable.

### Enabling

Heartbeat is off by default. It runs only if `withHeartbeat(...)` is set in the client builder,
or the `spring.data.tarantool.heartbeat` section is present in Spring Data. Without heartbeat
there are no periodic ping requests, and a connection is locked only when a connect attempt fails
or the node closes the connection. According to the SDK code, request timeouts do not lock a
connection, so without heartbeat a node that stopped responding but did not close the connection is
not excluded from selection.

### How it works

Node availability is judged by a sliding window of the latest ping requests. Consider an example
with the settings:

- `invalidationThreshold == 2`
- `deathThreshold == 3`
- `windowSize == 3`

<figure markdown="span" id="heartbeat">
<figcaption>How heartbeat works</figcaption>
![](../../../assets/drawio/client/heartbeat.drawio)
</figure>

The check runs after every ping request once the window is full. If the number of failed ping
requests in the window is greater than or equal to `invalidationThreshold`, the connection moves
to the `INVALIDATE` state: it stays in the pool, but the balancer no longer picks it (steps 4, 5, 6
on the diagram). Ping requests over this connection continue.

Every check on which the `invalidationThreshold` is reached increments the `currentDeathPings`
counter by 1. When the counter becomes greater than `deathThreshold`, the connection is considered
dead (the `KILL` state), and the client starts reconnecting it. On the diagram checks 4, 5, 6 and 7
set the counter to 1, 2, 3 and 4; at step 7 the counter exceeds `deathThreshold == 3`, and the
connection moves to `KILL`.

<figure markdown="span" id="heartbeat-1">
<figcaption>Transition from INVALIDATE to ACTIVATE</figcaption>
![](../../../assets/drawio/client/heartbeat-1.drawio)
</figure>

If on a later check the window has fewer failed ping requests than `invalidationThreshold`, the
connection returns to the `ACTIVATE` state, and the `currentDeathPings` counter is reset to 0 (see
the <a href="#heartbeat-1">diagram</a>). So only consecutive checks with the threshold reached lead
to `KILL`.

From the `KILL` state the connection returns to `ACTIVATE` after a successful reconnect.

With the default parameters (a window of 4 ping requests, `invalidationThreshold` 2,
`deathThreshold` 4) a connection is excluded from selection when at least 2 of the last 4 ping
requests failed, and is reconnected on the fifth such check in a row.

### Parameters

| Parameter               | Purpose                                                           | Default              |
|-------------------------|-------------------------------------------------------------------|----------------------|
| `pingInterval`          | Interval between ping requests and timeout of each ping request, ms | 3000               |
| `windowSize`            | Size of the sliding window, in ping requests                      | 4                    |
| `invalidationThreshold` | Number of failed ping requests in the window at which the connection is excluded from selection | 2 |
| `deathThreshold`        | How many checks in a row with the threshold reached are allowed before a reconnect | 4 |
| `pingFunction`          | Function of the check request                                     | `IProtoClient::ping` |

### Configuration

Parameters are set with `HeartbeatOpts`. `HeartbeatOpts.getDefault()` returns the default
parameters; the `with...` methods change individual values.

```java
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.HeartbeatOpts;

public class HeartbeatConfig {

  public static TarantoolCrudClient createClient() throws Exception {
    HeartbeatOpts heartbeatOpts =
        HeartbeatOpts.getDefault()
            .withPingInterval(1_000L)
            .withWindowSize(4)
            .withInvalidationThreshold(2)
            .withDeathThreshold(4);

    return TarantoolFactory.crud()
        // ... other settings
        .withHeartbeat(heartbeatOpts)
        .build();
  }
}
```

`pingInterval` is also the timeout of a ping request: in the example above a ping without a response
within 1 second counts as failed. A custom check function can be set with `withPingFunction(...)`.

In Spring Data the same parameters are set in the `spring.data.tarantool.heartbeat` section:

```yaml
spring:
  data:
    tarantool:
      heartbeat:
        ping-interval: 1000
        window-size: 4
        invalidation-threshold: 2
        death-threshold: 4
```
