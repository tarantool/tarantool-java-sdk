---
title: Архитектура heartbeat
---

## Heartbeat

`heartbeat` - это фоновая задача, оправляющая постоянные ping-запросы к узлу Tarantool и
анализирующая результаты успешных и неуспешных ping-запросов. Задача `heartbeat` устанавливается для
каждого `PoolEntry` в [IProtoClientPool](./connection-pool.md). Главная задача `heartbeat` -
отслеживать доступность соединения к узлу `Tarantool`

### Принцип работы

Для определения доступности узла Tarantool используется подход скользящего окна. Представим, что
были заданы следующие настройки `heartbeat`:

- `invalidationThreshold == 2`
- `deathThreshold == 4`
- `windowSize == 3`

<figure id="heartbeat">
  <figcaption>Принцип работы heartbeat</figcaption>
  <img src="../../../../assets/drawio/client/heartbeat.drawio" alt="">
</figure>

Анализ доступности узла проводится с помощью ping-запросов. Если количество неудачных ping-запросов
в момент рассмотрения превышает значение `invalidationThreshold` в окне, то соединение исключается
из [пула соединений](./connection-pool.md) для выбора [балансировщиком](./balancer.md) (4,5,6). Для
исключенного соединения процесс `heartbeat`продолжается. Каждое превышение `invalidationThreshold` 
увеличивает значение счетчика `currentDeathThreshold` на 1. Если `currentDeathThreshold` достигает
значения `deathThreshold` соединение считается `мертвым` (статус `KILL`) и запускается процесс
переподключения соединения (7).

<figure id="heartbeat-1">
  <figcaption>Переход из INVALIDATE к ACTIVATE</figcaption>
  <img src="../../../../assets/drawio/client/heartbeat-1.drawio" alt="">
</figure>

Переход из состояния `INVALIDATE` в состояние `ACTIVATE` происходит если при последующих итерациях
анализа доступности количество неудачных ping-запросов меньше значения `invalidationThreshold`.
Счетчик `currentDeathThreshold` сбрасывается до значения `0` (см. <a href="#heartbeat-1">
диаграмму</a>)

Переход из состояния `KILL` в состояние `ACTIVATE` происходит если соединение было успешно
переподключено.

### Параметры heartbeat

<table>
    <tr align="center">
        <td>Имя параметра</td>
        <td>Описание</td>
        <td>Значение по умолчанию</td>
    </tr>
    <tr>
        <td align="center">pingInterval</td>
        <td>Время в миллисекундах, по истечении которого выполняется очередной ping-запрос</td>
        <td>3000</td>
    </tr>
    <tr>
        <td align="center">invalidationThreshold</td>
        <td>Количество неудачных ping-запросов в окне, при достижении которого <b>PoolEntry</b> 
            изымается из пула соединений (балансировщик перестает видеть это соединение в процессе 
            выбора). Выполнение ping-запросов по этому соединению продолжаются
        </td>
        <td>2</td>
    </tr>
    <tr>
        <td align="center">windowSize</td>
        <td>Размер скользящего окна (в количествах ping-запросов)</td>
        <td>4</td>
    </tr>
    <tr>
        <td align="center">deathThreshold</td>
        <td>Количество превышений invalidationThreshold, после которых соединение переводится в 
            состояние переподключения
        </td>
        <td>4</td>
    </tr>
    <tr>
        <td align="center">pingFunction</td>
        <td>Метод для выполнения проверочного запроса (ping-запрос)</td>
        <td>IProtoClient::ping</td>
    </tr>
</table>

### Настройка параметров

Для того, чтобы настроить параметры `heartbeat` воспользуйтесь `HeartbeatOpts` API:

```java
final HeartbeatOpts heartbeatOpts = HeartbeatOpts.getDefault()
    .withPingInterval(pingInterval)
    .withWindowSize(windowSize)
    .withDeathThreshold(deathThreshold)
    .withInvalidationThreshold(invalidationThreshold)
    .withPingFunction(pingFunction);

final TarantoolCrudClient crudClient = TarantoolFactory.crud()
    // ... other options
    .withHeartbeat(heartbeatOpts)
    .build();
```
