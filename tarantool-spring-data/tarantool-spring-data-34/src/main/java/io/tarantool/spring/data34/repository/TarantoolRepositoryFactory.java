/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.repository;

import java.util.Optional;

import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.query.SpelQueryCreator;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data34.query.TarantoolPartTreeQuery;
import io.tarantool.spring.data34.repository.config.TarantoolQueryLookupStrategy;
import io.tarantool.spring.data34.repository.support.TarantoolSimpleRepository;

/**
 * <p>
 * Tarantool version of {@link KeyValueRepositoryFactory}, a factory to build tarantool repository instances.
 * </P>
 * <p>
 * The purpose of extending is to ensure that the {@link #getQueryLookupStrategy} method returns a
 * {@link TarantoolQueryLookupStrategy} rather than the default.
 * </P>
 * <p>
 * The end goal of this bean is for {@link TarantoolPartTreeQuery} to be used for query preparation.
 * </P>
 *
 * @author Artyom Dubinin
 */
public class TarantoolRepositoryFactory
    extends KeyValueRepositoryFactory {

  private static final Class<SpelQueryCreator> DEFAULT_QUERY_CREATOR = SpelQueryCreator.class;

  private final KeyValueOperations keyValueOperations;
  private final Class<? extends AbstractQueryCreator<?, ?>> queryCreator;
  private final TarantoolCrudClient client;

  /* Mirror functionality of super, to ensure private
   * fields are set.
   */
  public TarantoolRepositoryFactory(TarantoolCrudClient client, KeyValueOperations keyValueOperations) {
    this(client, keyValueOperations, DEFAULT_QUERY_CREATOR);
  }

  /* Capture KeyValueOperations and QueryCreator objects after passing to super.
   */
  public TarantoolRepositoryFactory(TarantoolCrudClient client, KeyValueOperations keyValueOperations,
      Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {

    super(keyValueOperations, queryCreator);

    this.client = client;
    this.keyValueOperations = keyValueOperations;
    this.queryCreator = queryCreator;
  }

  /**
   * <p>
   * Ensure the mechanism for query evaluation is Tarantool specific, as the original
   * {@code KeyValueQueryLookupStrategy} does not function correctly for Tarantool.
   * </P>
   */
  @Override
  protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
      QueryMethodEvaluationContextProvider evaluationContextProvider) {
    return Optional.of(new TarantoolQueryLookupStrategy(client, key, evaluationContextProvider, keyValueOperations,
        queryCreator));
  }

  @Override
  protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
    return TarantoolSimpleRepository.class;
  }
}
