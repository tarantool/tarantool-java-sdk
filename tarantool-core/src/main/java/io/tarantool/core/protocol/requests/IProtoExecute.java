/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_EXECUTE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_OPTIONS;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SQL_BIND;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SQL_TEXT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_STMT_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_OPTIONS;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SQL_BIND;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SQL_TEXT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_STMT_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_EXECUTE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_THREE_ITEMS;

public class IProtoExecute extends IProtoBaseRequest {

  private final Boolean withStatementId;
  private final long statementId;
  private final String statementText;
  private ArrayValue sqlBind;
  private ArrayValue options;
  private byte[] rawSqlBind;
  private byte[] rawOptions;

  public IProtoExecute(long statementId, ArrayValue sqlBind, ArrayValue options, Long streamId) {
    super();
    this.setStreamId(streamId);
    this.withStatementId = true;
    this.statementId = statementId;
    this.statementText = "";
    this.sqlBind = sqlBind;
    this.options = options;
  }

  public IProtoExecute(long statementId, byte[] sqlBind, byte[] options, Long streamId) {
    super();
    this.setStreamId(streamId);
    this.withStatementId = true;
    this.statementId = statementId;
    this.statementText = "";
    this.rawSqlBind = sqlBind;
    this.rawOptions = options;
  }

  public IProtoExecute(
      String statementText, ArrayValue sqlBind, ArrayValue options, Long streamId) {
    super();
    this.setStreamId(streamId);
    this.withStatementId = false;
    this.statementId = 0L;
    this.statementText = statementText;
    this.sqlBind = sqlBind;
    this.options = options;
  }

  public IProtoExecute(String statementText, byte[] sqlBind, byte[] options, Long streamId) {
    super();
    this.setStreamId(streamId);
    this.withStatementId = false;
    this.statementId = 0L;
    this.statementText = statementText;
    this.rawSqlBind = sqlBind;
    this.rawOptions = options;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_THREE_ITEMS);

    if (withStatementId) {
      packer.addPayload(RAW_IPROTO_STMT_ID); // key
      packer.packLong(statementId); // value
    } else {
      packer.addPayload(RAW_IPROTO_SQL_TEXT);
      packer.packString(statementText);
    }

    packer.addPayload(RAW_IPROTO_SQL_BIND); // key
    packValue(packer, rawSqlBind, sqlBind); // value

    packer.addPayload(RAW_IPROTO_OPTIONS); // key
    packValue(packer, rawOptions, options); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    if (withStatementId) {
      map.put(MP_IPROTO_STMT_ID, ValueFactory.newInteger(statementId));
    } else {
      map.put(MP_IPROTO_SQL_TEXT, ValueFactory.newString(statementText));
    }
    map.put(MP_IPROTO_SQL_BIND, sqlBind);
    map.put(MP_IPROTO_OPTIONS, options);
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_EXECUTE;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_EXECUTE;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      if (withStatementId) {
        this.stringBuilder
            .append("IProtoExecute(syncId = ")
            .append(getSyncId())
            .append(", statementId = ")
            .append(statementId)
            .append(", sqlBind = ")
            .append(sqlBind != null ? sqlBind : Arrays.toString(rawSqlBind))
            .append(", options = ")
            .append(options != null ? options : Arrays.toString(rawOptions))
            .append(", streamId = ")
            .append(getStreamId())
            .append(")");
      } else {
        this.stringBuilder
            .append("IProtoExecute(syncId = ")
            .append(getSyncId())
            .append(", statementText = ")
            .append(statementText)
            .append(", sqlBind = ")
            .append(sqlBind != null ? sqlBind : Arrays.toString(rawSqlBind))
            .append(", options = ")
            .append(options != null ? options : Arrays.toString(rawOptions))
            .append(", streamId = ")
            .append(getStreamId())
            .append(")");
      }
    }
    return this.stringBuilder.toString();
  }
}
