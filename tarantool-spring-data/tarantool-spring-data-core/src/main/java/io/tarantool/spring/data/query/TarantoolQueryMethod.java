/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

/** Tarantool {@code QueryMethod} Implementation */
public interface TarantoolQueryMethod {

  boolean hasAnnotatedQuery();

  String getQueryValue(Query query);

  Query getQuery();
}
