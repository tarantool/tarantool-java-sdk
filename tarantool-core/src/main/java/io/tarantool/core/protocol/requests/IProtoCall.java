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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TUPLE_FORMATS;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_CALL;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_FUNCTION_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_FUNCTION_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_CALL;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_THREE_ITEMS;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_TWO_ITEMS;

public class IProtoCall extends IProtoBaseRequest {

  private final String functionName;
  private ArrayValue args;
  private byte[] rawArgs;
  private byte[] rawFormats;

  public IProtoCall(String functionName, ArrayValue args, Long streamId) {
    this.functionName = functionName;
    this.args = args;
    this.setStreamId(streamId);
  }

  public IProtoCall(String functionName, byte[] args, byte[] formats, Long streamId) {
    this.functionName = functionName;
    this.rawArgs = args;
    this.rawFormats = formats;
    this.setStreamId(streamId);
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    if (rawFormats == null) {
      packer.addPayload(RAW_MAP_HEADER_WITH_TWO_ITEMS);
    } else {
      packer.addPayload(RAW_MAP_HEADER_WITH_THREE_ITEMS);
      packer.packInt(IPROTO_TUPLE_FORMATS); // key
      packer.addPayload(rawFormats); // value
    }

    packer.addPayload(RAW_IPROTO_FUNCTION_NAME); // key
    packer.packString(functionName); // value

    packer.addPayload(RAW_IPROTO_TUPLE); // key
    packValue(packer, rawArgs, args); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    if (args == null) {
      return null;
    }

    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_FUNCTION_NAME, ValueFactory.newString(functionName));
    map.put(MP_IPROTO_TUPLE, args);
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_CALL;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_CALL;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoCall(syncId = ")
          .append(getSyncId())
          .append(", function = ")
          .append(functionName)
          .append(", args = ")
          .append(args != null ? args : Arrays.toString(rawArgs))
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
