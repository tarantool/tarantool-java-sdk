/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.query;

import org.springframework.data.keyvalue.core.CriteriaAccessor;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;

import io.tarantool.spring.data.query.TarantoolCriteria;

/**
 * <p>
 * Provide a mechanism to convert the abstract query into the direct implementation in Tarantool.
 * </P>
 *
 * @author Artyom Dubinin
 */
public class TarantoolCriteriaAccessor
    implements CriteriaAccessor<TarantoolCriteria> {

  /**
   * @param query in Spring form
   * @return The same in Tarantool form
   */
  public TarantoolCriteria resolve(KeyValueQuery<?> query) {
    return (TarantoolCriteria) query.getCriteria();
  }
}
