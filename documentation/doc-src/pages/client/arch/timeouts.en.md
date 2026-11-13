---
title: Timeouts
---

The client has two independent timeouts: the request timeout limits waiting for the response to
each request, and the connect timeout limits opening a connection to a node.

## Request timeout

Every client request is limited in time: 5000 ms by default. This is
`BaseOptions.DEFAULT_TIMEOUT`; the option classes of operations (`SelectOptions`, `InsertOptions`
and others) have their own `DEFAULT_TIMEOUT` constant with the same value.

There is no client-wide request timeout setting: neither `TarantoolCrudClientBuilder` nor
`TarantoolBoxClientBuilder` provides one. A different timeout is set per request with
`withTimeout(...)` in its options. The value must be greater than zero, otherwise the options
builder throws `IllegalArgumentException`.

```java
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import io.tarantool.client.BaseOptions;
import io.tarantool.client.Options;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.mapping.Tuple;

public class Timeouts {

  // stored procedure call with a 1 second timeout
  public static void call(TarantoolCrudClient client) {
    Options options = BaseOptions.builder().withTimeout(1_000L).build();
    client.call("slow_function", Collections.emptyList(), options).join();
  }

  // select via crud with a 1 second timeout
  public static List<Tuple<List<?>>> select(
      TarantoolCrudSpace space, List<Condition> conditions) {
    SelectOptions options = SelectOptions.builder().withTimeout(1_000L).withFirst(500).build();
    try {
      return space.select(conditions, options).join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof TimeoutException) {
        // no response within 1 second: retry the request or report the error
      }
      throw e;
    }
  }
}
```

When the time is up, the request's `CompletableFuture` completes with
`java.util.concurrent.TimeoutException`. On `join()` it arrives wrapped in `CompletionException`,
as in the example above.

The countdown starts when the [balancer](balancer.md) has provided a connection for the request.
The time spent opening the connection is not part of the request timeout.

`SelectOptions` also has `withCrudTimeout(...)`. This is a different setting: it passes the value
to the `timeout` option of the `crud.select` function on the server and does not change how long
the client waits for the response.

## Connect timeout

The connect timeout is set in the client builder with `withConnectTimeout(...)`, and in Spring Data
with the `spring.data.tarantool.connect-timeout` property. The default is 3000 ms
(`TarantoolClient.DEFAULT_CONNECTION_TIMEOUT`). It counts from the start of the connect until the
greeting from the node is received.

The TCP connect itself is limited separately by the Netty channel option `CONNECT_TIMEOUT_MILLIS`:
in the default channel options (`TarantoolClient.DEFAULT_NETTY_CHANNEL_OPTIONS`) it is 3000 ms.
`withConnectTimeout(...)` and `connect-timeout` do not change it; it is set in the builder with
`withChannelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, ...)`, and in Spring Data only in your own
builder bean. So, according to the SDK code, a `withConnectTimeout(...)` above 3000 ms does not
extend the TCP connect itself (not measured).

After the greeting the client authenticates with a separate request with a 5000 ms timeout. If the
connection is opened as `guest` (the connection group has no user or the user is `guest`), there is
no authentication: instead the client sends one ping request with a 1000 ms timeout.

Connections of the CRUD client are opened not when it is created but on first use (see
[connection pool](connection-pool.md), which also describes the exception for the Box client). So
the first request over a connection that is not open
yet first waits for the connect and authentication, and only then runs. To keep the first requests
from waiting, warm up the pool when the application starts (section "Warming up the pool at
startup" on the [Connection Pool](connection-pool.md) page).

## Spring Data

Of the timeouts, the `spring.data.tarantool.*` properties have only `connect-timeout`. There is no
request timeout property: every request sent by a repository method runs with the 5000 ms timeout.
If an operation needs a different timeout, run it through the `TarantoolCrudClient` API with
options, as shown above.
