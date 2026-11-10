/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.unit.exceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.exceptions.BoxErrorStackItem;
import io.tarantool.core.exceptions.BoxErrorValue;
import io.tarantool.core.protocol.IProtoResponseImpl;
import io.tarantool.core.protocol.requests.IProtoConstant;

@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class BoxErrorTest {

  /** The field is not passed: the key is missing in the map. */
  private static final Value ABSENT = null;

  /** The field is passed, but with the nil value. */
  private static final Value NIL = ValueFactory.newNil();

  public static Stream<Arguments> dataForBoxErrorStackItem() {
    return Stream.of(
        // All fields are passed, MP_ERROR_FIELDS is an empty map.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            "1",
            "2",
            3L,
            "4",
            5L,
            6L,
            ValueFactory.emptyMap()),
        // All fields are passed, MP_ERROR_FIELDS is a non-empty map.
        Arguments.of(
            struct(
                str("1"),
                str("2"),
                num(3),
                str("4"),
                num(5),
                num(6),
                ValueFactory.newMap(
                    ValueFactory.newString("a"), ValueFactory.newString("b"),
                    ValueFactory.newInteger(1), ValueFactory.newInteger(2))),
            "1",
            "2",
            3L,
            "4",
            5L,
            6L,
            ValueFactory.newMap(
                ValueFactory.newString("a"), ValueFactory.newString("b"),
                ValueFactory.newInteger(1), ValueFactory.newInteger(2))),
        // MP_ERROR_FIELDS is not passed: the fields are null.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), num(6), ABSENT),
            "1",
            "2",
            3L,
            "4",
            5L,
            6L,
            null),
        // MP_ERROR_FIELDS = nil: the fields are null.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), num(6), NIL),
            "1",
            "2",
            3L,
            "4",
            5L,
            6L,
            null));
  }

  @ParameterizedTest
  @MethodSource("dataForBoxErrorStackItem")
  public void testBoxErrorStackItem(
      MapValue struct,
      String expectedType,
      String expectedFile,
      long expectedLine,
      String expectedMessage,
      long expectedErrNo,
      long expectedErrCode,
      MapValue expectedFields) {
    final BoxErrorStackItem stack = BoxErrorStackItem.fromStruct(struct);
    Assertions.assertEquals(expectedType, stack.type());
    Assertions.assertEquals(expectedFile, stack.file());
    Assertions.assertEquals(expectedLine, stack.line());
    Assertions.assertEquals(expectedMessage, stack.message());
    Assertions.assertEquals(expectedErrNo, stack.errNo());
    Assertions.assertEquals(expectedErrCode, stack.code());
    Assertions.assertEquals(expectedFields, stack.fields());
  }

  @ParameterizedTest
  @MethodSource("dataForBoxErrorStackItem")
  public void testBoxErrorStackItemSerialization(
      MapValue struct,
      String expectedType,
      String expectedFile,
      long expectedLine,
      String expectedMessage,
      long expectedErrNo,
      long expectedErrCode,
      MapValue expectedFields)
      throws IOException {
    final BoxErrorStackItem stack = BoxErrorStackItem.fromStruct(struct);

    final MapValue serialized = stack.serialize();
    // Tarantool does not accept nil values in the MP_ERROR fields.
    for (Map.Entry<Value, Value> entry : serialized.map().entrySet()) {
      Assertions.assertFalse(entry.getValue().isNilValue());
    }

    final BoxErrorStackItem restored = BoxErrorStackItem.fromStruct(serialized);
    Assertions.assertEquals(expectedType, restored.type());
    Assertions.assertEquals(expectedFile, restored.file());
    Assertions.assertEquals(expectedLine, restored.line());
    Assertions.assertEquals(expectedMessage, restored.message());
    Assertions.assertEquals(expectedErrNo, restored.errNo());
    Assertions.assertEquals(expectedErrCode, restored.code());
    Assertions.assertEquals(expectedFields, restored.fields());
  }

  @Test
  public void testBoxErrorValueSerialization() throws IOException {
    final BoxErrorValue value =
        new BoxErrorValue(
            Collections.singletonList(
                BoxErrorStackItem.builder()
                    .withType("ClientError")
                    .withLine(42L)
                    .withFile("file.lua")
                    .withMessage("message")
                    .withErrNo(7L)
                    .withCode(32L)
                    .withFields(null)
                    .build()));

    final Value unpacked;
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      value.serialize(packer);
      unpacked = MessagePack.newDefaultUnpacker(packer.toByteArray()).unpackValue();
    }

    // The MP_ERROR object is a map with the only MP_ERROR_STACK key.
    Assertions.assertTrue(unpacked.isMapValue());
    final Map<Value, Value> errorMap = unpacked.asMapValue().map();
    Assertions.assertEquals(1, errorMap.size());
    Assertions.assertTrue(errorMap.containsKey(IProtoConstant.MP_ERROR_STACK));

    final BoxErrorValue restored = BoxErrorValue.from(unpacked);
    Assertions.assertEquals("message", restored.stack().get(0).message());
    Assertions.assertEquals(1, restored.stack().size());

    final BoxErrorStackItem stackItem = restored.stack().get(0);
    Assertions.assertEquals("ClientError", stackItem.type());
    Assertions.assertEquals("file.lua", stackItem.file());
    Assertions.assertEquals(42L, stackItem.line());
    Assertions.assertEquals("message", stackItem.message());
    Assertions.assertEquals(7L, stackItem.errNo());
    Assertions.assertEquals(32L, stackItem.code());
    Assertions.assertNull(stackItem.fields());
  }

  @Test
  public void testBoxErrorValueFromNil() {
    // The MP_ERROR value may be nil: there is no object in this case.
    Assertions.assertNull(BoxErrorValue.from(null));
    Assertions.assertNull(BoxErrorValue.from(ValueFactory.newNil()));
  }

  @Test
  public void testBoxErrorValueWithEmptyStack() {
    // The MP_ERROR object must have at least one stack item.
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> new BoxErrorValue(Collections.emptyList()));
  }

  public static Stream<Arguments> dataForInvalidBoxErrorStackItem() {
    return Stream.of(
        // The structure is not passed at all.
        Arguments.of(
            null,
            IllegalArgumentException.class,
            "IPROTO_STACK object must have any fields! Current: 'null'"),
        // The structure is passed, but empty.
        Arguments.of(
            ValueFactory.emptyMap(),
            IllegalArgumentException.class,
            "IPROTO_STACK object must have any fields! Current: 'null'"),
        // MP_ERROR_TYPE has an invalid type.
        Arguments.of(
            struct(num(1), str("2"), num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            IllegalArgumentException.class,
            "MP_ERROR_TYPE must be String!"),
        // MP_ERROR_FILE has an invalid type.
        Arguments.of(
            struct(str("1"), num(2), num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            IllegalArgumentException.class,
            "MP_ERROR_FILE must be String!"),
        // MP_ERROR_LINE has an invalid type.
        Arguments.of(
            struct(str("1"), str("2"), str("3"), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            IllegalArgumentException.class,
            "MP_ERROR_LINE must be Long!"),
        // MP_ERROR_MESSAGE has an invalid type.
        Arguments.of(
            struct(str("1"), str("2"), num(3), num(4), num(5), num(6), ValueFactory.emptyMap()),
            IllegalArgumentException.class,
            "MP_ERROR_MESSAGE must be String!"),
        // MP_ERROR_ERRNO has an invalid type.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), str("5"), num(6), ValueFactory.emptyMap()),
            IllegalArgumentException.class,
            "MP_ERROR_ERRNO must be Long!"),
        // MP_ERROR_ERRCODE has an invalid type.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), str("6"), ValueFactory.emptyMap()),
            IllegalArgumentException.class,
            "MP_ERROR_ERRCODE must be Long!"),
        // MP_ERROR_FIELDS has an invalid type.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), num(6), str("7")),
            IllegalArgumentException.class,
            "MP_ERROR_FIELDS must be map!"),
        // MP_ERROR_TYPE is not passed.
        Arguments.of(
            struct(ABSENT, str("2"), num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_TYPE must not be null!. Current: null"),
        // MP_ERROR_TYPE = nil.
        Arguments.of(
            struct(NIL, str("2"), num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_TYPE must not be null!. Current: null"),
        // MP_ERROR_FILE is not passed.
        Arguments.of(
            struct(str("1"), ABSENT, num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_FILE must not be null!. Current: null"),
        // MP_ERROR_FILE = nil.
        Arguments.of(
            struct(str("1"), NIL, num(3), str("4"), num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_FILE must not be null!. Current: null"),
        // MP_ERROR_LINE is not passed.
        Arguments.of(
            struct(str("1"), str("2"), ABSENT, str("4"), num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_LINE must not be null!. Current: null"),
        // MP_ERROR_LINE = nil.
        Arguments.of(
            struct(str("1"), str("2"), NIL, str("4"), num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_LINE must not be null!. Current: null"),
        // MP_ERROR_MESSAGE is not passed.
        Arguments.of(
            struct(str("1"), str("2"), num(3), ABSENT, num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_MESSAGE must not be null!. Current: null"),
        // MP_ERROR_MESSAGE = nil.
        Arguments.of(
            struct(str("1"), str("2"), num(3), NIL, num(5), num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_MESSAGE must not be null!. Current: null"),
        // MP_ERROR_ERRNO is not passed.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), ABSENT, num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_ERRNO must not be null!. Current: null"),
        // MP_ERROR_ERRNO = nil.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), NIL, num(6), ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_ERRNO must not be null!. Current: null"),
        // MP_ERROR_ERRCODE is not passed.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), ABSENT, ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_ERRCODE must not be null!. Current: null"),
        // MP_ERROR_ERRCODE = nil.
        Arguments.of(
            struct(str("1"), str("2"), num(3), str("4"), num(5), NIL, ValueFactory.emptyMap()),
            NullPointerException.class,
            "MP_ERROR_ERRCODE must not be null!. Current: null"));
  }

  @ParameterizedTest
  @MethodSource("dataForInvalidBoxErrorStackItem")
  public void testInvalidBoxErrorStackItem(
      MapValue struct, Class<? extends Exception> expectedType, String expectedMessage) {
    final Exception exception =
        Assertions.assertThrows(expectedType, () -> BoxErrorStackItem.fromStruct(struct));
    Assertions.assertEquals(expectedMessage, exception.getMessage());
  }

  public static Stream<Arguments> dataForBoxError() {
    // The error message in the response is always the message of the top stack item.
    return Stream.of(
        Arguments.of(
            IProtoConstant.IPROTO_ERROR_BASE,
            "4",
            Collections.singletonList(
                Arrays.asList("1", "2", 3L, "4", 5L, 6L, ValueFactory.emptyMap())),
            "4",
            "BoxError{code='0', message='4', stack=[BoxErrorStackItem{type='1',"
                + " line='3', file='2', message='4', errno='5', code='6', fields='{}'}]}"),
        Arguments.of(
            IProtoConstant.IPROTO_ERROR_BASE,
            "4",
            Arrays.asList(
                Arrays.asList("1", "2", 3L, "4", 5L, 6L, ValueFactory.emptyMap()),
                Arrays.asList("8", "9", 10L, "11", 12L, 13L, ValueFactory.emptyMap())),
            "4",
            "BoxError{code='0', message='4', stack=[BoxErrorStackItem{type='1',"
                + " line='3', file='2', message='4', errno='5', code='6', fields='{}'},"
                + " BoxErrorStackItem{type='8', line='10', file='9', message='11', errno='12',"
                + " code='13', fields='{}'}]}"));
  }

  @ParameterizedTest
  @MethodSource("dataForBoxError")
  public void testBoxError(
      int requestType,
      String tarantoolMessage,
      List<List<Object>> stack,
      String expectedMessage,
      String exceptionMessage) {
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
        mp.put(IProtoConstant.MP_ERROR_TYPE, ValueFactory.newString(type));
      }
      if (file != null) {
        mp.put(IProtoConstant.MP_ERROR_FILE, ValueFactory.newString(file));
      }
      if (line != null) {
        mp.put(IProtoConstant.MP_ERROR_LINE, ValueFactory.newInteger(line));
      }
      if (message != null) {
        mp.put(IProtoConstant.MP_ERROR_MESSAGE, ValueFactory.newString(message));
      }
      if (errno != null) {
        mp.put(IProtoConstant.MP_ERROR_ERRNO, ValueFactory.newInteger(errno));
      }
      if (errcode != null) {
        mp.put(IProtoConstant.MP_ERROR_ERRCODE, ValueFactory.newInteger(errcode));
      }
      if (fields != null) {
        mp.put(IProtoConstant.MP_ERROR_FIELDS, fields);
      }
      encodedStack.add(mp.build());
    }

    BoxError result =
        BoxError.fromIProtoMessage(
            new IProtoResponseImpl(
                ValueFactory.newMap(
                    IProtoConstant.MP_IPROTO_REQUEST_TYPE, ValueFactory.newInteger(requestType)),
                ValueFactory.newMap(
                    IProtoConstant.MP_IPROTO_ERROR_24,
                    ValueFactory.newString(tarantoolMessage),
                    IProtoConstant.MP_IPROTO_ERROR,
                    ValueFactory.newMap(
                        IProtoConstant.MP_ERROR_STACK, ValueFactory.newArray(encodedStack)))));
    Assertions.assertEquals(exceptionMessage, result.getMessage());
    Assertions.assertEquals(requestType - IProtoConstant.IPROTO_ERROR_BASE, result.errorCode());
    Assertions.assertEquals(expectedMessage, result.tarantoolMessage());
    Assertions.assertEquals(stack.size(), result.stack().size());
    for (int i = 0; i < stack.size(); i++) {
      List<Object> expectedStackItem = stack.get(i);
      String type = (String) expectedStackItem.get(0);
      String file = (String) expectedStackItem.get(1);
      Long line = (Long) expectedStackItem.get(2);
      String message = (String) expectedStackItem.get(3);
      Long errno = (Long) expectedStackItem.get(4);
      Long errcode = (Long) expectedStackItem.get(5);
      MapValue fields = (MapValue) expectedStackItem.get(6);

      BoxErrorStackItem boxErrorStackItem = result.stack().get(i);

      Assertions.assertEquals(type, boxErrorStackItem.type());
      Assertions.assertEquals(file, boxErrorStackItem.file());
      Assertions.assertEquals(line, boxErrorStackItem.line());
      Assertions.assertEquals(message, boxErrorStackItem.message());
      Assertions.assertEquals(errno, boxErrorStackItem.errNo());
      Assertions.assertEquals(errcode, boxErrorStackItem.code());
      Assertions.assertEquals(fields, boxErrorStackItem.fields());
    }
  }

  public static Stream<Arguments> dataForInvalidBoxError() {
    return Stream.of(
        // The error object is not passed in the response.
        Arguments.of(
            ValueFactory.newMap(
                IProtoConstant.MP_IPROTO_ERROR_24, ValueFactory.newString("message")),
            NullPointerException.class,
            "IPROTO_ERROR must not be null!"),
        // The error message is not passed in the response.
        Arguments.of(
            ValueFactory.newMap(IProtoConstant.MP_IPROTO_ERROR, errorValue()),
            NullPointerException.class,
            "ERROR_24 must not be null!"),
        // The stack of the error object is empty.
        Arguments.of(
            ValueFactory.newMap(
                IProtoConstant.MP_IPROTO_ERROR_24,
                ValueFactory.newString("message"),
                IProtoConstant.MP_IPROTO_ERROR,
                ValueFactory.newMap(IProtoConstant.MP_ERROR_STACK, ValueFactory.newArray())),
            IllegalArgumentException.class,
            "IPROTO_STACK must have at least 1 stack item!"));
  }

  @ParameterizedTest
  @MethodSource("dataForInvalidBoxError")
  public void testInvalidBoxError(
      MapValue body, Class<? extends Exception> expectedType, String expectedMessage) {
    final Exception exception =
        Assertions.assertThrows(
            expectedType,
            () ->
                BoxError.fromIProtoMessage(
                    new IProtoResponseImpl(
                        ValueFactory.newMap(
                            IProtoConstant.MP_IPROTO_REQUEST_TYPE,
                            ValueFactory.newInteger(IProtoConstant.IPROTO_ERROR_BASE)),
                        body)));
    Assertions.assertEquals(expectedMessage, exception.getMessage());
  }

  /** Builds a valid MP_ERROR object with the single stack item. */
  private static MapValue errorValue() {
    return ValueFactory.newMap(
        IProtoConstant.MP_ERROR_STACK,
        ValueFactory.newArray(
            ValueFactory.newMap(
                IProtoConstant.MP_ERROR_TYPE,
                ValueFactory.newString("ClientError"),
                IProtoConstant.MP_ERROR_FILE,
                ValueFactory.newString("file.lua"),
                IProtoConstant.MP_ERROR_MESSAGE,
                ValueFactory.newString("message"),
                IProtoConstant.MP_ERROR_LINE,
                ValueFactory.newInteger(42),
                IProtoConstant.MP_ERROR_ERRNO,
                ValueFactory.newInteger(0),
                IProtoConstant.MP_ERROR_ERRCODE,
                ValueFactory.newInteger(32))));
  }

  private static Value str(String value) {
    return ValueFactory.newString(value);
  }

  private static Value num(long value) {
    return ValueFactory.newInteger(value);
  }

  /**
   * Builds the error object map. The {@code null} parameter means that the key is not passed at
   * all, {@link #NIL} - that the key is passed with the nil value.
   */
  private static MapValue struct(
      Value type, Value file, Value line, Value message, Value errno, Value errcode, Value fields) {
    ValueFactory.MapBuilder mp = ValueFactory.newMapBuilder();
    if (type != null) {
      mp.put(IProtoConstant.MP_ERROR_TYPE, type);
    }
    if (file != null) {
      mp.put(IProtoConstant.MP_ERROR_FILE, file);
    }
    if (line != null) {
      mp.put(IProtoConstant.MP_ERROR_LINE, line);
    }
    if (message != null) {
      mp.put(IProtoConstant.MP_ERROR_MESSAGE, message);
    }
    if (errno != null) {
      mp.put(IProtoConstant.MP_ERROR_ERRNO, errno);
    }
    if (errcode != null) {
      mp.put(IProtoConstant.MP_ERROR_ERRCODE, errcode);
    }
    if (fields != null) {
      mp.put(IProtoConstant.MP_ERROR_FIELDS, fields);
    }
    return mp.build();
  }
}
