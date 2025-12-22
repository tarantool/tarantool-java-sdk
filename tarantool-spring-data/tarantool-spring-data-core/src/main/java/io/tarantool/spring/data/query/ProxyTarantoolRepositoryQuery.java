/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

import java.util.Arrays;

import io.tarantool.client.crud.TarantoolCrudClient;

/**
 * {@code RepositoryQuery} using String based function name to call function in Tarantool.
 */
public class ProxyTarantoolRepositoryQuery {

  private final TarantoolQueryMethod queryMethod;
  private final TarantoolCrudClient client;

  public ProxyTarantoolRepositoryQuery(TarantoolCrudClient client, TarantoolQueryMethod queryMethod) {
    this.client = client;
    this.queryMethod = queryMethod;
  }

  public Object execute(Object[] parameters) {
    Query query = queryMethod.getQuery();
    String value = queryMethod.getQueryValue(query);
    if (query.mode().equals(QueryMode.CALL)) {
      return client.call(value, Arrays.asList(parameters)).join();
    }
    return client.eval(value, Arrays.asList(parameters)).join();
  }
}
