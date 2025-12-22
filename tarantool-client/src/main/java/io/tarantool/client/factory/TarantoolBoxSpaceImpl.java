/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import static io.tarantool.core.IProtoFeature.SPACE_AND_INDEX_NAMES;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.BaseOptions;
import io.tarantool.client.Options;
import io.tarantool.client.box.TarantoolBoxSpace;
import io.tarantool.client.box.options.DeleteOptions;
import io.tarantool.client.box.options.OptionsWithIndex;
import io.tarantool.client.box.options.SelectOptions;
import io.tarantool.client.box.options.UpdateOptions;
import io.tarantool.client.operation.Operations;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.SelectAfterMode;
import io.tarantool.mapping.SelectResponse;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.schema.Index;
import io.tarantool.schema.NoSchemaException;
import io.tarantool.schema.Space;
import io.tarantool.schema.TarantoolSchemaFetcher;

/**
 * <p>Class extends {@link AbstractTarantoolSpace} class and implementing {@link TarantoolBoxSpace}.</p>
 * <p>To use this class correctly, you can follow this example:</p>
 * <blockquote><pre>{@code
 *
 * // Creates box client with default settings.
 *  TarantoolBoxClient boxClient = TarantoolBoxClientImpl.builder().build();
 *  ...
 *
 *  TarantoolBoxSpace space = boxClient.space("spaceName");
 *  ...
 *
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolBoxSpace
 * @see OptionsWithIndex
 * @see TarantoolSchemaFetcher
 * @see TarantoolBalancer
 */
final class TarantoolBoxSpaceImpl extends AbstractTarantoolSpace implements TarantoolBoxSpace {

  /**
   * <p>{@link SelectOptions} default value.</p>
   */
  private static final SelectOptions defaultSelectOptions = SelectOptions.builder().build();

  /**
   * <p>{@link DeleteOptions} default value.</p>
   */
  private static final DeleteOptions defaultDeleteOptions = DeleteOptions.builder().build();

  /**
   * <p>{@link UpdateOptions} default value.</p>
   */
  private static final UpdateOptions defaultUpdateOptions = UpdateOptions.builder().build();

  /**
   * <p>{@link Options} default value.</p>
   */
  private static final Options defaultOptions = BaseOptions.builder().build();


  /**
   * <p>An object that fetches a new information about a schema only if version of schema is changed.</p>
   *
   * @see TarantoolSchemaFetcher
   */
  private final TarantoolSchemaFetcher fetcher;

  /**
   * <p>The balancer used for connect selection to send request</p>
   *
   * @see TarantoolBalancer
   */
  private final TarantoolBalancer balancer;

  /**
   * <p>Space id.</p>
   *
   * @see <a href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/id/">Space id</a>
   */
  private Integer spaceId;

  /**
   * <p>Space name.</p>
   */
  private String spaceName;

  /**
   * <p>{@link Space} object.</p>
   */
  private Space space;

  /**
   * <p> This constructor creates {@link TarantoolBoxSpaceImpl} based on the passed parameters.</p>
   *
   * @param balancer see also: {@link #balancer}.
   * @param spaceId  see also: {@link #spaceId}.
   * @param fetcher  see also: {@link #fetcher}.
   */
  TarantoolBoxSpaceImpl(TarantoolBalancer balancer, Integer spaceId, TarantoolSchemaFetcher fetcher) {
    this.balancer = balancer;
    this.fetcher = fetcher;
    Objects.requireNonNull(spaceId, "spaceId must be not null");
    this.spaceId = spaceId;
    if (fetcher != null) {
      this.space = fetcher.getSpace(spaceId); // could be useful for getting indexId
    }
  }

  /**
   * <p> This constructor creates {@link TarantoolBoxSpaceImpl} based on the passed parameters.</p>
   *
   * @param balancer  see also: {@link #balancer}.
   * @param spaceName see also: {@link #spaceName}.
   * @param fetcher   see also: {@link #fetcher}.
   */
  TarantoolBoxSpaceImpl(TarantoolBalancer balancer, String spaceName, TarantoolSchemaFetcher fetcher) {
    this.balancer = balancer;
    this.fetcher = fetcher;
    Objects.requireNonNull(spaceName, "spaceName must be not null");
    this.spaceName = spaceName;
    if (fetcher != null) {
      this.space = fetcher.getSpace(spaceName);
      this.spaceId = space.getId(); // TODO: add option to be able to have relevant metadata within box class
    }
  }

  @Override
  public CompletableFuture<?> insertObject(Object object) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> insert(Object tuple) {
    return insert(tuple, defaultOptions);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> insert(Object tuple, Options options) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoInsert(tuple, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(Object tuple, Class<T> entity) {
    return insert(tuple, defaultOptions, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(Object tuple, Options options, Class<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoInsert(tuple, options), entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> insert(Object tuple, TypeReference<T> entity) {
    return insert(tuple, defaultOptions, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> insert(Object tuple, Options options, TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoInsert(tuple, options), entity);
  }

  @Override
  public CompletableFuture<?> replaceObject(Object object) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> replace(Object tuple) {
    return replace(tuple, defaultOptions);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> replace(Object tuple, Options options) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoReplace(tuple, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(Object tuple, Class<T> entity) {
    return replace(tuple, defaultOptions, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(Object tuple, Options options, Class<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoReplace(tuple, options), entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> replace(Object tuple, TypeReference<T> entity) {
    return replace(tuple, defaultOptions, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> replace(Object tuple, Options options, TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoReplace(tuple, options), entity);
  }

  @Override
  public CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> select(List<?> key) {
    return select(key, defaultSelectOptions);
  }

  @Override
  public CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> select(Object... key) {
    return select(Arrays.asList(key), defaultSelectOptions);
  }

  @Override
  public CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> select(List<?> key, SelectOptions options) {
    return TarantoolJacksonMapping.convertSelectResultFuture(iprotoSelect(key, options));
  }

  @Override
  public <T> CompletableFuture<SelectResponse<List<Tuple<T>>>> select(List<?> key, Class<T> entity) {
    return select(key, defaultSelectOptions, entity);
  }

  @Override
  public <T> CompletableFuture<SelectResponse<List<Tuple<T>>>> select(List<?> key, SelectOptions options,
      Class<T> entity) {
    return TarantoolJacksonMapping.convertSelectResultFuture(iprotoSelect(key, options), entity);
  }

  @Override
  public <T> CompletableFuture<SelectResponse<T>> select(List<?> key, TypeReference<T> entity) {
    return select(key, defaultSelectOptions, entity);
  }

  @Override
  public <T> CompletableFuture<SelectResponse<T>> select(List<?> key, SelectOptions options,
      TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertSelectResultFuture(iprotoSelect(key, options), entity);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> delete(List<?> key) {
    return delete(key, defaultDeleteOptions);
  }

  @Override
  public CompletableFuture<?> delete(Object key) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> delete(Object... key) {
    return delete(Arrays.asList(key), defaultDeleteOptions);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> delete(List<?> key, DeleteOptions options) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoDelete(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(List<?> key, Class<T> entity) {
    return delete(key, defaultDeleteOptions, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(List<?> key, DeleteOptions options, Class<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoDelete(key, options), entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> delete(List<?> key, TypeReference<T> entity) {
    return delete(key, defaultDeleteOptions, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> delete(List<?> key, DeleteOptions options, TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoDelete(key, options), entity);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(List<?> key, List<List<?>> operations) {
    return update(key, operations, defaultUpdateOptions);
  }

  @Override
  public CompletableFuture<?> update(Object pk, List<List<?>> operations) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(List<?> key, Operations operations) {
    return update(key, operations, defaultUpdateOptions);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(List<?> key, List<List<?>> operations, UpdateOptions options) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoUpdate(key, operations, options));
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(List<?> key, Operations operations, UpdateOptions options) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoUpdate(key, operations, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(List<?> key, List<List<?>> operations, Class<T> entity) {
    return update(key, operations, defaultUpdateOptions, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(List<?> key, Operations operations, Class<T> entity) {
    return update(key, operations, defaultUpdateOptions, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(List<?> key,
      List<List<?>> operations,
      UpdateOptions options,
      Class<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoUpdate(key, operations, options), entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(List<?> key,
      Operations operations,
      UpdateOptions options,
      Class<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoUpdate(key, operations, options), entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(List<?> key, List<List<?>> operations, TypeReference<T> entity) {
    return update(key, operations, defaultUpdateOptions, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(List<?> key, Operations operations, TypeReference<T> entity) {
    return update(key, operations, defaultUpdateOptions, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(List<?> key,
      List<List<?>> operations,
      UpdateOptions options,
      TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoUpdate(key, operations, options), entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(List<?> key,
      Operations operations,
      UpdateOptions options,
      TypeReference<T> entity) {
    return TarantoolJacksonMapping.convertSpaceSingleResultFuture(iprotoUpdate(key, operations, options), entity);
  }

  @Override
  public CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations) {
    return upsert(tuple, operations, defaultUpdateOptions);
  }

  @Override
  public CompletableFuture<Void> upsert(Object tuple, Operations operations) {
    return upsert(tuple, operations, defaultUpdateOptions);
  }

  @Override
  public CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations, UpdateOptions options) {
    return iprotoUpsert(tuple, operations, options).thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Void> upsert(Object tuple, Operations operations, UpdateOptions options) {
    return iprotoUpsert(tuple, operations, options).thenAccept((r) -> {});
  }

  @Override
  public TarantoolSchemaFetcher getFetcher() {
    return fetcher;
  }

  /**
   * <p>Sends a low-level upsert request based on the passed parameters.</p>
   *
   * @param tuple      if such a tuple exists, then it updates based on the passed operations, otherwise it inserts the
   *                   passed tuple.
   * @param operations a list of operations indicating how to update fields in tuple.
   * @param options    {@link UpdateOptions} object.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   * {@link CompletableFuture} with exception.
   */
  private CompletableFuture<IProtoResponse> iprotoUpsert(Object tuple,
      List<?> operations,
      UpdateOptions options) {
    if (tuple == null) {
      throw new IllegalArgumentException("tuple can't be null");
    }
    if (operations == null) {
      throw new IllegalArgumentException("operations can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }

    final CompletableFuture<IProtoResponse> requestFuture =
        balancer.getNext()
            .thenCompose(client -> {
              final IProtoRequestOpts requestOpts =
                  IProtoRequestOpts.empty()
                      .withRequestTimeout(options.getTimeout())
                      .withStreamId(options.getStreamId());

              int indexId;
              if (fetcher != null) {
                indexId = getIndexIdWithEnabledFetcher(options);
              } else {
                assertSpaceName(client.isFeatureEnabled(SPACE_AND_INDEX_NAMES));
                assertIndexName(options);
                indexId = options.getIndexId();
              }

              return client.upsert(this.spaceId,
                  this.spaceName,
                  indexId,
                  TarantoolJacksonMapping.toValue(tuple),
                  TarantoolJacksonMapping.toValue(operations),
                  requestOpts);
            });

    if (fetcher != null) {
      return fetcher.processRequest(requestFuture);
    }
    return requestFuture;
  }

  /**
   * <p>Sends a low-level update request based on the passed parameters.</p>
   *
   * @param key        list of keys by which tuple is updated.
   * @param operations a list of operations indicating how to update fields in tuple.
   * @param options    {@link UpdateOptions} object.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   * {@link CompletableFuture} with exception.
   */
  private CompletableFuture<IProtoResponse> iprotoUpdate(List<?> key,
      List<?> operations,
      UpdateOptions options) {
    if (key == null) {
      throw new IllegalArgumentException("key can't be null");
    }
    if (operations == null) {
      throw new IllegalArgumentException("operations can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }

    final CompletableFuture<IProtoResponse> requestFuture =
        balancer.getNext()
            .thenCompose(client -> {
              final IProtoRequestOpts requestOpts =
                  IProtoRequestOpts.empty()
                      .withRequestTimeout(options.getTimeout())
                      .withStreamId(options.getStreamId());

              Integer indexId;
              String indexName = null;
              if (fetcher != null) {
                indexId = getIndexIdWithEnabledFetcher(options);
              } else {
                final boolean serverHasMetaNamesFeature = client.isFeatureEnabled(SPACE_AND_INDEX_NAMES);
                assertSpaceName(serverHasMetaNamesFeature);
                assertIndexName(options, serverHasMetaNamesFeature);
                indexName = options.getIndexName();
                indexId = getIndexIdDependingOnPriority(options, serverHasMetaNamesFeature);
              }

              return client.update(this.spaceId,
                  this.spaceName,
                  indexId,
                  indexName,
                  TarantoolJacksonMapping.toValue(key),
                  TarantoolJacksonMapping.toValue(operations),
                  requestOpts);
            });

    if (fetcher != null) {
      return fetcher.processRequest(requestFuture);
    }
    return requestFuture;
  }

  /**
   * <p>Sends a low-level delete request based on the passed parameters.</p>
   *
   * @param key     list of keys by which tuple is deleted.
   * @param options {@link DeleteOptions} object.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   * {@link CompletableFuture} with exception.
   */
  private CompletableFuture<IProtoResponse> iprotoDelete(List<?> key, DeleteOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }

    final CompletableFuture<IProtoResponse> requestFuture =
        balancer.getNext()
            .thenCompose(client -> {
              final IProtoRequestOpts requestOpts =
                  IProtoRequestOpts.empty()
                      .withRequestTimeout(options.getTimeout())
                      .withStreamId(options.getStreamId());

              Integer indexId;
              String indexName = null;
              if (fetcher != null) {
                indexId = getIndexIdWithEnabledFetcher(options);
              } else {
                final boolean serverHasMetaNamesFeature = client.isFeatureEnabled(SPACE_AND_INDEX_NAMES);
                assertSpaceName(serverHasMetaNamesFeature);
                assertIndexName(options, serverHasMetaNamesFeature);
                indexName = options.getIndexName();
                indexId = getIndexIdDependingOnPriority(options, serverHasMetaNamesFeature);
              }

              return client.delete(this.spaceId,
                  this.spaceName,
                  indexId,
                  indexName,
                  TarantoolJacksonMapping.toValue(key),
                  requestOpts);
            });

    if (fetcher != null) {
      return fetcher.processRequest(requestFuture);
    }
    return requestFuture;
  }

  /**
   * <p>Sends a low-level select request based on the passed parameters.</p>
   *
   * @param key     list of keys by which tuple is selected.
   * @param options {@link SelectOptions} object.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   * {@link CompletableFuture} with exception.
   */
  private CompletableFuture<IProtoResponse> iprotoSelect(List<?> key, SelectOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }

    SelectAfterMode afterMode;
    byte[] castedAfter;
    Object after = options.getAfter();
    if (after instanceof byte[]) {
      afterMode = SelectAfterMode.POSITION;
      castedAfter = (byte[]) after;
    } else if (after != null) {
      afterMode = SelectAfterMode.TUPLE;
      castedAfter = TarantoolJacksonMapping.toValue(after);
    } else {
      afterMode = null;
      castedAfter = null;
    }

    final CompletableFuture<IProtoResponse> requestFuture =
        balancer.getNext()
            .thenCompose(client -> {
              final IProtoRequestOpts requestOpts =
                  IProtoRequestOpts.empty()
                      .withRequestTimeout(options.getTimeout())
                      .withStreamId(options.getStreamId());

              Integer indexId;
              String indexName = null;
              if (fetcher != null) {
                indexId = getIndexIdWithEnabledFetcher(options);
              } else {
                final boolean serverHasMetaNamesFeature = client.isFeatureEnabled(SPACE_AND_INDEX_NAMES);
                assertSpaceName(serverHasMetaNamesFeature);
                assertIndexName(options, serverHasMetaNamesFeature);
                indexName = options.getIndexName();
                indexId = getIndexIdDependingOnPriority(options, serverHasMetaNamesFeature);
              }

              return client.select(this.spaceId,
                  this.spaceName,
                  indexId,
                  indexName,
                  TarantoolJacksonMapping.toValue(key),
                  options.getLimit(),
                  options.getOffset(),
                  options.getIterator(),
                  options.isPositionFetchEnabled(),
                  castedAfter,
                  afterMode,
                  requestOpts);
            });

    if (fetcher != null) {
      return fetcher.processRequest(requestFuture);
    }

    return requestFuture;
  }

  /**
   * <p>Sends a low-level replace request based on the passed parameters.</p>
   *
   * @param tuple   tuple object for replace.
   * @param options {@link Options} object.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   * {@link CompletableFuture} with exception.
   */
  private CompletableFuture<IProtoResponse> iprotoReplace(Object tuple, Options options) {
    if (tuple == null) {
      throw new IllegalArgumentException("tuple can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    final CompletableFuture<IProtoResponse> requestFuture =
        balancer.getNext().thenCompose(client -> {
          final IProtoRequestOpts requestOpts =
              IProtoRequestOpts.empty()
                  .withRequestTimeout(options.getTimeout())
                  .withStreamId(options.getStreamId());

          if (fetcher == null) {
            assertSpaceName(client.isFeatureEnabled(SPACE_AND_INDEX_NAMES));
          }
          return client.replace(this.spaceId,
              this.spaceName,
              TarantoolJacksonMapping.toValue(tuple),
              requestOpts);
        });

    if (fetcher != null) {
      return fetcher.processRequest(requestFuture);
    }
    return requestFuture;
  }

  /**
   * <p>Sends a low-level insert request based on the passed parameters.</p>
   *
   * @param tuple   tuple object for insertion.
   * @param options {@link Options} object.
   * @return if success - {@link CompletableFuture} with {@link IProtoResponse} object, otherwise -
   * {@link CompletableFuture} with exception.
   */
  private CompletableFuture<IProtoResponse> iprotoInsert(Object tuple, Options options) {
    if (tuple == null) {
      throw new IllegalArgumentException("tuple can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }

    final CompletableFuture<IProtoResponse> requestFuture =
        balancer.getNext()
            .thenCompose(client -> {
              final IProtoRequestOpts requestOpts =
                  IProtoRequestOpts.empty()
                      .withRequestTimeout(options.getTimeout())
                      .withStreamId(options.getStreamId());

              if (fetcher == null) {
                assertSpaceName(client.isFeatureEnabled(SPACE_AND_INDEX_NAMES));
              }
              return client.insert(this.spaceId,
                  this.spaceName,
                  TarantoolJacksonMapping.toValue(tuple),
                  requestOpts);
            });

    if (fetcher != null) {
      return fetcher.processRequest(requestFuture);
    }
    return requestFuture;
  }

  /**
   * <p>Checks whether the space name is allowed to be used directly via IProto. Used by the
   * {@link #iprotoDelete(List, DeleteOptions)}, {@link #iprotoSelect(List, SelectOptions)},
   * {@link #iprotoUpdate(List, List, UpdateOptions)}, {@link #iprotoUpsert(Object, List, UpdateOptions)},
   * {@link #iprotoInsert(Object, Options)}, {@link #iprotoReplace(Object, Options)} methods.</p>
   *
   * @param serverHasMetaNamesFeature if true then the {@link io.tarantool.core.IProtoFeature#SPACE_AND_INDEX_NAMES} is
   *                                  enabled
   * @throws IllegalArgumentException when space name is passed but serverHasMetaNamesFeature is false
   */
  private void assertSpaceName(boolean serverHasMetaNamesFeature)
      throws IllegalArgumentException {

    if (!serverHasMetaNamesFeature && spaceName != null) {
      throw new IllegalArgumentException(WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0);
    }
  }

  /**
   * <p>Checks, based on the passed parameters, whether the index name can be used directly via IProto. Used by the
   * {@link #iprotoDelete(List, DeleteOptions)}, {@link #iprotoSelect(List, SelectOptions)},
   * {@link #iprotoUpdate(List, List, UpdateOptions)}, {@link #iprotoInsert(Object, Options)},
   * {@link #iprotoReplace(Object, Options)} methods.</p>
   *
   * @param options                   request parameters
   * @param serverHasMetaNamesFeature if true then the {@link io.tarantool.core.IProtoFeature#SPACE_AND_INDEX_NAMES} is
   *                                  enabled
   * @throws IllegalArgumentException when index name is passed but serverHasMetaNamesFeature is false
   */
  private void assertIndexName(OptionsWithIndex options, boolean serverHasMetaNamesFeature)
      throws IllegalArgumentException {

    if (!serverHasMetaNamesFeature && options.getIndexName() != null) {
      throw new IllegalArgumentException(WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0);
    }
  }

  /**
   * <p>Checks, based on the passed parameters, whether the index name can be used directly via IProto. Used by the
   * {@link #iprotoUpsert(Object, List, UpdateOptions)} method.</p>
   *
   * @param options request parameters
   * @throws IllegalArgumentException when index name is passed
   */
  private void assertIndexName(OptionsWithIndex options)
      throws IllegalArgumentException {

    if (options.getIndexName() != null) {
      throw new IllegalArgumentException(WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0);
    }
  }

  /**
   * <p>Returns the index id provided that the fetcher is disabled and the index name can be passed directly through
   * IProto without fetcher. Used by the {@link #iprotoDelete(List, DeleteOptions)},
   * {@link #iprotoSelect(List, SelectOptions)}, {@link #iprotoUpdate(List, List, UpdateOptions)} methods.</p>
   *
   * @param options                   {@link OptionsWithIndex} object
   * @param serverHasMetaNamesFeature if true then the {@link io.tarantool.core.IProtoFeature#SPACE_AND_INDEX_NAMES} is
   *                                  enabled
   * @return index id
   */
  private Integer getIndexIdDependingOnPriority(OptionsWithIndex options, boolean serverHasMetaNamesFeature) {
    if (serverHasMetaNamesFeature && options.getIndexName() != null) {
      return null;
    }
    return options.getIndexId();
  }

  /**
   * <p>Returns the index identifier based on the options passed when fetcher is enabled.</p>
   *
   * @param options {@link OptionsWithIndex}.
   * @return index id.
   */
  private int getIndexIdWithEnabledFetcher(OptionsWithIndex options) {
    String indexName = options.getIndexName();
    if (indexName == null) {
      return options.getIndexId();
    }
    Index index = space.getIndex(indexName);
    if (index == null) {
      space = spaceName == null ? fetcher.getSpace(spaceId) : fetcher.getSpace(spaceName);
      // may indexes have been updated
      index = space.getIndex(indexName);
      if (index == null) {
        throw new NoSchemaException("No index " + indexName + " for space: " + space.getName());
      }
    }

    return index.getIndexId();
  }

  @Override
  public TarantoolBalancer getBalancer() {
    return balancer;
  }

  @Override
  public IProtoClientPool getPool() {
    return balancer.getPool();
  }
}
