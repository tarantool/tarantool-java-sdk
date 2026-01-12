---
title: Client Configuration
---

## Configuring the CRUD Client

Currently, `TarantoolCrudClient` configuration can be done through:

- Java classes (JavaConfig);
- Java classes (JavaConfig) without using the `@EnableTarantoolRepositories` annotation;
- Configuration files (.properties/.yaml).

### Configuration via Java Classes

To configure a client for working with `Spring Data Tarantool`, you need to create a class with
`@Configuration` and `@EnableTarantoolRepositories` annotations. In this class, you need to create a factory
method with the `@Bean` annotation that returns an instance of `TarantoolCrudClientBuilder` with all
necessary settings:

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

???+ warning "Important!"

    The full list of parameters and their purposes can be found in the javadoc for 
    `TarantoolCrudClientBuilder`

### Client Configuration via Configuration File

To configure the client through a configuration file (for example, via
`application.properties` or `application.yaml`), you need to use the following parameters:

=== "Configuration Class"

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

    1. The address to which the client connects
    2. The port to which the client connects on `host`
    3. The username for connecting to `Tarantool`
    4. The password for connecting to `Tarantool`
    5. Whether to use the `graceful shutdown` protocol
    6. Selection of the request balancing type
    7. Timeout for connecting to `Tarantool` in ms (`connect-timeout: 5000` - 5 seconds)
    8. Time in ms after which to reconnect upon failed connection 
       (`reconnect-after: 1000` - 1 second)
    9. Sets a list of connection groups with descriptions of each
    10. Authentication protocol type
    11. The address to which the client connects
    12. The port to which the client connects on `host`
    13. The username for connecting to `Tarantool`
    14. The password for connecting to `Tarantool`
    15. Number of connections in one group
    16. Tag, group name
    17. Allows configuring `flush` netty parameters
    18. Number of threads allocated to netty
    19. Settings for pinging nodes `Tarantool`
    
    ???+ warning "Important"
    
        - When specifying at least one element of the `connection-groups` group, the parameters  
          `spring.data.tarantool.host`, `spring.data.tarantool.port`, `spring.data.tarantool.user-name`, 
          `spring.data.tarantool.password`, `spring.data.tarantool.connection-group-size`, 
          `spring.data.tarantool.tag` are ignored
        - In each group, these parameters (`spring.data.tarantool.host`, 
          `spring.data.tarantool.connection-groups.[*].port`, 
          `spring.data.tarantool.connection-groups.[*].user-name`, 
          `spring.data.tarantool.connection-groups.[*].password`, ...) are set individually.

    ???+ note "Note"
    
        More information about parameters:
        
        - Information about `heartbeat` parameters can be found in the javadoc for Heartbeat
        - [Official Netty documentation about `flush-consolidation-handler`](https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html)

???+ warning "Important"

    The capabilities of client configuration through configuration files are limited compared to 
    configuration through Java classes, as some parameters are not available.

    Full list of unavailable features:
    
    - Defining Netty channel parameters;
    - Defining Watchers parameters;
    - Defining handler for packets that were ignored;
    - Defining SSL parameters;
    - Setting a custom TimerService;
    - Setting a custom Micrometer metrics registry.

???+ warning "Important"

    When using client settings through configuration files and Java classes simultaneously, the 
    configuration based on Java classes will be selected.

When starting the Spring context with `@EnableTarantoolRepositories`, the following actions will be performed:

- connecting the client to the `Tarantool` cluster based on the parameters specified in `TarantoolCrudClientBuilder`;
- registering user [`Repository`](repository.md) in the Spring context;
- registering the `TarantoolCrudClient` component in the Spring context.

### Configuration via Java Classes (JavaConfig) without using `@EnableTarantoolRepositories`

You can use the `TarantoolCrudClient` API directly by registering the component in the Spring
context:

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

???+ warning "Important"

    In this case, working with the cluster is only done through the `TarantoolCrudClient` API

## Configuring the BOX Client

Currently, box client configuration can only be done through Java classes (JavaConfig)
without using the `@EnableTarantoolRepositories` annotation:

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
