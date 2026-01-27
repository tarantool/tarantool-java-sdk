/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.core;

import org.springframework.data.keyvalue.core.IdentifierGenerator;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.ClassUtils;

import io.tarantool.spring.data35.core.annotation.DefaultIdClassResolver;
import io.tarantool.spring.data35.core.mapping.TarantoolMappingContext;

public class TarantoolTemplate extends KeyValueTemplate {

  public TarantoolTemplate(KeyValueAdapter adapter) {
    super(adapter, new TarantoolMappingContext<>());
  }

  public TarantoolTemplate(
      KeyValueAdapter adapter,
      MappingContext<
              ? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>>
          mappingContext) {
    super(adapter, mappingContext);
  }

  public TarantoolTemplate(
      KeyValueAdapter adapter,
      MappingContext<
              ? extends KeyValuePersistentEntity<?, ?>, ? extends KeyValuePersistentProperty<?>>
          mappingContext,
      IdentifierGenerator identifierGenerator) {
    super(adapter, mappingContext, identifierGenerator);
  }

  @Override
  public <T> T insert(T objectToInsert) {
    if (DefaultIdClassResolver.INSTANCE.resolveIdClassType(objectToInsert.getClass()) == null) {
      return super.insert(objectToInsert);
    }

    PersistentEntity<?, ?> entity =
        getMappingContext().getRequiredPersistentEntity(ClassUtils.getUserClass(objectToInsert));

    IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(objectToInsert);
    Object id = identifierAccessor.getRequiredIdentifier();
    return insert(id, objectToInsert);
  }
}
