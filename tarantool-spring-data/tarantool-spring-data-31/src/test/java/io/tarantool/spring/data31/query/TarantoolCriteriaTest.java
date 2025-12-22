/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.query;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import static io.tarantool.client.crud.ConditionOperator.EQ;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.spring.data.query.Conditions;
import io.tarantool.spring.data.query.TarantoolCriteria;

public class TarantoolCriteriaTest {

  @Test
  void testAddAndGetConditions() {
    final TarantoolCriteria tarantoolCriteria = new TarantoolCriteria();
    final String identifier = "name";
    final Object value = "value";
    final String operator = "==";

    final Condition condition = Condition.create(operator, identifier, value);
    assertEquals(operator, condition.getOperator());
    assertEquals(identifier, condition.getFieldIdentifier());
    assertEquals(value, condition.getValue());

    final Condition conditionConstructor = new Condition(operator, identifier, value);
    assertEquals(operator, conditionConstructor.getOperator());
    assertEquals(identifier, conditionConstructor.getFieldIdentifier());
    assertEquals(value, conditionConstructor.getValue());

    final Condition conditionFromBuilder =
        Condition.builder()
            .withOperator(operator)
            .withValue(value)
            .withFieldIdentifier(identifier)
            .build();
    assertEquals(operator, conditionFromBuilder.getOperator());
    assertEquals(identifier, conditionFromBuilder.getFieldIdentifier());
    assertEquals(value, conditionFromBuilder.getValue());

    final Condition conditionWithEnum = Condition.create(EQ, identifier, value);
    assertEquals(operator, conditionWithEnum.getOperator());
    assertEquals(identifier, conditionWithEnum.getFieldIdentifier());
    assertEquals(value, conditionWithEnum.getValue());

    final Condition conditionConstructorWithEnum = new Condition(EQ, identifier, value);
    assertEquals(operator, conditionConstructorWithEnum.getOperator());
    assertEquals(identifier, conditionConstructorWithEnum.getFieldIdentifier());
    assertEquals(value, conditionConstructorWithEnum.getValue());

    final Condition conditionFromBuilderWithEnum =
        Condition.builder()
            .withOperator(EQ)
            .withValue(value)
            .withFieldIdentifier(identifier)
            .build();
    assertEquals(operator, conditionFromBuilderWithEnum.getOperator());
    assertEquals(identifier, conditionFromBuilderWithEnum.getFieldIdentifier());
    assertEquals(value, conditionFromBuilderWithEnum.getValue());

    tarantoolCriteria.addCondition(condition);
    tarantoolCriteria.addCondition(conditionConstructor);
    tarantoolCriteria.addCondition(conditionFromBuilder);

    tarantoolCriteria.addCondition(conditionWithEnum);
    tarantoolCriteria.addCondition(conditionConstructorWithEnum);
    tarantoolCriteria.addCondition(conditionFromBuilderWithEnum);

    final Conditions expectedConditions = new Conditions();
    expectedConditions.addAll(
        Arrays.asList(
            condition,
            conditionConstructor,
            conditionFromBuilder,
            conditionWithEnum,
            conditionConstructorWithEnum,
            conditionFromBuilderWithEnum));

    assertEquals(expectedConditions, tarantoolCriteria.getConditions());
  }

  @Test
  void testFirst() {
    final TarantoolCriteria criteria = new TarantoolCriteria();
    criteria.withFirst(1);

    final String key = "first";
    SelectOptions options = criteria.getOptions();
    assertEquals(1, options.getOptions().get(key));

    criteria.clear();
    criteria.withFirst(-1);
    options = criteria.getOptions();
    assertEquals(-1, options.getOptions().get(key));
  }
}
