---
title: Heartbeat Architecture
---

## Heartbeat

`heartbeat` is a background task that sends periodic ping requests to the Tarantool node and
analyzes the results of successful and unsuccessful ping requests. The `heartbeat` task is set up for
each `PoolEntry` in [IProtoClientPool](./connection-pool.md). The main purpose of `heartbeat` -
to monitor the availability of the connection to the `Tarantool` node

### How It Works

To determine the availability of the Tarantool node, a sliding window approach is used. Let's imagine
that the following `heartbeat` settings were specified:

- `invalidationThreshold == 2`
- `deathThreshold == 4`
- `windowSize == 3`

<figure id="heartbeat">
  <figcaption>How heartbeat works</figcaption>
  <img src="../../../../../assets/drawio/client/heartbeat.drawio" alt="">
</figure>

Availability analysis of the node is performed using ping requests. If the number of failed ping requests
at the moment of consideration exceeds the value `invalidationThreshold` in the window, then the connection is excluded
from the [connection pool](./connection-pool.md) for selection by the [balancer](./balancer.md) (4,5,6). For
the excluded connection, the `heartbeat` process continues. Each exceeding of `invalidationThreshold` 
increases the `currentDeathThreshold` counter by 1. If `currentDeathThreshold` reaches the
value `deathThreshold`, the connection is considered `dead` (status `KILL`) and the reconnection process
of the connection is started (7).

<figure id="heartbeat-1">
  <figcaption>Transition from INVALIDATE to ACTIVATE</figcaption>
  <img src="../../../../../assets/drawio/client/heartbeat-1.drawio" alt="">
</figure>

The transition from the `INVALIDATE` state to the `ACTIVATE` state occurs if in subsequent iterations
of availability analysis, the number of failed ping requests is less than the value `invalidationThreshold`.
The `currentDeathThreshold` counter is reset to the value `0` (see <a href="#heartbeat-1">
diagram</a>)

The transition from the `KILL` state to the `ACTIVATE` state occurs if the connection was successfully
reconnected.

### Heartbeat Parameters

<table>
    <tr align="center">
        <td>Parameter Name</td>
        <td>Description</td>
        <td>Default Value</td>
    </tr>
    <tr>
        <td align="center">pingInterval</td>
        <td>The time in milliseconds after which the next ping request is executed</td>
        <td>3000</td>
    </tr>
    <tr>
        <td align="center">invalidationThreshold</td>
        <td>The number of failed ping requests in the window, upon reaching which <b>PoolEntry</b> 
            is removed from the connection pool (the balancer stops seeing this connection during 
            selection). Ping requests on this connection continue to be executed
        </td>
        <td>2</td>
    </tr>
    <tr>
        <td align="center">windowSize</td>
        <td>The size of the sliding window (in terms of ping requests)</td>
        <td>4</td>
    </tr>
    <tr>
        <td align="center">deathThreshold</td>
        <td>The number of invalidationThreshold exceedances after which the connection is switched to 
            the reconnection state
        </td>
        <td>4</td>
    </tr>
    <tr>
        <td align="center">pingFunction</td>
        <td>The method for executing the verification request (ping request)</td>
        <td>IProtoClient::ping</td>
    </tr>
</table>

### Parameter Configuration

To configure the `heartbeat` parameters, use the `HeartbeatOpts` API:

```java
final HeartbeatOpts heartbeatOpts = HeartbeatOpts.getDefault()
    .withPingInterval(pingInterval)
    .withWindowSize(windowSize)
    .withDeathThreshold(deathThreshold)
    .withInvalidationThreshold(invalidationThreshold)
    .withPingFunction(pingFunction);

final TarantoolCrudClient crudClient = TarantoolFactory.crud()
    // ... other options
    .withHeartbeat(heartbeatOpts)
    .build();
```
