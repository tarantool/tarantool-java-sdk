/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_BOX_CLIENT_BEAN_REF;
import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_BOX_KEY_VALUE_ADAPTER_REF;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolBoxClientBuilder;
import io.tarantool.spring.data31.TarantoolBoxKeyValueAdapter;
import io.tarantool.spring.data.config.BaseTarantoolBoxConfiguration;
import io.tarantool.spring.data31.config.properties.TarantoolProperties;

@Configuration(proxyBeanMethods = false)
public class TarantoolBoxConfiguration extends BaseTarantoolBoxConfiguration {

  public TarantoolBoxConfiguration(ObjectProvider<TarantoolProperties> properties,
      ObjectProvider<TarantoolBoxClientBuilder> tarantoolBoxClientBuilder) {
    super(properties.getIfAvailable(), tarantoolBoxClientBuilder.getIfAvailable());
  }

  @Bean(name = DEFAULT_TARANTOOL_BOX_KEY_VALUE_ADAPTER_REF)
  @ConditionalOnMissingBean(TarantoolBoxKeyValueAdapter.class)
  public TarantoolBoxKeyValueAdapter tarantoolCrudKeyValueAdapter(TarantoolBoxClient tarantoolBoxClient) {
    return new TarantoolBoxKeyValueAdapter(tarantoolBoxClient);
  }

  @Bean(name = DEFAULT_TARANTOOL_BOX_CLIENT_BEAN_REF)
  @ConditionalOnMissingBean(TarantoolBoxClient.class)
  public TarantoolBoxClient tarantoolBoxClient() throws Exception {
    return super.tarantoolBoxClient();
  }

  @Override
  public TarantoolBoxClientBuilder getClientBuilder() {
    return super.getClientBuilder();
  }
}
