/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.repository.support;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data32.repository.TarantoolRepositoryFactory;

public class TarantoolRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
    extends KeyValueRepositoryFactoryBean<T, S, ID> {

  @Autowired
  private TarantoolCrudClient client;

  public TarantoolRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
    super(repositoryInterface);
  }

  /**
   * <p>
   * Return a {@link TarantoolRepositoryFactory}.
   * </P>
   * <p>
   * {@code super} would return {@link KeyValueRepositoryFactory} which in turn builds {@code KeyValueRepository}
   * instances, and these have a private method that implement querying in a manner that does not fit with Tarantool.
   * More details are in {@link TarantoolRepositoryFactory}.
   * </P>
   *
   * @param operations           operations
   * @param queryCreator         creator
   * @param repositoryQueryType, not used
   * @return A {@link TarantoolRepositoryFactory} that creates tarantool repository instances.
   */
  @Override
  protected KeyValueRepositoryFactory createRepositoryFactory(KeyValueOperations operations,
      Class<? extends AbstractQueryCreator<?, ?>> queryCreator,
      Class<? extends RepositoryQuery> repositoryQueryType) {
    return new TarantoolRepositoryFactory(client, operations, queryCreator);
  }
}
