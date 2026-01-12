---
title: Конфигурация клиентов
---

## Конфигурирование CRUD-клиента

На данный момент конфигурирование `TarantoolCrudClient` клиента можно произвести через:

- Java-классы (JavaConfig);
- Java-классы (JavaConfig) без использования аннотации `@EnableTarantoolRepositories`;
- файлы конфигурации (.properties/.yaml).

### Конфигурация через Java классы

Для того чтобы настроить клиент для работы с `Spring Data Tarantool` необходимо создать класс с
аннотациями`@Configuration` и `@EnableTarantoolRepositories`. В этом классе нужно создать фабричный
метод с аннотацией `@Bean`, который возвращает экземпляр `TarantoolCrudClientBuilder` со всеми
необходимыми настройками:

```java
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.spring.data.repository.config.EnableTarantoolRepositories;

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

???+ warning "Важно!"

    Полный список параметров и их назначение можно посмотреть в javaDoc к 
    `TarantoolCrudClientBuilder`

### Настройка клиента через файл конфигурации

Для того чтобы настроить клиент через конфигурационный файл (например, через
`application.properties` или `application.yaml`), необходимо использовать следующие параметры:

=== "Класс конфигурации"

    ```java

    import io.tarantool.spring.data.repository.config.EnableTarantoolRepositories;
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
                    - auth-type: pap_sha256 #(10)!
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

    1. Адрес, к которому подключается клиент
    2. Порт, к которому подключается клиент на `host`
    3. Имя пользователя для подключения к `Tarantool`
    4. Пароль для подключения к `Tarantool`
    5. Использовать ли протокол `graceful shutdown`
    6. Выбор типа балансировщика запросов
    7. Тайм-аут для подключения к `Tarantool` в мс (`connect-timeout: 5000` - 5 секунд)
    8. Время в мс, через которое необходимо сделать пересоединение при неудачном подключении 
       (`reconnect-after: 1000` - 1 секунда)
    9. Задает список групп подключения с описанием каждой из них
    10. Тип протокола аутентификации
    11. Адрес, к которому подключается клиент
    12. Порт, к которому подключается клиент на `host`
    13. Имя пользователя для подключения к `Tarantool`
    14. Пароль для подключения к `Tarantool`
    15. Количество подключений в одной группе
    16. Тег, наименование группы
    17. Позволяет настроить параметры `flush` netty
    18. Количество потоков, выделенных для netty
    19. Настройки системы пингов с узлами `Tarantool`
    
    ???+ warning "Важно"
    
        - При задании хотя бы одного элемента группы `connections-groups` параметры  
          `spring.data.tarantool.host`, `spring.data.tarantool.port`, `spring.data.tarantool.user-name`, 
          `spring.data.tarantool.password`, `spring.data.tarantool.connection-group-size`, 
          `spring.data.tarantool.tag` игнорируются
        - В каждой группе данные параметры(`spring.data.tarantool.host`, 
          `spring.data.tarantool.connection-groups.[*].port`, 
          `spring.data.tarantool.connection-groups.[*].user-name`, 
          `spring.data.tarantool.connection-groups.[*].password`, ...) задаются индивидуально.

    ???+ note "Заметка"
    
        Подробнее о параметрах:
        
        -  Информацию о параметрах `heartbeat` можно найти в javadoc к Heartbeat
        -  [Официальная документация Netty о `flush-consolidation-handler`](https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html)

???+ warning "Важно"

    Возможности настройки клиента через файлы конфигурации ограничены, в сравнении с конфигурацией 
    через Java-классы, т.к. некоторые параметры недоступны.

    Полный список недоступных возможностей:
    
    - определение параметров канала Netty;
    - определение параметров Watchers;
    - определение обработчика пакетов, которые были проигнорированы;
    - определение параметров ssl;
    - задание собственного TimerService;
    - задание собственного реестра метрик Micrometer.

???+ warning "Важно"

    При использовании настроек клиента через конфигурационный файл и Java-классы одновременно, будет
    выбрана настройка, основанная на Java-классах.

При поднятии контекста Spring с использованием `@EnableTarantoolRepositories` будет произведены
следующие действия:

- подключение клиента к кластеру `Tarantool`, исходя из указанных в `TarantoolCrudClientBuilder`
  параметров;
- регистрация пользовательских [`Repository`](repository.md) в контексте Spring ;
- регистрация компонента `TarantoolCrudClient` в контексте Spring.

### Настройка через Java-классы (JavaConfig) без использования `@EnableTarantoolRepositories`

Вы можете использовать API `TarantoolCrudClient` напрямую, зарегистрировав компонент в контексте
Spring:

```java
import io.tarantool.client.factory.TarantoolFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public TarantoolCrudClient crudClient() {
    return TarantoolFactory.crud()
        .withHost("localhost")
        .withPort(3301)
        .withPassword("secret-cluster-cookie")
        .build();
  }
}
```

???+ warning "Важно"

    В случае такой настройки работа с кластером производится только через API `TarantoolCrudClient`

## Конфигурирование BOX-клиента

На данный момент конфигурирование box-клиента можно произвести только через Java-классы (JavaConfig)
без использования аннотации `@EnableTarantoolRepositories`:

```java
import io.tarantool.client.factory.TarantoolFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public TarantoolBoxClient boxClient() {
    return TarantoolFactory.box()
        .withHost("localhost")
        .withPort(3301)
        .withUser("guest")
        .build();
  }
}
```
