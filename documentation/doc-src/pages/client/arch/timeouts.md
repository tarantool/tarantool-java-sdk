---
title: Тайм-ауты
---

У клиента два независимых тайм-аута: тайм-аут запроса ограничивает ожидание ответа на каждый
запрос, тайм-аут подключения — открытие соединения с узлом.

## Тайм-аут запроса

Каждый запрос клиента ограничен по времени: по умолчанию 5000 мс. Это значение
`BaseOptions.DEFAULT_TIMEOUT`; у классов опций операций (`SelectOptions`, `InsertOptions` и других)
есть своя константа `DEFAULT_TIMEOUT` с тем же значением.

Общей настройки тайм-аута запроса для всего клиента нет: ни `TarantoolCrudClientBuilder`, ни
`TarantoolBoxClientBuilder` её не предоставляют. Другой тайм-аут задаётся для каждого запроса
через `withTimeout(...)` в его опциях. Значение должно быть больше нуля, иначе билдер опций
выбросит `IllegalArgumentException`.

```java
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import io.tarantool.client.BaseOptions;
import io.tarantool.client.Options;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.mapping.Tuple;

public class Timeouts {

  // вызов хранимой процедуры с тайм-аутом 1 секунда
  public static void call(TarantoolCrudClient client) {
    Options options = BaseOptions.builder().withTimeout(1_000L).build();
    client.call("slow_function", Collections.emptyList(), options).join();
  }

  // выборка через crud с тайм-аутом 1 секунда
  public static List<Tuple<List<?>>> select(
      TarantoolCrudSpace space, List<Condition> conditions) {
    SelectOptions options = SelectOptions.builder().withTimeout(1_000L).withFirst(500).build();
    try {
      return space.select(conditions, options).join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof TimeoutException) {
        // ответ не пришёл за 1 секунду: повторите запрос или сообщите об ошибке
      }
      throw e;
    }
  }
}
```

Когда время истекает, `CompletableFuture` запроса завершается исключением
`java.util.concurrent.TimeoutException`. При `join()` оно приходит обёрнутым в
`CompletionException`, как в примере выше.

Отсчёт начинается, когда [балансировщик](balancer.md) выдал запросу соединение. Время на открытие
соединения в тайм-аут запроса не входит.

У `SelectOptions` есть ещё `withCrudTimeout(...)`. Это другая настройка: она передаёт значение в
опцию `timeout` функции `crud.select` на сервере и тайм-аут ожидания ответа клиентом не меняет.

## Тайм-аут подключения

Тайм-аут подключения задаётся в билдере клиента через `withConnectTimeout(...)`, в Spring Data —
свойством `spring.data.tarantool.connect-timeout`. По умолчанию — 3000 мс
(`TarantoolClient.DEFAULT_CONNECTION_TIMEOUT`). Отсчёт идёт от начала подключения до получения
приветствия от узла.

Установку TCP-соединения отдельно ограничивает опция канала Netty `CONNECT_TIMEOUT_MILLIS`: в
опциях канала по умолчанию (`TarantoolClient.DEFAULT_NETTY_CHANNEL_OPTIONS`) она равна 3000 мс.
`withConnectTimeout(...)` и `connect-timeout` её не меняют; она задаётся в билдере через
`withChannelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, ...)`, а в Spring Data — только в своём
бине билдера. Поэтому по коду SDK `withConnectTimeout(...)` больше 3000 мс не продлевает саму
установку TCP-соединения (не измерено).

После приветствия клиент аутентифицируется отдельным запросом с тайм-аутом 5000 мс. Если
соединение открывается от имени `guest` (в группе соединений не задан пользователь или задан
`guest`), аутентификации нет: вместо неё клиент отправляет один ping-запрос с тайм-аутом 1000 мс.

Соединения CRUD-клиента открываются не при его создании, а при первом обращении к ним (см.
[пул соединений](connection-pool.md); там же — исключение для Box-клиента). Поэтому первый запрос через ещё не открытое соединение
сначала ждёт подключения и аутентификации и только потом выполняется сам. Чтобы первые запросы
не ждали, прогрейте пул при старте приложения (раздел «Прогрев пула при старте» на странице
[Пул соединений](connection-pool.md)).

## Spring Data

Из тайм-аутов в свойствах `spring.data.tarantool.*` есть только `connect-timeout`. Свойства для
тайм-аута запроса нет: каждый запрос, который отправляет метод репозитория, выполняется с
тайм-аутом 5000 мс. Если для операции нужен другой тайм-аут, выполните её через API
`TarantoolCrudClient` с опциями, как показано выше.
