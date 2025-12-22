/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.MINIMAL_HEADER_SIZE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_REQUEST_TYPE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_STREAM_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_SYNC_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_REQUEST_TYPE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_SYNC_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_THREE_ITEMS_PLUS_IPROTO_STREAM_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_TWO_ITEMS;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_UINT32_RESERVED_FOR_SIZE;
import io.tarantool.core.protocol.IProtoRequest;

public abstract class IProtoBaseRequest implements IProtoRequest {

  protected static byte[] packType(int type) {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packInt(type);
      byte[] typeRaw = packer.toByteArray();
      packer.clear();
      return typeRaw;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected long syncId = -1L;
  protected Long streamId;
  protected StringBuilder stringBuilder;

  protected static void reserveBytesForSize(MessageBufferPacker packer) throws IOException {
    packer.addPayload(RAW_UINT32_RESERVED_FOR_SIZE);
  }

  protected static void patchSize(byte[] packet) {
    int packetSize = packet.length - MINIMAL_HEADER_SIZE;
    packet[1] = (byte) (packetSize >>> 24);
    packet[2] = (byte) (packetSize >>> 16);
    packet[3] = (byte) (packetSize >>> 8);
    packet[4] = (byte) packetSize;
  }

  protected MessageBufferPacker preparePacker(MessageBufferPacker packer) throws IOException {
    reserveBytesForSize(packer);
    packHeader(packer);
    return packer;
  }

  protected byte[] getPacketFromBase(MessageBufferPacker packer) throws IOException {
    packer.flush();
    byte[] packet = packer.toByteArray();
    packer.close();

    patchSize(packet);
    return packet;
  }

  protected void packHeader(MessageBufferPacker packer) throws IOException {
    if (streamId != null) {
      packer.addPayload(RAW_MAP_HEADER_WITH_THREE_ITEMS_PLUS_IPROTO_STREAM_ID);
      packer.packLong(streamId);
    } else {
      packer.addPayload(RAW_MAP_HEADER_WITH_TWO_ITEMS);
    }

    packer.addPayload(RAW_IPROTO_REQUEST_TYPE);
    packer.addPayload(getRequestTypeRaw());

    packer.addPayload(RAW_IPROTO_SYNC_ID);
    packer.packLong(syncId);
  }

  protected void packValue(MessageBufferPacker packer, byte[] rawValue, Value value) throws IOException {
    if (rawValue != null) {
      packer.addPayload(rawValue);
      return;
    }
    packer.packValue(value);
  }

  protected abstract byte[] getRequestTypeRaw();

  @Override
  public abstract MapValue getBody() throws Exception;

  @Override
  public MapValue getHeader() {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_REQUEST_TYPE, ValueFactory.newInteger(getRequestType()));
    map.put(MP_IPROTO_SYNC_ID, ValueFactory.newInteger(getSyncId()));
    if (streamId != null) {
      map.put(MP_IPROTO_STREAM_ID, ValueFactory.newInteger(streamId));
    }
    return ValueFactory.newMap(map);
  }

  @Override
  public long getSyncId() {
    return this.syncId;
  }

  @Override
  public void setSyncId(long syncId) {
    this.syncId = syncId;
  }

  @Override
  public boolean hasSyncId() {
    return this.syncId != -1;
  }

  @Override
  public abstract int getRequestType();

  public Long getStreamId() {
    return this.streamId;
  }

  public void setStreamId(Long streamId) {
    this.streamId = streamId;
  }

  protected MapValue packVclock(Map<Integer, Long> vclock) {
    Map<Value, Value> vclockMap = new HashMap<>();
    vclock.forEach((k, v) -> vclockMap.put(
        ValueFactory.newInteger(k),
        ValueFactory.newInteger(v)
    ));
    return ValueFactory.newMap(vclockMap);
  }
}
