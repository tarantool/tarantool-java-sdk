/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_CRUD_CLIENT_BEAN_REF;
import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.factory.TarantoolCrudClientBuilder;
import io.tarantool.spring.data.config.BaseTarantoolCrudConfiguration;
import io.tarantool.spring.data34.TarantoolCrudKeyValueAdapter;
import io.tarantool.spring.data34.config.properties.TarantoolProperties;

@Configuration(proxyBeanMethods = false)
public class TarantoolCrudConfiguration extends BaseTarantoolCrudConfiguration {

  public TarantoolCrudConfiguration(
      ObjectProvider<TarantoolProperties> properties,
      ObjectProvider<TarantoolCrudClientBuilder> tarantoolClientConfiguration) {
    super(properties.getIfAvailable(), tarantoolClientConfiguration.getIfAvailable());
  }

  @Bean(name = DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF)
  @ConditionalOnMissingBean(TarantoolCrudKeyValueAdapter.class)
  public TarantoolCrudKeyValueAdapter tarantoolCrudKeyValueAdapter(
      TarantoolCrudClient tarantoolCrudClient) {
    return new TarantoolCrudKeyValueAdapter(tarantoolCrudClient);
  }

  @Bean(name = DEFAULT_TARANTOOL_CRUD_CLIENT_BEAN_REF)
  @ConditionalOnMissingBean(TarantoolCrudClient.class)
  public TarantoolCrudClient tarantoolCrudClient() throws Exception {
    return super.tarantoolCrudClient();
  }

  public TarantoolCrudClientBuilder getClientBuilder() {
    return super.getClientBuilder();
  }
}
