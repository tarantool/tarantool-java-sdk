/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.repository.support;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.repository.support.SimpleKeyValueRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

import static io.tarantool.spring.data27.query.PaginationUtils.doPageQuery;
import io.tarantool.spring.data.query.TarantoolCriteria;

public class TarantoolSimpleRepository<T, ID> extends SimpleKeyValueRepository<T, ID> {

  private static final String FIND_ALL_EXC_MSG = "This method is not supported to avoid sampling huge data massive. "
      + "Please use derived methods with scrolling or pagination to select all data.";

  private static final String FIND_ALL_SORT_EXC_MSG = "This method is not supported because sorting is not supported "
      + "in Tarantool. Organize sorting of results using Java tools.";

  private static final String PAGEABLE_NULL_EXC_MSG = "Pageable must be not null";

  private final KeyValueOperations operations;
  private final EntityInformation<T, ID> entityInformation;

  /**
   * Creates a new {@link SimpleKeyValueRepository} for the given {@link EntityInformation} and
   * {@link KeyValueOperations}.
   *
   * @param metadata   must not be {@literal null}.
   * @param operations must not be {@literal null}.
   */
  public TarantoolSimpleRepository(EntityInformation<T, ID> metadata, KeyValueOperations operations) {
    super(metadata, operations);
    this.entityInformation = metadata;
    this.operations = operations;
  }

  @Override
  public List<T> findAll() {
    throw new UnsupportedOperationException(FIND_ALL_EXC_MSG);
  }

  @Override
  public Iterable<T> findAll(Sort sort) {
    throw new UnsupportedOperationException(FIND_ALL_SORT_EXC_MSG);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Page<T> findAll(Pageable pageable) {
    Assert.notNull(pageable, PAGEABLE_NULL_EXC_MSG);

    KeyValueQuery<?> query = new KeyValueQuery<>(new TarantoolCriteria());
    return (Page<T>) doPageQuery(pageable, query, this.operations, this.entityInformation.getJavaType());
  }
}
