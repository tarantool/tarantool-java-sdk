/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.query;

import org.springframework.data.keyvalue.core.CriteriaAccessor;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;

import io.tarantool.spring.data.query.TarantoolCriteria;

/**
 * Provide a mechanism to convert the abstract query into the direct implementation in Tarantool.
 *
 * @author Artyom Dubinin
 */
public class TarantoolCriteriaAccessor implements CriteriaAccessor<TarantoolCriteria> {

  /**
   * @param query in Spring form
   * @return The same in Tarantool form
   */
  public TarantoolCriteria resolve(KeyValueQuery<?> query) {
    return (TarantoolCriteria) query.getCriteria();
  }
}
