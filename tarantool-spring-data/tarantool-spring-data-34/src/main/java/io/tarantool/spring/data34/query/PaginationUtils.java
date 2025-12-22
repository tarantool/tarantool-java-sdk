/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.keyvalue.core.IterableConverter;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.util.Assert;

import io.tarantool.spring.data.query.PaginationDirection;
import io.tarantool.spring.data.query.TarantoolCriteria;

public class PaginationUtils {

  private static final String CRITERIA_NULL_EXC_MSG = "TarantoolCriteria must be not null";

  /**
   * Returns {@link TarantoolPageable} cast from {@link Pageable} with type checking.
   *
   * @param sliceParams pageable
   * @return {@link TarantoolPageable}
   */
  public static TarantoolPageable<?> castToTarantoolPageable(Pageable sliceParams) {
    Assert.isInstanceOf(TarantoolPageable.class, sliceParams, "Pageable must be TarantoolPageable");
    return (TarantoolPageable<?>) sliceParams;
  }

  /**
   * A general method for retrieving data for paginated queries.
   *
   * @param query       query
   * @param pageRequest {@link TarantoolPageable}
   * @param pageSize    page size
   * @return selection result.
   */
  public static List<?> doPaginationQuery(final KeyValueQuery<?> query, TarantoolPageable<?> pageRequest,
      int pageSize, KeyValueOperations keyValueOperations, Class<?> targetType) {

    TarantoolCriteria criteria = (TarantoolCriteria) query.getCriteria();
    PaginationDirection paginationDirection = pageRequest.getPaginationDirection();

    Assert.notNull(criteria, CRITERIA_NULL_EXC_MSG);
    criteria.withAfter(pageRequest.getTupleCursor());

    query.setRows(pageSize * paginationDirection.getMultiplier());

    return IterableConverter.toList(keyValueOperations.find(query, targetType));
  }


  public static Page<?> doPageQuery(Pageable pageable, KeyValueQuery<?> query, KeyValueOperations keyValueOperations,
      Class<?> targetType) {
    if (pageable.isUnpaged()) {
      return new TarantoolPageImpl<>();
    }
    TarantoolPageable<?> resultSliceParams = castToTarantoolPageable(pageable);

    int pageSize = resultSliceParams.getPageSize();

    // non transactional calls
    List<?> content = doPaginationQuery(query, resultSliceParams, pageSize, keyValueOperations, targetType);

    if (content.isEmpty()) {
      return new TarantoolPageImpl<>();
    }

    long totalElements = keyValueOperations.count(query, targetType);
    return new TarantoolPageImpl<>(content, resultSliceParams, totalElements);
  }
}
