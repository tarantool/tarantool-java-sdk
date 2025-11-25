/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

@Timeout(value = 5)
@Testcontainers
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

  @Container
  private static final TarantoolContainer tt =
      new TarantoolContainer().withEnv(ENV_MAP).withScriptFileName("server-mvcc.lua");

  @BeforeAll
  public static void setUp() throws Exception {
    address = new InetSocketAddress(tt.getHost(), tt.getPort());

    List<?> result = tt.executeCommandDecoded("return box.space.space_a.id");
    spaceAId = (Integer) result.get(0);

    result = tt.executeCommandDecoded("return box.space.space_b.id");
    spaceBId = (Integer) result.get(0);
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    tt.executeCommand("return box.space.test:truncate()");
    tt.executeCommand("return box.space.space_a:truncate()");
    tt.executeCommand("return box.space.space_b:truncate()");
  }

  @SuppressWarnings("unchecked")
  private void checkTuple(String ttCheck, ArrayValue tuple) throws Exception {
    List<? extends Object> result = tt.executeCommandDecoded(ttCheck);
    List<Object> stored = (List<Object>) result.get(0);
    assertEquals(
        tuple,
        ValueFactory.newArray(
            ValueFactory.newString((String) stored.get(0)),
            ValueFactory.newString((String) stored.get(1))));
  }

  @SuppressWarnings("unchecked")
  private void checkNoTuple(String ttCheck) throws Exception {
    List<?> result = tt.executeCommandDecoded(ttCheck);
    assertEquals(0, result.size());
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
    Exception ex1 = assertThrows(CompletionException.class, () -> callFuture.join());
    Throwable rootCause1 = findRootCause(ex1);
    assertEquals(BoxError.class, rootCause1.getClass());
    assertEquals(IPROTO_ERR_PROC_LUA, ((BoxError) rootCause1).getErrorCode());

    CompletableFuture<IProtoResponse> insertA2Future = client.insert(spaceAId, null, tupleA, opts);
    Exception ex2 = assertThrows(CompletionException.class, () -> insertA2Future.join());
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
    assertTrue(ex.getCause() instanceof BoxError);
    assertTrue(ex.getCause().getMessage().contains("Transaction has been aborted by timeout"));
  }
}
