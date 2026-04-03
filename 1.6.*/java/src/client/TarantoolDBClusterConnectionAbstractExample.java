/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.testcontainers.containers.tdb.TDB2ClusterImpl;
import org.testcontainers.containers.tdb.TDBCluster;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;

public abstract class TarantoolDBClusterConnectionAbstractExample {

  private static final List<String> ROLES = List.of("super");

  protected static final String TARANTOOL_DB_IMAGE =
      System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") + "tarantooldb:2.2.1";

  protected static final String SELLER_USER = "seller-user";
  protected static final String SELLER_USER_PWD = "pwd-1";

  protected static final String USER_1182 = "user-1182";
  protected static final String USER_1182_PWD = "pwd-2";

  protected static final String FIRST_ROUTER_CONTAINER_NAME = "router-1";
  protected static final String SECOND_ROUTER_CONTAINER_NAME = "router-2";

  protected static TDBCluster CLUSTER;

  protected abstract void simpleCrudConnection();

  @BeforeAll
  static void beforeAll() {
    CLUSTER = createTDBCluster();
    CLUSTER.start();
  }

  @AfterAll
  static void afterAll() {
    CLUSTER.close();
  }

  @SuppressWarnings("unchecked")
  private static TDBCluster createTDBCluster() {

    // Adds custom users to emulate documentation example
    Map<String, UsersProperty> users =
        Map.of(
            SELLER_USER,
            UsersProperty.builder().withPassword(SELLER_USER_PWD).withRoles(ROLES).build(),
            USER_1182,
            UsersProperty.builder().withPassword(USER_1182_PWD).withRoles(ROLES).build());

    final Tarantool3Configuration commonConfigWithoutCustomUsers =
        ConfigurationUtils.generateSimpleConfiguration(2, 3, 2);
    final Tarantool3Configuration config =
        ConfigurationUtils.addUsers(commonConfigWithoutCustomUsers, users);

    return TDB2ClusterImpl.builder(TARANTOOL_DB_IMAGE).withTDB2Configuration(config).build();
  }

  protected static InetSocketAddress getRouterAddress(String routerName) {
    final Map<String, TarantoolContainer<?>> routers = CLUSTER.routers();
    return routers.get(routerName).mappedAddress();
  }
}

// --8<-- [end:all]
