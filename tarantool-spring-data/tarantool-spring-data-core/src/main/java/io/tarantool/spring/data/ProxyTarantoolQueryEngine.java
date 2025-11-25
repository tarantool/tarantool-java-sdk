/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import static io.tarantool.spring.data.Helper.assertNotNull;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.mapping.Tuple;
import io.tarantool.spring.data.query.TarantoolCriteria;

/**
 * Implementation of {@code findBy*()} and {@code countBy*{}} queries.
 *
 * @author Artyom Dubinin
 */
public class ProxyTarantoolQueryEngine {

  private final TarantoolCrudClient client;

  public ProxyTarantoolQueryEngine(TarantoolCrudClient client) {
    assertNotNull(client, "tarantoolCrudClient must be not null");
    this.client = client;
  }

  public Collection<?> execute(
      final TarantoolCriteria criteria,
      final Comparator<Entry<?, ?>> sort,
      final long offset,
      final int rows,
      final String keyspace) {
    assertNotNull(criteria, "criteria must be not null");
    criteria.withFirst(rows);
    // TODO сделать для всех опций hashcode и equals для того, чтобы можно было реализовать
    // кэширование
    return client.space(keyspace).select(criteria.getConditions(), criteria.getOptions()).join();
  }

  /**
   * Construct the final query predicate for Tarantool to execute, from the base query plus any
   * paging and sorting.
   *
   * <p>Variations here allow the base query predicate to be omitted, sorting to be omitted, and
   * paging to be omitted.
   *
   * @param criteria Search criteria, null means match everything
   * @param sort Possibly null collation
   * @param offset Start point of returned page, -1 if not used
   * @param rows Size of page, -1 if not used
   * @param keyspace The map name
   * @param type return target type entity
   * @param <T> return entity
   * @return Results from Tarantool
   */
  public <T> Collection<T> execute(
      final TarantoolCriteria criteria,
      final Comparator<Entry<?, ?>> sort,
      final long offset,
      final int rows,
      final String keyspace,
      Class<T> type) {
    assertNotNull(criteria, "criteria must be not null");
    criteria.withFirst(rows);
    // TODO: сделать для всех опций hashcode и equals для того, чтобы можно было реализовать
    // кэширование
    // TODO: unwrap tuples in more elegant way (e.g. by option in select)
    return unwrapTuples(
        client
            .space(keyspace)
            .select(criteria.getConditions(), criteria.getOptions(), type)
            .join());
  }

  public static <T> List<T> unwrapTuples(List<Tuple<T>> tuples) {
    ArrayList<T> result = new ArrayList<>();
    for (Tuple<T> t : tuples) {
      result.add(t.get());
    }
    return result;
  }

  /**
   * Execute {@code countBy*()} queries against a Tarantool space.
   *
   * @param criteria Predicate to use, not null
   * @param keyspace The map name
   * @return Results from Tarantool
   */
  public long count(final TarantoolCriteria criteria, final String keyspace) {
    assertNotNull(criteria, "criteria must be not null");
    return client.space(keyspace).count(criteria.getConditions()).join();
  }
}
