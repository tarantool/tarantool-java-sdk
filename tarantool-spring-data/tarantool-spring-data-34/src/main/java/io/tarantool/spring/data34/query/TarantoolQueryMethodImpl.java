/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import io.tarantool.spring.data.query.Query;
import io.tarantool.spring.data.query.TarantoolQueryMethod;

/**
 * Tarantool {@link QueryMethod} Implementation
 */
public class TarantoolQueryMethodImpl
    extends QueryMethod implements TarantoolQueryMethod {

  private final Method method;

  public TarantoolQueryMethodImpl(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
    super(method, metadata, factory);
    this.method = method;
  }

  public boolean hasAnnotatedQuery() {
    return getQuery() != null;
  }

  public String getQueryValue(Query query) {
    return (String) AnnotationUtils.getValue(query);
  }

  public Query getQuery() {
    return method.getAnnotation(Query.class);
  }
}
