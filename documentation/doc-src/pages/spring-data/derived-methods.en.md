---
title: Dynamic Queries
---

## Derived methods

A derived (dynamic) query is a repository method whose query Spring Data builds from the method
name. To use one, declare the method in your `Repository` interface:

```java
import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  List<Person> findByName(String name);

  List<Person> findPersonByNameAfter(String name);
}
```

Here `Person` is the entity class of the space. The general naming rules are described in the
[Spring Data documentation](https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html).

### Supported methods and keywords

Methods:

- `findBy`
- `deleteBy`
- `existsBy`
- `countBy`

Keywords:

- `True`, `False`
- `Equal`
- `LessThan`, `LessThanEqual`
- `GreaterThan`, `GreaterThanEqual`
- `Between`
- `IsNull`
- `IsEmpty`
- `Distinct`
- `First`, `Top`
- `After`, `Before`

A method can have only one predicate: `And` and `Or` are not supported, and such a method fails
with `UnsupportedOperationException`. Any keyword not listed above (for example `In`, `NotIn`,
`Like`) fails with `InvalidDataAccessApiUsageException: Unsupported type ...`. Workarounds are
described in [Limitations and workarounds](../client/limitations.md).

### How many records a method returns

If the method name has no `First`/`Top` and no `Limit` parameter is passed, the query runs with the
`SelectOptions.DEFAULT_LIMIT` limit, which is 100. There is no sign that the result was truncated.

- `findBy...` that returns a list, a stream or one record returns **at most 100 records**.
- Methods with `Pageable` and `Scroll` return at most the page size. For `Page` the query runs with
  the page size as the limit; for `Slice` and `Scroll` the SDK requests one record more to find out
  whether there is a next page.
- `deleteBy...` deletes **at most 100 records**: the method first selects records with this limit
  and then deletes the found ones one by one. Other matching records stay in the space.
- `countBy...` counts all matching records. The exception is `countDistinctBy...`: it is limited to
  100 records as well.

To read more records, pass `Limit.of(...)` (see below) or read page by page with
[Page, Slice](page-slice-pageable.md) or the [Scroll API](scroll-api.md).

`findAll()` and `findAll(Sort)` are not supported and fail with `UnsupportedOperationException`.
Read all records of a space page by page with `findAll(Pageable)`.

Selection in the client without Spring Data is limited in the same way, see
[Selecting and paginating](../client/examples/select/select.md).

### The `Limit` parameter

Starting with the `tarantool-spring-data-32` module, derived methods accept a
[Limit](https://docs.spring.io/spring-data/data-commons/docs/3.2.3/api/org/springframework/data/domain/Limit.html)
parameter.

- A negative `Limit.of(...)` value leads to `IllegalArgumentException`: the SDK checks only for
  "less than zero", so 0 passes this check.
- `Limit.of(...)` takes precedence over the limit in the method name (`Top`/`First`).
- The limit in the method name takes precedence over `Limit.unlimited()` and `null` passed as
  `Limit`.
- Without a limit in the name, `Limit.unlimited()` and `null` give the default limit of 100
  records.

```java
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  List<Person> findPersonByIdGreaterThanEqual(int id, Limit limit);

  List<Person> findTop4ByIdGreaterThanEqual(int id, Limit limit);
}
```

```java
List<Person> a = repository.findPersonByIdGreaterThanEqual(0, Limit.of(10)); // <= 10 records
List<Person> b = repository.findPersonByIdGreaterThanEqual(0, Limit.unlimited()); // <= 100 records
List<Person> c = repository.findPersonByIdGreaterThanEqual(0, null); // <= 100 records
List<Person> d = repository.findTop4ByIdGreaterThanEqual(0, Limit.of(10)); // <= 10 records
List<Person> e = repository.findTop4ByIdGreaterThanEqual(0, Limit.unlimited()); // <= 4 records
List<Person> f = repository.findTop4ByIdGreaterThanEqual(0, null); // <= 4 records
repository.findPersonByIdGreaterThanEqual(0, Limit.of(-10)); // IllegalArgumentException
```

More examples can be found in the tests of the `tarantool-spring-data` modules.
