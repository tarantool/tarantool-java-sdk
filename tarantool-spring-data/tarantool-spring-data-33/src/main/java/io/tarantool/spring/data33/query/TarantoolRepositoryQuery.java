/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.query;

import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data.query.ProxyTarantoolRepositoryQuery;

/**
 * {@link RepositoryQuery} using String based function name to call function in Tarantool.
 */
public class TarantoolRepositoryQuery
    implements RepositoryQuery {

  private final ProxyTarantoolRepositoryQuery proxy;
  private final TarantoolQueryMethodImpl queryMethod;

  public TarantoolRepositoryQuery(TarantoolCrudClient client, TarantoolQueryMethodImpl queryMethod) {
    this.queryMethod = queryMethod;
    this.proxy = new ProxyTarantoolRepositoryQuery(client, queryMethod);
  }

  @Override
  public Object execute(Object[] parameters) {
    return proxy.execute(parameters);
  }

  @Override
  public QueryMethod getQueryMethod() {
    return queryMethod;
  }
}
