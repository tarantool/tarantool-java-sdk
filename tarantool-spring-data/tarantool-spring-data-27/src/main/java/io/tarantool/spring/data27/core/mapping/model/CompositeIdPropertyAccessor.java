/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.core.mapping.model;

import java.lang.reflect.Field;
import java.util.Map;

import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import io.tarantool.spring.data.mapping.model.CompositeKey;

/**
 * Class that allows to convert {@code @Id} annotated fields in an entity into a composite key.
 */
public class CompositeIdPropertyAccessor extends TargetAwareIdentifierAccessor {

  private final Map<Field, Field> entityIdClassFields;

  private final Class<?> idClassType;

  private final Object target;

  public CompositeIdPropertyAccessor(Object target,
      @NonNull Map<Field, Field> entityIdClassFields,
      Class<?> idClassType) {

    super(target);
    this.target = target;
    this.entityIdClassFields = entityIdClassFields;
    this.idClassType = idClassType;
  }

  @Nullable
  public Object getIdentifier() {
    try {
      return generateIdentifier();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates a composite identifier from those marked with the {@code @Id} annotation in a domain class based on
   * fields, specified in the composite key class.
   *
   * @return identifier
   * @throws IllegalAccessException if the class or its nullary constructor is not accessible.
   * @throws InstantiationException if this {@code Class} represents an abstract class, an interface, an array class, a
   *                                primitive type, or void; or if the class has no nullary constructor; or if the
   *                                instantiation fails for some other reason.
   */
  private Object generateIdentifier() throws InstantiationException, IllegalAccessException {
    Assert.notNull(this.target, "target object must be not null!");
    Object compositeKey = idClassType.newInstance();
    for (Map.Entry<Field, Field> fieldPair : this.entityIdClassFields.entrySet()) {
      writeValuesFromEntityId(compositeKey, fieldPair.getKey(), fieldPair.getValue());
    }
    return compositeKey;
  }

  /**
   * Write the composite primary key fields provided in the entity to the class which implements {@link CompositeKey}.
   *
   * @param compositeKey      object of the composite key into which the values are written.
   * @param entityField       field from the represented entity.
   * @param compositeKeyField field from the composite key class.
   */
  private void writeValuesFromEntityId(Object compositeKey, Field entityField, Field compositeKeyField) {
    ReflectionUtils.makeAccessible(entityField);
    ReflectionUtils.makeAccessible(compositeKeyField);

    Object value = ReflectionUtils.getField(entityField, this.target);
    ReflectionUtils.setField(compositeKeyField, compositeKey, value);
  }
}
