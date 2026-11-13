---
title: Пул соединений
---

## Пул соединений (IProtoClientPool)

Пул соединений `IProtoClientPool` — набор соединений с узлами Tarantool, объединённых в логические
группы. Группа описывается объектом [InstanceConnectionGroup](instance-connection-group.md): к
какому узлу подключаться, сколько соединений открыть и с какими учётными данными.

При создании `TarantoolCrudClient` или `TarantoolBoxClient` создаётся один экземпляр
`IProtoClientPool`. По переданным `InstanceConnectionGroup` пул создаёт объекты `PoolEntry`, по
одному на соединение. `PoolEntry` управляет жизненным циклом своего соединения: подключением,
отключением, переподключением и [heartbeat](heartbeat.md). Каждый `PoolEntry` относится к одной
группе, и все `PoolEntry` группы держат собственные соединения с одним и тем же узлом.

<figure markdown="span" id="connection-pool-1">
<figcaption>Место <b>IProtoClientPool</b> в архитектуре Java-клиента</figcaption>
![](../../../assets/drawio/client/connection-pool.drawio)
</figure>

Сам пул запросы не распределяет: он хранит соединения и выдаёт [балансировщику](balancer.md)
доступные из них. Какое соединение получит запрос, решает балансировщик.

### Когда открываются соединения

Соединение открывается, когда его впервые запрашивают у пула, то есть когда балансировщик впервые
выбирает его для запроса. Создание CRUD-клиента соединений не открывает, поэтому первые запросы
после старта приложения ждут подключения к узлу и аутентификации (см. [Тайм-ауты](timeouts.md)).

Box-клиент по умолчанию загружает схему спейсов (`TarantoolBoxClient.DEFAULT_FETCH_SCHEMA`): уже при
создании он отправляет запросы схемы через балансировщик и ждёт ответа, поэтому соединения для этих
запросов открываются в `build()`. Загрузку схемы отключает `withFetchSchema(false)` в
`TarantoolBoxClientBuilder`.

### Прогрев пула при старте

Настройки «открыть все соединения при создании клиента» нет. Чтобы первые запросы не ждали
подключения, откройте соединения сами через публичный API пула: обойдите все группы и все
соединения внутри группы и дождитесь подключения.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.tarantool.client.TarantoolClient;
import io.tarantool.pool.IProtoClientPool;

public class PoolWarmUp {

  public static void warmUp(TarantoolClient client) {
    IProtoClientPool pool = client.getPool();
    List<CompletableFuture<?>> connects = new ArrayList<>();
    for (String tag : pool.getTags()) {
      for (int i = 0; i < pool.getGroupSize(tag); i++) {
        CompletableFuture<?> connect = pool.get(tag, i);
        if (connect != null) {
          connects.add(connect);
        }
      }
    }
    CompletableFuture.allOf(connects.toArray(new CompletableFuture[0])).join();
  }
}
```

`pool.get(tag, index)` возвращает `CompletableFuture` с соединением и при первом вызове начинает
подключение. Для заблокированного соединения (см. [балансировщик](balancer.md)) метод возвращает
`null`, поэтому такие соединения в примере пропускаются.

Если подключение не удалось, future этого соединения завершается исключением, и тогда `join()` в
примере выбросит `CompletionException`: так работает `CompletableFuture.allOf`. Если неудачный
прогрев не должен останавливать старт приложения, перехватите это исключение и запишите его в лог.
