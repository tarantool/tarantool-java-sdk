---
title: Ограничения и способы обхода
---

## Чего нет и как обойти

В модуле `crud` на стороне Tarantool нет пакетного удаления (`delete_many`) и условия `IN`:
условия одного `select` поддерживают только `==`, `<`, `<=`, `>`, `>=` и объединяются по `AND`.
Клиент это отражает: в `TarantoolCrudSpace` есть `insertMany`, `replaceMany`, `upsertMany`, но
нет `deleteMany`. Обходные пути ниже отправляют несколько запросов сразу; запросы одного клиента идут
конвейером по его соединениям и не ждут друг друга.

???+ warning "Важно"

    Ни один из способов не атомарен: каждый ключ — отдельная операция, ключи могут лежать на разных
    наборах реплик. Часть запросов может пройти, часть упасть; проверяйте каждый future.
    Скорость способов друг относительно друга не измерялась.

### Удалить несколько записей по первичному ключу

```java
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.tarantool.client.crud.TarantoolCrudSpace;

CompletableFuture<?>[] futures =
    keys.stream().map(space::delete).toArray(CompletableFuture[]::new);
CompletableFuture.allOf(futures).get();
```

В Spring Data `deleteAllById(ids)` отправляет по запросу на ключ и ждёт ответа на каждый, прежде
чем отправить следующий.

Другой путь: хранимая процедура на роутере, которая удаляет ключи в цикле, и вызов через
`client.call(...)` — один сетевой запрос от приложения (тоже не атомарно).

### Выборка `col IN (v1, v2, v3)`

Отправьте параллельно по одному `select` с условием `==` на каждое значение и склейте результат:

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

Если `col` — ключ шардирования, каждый запрос идёт в один набор реплик; иначе каждый `select` обходит
все хранилища. Для первичного ключа — `space.get(key)` по каждому ключу параллельно. Ещё вариант —
хранимая процедура, принимающая список значений и выполняющая выборки на стороне сервера.

### Производные методы Spring Data

Поддерживаемые ключевые слова перечислены в [Динамические запросы](../spring-data/derived-methods.md).
Ключевые слова `In`, `NotIn`, `Like` не поддерживаются: такой метод завершается ошибкой
`InvalidDataAccessApiUsageException: Unsupported type ...`; метод с `And` или `Or` завершается
`UnsupportedOperationException`. Используйте подходы выше или
[хранимые процедуры](../spring-data/query-lua.md).
