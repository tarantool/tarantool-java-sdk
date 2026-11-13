---
title: Динамические запросы
---

## Производные методы

Производный (динамический) запрос — метод репозитория, запрос для которого Spring Data строит по
имени метода. Чтобы им воспользоваться, объявите метод в своём интерфейсе `Repository`:

```java
import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface PersonRepository extends PagingAndSortingRepository<Person, Integer> {

  List<Person> findByName(String name);

  List<Person> findPersonByNameAfter(String name);
}
```

Здесь `Person` — класс-сущность спейса. Общие правила составления имён описаны в
[документации Spring Data](https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html).

### Поддерживаемые методы и ключевые слова

Методы:

- `findBy`
- `deleteBy`
- `existsBy`
- `countBy`

Ключевые слова:

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

Метод может содержать только один предикат: `And` и `Or` не поддерживаются, такой метод
завершается `UnsupportedOperationException`. Любое ключевое слово вне списка выше (например, `In`,
`NotIn`, `Like`) приводит к `InvalidDataAccessApiUsageException: Unsupported type ...`. Способы
обхода описаны в разделе [Ограничения и способы обхода](../client/limitations.md).

### Сколько записей возвращает метод

Если в имени метода нет `First`/`Top` и не передан параметр `Limit`, запрос выполняется с лимитом
`SelectOptions.DEFAULT_LIMIT`, равным 100. Признака того, что результат обрезан, нет.

- `findBy...`, возвращающий список, поток или одну запись, возвращает **не больше 100 записей**.
- Методы с `Pageable` и `Scroll` возвращают не больше размера страницы. Для `Page` запрос
  выполняется с лимитом, равным размеру страницы; для `Slice` и `Scroll` SDK запрашивает на одну
  запись больше, чтобы узнать, есть ли следующая страница.
- `deleteBy...` удаляет **не больше 100 записей**: метод сначала выбирает записи с этим лимитом,
  затем удаляет найденные по одной. Остальные подходящие записи остаются в спейсе.
- `countBy...` считает все подходящие записи. Исключение — `countDistinctBy...`: он тоже ограничен
  100 записями.

Чтобы прочитать больше записей, передайте `Limit.of(...)` (см. ниже) или читайте постранично через
[Page, Slice](page-slice-pageable.md) или [Scroll API](scroll-api.md).

`findAll()` и `findAll(Sort)` не поддерживаются и завершаются `UnsupportedOperationException`. Все
записи спейса читайте постранично через `findAll(Pageable)`.

Так же ограничена выборка в клиенте без Spring Data, см.
[Выборка и постраничное чтение](../client/examples/select/select.md).

### Параметр `Limit`

Начиная с модуля `tarantool-spring-data-32`, в производные методы можно передавать параметр
[Limit](https://docs.spring.io/spring-data/data-commons/docs/3.2.3/api/org/springframework/data/domain/Limit.html).

- Отрицательное значение `Limit.of(...)` приводит к `IllegalArgumentException`: SDK проверяет
  только условие «меньше нуля», значение 0 эту проверку проходит.
- `Limit.of(...)` важнее ограничения в имени метода (`Top`/`First`).
- Ограничение в имени метода важнее `Limit.unlimited()` и `null` вместо `Limit`.
- Без ограничения в имени `Limit.unlimited()` и `null` дают лимит по умолчанию — 100 записей.

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
List<Person> a = repository.findPersonByIdGreaterThanEqual(0, Limit.of(10)); // <= 10 записей
List<Person> b = repository.findPersonByIdGreaterThanEqual(0, Limit.unlimited()); // <= 100 записей
List<Person> c = repository.findPersonByIdGreaterThanEqual(0, null); // <= 100 записей
List<Person> d = repository.findTop4ByIdGreaterThanEqual(0, Limit.of(10)); // <= 10 записей
List<Person> e = repository.findTop4ByIdGreaterThanEqual(0, Limit.unlimited()); // <= 4 записи
List<Person> f = repository.findTop4ByIdGreaterThanEqual(0, null); // <= 4 записи
repository.findPersonByIdGreaterThanEqual(0, Limit.of(-10)); // IllegalArgumentException
```

Больше примеров — в тестах модулей `tarantool-spring-data`.
