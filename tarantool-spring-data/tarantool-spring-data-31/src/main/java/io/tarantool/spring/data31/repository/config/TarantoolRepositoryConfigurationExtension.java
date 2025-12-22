/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.repository.config;

import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.keyvalue.repository.config.KeyValueRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF;
import static io.tarantool.spring.data.TarantoolBeanNames.DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF;
import io.tarantool.client.ClientType;
import io.tarantool.spring.data31.config.TarantoolCrudConfiguration;
import io.tarantool.spring.data31.core.TarantoolTemplate;
import io.tarantool.spring.data31.core.mapping.TarantoolMappingContext;

public class TarantoolRepositoryConfigurationExtension extends KeyValueRepositoryConfigurationExtension {

  /*
   * (non-Javadoc)
   * @see org.springframework.data.keyvalue.repository.config.KeyValueRepositoryConfigurationExtension
   *                          #getModuleName()
   */
  @Override
  public String getModuleName() {
    return "Tarantool";
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.keyvalue.repository.config.KeyValueRepositoryConfigurationExtension
   *                          #getModulePrefix()
   */
  @Override
  public String getModulePrefix() {
    return "tarantool";
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.keyvalue.repository.config.KeyValueRepositoryConfigurationExtension
   *                          #getDefaultKeyValueTemplateRef()
   */
  @Override
  public String getDefaultKeyValueTemplateRef() {
    return DEFAULT_TARANTOOL_KEY_VALUE_TEMPLATE_REF;
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.keyvalue.repository.config.KeyValueRepositoryConfigurationExtension
   *                          #registerBeansForRoot()
   */
  @Override
  public void registerBeansForRoot(BeanDefinitionRegistry registry,
      RepositoryConfigurationSource configurationSource) {
    // register a default set of beans based on the passed annotation attributes.
    final ClientType clientTypeAttribute = configurationSource.getRequiredAttribute("clientType", ClientType.class);

    if (clientTypeAttribute == ClientType.CRUD) {
      final String defaultCrudConfigurationBeanName = TarantoolCrudConfiguration.class.getCanonicalName();
      registerIfNotAlreadyRegistered(() -> new RootBeanDefinition(TarantoolCrudConfiguration.class),
          registry,
          defaultCrudConfigurationBeanName,
          configurationSource);
    } else {
      throw new IllegalArgumentException("The Box client is not yet supported.");
    }
    super.registerBeansForRoot(registry, configurationSource);

    // remove KeyValueMappingContext to add TarantoolMappingContext
    registry.removeBeanDefinition(getMappingContextBeanRef());
    registerIfNotAlreadyRegistered(() -> {

      RootBeanDefinition definition = new RootBeanDefinition(TarantoolMappingContext.class);
      definition.setSource(configurationSource.getSource());

      return definition;

    }, registry, getMappingContextBeanRef(), configurationSource);
  }

  @Override
  protected AbstractBeanDefinition getDefaultKeyValueTemplateBeanDefinition(
      RepositoryConfigurationSource configurationSource) {
    final RootBeanDefinition keyValueTemplateBeanDefinition = new RootBeanDefinition(TarantoolTemplate.class);

    final ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
    constructorArgumentValues.addIndexedArgumentValue(0, new RuntimeBeanReference(
        DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF));
    constructorArgumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference(
        getMappingContextBeanRef()));

    keyValueTemplateBeanDefinition.setConstructorArgumentValues(constructorArgumentValues);
    keyValueTemplateBeanDefinition.setSource(configurationSource);
    keyValueTemplateBeanDefinition.setDependsOn(DEFAULT_TARANTOOL_CRUD_KEY_VALUE_ADAPTER_REF);
    keyValueTemplateBeanDefinition.setDependsOn(getMappingContextBeanRef());

    return keyValueTemplateBeanDefinition;
  }
}
