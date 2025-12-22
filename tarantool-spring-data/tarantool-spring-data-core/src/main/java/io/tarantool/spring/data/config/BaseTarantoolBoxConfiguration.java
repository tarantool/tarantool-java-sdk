/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.config;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties;

public class BaseTarantoolBoxConfiguration extends TarantoolConfiguration<TarantoolBoxClientBuilder> {

  private final TarantoolBoxClientBuilder tarantoolBoxClientBuilder;

  public BaseTarantoolBoxConfiguration(BaseTarantoolProperties properties,
      TarantoolBoxClientBuilder tarantoolBoxClientBuilder) {
    super(properties);
    this.tarantoolBoxClientBuilder = tarantoolBoxClientBuilder;
  }

  public TarantoolBoxClient tarantoolBoxClient() throws Exception {
    return getClientBuilder().build();
  }

  @Override
  public TarantoolBoxClientBuilder getClientBuilder() {
    if (this.tarantoolBoxClientBuilder != null) {
      return this.tarantoolBoxClientBuilder;
    }

    final TarantoolBoxClientBuilder builder =
        TarantoolFactory.box()
            .withHost(properties.getHost())
            .withPassword(properties.getPassword())
            .withBalancerClass(properties.getBalancerClass())
            .withConnectTimeout(properties.getConnectTimeout())
            .withReconnectAfter(properties.getReconnectAfter())
            .withPort(properties.getPort())
            .withEventLoopThreadsCount(properties.getEventLoopThreadsCount())
            .withUser(properties.getUserName())
            .withGroups(properties.getInstanceConnectionGroups())
            .withHeartbeat(properties.getHeartbeatOpts())
            .withFetchSchema(properties.isFetchSchema());

    if (!properties.isGracefulShutdownEnabled()) {
      builder.disableGracefulShutdown();
    }

    if (!properties.isIgnoreOldSchemaVersion()) {
      builder.enableOldSchemaVersionCheck();
    }
    return builder;
  }
}
