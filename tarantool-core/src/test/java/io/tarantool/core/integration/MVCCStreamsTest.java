/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.Tarantool2Container;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;
import org.testcontainers.utility.DockerImageName;

import static io.tarantool.core.HelpersUtils.findRootCause;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_PROC_LUA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERR_TUPLE_FOUND;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_OK;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.TransactionIsolationLevel;

@DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
@Timeout(value = 5)
public class MVCCStreamsTest extends BaseTest {

  private static InetSocketAddress address;
  private static int spaceAId;
  private static int spaceBId;

  private static final ArrayValue tupleA =
      ValueFactory.newArray(ValueFactory.newString("key_c"), ValueFactory.newString("value_c"));
  private static final ArrayValue tupleB =
      ValueFactory.newArray(ValueFactory.newString("key_d"), ValueFactory.newString("value_d"));
  private static final ArrayValue tupleTest =
      ValueFactory.newArray(ValueFactory.newString("test"), ValueFactory.newString("test"));
  private static final ArrayValue keyA = ValueFactory.newArray(ValueFactory.newString("key_c"));
  private static final ArrayValue keyB = ValueFactory.newArray(ValueFactory.newString("key_d"));

  private static TarantoolContainer<?> tt;

  @BeforeAll
  public static void setUp() throws Exception {
    DockerImageName dockerImage =
        DockerImageName.parse(
            String.format(
                "%s:%s",
                TarantoolContainerClientHelper.IMAGE_PREFIX,
                TarantoolContainerClientHelper.TARANTOOL_VERSION));
    Path initScriptPath = null;
    try {
      initScriptPath =
          Paths.get(
              Objects.requireNonNull(
                      MVCCStreamsTest.class.getClassLoader().getResource("server-mvcc.lua"))
                  .toURI());
    } catch (Exception e) {
      // ignore
    }
    tt =
        Tarantool2Container.builder(dockerImage, initScriptPath)
            .build()
            .withEnv(ENV_MAP)
            .withExposedPorts(3301);

    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);

    address = tt.mappedAddress();

    List<?> result =
        TarantoolContainerClientHelper.executeCommandDecoded(tt, "return box.space.space_a.id");
    spaceAId = (Integer) result.get(0);

    result =
        TarantoolContainerClientHelper.executeCommandDecoded(tt, "return box.space.space_b.id");
    spaceBId = (Integer) result.get(0);
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    TarantoolContainerClientHelper.executeCommand(tt, "return box.space.test:truncate()");
    TarantoolContainerClientHelper.executeCommand(tt, "return box.space.space_a:truncate()");
    TarantoolContainerClientHelper.executeCommand(tt, "return box.space.space_b:truncate()");
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  @SuppressWarnings("unchecked")
  private void checkTuple(String ttCheck, ArrayValue tuple) throws Exception {
    List<? extends Object> result =
        TarantoolContainerClientHelper.executeCommandDecoded(tt, ttCheck);
    List<Object> stored = (List<Object>) result.get(0);
    assertEquals(
        tuple,
        ValueFactory.newArray(
            ValueFactory.newString((String) stored.get(0)),
            ValueFactory.newString((String) stored.get(1))));
  }

  private void checkNoTuple(String ttCheck) throws Exception {
    assertNull(TarantoolContainerClientHelper.executeCommandDecoded(tt, ttCheck));
  }

  private IProtoClient getClientAndConnect() throws Exception {
    IProtoClient client = new IProtoClientImpl(factory, factory.getTimerService());
    client.connect(address, 3_000).get();
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    return client;
  }

  @Test
  public void testStreamBeginAndCommit() throws Exception {
    IProtoClient client = getClientAndConnect();
    Long streamId = client.allocateStreamId();
    IProtoMessage messageBegin =
        client.begin(streamId, 3_000L, TransactionIsolationLevel.DEFAULT).get();
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withStreamId(streamId);
    IProtoMessage messageInsertA = client.insert(spaceAId, null, tupleA, opts).get();
    IProtoMessage messageInsertB = client.insert(spaceBId, null, tupleB, opts).get();
    IProtoMessage messageSelectAInStream =
        client
            .select(spaceAId, null, 0, null, keyA, 1, 0, BoxIterator.EQ, false, null, null, opts)
            .get();
    IProtoMessage messageSelectBInStream =
        client
            .select(spaceBId, null, 0, null, keyB, 1, 0, BoxIterator.EQ, false, null, null, opts)
            .get();
    IProtoMessage messageSelectOutOfStream =
        client.select(spaceAId, 0, keyB, 1, 0, BoxIterator.EQ).get();
    IProtoMessage messageCommit = client.commit(streamId).get();
    assertEquals(IPROTO_OK, messageBegin.getRequestType());
    assertEquals(IPROTO_OK, messageInsertA.getRequestType());
    assertEquals(IPROTO_OK, messageInsertB.getRequestType());
    assertEquals(IPROTO_OK, messageCommit.getRequestType());
    assertEquals(IPROTO_OK, messageSelectAInStream.getRequestType());
    ArrayValue selectAInStreamData = messageSelectAInStream.getBodyArrayValue(IPROTO_DATA);
    ArrayValue selectBInStreamData = messageSelectBInStream.getBodyArrayValue(IPROTO_DATA);
    ArrayValue selectOutOfStreamData = messageSelectOutOfStream.getBodyArrayValue(IPROTO_DATA);
    assertEquals(ValueFactory.newArray(tupleA), decodeTuple(client, selectAInStreamData));
    assertEquals(ValueFactory.newArray(tupleB), decodeTuple(client, selectBInStreamData));
    assertEquals(ValueFactory.emptyArray(), decodeTuple(client, selectOutOfStreamData));
    checkTuple("return box.space.space_a:get('key_c')", tupleA);
    checkTuple("return box.space.space_b:get('key_d')", tupleB);
  }

  @Test
  public void testStreamBeginAndRollback() throws Exception {
    IProtoClient client = getClientAndConnect();
    Long streamId = client.allocateStreamId();
    IProtoMessage messageBegin =
        client.begin(streamId, 3_000L, TransactionIsolationLevel.DEFAULT).get();
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withStreamId(streamId);
    IProtoMessage messageInsertA = client.insert(spaceAId, null, tupleA, opts).get();
    IProtoMessage messageInsertB = client.insert(spaceBId, null, tupleB, opts).get();
    IProtoMessage messageRollback = client.rollback(streamId).get();
    assertEquals(IPROTO_OK, messageBegin.getRequestType());
    assertEquals(IPROTO_OK, messageInsertA.getRequestType());
    assertEquals(IPROTO_OK, messageInsertB.getRequestType());
    assertEquals(IPROTO_OK, messageRollback.getRequestType());
    checkNoTuple("return box.space.space_a:get('key_c')");
    checkNoTuple("return box.space.space_b:get('key_d')");
  }

  @Test
  public void testStreamBeginAndCommitWithYieldingCall() throws Exception {
    IProtoClient client = getClientAndConnect();
    Long streamId = client.allocateStreamId();
    IProtoMessage messageBegin =
        client.begin(streamId, 3_000L, TransactionIsolationLevel.DEFAULT).get();
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withStreamId(streamId);
    IProtoMessage messageInsertA = client.insert(spaceAId, null, tupleA, opts).get();
    IProtoMessage messageCall = client.call("insert_c", ValueFactory.emptyArray(), opts).get();
    IProtoMessage messageInsertB = client.insert(spaceBId, null, tupleB, opts).get();
    IProtoMessage messageCommit = client.commit(streamId).get();
    assertEquals(IPROTO_OK, messageBegin.getRequestType());
    assertEquals(IPROTO_OK, messageInsertA.getRequestType());
    assertEquals(IPROTO_OK, messageInsertB.getRequestType());
    assertEquals(IPROTO_OK, messageCall.getRequestType());
    assertEquals(IPROTO_OK, messageCommit.getRequestType());
    checkTuple("return box.space.space_a:get('key_c')", tupleA);
    checkTuple("return box.space.space_b:get('key_d')", tupleB);
    checkTuple("return box.space.test:get('test')", tupleTest);
  }

  @Test
  public void testStreamBeginAndCommitWithThrowingCall() throws Exception {
    IProtoClient client = getClientAndConnect();
    Long streamId = client.allocateStreamId();
    IProtoMessage messageBegin =
        client.begin(streamId, 3_000L, TransactionIsolationLevel.DEFAULT).get();
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withStreamId(streamId);
    IProtoMessage messageInsertA = client.insert(spaceAId, null, tupleA, opts).get();

    CompletableFuture<IProtoResponse> callFuture =
        client.call("fail", ValueFactory.emptyArray(), opts);
    Exception ex1 = assertThrows(CompletionException.class, callFuture::join);
    Throwable rootCause1 = findRootCause(ex1);
    assertEquals(BoxError.class, rootCause1.getClass());
    assertEquals(IPROTO_ERR_PROC_LUA, ((BoxError) rootCause1).getErrorCode());

    CompletableFuture<IProtoResponse> insertA2Future = client.insert(spaceAId, null, tupleA, opts);
    Exception ex2 = assertThrows(CompletionException.class, insertA2Future::join);
    Throwable rootCause2 = findRootCause(ex2);
    assertEquals(BoxError.class, rootCause2.getClass());
    assertEquals(IPROTO_ERR_TUPLE_FOUND, ((BoxError) rootCause2).getErrorCode());

    IProtoMessage messageInsertB = client.insert(spaceBId, null, tupleB, opts).get();
    IProtoMessage messageCommit = client.commit(streamId).get();
    assertEquals(IPROTO_OK, messageBegin.getRequestType());
    assertEquals(IPROTO_OK, messageInsertA.getRequestType());
    assertEquals(IPROTO_OK, messageInsertB.getRequestType());
    assertEquals(IPROTO_OK, messageCommit.getRequestType());
    checkTuple("return box.space.space_a:get('key_c')", tupleA);
    checkTuple("return box.space.space_b:get('key_d')", tupleB);
  }

  @Test
  public void testStreamAutoRollback() throws Exception {
    IProtoClient client = getClientAndConnect();
    Long streamId = client.allocateStreamId();
    IProtoMessage messageBegin = client.begin(streamId, 1, TransactionIsolationLevel.DEFAULT).get();
    Thread.sleep(3_000L);
    IProtoRequestOpts opts = IProtoRequestOpts.empty().withStreamId(streamId);

    ExecutionException ex =
        assertThrows(
            ExecutionException.class, () -> client.insert(spaceAId, null, tupleA, opts).get());
    assertInstanceOf(BoxError.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("Transaction has been aborted by timeout"));
  }
}
