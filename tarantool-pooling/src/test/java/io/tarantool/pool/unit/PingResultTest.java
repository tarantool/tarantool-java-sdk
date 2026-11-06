/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.unit;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.protocol.IProtoRawResponse;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.IProtoConstant;
import io.tarantool.pool.availability.heartbeat.PingResult;

class PingResultTest {

  static Stream<Arguments> dataForTestPingEvaluation() {
    return Stream.of(
        Arguments.of(IProtoResponses.emptyBody(), true, "пустое тело (IPROTO_PING)"),
        Arguments.of(IProtoResponses.dataArray(ValueFactory.newBoolean(true)), true, "DATA=[true]"),
        Arguments.of(
            IProtoResponses.dataArray(
                ValueFactory.newBoolean(true), ValueFactory.newBoolean(false)),
            false,
            "DATA=[true, false]"),

        // --- Shapes that unambiguously mean "failure" ---
        Arguments.of(
            IProtoResponses.dataArray(ValueFactory.newBoolean(false)), false, "DATA=[false]"),
        Arguments.of(IProtoResponses.dataArray(ValueFactory.newInteger(1)), false, "DATA=[1]"),
        Arguments.of(IProtoResponses.dataArray(ValueFactory.newString("ok")), false, "DATA=['ok']"),
        Arguments.of(
            IProtoResponses.dataArray(
                ValueFactory.newMap(ValueFactory.newString("ok"), ValueFactory.newBoolean(true))),
            false,
            "DATA=[{ok=true}] — map вместо boolean"),
        Arguments.of(IProtoResponses.dataArray(), true, "DATA=[]"),
        Arguments.of(
            IProtoResponses.dataArray(ValueFactory.newNil()),
            false,
            "DATA=[nil] (eval 'return nil')"),
        Arguments.of(
            IProtoResponses.dataArray(ValueFactory.newNil(), ValueFactory.newBoolean(true)),
            false,
            "DATA=[nil, true] — проверяется только [0]"),
        Arguments.of(IProtoResponses.error(0x2a, "boom"), false, "ответ-ошибка сервера"),
        Arguments.of(null, false, "ответа нет (null)"),
        Arguments.of(IProtoResponses.withoutData(), false, "no data"),
        Arguments.of(IProtoResponses.dataNotArray(), false, "data is not array"));
  }

  @ParameterizedTest(name = "[{index}] {2}")
  @MethodSource("dataForTestPingEvaluation")
  void testPingEvaluation(IProtoResponse response, boolean expected, String description) {
    Assertions.assertEquals(
        expected, PingResult.hasSuccessPingResult(response, "test-ping"), description);
  }
}

/** Utils */
final class IProtoResponses {

  private IProtoResponses() {}

  /** Заголовок обычного (не ошибочного) ответа: request type = 0 (IPROTO_OK). */
  private static final MapValue OK_HEADER =
      ValueFactory.newMap(
          ValueFactory.newInteger(IProtoConstant.IPROTO_REQUEST_TYPE), ValueFactory.newInteger(0));

  /** Заголовок ошибочного ответа: 0x8000 | code. */
  private static MapValue errorHeader(int code) {
    return ValueFactory.newMap(
        ValueFactory.newInteger(IProtoConstant.IPROTO_REQUEST_TYPE),
        ValueFactory.newInteger(IProtoConstant.IPROTO_ERROR_BASE + code));
  }

  /**
   * Собирает ответ из заголовка и тела.
   *
   * <p>Формат пакета: [заголовок][тело]; {@code headerSize} = длина заголовка.
   */
  static IProtoResponse response(MapValue header, Value body) {
    try (final MessageBufferPacker headerPacker = MessagePack.newDefaultBufferPacker();
        final MessageBufferPacker bodyPacker = MessagePack.newDefaultBufferPacker()) {
      headerPacker.packValue(header);
      byte[] headerBytes = headerPacker.toByteArray();
      int headerSize = headerBytes.length;
      bodyPacker.packValue(body);
      byte[] bodyBytes = bodyPacker.toByteArray();

      byte[] packet = new byte[headerSize + bodyBytes.length];
      System.arraycopy(headerBytes, 0, packet, 0, headerSize);
      System.arraycopy(bodyBytes, 0, packet, headerSize, bodyBytes.length);

      return new IProtoRawResponse(header, packet, headerSize);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Ответ без тела — как от {@code IPROTO_PING}. */
  static IProtoResponse emptyBody() {
    return response(OK_HEADER, ValueFactory.emptyMap());
  }

  /** То же, но с произвольным числом элементов в DATA. */
  static IProtoResponse dataArray(Value... values) {
    final ValueFactory.MapBuilder bodyBuilder = ValueFactory.newMapBuilder();
    bodyBuilder.put(
        ValueFactory.newInteger(IProtoConstant.IPROTO_DATA), ValueFactory.newArray(values));
    return response(OK_HEADER, bodyBuilder.build());
  }

  /** Ответ-ошибка сервера. */
  static IProtoResponse error(int code, String message) {
    final MapValue body =
        ValueFactory.newMap(
            ValueFactory.newInteger(IProtoConstant.IPROTO_ERROR_24),
            ValueFactory.newString(message),
            ValueFactory.newInteger(IProtoConstant.IPROTO_ERROR),
            ValueFactory.newMap(ValueFactory.newInteger(0x00), ValueFactory.emptyArray()));
    return response(errorHeader(code), body);
  }

  /** Ответ без IPROTO_DATA — например, ответ на IPROTO_ID. */
  static IProtoResponse withoutData() {
    return IProtoResponses.response(
        OK_HEADER,
        ValueFactory.newMap(
            ValueFactory.newInteger(IProtoConstant.IPROTO_VERSION), ValueFactory.newInteger(7),
            ValueFactory.newInteger(IProtoConstant.IPROTO_FEATURES), ValueFactory.emptyArray()));
  }

  /** IPROTO_DATA есть, но это не массив. */
  static IProtoResponse dataNotArray() {
    return IProtoResponses.response(
        OK_HEADER,
        ValueFactory.newMap(
            ValueFactory.newInteger(IProtoConstant.IPROTO_DATA), ValueFactory.newString("ok")));
  }
}
