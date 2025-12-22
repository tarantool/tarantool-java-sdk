/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_POSITION;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TUPLE_FORMATS;
import io.tarantool.core.protocol.ByteBodyValueWrapper;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.crud.CrudBatchResponse;
import io.tarantool.mapping.crud.CrudResponse;

public class TarantoolJacksonMappingWithoutTargetType extends BaseTarantoolJacksonMapping {

  public static final TypeReference<Map<Integer, List<Field>>> TYPE_REF_MAP_INTEGER_LIST_FIELD
      = new TypeReference<Map<Integer, List<Field>>>() {};

  public static CompletableFuture<TarantoolResponse<List<?>>> convertFutureResult(
      CompletableFuture<IProtoResponse> future) {
    return future.thenApply(TarantoolJacksonMapping::readResponse);
  }

  public static TarantoolResponse<List<?>> readResponse(IProtoResponse response) {
    return new TarantoolResponse<List<?>>(
        readData(
            response,
            List.class
        ),
        getFormats(response)
    );
  }

  public static CompletableFuture<Tuple<List<?>>> convertSpaceSingleResultFuture(
      CompletableFuture<IProtoResponse> future) {
    return future
        .thenApply(TarantoolJacksonMappingWithoutTargetType::readSpaceData)
        .thenApply(TarantoolJacksonMappingWithoutTargetType::getTupleWithInjectedFormat);
  }

  private static TarantoolResponse<List<Tuple<List<?>>>> readSpaceData(IProtoResponse response) {
    return new TarantoolResponse<>(
        readData(
            response,
            LIST_TUPLE_LIST
        ),
        getFormats(response)
    );
  }

  private static Tuple<List<?>> getTupleWithInjectedFormat(TarantoolResponse<List<Tuple<List<?>>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    List<Tuple<List<?>>> data = resp.get();
    if (data != null) {
      for (Tuple<List<?>> tuple : data) {
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

  public static CompletableFuture<Tuple<List<?>>> convertCrudSingleResultFuture(
      CompletableFuture<IProtoResponse> future) {
    return future
        .thenApply(
            TarantoolJacksonMappingWithoutTargetType::readCrudSingleResultData
        )
        .thenApply(
            TarantoolJacksonMappingWithoutTargetType::getTupleWithInjectedFormat
        );

  }

  public static TarantoolResponse<List<Tuple<List<?>>>> readCrudSingleResultData(
      IProtoResponse response) {
    return new TarantoolResponse<>(
        getRows(
            readData(
                response,
                wrapIntoType(
                    CrudResponse.class,
                    LIST_TUPLE_LIST
                )
            )
        ),
        getFormats(response)
    );
  }

  public static <T> T getRows(CrudResponse<T> response) {
    return response.getRows();
  }

  public static CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> convertSelectResultFuture(
      CompletableFuture<IProtoResponse> future) {
    return future
        .thenApply(TarantoolJacksonMappingWithoutTargetType::readSelectResult)
        .thenApply(TarantoolJacksonMappingWithoutTargetType::injectFormatIntoTuples);
  }

  public static SelectResponse<List<Tuple<List<?>>>> readSelectResult(IProtoResponse response) {
    return new SelectResponse<>(
        readData(response, LIST_TUPLE),
        getPosition(response),
        getFormats(response)
    );
  }

  private static SelectResponse<List<Tuple<List<?>>>> injectFormatIntoTuples(
      SelectResponse<List<Tuple<List<?>>>> resp) {
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

  public static Map<Integer, List<Field>> getFormats(IProtoResponse response) {
    Map<Integer, List<Field>> formats = Collections.emptyMap();
    ByteBodyValueWrapper rawFormats = response.getByteBodyValue(IPROTO_TUPLE_FORMATS);
    if (rawFormats != null) {
      formats = readValue(
          rawFormats,
          TYPE_REF_MAP_INTEGER_LIST_FIELD
      );
    }
    return formats;
  }

  public static byte[] getPosition(IProtoResponse response) {
    byte[] position = null;
    ByteBodyValueWrapper rawPosition = response.getByteBodyValue(IPROTO_POSITION);
    if (rawPosition != null) {
      position = Arrays.copyOfRange(
          rawPosition.getPacket(),
          rawPosition.getOffset(),
          rawPosition.getOffset() + rawPosition.getValueLength()
      );
    }
    return position;
  }

  public static CompletableFuture<List<Tuple<List<?>>>> convertCrudSelectResultFuture(
      CompletableFuture<IProtoResponse> future) {
    return future
        .thenApply(resp -> readCrudSelectResult(resp))
        .thenApply(resp -> getTuplesWithInjectedFormat(resp));
  }

  public static TarantoolResponse<List<Tuple<List<?>>>> readCrudSelectResult(IProtoResponse response) {
    return new TarantoolResponse<>(
        getRows(
            readData(
                response,
                wrapIntoType(
                    CrudResponse.class,
                    LIST_TUPLE_LIST
                )
            )
        ),
        getFormats(response)
    );
  }

  private static List<Tuple<List<?>>> getTuplesWithInjectedFormat(TarantoolResponse<List<Tuple<List<?>>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    List<Tuple<List<?>>> tuples = resp.get();
    if (!formats.isEmpty()) {
      for (Tuple<?> tuple : tuples) {
        Integer formatId = tuple.getFormatId();
        List<Field> format = formats.get(formatId);
        tuple.setFormat(format);
      }
    }
    return tuples;
  }

  public static CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> convertCrudBatchResultFuture(
      CompletableFuture<IProtoResponse> future) {
    return future
        .thenApply(resp -> readCrudBatchResult(resp))
        .thenApply(resp -> getBatchTuplesWithInjectedFormat(resp));
  }

  public static TarantoolResponse<CrudBatchResponse<List<Tuple<List<?>>>>> readCrudBatchResult(IProtoResponse response) {
    return new TarantoolResponse<>(
        readData(
            response,
            wrapIntoType(
                CrudBatchResponse.class,
                LIST_TUPLE_LIST
            )
        ),
        getFormats(response)
    );
  }

  private static CrudBatchResponse<List<Tuple<List<?>>>> getBatchTuplesWithInjectedFormat(TarantoolResponse<CrudBatchResponse<List<Tuple<List<?>>>>> resp) {
    Map<Integer, List<Field>> formats = resp.getFormats();
    CrudBatchResponse<List<Tuple<List<?>>>> batchResp = resp.get();
    List<Tuple<List<?>>> tuples = batchResp.getRows();
    if (tuples != null && !formats.isEmpty()) {
      for (Tuple<?> tuple : tuples) {
        Integer formatId = tuple.getFormatId();
        List<Field> format = formats.get(formatId);
        tuple.setFormat(format);
      }
    }
    return batchResp;
  }

  public static TarantoolResponse<?> fromEventData(IProtoResponse response) {
    return new TarantoolResponse<>(
        readEventData(response),
        getFormats(response)
    );
  }
}
