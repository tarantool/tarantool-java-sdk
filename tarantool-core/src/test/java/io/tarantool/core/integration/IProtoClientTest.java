/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.netty.util.HashedWheelTimer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.StringValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.tarantool.core.HelpersUtils.findRootCause;
import static io.tarantool.core.IProtoClientImpl.DEFAULT_WATCHER_OPTS;
import static io.tarantool.core.IProtoFeature.PAGINATION;
import static io.tarantool.core.IProtoFeature.STREAMS;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_ACCESS_DENIED;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_CREDS_MISMATCH;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_INVALID_MSGPACK;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_NO_SUCH_PROC;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_PROC_LUA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_SPACE_DOES_NOT_EXIST;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_UNKNOWN;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_OK;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SCHEMA_VERSION;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_STMT_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TUPLE_FORMATS;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.IProtoFeature;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.exceptions.BoxErrorStackItem;
import io.tarantool.core.exceptions.ClientException;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;

@Timeout(value = 10)
@Testcontainers
public class IProtoClientTest extends BaseTest {

  private static final IProtoRequestOpts DEFAULT_REQUEST_OPTS =
      IProtoRequestOpts.empty().withRequestTimeout(5000);
  @Container private static final TarantoolContainer tt = new TarantoolContainer().withEnv(ENV_MAP);
  private static int spaceAId;
  private static int spaceBId;

  private static String spaceAName;

  private static String indexAName;

  private static int schemaVersion;
  private static InetSocketAddress address;
  private static char tarantoolVersion;

  @BeforeAll
  public static void setUp() throws Exception {
    List<?> result = tt.executeCommandDecoded("return box.space.space_a.id");
    spaceAId = (Integer) result.get(0);

    result = tt.executeCommandDecoded("return box.space.space_b.id");
    spaceBId = (Integer) result.get(0);

    result = tt.executeCommandDecoded("return box.space.space_a.name");
    spaceAName = (String) result.get(0);

    result = tt.executeCommandDecoded("return box.space.space_a.index[0].name");
    indexAName = (String) result.get(0);

    result =
        tt.executeCommandDecoded(
            "do local net = require('net.box'); "
                + "local c = net.connect('127.0.0.1:3301'); "
                + "return c.schema_version end");
    schemaVersion = (Integer) result.get(0);

    address = new InetSocketAddress(tt.getHost(), tt.getPort());

    try {
      tarantoolVersion = System.getenv("TARANTOOL_VERSION").charAt(0);
    } catch (Exception e) {
      tarantoolVersion = '2';
    }
  }

  public static byte[] ArrayValueToBytes(ArrayValue arrayValue) throws IOException {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packValue(arrayValue);
      return packer.toByteArray();
    }
  }

  private static Stream<Arguments> dataForTestSelect() {
    return Stream.of(
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            0,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "box.space.space_a:insert({'key_a', 'value_a'})",
            ValueFactory.newArray(ValueFactory.newString("key_a")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_a"), ValueFactory.newString("value_a"))),
            spaceAId,
            0,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "box.space.space_a:insert({'key_a', 'value_a'});"
                + "box.space.space_a:insert({'key_b', 'value_b'})",
            ValueFactory.newArray(ValueFactory.newString("key_a")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_a"), ValueFactory.newString("value_a")),
                ValueFactory.newArray(
                    ValueFactory.newString("key_b"), ValueFactory.newString("value_b"))),
            spaceAId,
            0,
            2,
            0,
            BoxIterator.GE));
  }

  private static Stream<Arguments> dataForTestSelectWithSpaceAndIndexNames() {
    return Stream.of(
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            spaceAName,
            0,
            indexAName,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            spaceAName,
            0,
            null,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            spaceAName,
            null,
            indexAName,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            null,
            0,
            indexAName,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            null,
            0,
            null,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            spaceAId,
            null,
            null,
            indexAName,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            null,
            spaceAName,
            0,
            indexAName,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            null,
            spaceAName,
            0,
            null,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            "return",
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            ValueFactory.emptyArray(),
            null,
            spaceAName,
            null,
            indexAName,
            1,
            0,
            BoxIterator.EQ));
  }

  private static Stream<Arguments> dataForTestInsertAndReplace() {
    ArrayValue tuple =
        ValueFactory.newArray(ValueFactory.newString("key_c"), ValueFactory.newString("value_c"));
    return Stream.of(
        Arguments.of(spaceAId, tuple, "return box.space.space_a:get('key_c')"),
        Arguments.of(spaceBId, tuple, "return box.space.space_b:get('key_c')"));
  }

  private static Stream<Arguments> dataForTestInsertAndReplaceWithSpaceAndeIndexNames() {
    ArrayValue tuple =
        ValueFactory.newArray(ValueFactory.newString("key_c"), ValueFactory.newString("value_c"));
    return Stream.of(
        Arguments.of(null, spaceAName, tuple, "return box.space.space_a:get('key_c')"),
        Arguments.of(spaceAId, null, tuple, "return box.space.space_a:get('key_c')"),
        Arguments.of(spaceAId, spaceAName, tuple, "return box.space.space_a:get('key_c')"));
  }

  private static Stream<Arguments> dataForTestUpdate() {
    return Stream.of(
        // normal update
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            0,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        // update in space with trigger calling box.session.push
        Arguments.of(
            "return insert('space_b', {'key_b', 'value_b'})",
            spaceBId,
            0,
            ValueFactory.newArray(ValueFactory.newString("key_b")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_b"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_b"), ValueFactory.newString("new_value_b"))),
            "return box.space.space_b:get('key_b')"));
  }

  private static Stream<Arguments> dataForTestUpdateWithSpaceAndIndexNames() {
    return Stream.of(
        // normal update
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            spaceAName,
            0,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            spaceAName,
            0,
            null,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            spaceAName,
            null,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            null,
            0,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            null,
            0,
            null,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            spaceAId,
            null,
            null,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            null,
            spaceAName,
            0,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            null,
            spaceAName,
            0,
            null,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_c', 'value_c'})",
            null,
            spaceAName,
            null,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_c")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("="),
                    ValueFactory.newInteger(1),
                    ValueFactory.newString("new_value_c"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_c"), ValueFactory.newString("new_value_c"))),
            "return box.space.space_a:get('key_c')"));
  }

  private static Stream<Arguments> dataForTestDelete() {
    List<Arguments> argumentsList = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      argumentsList.add(
          // normal delete
          Arguments.of(
              "return box.space.space_a:insert({'key_d', 'value_d'})",
              spaceAId,
              0,
              ValueFactory.newArray(ValueFactory.newString("key_d")),
              ValueFactory.newArray(
                  ValueFactory.newArray(
                      ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
              "return box.space.space_a:get('key_d')",
              i % 2 == 0));
      argumentsList.add(
          // delete non-existing record
          Arguments.of(
              "return",
              spaceAId,
              0,
              ValueFactory.newArray(ValueFactory.newString("key_d")),
              ValueFactory.emptyArray(),
              "return box.space.space_a:get('key_d')",
              i % 2 == 0));
      argumentsList.add(
          // delete from space with trigger calling box.session.push()
          Arguments.of(
              "return insert('space_b', {'key_b', 'value_b'})",
              spaceBId,
              0,
              ValueFactory.newArray(ValueFactory.newString("key_b")),
              ValueFactory.newArray(
                  ValueFactory.newArray(
                      ValueFactory.newString("key_b"), ValueFactory.newString("value_b"))),
              "return box.space.space_b:get('key_b')",
              i % 2 == 0));
    }

    return argumentsList.stream();
  }

  private static Stream<Arguments> dataForTestDeleteWithSpaceAndIndexNames() {
    return Stream.of(
        // normal delete
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            spaceAId,
            spaceAName,
            0,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            spaceAId,
            spaceAName,
            0,
            null,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            spaceAId,
            spaceAName,
            null,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            spaceAId,
            null,
            0,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            spaceAId,
            null,
            0,
            null,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            spaceAId,
            null,
            null,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            null,
            spaceAName,
            0,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            null,
            spaceAName,
            0,
            null,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"),
        Arguments.of(
            "return box.space.space_a:insert({'key_d', 'value_d'})",
            null,
            spaceAName,
            null,
            indexAName,
            ValueFactory.newArray(ValueFactory.newString("key_d")),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_d"), ValueFactory.newString("value_d"))),
            "return box.space.space_a:get('key_d')"));
  }

  private static Stream<Arguments> dataForTestUpsert() {
    String stringKey = "key_e";
    StringValue key = ValueFactory.newString(stringKey);
    StringValue valueAfterInsert = ValueFactory.newString("value_insert");
    StringValue valueAfterUpdate = ValueFactory.newString("value_update");
    ArrayValue toInsert = ValueFactory.newArray(key, valueAfterInsert);
    ArrayValue toUpdate =
        ValueFactory.newArray(
            ValueFactory.newArray(
                ValueFactory.newString("="), ValueFactory.newInteger(1), valueAfterUpdate));
    String checkA = "return box.space.space_a:get('key_e')";
    String checkB = "return box.space.space_b:get('key_e')";
    return Stream.of(
        Arguments.of(
            "return box.space.space_a:insert({'key_e', 'value_old'})",
            spaceAId,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterUpdate),
            checkA),
        Arguments.of(
            "return",
            spaceAId,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterInsert),
            checkA),
        Arguments.of(
            "return insert('space_b', {'key_e', 'value_old'})",
            spaceBId,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterUpdate),
            checkB),
        Arguments.of(
            "return",
            spaceBId,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterInsert),
            checkB));
  }

  private static Stream<Arguments> dataForTestUpsertWithSpaceAndIndexNames() {
    String stringKey = "key_e";
    StringValue key = ValueFactory.newString(stringKey);
    StringValue valueAfterInsert = ValueFactory.newString("value_insert");
    StringValue valueAfterUpdate = ValueFactory.newString("value_update");
    ArrayValue toInsert = ValueFactory.newArray(key, valueAfterInsert);
    ArrayValue toUpdate =
        ValueFactory.newArray(
            ValueFactory.newArray(
                ValueFactory.newString("="), ValueFactory.newInteger(1), valueAfterUpdate));
    String checkA = "return box.space.space_a:get('key_e')";
    return Stream.of(
        Arguments.of(
            "return box.space.space_a:insert({'key_e', 'value_old'})",
            spaceAId,
            spaceAName,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterUpdate),
            checkA),
        Arguments.of(
            "return box.space.space_a:insert({'key_e', 'value_old'})",
            null,
            spaceAName,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterUpdate),
            checkA),
        Arguments.of(
            "return box.space.space_a:insert({'key_e', 'value_old'})",
            spaceAId,
            null,
            0,
            toInsert,
            toUpdate,
            ValueFactory.newArray(key, valueAfterUpdate),
            checkA));
  }

  private static Stream<Arguments> dataForTestEval() {
    return Stream.of(
        Arguments.of(
            "local a, b, c = ...; return a + b, 'three'",
            ValueFactory.newArray(
                ValueFactory.newInteger(1),
                ValueFactory.newInteger(2),
                ValueFactory.newString("three")),
            ValueFactory.newArray(ValueFactory.newInteger(3), ValueFactory.newString("three"))),
        Arguments.of(
            "return nil", ValueFactory.newArray(), ValueFactory.newArray(ValueFactory.newNil())),
        Arguments.of("return", ValueFactory.emptyArray(), ValueFactory.emptyArray()),
        Arguments.of(
            "return nil, nil",
            ValueFactory.emptyArray(),
            ValueFactory.newArray(ValueFactory.newNil(), ValueFactory.newNil())),
        Arguments.of(
            "return 1, nil",
            ValueFactory.emptyArray(),
            ValueFactory.newArray(ValueFactory.newInteger(1), ValueFactory.newNil())),
        Arguments.of(
            "return nil, { 1, 2, 3 }",
            ValueFactory.emptyArray(),
            ValueFactory.newArray(
                ValueFactory.newNil(),
                ValueFactory.newArray(
                    ValueFactory.newInteger(1),
                    ValueFactory.newInteger(2),
                    ValueFactory.newInteger(3)))),
        Arguments.of(
            "return echo(1, nil)",
            ValueFactory.emptyArray(),
            ValueFactory.newArray(ValueFactory.newInteger(1), ValueFactory.newNil())),
        Arguments.of(
            "return nil, echo(1, box.NULL)",
            ValueFactory.emptyArray(),
            ValueFactory.newArray(
                ValueFactory.newNil(), ValueFactory.newInteger(1), ValueFactory.newNil())));
  }

  private static Stream<Arguments> dataForTestEvalError() {
    return Stream.of(
        Arguments.of("return echo", IPROTO_ERR_INVALID_MSGPACK),
        Arguments.of("return error('lua error')", IPROTO_ERR_INVALID_MSGPACK),
        Arguments.of("error('lua error')", IPROTO_ERR_INVALID_MSGPACK),
        Arguments.of("return 'zero' / 0", IPROTO_ERR_INVALID_MSGPACK),
        Arguments.of("return box.error({reason = 'box.error'})", IPROTO_ERR_UNKNOWN),
        Arguments.of("return box.error({reason = 'box.error', code = 32})", IPROTO_ERR_PROC_LUA));
  }

  public static Stream<Arguments> dataForTestEvalWithPushHandler() {
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    return Stream.of(
        Arguments.of(
            "return echo_with_push(...)",
            args,
            Arrays.asList(
                ValueFactory.newArray(
                    ValueFactory.newArray(
                        ValueFactory.newString("push1"), ValueFactory.newString("out of band"))),
                ValueFactory.newArray(
                    ValueFactory.newArray(
                        ValueFactory.newString("push2"), ValueFactory.newString("out of band"))))),
        Arguments.of("return echo(...)", args, Collections.emptyList()));
  }

  private static Stream<Arguments> dataForTestCallError() {
    return Stream.of(
        Arguments.of(
            "nonecho",
            IPROTO_ERR_NO_SUCH_PROC,
            Collections.singletonList(
                Arrays.asList(
                    "ClientError",
                    "./src/box/lua/call.c",
                    "Procedure 'nonecho' is not defined",
                    33L,
                    0L))),
        Arguments.of(
            "fail",
            IPROTO_ERR_PROC_LUA,
            Collections.singletonList(
                Arrays.asList("LuajitError", "./src/lua/utils.c", "Fail!", 32L, 0L))),
        Arguments.of(
            "fail_by_box_error",
            IPROTO_ERR_UNKNOWN,
            Collections.singletonList(
                Arrays.asList("ClientError", "/app/server.lua", "fail", 0L, 0L))),
        Arguments.of(
            "wrong_ret",
            IPROTO_ERR_INVALID_MSGPACK,
            Collections.singletonList(
                Arrays.asList(
                    "LuajitError",
                    "./src/lua/serializer.c",
                    "unsupported Lua type 'function'",
                    32L,
                    0L))),
        Arguments.of(
            "wrapped_fail_by_box_error",
            IPROTO_ERR_UNKNOWN,
            Arrays.asList(
                Arrays.asList("ClientError", "/app/server.lua", "wrapped failure", 0L, 0L),
                Arrays.asList("ClientError", "/app/server.lua", "fail", 0L, 0L))));
  }

  private static Stream<Arguments> dataForTestCallWithPushHandler() {
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    return Stream.of(
        Arguments.of(
            "echo_with_push",
            args,
            Arrays.asList(
                ValueFactory.newArray(
                    ValueFactory.newArray(
                        ValueFactory.newString("push1"), ValueFactory.newString("out of band"))),
                ValueFactory.newArray(
                    ValueFactory.newArray(
                        ValueFactory.newString("push2"), ValueFactory.newString("out of band"))))),
        Arguments.of("echo", args, Collections.emptyList()));
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    tt.executeCommand("return box.space.test:truncate()");
    tt.executeCommand("return box.space.space_a:truncate()");
    tt.executeCommand("return box.space.space_b:truncate()");
  }

  private void checkMessageHeader(IProtoMessage message, int requestType, long syncId) {
    assertEquals(requestType, message.getRequestType());
    assertEquals(syncId, message.getSyncId());
    assertEquals(schemaVersion, message.getHeaderIntegerValue(IPROTO_SCHEMA_VERSION).asLong());
  }

  @SuppressWarnings("unchecked")
  private void checkTuple(String ttCheck, ArrayValue tuple) throws Exception {
    List<Object> result = tt.executeCommandDecoded(ttCheck);
    List<Object> stored = (List<Object>) result.get(0);
    assertEquals(
        tuple,
        ValueFactory.newArray(
            ValueFactory.newString((String) stored.get(0)),
            ValueFactory.newString((String) stored.get(1))));
  }

  @ParameterizedTest
  @MethodSource("dataForTestSelect")
  public void testSelect(
      String toPrepare,
      ArrayValue key,
      ArrayValue expected,
      Integer space,
      Integer index,
      Integer limit,
      Integer offset,
      BoxIterator iterator)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.select(space, index, key, limit, offset, iterator).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
  }

  @ParameterizedTest
  @MethodSource("dataForTestSelect")
  public void testRawSelect(
      String toPrepare,
      ArrayValue key,
      ArrayValue expected,
      Integer space,
      Integer index,
      Integer limit,
      Integer offset,
      BoxIterator iterator)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawKey = ArrayValueToBytes(key);
    IProtoMessage message = client.select(space, index, rawKey, limit, offset, iterator).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
  }

  @ParameterizedTest
  @MethodSource("dataForTestSelectWithSpaceAndIndexNames")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testSelectWithSpaceAndIndexNames(
      String toPrepare,
      ArrayValue key,
      ArrayValue expected,
      Integer space,
      String spaceName,
      Integer index,
      String indexName,
      Integer limit,
      Integer offset,
      BoxIterator iterator)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawKey = ArrayValueToBytes(key);

    IProtoMessage message =
        client
            .select(
                space,
                spaceName,
                index,
                indexName,
                rawKey,
                limit,
                offset,
                iterator,
                false,
                null,
                null,
                DEFAULT_REQUEST_OPTS)
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(expected, message.getBodyArrayValue(IPROTO_DATA));

    message =
        client
            .select(
                space,
                spaceName,
                index,
                indexName,
                key,
                limit,
                offset,
                iterator,
                false,
                null,
                null,
                DEFAULT_REQUEST_OPTS)
            .get();
    checkMessageHeader(message, IPROTO_OK, 5);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndReplace")
  public void testInsert(Integer space, ArrayValue tuple, String check) throws Exception {

    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.insert(space, tuple).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndReplace")
  public void testRawInsert(Integer space, ArrayValue tuple, String check) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawTuple = ArrayValueToBytes(tuple);
    IProtoMessage message = client.insert(space, rawTuple).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndReplaceWithSpaceAndeIndexNames")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testInsertWithSpaceAndIndexNames(
      Integer space, String spaceName, ArrayValue tuple, String check) throws Exception {

    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawTuple = ArrayValueToBytes(tuple);
    IProtoMessage message = client.insert(space, spaceName, rawTuple, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);

    truncateSpaces();
    message = client.insert(space, spaceName, tuple, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndReplace")
  public void testReplace(Integer space, ArrayValue tuple, String check) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.replace(space, tuple).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndReplace")
  public void testRawReplace(Integer space, ArrayValue tuple, String check) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawTuple = ArrayValueToBytes(tuple);
    IProtoMessage message = client.replace(space, rawTuple).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndReplaceWithSpaceAndeIndexNames")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testReplaceWithSpaceAndIndexNames(
      Integer space, String spaceName, ArrayValue tuple, String check) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawTuple = ArrayValueToBytes(tuple);
    IProtoMessage message = client.replace(space, spaceName, rawTuple, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);

    message = client.replace(space, spaceName, tuple, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));
    checkTuple(check, tuple);
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpdate")
  public void testUpdate(
      String toPrepare,
      Integer space,
      Integer index,
      ArrayValue key,
      ArrayValue operations,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.update(space, index, key, operations).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    checkTuple(check, expected.get(0).asArrayValue());
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpdate")
  public void testRawUpdate(
      String toPrepare,
      Integer space,
      Integer index,
      ArrayValue key,
      ArrayValue operations,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawKey = ArrayValueToBytes(key);
    byte[] rawOperations = ArrayValueToBytes(operations);
    IProtoMessage message = client.update(space, index, rawKey, rawOperations).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    checkTuple(check, expected.get(0).asArrayValue());
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpdateWithSpaceAndIndexNames")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testUpdateWithSpaceAndIndexNames(
      String toPrepare,
      Integer space,
      String spaceName,
      Integer index,
      String indexName,
      ArrayValue key,
      ArrayValue operations,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawKey = ArrayValueToBytes(key);
    byte[] rawOperations = ArrayValueToBytes(operations);
    IProtoMessage message =
        client
            .update(space, spaceName, index, indexName, rawKey, rawOperations, DEFAULT_REQUEST_OPTS)
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    checkTuple(check, expected.get(0).asArrayValue());

    message =
        client
            .update(space, spaceName, index, indexName, key, operations, DEFAULT_REQUEST_OPTS)
            .get();
    checkMessageHeader(message, IPROTO_OK, 5);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    checkTuple(check, expected.get(0).asArrayValue());
  }

  @ParameterizedTest
  @MethodSource("dataForTestDelete")
  public void testDelete(
      String toPrepare,
      Integer space,
      Integer index,
      ArrayValue key,
      ArrayValue expected,
      String check,
      boolean useTupleExtension)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, useTupleExtension);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.delete(space, index, key).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    List<?> result = tt.executeCommandDecoded(check);
    assertEquals(0, result.size());
  }

  @ParameterizedTest
  @MethodSource("dataForTestDelete")
  public void testRawDelete(
      String toPrepare,
      Integer space,
      Integer index,
      ArrayValue key,
      ArrayValue expected,
      String check,
      boolean useTupleExtension)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, useTupleExtension);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawKey = ArrayValueToBytes(key);
    IProtoMessage message = client.delete(space, index, rawKey).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    List<?> result = tt.executeCommandDecoded(check);
    assertEquals(0, result.size());
  }

  @ParameterizedTest
  @MethodSource("dataForTestDeleteWithSpaceAndIndexNames")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testDeleteWithSpaceAndIndexNames(
      String toPrepare,
      Integer space,
      String spaceName,
      Integer index,
      String indexName,
      ArrayValue key,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawKey = ArrayValueToBytes(key);
    IProtoMessage message =
        client.delete(space, spaceName, index, indexName, rawKey, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    List<?> result = tt.executeCommandDecoded(check);
    assertEquals(0, result.size());

    tt.executeCommand(toPrepare);
    message = client.delete(space, spaceName, index, indexName, key, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(expected, decodeTuple(client, data));
    result = tt.executeCommandDecoded(check);
    assertEquals(0, result.size());
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpsert")
  public void testUpsert(
      String toPrepare,
      Integer space,
      Integer index,
      ArrayValue toInsert,
      ArrayValue toUpdate,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.upsert(space, index, toInsert, toUpdate).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(0, message.getBodyArrayValue(IPROTO_DATA).size());
    checkTuple(check, expected);
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpsert")
  public void testRawUpsert(
      String toPrepare,
      Integer space,
      Integer index,
      ArrayValue toInsert,
      ArrayValue toUpdate,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawToInsert = ArrayValueToBytes(toInsert);
    byte[] rawToUpdate = ArrayValueToBytes(toUpdate);
    IProtoMessage message = client.upsert(space, index, rawToInsert, rawToUpdate).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(0, message.getBodyArrayValue(IPROTO_DATA).size());
    checkTuple(check, expected);
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpsertWithSpaceAndIndexNames")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testUpsertWithSpaceAndIndexNames(
      String toPrepare,
      Integer space,
      String spaceName,
      Integer index,
      ArrayValue toInsert,
      ArrayValue toUpdate,
      ArrayValue expected,
      String check)
      throws Exception {
    tt.executeCommand(toPrepare);
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawToInsert = ArrayValueToBytes(toInsert);
    byte[] rawToUpdate = ArrayValueToBytes(toUpdate);
    IProtoMessage message =
        client
            .upsert(space, spaceName, index, rawToInsert, rawToUpdate, DEFAULT_REQUEST_OPTS)
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(0, message.getBodyArrayValue(IPROTO_DATA).size());
    checkTuple(check, expected);

    message =
        client.upsert(space, spaceName, index, toInsert, toUpdate, DEFAULT_REQUEST_OPTS).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    assertEquals(0, message.getBodyArrayValue(IPROTO_DATA).size());
    checkTuple(check, expected);
  }

  @Test
  public void testOperationsWithPushHandler() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();

    ArrayValue tuple =
        ValueFactory.newArray(ValueFactory.newString("key_f"), ValueFactory.newString("value_f"));
    List<ArrayValue> messages = new ArrayList<>();
    Consumer<IProtoMessage> consumer = m -> messages.add(m.getBodyArrayValue(IPROTO_DATA));
    IProtoMessage message;

    IProtoRequestOpts opts = IProtoRequestOpts.empty().withPushHandler(consumer);
    message = client.insert(spaceBId, null, tuple, opts).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));

    message = client.replace(spaceBId, null, tuple, opts).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tuple), decodeTuple(client, data));

    message =
        client
            .update(
                spaceBId,
                null,
                0,
                null,
                ValueFactory.newArray(ValueFactory.newString("key_f")),
                ValueFactory.newArray(
                    ValueFactory.newArray(
                        ValueFactory.newString("="),
                        ValueFactory.newInteger(1),
                        ValueFactory.newString("value_g"))),
                opts)
            .get();
    checkMessageHeader(message, IPROTO_OK, 6);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(
        ValueFactory.newArray(
            ValueFactory.newArray(
                ValueFactory.newString("key_f"), ValueFactory.newString("value_g"))),
        decodeTuple(client, data));

    message =
        client
            .delete(
                spaceBId,
                null,
                0,
                null,
                ValueFactory.newArray(ValueFactory.newString("key_f")),
                opts)
            .get();
    checkMessageHeader(message, IPROTO_OK, 7);
    data = message.getBodyArrayValue(IPROTO_DATA);
    assertEquals(
        ValueFactory.newArray(
            ValueFactory.newArray(
                ValueFactory.newString("key_f"), ValueFactory.newString("value_g"))),
        decodeTuple(client, data));

    assertEquals(
        Arrays.asList(
            ValueFactory.newArray(ValueFactory.newNil()),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_f"), ValueFactory.newString("value_f"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_f"), ValueFactory.newString("value_f"))),
            ValueFactory.newArray(
                ValueFactory.newArray(
                    ValueFactory.newString("key_f"), ValueFactory.newString("value_g")))),
        messages);
  }

  @Test
  public void testAuthorization() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    IProtoMessage message = client.authorize("user_a", "secret_a").get();
    checkMessageHeader(message, IPROTO_OK, 3);
    assertEquals(0, message.getBody().asMapValue().map().size());
  }

  @Test
  public void testWhenEvalWithGuestRoleThenThrowBoxError() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);

    CompletableFuture<IProtoResponse> response =
        client.eval("return box.info.version", ValueFactory.emptyArray());

    Throwable throwable =
        findRootCause(Assertions.assertThrows(CompletionException.class, response::join));

    Assertions.assertEquals(BoxError.class, throwable.getClass());
  }

  @Test
  public void testAuthenticationErrorIncorrectPassword() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    CompletableFuture<IProtoResponse> future = client.authorize("user_a", "secret_b");

    Exception ex = assertThrows(CompletionException.class, future::join);

    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());

    BoxError boxError = (BoxError) rootCause;

    List<String> ERROR_MESSAGES =
        Arrays.asList(
            "Incorrect password supplied for user 'user_a'",
            "User not found or supplied credentials are invalid");

    assertEquals(IPROTO_ERR_CREDS_MISMATCH, boxError.getErrorCode());
    assertEquals(1, boxError.getTarantoolStack().size());
    assertTrue(ERROR_MESSAGES.contains(boxError.getTarantoolMessage()));

    BoxErrorStackItem item = boxError.getTarantoolStack().get(0);
    assertEquals("ClientError", item.getType());
    assertTrue(ERROR_MESSAGES.contains(item.getMessage()));
  }

  @Test
  public void testAuthenticationErrorIncorrectUser() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    CompletableFuture<IProtoResponse> future = client.authorize("user_x", "secret_a");

    Exception ex = assertThrows(CompletionException.class, future::join);

    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());

    BoxError boxError = (BoxError) rootCause;

    String ERROR_MESSAGE = "User not found or supplied credentials are invalid";

    assertEquals(IPROTO_ERR_CREDS_MISMATCH, boxError.getErrorCode());
    assertEquals(1, boxError.getTarantoolStack().size());
    assertEquals(ERROR_MESSAGE, boxError.getTarantoolMessage());

    BoxErrorStackItem item = boxError.getTarantoolStack().get(0);
    assertEquals("ClientError", item.getType());
    assertEquals(ERROR_MESSAGE, item.getMessage());
  }

  @Test
  public void testAuthenticationErrorNoSessionGranted() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    CompletableFuture<IProtoResponse> future = client.authorize("user_c", "secret_c");

    Exception ex = assertThrows(CompletionException.class, future::join);

    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());

    BoxError boxError = (BoxError) rootCause;

    String ERROR_MESSAGE = "Session access to universe '' is denied for user 'user_c'";

    assertEquals(IPROTO_ERR_ACCESS_DENIED, boxError.getErrorCode());
    assertEquals(1, boxError.getTarantoolStack().size());
    assertEquals(ERROR_MESSAGE, boxError.getTarantoolMessage());

    BoxErrorStackItem item = boxError.getTarantoolStack().get(0);
    assertEquals("AccessDeniedError", item.getType());
    assertEquals(ERROR_MESSAGE, item.getMessage());
  }

  @Test
  public void testAuthorizationErrorNoUsageGranted() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    assertDoesNotThrow(() -> client.authorize("user_d", "secret_d").join());

    Exception ex =
        assertThrows(
            CompletionException.class,
            () -> client.eval("return 1 + 1", ValueFactory.emptyArray()).join());

    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());

    BoxError boxError = (BoxError) rootCause;

    String ERROR_MESSAGE = "Usage access to universe '' is denied for user 'user_d'";
    if (tarantoolVersion == '3') {
      ERROR_MESSAGE = "Execute access to universe '' is denied for user 'user_d'";
    }

    assertEquals(IPROTO_ERR_ACCESS_DENIED, boxError.getErrorCode());
    assertEquals(1, boxError.getTarantoolStack().size());
    String tarantoolMessage = boxError.getTarantoolMessage();
    assertEquals(tarantoolMessage, ERROR_MESSAGE);

    BoxErrorStackItem item = boxError.getTarantoolStack().get(0);
    assertEquals("AccessDeniedError", item.getType());
    tarantoolMessage = item.getMessage();
    assertEquals(tarantoolMessage, ERROR_MESSAGE);
  }

  @Test
  public void testPing() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.ping().get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(0, message.getBody().map().size());
  }

  @Test
  public void testTimeoutCancel() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.ping().get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(0, message.getBody().map().size());

    Set<io.netty.util.Timeout> timers = timerService.stop();
    assertTrue(timers.isEmpty());

    timerService = new HashedWheelTimer();
  }

  @Test
  public void testId() throws Exception {
    Set<IProtoFeature> iprotoFeature = EnumSet.allOf(IProtoFeature.class);

    IProtoClient client =
        new IProtoClientImpl(
            factory, factory.getTimerService(), DEFAULT_WATCHER_OPTS, null, null, true);
    assertEquals(iprotoFeature, client.getClientFeatures());
    assertEquals(7, client.getClientProtocolVersion());

    int clientFeatureAmount = 4;
    if (tarantoolVersion == '3') {
      clientFeatureAmount = 7;
    }

    Throwable ex = assertThrows(CompletionException.class, client::getServerFeatures).getCause();
    assertInstanceOf(ClientException.class, ex);
    assertEquals("Call connect before getting server details", ex.getMessage());
    ex = assertThrows(CompletionException.class, client::getServerProtocolVersion).getCause();
    assertEquals("Call connect before getting server details", ex.getMessage());

    client
        .connect(address, 3_000)
        .get(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    if (tarantoolVersion != '3') {
      iprotoFeature = EnumSet.range(STREAMS, PAGINATION);
      assertEquals(iprotoFeature, client.getServerFeatures());
      assertEquals(clientFeatureAmount, client.getServerProtocolVersion());
    }
  }

  @ParameterizedTest
  @MethodSource("dataForTestEval")
  public void testEval(String expression, ArrayValue args, ArrayValue expected) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    IProtoMessage message = client.eval(expression, args).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(expected, message.getBodyArrayValue(IPROTO_DATA));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEval")
  public void testRawEval(String expression, ArrayValue args, ArrayValue expected)
      throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    byte[] rawArgs = ArrayValueToBytes(args);
    IProtoMessage message = client.eval(expression, rawArgs).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(expected, message.getBodyArrayValue(IPROTO_DATA));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEvalError")
  public void testEvalError(String expression, Integer errorCode) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args = ValueFactory.emptyArray();
    CompletableFuture<IProtoResponse> future = client.eval(expression, args);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(errorCode, ((BoxError) rootCause).getErrorCode());
  }

  @ParameterizedTest
  @MethodSource("dataForTestEvalError")
  public void testRawEvalError(String expression, Integer errorCode) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args = ValueFactory.emptyArray();
    byte[] rawArgs = ArrayValueToBytes(args);
    CompletableFuture<IProtoResponse> future = client.eval(expression, rawArgs);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(errorCode, ((BoxError) rootCause).getErrorCode());
  }

  @ParameterizedTest
  @MethodSource("dataForTestEvalWithPushHandler")
  public void testEvalWithPushHandler(String expression, ArrayValue args, List<ArrayValue> expected)
      throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    List<ArrayValue> messages = new ArrayList<>();
    IProtoMessage message =
        client
            .eval(
                expression,
                args,
                IProtoRequestOpts.empty()
                    .withPushHandler(m -> messages.add(m.getBodyArrayValue(IPROTO_DATA))))
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
    assertEquals(expected, messages);
  }

  @ParameterizedTest
  @MethodSource("dataForTestEvalWithPushHandler")
  public void testRawEvalWithPushHandler(
      String expression, ArrayValue args, List<ArrayValue> expected) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    List<ArrayValue> messages = new ArrayList<>();
    byte[] rawArgs = ArrayValueToBytes(args);
    IProtoMessage message =
        client
            .eval(
                expression,
                rawArgs,
                null,
                IProtoRequestOpts.empty()
                    .withPushHandler(m -> messages.add(m.getBodyArrayValue(IPROTO_DATA))))
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
    assertEquals(expected, messages);
  }

  @ParameterizedTest
  @ValueSource(strings = {"echo", "echo_with_push"})
  public void testCall(String function) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    IProtoMessage message = client.call(function, args).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
  }

  @ParameterizedTest
  @ValueSource(strings = {"echo", "echo_with_push"})
  public void testRawCall(String function) throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    byte[] rawArgs = ArrayValueToBytes(args);
    IProtoMessage message = client.call(function, rawArgs).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testCallTimeout() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withRequestTimeout(1000);
    CompletableFuture<IProtoResponse> future = client.call("slow_echo", args, opts);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable cause = ex.getCause();
    assertEquals(TimeoutException.class, cause.getClass());
    assertEquals(
        "Request timeout: "
            + "IProtoCall(syncId = 4, function = slow_echo, args = [\"one\",1]); "
            + "timeout = 1000ms",
        cause.getMessage());
  }

  @Test
  public void testCallTimeoutWithIgnoredPacketsHandler() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    Map<Long, IProtoResponse> ignoredPackets = new HashMap<>();
    client.onIgnoredPacket(packet -> ignoredPackets.put(packet.getSyncId(), packet));
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withRequestTimeout(1000);
    CompletableFuture<IProtoResponse> future = client.call("slow_echo", args, opts);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable cause = ex.getCause();
    assertEquals(TimeoutException.class, cause.getClass());
    assertEquals(
        "Request timeout: "
            + "IProtoCall(syncId = 4, function = slow_echo, args = [\"one\",1]); "
            + "timeout = 1000ms",
        cause.getMessage());
    assertEquals(0, ignoredPackets.size());
    Thread.sleep(600);
    assertEquals(1, ignoredPackets.size());
    assertEquals(Collections.singleton(4L), ignoredPackets.keySet());
    IProtoResponse ignored = ignoredPackets.get(4L);
    assertEquals(args, ignored.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testRawCallTimeout() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    byte[] rawArgs = ArrayValueToBytes(args);
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withRequestTimeout(1000);
    CompletableFuture<IProtoResponse> future = client.call("slow_echo", rawArgs, null, opts);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable cause = ex.getCause();
    assertEquals(TimeoutException.class, cause.getClass());
  }

  @Test
  public void testCallNoTimeout() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withRequestTimeout(1000);
    IProtoMessage message = client.call("nonslow_echo", args, opts).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testRawCallNoTimeout() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args =
        ValueFactory.newArray(ValueFactory.newString("one"), ValueFactory.newInteger(1));
    byte[] rawArgs = ArrayValueToBytes(args);
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withRequestTimeout(1000);
    IProtoMessage message = client.call("nonslow_echo", rawArgs, null, opts).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
  }

  @ParameterizedTest
  @MethodSource("dataForTestCallError")
  public void testCallError(String function, Integer errorCode, List<List<Object>> stack)
      throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args = ValueFactory.emptyArray();
    CompletableFuture<IProtoResponse> future = client.call(function, args);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(errorCode, ((BoxError) rootCause).getErrorCode());
    List<BoxErrorStackItem> stacktrace = ((BoxError) rootCause).getTarantoolStack();
    assertEquals(stacktrace.size(), stack.size());
    for (int i = 0; i < stack.size(); i++) {
      List<?> stackline = stack.get(i);
      BoxErrorStackItem item = stacktrace.get(i);
      assertEquals(stackline.get(0), item.getType());
      assertEquals(stackline.get(1), item.getFile());
      assertTrue(item.getMessage().contains(stackline.get(2).toString()));
      assertEquals(stackline.get(3), item.getCode());
      assertEquals(stackline.get(4), item.getErrno());
    }
  }

  @ParameterizedTest
  @MethodSource("dataForTestCallError")
  public void testRawCallError(String function, Integer errorCode, List<List<Object>> stack)
      throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue args = ValueFactory.emptyArray();
    byte[] rawArgs = ArrayValueToBytes(args);
    CompletableFuture<IProtoResponse> future = client.call(function, rawArgs);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(errorCode, ((BoxError) rootCause).getErrorCode());
    List<BoxErrorStackItem> stacktrace = ((BoxError) rootCause).getTarantoolStack();
    assertEquals(stacktrace.size(), stack.size());
    for (int i = 0; i < stack.size(); i++) {
      List<?> stackline = stack.get(i);
      BoxErrorStackItem item = stacktrace.get(i);
      assertEquals(stackline.get(0), item.getType());
      assertEquals(stackline.get(1), item.getFile());
      assertTrue(item.getMessage().contains(stackline.get(2).toString()));
      assertEquals(stackline.get(3), item.getCode());
      assertEquals(stackline.get(4), item.getErrno());
    }
  }

  @ParameterizedTest
  @MethodSource("dataForTestCallWithPushHandler")
  public void testCallWithPushHadler(String function, ArrayValue args, List<ArrayValue> expected)
      throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    List<ArrayValue> messages = new ArrayList<>();
    IProtoMessage message =
        client
            .call(
                function,
                args,
                IProtoRequestOpts.empty()
                    .withPushHandler(m -> messages.add(m.getBodyArrayValue(IPROTO_DATA))))
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
    assertEquals(expected, messages);
  }

  @ParameterizedTest
  @MethodSource("dataForTestCallWithPushHandler")
  public void testRawCallWithPushHadler(String function, ArrayValue args, List<ArrayValue> expected)
      throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    List<ArrayValue> messages = new ArrayList<>();
    byte[] rawArgs = ArrayValueToBytes(args);
    IProtoMessage message =
        client
            .call(
                function,
                rawArgs,
                null,
                IProtoRequestOpts.empty()
                    .withPushHandler(m -> messages.add(m.getBodyArrayValue(IPROTO_DATA))))
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    assertEquals(args, message.getBodyArrayValue(IPROTO_DATA));
    assertEquals(expected, messages);
  }

  @Test
  public void testPrepareWithWrongSpaceName() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    CompletableFuture<IProtoResponse> future = client.prepare("select * from \"unknown_space\";");
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(IPROTO_ERR_SPACE_DOES_NOT_EXIST, ((BoxError) rootCause).getErrorCode());
    assertTrue(
        rootCause
            .getMessage()
            .matches(
                "BoxError\\{code=36, message='Space 'unknown_space' does not exist', "
                    + "stack=\\[BoxErrorStackItem\\{type='ClientError', line=[0-9]*,"
                    + " file='[./a-z]*', message='Space 'unknown_space' does not exist', errno=0,"
                    + " code=36, details=null}]}"));
  }

  @Test
  public void testExecuteWithWrongSpaceName() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue emptyArrayValue = ValueFactory.newArray();
    CompletableFuture<IProtoResponse> future =
        client.execute("select * from \"unknown_space\";", emptyArrayValue, emptyArrayValue);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(IPROTO_ERR_SPACE_DOES_NOT_EXIST, ((BoxError) rootCause).getErrorCode());
    assertEquals(
        "Space 'unknown_space' does not exist", ((BoxError) rootCause).getTarantoolMessage());
  }

  @Test
  public void testRawExecuteWithWrongSpaceName() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    ArrayValue emptyArrayValue = ValueFactory.newArray();
    byte[] emptyRawArray = ArrayValueToBytes(emptyArrayValue);
    CompletableFuture<IProtoResponse> future =
        client.execute("select * from \"unknown_space\";", emptyRawArray, emptyRawArray);
    Exception ex = assertThrows(CompletionException.class, future::join);
    Throwable rootCause = findRootCause(ex);
    assertEquals(BoxError.class, rootCause.getClass());
    assertEquals(IPROTO_ERR_SPACE_DOES_NOT_EXIST, ((BoxError) rootCause).getErrorCode());
    assertTrue(
        rootCause
            .getMessage()
            .matches(
                "BoxError\\{code=36, message='Space 'unknown_space' does not exist', "
                    + "stack=\\[BoxErrorStackItem\\{type='ClientError', line=[0-9]*,"
                    + " file='[./a-z]*', message='Space 'unknown_space' does not exist', errno=0,"
                    + " code=36, details=null}]}"));
  }

  @Test
  public void testExecuteWithPreparedStatementId() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    IProtoMessage message =
        client.prepare("select \"id\", \"value\" from seqscan \"space_a\";").get();
    checkMessageHeader(message, IPROTO_OK, 4);
    IntegerValue statementId = message.getBodyIntegerValue(IPROTO_STMT_ID);

    ArrayValue emptyArrayValue = ValueFactory.newArray();
    message = client.execute(statementId.asLong(), emptyArrayValue, emptyArrayValue).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    ArrayValue expectedValue =
        ValueFactory.newArray(
            ValueFactory.newArray(ValueFactory.newString("key"), ValueFactory.newString("value")));
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testExecuteWithSqlStatementId() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    ArrayValue emptyArrayValue = ValueFactory.newArray();
    IProtoMessage message =
        client
            .execute(
                "select \"id\", \"value\" from seqscan \"space_a\";",
                emptyArrayValue,
                emptyArrayValue)
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue expectedValue =
        ValueFactory.newArray(
            ValueFactory.newArray(ValueFactory.newString("key"), ValueFactory.newString("value")));
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testExecuteWithOptions() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    ArrayValue emptyArrayValue = ValueFactory.newArray();
    IProtoMessage message =
        client
            .execute(
                "select \"id\", \"value\" from seqscan \"space_a\";",
                emptyArrayValue,
                emptyArrayValue)
            .get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue expectedValue =
        ValueFactory.newArray(
            ValueFactory.newArray(ValueFactory.newString("key"), ValueFactory.newString("value")));
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testExecuteWithSqlStatementAndSqlBind() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    ArrayValue sqlBind =
        ValueFactory.newArray(ValueFactory.newInteger(1), ValueFactory.newString("a"));
    ArrayValue options = ValueFactory.newArray();
    IProtoMessage message = client.execute("VALUES (?, ?);", sqlBind, options).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue expectedValue = ValueFactory.newArray(sqlBind);
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testRawExecuteWithSqlStatementAndSqlBind() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    ArrayValue sqlBind =
        ValueFactory.newArray(ValueFactory.newInteger(1), ValueFactory.newString("a"));
    ArrayValue options = ValueFactory.newArray();
    byte[] rawSqlBind = ArrayValueToBytes(sqlBind);
    byte[] rawOptions = ArrayValueToBytes(options);
    IProtoMessage message = client.execute("VALUES (?, ?);", rawSqlBind, rawOptions).get();
    checkMessageHeader(message, IPROTO_OK, 4);
    ArrayValue expectedValue = ValueFactory.newArray(sqlBind);
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testExecuteWithPreparedStatementIdAndSqlBind() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    IProtoMessage message = client.prepare("VALUES (?, ?);").get();
    checkMessageHeader(message, IPROTO_OK, 4);
    IntegerValue statementId = message.getBodyIntegerValue(IPROTO_STMT_ID);

    ArrayValue sqlBind =
        ValueFactory.newArray(ValueFactory.newInteger(1), ValueFactory.newString("a"));
    ArrayValue options = ValueFactory.newArray();
    message = client.execute(statementId.asLong(), sqlBind, options).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    ArrayValue expectedValue = ValueFactory.newArray(sqlBind);
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  public void testRawExecuteWithPreparedStatementIdAndSqlBind() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    tt.executeCommand("return box.space.space_a:insert({'key', 'value'})");

    IProtoMessage message = client.prepare("VALUES (?, ?);").get();
    checkMessageHeader(message, IPROTO_OK, 4);
    IntegerValue statementId = message.getBodyIntegerValue(IPROTO_STMT_ID);

    ArrayValue sqlBind =
        ValueFactory.newArray(ValueFactory.newInteger(1), ValueFactory.newString("a"));
    ArrayValue options = ValueFactory.newArray();
    byte[] rawSqlBind = ArrayValueToBytes(sqlBind);
    byte[] rawOptions = ArrayValueToBytes(options);
    message = client.execute(statementId.asLong(), rawSqlBind, rawOptions).get();
    checkMessageHeader(message, IPROTO_OK, 5);
    ArrayValue expectedValue = ValueFactory.newArray(sqlBind);
    assertEquals(expectedValue, message.getBodyArrayValue(IPROTO_DATA));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testDMLTupleExtension() throws Exception {
    IProtoClient client = createClientAndConnect(address, true);
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    assertTrue(
        client.hasTupleExtension(),
        "Client and server should support the feature DML_TUPLE_EXTENSION");

    tt.executeCommand("return box.space.space_a:insert({'key1', 'value1'})");

    IProtoMessage message =
        client
            .select(
                spaceAId,
                0,
                ValueFactory.newArray(ValueFactory.newString("key1")),
                1,
                0,
                BoxIterator.EQ)
            .get();

    Map<Value, Value> format = message.getBodyMapValue(IPROTO_TUPLE_FORMATS).map();
    HashMap<Value, Value> keyMap =
        new HashMap<Value, Value>() {
          {
            put(ValueFactory.newString("name"), ValueFactory.newString("id"));
            put(ValueFactory.newString("type"), ValueFactory.newString("string"));
          }
        };
    Map<Value, Value> valueMap =
        new HashMap<Value, Value>() {
          {
            put(ValueFactory.newString("type"), ValueFactory.newString("string"));
            put(ValueFactory.newString("name"), ValueFactory.newString("value"));
            put(ValueFactory.newString("is_nullable"), ValueFactory.newBoolean(true));
          }
        };
    ArrayValue expectedFormat =
        ValueFactory.newArray(ValueFactory.newMap(keyMap), ValueFactory.newMap(valueMap));

    assertEquals(expectedFormat, format.get(format.keySet().iterator().next()).asArrayValue());
  }

  @Test
  void testConnectionAutoReadState() throws Exception {
    IProtoClient client = createClientAndConnect(address, false);
    assertTrue(client.isConnected());

    client.pause();

    CompletableFuture<IProtoResponse> pingFuture = client.ping();
    assertThrows(CompletionException.class, pingFuture::join);

    client.resume();

    IProtoResponse pingResponse = client.ping().get();
    assertTrue(pingResponse.isBodyEmpty());
  }
}
