/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.OnlyKeyValueOptions;
import io.tarantool.client.Options;
import io.tarantool.client.tdg.TarantoolDataGridSpace;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.tdg.TDGResponse;

/**
 * Class extends {@link IProtoCallClusterSpace} class and implementing {@link
 * TarantoolDataGridSpace}.
 *
 * <p>To use this class correctly, you can follow this example:
 *
 * <blockquote>
 *
 * <pre>{@code
 * // Creates box client with default settings.
 * TarantoolDataGridClient client = TarantoolDataGridClientImpl.builder().build();
 * ...
 *
 * TarantoolDataGridSpace space = client.space("spaceName");
 * ...
 *
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolDataGridSpace
 * @see TarantoolBalancer
 */
public class TarantoolDataGridSpaceImpl extends IProtoCallClusterSpace
    implements TarantoolDataGridSpace {

  /** Template for repository put function. */
  public static final String REPOSITORY_PUT = "repository.put";

  /** Template for repository get function. */
  public static final String REPOSITORY_GET = "repository.get";

  /** Template for repository delete function. */
  public static final String REPOSITORY_DELETE = "repository.delete";

  /** Template for repository find function. */
  public static final String REPOSITORY_FIND = "repository.find";

  /** Template for repository count function. */
  public static final String REPOSITORY_COUNT = "repository.count";

  /** Template for repository update function. */
  public static final String REPOSITORY_UPDATE = "repository.update";

  private static final OnlyKeyValueOptions DEFAULT_ONLY_KV_OPTIONS =
      OnlyKeyValueOptions.builder().build();

  /** Template type reference for single result value tdg result. */
  private final TypeReference<TDGResponse<Map<?, ?>>> TDG_RESPONSE_MAP =
      new TypeReference<TDGResponse<Map<?, ?>>>() {};

  /** Template type reference for many values tdg result. */
  private final TypeReference<TDGResponse<List<Map<?, ?>>>> TDG_RESPONSE_LIST_MAP =
      new TypeReference<TDGResponse<List<Map<?, ?>>>>() {};

  /** Template type reference for single integer tdg result. */
  private final TypeReference<TDGResponse<Integer>> TDG_RESPONSE_INTEGER =
      new TypeReference<TDGResponse<Integer>>() {};

  private final Boolean useTdg1Context;
  private final Map<String, Object> credentials;

  /**
   * This constructor creates {@link TarantoolDataGridSpaceImpl} based on the passed parameters.
   *
   * @param balancer see also: {@link #balancer}.
   * @param spaceName see also: {@link #spaceName}.
   */
  public TarantoolDataGridSpaceImpl(
      TarantoolBalancer balancer,
      String spaceName,
      Boolean useTdg1Context,
      Map<String, Object> credentials) {
    super(balancer, spaceName);
    this.useTdg1Context = useTdg1Context;
    this.credentials = credentials;
  }

  @Override
  public CompletableFuture<Map<?, ?>> put(Object tuple) {
    return put(tuple, DEFAULT_ONLY_KV_OPTIONS, Collections.emptyMap());
  }

  @Override
  public CompletableFuture<Map<?, ?>> put(
      Object tuple, OnlyKeyValueOptions options, Map<String, Object> context) {
    return tdgCallMultiResult(
            REPOSITORY_PUT, options, toArgs(tuple, options.getOptions(), context, credentials))
        .thenApply(this::getFirstOrNullForReturnAsClass);
  }

  @Override
  public CompletableFuture<Map<?, ?>> get(Object key) {
    return get(key, DEFAULT_ONLY_KV_OPTIONS);
  }

  @Override
  public CompletableFuture<Map<?, ?>> get(Object key, OnlyKeyValueOptions options) {
    if (useTdg1Context) {
      throw new UnsupportedOperationException("Unsupported for tdg1 version");
    }
    return tdgCallSingleResult(
        REPOSITORY_GET, options, TDG_RESPONSE_MAP, toArgs(key, options.getOptions(), credentials));
  }

  @Override
  public CompletableFuture<List<Map<?, ?>>> update(List<List<?>> filters, List<List<?>> updaters) {
    return update(filters, updaters, DEFAULT_ONLY_KV_OPTIONS, Collections.emptyMap());
  }

  @Override
  public CompletableFuture<List<Map<?, ?>>> update(
      List<List<?>> filters,
      List<List<?>> updaters,
      OnlyKeyValueOptions options,
      Map<String, Object> context) {
    return tdgCallMultiResult(
        REPOSITORY_UPDATE,
        options,
        toArgs(filters, updaters, options.getOptions(), context, credentials));
  }

  @Override
  public CompletableFuture<List<Map<?, ?>>> delete(List<List<?>> filters) {
    return delete(filters, DEFAULT_ONLY_KV_OPTIONS);
  }

  @Override
  public CompletableFuture<List<Map<?, ?>>> delete(
      List<List<?>> filters, OnlyKeyValueOptions options) {
    return tdgCallMultiResult(
        REPOSITORY_DELETE, options, toArgs(filters, options.getOptions(), credentials));
  }

  @Override
  public CompletableFuture<List<Map<?, ?>>> find(List<List<?>> filters) {
    return find(filters, DEFAULT_ONLY_KV_OPTIONS);
  }

  @Override
  public CompletableFuture<List<Map<?, ?>>> find(
      List<List<?>> filters, OnlyKeyValueOptions options) {
    return tdgCallMultiResult(
        REPOSITORY_FIND, options, toArgs(filters, options.getOptions(), credentials));
  }

  @Override
  public CompletableFuture<Integer> count(List<List<?>> filters) {
    return count(filters, DEFAULT_ONLY_KV_OPTIONS);
  }

  @Override
  public CompletableFuture<Integer> count(List<List<?>> filters, OnlyKeyValueOptions options) {
    return tdgCallSingleResult(
        REPOSITORY_COUNT,
        options,
        TDG_RESPONSE_INTEGER,
        toArgs(filters, options.getOptions(), credentials));
  }

  @Override
  public CompletableFuture<?> insertObject(Object object) {
    return put(object);
  }

  @Override
  public CompletableFuture<?> replaceObject(Object object) {
    return put(object);
  }

  @Override
  public CompletableFuture<?> delete(Object key) {
    throw new UnsupportedOperationException(
        "TDG doesn't support delete by PK without index/field name");
  }

  @Override
  public CompletableFuture<?> update(Object key, List<List<?>> operations) {
    throw new UnsupportedOperationException(
        "TDG doesn't support delete by PK  without index/field name");
  }

  private Object[] toArgs(Object... args) {
    if (args[0] == null) {
      throw new IllegalArgumentException("first argument can't be null");
    }
    return args;
  }

  private CompletableFuture<List<Map<?, ?>>> tdgCallMultiResult(
      String functionName, Options options, Object... args) {
    return TarantoolJacksonMapping.convertFutureResult(
            iprotoCall(options, functionName, args), TDG_RESPONSE_LIST_MAP)
        .thenApply(TarantoolResponse::get)
        .thenApply(TDGResponse::getData);
  }

  private <T> CompletableFuture<T> tdgCallSingleResult(
      String functionName,
      Options options,
      TypeReference<TDGResponse<T>> typeReference,
      Object... args) {
    return TarantoolJacksonMapping.convertFutureResult(
            iprotoCall(options, functionName, args), typeReference)
        .thenApply(TarantoolResponse::get)
        .thenApply(TDGResponse::getData);
  }
}
