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
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_DELETE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_DELETE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_THREE_ITEMS;

public class IProtoDelete extends IProtoBaseRequest {

  private final Integer spaceId;
  private final String spaceName;
  private final Integer indexId;
  private final String indexName;
  private Value key;
  private byte[] rawKey;

  public IProtoDelete(
      final Integer spaceId,
      final String spaceName,
      final Integer indexId,
      final String indexName,
      Value key,
      Long streamId) {
    this.spaceId = spaceId;
    this.key = key;
    this.indexId = indexId;
    this.spaceName = spaceName;
    this.indexName = indexName;
    this.setStreamId(streamId);
  }

  public IProtoDelete(
      final Integer spaceId,
      final String spaceName,
      final Integer indexId,
      final String indexName,
      byte[] key,
      Long streamId) {
    this.spaceId = spaceId;
    this.indexId = indexId;
    this.spaceName = spaceName;
    this.indexName = indexName;
    this.rawKey = key;
    this.setStreamId(streamId);
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_THREE_ITEMS);

    if (spaceId != null) {
      packer.addPayload(RAW_IPROTO_SPACE_ID); // key
      packer.packInt(spaceId); // value
    } else {
      packer.addPayload(RAW_IPROTO_SPACE_NAME); // key
      packer.packString(spaceName); // value
    }

    if (indexId != null) {
      packer.addPayload(RAW_IPROTO_INDEX_ID); // key
      packer.packInt(indexId); // value
    } else {
      packer.addPayload(RAW_IPROTO_INDEX_NAME); // key
      packer.packString(indexName); // value
    }

    packer.addPayload(RAW_IPROTO_KEY); // key
    packValue(packer, rawKey, key); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    if (key == null) {
      return null;
    }

    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_SPACE_ID, ValueFactory.newInteger(spaceId));
    map.put(MP_IPROTO_SPACE_NAME, ValueFactory.newString(spaceName));
    map.put(MP_IPROTO_INDEX_ID, ValueFactory.newInteger(indexId));
    map.put(MP_IPROTO_INDEX_NAME, ValueFactory.newString(indexName));
    map.put(MP_IPROTO_KEY, key);
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_DELETE;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_DELETE;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoDelete(syncId = ")
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
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
