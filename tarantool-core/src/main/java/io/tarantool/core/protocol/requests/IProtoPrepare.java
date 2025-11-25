/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_PREPARE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SQL_TEXT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SQL_TEXT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_PREPARE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_ONE_ITEM;

public class IProtoPrepare extends IProtoBaseRequest {

  private final String statementText;

  public IProtoPrepare(String statementText, Long streamId) {
    super();
    this.setStreamId(streamId);
    this.statementText = statementText;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_ONE_ITEM);

    packer.addPayload(RAW_IPROTO_SQL_TEXT); // key
    packer.packString(statementText); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_SQL_TEXT, ValueFactory.newString(statementText));
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_PREPARE;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_PREPARE;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoPrepare(syncId = ")
          .append(getSyncId())
          .append(", statementText = ")
          .append(statementText)
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
