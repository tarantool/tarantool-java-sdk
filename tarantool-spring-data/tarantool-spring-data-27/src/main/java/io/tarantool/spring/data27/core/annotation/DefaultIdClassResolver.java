/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.core.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.tarantool.spring.data.core.annotation.IdClass;
import io.tarantool.spring.data.core.annotation.IdClassResolver;

/**
 * Default implementation of {@link IdClassResolver}.
 */
public enum DefaultIdClassResolver implements IdClassResolver {
  INSTANCE;

  public static final String ANNOTATION_TYPE_EXCEPTION =
      "The class of a composite identifier specified in the @IdClass annotation cannot be annotation!";

  @Nullable
  @Override
  public Class<?> resolveIdClassType(Class<?> type) {
    Assert.notNull(type, "Type for IdClass must be not null!");

    IdClass idClassTypeAnnotation = AnnotatedElementUtils.findMergedAnnotation(type, IdClass.class);

    if (idClassTypeAnnotation == null) {
      return null;
    }

    Class<?> idClassTypeValue = idClassTypeAnnotation.value();
    Assert.isTrue(!idClassTypeValue.isAnnotation(), ANNOTATION_TYPE_EXCEPTION);

    return idClassTypeValue;
  }
}
