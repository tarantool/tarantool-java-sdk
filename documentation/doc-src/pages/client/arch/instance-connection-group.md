---
title: Группа соединений
---

## InstanceConnectionGroup

`InstanceConnectionGroup` описывает группу соединений с одним узлом Tarantool: адрес узла,
учётные данные, число соединений и другие параметры.

### Группы соединений

<figure markdown="span" id="instance-connection-group-img-anchor">
<figcaption>Место <b>InstanceConnectionGroup</b> в архитектуре Java-клиента</figcaption>
![](../../../assets/drawio/client/instance-connection-group.drawio)
</figure>

Высокоуровневые клиенты `TarantoolCrudClient` и `TarantoolBoxClient` работают с узлами Tarantool
через группы соединений. Каждая группа — набор соединений (`IProtoClient`) с одним узлом.

При создании клиента [пул соединений](connection-pool.md) `IProtoClientPool` создаёт по одной группе
на каждый переданный объект `InstanceConnectionGroup`.

Несколько соединений в одной группе позволяют распределять параллельные запросы к узлу по разным
соединениям; может ли это повысить производительность клиента, зависит от нагрузки (не измерено). Какое соединение получит запрос, решает
[балансировщик](balancer.md).

### Параметры

| Параметр                    | Назначение                                                        | По умолчанию                |
|-----------------------------|-------------------------------------------------------------------|-----------------------------|
| `host`                      | Имя или IP-адрес узла                                             | `localhost`                 |
| `port`                      | Порт, на котором узел принимает соединения                        | `3301`                      |
| `user`                      | Пользователь для аутентификации на узле                           | `guest`                     |
| `password`                  | Пароль пользователя `user`                                        | не задан                    |
| `size`                      | Число соединений с узлом (Netty-соединений, см. схему выше)       | `1`                         |
| `tag`                       | Имя группы: по нему группа различается в пуле и в логах           | `<user>:<host>:<port>`      |
| `authType`                  | Алгоритм аутентификации, см. ниже                                 | `AuthType.CHAP_SHA1`        |
| `flushConsolidationHandler` | [FlushConsolidationHandler](https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html) Netty для соединений группы | не задан |

`host` и `port` задаются так же, как для
[InetSocketAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetSocketAddress.html).

Пользователь `guest` входит без пароля: если `user` не задан или равен `guest`, пароль задавать
нельзя, иначе `build()` выбросит `IllegalArgumentException`.

`authType` должен соответствовать способу, которым на узле хранится пароль пользователя
(подробнее — Javadoc класса `AuthType` и
[документация Tarantool](https://www.tarantool.io/en/doc/latest/reference/configuration/configuration_reference/#confval-security.auth_type)).

### Разрешение имени узла

Имя узла из `host` разрешается в IP-адрес один раз — при создании группы. Все подключения и
переподключения группы идут на этот адрес. Если IP-адрес узла меняется (контейнер или ВМ
пересоздаётся, меняется запись DNS), клиент продолжит подключаться к старому адресу; чтобы
подключиться к новому, пересоздайте клиент.

### Использование

Группа создаётся через `InstanceConnectionGroup.Builder` и передаётся в билдер клиента
(`TarantoolCrudClientBuilder` или `TarantoolBoxClientBuilder`):

```java
import java.util.Collections;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.core.protocol.requests.IProtoAuth.AuthType;
import io.tarantool.pool.InstanceConnectionGroup;

public class ConnectionGroupConfig {

  public static TarantoolCrudClient createClient() throws Exception {
    InstanceConnectionGroup connectionGroup =
        InstanceConnectionGroup.builder()
            .withHost("localhost")
            .withPort(3301)
            .withUser("user2581-test")
            .withPassword("pwd-1")
            .withSize(3)
            .withTag("Node-1")
            .withAuthType(AuthType.CHAP_SHA1)
            .build();

    return TarantoolFactory.crud()
        .withGroups(Collections.singletonList(connectionGroup))
        .build();
  }
}
```

Если группы не переданы, `TarantoolCrudClientBuilder` создаёт одну группу из своих `withHost(...)`,
`withPort(...)`, `withUser(...)` и `withPassword(...)` с одним соединением и тегом `default`. Если
группы переданы, эти настройки билдера не используются.

Пользователь по умолчанию у `TarantoolCrudClientBuilder` — `api_user` с паролем `secret`, а не
`guest`. Пароль `secret` остаётся и после `withUser("guest")`, а `guest` с паролем не допускается,
поэтому для входа гостем без своих групп сбросьте пароль явно:
`withUser("guest").withPassword(null)`.
