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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_UPSERT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_BASE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_OPS;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_BASE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_OPS;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_UPSERT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_FOUR_ITEMS;

public class IProtoUpsert extends IProtoBaseRequest {

  private final Integer spaceId;
  private final int indexBase;
  private final String spaceName;

  private ArrayValue tuple;
  private ArrayValue operations;
  private byte[] rawTuple;
  private byte[] rawOperations;

  public IProtoUpsert(
      final Integer spaceId,
      final String spaceName,
      int indexBase,
      ArrayValue tuple,
      ArrayValue operations,
      Long streamId) {
    this.spaceId = spaceId;
    this.indexBase = indexBase;
    this.spaceName = spaceName;
    this.tuple = tuple;
    this.operations = operations;
    this.setStreamId(streamId);
  }

  public IProtoUpsert(
      final Integer spaceId,
      final String spaceName,
      int indexBase,
      byte[] tuple,
      byte[] operations,
      Long streamId) {
    this.spaceId = spaceId;
    this.indexBase = indexBase;
    this.spaceName = spaceName;
    this.rawTuple = tuple;
    this.rawOperations = operations;
    this.setStreamId(streamId);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_UPSERT;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_FOUR_ITEMS);

    if (spaceId != null) {
      packer.addPayload(RAW_IPROTO_SPACE_ID); // key
      packer.packInt(spaceId); // value
    } else {
      packer.addPayload(RAW_IPROTO_SPACE_NAME); // key
      packer.packString(spaceName); // value
    }

    packer.addPayload(RAW_IPROTO_INDEX_BASE); // key
    packer.packInt(indexBase); // value

    packer.addPayload(RAW_IPROTO_TUPLE); // key
    packValue(packer, rawTuple, tuple); // value

    packer.addPayload(RAW_IPROTO_OPS); // key
    packValue(packer, rawOperations, operations); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() throws Exception {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_SPACE_ID, ValueFactory.newInteger(spaceId));
    map.put(MP_IPROTO_SPACE_NAME, ValueFactory.newString(spaceName));
    map.put(MP_IPROTO_INDEX_BASE, ValueFactory.newInteger(indexBase));
    map.put(MP_IPROTO_TUPLE, tuple);
    map.put(MP_IPROTO_OPS, operations);
    return ValueFactory.newMap(map);
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_UPSERT;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoUpsert(syncId = ")
          .append(getSyncId())
          .append(", spaceId = ")
          .append(spaceId)
          .append(", indexId = ")
          .append(indexBase)
          .append(", spaceName = ")
          .append(spaceName)
          .append(", tuple = ")
          .append(tuple != null ? tuple : Arrays.toString(rawTuple))
          .append(", operations = ")
          .append(operations != null ? operations : Arrays.toString(rawOperations))
          .append(")");
    }
    return stringBuilder.toString();
  }
}
