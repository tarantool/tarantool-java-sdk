---
title: Балансировка
---

## Балансировщик

Балансировщик — компонент клиента, который распределяет запросы между соединениями с узлами
Tarantool. В `Tarantool Java SDK` он представлен интерфейсом `TarantoolBalancer`.

### Как выполняется запрос

<figure markdown="span" id="balancer">
<figcaption>Работа балансировщика</figcaption>
![](../../../assets/drawio/client/balancer.drawio)
</figure>

1. Приложение выполняет запрос через API высокоуровневого клиента (`TarantoolClient`,
   `TarantoolCrudClient` или `TarantoolBoxClient`).
2. Клиент запрашивает у `TarantoolBalancer` соединение для этого запроса.
3. Балансировщик по своему алгоритму выбирает соединение в [пуле соединений](connection-pool.md)
   (`IProtoClientPool`).
4. Пул возвращает балансировщику доступное соединение.
5. Балансировщик передаёт соединение клиенту.
6. Клиент отправляет запрос через это соединение.
7. Клиент возвращает вызывающему коду `CompletableFuture` с результатом запроса.

### Доступные и заблокированные соединения

Соединение открывается, когда балансировщик выбирает его впервые. Если выбранное соединение
недоступно, балансировщик переходит к следующему. Если доступных соединений нет совсем,
`CompletableFuture` запроса завершается исключением `NoAvailableClientsException`.

Недоступным (заблокированным) соединение становится в трёх случаях:

- подключение по нему не удалось;
- узел закрыл соединение;
- [heartbeat](heartbeat.md) перевёл соединение в состояние `INVALIDATE`.

Блокировка снимается, когда соединение снова успешно подключилось или heartbeat перевёл его в
состояние `ACTIVATE`. Без настроенного heartbeat остаются только первые два случая.

По коду SDK долгие запросы соединение не блокируют: по одному соединению одновременно выполняется
много запросов.

## Реализации

В клиенте есть две реализации `TarantoolBalancer`.

### TarantoolRoundRobinBalancer

<figure markdown="span" id="round-robin-balancer">
<figcaption>Работа TarantoolRoundRobinBalancer</figcaption>
![](../../../assets/drawio/client/round-robin.drawio)
</figure>

Перебирает соединения группы за группой:

1. Выбирает группу соединений, с которой начинается перебор (порядок групп не определён).
2. Выбирает соединения этой группы по порядку, с первого до последнего.
3. После последнего соединения группы переходит к следующей группе.

### TarantoolDistributingRoundRobinBalancer

<figure markdown="span" id="distributing-round-robin-balancer">
<figcaption>Работа TarantoolDistributingRoundRobinBalancer</figcaption>
![](../../../assets/drawio/client/distributing-round-robin.drawio)
</figure>

Чередует группы, так что соседние запросы уходят в разные группы:

1. Выбирает группу соединений, с которой начинается перебор (порядок групп не определён).
2. Выбирает первое соединение этой группы.
3. Переходит к следующей группе и выбирает в ней первое соединение.
4. Обойдя все группы, повторяет шаг 3, выбирая в каждой группе следующее соединение.

## Выбор балансировщика

Класс балансировщика задаётся в билдере клиента:

```java
import io.tarantool.balancer.TarantoolRoundRobinBalancer;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;

public class BalancerConfig {

  public static TarantoolCrudClient createClient() throws Exception {
    return TarantoolFactory.crud()
        .withBalancerClass(TarantoolRoundRobinBalancer.class)
        // ... прочие настройки
        .build();
  }
}
```

В Spring Data балансировщик выбирается свойством `spring.data.tarantool.balancer-mode` (см.
[Конфигурация клиентов](../../spring-data/configuration.md)).

## Свой балансировщик

Чтобы использовать свой алгоритм, создайте класс, реализующий интерфейс `TarantoolBalancer`, и
передайте его в `withBalancerClass(...)`. Клиент создаёт балансировщик через публичный конструктор
с сигнатурой `public <ИмяКласса>(IProtoClientPool pool)`, поэтому такой конструктор у класса
обязателен.
