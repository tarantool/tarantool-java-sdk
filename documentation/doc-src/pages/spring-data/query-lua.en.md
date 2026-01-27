---
title: Stored Procedures
---

## Working with Stored Procedures and LUA Scripts

To work with LUA or stored procedures through `Repository`, declare a custom method (
the name is chosen at your discretion) and add the `@Query` annotation, selecting the
desired mode in the parameters:

```java
public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  @Query("echo")
  List<?> echo(String stringArg, Boolean booleanArg, Integer intArg); //(1)!

  @Query(value = "return ...", mode = QueryMode.EVAL)
  List<?> evalWithArgs(String stringArg, Boolean booleanArg, Integer intArg); //(2)!
}
```

1. The method calls the stored procedure `echo`, which can have the following form in lua:
   ```lua
    function echo(...) 
      return ...
    end
   ```
2. Executes the LUA script passed through `value`

More examples can be found in the tests.
