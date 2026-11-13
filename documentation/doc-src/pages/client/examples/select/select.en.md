---
title: Selecting and paginating
---

## How to select data

Data is selected from a cluster space with the `select` method of the `TarantoolCrudSpace`
interface. The client calls the `crud.select` function on the router and passes it the selection
conditions and options. A condition is a `Condition` object (operator, field, value); several
conditions are combined with `AND`.

```java
TarantoolCrudSpace space = crudClient.space("person");

List<Tuple<List<?>>> persons = space.select(Condition.create(">", "id", 5)).join();
```

Every tuple of the result is wrapped in `Tuple`: `get()` returns its fields. For a tuple as a list
this is `List<?>`, for a selection with `Class<T>` it is a `T` object.

### `select` variants

All variants return a `CompletableFuture` with the result.

| Variant                                                    | Result                 |
|------------------------------------------------------------|------------------------|
| `select(Condition...)`, `select(List<Condition>)`          | `List<Tuple<List<?>>>` |
| `select(List<Condition>, SelectOptions)`                   | `List<Tuple<List<?>>>` |
| `select(List<Condition>, Class<T>)`                        | `List<Tuple<T>>`       |
| `select(List<Condition>, SelectOptions, Class<T>)`         | `List<Tuple<T>>`       |
| `select(List<Condition>, TypeReference<T>)`                | `TarantoolResponse<T>` |
| `select(List<Condition>, SelectOptions, TypeReference<T>)` | `TarantoolResponse<T>` |
| `select(Options, TypeReference<T>, Object...)`             | `TarantoolResponse<T>` |

The variants with conditions but without `SelectOptions` use the default options, that is, the result of
`SelectOptions.builder().build()`. The last variant passes its arguments to `crud.select` as is:
you build the conditions and the selection options yourself, and the client adds nothing to them.

How tuples are mapped to classes is described in [Data Mapping](../../arch/tuple_pojo_mapping.md).

## How many tuples a selection returns

### Default limit

`SelectOptions.builder()` sets the `first` option to `SelectOptions.DEFAULT_LIMIT`, which is 100,
right away. So a selection without an explicit `withFirst(...)` returns **at most 100 tuples**, even
if more tuples match. This applies to the variants with conditions but without `SelectOptions` and
to variants with options built with `SelectOptions.builder()` without `withFirst(...)`. It does not
apply to `select(Options, TypeReference<T>, Object...)`, which passes its arguments as is.

There is no sign that the result was truncated: a list of 100 tuples does not tell you whether
more tuples match.

```java
// at most 100 tuples, even if 1000 match
List<Tuple<List<?>>> persons = space.select(Condition.create(">", "id", 5)).join();
```

The same limit applies in Spring Data to derived methods without `First`/`Top` in the name and
without a `Limit` parameter, see [Dynamic Queries](../../../spring-data/derived-methods.md).

### Explicit limit

If you know how many tuples can match, set a limit with a margin using `withFirst(...)`:

```java
SelectOptions options = SelectOptions.builder().withFirst(500).build();

List<Tuple<List<?>>> persons =
    space.select(Collections.singletonList(Condition.create(">", "id", 5)), options).join();
```

If you cannot name an upper bound, read page by page.

## Pagination

The `after` option sets the tuple after which the selection starts. To read all matching tuples,
request pages of a fixed size (`withFirst(...)`) and pass the last tuple of the previous page to
`withAfter(...)`. Reading ends when a page is shorter than the page size, including an empty page.

For tuples as lists, pass the field list of the last tuple to `withAfter(...)`, that is, `get()` of
its `Tuple`:

```java
import java.util.ArrayList;
import java.util.List;

import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.mapping.Tuple;

public class SelectAll {

  public static List<Tuple<List<?>>> selectAll(
      TarantoolCrudSpace space, List<Condition> conditions, int pageSize) {
    List<Tuple<List<?>>> all = new ArrayList<>();
    List<?> after = null;
    while (true) {
      SelectOptions.Builder options = SelectOptions.builder().withFirst(pageSize);
      if (after != null) {
        options.withAfter(after);
      }
      List<Tuple<List<?>>> page = space.select(conditions, options.build()).join();
      all.addAll(page);
      if (page.size() < pageSize) {
        return all;
      }
      after = page.get(page.size() - 1).get();
    }
  }
}
```

For a selection with `Class<T>`, pass the last object of the previous page to `withAfter(...)`:

```java
import java.util.ArrayList;
import java.util.List;

import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.mapping.Tuple;

public class SelectAllPersons {

  public static List<Person> selectAll(
      TarantoolCrudSpace space, List<Condition> conditions, int pageSize) {
    List<Person> all = new ArrayList<>();
    Person after = null;
    while (true) {
      SelectOptions.Builder options = SelectOptions.builder().withFirst(pageSize);
      if (after != null) {
        options.withAfter(after);
      }
      List<Tuple<Person>> page = space.select(conditions, options.build(), Person.class).join();
      for (Tuple<Person> tuple : page) {
        all.add(tuple.get());
      }
      if (page.size() < pageSize) {
        return all;
      }
      after = page.get(page.size() - 1).get();
    }
  }
}
```

Here `Person` is your class mapped to the tuples of the space.

???+ warning "This is not a snapshot"

    Every page is a separate request, and the data may change between requests. A result
    collected page by page is not a consistent snapshot of the space.
