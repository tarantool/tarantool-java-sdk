/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.core.mapping;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Persistable;
import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.tarantool.spring.data.core.annotation.IdClass;
import io.tarantool.spring.data.mapping.model.CompositeKey;
import io.tarantool.spring.data32.core.mapping.model.CompositeIdPropertyAccessor;
import io.tarantool.spring.data32.core.mapping.model.PersistentCompositeIdIsNewStrategy;

public class BasicKeyValueCompositePersistentEntity<T, P extends KeyValueCompositeProperty<P>>
    extends BasicKeyValuePersistentEntity<T, P> implements KeyValueCompositePersistentEntity<T, P> {

  public static final String TYPE_MISMATCH =
      "Target bean of type %s is not of type of the persistent entity (%s)";

  private final Class<?> idClassTypeValue;
  private @Nullable P idProperty;

  private Map<Field, Field> entityIdClassFields;

  /**
   * @param information must not be {@literal null}.
   * @param fallbackKeySpaceResolver can be {@literal null}.
   * @param idClassTypeValue class that specified in {@link IdClass}
   */
  public BasicKeyValueCompositePersistentEntity(
      TypeInformation<T> information,
      @Nullable KeySpaceResolver fallbackKeySpaceResolver,
      Class<?> idClassTypeValue) {
    super(information, fallbackKeySpaceResolver);

    Assert.notNull(idClassTypeValue, "idClassTypeValue must be not null for this class!");
    this.idClassTypeValue = idClassTypeValue;
  }

  @Override
  public IdentifierAccessor getIdentifierAccessor(Object bean) {
    verifyBeanType(bean);

    if (Persistable.class.isAssignableFrom(getType())) {
      throw new IllegalArgumentException(
          "Persistable override is not currently supported for entities with a composite key.");
    }

    return hasIdProperty()
        ? new CompositeIdPropertyAccessor(bean, this.entityIdClassFields, getIdClassType())
        : new AbsentIdentifierAccessor();
  }

  @Override
  public Class<?> getIdClassType() {
    return this.idClassTypeValue;
  }

  @Override
  public void verify() {
    super.verify();

    if (this.idProperty != null) {
      this.entityIdClassFields = KeyPartTypeChecker.getFieldMapIfTypesValid(this, this.idProperty);
    }
  }

  @Override
  protected IsNewStrategy getFallbackIsNewStrategy() {
    return PersistentCompositeIdIsNewStrategy.of(this);
  }

  @Override
  protected P returnPropertyIfBetterIdPropertyCandidateOrNull(P property) {
    Assert.isInstanceOf(Identifier.class, property, "property must be Identifier for this class");

    if (!property.isIdProperty()) {
      return null;
    }

    if (this.idProperty == null) {
      this.idProperty = property;
      return this.idProperty;
    }

    this.idProperty.addPart(property);

    return this.idProperty;
  }

  /**
   * Verifies the given bean type to no be {@literal null} and of the type of the current {@link
   * PersistentEntity}.
   *
   * @param bean must not be {@literal null}.
   */
  private void verifyBeanType(Object bean) {

    Assert.notNull(bean, "Target bean must not be null");
    Assert.isInstanceOf(
        getType(),
        bean,
        () -> String.format(TYPE_MISMATCH, bean.getClass().getName(), getType().getName()));
  }

  /**
   * A null-object implementation of {@link IdentifierAccessor} to be able to return an accessor for
   * entities that do not have an identifier property.
   */
  private static class AbsentIdentifierAccessor implements IdentifierAccessor {

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.IdentifierAccessor#getIdentifier()
     */
    @Override
    @Nullable
    public Object getIdentifier() {
      return null;
    }
  }

  /**
   * A class that checks the types of fields of a composite key. In addition, the correspondence
   * between types and field names marked with {@code @Id}.
   */
  public static class KeyPartTypeChecker {

    public static final String COMPOSITE_KEY_FIELDS_NUMBER_EXCEPTION =
        "Number of fields specified in domain class and composite class the key is different!";

    public static final String COMPOSITE_KEY_FIELD_DIFFERENT_EXCEPTION =
        "Domain class fields marked with @Id differ from fields specified in the composite key"
            + " class";

    /**
     * Check the number and types of entity fields, annotated {@code @Id} and fields {@link
     * CompositeKey} and if the types and quantities match return the id part fields mapping.
     *
     * @param persistentEntity persistent entity
     * @param idProperty id property
     * @return mapping between fields of composite key class and fields annotated {@code @Id} in
     *     entity.
     */
    public static Map<Field, Field> getFieldMapIfTypesValid(
        KeyValueCompositePersistentEntity<?, ?> persistentEntity, Identifier<?> idProperty) {
      Map<Field, Field> entityIdClassFields = new HashMap<>();

      List<Field> compositeKeyFields = persistentEntity.getIdClassTypeFields();

      Field[] entityFields = idProperty.getFields();

      if (compositeKeyFields.size() != entityFields.length) {
        throw new IllegalArgumentException(COMPOSITE_KEY_FIELDS_NUMBER_EXCEPTION);
      }

      for (int i = 0; i < compositeKeyFields.size(); i++) {
        Field compositeKeyField = compositeKeyFields.get(i);
        Field entityField = entityFields[i];

        if (!equalFields(compositeKeyField, entityField)) {
          throw new IllegalArgumentException(COMPOSITE_KEY_FIELD_DIFFERENT_EXCEPTION);
        }

        entityIdClassFields.put(entityField, compositeKeyField);
      }
      return entityIdClassFields;
    }

    /**
     * Compares two class fields by name and type.
     *
     * @param firstField first field
     * @param secondField second field
     * @return true if fields are equal
     */
    private static boolean equalFields(Field firstField, Field secondField) {
      return firstField.getName().equals(secondField.getName())
          && firstField.getType().equals(secondField.getType());
    }
  }
}
