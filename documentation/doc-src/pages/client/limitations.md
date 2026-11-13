---
title: Ограничения и способы обхода
---

## Чего нет и как обойти

В модуле `crud` на стороне Tarantool нет пакетного удаления (`delete_many`) и условия `IN`:
условия одного `select` поддерживают только `==`, `<`, `<=`, `>`, `>=` и объединяются по `AND`.
Клиент это отражает: в `TarantoolCrudSpace` есть `insertMany`, `replaceMany`, `upsertMany`, но
нет `deleteMany`.

Способы обхода ниже отправляют несколько запросов сразу. Запросы одного клиента идут конвейером
по его соединениям и не ждут друг друга.

???+ warning "Важно"

    Ни один из способов не атомарен: каждый ключ — отдельная операция, а ключи могут лежать на
    разных наборах реплик. Часть запросов может пройти, часть — завершиться ошибкой, поэтому
    проверяйте результат каждого запроса. Скорость способов друг относительно друга не измерялась.

### Удалить несколько записей по первичному ключу

Отправьте `delete` по каждому ключу и проверьте каждый результат. Метод ниже возвращает ключи,
запрос по которым завершился исключением:

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

Два случая требуют внимания:

- если в ответе нет кортежа, future по коду SDK завершается значением `null`, а не исключением, и
  такой ключ в список не попадает. Чтобы узнать, был ли удалён кортеж, проверяйте результат
  `join()` на `null`;
- при тайм-ауте исход операции не определён: по коду SDK тайм-аут завершает future на стороне
  клиента, но не отменяет запрос на сервере, и запись могла быть удалена.

В Spring Data `deleteAllById(ids)` отправляет по запросу на ключ и ждёт ответа на каждый, прежде
чем отправить следующий.

Другой путь — хранимая процедура на роутере, которая удаляет ключи в цикле, и её вызов через
`client.call(...)`: от приложения уходит один сетевой запрос (тоже не атомарно).

### Выборка `col IN (v1, v2, v3)`

Отправьте параллельно по одному `select` с условием `==` на каждое значение и объедините
результаты:

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

В `options` задайте `withFirst(...)` с запасом на одно значение: без него каждый `select`
вернёт не больше 100 кортежей на значение. Если верхнюю границу назвать нельзя, читайте каждое
значение постранично (см. [Выборка и постраничное чтение](examples/select/select.md)).

Если `col` — ключ шардирования, каждый запрос идёт в один набор реплик; иначе каждый `select`
обходит все хранилища. Для первичного ключа используйте `space.get(key)` по каждому ключу
параллельно. Ещё вариант — хранимая процедура, которая принимает список значений и выполняет
выборки на стороне сервера.

### Производные методы Spring Data

Поддерживаемые ключевые слова перечислены в разделе
[Динамические запросы](../spring-data/derived-methods.md). Ключевые слова `In`, `NotIn`, `Like`
не поддерживаются: такой метод завершается ошибкой
`InvalidDataAccessApiUsageException: Unsupported type ...`. Метод с `And` или `Or` завершается
`UnsupportedOperationException`. Используйте способы выше или
[хранимые процедуры](../spring-data/query-lua.md).
