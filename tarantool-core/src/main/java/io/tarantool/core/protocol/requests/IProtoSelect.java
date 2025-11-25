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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_SELECT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_INDEX_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_ITERATOR;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_LIMIT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_OFFSET;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_AFTER_POSITION;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_AFTER_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_FETCH_POSITION;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_INDEX_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_ITERATOR;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_LIMIT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_OFFSET;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SPACE_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_SELECT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_EIGHT_ITEMS;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_SEVEN_ITEMS;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_SIX_ITEMS;

public class IProtoSelect extends IProtoBaseRequest {

  private final Integer spaceId;
  private final String spaceName;
  private final Integer indexId;
  private final String indexName;
  private final int limit;
  private final int offset;
  private final int iterator;
  private final boolean fetchPosition;
  private final byte[] after;
  private final SelectAfterMode afterMode;
  private ArrayValue key;
  private byte[] rawKey;

  public IProtoSelect(
      final Integer spaceId,
      final String spaceName,
      final Integer indexId,
      final String indexName,
      int limit,
      int offset,
      int iterator,
      ArrayValue key,
      Long streamId,
      boolean fetchPosition,
      byte[] after,
      SelectAfterMode afterMode) {
    super();
    this.setStreamId(streamId);
    this.spaceId = spaceId;
    this.indexId = indexId;
    this.spaceName = spaceName;
    this.indexName = indexName;
    this.limit = limit;
    this.offset = offset;
    this.iterator = iterator;
    this.key = key;
    this.fetchPosition = fetchPosition;
    this.after = after;
    this.afterMode = afterMode;
  }

  public IProtoSelect(
      final Integer spaceId,
      final String spaceName,
      final Integer indexId,
      final String indexName,
      int limit,
      int offset,
      int iterator,
      byte[] key,
      Long streamId,
      boolean fetchPosition,
      byte[] after,
      SelectAfterMode afterMode) {
    super();
    this.setStreamId(streamId);
    this.spaceId = spaceId;
    this.indexId = indexId;
    this.spaceName = spaceName;
    this.indexName = indexName;
    this.limit = limit;
    this.offset = offset;
    this.iterator = iterator;
    this.rawKey = key;
    this.fetchPosition = fetchPosition;
    this.after = after;
    this.afterMode = afterMode;
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_SELECT;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    int items = 6;
    if (fetchPosition) {
      items++;
    }

    if (after != null) {
      items++;
    }

    switch (items) {
      case 7:
        packer.addPayload(RAW_MAP_HEADER_WITH_SEVEN_ITEMS);
        break;
      case 8:
        packer.addPayload(RAW_MAP_HEADER_WITH_EIGHT_ITEMS);
        break;
      default:
        packer.addPayload(RAW_MAP_HEADER_WITH_SIX_ITEMS);
    }

    if (spaceId != null) {
      packer.addPayload(RAW_IPROTO_SPACE_ID); // key
      packer.packInt(spaceId); // value
    } else {
      packer.addPayload(RAW_IPROTO_SPACE_NAME); // key
      packer.packString(spaceName);
    }

    if (indexId != null) {
      packer.addPayload(RAW_IPROTO_INDEX_ID); // key
      packer.packInt(indexId); // value
    } else {
      packer.addPayload(RAW_IPROTO_INDEX_NAME); // key
      packer.packString(indexName);
    }

    packer.addPayload(RAW_IPROTO_LIMIT); // key
    packer.packInt(limit); // value

    packer.addPayload(RAW_IPROTO_OFFSET); // key
    packer.packInt(offset); // value

    packer.addPayload(RAW_IPROTO_ITERATOR); // key
    packer.packInt(iterator); // value

    packer.addPayload(RAW_IPROTO_KEY); // key
    packValue(packer, rawKey, key); // value

    if (fetchPosition) {
      packer.addPayload(RAW_IPROTO_FETCH_POSITION); // key
      packer.packBoolean(true); // value
    }

    if (after != null) {
      if (afterMode != null && afterMode.equals(SelectAfterMode.POSITION)) {
        packer.addPayload(RAW_IPROTO_AFTER_POSITION); // key
      } else {
        packer.addPayload(RAW_IPROTO_AFTER_TUPLE); // key
      }
      packer.addPayload(after); // value
    }

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_SPACE_ID, ValueFactory.newInteger(spaceId));
    map.put(MP_IPROTO_SPACE_NAME, ValueFactory.newString(spaceName));
    map.put(MP_IPROTO_INDEX_ID, ValueFactory.newInteger(indexId));
    map.put(MP_IPROTO_INDEX_NAME, ValueFactory.newString(indexName));
    map.put(MP_IPROTO_LIMIT, ValueFactory.newInteger(limit));
    map.put(MP_IPROTO_OFFSET, ValueFactory.newInteger(offset));
    map.put(MP_IPROTO_ITERATOR, ValueFactory.newInteger(iterator));
    map.put(MP_IPROTO_KEY, key);
    return ValueFactory.newMap(map);
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_SELECT;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoSelect(syncId = ")
          .append(getSyncId())
          .append(", spaceId = ")
          .append(spaceId)
          .append(", indexId = ")
          .append(indexId)
          .append(", spaceName = ")
          .append(spaceName)
          .append(", indexName = ")
          .append(indexName)
          .append(", limit = ")
          .append(limit)
          .append(", offset = ")
          .append(offset)
          .append(", iterator = ")
          .append(iterator)
          .append(", key = ")
          .append(key != null ? key : Arrays.toString(rawKey))
          .append(", fetchPosition = ")
          .append(fetchPosition)
          .append(", after = ")
          .append(Arrays.toString(after))
          .append(", afterMode = ")
          .append(afterMode)
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
