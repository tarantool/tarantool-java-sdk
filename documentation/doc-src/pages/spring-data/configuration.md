---
title: Конфигурация клиентов
---

## Конфигурирование CRUD-клиента

`TarantoolCrudClient` для Spring Data Tarantool можно настроить тремя способами:

- Java-классом конфигурации с `@EnableTarantoolRepositories` и своим бином
  `TarantoolCrudClientBuilder`;
- файлом конфигурации (`application.properties` или `application.yaml`);
- Java-классом конфигурации без `@EnableTarantoolRepositories` — тогда репозитории не создаются,
  и с кластером работают только через API `TarantoolCrudClient`.

Примеры ниже написаны для модуля `tarantool-spring-data-40`. В других модулях аннотация
`@EnableTarantoolRepositories` и класс `TarantoolProperties` лежат в пакетах своего модуля:
например, для `tarantool-spring-data-35` — `io.tarantool.spring.data35.repository.config` и
`io.tarantool.spring.data35.config.properties`. Какой модуль нужен вашему приложению, показано в
[таблице модулей](index.md).

### Что происходит при старте контекста

При подъёме контекста Spring с `@EnableTarantoolRepositories`:

- регистрируется бин `TarantoolProperties` со свойствами `spring.data.tarantool.*`;
- создаётся клиент `TarantoolCrudClient` и регистрируется в контексте как бин. Если в контексте
  есть свой бин `TarantoolCrudClientBuilder`, клиент собирается из него как есть: свойства
  `spring.data.tarantool.*` к нему не применяются. Если такого бина нет, билдер собирается из
  свойств;
- регистрируются пользовательские [репозитории](repository.md).

Соединения с кластером при этом не открываются: они открываются при первом запросе (см.
[пул соединений](../client/arch/connection-pool.md)).

### Настройка через Java-класс

Создайте класс с аннотациями `@Configuration` и `@EnableTarantoolRepositories` и объявите в нём
бин `TarantoolCrudClientBuilder` со всеми нужными настройками:

```java
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.spring.data40.repository.config.EnableTarantoolRepositories;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableTarantoolRepositories
public class Config {

  @Bean
  public TarantoolCrudClientBuilder clientSettings() {
    return TarantoolFactory.crud()
        .withHost("localhost")
        .withPort(3301)
        .withPassword("secret-cluster-cookie");
  }
}
```

Полный список настроек и их назначение — в Javadoc класса `TarantoolCrudClientBuilder`.

### Настройка через файл конфигурации

Чтобы настроить клиент файлом конфигурации, оставьте в классе конфигурации только аннотации и
задайте свойства `spring.data.tarantool.*`:

=== "Класс конфигурации"

    ```java
    import io.tarantool.spring.data40.repository.config.EnableTarantoolRepositories;

    import org.springframework.context.annotation.Configuration;

    @Configuration
    @EnableTarantoolRepositories
    public class Config {}
    ```

=== "application.yaml"

    ```yaml
    spring:
        data:
            tarantool:
                host: localhost #(1)!
                port: 3301 #(2)!
                user-name: user #(3)!
                password: password #(4)!
                graceful-shutdown-enabled: true #(5)!
                balancer-mode: distributing_round_robin #(6)!
                connect-timeout: 5000 #(7)!
                reconnect-after: 1000 #(8)!
                connection-groups: #(9)!
                    - auth-type: chap_sha1 #(10)!
                      host: localhost #(11)!
                      port: 3301 #(12)!
                      user-name: "user" #(13)!
                      password: secret #(14)!
                      connection-group-size: 4 #(15)!
                      tag: first-router #(16)!
                      flush-consolidation-handler: #(17)!
                          explicit-flush-after-flushes: 256
                          consolidate-when-no-read-in-progress: false
                    - host: localhost
                      port: 3302
                      user-name: "user"
                      password: secret
                      connection-group-size: 2
                      tag: second-router
                event-loop-threads-count: 10 #(18)!
                heartbeat: #(19)!
                  death-threshold: 4
                  invalidation-threshold: 2
                  ping-interval: 56
                  window-size: 12
    ```

    1. Адрес узла, к которому подключается клиент
    2. Порт узла `host`
    3. Имя пользователя для подключения к Tarantool
    4. Пароль пользователя
    5. Использовать ли протокол `graceful shutdown`
    6. Тип [балансировщика](../client/arch/balancer.md)
    7. Тайм-аут подключения к Tarantool в мс (`connect-timeout: 5000` — 5 секунд). По умолчанию —
       3000 мс. Свойства для тайм-аута запроса нет, см. [Тайм-ауты](../client/arch/timeouts.md)
    8. Через сколько миллисекунд повторить подключение после неудачного
       (`reconnect-after: 1000` — 1 секунда)
    9. Список групп соединений, см. [Группа соединений](../client/arch/instance-connection-group.md)
    10. Тип протокола аутентификации. Должен совпадать со способом, которым хранится пароль
        пользователя, то есть с `security.auth_type` (`box.cfg.auth_type`) кластера; иначе вход
        завершится ошибкой `User not found or supplied credentials are invalid`. По умолчанию
        `chap-sha1` — это же умолчание клиента, так что параметр можно не указывать. `pap-sha256`
        доступен только в Enterprise Edition (и TDB) и только по шифрованному (TLS) соединению
    11. Адрес узла группы
    12. Порт узла группы
    13. Имя пользователя группы
    14. Пароль пользователя группы
    15. Число соединений в группе
    16. Тег — имя группы
    17. Параметры `FlushConsolidationHandler` Netty
    18. Число потоков Netty
    19. Параметры [heartbeat](../client/arch/heartbeat.md)

    Если задан хотя бы один элемент `connection-groups`, свойства верхнего уровня `host`, `port`,
    `user-name` и `password` не используются: адрес и учётные данные задаются в каждой группе
    отдельно.

    Описание `flush-consolidation-handler` — в
    [документации Netty](https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html).

Файлом конфигурации задаётся не всё, что доступно в билдере. Через свойства нельзя задать:

- параметры канала Netty;
- параметры Watchers;
- обработчик проигнорированных пакетов;
- параметры SSL;
- собственный `TimerService`;
- собственный реестр метрик Micrometer;
- слушатель событий пула `PoolEventListener`.

Если нужны и они, и настройки из файла, соберите билдер из свойств в своём бине, как показано в
следующем разделе.

### Свои слушатели и обработчики при настройке через файл

Автоконфигурация собирает клиент только из свойств `spring.data.tarantool.*`: бины
`PoolEventListener` и других обработчиков она в контексте не ищет. Чтобы добавить их и сохранить
настройки из файла, объявите свой бин `TarantoolCrudClientBuilder` и соберите его из тех же
свойств через `BaseTarantoolCrudConfiguration`:

```java
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.spring.data.config.BaseTarantoolCrudConfiguration;
import io.tarantool.spring.data40.config.properties.TarantoolProperties;
import io.tarantool.spring.data40.repository.config.EnableTarantoolRepositories;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableTarantoolRepositories
public class Config {

  @Bean
  public TarantoolCrudClientBuilder clientSettings(TarantoolProperties properties) {
    PoolEventListener listener =
        new PoolEventListener() {
          @Override
          public void onConnectionFailed(String tag, int index, Throwable error) {
            // например, записать событие в лог или в метрики
          }
        };
    return new BaseTarantoolCrudConfiguration(properties, null)
        .getClientBuilder()
        .withPoolEventListener(listener);
  }
}
```

`getClientBuilder()` переносит в билдер свойства `host`, `port`, `user-name`, `password`,
`connection-groups`, `balancer-mode`, `connect-timeout`, `reconnect-after`,
`event-loop-threads-count`, `heartbeat` и `graceful-shutdown-enabled`. Остальное задаётся методами
билдера, как `withPoolEventListener(...)` в примере.

### Настройка без `@EnableTarantoolRepositories`

Если репозитории не нужны, зарегистрируйте в контексте Spring сам клиент:

```java
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public TarantoolCrudClient crudClient() throws Exception {
    return TarantoolFactory.crud()
        .withHost("localhost")
        .withPort(3301)
        .withPassword("secret-cluster-cookie")
        .build();
  }
}
```

С кластером в этом случае работают только через API `TarantoolCrudClient`.

## Конфигурирование BOX-клиента

`TarantoolBoxClient` можно настроить Java-классом или из свойств `spring.data.tarantool.*`.
Репозитории для Box-клиента не создаются, `@EnableTarantoolRepositories` для него не нужна.

### Настройка через Java-класс

Зарегистрируйте в контексте Spring сам клиент:

```java
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public TarantoolBoxClient boxClient() throws Exception {
    return TarantoolFactory.box()
        .withHost("localhost")
        .withPort(3301)
        .withUser("guest")
        .build();
  }
}
```

### Настройка из свойств

Бин `TarantoolBoxClient` из свойств собирает класс `TarantoolBoxConfiguration` (пакет
`io.tarantool.spring.data40.config`). Если в контексте есть ваш бин `TarantoolBoxClientBuilder`,
клиент собирается из него, иначе из `TarantoolProperties`. По коду SDK ни
`@EnableTarantoolRepositories` (она регистрирует только конфигурацию CRUD-клиента), ни
автоконфигурация этот класс не подключают, поэтому импортируйте его явно вместе со свойствами:

```java
import io.tarantool.spring.data40.config.TarantoolBoxConfiguration;
import io.tarantool.spring.data40.config.properties.TarantoolProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(TarantoolProperties.class)
@Import(TarantoolBoxConfiguration.class)
public class Config {}
```

Из свойств переносятся те же настройки, что и для CRUD-клиента, а также `fetch-schema` и
`ignore-old-schema-version`.
