---
title: Динамические запросы
---

## Пользовательские динамические запросы

Для того чтобы воспользоваться динамическими запросами, определите необходимый метод в
пользовательском `Repository`. Например:

```java
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  List<Person> findByName(String name);

  List<Person> findPersonByNameAfter(String name);
}
```

???+ note "Заметка"

    Больше информации о динамических запросах в
    [`официальной документации`](https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html)

???+ note "Заметка"

    Доступны следующие методы и ключевые слова:

    - findBy
    - deleteBy
    - existsBy
    - countBy

    Ключевые слова:

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

???+ warning "Важно"

    На данный момент поддерживаются только методы с одним предикатом (не поддерживаются `AND` или 
    `OR`).

???+ note "Заметка"

    В версию tarantool-spring-data32 добавлена возможность использовать в производных методах
    интерфейс [Limit](https://docs.spring.io/spring-data/data-commons/docs/3.2.3/api/org/springframework/data/domain/Limit.html)

    **Важно**:
    
    - Максимальное количество ожидаемых результатов должно быть **положительным числом**, в ином 
      случае будет выброшено исключение.
    - Приоритетность ограничения количества записей в результате:
        - `Limit.of(...)` приоритетнее статического ограничения в названии метода 
          (ключевые слова Top/First).
    - Статическое ограничение в названиях методов приоритетнее `Limit.unlimited()` или передачи 
      `null` в качестве `Limit`.
    
    Примеры:
    
    ```java
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, Limit.of(10)); // вернет <= 10 записей
    // ...
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, Limit.unlimited()); // вернет <= 100 записей
    // ...
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, null); // вернет <= 100 записей
    // ...
    List<Person> result = repository.findTop4ByIdGreaterThanEqual(0, Limit.of(10)); // вернет <= 10 записей
    // ...
    List<Person> result = repository.findTop4ByIdGreaterThanEqual(0, Limit.unlimited()); // вернет <= 4 записей
    // ...
    List<Person> result = repository.findTop4ByIdGreaterThanEqual(0, null); // вернет <= 4 записей
    // ...
    List<Person> result = repository.findPersonByIdGreaterThanEqual(0, Limit.of(-10)); // выбросит IllegalArgumentException
    ```

Больше примеров можно найти в тестах.
