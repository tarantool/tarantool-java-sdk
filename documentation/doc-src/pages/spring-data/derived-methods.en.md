---
title: Dynamic Queries
---

## Custom Dynamic Queries

To use dynamic queries, define the necessary method in
the custom `Repository`. For example:

```java
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  List<Person> findByName(String name);

  List<Person> findPersonByNameAfter(String name);
}
```

???+ note "Note"

    More information about dynamic queries in
    [`official documentation`](https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html)

???+ note "Note"

    The following methods and keywords are available:

    - findBy
    - deleteBy
    - existsBy
    - countBy

    Keywords:

    - True
    - False
    - Equal
    - LessThan
    - LessThanEqual
    - GreaterThan
    - GreaterThanEqual
    - Between
    - IsNull
    - Distinct
    - IsEmpty
    - ExistsBy
    - First
    - Top
    - After
    - Before

???+ warning "Important"

    Currently, only methods with a single predicate are supported (nested `AND` or 
    `OR` are not supported).

???+ note "Note"

    In version tarantool-spring-data32, the ability to use the [Limit](https://docs.spring.io/spring-data/data-commons/docs/3.2.3/api/org/springframework/data/domain/Limit.html)
    interface in derived methods has been added.

    **Important**:
    
    - The maximum number of expected results must be a **positive number**, otherwise an exception will be thrown.
    - Priority of result count limitation:
        - `Limit.of(...)` takes precedence over static limitation in method name 
          (keywords Top/First).
    - Static limitation in method names takes precedence over `Limit.unlimited()` or passing 
      `null` as `Limit`.
    
    Examples:
    
    ```java
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, Limit.of(10)); // returns <= 10 records
    // ...
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, Limit.unlimited()); // returns <= 100 records
    // ...
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, null); // returns <= 100 records
    // ...
    List<Person> result = repository.findTop4ByIdGreaterThanEqual(0, Limit.of(10)); // returns <= 10 records
    // ...
    List<Person> result = repository.findTop4ByIdGreaterThanEqual(0, Limit.unlimited()); // returns <= 4 records
    // ...
    List<Person> result = repository.findTop4ByIdGreaterThanEqual(0, null); // returns <= 4 records
    // ...
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, Limit.of(-10)); // throws IllegalArgumentException
    ```

More examples can be found in the tests.
