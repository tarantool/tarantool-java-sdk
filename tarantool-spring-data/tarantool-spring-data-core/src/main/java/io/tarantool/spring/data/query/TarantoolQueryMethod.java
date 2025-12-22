/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

/**
 * Tarantool {@code QueryMethod} Implementation
 */
public interface TarantoolQueryMethod {

  boolean hasAnnotatedQuery();

  String getQueryValue(Query query);

  Query getQuery();
}
