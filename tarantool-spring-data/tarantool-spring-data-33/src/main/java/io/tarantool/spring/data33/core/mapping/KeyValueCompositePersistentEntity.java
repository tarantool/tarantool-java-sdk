/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.core.mapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.MutablePersistentEntity;

import io.tarantool.spring.data.core.annotation.IdClass;

/**
 * An interface to enable {@link PersistentEntity}-specific operations with a composite key.
 *
 * @param <T> domain class type
 * @param <P> {@link PersistentProperty} type
 */
public interface KeyValueCompositePersistentEntity<T, P extends KeyValuePersistentProperty<P>>
    extends MutablePersistentEntity<T, P> {

  /**
   * Returns the fields of the class specified in the {@link IdClass} annotation.
   *
   * @return all fields that are in composite key class
   */
  default List<Field> getIdClassTypeFields() {
    List<Field> fields = new ArrayList<>();

    for (Field field : getIdClassType().getDeclaredFields()) {
      if (!field.isSynthetic()) {
        fields.add(field);
      }
    }
    return fields;
  }

  /**
   * Returns the type of the class specified in the {@link IdClass} annotation.
   *
   * @return class that specified in {@link IdClass}
   */
  Class<?> getIdClassType();
}
