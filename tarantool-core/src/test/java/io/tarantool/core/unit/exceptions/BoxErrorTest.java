/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.unit.exceptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERROR_BASE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_ERRCODE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_ERRNO;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_FIELDS;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_FILE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_LINE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_MESSAGE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_STACK;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_TYPE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_ERROR;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_ERROR_24;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_REQUEST_TYPE;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.exceptions.BoxErrorStackItem;
import io.tarantool.core.protocol.IProtoResponseImpl;

public class BoxErrorTest {

  public static Stream<Arguments> dataForBoxErrorStackItem() {
    return Stream.of(
        Arguments.of("1", "2", 3L, "4", 5L, 6L, ValueFactory.emptyMap()),
        Arguments.of("1", "2", 3L, "4", 5L, 6L, null),
        Arguments.of("1", "2", 3L, "4", 5L, 6L, ValueFactory.newMap(
            ValueFactory.newString("a"), ValueFactory.newString("b"),
            ValueFactory.newInteger(1), ValueFactory.newInteger(2)
        ))
    );
  }

  @ParameterizedTest
  @MethodSource("dataForBoxErrorStackItem")
  public void testBoxErrorStackItem(
      String type, String file, Long line, String message, Long errno, Long errcode, MapValue fields) {
    ValueFactory.MapBuilder mp = ValueFactory.newMapBuilder();
    if (type != null) {
      mp.put(MP_ERROR_TYPE, ValueFactory.newString(type));
    }
    if (file != null) {
      mp.put(MP_ERROR_FILE, ValueFactory.newString(file));
    }
    if (line != null) {
      mp.put(MP_ERROR_LINE, ValueFactory.newInteger(line));
    }
    if (message != null) {
      mp.put(MP_ERROR_MESSAGE, ValueFactory.newString(message));
    }
    if (errno != null) {
      mp.put(MP_ERROR_ERRNO, ValueFactory.newInteger(errno));
    }
    if (errcode != null) {
      mp.put(MP_ERROR_ERRCODE, ValueFactory.newInteger(errcode));
    }
    if (fields != null) {
      mp.put(MP_ERROR_FIELDS, fields);
    }

    BoxErrorStackItem res = BoxErrorStackItem.fromStruct(mp.build());
    assertEquals(type, res.getType());
    assertEquals(file, res.getFile());
    assertEquals(line, res.getLine());
    assertEquals(message, res.getMessage());
    assertEquals(errno, res.getErrno());
    assertEquals(errcode, res.getCode());
    assertEquals(fields, res.getDetails());
  }

  public static Stream<Arguments> dataForBoxError() {
    return Stream.of(
        Arguments.of(
            IPROTO_ERROR_BASE,
            "tarantool_message",
            Arrays.asList(
                Arrays.asList("1", "2", 3L, "4", 5L, 6L, ValueFactory.emptyMap())
            ),
            "BoxError{code=0, message='tarantool_message', stack=[" +
                "BoxErrorStackItem{type='1', line=3, file='2', message='4', errno=5, code=6, details={}}" +
                "]}"
        ),
        Arguments.of(
            IPROTO_ERROR_BASE,
            "tarantool_message",
            Arrays.asList(
                Arrays.asList("1", "2", 3L, "4", 5L, 6L, ValueFactory.emptyMap()),
                Arrays.asList("8", "9", 10L, "11", 12L, 13L, ValueFactory.emptyMap())
            ),
            "BoxError{code=0, message='tarantool_message', stack=[" +
                "BoxErrorStackItem{type='1', line=3, file='2', message='4', errno=5, code=6, details={}}, " +
                "BoxErrorStackItem{type='8', line=10, file='9', message='11', errno=12, code=13, details={}}]" +
                "}")
    );
  }

  @ParameterizedTest
  @MethodSource("dataForBoxError")
  public void testBoxError(
      int requestType, String tarantoolMessage, List<List<Object>> stack, String exceptionMessage) {
    ArrayList<Value> encodedStack = new ArrayList<>();
    for (List<Object> stackItem : stack) {
      String type = (String) stackItem.get(0);
      String file = (String) stackItem.get(1);
      Long line = (Long) stackItem.get(2);
      String message = (String) stackItem.get(3);
      Long errno = (Long) stackItem.get(4);
      Long errcode = (Long) stackItem.get(5);
      MapValue fields = (MapValue) stackItem.get(6);
      ValueFactory.MapBuilder mp = ValueFactory.newMapBuilder();
      if (type != null) {
        mp.put(MP_ERROR_TYPE, ValueFactory.newString(type));
      }
      if (file != null) {
        mp.put(MP_ERROR_FILE, ValueFactory.newString(file));
      }
      if (line != null) {
        mp.put(MP_ERROR_LINE, ValueFactory.newInteger(line));
      }
      if (message != null) {
        mp.put(MP_ERROR_MESSAGE, ValueFactory.newString(message));
      }
      if (errno != null) {
        mp.put(MP_ERROR_ERRNO, ValueFactory.newInteger(errno));
      }
      if (errcode != null) {
        mp.put(MP_ERROR_ERRCODE, ValueFactory.newInteger(errcode));
      }
      if (fields != null) {
        mp.put(MP_ERROR_FIELDS, fields);
      }
      encodedStack.add(mp.build());
    }

    BoxError result = BoxError.fromIProtoMessage(new IProtoResponseImpl(
        ValueFactory.newMap(
            MP_IPROTO_REQUEST_TYPE, ValueFactory.newInteger(requestType)
        ),
        ValueFactory.newMap(
            MP_IPROTO_ERROR_24, ValueFactory.newString(tarantoolMessage),
            MP_IPROTO_ERROR, ValueFactory.newMap(
                MP_ERROR_STACK, ValueFactory.newArray(encodedStack)
            )
        )
    ));
    assertEquals(exceptionMessage, result.getMessage());
    assertEquals(requestType - IPROTO_ERROR_BASE, result.getErrorCode());
    assertEquals(tarantoolMessage, result.getTarantoolMessage());
    assertEquals(stack.size(), result.getTarantoolStack().size());
    for (int i = 0; i < stack.size(); i++) {
      List<Object> expectedStackItem = stack.get(0);
      String type = (String) expectedStackItem.get(0);
      String file = (String) expectedStackItem.get(1);
      Long line = (Long) expectedStackItem.get(2);
      String message = (String) expectedStackItem.get(3);
      Long errno = (Long) expectedStackItem.get(4);
      Long errcode = (Long) expectedStackItem.get(5);
      MapValue fields = (MapValue) expectedStackItem.get(6);

      BoxErrorStackItem boxErrorStackItem = result.getTarantoolStack().get(0);

      assertEquals(type, boxErrorStackItem.getType());
      assertEquals(file, boxErrorStackItem.getFile());
      assertEquals(line, boxErrorStackItem.getLine());
      assertEquals(message, boxErrorStackItem.getMessage());
      assertEquals(errno, boxErrorStackItem.getErrno());
      assertEquals(errcode, boxErrorStackItem.getCode());
      assertEquals(fields, boxErrorStackItem.getDetails());
    }
  }
}
