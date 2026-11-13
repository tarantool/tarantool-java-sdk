---
title: Выборка и постраничное чтение
---

## Как выбрать данные

Выборка из спейса кластера выполняется методом `select` интерфейса `TarantoolCrudSpace`. Клиент
вызывает на роутере функцию `crud.select`, передавая ей условия и опции выборки. Условие задаётся
объектом `Condition` (оператор, поле, значение); несколько условий объединяются по `AND`.

```java
TarantoolCrudSpace space = crudClient.space("person");

List<Tuple<List<?>>> persons = space.select(Condition.create(">", "id", 5)).join();
```

Каждый кортеж результата обёрнут в `Tuple`: `get()` возвращает его поля. Для кортежа в виде списка
это `List<?>`, для выборки с `Class<T>` — объект `T`.

### Варианты `select`

Все варианты возвращают `CompletableFuture` с результатом.

| Вариант                                                    | Результат              |
|------------------------------------------------------------|------------------------|
| `select(Condition...)`, `select(List<Condition>)`          | `List<Tuple<List<?>>>` |
| `select(List<Condition>, SelectOptions)`                   | `List<Tuple<List<?>>>` |
| `select(List<Condition>, Class<T>)`                        | `List<Tuple<T>>`       |
| `select(List<Condition>, SelectOptions, Class<T>)`         | `List<Tuple<T>>`       |
| `select(List<Condition>, TypeReference<T>)`                | `TarantoolResponse<T>` |
| `select(List<Condition>, SelectOptions, TypeReference<T>)` | `TarantoolResponse<T>` |
| `select(Options, TypeReference<T>, Object...)`             | `TarantoolResponse<T>` |

Варианты с условиями, но без `SelectOptions`, используют опции по умолчанию, то есть результат
`SelectOptions.builder().build()`. Последний вариант передаёт аргументы в `crud.select` как есть:
условия и опции выборки вы собираете сами, клиент ничего к ним не добавляет.

Как кортежи отображаются на классы, описано в разделе
[Маппинг данных](../../arch/tuple_pojo_mapping.md).

## Сколько кортежей возвращает выборка

### Лимит по умолчанию

`SelectOptions.builder()` сразу задаёт опции `first` значение `SelectOptions.DEFAULT_LIMIT`, равное
100. Поэтому выборка без явного `withFirst(...)` возвращает **не больше 100 кортежей**, даже если
подходящих больше. Это касается вариантов с условиями, но без `SelectOptions`, и вариантов с
опциями, собранными через `SelectOptions.builder()` без `withFirst(...)`. К варианту
`select(Options, TypeReference<T>, Object...)` это не относится: он передаёт аргументы как есть.

Признака того, что результат обрезан, нет: по одному списку из 100 кортежей нельзя понять, есть ли
ещё подходящие.

```java
// не больше 100 кортежей, даже если подходящих 1000
List<Tuple<List<?>>> persons = space.select(Condition.create(">", "id", 5)).join();
```

Тот же лимит действует в Spring Data для производных методов без `First`/`Top` в имени и без
параметра `Limit`, см. [Динамические запросы](../../../spring-data/derived-methods.md).

### Явный лимит

Если известно, сколько кортежей может подойти, задайте лимит с запасом через `withFirst(...)`:

```java
SelectOptions options = SelectOptions.builder().withFirst(500).build();

List<Tuple<List<?>>> persons =
    space.select(Collections.singletonList(Condition.create(">", "id", 5)), options).join();
```

Если верхнюю границу назвать нельзя, читайте постранично.

## Постраничное чтение

Опция `after` задаёт кортеж, после которого начинается выборка. Чтобы прочитать все подходящие
кортежи, запрашивайте страницы фиксированного размера (`withFirst(...)`) и передавайте в
`withAfter(...)` последний кортеж предыдущей страницы. Чтение заканчивается, когда страница пришла
короче заданного размера, в том числе пустой.

Для кортежей в виде списка в `withAfter(...)` передаётся список полей последнего кортежа, то есть
`get()` его `Tuple`:

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

При выборке с `Class<T>` в `withAfter(...)` передаётся последний объект предыдущей страницы:

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

Здесь `Person` — ваш класс, отображённый на кортежи спейса.

???+ warning "Это не снимок данных"

    Каждая страница — отдельный запрос, и между запросами данные могут измениться. Собранный
    постранично результат не является согласованным снимком спейса.
