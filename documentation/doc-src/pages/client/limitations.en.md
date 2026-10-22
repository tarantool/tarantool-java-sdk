---
title: Limitations and workarounds
---

## What is not supported and how to work around it

The `crud` module on the Tarantool side has no batch delete (`delete_many`) and no `IN` condition:
conditions of a single `select` support only `==`, `<`, `<=`, `>`, `>=` and are combined by `AND`.
The client mirrors this: `TarantoolCrudSpace` has `insertMany`, `replaceMany` and `upsertMany`, but
no `deleteMany`. The workarounds below send several requests at once; requests of one client are
pipelined over its connections, so they do not wait for each other.

???+ warning "Important"

    Neither workaround is atomic: each key is a separate operation, and keys can live on
    different replica sets. Some requests may succeed while others fail; check every future.
    The speed of these approaches relative to each other was not measured.

### Delete several records by primary key

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.tarantool.client.crud.TarantoolCrudSpace;

CompletableFuture<?>[] futures =
    keys.stream().map(space::delete).toArray(CompletableFuture[]::new);
CompletableFuture.allOf(futures).get();
```

In Spring Data, `deleteAllById(ids)` sends one request per key and waits for each response before
sending the next request.

Alternatively, write a stored procedure on the router that deletes the keys in a loop and call it with
`client.call(...)`: this is one network request from the application (also not atomic).

### Select by `col IN (v1, v2, v3)`

Send one `select` with the `==` condition per value in parallel and merge the results:

```java
import io.tarantool.client.crud.Condition;
import io.tarantool.mapping.Tuple;

List<CompletableFuture<List<Tuple<List<?>>>>> futures =
    values.stream()
        .map(v -> space.select(Condition.create("==", "col", v)))
        .collect(Collectors.toList());
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
List<Tuple<List<?>>> result = new ArrayList<>();
for (CompletableFuture<List<Tuple<List<?>>>> f : futures) {
  result.addAll(f.get());
}
```

If `col` is the sharding key, each request goes to a single replica set; otherwise each `select` walks
all storages. For the primary key use `space.get(key)` for each key in parallel. Another option is
a stored procedure that takes the list of values and runs the selects on the server side.

### Spring Data derived methods

Supported keywords are listed in [Dynamic Queries](../spring-data/derived-methods.md). Keywords such
as `In`, `NotIn`, `Like` are not supported: such a method fails with
`InvalidDataAccessApiUsageException: Unsupported type ...`; a method with `And` or `Or` fails with
`UnsupportedOperationException`. Use the approaches above, or
[stored procedures](../spring-data/query-lua.md).
