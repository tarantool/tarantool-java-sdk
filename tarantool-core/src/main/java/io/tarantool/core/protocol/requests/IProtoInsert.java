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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_INSERT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_INSERT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_TWO_ITEMS;

public class IProtoInsert extends IProtoBaseRequest {

  protected final Integer spaceId;
  protected final String spaceName;
  protected ArrayValue tuple;
  protected byte[] rawTuple;

  public IProtoInsert(
      final Integer spaceId, final String spaceName, ArrayValue tuple, Long streamId) {
    this.spaceId = spaceId;
    this.spaceName = spaceName;
    this.tuple = tuple;
    this.setStreamId(streamId);
  }

  public IProtoInsert(final Integer spaceId, final String spaceName, byte[] tuple, Long streamId) {
    this.spaceId = spaceId;
    this.spaceName = spaceName;
    this.rawTuple = tuple;
    this.setStreamId(streamId);
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_TWO_ITEMS);

    if (spaceId != null) {
      packer.addPayload(RAW_IPROTO_SPACE_ID); // key
      packer.packInt(spaceId); // value
    } else {
      packer.addPayload(RAW_IPROTO_SPACE_NAME); // key
      packer.packString(spaceName); // value
    }

    packer.addPayload(RAW_IPROTO_TUPLE); // key
    packValue(packer, rawTuple, tuple); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() throws Exception {
    if (tuple == null) {
      return null;
    }

    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_SPACE_ID, ValueFactory.newInteger(spaceId));
    map.put(MP_IPROTO_SPACE_NAME, ValueFactory.newString(spaceName));
    map.put(MP_IPROTO_TUPLE, tuple);
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_INSERT;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_INSERT;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoInsert(syncId = ")
          .append(getSyncId())
          .append(", tuple = ")
          .append(tuple != null ? tuple : Arrays.toString(rawTuple))
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
