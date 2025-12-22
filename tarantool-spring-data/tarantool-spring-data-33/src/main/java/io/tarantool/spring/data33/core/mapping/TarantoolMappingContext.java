/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.core.mapping;

import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import io.tarantool.spring.data.core.annotation.IdClassResolver;
import io.tarantool.spring.data33.core.annotation.DefaultIdClassResolver;

public class TarantoolMappingContext<E extends KeyValuePersistentEntity<?, P>, P extends KeyValuePersistentProperty<P>>
    extends KeyValueMappingContext<E, P> {

  private final IdClassResolver idClassResolver;

  private @Nullable KeySpaceResolver keySpaceResolver;

  public TarantoolMappingContext() {
    super();
    this.idClassResolver = DefaultIdClassResolver.INSTANCE;
  }

  @Override
  public void setKeySpaceResolver(KeySpaceResolver keySpaceResolver) {
    super.setKeySpaceResolver(keySpaceResolver);
    this.keySpaceResolver = keySpaceResolver;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> E createPersistentEntity(TypeInformation<T> typeInformation) {
    final Class<?> idClassTypeValue = this.idClassResolver.resolveIdClassType(typeInformation.getType());
    if (idClassTypeValue == null) {
      return (E) new BasicKeyValuePersistentEntity<T, P>(typeInformation, this.keySpaceResolver);
    }
    return (E) new BasicKeyValueCompositePersistentEntity<>(typeInformation,
        this.keySpaceResolver,
        idClassTypeValue);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected P createPersistentProperty(Property property, E owner, SimpleTypeHolder simpleTypeHolder) {
    if (KeyValueCompositePersistentEntity.class.isAssignableFrom(owner.getClass())) {
      return (P) new KeyValueCompositeProperty<>(property, owner, simpleTypeHolder);
    }
    return (P) new KeyValuePersistentProperty<>(property, owner, simpleTypeHolder);
  }
}
