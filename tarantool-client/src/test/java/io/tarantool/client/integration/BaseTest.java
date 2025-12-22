/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.tarantool.mapping.SelectResponse;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;

public abstract class BaseTest {

  protected static final String API_USER = "api_user";

  protected static final Map<String, String> CREDS = new HashMap<String, String>(){{
    put(API_USER, "secret");
  }};

  protected static final Map<String, String> ENV_MAP = new HashMap<String, String>(){{
    put("TARANTOOL_USER_NAME", API_USER);
    put("TARANTOOL_USER_PASSWORD", CREDS.get(API_USER));
  }};

  /**
   * Gets collection of data without object-wrapper like {@link TarantoolResponse} or {@link SelectResponse}.
   *
   * @return the collection of data
   */
  public List<?> unpack(SelectResponse<List<Tuple<List<?>>>> response) {
    return response.get().stream().map(Tuple::get).collect(Collectors.toList());
  }

  /**
   * Gets collection of data without object-wrapper like {@link TarantoolResponse} or {@link SelectResponse} for Target
   * class.
   *
   * @return the collection of data
   */
  public <T> List<T> unpackT(SelectResponse<List<Tuple<T>>> response) {
    return response.get().stream().map(Tuple::get).collect(Collectors.toList());
  }

  public List<List<?>> unpack(List<Tuple<List<?>>> response) {
    return response.stream().map(Tuple::get).collect(Collectors.toList());
  }

  public <T> List<T> unpackT(List<Tuple<T>> response) {
    return response.stream().map(Tuple::get).collect(Collectors.toList());
  }
}
