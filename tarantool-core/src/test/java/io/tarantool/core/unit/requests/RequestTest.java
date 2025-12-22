/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.unit.requests;


import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.protocol.IProtoRequest;
import io.tarantool.core.protocol.TransactionIsolationLevel;
import io.tarantool.core.protocol.requests.IProtoAuth;
import io.tarantool.core.protocol.requests.IProtoAuth.AuthType;
import io.tarantool.core.protocol.requests.IProtoBegin;
import io.tarantool.core.protocol.requests.IProtoCall;
import io.tarantool.core.protocol.requests.IProtoCommit;
import io.tarantool.core.protocol.requests.IProtoDelete;
import io.tarantool.core.protocol.requests.IProtoEval;
import io.tarantool.core.protocol.requests.IProtoExecute;
import io.tarantool.core.protocol.requests.IProtoId;
import io.tarantool.core.protocol.requests.IProtoInsert;
import io.tarantool.core.protocol.requests.IProtoPing;
import io.tarantool.core.protocol.requests.IProtoPrepare;
import io.tarantool.core.protocol.requests.IProtoReplace;
import io.tarantool.core.protocol.requests.IProtoRollback;
import io.tarantool.core.protocol.requests.IProtoSelect;
import io.tarantool.core.protocol.requests.IProtoUnwatch;
import io.tarantool.core.protocol.requests.IProtoUpdate;
import io.tarantool.core.protocol.requests.IProtoUpsert;
import io.tarantool.core.protocol.requests.IProtoWatch;

public class RequestTest {

  @Test
  public void testRequestsToString() {

    // auth
    String user = "user";
    String password = "password";
    byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);
    AuthType authType = AuthType.CHAP_SHA1;
    IProtoRequest auth = new IProtoAuth(user, password, salt, authType);
    assertEquals(
        "IProtoAuth(syncId = " + auth.getSyncId() +
            ", user = " + user +
            ", authType = " + authType + ")",
        auth.toString());

    // begin
    final Long streamId = 1L;
    long timeout = 1L;
    IProtoRequest begin = new IProtoBegin(streamId, timeout, TransactionIsolationLevel.DEFAULT);
    assertEquals("IProtoBegin(syncId = " + begin.getSyncId() + ", timeout = " + timeout + ", level = " +
        TransactionIsolationLevel.DEFAULT + ")", begin.toString());

    // call
    String functionName = "function";
    ArrayValue args = ValueFactory.newArray(ValueFactory.newInteger(5));
    IProtoRequest call = new IProtoCall(functionName, args, streamId);
    assertEquals("IProtoCall(syncId = " + call.getSyncId() +
            ", function = " + functionName +
            ", args = " + args + ")",
        call.toString());

    // commit
    IProtoRequest commit = new IProtoCommit(streamId);
    assertEquals("IProtoCommit(syncId = " + commit.getSyncId() +
            ", streamId = " + streamId + ")",
        commit.toString());

    // delete
    int spaceId = 24;
    int indexId = 45;
    Value key = ValueFactory.newInteger(10);
    String spaceName = "null";
    String indexName = "null";

    IProtoRequest delete = new IProtoDelete(spaceId, spaceName, indexId, indexName, key, streamId);
    assertEquals("IProtoDelete(syncId = " + delete.getSyncId() +
            ", spaceId = " + spaceId +
            ", indexId = " + indexId +
            ", spaceName = " + spaceName +
            ", indexName = " + indexName +
            ", key = " + key + ")",
        delete.toString());

    // eval
    String expression = "exp";
    IProtoRequest eval = new IProtoEval(expression, args, streamId);
    assertEquals("IProtoEval(syncId = " + eval.getSyncId() +
            ", expr = " + expression +
            ", args = " + args + ")",
        eval.toString());

    // execute
    long statementId = 1L;
    ArrayValue sqlBind = ValueFactory.newArray(ValueFactory.newInteger(3));
    ArrayValue options = ValueFactory.newArray(ValueFactory.newInteger(23));

    IProtoRequest execute = new IProtoExecute(statementId, sqlBind, options, streamId);
    assertEquals("IProtoExecute(syncId = " + execute.getSyncId() +
            ", statementId = " + statementId +
            ", sqlBind = " + sqlBind +
            ", options = " + options +
            ", streamId = " + streamId + ")",
        execute.toString());

    // id
    int protocolVersion = 1;
    List<Integer> features = Arrays.asList(3, 4, 5);
    IProtoRequest id = new IProtoId(protocolVersion, features);
    assertEquals("IProtoId(syncId = " + id.getSyncId() +
            ", version = " + protocolVersion +
            ", features = " + features + ")",
        id.toString());

    // insert
    ArrayValue tuple = ValueFactory.newArray(ValueFactory.newString("tuple"));
    IProtoRequest insert = new IProtoInsert(spaceId, spaceName, tuple, streamId);
    assertEquals("IProtoInsert(syncId = " + insert.getSyncId() +
            ", tuple = " + tuple + ")",
        insert.toString());

    // ping
    IProtoRequest ping = new IProtoPing();
    assertEquals("IProtoPing(syncId = " + ping.getSyncId() + ")",
        ping.toString());

    // prepare
    String statementText = "test";
    IProtoRequest prepare = new IProtoPrepare(statementText, streamId);
    assertEquals("IProtoPrepare(syncId = " + prepare.getSyncId() +
            ", statementText = " + statementText + ")",
        prepare.toString());

    // replace
    IProtoRequest replace = new IProtoReplace(spaceId, null, tuple, streamId);
    assertEquals("IProtoReplace(syncId = " + replace.getSyncId() +
            ", tuple = " + tuple + ")",
        replace.toString());

    // rollback
    IProtoRequest rollback = new IProtoRollback(streamId);
    assertEquals("IProtoRollback(syncId = " + rollback.getSyncId() +
            ", streamId = " + streamId + ")",
        rollback.toString());

    // select
    int limit = 5;
    int offset = 5;
    int iterator = 5;
    ArrayValue keyS = ValueFactory.newArray(ValueFactory.newString("hello"));
    IProtoRequest select = new IProtoSelect(spaceId,
        spaceName,
        indexId,
        indexName,
        limit,
        offset,
        iterator,
        keyS,
        streamId,
        false,
        null,
        null);
    assertEquals("IProtoSelect(syncId = " + select.getSyncId() +
            ", spaceId = " + spaceId +
            ", indexId = " + indexId +
            ", spaceName = " + spaceName +
            ", indexName = " + indexName +
            ", limit = " + limit +
            ", offset = " + offset +
            ", iterator = " + iterator +
            ", key = " + keyS +
            ", fetchPosition = false" +
            ", after = null" +
            ", afterMode = null)",
        select.toString());

    // unwatch
    String unWatchKey = "unwatch";
    IProtoRequest unwatch = new IProtoUnwatch(unWatchKey);
    assertEquals("IProtoUnwatch(key = " + unWatchKey + ")",
        unwatch.toString());

    // update
    ArrayValue operations = ValueFactory.newArray(ValueFactory.newString("+"));
    IProtoRequest update = new IProtoUpdate(spaceId, spaceName, indexId, indexName, keyS, operations, streamId);
    assertEquals("IProtoUpdate(syncId = " + update.getSyncId() +
            ", spaceId = " + spaceId +
            ", indexId = " + indexId +
            ", spaceName = " + spaceName +
            ", indexName = " + indexName +
            ", key = " + keyS +
            ", operations = " + operations + ")",
        update.toString());

    // upsert
    int indexBase = 5;
    IProtoRequest upsert = new IProtoUpsert(spaceId, spaceName, indexBase, tuple, operations, streamId);
    assertEquals("IProtoUpsert(syncId = " + upsert.getSyncId() +
            ", spaceId = " + spaceId +
            ", indexId = " + indexBase +
            ", spaceName = " + spaceName +
            ", tuple = " + tuple +
            ", operations = " + operations + ")",
        upsert.toString());

    // watch
    String eventKey = "eventKey";
    IProtoRequest watch = new IProtoWatch(eventKey);
    assertEquals("IProtoWatch(key = " + eventKey + ")",
        watch.toString());
  }
}
