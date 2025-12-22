/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static io.tarantool.client.crud.ConditionOperator.EQ;
import static io.tarantool.client.crud.ConditionOperator.GREATER;
import static io.tarantool.client.crud.ConditionOperator.GREATER_EQ;
import static io.tarantool.client.crud.ConditionOperator.LESS;
import static io.tarantool.client.crud.ConditionOperator.LESS_EQ;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.ConditionOperator;
import io.tarantool.spring.data.query.TarantoolCriteria;

public class TarantoolQueryCreator extends AbstractQueryCreator<KeyValueQuery<TarantoolCriteria>, TarantoolCriteria> {

  private final Map<Part, String> cache = new ConcurrentHashMap<>();

  public static final String INVALID_DATA_ACCESS_API_USAGE_EXCEPTION_MESSAGE_TEMPLATE =
      "Logic error for '%s' in query. ";

  public TarantoolQueryCreator(PartTree tree, ParameterAccessor parameters) {
    super(tree, parameters);
  }

  private String getFieldName(Part part) {
    PropertyPath property = part.getProperty();
    String segment = property.toDotPath();
    Class<?> domainType = property.getOwningType().getType();
    java.lang.reflect.Field field = ReflectionUtils.findField(domainType, segment);

    if (field == null) {
      throw new IllegalArgumentException("No such field: " + segment + " in " + domainType);
    }
    Field annotationField = field.getAnnotation(Field.class);

    if (annotationField != null) {
      String value = annotationField.value();
      if (StringUtils.hasText(value)) {
        return value;
      }
      String name = annotationField.name();
      if (StringUtils.hasText(name)) {
        return name;
      }
    }

    return segment;
  }

  @Override
  @NonNull
  protected TarantoolCriteria create(Part part, @NonNull Iterator<Object> iterator) {
    final TarantoolCriteria tarantoolCriteria = new TarantoolCriteria();

    Part.Type type = part.getType();
    if (isIgnoreCase(part)) {
      throw new InvalidDataAccessApiUsageException(String.format(
          INVALID_DATA_ACCESS_API_USAGE_EXCEPTION_MESSAGE_TEMPLATE + "IgnoreCase isn't supported yet", type));
    }
    String property = cache.computeIfAbsent(part, this::getFieldName);

    switch (type) {
      case SIMPLE_PROPERTY:
        generateConditions(tarantoolCriteria, type, property, iterator, EQ);
        break;
      case LESS_THAN:
      case BEFORE:
        generateConditions(tarantoolCriteria, type, property, iterator, LESS);
        break;
      case LESS_THAN_EQUAL:
        generateConditions(tarantoolCriteria, type, property, iterator, LESS_EQ);
        break;
      case GREATER_THAN:
      case AFTER:
        generateConditions(tarantoolCriteria, type, property, iterator, GREATER);
        break;
      case GREATER_THAN_EQUAL:
        generateConditions(tarantoolCriteria, type, property, iterator, GREATER_EQ);
        break;
      case BETWEEN:
        generateConditions(tarantoolCriteria, type, property, iterator, GREATER, LESS);
        break;
      case TRUE:
        tarantoolCriteria.addCondition(Condition.create(EQ, property, true));
        break;
      case FALSE:
        tarantoolCriteria.addCondition(Condition.create(EQ, property, false));
        break;
      case IS_EMPTY:
        tarantoolCriteria.addCondition(Condition.create(EQ, property, ""));
        break;
      case IS_NULL:
        tarantoolCriteria.addCondition(Condition.create(EQ, property, null));
        break;
      default:
        throw new InvalidDataAccessApiUsageException(String.format("Unsupported type '%s'", type));
    }

    return tarantoolCriteria;
  }

  private void generateConditions(TarantoolCriteria tarantoolCriteria, Part.Type type, String property,
      Iterator<Object> iterator, ConditionOperator... operators) {
    for (int i = 0; i < type.getNumberOfArguments(); i++) {
      if (!iterator.hasNext()) {
        throw new InvalidDataAccessApiUsageException(String.format(
            INVALID_DATA_ACCESS_API_USAGE_EXCEPTION_MESSAGE_TEMPLATE + "Transmitted not enough arguments (%d of %d)",
            type,
            i,
            type.getNumberOfArguments()));
      }
      tarantoolCriteria.addCondition(Condition.create(operators[i], property, iterator.next()));
    }
  }

  private boolean isIgnoreCase(Part part) {
    switch (part.shouldIgnoreCase()) {
      case ALWAYS:
        Assert.state(canUpperCase(part.getProperty()),
            String.format("Unable to ignore case of %s types, the property '%s' must reference a String",
                part.getProperty().getType().getName(), part.getProperty().getSegment()));
        return true;
      case WHEN_POSSIBLE:
        return canUpperCase(part.getProperty());
      case NEVER:
      default:
        return false;
    }
  }

  private boolean canUpperCase(PropertyPath path) {
    return String.class.equals(path.getType());
  }

  @Override
  @NonNull
  protected TarantoolCriteria and(@NonNull Part part, @NonNull TarantoolCriteria base,
      @NonNull Iterator<Object> iterator) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull
  protected TarantoolCriteria or(@NonNull TarantoolCriteria base, @NonNull TarantoolCriteria criteria) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull
  protected KeyValueQuery<TarantoolCriteria> complete(@Nullable final TarantoolCriteria criteria, @NonNull Sort sort) {
    return new KeyValueQuery<>(criteria);
  }
}
