/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data40.core.mapping;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import io.tarantool.spring.data40.core.mapping.BasicKeyValueCompositePersistentEntity.KeyPartTypeChecker;
import io.tarantool.spring.data40.utils.entity.ComplexPerson;
import io.tarantool.spring.data40.utils.entity.EntityWithWrongCompositeKeyPartsCount;
import io.tarantool.spring.data40.utils.entity.EntityWithWrongFieldTypes;
import io.tarantool.spring.data40.utils.entity.Person;

class TarantoolMappingContextTest {

  @Test
  void testSetFallbackKeySpaceResolver() throws NoSuchFieldException {
    final KeySpaceResolver RESOLVER = TestResolver.INSTANCE;
    final TarantoolMappingContext<?, ?> mappingContext = new TarantoolMappingContext<>();

    mappingContext.setKeySpaceResolver(RESOLVER);

    final Field field = TarantoolMappingContext.class.getDeclaredField("keySpaceResolver");
    ReflectionUtils.makeAccessible(field);

    final Object resolverFieldValue = ReflectionUtils.getField(field, mappingContext);

    assertEquals(RESOLVER, resolverFieldValue);
  }

  @Test
  void testCreatePersistentEntity() {
    final TarantoolMappingContext<?, ?> mappingContext = new TarantoolMappingContext<>();

    final TypeInformation<Person> informationForSimpleClass = TypeInformation.of(Person.class);

    final TypeInformation<ComplexPerson> informationForIdentifierClass =
        TypeInformation.of(ComplexPerson.class);

    assertInstanceOf(
        KeyValueCompositePersistentEntity.class, createEntityForClass(ComplexPerson.class));

    assertInstanceOf(KeyValuePersistentEntity.class, createEntityForClass(ComplexPerson.class));
  }

  @Test
  void testCreatePersistentProperty() {
    ReflectionUtils.FieldFilter idFilter = field -> field.isAnnotationPresent(Id.class);

    PersistentProperty<?> simpleIdProperty = createProperty(Person.class, idFilter);
    assertInstanceOf(KeyValuePersistentProperty.class, simpleIdProperty);

    PersistentProperty<?> compositeIdProperty = createProperty(ComplexPerson.class, idFilter);
    assertInstanceOf(KeyValueCompositeProperty.class, compositeIdProperty);
  }

  @Test
  void testCreateEntityWithWrongCompositeKeyPartTypes() {
    Set<Class<?>> initialSet =
        new HashSet<>() {
          {
            add(EntityWithWrongFieldTypes.class);
          }
        };
    MappingException exception =
        assertThrows(MappingException.class, () -> initEntities(initialSet));
    final Throwable cause = exception.getCause();
    Assertions.assertInstanceOf(IllegalArgumentException.class, cause);

    Assertions.assertEquals(
        KeyPartTypeChecker.COMPOSITE_KEY_FIELD_DIFFERENT_EXCEPTION, cause.getMessage());
  }

  @Test
  void testCreateEntityWithWrongCompositeKeyPartCount() {
    Set<Class<?>> initialSet =
        new HashSet<>() {
          {
            add(EntityWithWrongCompositeKeyPartsCount.class);
          }
        };
    MappingException exception =
        assertThrows(MappingException.class, () -> initEntities(initialSet));
    final Throwable cause = exception.getCause();
    Assertions.assertInstanceOf(IllegalArgumentException.class, cause);
    assertEquals(KeyPartTypeChecker.COMPOSITE_KEY_FIELDS_NUMBER_EXCEPTION, cause.getMessage());
  }

  /**
   * Create a mappingContext from the passed domain classes. After initialize - create for them PersistentEntities and
   * add PersistentProperties to them.
   */
  private void initEntities(Set<Class<?>> entitySet) {
    TarantoolMappingContext<?, ?> mappingContext = new TarantoolMappingContext<>();
    mappingContext.setInitialEntitySet(entitySet);
    mappingContext.initialize();
  }

  private enum TestResolver implements KeySpaceResolver {
    INSTANCE;

    @Override
    @Nullable
    public String resolveKeySpace(@NonNull Class<?> type) {

      Assert.notNull(type, "Type for keyspace for null!");

      Class<?> userClass = ClassUtils.getUserClass(type);
      Object keySpace = getKeySpace(userClass);

      return keySpace != null ? keySpace.toString() : null;
    }

    @Nullable
    private static Object getKeySpace(Class<?> type) {

      KeySpace keyspace = AnnotatedElementUtils.findMergedAnnotation(type, KeySpace.class);

      if (keyspace != null) {
        return AnnotationUtils.getValue(keyspace);
      }

      return null;
    }
  }

  private PersistentEntity<?, ?> createEntityForClass(Class<?> entityClass) {
    final TarantoolMappingContext<?, ?> mappingContext = new TarantoolMappingContext<>();

    final TypeInformation<?> informationForClass = TypeInformation.of(entityClass);
    return mappingContext.createPersistentEntity(informationForClass);
  }

  private <P extends KeyValueCompositeProperty<P>> PersistentProperty<?> createProperty(
      Class<?> classType, ReflectionUtils.FieldFilter fieldFilter) {

    final TarantoolMappingContext<KeyValuePersistentEntity<?, P>, P> context =
        new TarantoolMappingContext<>();
    KeyValuePersistentEntity<?, P> entity =
        context.createPersistentEntity(TypeInformation.of(classType));
    Field field = org.springframework.data.util.ReflectionUtils.findField(classType, fieldFilter);
    assertNotNull(field);

    return context.createPersistentProperty(
        Property.of(entity.getTypeInformation(), field), entity, SimpleTypeHolder.DEFAULT);
  }
}
