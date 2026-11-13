---
title: Client Configuration
---

## Configuring the CRUD Client

`TarantoolCrudClient` for Spring Data Tarantool can be configured in three ways:

- a Java configuration class with `@EnableTarantoolRepositories` and your own
  `TarantoolCrudClientBuilder` bean;
- a configuration file (`application.properties` or `application.yaml`);
- a Java configuration class without `@EnableTarantoolRepositories`: then no repositories are
  created, and the cluster is accessed only through the `TarantoolCrudClient` API.

The examples below are written for the `tarantool-spring-data-40` module. In other modules the
`@EnableTarantoolRepositories` annotation and the `TarantoolProperties` class are in the packages
of their module: for example, for `tarantool-spring-data-35` they are
`io.tarantool.spring.data35.repository.config` and `io.tarantool.spring.data35.config.properties`.
Which module your application needs is shown in [the module table](index.md).

### What happens when the context starts

When the Spring context starts with `@EnableTarantoolRepositories`:

- the `TarantoolProperties` bean with the `spring.data.tarantool.*` properties is registered;
- the `TarantoolCrudClient` client is created and registered in the context as a bean. If the
  context has your own `TarantoolCrudClientBuilder` bean, the client is built from it as is: the
  `spring.data.tarantool.*` properties are not applied to it. If there is no such bean, the builder
  is built from the properties;
- user [repositories](repository.md) are registered.

No connections to the cluster are opened at this point: they are opened on the first request (see
[connection pool](../client/arch/connection-pool.md)).

### Configuration via a Java class

Create a class with the `@Configuration` and `@EnableTarantoolRepositories` annotations and declare
a `TarantoolCrudClientBuilder` bean with all the settings you need:

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

The full list of settings and their purpose is in the Javadoc of the `TarantoolCrudClientBuilder`
class.

### Configuration via a configuration file

To configure the client with a configuration file, keep only the annotations in the configuration
class and set the `spring.data.tarantool.*` properties:

=== "Configuration class"

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

    1. Address of the node the client connects to
    2. Port of the `host` node
    3. User name for connecting to Tarantool
    4. Password of the user
    5. Whether to use the `graceful shutdown` protocol
    6. Type of the [balancer](../client/arch/balancer.md)
    7. Connect timeout to Tarantool in ms (`connect-timeout: 5000` — 5 seconds). The default is
       3000 ms. There is no request timeout property, see [Timeouts](../client/arch/timeouts.md)
    8. How many milliseconds to wait before reconnecting after a failed connect
       (`reconnect-after: 1000` — 1 second)
    9. List of connection groups, see [Connection Group](../client/arch/instance-connection-group.md)
    10. Authentication protocol type. It must match the way the user's password is stored, that is,
        `security.auth_type` (`box.cfg.auth_type`) of the cluster; otherwise the login fails with
        `User not found or supplied credentials are invalid`. The default is `chap-sha1`, which is
        also the client default, so the parameter can be omitted. `pap-sha256` is available only in
        Enterprise Edition (and TDB) and only over an encrypted (TLS) connection
    11. Address of the group's node
    12. Port of the group's node
    13. User name of the group
    14. Password of the group's user
    15. Number of connections in the group
    16. Tag, the name of the group
    17. Netty `FlushConsolidationHandler` parameters
    18. Number of Netty threads
    19. [Heartbeat](../client/arch/heartbeat.md) parameters

    If at least one `connection-groups` element is set, the top-level `host`, `port`, `user-name`
    and `password` properties are not used: the address and credentials are set in each group
    separately.

    `flush-consolidation-handler` is described in the
    [Netty documentation](https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html).

A configuration file does not cover everything the builder offers. The properties cannot set:

- Netty channel parameters;
- Watchers parameters;
- a handler for ignored packets;
- SSL parameters;
- a custom `TimerService`;
- a custom Micrometer metrics registry;
- a `PoolEventListener` pool event listener.

If you need these as well as the settings from the file, build the builder from the properties in
your own bean, as shown in the next section.

### Custom listeners and handlers with file configuration

The auto-configuration builds the client only from the `spring.data.tarantool.*` properties: it
does not look for `PoolEventListener` beans or other handlers in the context. To add them and keep
the settings from the file, declare your own `TarantoolCrudClientBuilder` bean and build it from
the same properties with `BaseTarantoolCrudConfiguration`:

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
            // for example, log the event or record it in metrics
          }
        };
    return new BaseTarantoolCrudConfiguration(properties, null)
        .getClientBuilder()
        .withPoolEventListener(listener);
  }
}
```

`getClientBuilder()` copies the `host`, `port`, `user-name`, `password`, `connection-groups`,
`balancer-mode`, `connect-timeout`, `reconnect-after`, `event-loop-threads-count`, `heartbeat` and
`graceful-shutdown-enabled` properties to the builder. Everything else is set with builder methods,
like `withPoolEventListener(...)` in the example.

### Configuration without `@EnableTarantoolRepositories`

If you do not need repositories, register the client itself in the Spring context:

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

In this case the cluster is accessed only through the `TarantoolCrudClient` API.

## Configuring the BOX Client

`TarantoolBoxClient` can be configured with a Java class or from the `spring.data.tarantool.*`
properties. No repositories are created for the Box client, and it does not need
`@EnableTarantoolRepositories`.

### Configuration via a Java class

Register the client itself in the Spring context:

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

### Configuration from properties

The `TarantoolBoxClient` bean is built from the properties by the `TarantoolBoxConfiguration` class
(package `io.tarantool.spring.data40.config`). If the context has your own
`TarantoolBoxClientBuilder` bean, the client is built from it, otherwise from `TarantoolProperties`.
According to the SDK code, neither `@EnableTarantoolRepositories` (it registers only the CRUD client
configuration) nor auto-configuration imports this class, so import it explicitly together with
the properties:

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

The same settings as for the CRUD client are taken from the properties, plus `fetch-schema` and
`ignore-old-schema-version`.
