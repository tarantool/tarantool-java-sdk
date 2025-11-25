/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.crud.CrudResponse;

public class TarantoolJacksonMappingWithTargetTypeReference
    extends TarantoolJacksonMappingWithTargetClass {

  public static <T> CompletableFuture<TarantoolResponse<T>> convertFutureResult(
      CompletableFuture<IProtoResponse> future, TypeReference<T> entity) {
    return future.thenApply(response -> readResponse(response, entity));
  }

  public static <T> TarantoolResponse<T> readResponse(
      IProtoResponse response, TypeReference<T> entity) {
    return new TarantoolResponse<>(readData(response, entity), getFormats(response));
  }

  public static <T> CompletableFuture<TarantoolResponse<T>> convertFutureResult(
      CompletableFuture<IProtoResponse> future, JavaType entity) {
    return future.thenApply(response -> TarantoolJacksonMapping.readResponse(response, entity));
  }

  public static <T> TarantoolResponse<T> readResponse(IProtoResponse response, JavaType entity) {
    return new TarantoolResponse<>(readData(response, entity), getFormats(response));
  }

  public static <T> CompletableFuture<TarantoolResponse<Tuple<T>>> convertSpaceSingleResultFuture(
      CompletableFuture<IProtoResponse> future, TypeReference<T> entity) {
    return future.thenApply(
        response -> readSpaceSingleResultData(response, wrapIntoList(wrapIntoTuple(entity))));
  }

  public static <T> TarantoolResponse<T> readSpaceSingleResultData(
      IProtoResponse response, JavaType entity) {
    return new TarantoolResponse<>(
        getFirstOrNullForReturnAsClass(readData(response, entity)), getFormats(response));
  }

  public static <T> CompletableFuture<Tuple<T>> convertCrudSingleResultFuture(
      CompletableFuture<IProtoResponse> future, TypeReference<T> entity) {
    return future
        .thenApply(response -> readCrudSingleResultData(response, entity))
        .thenApply(response -> getTupleWithInjectedFormat(response));
  }

  public static <T> TarantoolResponse<List<Tuple<T>>> readCrudSingleResultData(
      IProtoResponse response, TypeReference<T> entity) {
    return new TarantoolResponse<>(
        getRows(
            readData(
                response, wrapIntoType(CrudResponse.class, wrapIntoList(wrapIntoTuple(entity))))),
        getFormats(response));
  }

  public static <T> CompletableFuture<SelectResponse<T>> convertSelectResultFuture(
      CompletableFuture<IProtoResponse> future, TypeReference<T> entity) {
    return future.thenApply(resp -> readSelectResult(resp, entity));
  }

  public static <T> SelectResponse<T> readSelectResult(
      IProtoResponse response, TypeReference<T> entity) {
    return new SelectResponse<>(
        readData(response, entity), getPosition(response), getFormats(response));
  }

  public static <T> CompletableFuture<TarantoolResponse<T>> convertCrudSelectResultFuture(
      CompletableFuture<IProtoResponse> future, TypeReference<T> entity) {
    return future.thenApply(resp -> readCrudSelectResult(resp, entity));
  }

  public static <T> TarantoolResponse<T> readCrudSelectResult(
      IProtoResponse response, TypeReference<T> entity) {
    return new TarantoolResponse<>(
        getRows(readData(response, wrapIntoType(CrudResponse.class, entity))),
        getFormats(response));
  }

  public static <T> TarantoolResponse<T> fromEventData(
      IProtoResponse response, TypeReference<T> entity) {
    return new TarantoolResponse<>(readEventData(response, entity), getFormats(response));
  }
}
