/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.core.mapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KeyValueCompositePropertyTest<P extends KeyValueCompositeProperty<P>> {

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
        new BasicKeyValuePersistentEntity<>(ClassTypeInformation.from(TestEntity.class), null);

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
        new BasicKeyValuePersistentEntity<>(ClassTypeInformation.from(TestEntity.class), null);

    ReflectionUtils.FieldFilter idFieldFiler = field -> field.isAnnotationPresent(Id.class);
    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner), idFieldFiler);

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

    PersistentEntity<?, P> owner = new BasicKeyValuePersistentEntity<>(ClassTypeInformation.from(TestEntity.class),
        null);
    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner),
        field -> field.isAnnotationPresent(Id.class));

    assertNull(identifier);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testAddWithRepeatedProperty() {
    Class<?> entityClass = TestEntity.class;

    final List<Field> exceptedFields = getExpectedIdFields(entityClass);

    PersistentEntity<?, P> owner =
        new BasicKeyValuePersistentEntity<>(ClassTypeInformation.from(entityClass), null);

    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner),
        field -> field.isAnnotationPresent(Id.class));

    // repeat
    assertNotNull(identifier);
    identifier.addPart((P) ((List<?>) identifier.getParts()).get(0));

    assertNotNull(identifier);
    final List<Field> addedFields = Arrays.asList(identifier.getFields());

    compareFieldCollection(exceptedFields, addedFields);
  }

  private Stream<Arguments> dataForTestEquals() {
    final Function<Class<?>, List<KeyValueCompositeProperty<P>>> doCreateEntityAndGetProperties = (entityType) -> {

      final PersistentEntity<?, P> owner =
          new BasicKeyValuePersistentEntity<>(ClassTypeInformation.from(entityType),
              null);

      ReflectionUtils.doWithFields(entityType, field -> addPart(field, owner),
          field -> field.isAnnotationPresent(Id.class));

      assertNotNull(identifier);

      return new ArrayList<>(identifier.getParts());
    };

    return Stream.of(
        Arguments.of(doCreateEntityAndGetProperties, TestEntity.class, SecondTestEntity.class,
            TestEntityWithOneId.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEquals")
  void testEquals(
      Function<Class<?>, List<KeyValueCompositeProperty<P>>> generatePropertiesFunction,
      Class<?> entityType, Class<?> sameEntityWithOtherClass, Class<?> entityWithOneId) {
    // first set
    List<KeyValueCompositeProperty<P>> firstPropertiesList = generatePropertiesFunction.apply(entityType);
    Identifier<P> firstIdentifier = identifier;

    identifier = null;

    // second set
    List<KeyValueCompositeProperty<P>> secondPropertiesList = generatePropertiesFunction.apply(entityType);
    Identifier<P> secondIdentifier = identifier;

    identifier = null;

    // third set
    List<KeyValueCompositeProperty<P>> thirdPropertiesList = generatePropertiesFunction.apply(entityType);
    Identifier<P> thirdIdentifier = identifier;

    identifier = null;

    // same set but with other class
    List<KeyValueCompositeProperty<P>> samePropertiesListWithOtherClass =
        generatePropertiesFunction.apply(sameEntityWithOtherClass);
    Identifier<P> sameIdentifierWithOtherClass = identifier;

    identifier = null;

    // extra set
    List<KeyValueCompositeProperty<P>> oneIdPropertiesList =
        generatePropertiesFunction.apply(entityWithOneId);
    Identifier<P> oneIdIdentifier = identifier;

    identifier = null;

    // always false
    assertNotEquals(firstIdentifier, null);
    assertNotEquals(null, firstIdentifier);

    // transitive
    for (int i = 0; i < firstPropertiesList.size(); i++) {
      assertEquals(firstPropertiesList.get(i), secondPropertiesList.get(i));
      assertEquals(firstPropertiesList.get(i), thirdPropertiesList.get(i));
      assertEquals(secondPropertiesList.get(i), thirdPropertiesList.get(i));

      assertNotEquals(firstPropertiesList.get(i), samePropertiesListWithOtherClass.get(i));
      assertNotEquals(secondPropertiesList.get(i), samePropertiesListWithOtherClass.get(i));
      assertNotEquals(thirdPropertiesList.get(i), samePropertiesListWithOtherClass.get(i));


    }

    for (int i = 0; i < oneIdPropertiesList.size(); i++) {
      assertNotEquals(firstPropertiesList.get(i), oneIdPropertiesList.get(i));
      assertNotEquals(secondPropertiesList.get(i), oneIdPropertiesList.get(i));
      assertNotEquals(thirdPropertiesList.get(i), oneIdPropertiesList.get(i));
    }

    // simple compare complex property
    for (int i = 1; i < firstPropertiesList.size(); i++) {
      assertNotEquals(firstPropertiesList.get(i), firstIdentifier);
      assertNotEquals(firstIdentifier, firstPropertiesList.get(i));
    }

    // compare itself
    assertEquals(firstIdentifier, firstIdentifier);
    assertEquals(firstIdentifier, secondIdentifier);
    assertEquals(secondIdentifier, thirdIdentifier);

    //compare complex same identifier but other class
    assertNotEquals(firstIdentifier, sameIdentifierWithOtherClass);
    assertNotEquals(secondIdentifier, sameIdentifierWithOtherClass);
    assertNotEquals(thirdIdentifier, sameIdentifierWithOtherClass);

    //compare complex identifier with one id
    assertNotEquals(firstIdentifier, oneIdIdentifier);
    assertNotEquals(secondIdentifier, oneIdIdentifier);
    assertNotEquals(thirdIdentifier, oneIdIdentifier);
  }

  @Test
  void testHashCode() {
    Class<?> entityClass = TestEntity.class;

    // first set
    PersistentEntity<?, P> owner =
        new BasicKeyValuePersistentEntity<>(ClassTypeInformation.from(entityClass), null);
    ReflectionUtils.doWithFields(entityClass, field -> addPart(field, owner),
        field -> field.isAnnotationPresent(Id.class));

    assertNotNull(identifier);
    List<KeyValueCompositeProperty<P>> properties = new ArrayList<>(identifier.getParts());

    Set<KeyValueCompositeProperty<P>> propertiesSet = new HashSet<>(properties);

    assertTrue(propertiesSet.containsAll(properties));
  }

  @Getter
  static class TestEntity {

    @Id
    private long id;

    @Id
    private int secondId;

    private int age;
  }

  @Getter
  static class SecondTestEntity {

    @Id
    private long id;

    @Id
    private int secondId;

    private int age;
  }

  @Getter
  static class TestEntityWithOneId {

    @Id
    private long id;

    private int secondId;

    private int age;
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
      return;
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
