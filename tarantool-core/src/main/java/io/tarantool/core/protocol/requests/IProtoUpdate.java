/*
 * Copyright (c) 2025 VK Company Limited.
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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_UPDATE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_UPDATE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_FOUR_ITEMS;

public class IProtoUpdate extends IProtoBaseRequest {

  private final Integer spaceId;
  private final String spaceName;
  private final Integer indexId;
  private final String indexName;
  private ArrayValue key;
  private ArrayValue operations;
  private byte[] rawKey;
  private byte[] rawOperations;


  public IProtoUpdate(final Integer spaceId,
      final String spaceName,
      final Integer indexId,
      final String indexName,
      ArrayValue key,
      ArrayValue operations,
      Long streamId) {
    this.spaceId = spaceId;
    this.indexId = indexId;
    this.spaceName = spaceName;
    this.indexName = indexName;
    this.key = key;
    this.operations = operations;
    this.setStreamId(streamId);
  }

  public IProtoUpdate(final Integer spaceId,
      final String spaceName,
      final Integer indexId,
      final String indexName,
      byte[] key,
      byte[] operations,
      Long streamId) {
    this.spaceId = spaceId;
    this.indexId = indexId;
    this.spaceName = spaceName;
    this.indexName = indexName;
    this.rawKey = key;
    this.rawOperations = operations;
    this.setStreamId(streamId);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_UPDATE;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_FOUR_ITEMS);

    if (spaceId != null) {
      packer.addPayload(RAW_IPROTO_SPACE_ID);  // key
      packer.packInt(spaceId);                 // value
    } else {
      packer.addPayload(RAW_IPROTO_SPACE_NAME);  // key
      packer.packString(spaceName);              // value
    }

    if (indexId != null) {
      packer.addPayload(RAW_IPROTO_INDEX_ID);  // key
      packer.packInt(indexId);                 // value
    } else {
      packer.addPayload(RAW_IPROTO_INDEX_NAME);  // key
      packer.packString(indexName);              // value
    }

    packer.addPayload(RAW_IPROTO_KEY); // key
    packValue(packer, rawKey, key);    // value

    packer.addPayload(RAW_IPROTO_TUPLE);           // key
    packValue(packer, rawOperations, operations);  // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() throws Exception {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_SPACE_ID, ValueFactory.newInteger(spaceId));
    map.put(MP_IPROTO_SPACE_NAME, ValueFactory.newString(spaceName));
    map.put(MP_IPROTO_INDEX_ID, ValueFactory.newInteger(indexId));
    map.put(MP_IPROTO_INDEX_NAME, ValueFactory.newString(indexName));
    map.put(MP_IPROTO_KEY, key);
    map.put(MP_IPROTO_TUPLE, operations);
    return ValueFactory.newMap(map);
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_UPDATE;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoUpdate(syncId = ")
          .append(getSyncId())
          .append(", spaceId = ")
          .append(spaceId)
          .append(", indexId = ")
          .append(indexId)
          .append(", spaceName = ")
          .append(spaceName)
          .append(", indexName = ")
          .append(indexName)
          .append(", key = ")
          .append(key != null ? key : Arrays.toString(rawKey))
          .append(", operations = ")
          .append(operations != null ? operations : Arrays.toString(rawOperations))
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
