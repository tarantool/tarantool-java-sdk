/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.config;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties;

public class BaseTarantoolCrudConfiguration
    extends TarantoolConfiguration<TarantoolCrudClientBuilder> {

  private final TarantoolCrudClientBuilder tarantoolCrudClientBuilder;

  public BaseTarantoolCrudConfiguration(
      BaseTarantoolProperties properties, TarantoolCrudClientBuilder tarantoolClientConfiguration) {
    super(properties);
    this.tarantoolCrudClientBuilder = tarantoolClientConfiguration;
  }

  public TarantoolCrudClient tarantoolCrudClient() throws Exception {
    return getClientBuilder().build();
  }

  @Override
  public TarantoolCrudClientBuilder getClientBuilder() {
    if (this.tarantoolCrudClientBuilder != null) {
      return this.tarantoolCrudClientBuilder;
    }
    final TarantoolCrudClientBuilder builder =
        TarantoolFactory.crud()
            .withHost(properties.getHost())
            .withPassword(properties.getPassword())
            .withBalancerClass(properties.getBalancerClass())
            .withConnectTimeout(properties.getConnectTimeout())
            .withReconnectAfter(properties.getReconnectAfter())
            .withPort(properties.getPort())
            .withEventLoopThreadsCount(properties.getEventLoopThreadsCount())
            .withUser(properties.getUserName())
            .withGroups(properties.getInstanceConnectionGroups())
            .withHeartbeat(properties.getHeartbeatOpts());

    if (!properties.isGracefulShutdownEnabled()) {
      builder.disableGracefulShutdown();
    }
    return builder;
  }
}
