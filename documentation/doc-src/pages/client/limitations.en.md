---
title: Limitations and workarounds
---

## What is not supported and how to work around it

The `crud` module on the Tarantool side has no batch delete (`delete_many`) and no `IN` condition:
conditions of a single `select` support only `==`, `<`, `<=`, `>`, `>=` and are combined with
`AND`. The client mirrors this: `TarantoolCrudSpace` has `insertMany`, `replaceMany` and
`upsertMany`, but no `deleteMany`.

The workarounds below send several requests at once. Requests of one client are pipelined over its
connections and do not wait for each other.

???+ warning "Important"

    Neither workaround is atomic: each key is a separate operation, and keys can live on different
    replica sets. Some requests may succeed while others fail, so check the result of every
    request. The speed of these approaches relative to each other was not measured.

### Delete several records by primary key

Send `delete` for every key and check every result. The method below returns the keys whose
request completed exceptionally:

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.mapping.Tuple;

public class DeleteMany {

  public static List<Object> deleteMany(TarantoolCrudSpace space, List<?> keys) {
    List<CompletableFuture<Tuple<List<?>>>> futures =
        keys.stream().map(space::delete).collect(Collectors.toList());
    List<Object> failed = new ArrayList<>();
    for (int i = 0; i < futures.size(); i++) {
      try {
        futures.get(i).join();
      } catch (CompletionException e) {
        failed.add(keys.get(i));
      }
    }
    return failed;
  }
}
```

Two cases need attention:

- if the response has no tuple, according to the SDK code the future completes with `null`, not
  with an exception, and such a key is not in the list. To find out whether a tuple was deleted,
  check the result of `join()` for `null`;
- on a timeout the outcome is undefined: according to the SDK code the timeout completes the future
  on the client side but does not cancel the request on the server, so the record may have been
  deleted.

In Spring Data, `deleteAllById(ids)` sends one request per key and waits for each response before
sending the next request.

Alternatively, write a stored procedure on the router that deletes the keys in a loop and call it
with `client.call(...)`: the application sends one network request (also not atomic).

### Select by `col IN (v1, v2, v3)`

Send one `select` with the `==` condition per value in parallel and merge the results:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.mapping.Tuple;

public class SelectIn {

  public static List<Tuple<List<?>>> selectIn(
      TarantoolCrudSpace space, String field, List<?> values, SelectOptions options) {
    List<CompletableFuture<List<Tuple<List<?>>>>> futures =
        values.stream()
            .map(v -> Collections.singletonList(Condition.create("==", field, v)))
            .map(conditions -> space.select(conditions, options))
            .collect(Collectors.toList());
    List<Tuple<List<?>>> result = new ArrayList<>();
    for (CompletableFuture<List<Tuple<List<?>>>> future : futures) {
      result.addAll(future.join());
    }
    return result;
  }
}
```

Set `withFirst(...)` in `options` with a margin for one value: without it each `select` returns at
most 100 tuples per value. If you cannot name an upper bound, read each value page by page (see
[Selecting and paginating](examples/select/select.md)).

If `col` is the sharding key, each request goes to a single replica set; otherwise each `select`
walks all storages. For the primary key use `space.get(key)` for each key in parallel. Another
option is a stored procedure that takes the list of values and runs the selects on the server side.

### Spring Data derived methods

Supported keywords are listed in [Dynamic Queries](../spring-data/derived-methods.md). The `In`,
`NotIn` and `Like` keywords are not supported: such a method fails with
`InvalidDataAccessApiUsageException: Unsupported type ...`. A method with `And` or `Or` fails with
`UnsupportedOperationException`. Use the approaches above, or
[stored procedures](../spring-data/query-lua.md).
