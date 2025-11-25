/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.core.mapping.model;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ReflectionUtils;

import io.tarantool.spring.data32.core.annotation.DefaultIdClassResolver;
import io.tarantool.spring.data32.core.mapping.BasicKeyValueCompositePersistentEntity;
import io.tarantool.spring.data32.core.mapping.KeyValueCompositeProperty;
import io.tarantool.spring.data32.utils.entity.ComplexPerson;

public class PersistentCompositeIdIsNewStrategyTest<P extends KeyValueCompositeProperty<P>> {

  /** Check the strategy creation method and check whether the domain entity is new. */
  @Test
  public void testForIdOnly() {

    // Currently identical like of 'of' method (version doesn't support)
    PersistentCompositeIdIsNewStrategy strategy =
        PersistentCompositeIdIsNewStrategy.forIdOnly(createPersistentEntity(ComplexPerson.class));

    // Now is stub - always is false
    ComplexPerson emptyEntity = new ComplexPerson();
    assertFalse(strategy.isNew(emptyEntity));
  }

  /** Check the strategy creation method and check whether the domain entity is new. */
  @Test
  public void testIsNew() {
    PersistentCompositeIdIsNewStrategy strategy =
        PersistentCompositeIdIsNewStrategy.of(createPersistentEntity(ComplexPerson.class));

    // Now is stub - always is false
    ComplexPerson emptyEntity = new ComplexPerson();
    assertFalse(strategy.isNew(emptyEntity));
  }

  private BasicKeyValueCompositePersistentEntity<?, P> createPersistentEntity(Class<?> entityType) {
    Class<?> compositeKeyType = DefaultIdClassResolver.INSTANCE.resolveIdClassType(entityType);
    assertNotNull(compositeKeyType);

    BasicKeyValueCompositePersistentEntity<?, P> persistentEntity =
        new BasicKeyValueCompositePersistentEntity<>(
            TypeInformation.of(entityType), null, compositeKeyType);
    ReflectionUtils.doWithFields(entityType, field -> addProperty(field, persistentEntity));
    return persistentEntity;
  }

  /** Add Persistent property into PersistentEntity. */
  @SuppressWarnings("unchecked")
  private void addProperty(Field field, BasicKeyValueCompositePersistentEntity<?, P> owner) {
    Property property = Property.of(owner.getTypeInformation(), field);
    P persistentProperty =
        (P) new KeyValueCompositeProperty<>(property, owner, SimpleTypeHolder.DEFAULT);
    owner.addPersistentProperty(persistentProperty);
  }
}
