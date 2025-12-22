/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.core.mapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import io.tarantool.spring.data32.core.mapping.Identifier;
import io.tarantool.spring.data32.core.mapping.KeyValueCompositeProperty;

public class KeyValueCompositePropertyTest<P extends KeyValuePersistentProperty<P>> {

  private @Nullable Identifier<P> identifier;

  @BeforeEach
  public void setUp() {
    this.identifier = null;
  }

  /**
   * Check methods for adding and retrieving parts of a composite key.
   */
  @Test
  public void testAddAndGetParts() {
    Class<?> entityClass = TestEntity.class;

    PersistentEntity<?, P> owner =
        new BasicKeyValuePersistentEntity<>(TypeInformation.of(TestEntity.class), null);

    ReflectionUtils.FieldFilter isIdFieldFiler = field -> field.isAnnotationPresent(Id.class);
    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner), isIdFieldFiler);

    assertNotNull(identifier);
    List<Field> identifierFieldsFromProperties = this.identifier.getParts()
        .stream()
        .map(PersistentProperty::getField)
        .collect(Collectors.toList());

    compareFieldCollection(identifierFieldsFromProperties, getExpectedIdFields(entityClass));
  }

  /**
   * Check methods for adding and retrieving parts of a composite key.
   */
  @Test
  public void testAddAndGetFields() {
    Class<?> entityClass = TestEntity.class;

    PersistentEntity<?, P> owner =
        new BasicKeyValuePersistentEntity<>(TypeInformation.of(TestEntity.class), null);

    ReflectionUtils.FieldFilter isIdFieldFiler = field -> field.isAnnotationPresent(Id.class);
    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner), isIdFieldFiler);

    assertNotNull(identifier);
    List<Field> fieldsFromIdentifier = Arrays.asList(identifier.getFields());

    compareFieldCollection(fieldsFromIdentifier, getExpectedIdFields(entityClass));
  }

  /**
   * Check methods for adding and getting parts of a composite key for an object without {@code @Id}.
   */
  @Test
  public void testAddAndGetPartsWithEntityWithoutId() {
    Class<?> entityClass = TestEntityWithoutId.class;

    PersistentEntity<?, P> owner = new BasicKeyValuePersistentEntity<>(TypeInformation.of(TestEntity.class),
        null);
    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner),
        field -> field.isAnnotationPresent(Id.class));

    assertNull(identifier);
  }

  static class TestEntity {

    @Id
    private long id;

    @Id
    private int secondId;
  }

  static class TestEntityWithoutId {

    private long id;

    @Nullable
    private String name;

    private int secondId;
  }

  /**
   * Create a PersistentProperty and add to first id property.
   */
  @SuppressWarnings("unchecked")
  private void addPart(Field field, PersistentEntity<?, P> owner) {
    Property property = Property.of(owner.getTypeInformation(), field);
    Identifier<P> persistentProperty = new KeyValueCompositeProperty<>(property, owner, SimpleTypeHolder.DEFAULT);

    if (this.identifier == null) {
      this.identifier = persistentProperty;
    }
    this.identifier.addPart((P) persistentProperty);
  }

  /**
   * Compare fields by name and type.
   */
  private void compareFieldCollection(List<Field> first, List<Field> second) {
    assertEquals(first.size(), second.size());

    first.sort(Comparator.comparing(Field::getName));
    second.sort(Comparator.comparing(Field::getName));

    for (int i = 0; i < first.size(); i++) {
      assertEquals(first.get(i).getName(), second.get(i).getName());
      assertEquals(first.get(i).getType(), second.get(i).getType());
    }
  }

  /**
   * Get the expected fields marked with the {@code @Id} annotation.
   */
  private List<Field> getExpectedIdFields(Class<?> entityClass) {
    return Arrays.stream(entityClass.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(Id.class))
        .collect(Collectors.toList());
  }
}
