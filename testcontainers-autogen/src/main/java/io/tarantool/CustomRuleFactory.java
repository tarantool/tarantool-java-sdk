/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool;

import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.util.ParcelableHelper;

public class CustomRuleFactory extends RuleFactory {

  public CustomRuleFactory() {
    super();
  }

  public CustomRuleFactory(
      GenerationConfig config, Jackson2Annotator jackson2Annotator, SchemaStore schemaStore) {
    super(config, jackson2Annotator, schemaStore);
  }

  @Override
  public Rule<JPackage, JType> getObjectRule() {
    return new SeparatedPackageObjectRule(this, new ParcelableHelper(), getReflectionHelper());
  }

  @Override
  public Rule<JFieldVar, JFieldVar> getDefaultRule() {
    return new WithoutDefaultRule(this);
  }
}
