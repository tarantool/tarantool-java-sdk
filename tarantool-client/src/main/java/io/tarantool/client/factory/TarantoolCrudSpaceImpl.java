/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import static io.tarantool.mapping.TarantoolJacksonMappingWithTargetTypeReference.convertCrudSelectResultFuture;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.Options;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.UpsertBatch;
import io.tarantool.client.crud.options.CountOptions;
import io.tarantool.client.crud.options.CrudOptions;
import io.tarantool.client.crud.options.DeleteOptions;
import io.tarantool.client.crud.options.GetOptions;
import io.tarantool.client.crud.options.InsertManyOptions;
import io.tarantool.client.crud.options.InsertOptions;
import io.tarantool.client.crud.options.LenOptions;
import io.tarantool.client.crud.options.MinMaxOptions;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.client.crud.options.TruncateOptions;
import io.tarantool.client.crud.options.UpdateOptions;
import io.tarantool.client.crud.options.UpsertManyOptions;
import io.tarantool.client.operation.Operations;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.mapping.crud.CrudBatchResponse;
import io.tarantool.mapping.crud.CrudError;
import io.tarantool.mapping.crud.CrudScalarResponse;

/**
 * Class extends {@link AbstractTarantoolSpace} class and implementing {@link TarantoolCrudSpace}.
 *
 * <p>To use this class correctly, you can follow this example:
 *
 * <blockquote>
 *
 * <pre>{@code
 * // Creates box client with default settings.
 * TarantoolCrudClient crudClient = TarantoolCrudClientImpl.builder().build();
 * ...
 *
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 * ...
 *
 * }</pre>
 *
 * </blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudSpace
 * @see CrudOptions
 * @see TarantoolBalancer
 */
final class TarantoolCrudSpaceImpl extends IProtoCallClusterSpace implements TarantoolCrudSpace {

  /** Template for crud insert function. */
  public static final String CRUD_INSERT = "crud.insert";

  /** Template for crud_insert function. */
  public static final String CRUD_INSERT_OBJECT = "crud.insert_object";

  /** Template for crud insert_many function. */
  public static final String CRUD_INSERT_MANY = "crud.insert_many";

  /** Template for crud insert_object_many function. */
  public static final String CRUD_INSERT_MANY_OBJECT = "crud.insert_object_many";

  /** Template for crud replace function. */
  public static final String CRUD_REPLACE = "crud.replace";

  /** Template for crud replace_object function. */
  public static final String CRUD_REPLACE_OBJECT = "crud.replace_object";

  /** Template for crud replace_many function. */
  public static final String CRUD_REPLACE_MANY = "crud.replace_many";

  /** Template for crud replace_object_many function. */
  public static final String CRUD_REPLACE_OBJECT_MANY = "crud.replace_object_many";

  /** Template for crud get function. */
  public static final String CRUD_GET = "crud.get";

  /** Template for crud select function. */
  public static final String CRUD_SELECT = "crud.select";

  /** Template for crud delete function. */
  public static final String CRUD_DELETE = "crud.delete";

  /** Template for crud update function. */
  public static final String CRUD_UPDATE = "crud.update";

  /** Template for crud upsert function. */
  public static final String CRUD_UPSERT = "crud.upsert";

  /** Template for crud upsert_many function. */
  public static final String CRUD_UPSERT_MANY = "crud.upsert_many";

  /** Template for crud upsert_object function. */
  public static final String CRUD_UPSERT_OBJECT = "crud.upsert_object";

  /** Template for crud upsert_object_many function. */
  public static final String CRUD_UPSERT_OBJECT_MANY = "crud.upsert_object_many";

  /** Template for crud truncate function. */
  public static final String CRUD_TRUNCATE = "crud.truncate";

  /** Template for crud count function. */
  public static final String CRUD_COUNT = "crud.count";

  /** Template for crud len function. */
  public static final String CRUD_LEN = "crud.len";

  /** Template for crud min function. */
  public static final String CRUD_MIN = "crud.min";

  /** Template for crud max function. */
  public static final String CRUD_MAX = "crud.max";

  /** Template type reference for scalar return values. */
  public static final TypeReference<CrudScalarResponse<Integer>> CRUD_CALL_INT_RESULT =
      new TypeReference<CrudScalarResponse<Integer>>() {};

  /** Template type reference for boolean return values. */
  public static final TypeReference<CrudScalarResponse<Boolean>> CRUD_CALL_BOOL_RESULT =
      new TypeReference<CrudScalarResponse<Boolean>>() {};

  /** {@link InsertOptions} default value. */
  private static final InsertOptions DEFAULT_INSERT_OPTIONS = InsertOptions.builder().build();

  /** {@link InsertManyOptions} default value. */
  private static final InsertManyOptions DEFAULT_INSERT_MANY_OPTIONS =
      InsertManyOptions.builder().build();

  /** {@link UpsertManyOptions} default value. */
  private static final UpsertManyOptions DEFAULT_UPSERT_MANY_OPTIONS =
      UpsertManyOptions.builder().build();

  /** {@link GetOptions} default value. */
  private static final GetOptions DEFAULT_GET_OPTIONS = GetOptions.builder().build();

  /** {@link SelectOptions} default value. */
  private static final SelectOptions DEFAULT_SELECT_OPTIONS = SelectOptions.builder().build();

  /** {@link DeleteOptions} default value. */
  private static final DeleteOptions DEFAULT_DELETE_OPTIONS = DeleteOptions.builder().build();

  /** {@link UpdateOptions} default value. */
  private static final UpdateOptions DEFAULT_UPDATE_OPTIONS = UpdateOptions.builder().build();

  /** {@link TruncateOptions} default value. */
  private static final TruncateOptions DEFAULT_TRUNCATE_OPTIONS = TruncateOptions.builder().build();

  /** {@link LenOptions} default value. */
  private static final LenOptions DEFAULT_LEN_OPTIONS = LenOptions.builder().build();

  /** {@link CountOptions} default value. */
  private static final CountOptions DEFAULT_COUNT_OPTIONS = CountOptions.builder().build();

  /** {@link MinMaxOptions} default value. */
  private static final MinMaxOptions DEFAULT_MIN_MAX_OPTIONS = MinMaxOptions.builder().build();

  /**
   * This constructor creates {@link TarantoolCrudSpaceImpl} based on the passed parameters.
   *
   * @param balancer see also: {@link #balancer}.
   * @param spaceName see also: {@link #spaceName}.
   */
  public TarantoolCrudSpaceImpl(TarantoolBalancer balancer, String spaceName) {
    super(balancer, spaceName);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_INSERT, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> insert(Object tuple) {
    return insert(tuple, DEFAULT_INSERT_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> insertObject(Object tuple) {
    return insertObject(tuple, DEFAULT_INSERT_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> insert(Object tuple, InsertOptions options) {
    return crudCallSingleResult(options, CRUD_INSERT, toTupleOptsArgs(tuple, options));
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> insertObject(Object tuple, InsertOptions options) {
    return crudCallSingleResult(options, CRUD_INSERT_OBJECT, toTupleOptsArgs(tuple, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(Object tuple, Class<T> entity) {
    return insert(tuple, DEFAULT_INSERT_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(
      Object tuple, InsertOptions options, Class<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_INSERT, toTupleOptsArgs(tuple, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(Object tuple, TypeReference<T> entity) {
    return insert(tuple, DEFAULT_INSERT_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> insert(
      Object tuple, InsertOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_INSERT, toTupleOptsArgs(tuple, options));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> insertMany(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudBatchCall(CRUD_INSERT_MANY, options, entity, arguments);
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertMany(List<?> tuples) {
    return insertMany(tuples, DEFAULT_INSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertObjectMany(
      List<?> tuples) {
    return insertObjectMany(tuples, DEFAULT_INSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertMany(
      List<?> tuples, InsertManyOptions options) {
    return crudBatchCall(CRUD_INSERT_MANY, options, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertObjectMany(
      List<?> tuples, InsertManyOptions options) {
    return crudBatchCall(CRUD_INSERT_MANY_OBJECT, options, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> insertMany(
      List<?> tuples, Class<T> entity) {
    return insertMany(tuples, DEFAULT_INSERT_MANY_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> insertMany(
      List<?> tuples, InsertManyOptions options, Class<T> entity) {
    return crudBatchCall(CRUD_INSERT_MANY, options, entity, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> insertMany(
      List<?> tuples, TypeReference<T> entity) {
    return insertMany(tuples, DEFAULT_INSERT_MANY_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> insertMany(
      List<?> tuples, InsertManyOptions options, TypeReference<T> entity) {
    return crudBatchCall(CRUD_INSERT_MANY, options, entity, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> replaceMany(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudBatchCall(CRUD_REPLACE_MANY, options, entity, arguments);
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceMany(List<?> tuples) {
    return replaceMany(tuples, DEFAULT_INSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceObjectMany(
      List<?> tuples) {
    return replaceObjectMany(tuples, DEFAULT_INSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceMany(
      List<?> tuples, InsertManyOptions options) {
    return crudBatchCall(CRUD_REPLACE_MANY, options, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceObjectMany(
      List<?> tuples, InsertManyOptions options) {
    return crudBatchCall(CRUD_REPLACE_OBJECT_MANY, options, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> replaceMany(
      List<?> tuples, Class<T> entity) {
    return replaceMany(tuples, DEFAULT_INSERT_MANY_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> replaceMany(
      List<?> tuples, InsertManyOptions options, Class<T> entity) {
    return crudBatchCall(CRUD_REPLACE_MANY, options, entity, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> replaceMany(
      List<?> tuples, TypeReference<T> entity) {
    return replaceMany(tuples, DEFAULT_INSERT_MANY_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> replaceMany(
      List<?> tuples, InsertManyOptions options, TypeReference<T> entity) {
    return crudBatchCall(CRUD_REPLACE_MANY, options, entity, toTuplesOptsArgs(tuples, options));
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertMany(Options options, Object... arguments) {
    return crudBatchCall(CRUD_UPSERT_MANY, options, arguments)
        .thenApply(CrudBatchResponse::getErrors);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertObjectMany(Options options, Object... arguments) {
    return crudBatchCall(CRUD_UPSERT_OBJECT_MANY, options, arguments)
        .thenApply(CrudBatchResponse::getErrors);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertMany(List<?> tuplesOperationData) {
    return upsertMany(tuplesOperationData, DEFAULT_UPSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertObjectMany(List<?> tuplesOperationData) {
    return upsertObjectMany(tuplesOperationData, DEFAULT_UPSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertMany(UpsertBatch batch) {
    return upsertMany(batch, DEFAULT_UPSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertObjectMany(UpsertBatch batch) {
    return upsertObjectMany(batch, DEFAULT_UPSERT_MANY_OPTIONS);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertMany(
      List<?> tuplesOperationData, UpsertManyOptions options) {
    return crudBatchCall(CRUD_UPSERT_MANY, options, toTuplesOptsArgs(tuplesOperationData, options))
        .thenApply(CrudBatchResponse::getErrors);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertObjectMany(
      List<?> tuplesOperationData, UpsertManyOptions options) {
    return crudBatchCall(
            CRUD_UPSERT_OBJECT_MANY, options, toTuplesOptsArgs(tuplesOperationData, options))
        .thenApply(CrudBatchResponse::getErrors);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertMany(
      UpsertBatch batch, UpsertManyOptions options) {
    return crudBatchCall(CRUD_UPSERT_MANY, options, toTuplesOptsArgs(batch, options))
        .thenApply(CrudBatchResponse::getErrors);
  }

  @Override
  public CompletableFuture<List<CrudError>> upsertObjectMany(
      UpsertBatch batch, UpsertManyOptions options) {
    return crudBatchCall(CRUD_UPSERT_OBJECT_MANY, options, toTuplesOptsArgs(batch, options))
        .thenApply(CrudBatchResponse::getErrors);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_REPLACE, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> replace(Object tuple) {
    return replace(tuple, DEFAULT_INSERT_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> replaceObject(Object tuple) {
    return replaceObject(tuple, DEFAULT_INSERT_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> replace(Object tuple, InsertOptions options) {
    return crudCallSingleResult(options, CRUD_REPLACE, toTupleOptsArgs(tuple, options));
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> replaceObject(Object tuple, InsertOptions options) {
    return crudCallSingleResult(options, CRUD_REPLACE_OBJECT, toTupleOptsArgs(tuple, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(Object tuple, Class<T> entity) {
    return replace(tuple, DEFAULT_INSERT_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(
      Object tuple, InsertOptions options, Class<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_REPLACE, toTupleOptsArgs(tuple, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(Object tuple, TypeReference<T> entity) {
    return replace(tuple, DEFAULT_INSERT_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> replace(
      Object tuple, InsertOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_REPLACE, toTupleOptsArgs(tuple, options));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> select(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSelectResult(options, entity, arguments);
  }

  @Override
  public CompletableFuture<List<Tuple<List<?>>>> select(List<Condition> conditions) {
    return select(conditions, DEFAULT_SELECT_OPTIONS);
  }

  @Override
  public CompletableFuture<List<Tuple<List<?>>>> select(Condition... conditions) {
    return select(Arrays.asList(conditions), DEFAULT_SELECT_OPTIONS);
  }

  @Override
  public CompletableFuture<List<Tuple<List<?>>>> select(
      List<Condition> conditions, SelectOptions options) {
    return crudCallSelectResult(options, toSelectArgs(conditions, options));
  }

  @Override
  public <T> CompletableFuture<List<Tuple<T>>> select(List<Condition> conditions, Class<T> entity) {
    return select(conditions, DEFAULT_SELECT_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<List<Tuple<T>>> select(
      List<Condition> conditions, SelectOptions options, Class<T> entity) {
    return crudCallSelectResult(options, entity, toSelectArgs(conditions, options));
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> select(
      List<Condition> conditions, TypeReference<T> entity) {
    return select(conditions, DEFAULT_SELECT_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<TarantoolResponse<T>> select(
      List<Condition> conditions, SelectOptions options, TypeReference<T> entity) {
    return crudCallSelectResult(options, entity, toSelectArgs(conditions, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> get(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_GET, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> get(Object key) {
    return get(key, DEFAULT_GET_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> get(Object key, GetOptions options) {
    return crudCallSingleResult(options, CRUD_GET, toKeyOptsArgs(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> get(Object key, Class<T> entity) {
    return get(key, DEFAULT_GET_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> get(Object key, GetOptions options, Class<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_GET, toKeyOptsArgs(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> get(Object key, TypeReference<T> entity) {
    return get(key, DEFAULT_GET_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> get(
      Object key, GetOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_GET, toKeyOptsArgs(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_DELETE, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> delete(Object key) {
    return delete(key, DEFAULT_DELETE_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> delete(Object key, DeleteOptions options) {
    return crudCallSingleResult(options, CRUD_DELETE, toKeyOptsArgs(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(Object key, Class<T> entity) {
    return delete(key, DEFAULT_DELETE_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(
      Object key, DeleteOptions options, Class<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_DELETE, toKeyOptsArgs(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(Object key, TypeReference<T> entity) {
    return delete(key, DEFAULT_DELETE_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> delete(
      Object key, DeleteOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_DELETE, toKeyOptsArgs(key, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> min(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_MIN, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> min(String indexName) {
    return min(indexName, DEFAULT_MIN_MAX_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> min(String indexName, MinMaxOptions options) {
    return crudCallSingleResult(options, CRUD_MIN, toIndexNameOptsArgs(indexName, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> min(String indexName, Class<T> entity) {
    return min(indexName, DEFAULT_MIN_MAX_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> min(
      String indexName, MinMaxOptions options, Class<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_MIN, toIndexNameOptsArgs(indexName, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> min(String indexName, TypeReference<T> entity) {
    return min(indexName, DEFAULT_MIN_MAX_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> min(
      String indexName, MinMaxOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_MIN, toIndexNameOptsArgs(indexName, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> max(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_MAX, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> max(String indexName) {
    return max(indexName, DEFAULT_MIN_MAX_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> max(String indexName, MinMaxOptions options) {
    return crudCallSingleResult(options, CRUD_MAX, toIndexNameOptsArgs(indexName, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> max(String indexName, Class<T> entity) {
    return max(indexName, DEFAULT_MIN_MAX_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> max(
      String indexName, MinMaxOptions options, Class<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_MAX, toIndexNameOptsArgs(indexName, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> max(String indexName, TypeReference<T> entity) {
    return max(indexName, DEFAULT_MIN_MAX_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> max(
      String indexName, MinMaxOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(options, entity, CRUD_MAX, toIndexNameOptsArgs(indexName, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Options options, TypeReference<T> entity, Object... arguments) {
    return crudCallSingleResult(options, entity, CRUD_UPDATE, arguments);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(Object key, List<List<?>> operations) {
    return update(key, operations, DEFAULT_UPDATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(Object key, Operations operations) {
    return update(key, operations, DEFAULT_UPDATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(
      Object key, List<List<?>> operations, UpdateOptions options) {
    return crudCallSingleResult(
        options, CRUD_UPDATE, toKeyOperationsOptsArgs(key, operations, options));
  }

  @Override
  public CompletableFuture<Tuple<List<?>>> update(
      Object key, Operations operations, UpdateOptions options) {
    return crudCallSingleResult(
        options, CRUD_UPDATE, toKeyOperationsOptsArgs(key, operations, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, Class<T> entity) {
    return update(key, operations, DEFAULT_UPDATE_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, Class<T> entity) {
    return update(key, operations, DEFAULT_UPDATE_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, UpdateOptions options, Class<T> entity) {
    return crudCallSingleResult(
        options, entity, CRUD_UPDATE, toKeyOperationsOptsArgs(key, operations, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, UpdateOptions options, Class<T> entity) {
    return crudCallSingleResult(
        options, entity, CRUD_UPDATE, toKeyOperationsOptsArgs(key, operations, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, TypeReference<T> entity) {
    return update(key, operations, DEFAULT_UPDATE_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, TypeReference<T> entity) {
    return update(key, operations, DEFAULT_UPDATE_OPTIONS, entity);
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, UpdateOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(
        options, entity, CRUD_UPDATE, toKeyOperationsOptsArgs(key, operations, options));
  }

  @Override
  public <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, UpdateOptions options, TypeReference<T> entity) {
    return crudCallSingleResult(
        options, entity, CRUD_UPDATE, toKeyOperationsOptsArgs(key, operations, options));
  }

  @Override
  public CompletableFuture<Void> upsert(Options options, Object... arguments) {
    return crudCallSingleResult(options, CRUD_UPSERT, arguments).thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Void> upsertObject(Options options, Object... arguments) {
    return crudCallSingleResult(options, CRUD_UPSERT_OBJECT, arguments).thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations) {
    return upsert(tuple, operations, DEFAULT_UPDATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Void> upsertObject(Object tuple, List<List<?>> operations) {
    return upsertObject(tuple, operations, DEFAULT_UPDATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Void> upsert(Object tuple, Operations operations) {
    return upsert(tuple, operations, DEFAULT_UPDATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Void> upsertObject(Object tuple, Operations operations) {
    return upsertObject(tuple, operations, DEFAULT_UPDATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Void> upsert(
      Object tuple, List<List<?>> operations, UpdateOptions options) {
    return crudCallSingleResult(
            options, CRUD_UPSERT, toTupleOperationsOptsArgs(tuple, operations, options))
        .thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Void> upsertObject(
      Object tuple, List<List<?>> operations, UpdateOptions options) {
    return crudCallSingleResult(
            options, CRUD_UPSERT_OBJECT, toTupleOperationsOptsArgs(tuple, operations, options))
        .thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Void> upsert(
      Object tuple, Operations operations, UpdateOptions options) {
    return crudCallSingleResult(
            options, CRUD_UPSERT, toTupleOperationsOptsArgs(tuple, operations, options))
        .thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Void> upsertObject(
      Object tuple, Operations operations, UpdateOptions options) {
    return crudCallSingleResult(
            options, CRUD_UPSERT_OBJECT, toTupleOperationsOptsArgs(tuple, operations, options))
        .thenAccept((r) -> {});
  }

  @Override
  public CompletableFuture<Boolean> truncate(Options options, Object... arguments) {
    return mapTruncateResult(iprotoCall(options, CRUD_TRUNCATE, arguments));
  }

  @Override
  public CompletableFuture<Boolean> truncate() {
    return truncate(DEFAULT_TRUNCATE_OPTIONS);
  }

  @Override
  public CompletableFuture<Boolean> truncate(TruncateOptions options) {
    return mapTruncateResult(iprotoCall(options, CRUD_TRUNCATE, getOptsArgs(options)));
  }

  @Override
  public CompletableFuture<Integer> len(Options options, Object... arguments) {
    return mapCountLenResult(iprotoCall(options, CRUD_LEN, arguments));
  }

  @Override
  public CompletableFuture<Integer> len() {
    return len(DEFAULT_LEN_OPTIONS);
  }

  @Override
  public CompletableFuture<Integer> len(LenOptions options) {
    return mapCountLenResult(iprotoCall(options, CRUD_LEN, getOptsArgs(options)));
  }

  @Override
  public CompletableFuture<Integer> count(Options options, Object... arguments) {
    return mapCountLenResult(iprotoCall(options, CRUD_COUNT, arguments));
  }

  @Override
  public CompletableFuture<Integer> count(Condition... conditions) {
    return count(Arrays.asList(conditions));
  }

  @Override
  public CompletableFuture<Integer> count(List<Condition> conditions) {
    return count(conditions, DEFAULT_COUNT_OPTIONS);
  }

  @Override
  public CompletableFuture<Integer> count(List<Condition> conditions, CountOptions options) {
    return mapCountLenResult(iprotoCall(options, CRUD_COUNT, toSelectArgs(conditions, options)));
  }

  /**
   * Converts completable future response for requests returning scalar values to completable future
   * with integer.
   *
   * @param responseFuture response for requests returning scalar.
   * @return {@link CompletableFuture} object with integer.
   */
  private CompletableFuture<Integer> mapCountLenResult(
      CompletableFuture<IProtoResponse> responseFuture) {
    return TarantoolJacksonMapping.convertFutureResult(responseFuture, CRUD_CALL_INT_RESULT)
        .thenApply(response -> response.get().getValue());
  }

  /**
   * Get options as Map.
   *
   * @param options {@link CrudOptions} object.
   * @return list options.
   */
  private Map<String, Object> getOptsArgs(CrudOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return options.getOptions();
  }

  /**
   * Converts completable future response for requests returning boolean value to completable future
   * with boolean.
   *
   * @param responseFuture response for boolean requests.
   * @return {@link CompletableFuture} object with boolean.
   */
  private CompletableFuture<Boolean> mapTruncateResult(
      CompletableFuture<IProtoResponse> responseFuture) {
    return TarantoolJacksonMapping.convertFutureResult(responseFuture, CRUD_CALL_BOOL_RESULT)
        .thenApply(response -> response.get().getValue());
  }

  /**
   * Converts tuple, operations and options to array of arguments.
   *
   * @param tuple tuple object.
   * @param operations update or upsert operations.
   * @param options {@link CrudOptions} object.
   * @return list of tuple, operations and options.
   */
  private Object[] toTupleOperationsOptsArgs(
      Object tuple, List<?> operations, CrudOptions options) {
    if (tuple == null) {
      throw new IllegalArgumentException("key can't be null");
    }
    if (operations == null) {
      throw new IllegalArgumentException("operations can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {tuple, operations, options.getOptions()};
  }

  /**
   * Converts key, operations and options to array of arguments.
   *
   * @param key key of tuple.
   * @param operations update or upsert operations.
   * @param options {@link CrudOptions} object.
   * @return list of key, operations and options.
   */
  private Object[] toKeyOperationsOptsArgs(
      Object key, List<List<?>> operations, CrudOptions options) {
    if (key == null) {
      throw new IllegalArgumentException("key can't be null");
    }
    if (operations == null) {
      throw new IllegalArgumentException("operations can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {key, operations, options.getOptions()};
  }

  /**
   * Converts key, operations and options to array of arguments.
   *
   * @param key key of tuple.
   * @param operations update or upsert operations.
   * @param options {@link CrudOptions} object.
   * @return list of key, operations and options.
   */
  private Object[] toKeyOperationsOptsArgs(Object key, Operations operations, CrudOptions options) {
    if (key == null) {
      throw new IllegalArgumentException("key can't be null");
    }
    if (operations == null) {
      throw new IllegalArgumentException("operations can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {key, operations, options.getOptions()};
  }

  /**
   * Converts index name and options to array of arguments.
   *
   * @param indexName index name.
   * @param options {@link CrudOptions} object.
   * @return list of index name and options.
   */
  private Object[] toIndexNameOptsArgs(String indexName, CrudOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {indexName, options.getOptions()};
  }

  /**
   * Converts key and options to array of arguments.
   *
   * @param key key of tuple.
   * @param options {@link CrudOptions} object.
   * @return list of key and options.
   */
  private Object[] toKeyOptsArgs(Object key, CrudOptions options) {
    if (key == null) {
      throw new IllegalArgumentException("key can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {key, options.getOptions()};
  }

  /**
   * Get conditions and options as array of select arguments.
   *
   * @param conditions list of {@link Condition} objects.
   * @param options {@link CrudOptions} object.
   * @return list of conditions and options.
   */
  private Object[] toSelectArgs(List<Condition> conditions, CrudOptions options) {
    if (conditions == null) {
      throw new IllegalArgumentException("conditions can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {conditions, options.getOptions()};
  }

  /**
   * Converts tuples and options to array of arguments .
   *
   * @param tuples list of tuple objects.
   * @param options {@link CrudOptions} object.
   * @return array of tuples and options.
   */
  private Object[] toTuplesOptsArgs(List<?> tuples, CrudOptions options) {
    if (tuples == null) {
      throw new IllegalArgumentException("tuples can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {tuples, options.getOptions()};
  }

  /**
   * Converts tuple operations as UpsertBatch and options to array of arguments .
   *
   * @param tuples list of tuple objects.
   * @param options {@link CrudOptions} object.
   * @return list of tuples and options.
   */
  private Object[] toTuplesOptsArgs(UpsertBatch tuples, CrudOptions options) {
    if (tuples == null) {
      throw new IllegalArgumentException("tuples can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {tuples, options.getOptions()};
  }

  /**
   * Converts tuple and options to array of arguments.
   *
   * @param tuple tuple object.
   * @param options {@link CrudOptions} object.
   * @return list of tuple and options.
   */
  private Object[] toTupleOptsArgs(Object tuple, CrudOptions options) {
    if (tuple == null) {
      throw new IllegalArgumentException("tuple can't be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options can't be null");
    }
    return new Object[] {tuple, options.getOptions()};
  }

  /**
   * Sends a low-level call request based on the passed parameters.
   *
   * @param <T> return entity
   * @param options {@link Options} object.
   * @param entity {@link TypeReference} object.
   * @param args list of arguments.
   * @return if success - {@link CompletableFuture} with list of tuples list, otherwise - {@link
   *     CompletableFuture} with exception.
   */
  private <T> CompletableFuture<TarantoolResponse<T>> crudCallSelectResult(
      Options options, TypeReference<T> entity, Object... args) {
    return convertCrudSelectResultFuture(iprotoCall(options, CRUD_SELECT, args), entity);
  }

  private <T> CompletableFuture<Tuple<T>> crudCallSingleResult(
      Options options, TypeReference<T> entity, String functionName, Object... args) {
    return TarantoolJacksonMapping.convertCrudSingleResultFuture(
        iprotoCall(options, functionName, args), entity);
  }

  private <T> CompletableFuture<List<Tuple<T>>> crudCallSelectResult(
      Options options, Class<T> entity, Object... args) {
    return convertCrudSelectResultFuture(
        iprotoCall(options, TarantoolCrudSpaceImpl.CRUD_SELECT, args), entity);
  }

  private <T> CompletableFuture<Tuple<T>> crudCallSingleResult(
      Options options, Class<T> entity, String functionName, Object... args) {
    return TarantoolJacksonMapping.convertCrudSingleResultFuture(
        iprotoCall(options, functionName, args), entity);
  }

  private CompletableFuture<List<Tuple<List<?>>>> crudCallSelectResult(
      Options options, Object... args) {
    return convertCrudSelectResultFuture(
        iprotoCall(options, TarantoolCrudSpaceImpl.CRUD_SELECT, args));
  }

  private CompletableFuture<Tuple<List<?>>> crudCallSingleResult(
      Options options, String functionName, Object... args) {
    return TarantoolJacksonMapping.convertCrudSingleResultFuture(
        iprotoCall(options, functionName, args));
  }

  /**
   * Sends a low-level batch call request based on the passed parameters.
   *
   * @param functionName crud function name.
   * @param options {@link Options} object.
   * @param entity {@link TypeReference} object.
   * @param args list of arguments.
   * @param <T> return entity
   * @return if success - {@link CompletableFuture} with {@link CrudBatchResponse} object, otherwise
   *     - {@link CompletableFuture} with exception.
   */
  private <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> crudBatchCall(
      String functionName, Options options, TypeReference<T> entity, Object... args) {
    return TarantoolJacksonMapping.convertFutureResult(
        iprotoCall(options, functionName, args), wrapIntoType(CrudBatchResponse.class, entity));
  }

  /**
   * Sends a low-level batch call request based on the passed parameters.
   *
   * @param functionName crud function name.
   * @param options {@link Options} object.
   * @param entity {@link Class} object.
   * @param args list of arguments.
   * @param <T> return entity
   * @return if success - {@link CompletableFuture} with {@link CrudBatchResponse} object, otherwise
   *     - {@link CompletableFuture} with exception.
   */
  private <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> crudBatchCall(
      String functionName, Options options, Class<T> entity, Object... args) {
    return TarantoolJacksonMapping.convertCrudBatchResultFuture(
        iprotoCall(options, functionName, args), entity);
  }

  /**
   * Sends a low-level batch call request based on the passed parameters.
   *
   * @param functionName crud function name.
   * @param options {@link Options} object.
   * @param args list of arguments.
   * @return if success - {@link CompletableFuture} with {@link CrudBatchResponse} object, otherwise
   *     - {@link CompletableFuture} with exception.
   */
  private CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> crudBatchCall(
      String functionName, Options options, Object... args) {
    return TarantoolJacksonMapping.convertCrudBatchResultFuture(
        iprotoCall(options, functionName, args));
  }
}
