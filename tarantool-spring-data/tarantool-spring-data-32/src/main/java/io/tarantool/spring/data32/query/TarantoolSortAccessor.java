/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.query;

import java.util.Comparator;
import java.util.Map.Entry;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.keyvalue.core.SortAccessor;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;

/**
 * <p>
 * Implements sorting for Tarantool repository queries.
 * </P>
 *
 * @author Artyom Dubinin
 */
public class TarantoolSortAccessor
    implements SortAccessor<Comparator<Entry<?, ?>>> {

  /**
   * <p>
   * Sort on a sequence of fields, possibly none.
   * </P>
   *
   * @param query If not null, will contain one of more {@link Order} objects.
   * @return A sequence of comparators or {@code null}
   */
  public Comparator<Entry<?, ?>> resolve(KeyValueQuery<?> query) {
    if (query == null || query.getSort() == Sort.unsorted()) {
      return null;
    }

    throw new UnsupportedOperationException();
  }

}
