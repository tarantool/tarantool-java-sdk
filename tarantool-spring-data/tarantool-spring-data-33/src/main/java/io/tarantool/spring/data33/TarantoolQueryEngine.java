/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;

import org.springframework.data.keyvalue.core.QueryEngine;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data.ProxyTarantoolQueryEngine;
import io.tarantool.spring.data.query.TarantoolCriteria;
import io.tarantool.spring.data33.query.TarantoolCriteriaAccessor;
import io.tarantool.spring.data33.query.TarantoolSortAccessor;

/**
 * <p>
 * Implementation of {@code findBy*()} and {@code countBy*{}} queries.
 * </P>
 *
 * @author Artyom Dubinin
 */
public class TarantoolQueryEngine
    extends QueryEngine<TarantoolCrudKeyValueAdapter, TarantoolCriteria, Comparator<Entry<?, ?>>> {

  private final ProxyTarantoolQueryEngine engine;

  public TarantoolQueryEngine(TarantoolCrudClient client) {
    super(new TarantoolCriteriaAccessor(), new TarantoolSortAccessor());
    this.engine = new ProxyTarantoolQueryEngine(client);
  }

  @Override
  @NonNull
  public Collection<?> execute(@Nullable final TarantoolCriteria criteria,
      @Nullable final Comparator<Entry<?, ?>> sort,
      final long offset,
      final int rows,
      @NonNull final String keyspace) {
    return engine.execute(criteria, sort, offset, rows, keyspace);
  }

  /**
   * <p>
   * Construct the final query predicate for Tarantool to execute, from the base query plus any paging and sorting.
   * </P>
   * <p>
   * Variations here allow the base query predicate to be omitted, sorting to be omitted, and paging to be omitted.
   * </P>
   *
   * @param criteria Search criteria, null means match everything
   * @param sort     Possibly null collation
   * @param offset   Start point of returned page, -1 if not used
   * @param rows     Size of page, -1 if not used
   * @param keyspace The map name
   * @return Results from Tarantool
   */
  @Override
  @NonNull
  public <T> Collection<T> execute(@Nullable final TarantoolCriteria criteria,
      @Nullable final Comparator<Entry<?, ?>> sort,
      final long offset,
      final int rows,
      @NonNull final String keyspace,
      @NonNull Class<T> type) {
    return engine.execute(criteria, sort, offset, rows, keyspace, type);
  }

  /**
   * <p>
   * Execute {@code countBy*()} queries against a Tarantool space.
   * </P>
   *
   * @param criteria Predicate to use, not null
   * @param keyspace The map name
   * @return Results from Tarantool
   */
  @Override
  public long count(@Nullable final TarantoolCriteria criteria, @NonNull final String keyspace) {
    return engine.count(criteria, keyspace);
  }
}
