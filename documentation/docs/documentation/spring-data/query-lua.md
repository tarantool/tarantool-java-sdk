---
title: Хранимые процедуры
---

## Работа с хранимыми процедурами и LUA скриптами

Для работы с LUA или хранимыми процедурами через `Repository` объявите пользовательский метод (
название задается по своему усмотрению) и добавьте аннотацию `@Query`, выбрав в параметрах
необходимый режим:

```java
public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  @Query("echo")
  List<?> echo(String stringArg, Boolean booleanArg, Integer intArg); //(1)!

  @Query(value = "return ...", mode = QueryMode.EVAL)
  List<?> evalWithArgs(String stringArg, Boolean booleanArg, Integer intArg); //(2)!
}
```

1. Метод вызывает хранимую процедуру `echo`, которая может иметь следующий вид в lua:
   ```lua
    function echo(...) 
      return ...
    end
   ```
2. Производит выполнение LUA-скрипта, переданного через `value`

Больше примеров можно найти в тестах.
