/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.repository.config;

import java.lang.reflect.Method;

import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.util.Assert;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data35.query.TarantoolPartTreeQuery;
import io.tarantool.spring.data35.query.TarantoolQueryMethodImpl;
import io.tarantool.spring.data35.query.TarantoolRepositoryQuery;

/**
 * Ensures {@link TarantoolPartTreeQuery} is used for query preparation rather than {@link
 * KeyValuePartTreeQuery} or other alternatives.
 *
 * @author Artyom Dubinin
 */
public class TarantoolQueryLookupStrategy implements QueryLookupStrategy {

  private final QueryMethodEvaluationContextProvider evaluationContextProvider;
  private final KeyValueOperations keyValueOperations;
  private final Class<? extends AbstractQueryCreator<?, ?>> queryCreator;
  private final TarantoolCrudClient client;

  /**
   * Required constructor, capturing arguments for use in {@link #resolveQuery}.
   *
   * @param client tarantool crud client
   * @param key Not used
   * @param evaluationContextProvider For evaluation of query expressions
   * @param keyValueOperations Bean to use for Key/Value operations on Tarantool repos
   * @param queryCreator Query creator
   */
  public TarantoolQueryLookupStrategy(
      TarantoolCrudClient client,
      Key key,
      QueryMethodEvaluationContextProvider evaluationContextProvider,
      KeyValueOperations keyValueOperations,
      Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {

    Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
    Assert.notNull(keyValueOperations, "KeyValueOperations must not be null!");
    Assert.notNull(queryCreator, "Query creator type must not be null!");

    this.client = client;
    this.evaluationContextProvider = evaluationContextProvider;
    this.keyValueOperations = keyValueOperations;
    this.queryCreator = queryCreator;
  }

  /**
   * Use {@link TarantoolPartTreeQuery} for resolving queries against Tarantool repositories.
   *
   * @param method, the query method
   * @param metadata, not used
   * @param projectionFactory, not used
   * @param namedQueries, not used
   * @return A mechanism for querying Tarantool repositories
   */
  public RepositoryQuery resolveQuery(
      Method method,
      RepositoryMetadata metadata,
      ProjectionFactory projectionFactory,
      NamedQueries namedQueries) {

    TarantoolQueryMethodImpl queryMethod =
        new TarantoolQueryMethodImpl(method, metadata, projectionFactory);

    if (queryMethod.hasAnnotatedQuery()) {
      return new TarantoolRepositoryQuery(client, queryMethod);
    }

    return new TarantoolPartTreeQuery(
        queryMethod, evaluationContextProvider, this.keyValueOperations, this.queryCreator);
  }
}
