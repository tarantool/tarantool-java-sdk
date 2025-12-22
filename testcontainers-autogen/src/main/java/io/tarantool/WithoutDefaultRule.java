/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.DefaultRule;
import org.jsonschema2pojo.rules.RuleFactory;

/** Implementation of {@link DefaultRule} that ignore default values */
public class WithoutDefaultRule extends DefaultRule {

  private final RuleFactory ruleFactory;

  public WithoutDefaultRule(RuleFactory ruleFactory) {
    super(ruleFactory);
    this.ruleFactory = ruleFactory;
  }

  /**
   * Applies this schema rule to take the required code generation steps.
   *
   * <p>Default values are implemented by assigning an expression to the given field (so when
   * instances of the generated POJO are created, its fields will then contain their default
   * values).
   *
   * <p>Collections (Lists and Sets) are initialized to an empty collection, even when no default
   * value is present in the schema (node is null).
   *
   * @param nodeName the name of the property which has (or may have) a default
   * @param node the default node (may be null if no default node was present for this property)
   * @param field the Java field that has added to a generated type to represent this property
   * @return field, which will have an init expression is appropriate
   */
  @Override
  public JFieldVar apply(
      String nodeName, JsonNode node, JsonNode parent, JFieldVar field, Schema currentSchema) {

    String fieldType = field.type().fullName();

    if (fieldType.startsWith(List.class.getName())) {
      field.init(getDefaultEmptyCollection(field.type(), ArrayList.class));
    } else if (fieldType.startsWith(Set.class.getName())) {
      field.init(getDefaultEmptyCollection(field.type(), LinkedHashSet.class));
    }
    return field;
  }

  private JExpression getDefaultEmptyCollection(JType fieldType, Class<?> clazz) {
    final JClass listGenericType = ((JClass) fieldType).getTypeParameters().get(0);

    JClass listImplClass = fieldType.owner().ref(clazz);
    listImplClass = listImplClass.narrow(listGenericType);
    JInvocation newCollection = JExpr._new(listImplClass);
    if (!ruleFactory.getGenerationConfig().isInitializeCollections()) {
      return null;
    }
    return newCollection;
  }
}
