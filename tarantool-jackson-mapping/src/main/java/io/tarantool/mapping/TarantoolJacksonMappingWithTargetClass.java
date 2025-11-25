/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.crud.CrudBatchResponse;
import io.tarantool.mapping.crud.CrudResponse;

public class TarantoolJacksonMappingWithTargetClass
    extends TarantoolJacksonMappingWithoutTargetType {

  public static <T> CompletableFuture<TarantoolResponse<List<T>>> convertFutureResult(
      CompletableFuture<IProtoResponse> future, Class<T> entity) {
    return future.thenApply(response -> readResponse(response, entity));
  }

  public static <T> TarantoolResponse<List<T>> readResponse(
      IProtoResponse response, Class<T> entity) {
    return new TarantoolResponse<>(readData(response, wrapIntoList(entity)), getFormats(response));
  }

  public static <T> CompletableFuture<Tuple<T>> convertSpaceSingleResultFuture(
      CompletableFuture<IProtoResponse> future, Class<T> entity) {
    return future
        .thenApply(resp -> readSpaceData(resp, entity))
        .thenApply(resp -> getTupleWithInjectedFormat(resp));
  }

  public static <T> TarantoolResponse<List<Tuple<T>>> readSpaceData(
      IProtoResponse response, Class<T> entity) {
    return new TarantoolResponse<>(
        readData(response, wrapIntoList(wrapIntoTuple(entity))), getFormats(response));
  }

  public static <T> Tuple<T> getTupleWithInjectedFormat(TarantoolResponse<List<Tuple<T>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    List<Tuple<T>> data = resp.get();
    if (data != null) {
      for (Tuple<T> tuple : data) {
        if (formats.isEmpty()) {
          return tuple;
        }
        Integer formatId = tuple.getFormatId();
        List<Field> format = formats.get(formatId);
        tuple.setFormat(format);
        return tuple;
      }
    }
    return null;
  }

  public static <T> CompletableFuture<Tuple<T>> convertCrudSingleResultFuture(
      CompletableFuture<IProtoResponse> future, Class<T> entity) {
    return future
        .thenApply(response -> readCrudSingleResultData(response, entity))
        .thenApply(response -> getTupleWithInjectedFormat(response));
  }

  public static <T> TarantoolResponse<List<Tuple<T>>> readCrudSingleResultData(
      IProtoResponse response, Class<T> entity) {
    return new TarantoolResponse<>(
        getRows(
            readData(
                response, wrapIntoType(CrudResponse.class, wrapIntoList(wrapIntoTuple(entity))))),
        getFormats(response));
  }

  public static <T> CompletableFuture<SelectResponse<List<Tuple<T>>>> convertSelectResultFuture(
      CompletableFuture<IProtoResponse> future, Class<T> entity) {
    return future
        .thenApply(resp -> readSelectResult(resp, entity))
        .thenApply(resp -> injectFormatIntoTuples(resp));
  }

  public static <T> SelectResponse<List<Tuple<T>>> readSelectResult(
      IProtoResponse response, Class<T> entity) {
    return new SelectResponse<>(
        readData(response, wrapIntoList(wrapIntoTuple(entity))),
        getPosition(response),
        getFormats(response));
  }

  private static <T> SelectResponse<List<Tuple<T>>> injectFormatIntoTuples(
      SelectResponse<List<Tuple<T>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    if (!formats.isEmpty()) {
      for (Tuple<?> tuple : resp.get()) {
        Integer formatId = tuple.getFormatId();
        List<Field> format = formats.get(formatId);
        tuple.setFormat(format);
      }
    }
    return resp;
  }

  public static <T> CompletableFuture<List<Tuple<T>>> convertCrudSelectResultFuture(
      CompletableFuture<IProtoResponse> future, Class<T> entity) {
    return future
        .thenApply(resp -> readCrudSelectResult(resp, entity))
        .thenApply(resp -> getTuplesWithInjectedFormat(resp));
  }

  public static <T> TarantoolResponse<List<Tuple<T>>> readCrudSelectResult(
      IProtoResponse response, Class<T> entity) {
    return new TarantoolResponse<>(
        getRows(
            readData(
                response, wrapIntoType(CrudResponse.class, wrapIntoList(wrapIntoTuple(entity))))),
        getFormats(response));
  }

  public static <T>
      CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> convertCrudBatchResultFuture(
          CompletableFuture<IProtoResponse> future, Class<T> entity) {
    return future
        .thenApply(resp -> readCrudBatchResult(resp, entity))
        .thenApply(resp -> getBatchTuplesWithInjectedFormat(resp));
  }

  public static <T> TarantoolResponse<CrudBatchResponse<List<Tuple<T>>>> readCrudBatchResult(
      IProtoResponse response, Class<T> entity) {
    return new TarantoolResponse<>(
        readData(
            response, wrapIntoType(CrudBatchResponse.class, wrapIntoList(wrapIntoTuple(entity)))),
        getFormats(response));
  }

  private static <T> CrudBatchResponse<List<Tuple<T>>> getBatchTuplesWithInjectedFormat(
      TarantoolResponse<CrudBatchResponse<List<Tuple<T>>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    CrudBatchResponse<List<Tuple<T>>> batchResp = resp.get();
    List<Tuple<T>> tuples = batchResp.getRows();
    if (!formats.isEmpty()) {
      for (Tuple<?> tuple : tuples) {
        Integer formatId = tuple.getFormatId();
        List<Field> format = formats.get(formatId);
        tuple.setFormat(format);
      }
    }
    return batchResp;
  }

  private static <T> List<Tuple<T>> getTuplesWithInjectedFormat(
      TarantoolResponse<List<Tuple<T>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    List<Tuple<T>> tuples = resp.get();
    if (!formats.isEmpty()) {
      for (Tuple<?> tuple : tuples) {
        Integer formatId = tuple.getFormatId();
        List<Field> format = formats.get(formatId);
        tuple.setFormat(format);
      }
    }
    return tuples;
  }

  public static <T> TarantoolResponse<T> fromEventData(IProtoResponse response, Class<T> entity) {
    return new TarantoolResponse<>(readEventData(response, entity), getFormats(response));
  }
}
