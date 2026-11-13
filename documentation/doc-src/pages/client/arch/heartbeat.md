---
title: Архитектура heartbeat
---

## Heartbeat

Heartbeat — фоновая задача, которая регулярно отправляет ping-запросы узлу Tarantool и по их
результатам решает, доступно ли соединение. Задача запускается для каждого `PoolEntry`, то есть
для каждого соединения [пула](connection-pool.md). Соединение, которое heartbeat признал
недоступным, [балансировщик](balancer.md) не выбирает для запросов.

### Включение

По умолчанию heartbeat выключен. Он работает, только если в билдере клиента задан
`withHeartbeat(...)`, а в Spring Data — раздел `spring.data.tarantool.heartbeat`. Без heartbeat
периодических ping-запросов нет, и соединение блокируется, только когда подключение не удалось или
узел закрыл соединение. По коду SDK тайм-ауты запросов соединение не блокируют, так что узел,
который перестал отвечать, но соединение не закрыл, без heartbeat из выбора не исключается.

### Принцип работы

Доступность узла оценивается по скользящему окну из последних ping-запросов. Рассмотрим пример с
настройками:

- `invalidationThreshold == 2`
- `deathThreshold == 3`
- `windowSize == 3`

<figure markdown="span" id="heartbeat">
<figcaption>Принцип работы heartbeat</figcaption>
![](../../../assets/drawio/client/heartbeat.drawio)
</figure>

Проверка выполняется после каждого ping-запроса, как только окно заполнено. Если число неудачных
ping-запросов в окне больше или равно `invalidationThreshold`, соединение переходит в состояние
`INVALIDATE`: оно остаётся в пуле, но балансировщик его больше не выбирает (шаги 4, 5, 6 на
схеме). Ping-запросы по такому соединению продолжаются.

Каждая проверка, на которой порог `invalidationThreshold` достигнут, увеличивает счётчик
`currentDeathPings` на 1. Когда счётчик становится больше `deathThreshold`, соединение считается
мёртвым (состояние `KILL`), и клиент начинает его переподключение. На схеме проверки 4, 5, 6 и 7
дают счётчику значения 1, 2, 3 и 4; на шаге 7 счётчик превышает `deathThreshold == 3`, и соединение
переходит в `KILL`.

<figure markdown="span" id="heartbeat-1">
<figcaption>Переход из INVALIDATE в ACTIVATE</figcaption>
![](../../../assets/drawio/client/heartbeat-1.drawio)
</figure>

Если на очередной проверке неудачных ping-запросов в окне меньше `invalidationThreshold`,
соединение возвращается в состояние `ACTIVATE`, и счётчик `currentDeathPings` сбрасывается в 0
(см. <a href="#heartbeat-1">схему</a>). Поэтому до `KILL` доводят только проверки с достигнутым
порогом, идущие подряд.

Из состояния `KILL` соединение возвращается в `ACTIVATE` после успешного переподключения.

С параметрами по умолчанию (окно из 4 ping-запросов, `invalidationThreshold` 2, `deathThreshold` 4)
соединение исключается из выбора, когда неудачны хотя бы 2 из 4 последних ping-запросов, и
переподключается на пятой такой проверке подряд.

### Параметры

| Параметр                | Назначение                                                        | По умолчанию         |
|-------------------------|-------------------------------------------------------------------|----------------------|
| `pingInterval`          | Интервал между ping-запросами и тайм-аут каждого ping-запроса, мс | 3000                 |
| `windowSize`            | Размер скользящего окна, в ping-запросах                          | 4                    |
| `invalidationThreshold` | Число неудачных ping-запросов в окне, при котором соединение исключается из выбора | 2 |
| `deathThreshold`        | Сколько проверок подряд с достигнутым порогом допускается до переподключения | 4    |
| `pingFunction`          | Функция проверочного запроса                                      | `IProtoClient::ping` |

### Настройка

Параметры задаются через `HeartbeatOpts`. `HeartbeatOpts.getDefault()` возвращает параметры по
умолчанию; методы `with...` меняют отдельные значения.

```java
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.pool.HeartbeatOpts;

public class HeartbeatConfig {

  public static TarantoolCrudClient createClient() throws Exception {
    HeartbeatOpts heartbeatOpts =
        HeartbeatOpts.getDefault()
            .withPingInterval(1_000L)
            .withWindowSize(4)
            .withInvalidationThreshold(2)
            .withDeathThreshold(4);

    return TarantoolFactory.crud()
        // ... прочие настройки
        .withHeartbeat(heartbeatOpts)
        .build();
  }
}
```

`pingInterval` служит и тайм-аутом ping-запроса: в примере выше ping, не получивший ответа за
1 секунду, считается неудачным. Свою функцию проверки можно задать через `withPingFunction(...)`.

В Spring Data те же параметры задаются в разделе `spring.data.tarantool.heartbeat`:

```yaml
spring:
  data:
    tarantool:
      heartbeat:
        ping-interval: 1000
        window-size: 4
        invalidation-threshold: 2
        death-threshold: 4
```
