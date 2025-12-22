/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_REQUEST_TYPE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SCHEMA_VERSION;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SYNC_ID;

public class IProtoRawResponse extends IProtoResponseImpl implements IProtoResponse {

  private final byte[] packet;
  private final int headerSize;
  private final int bodySize;
  private long schemaVersion;
  private final Map<Integer, ByteBodyValueWrapper> byteBodyValues;

  public IProtoRawResponse(MapValue header, byte[] packet, int headerSize) {
    this.header = header;
    this.headerMap = convertMap(header);
    if (headerMap.containsKey(IPROTO_SYNC_ID)) {
      this.syncId = headerMap.get(IPROTO_SYNC_ID).asIntegerValue().asLong();
    } else {
      this.syncId = null;
    }
    if (headerMap.containsKey(IPROTO_SCHEMA_VERSION)) {
      this.schemaVersion = headerMap.get(IPROTO_SCHEMA_VERSION).asIntegerValue().asLong();
    }
    this.requestType = headerMap.get(IPROTO_REQUEST_TYPE).asIntegerValue().asInt();
    this.packet = packet;
    this.headerSize = headerSize;
    this.bodySize = packet.length - headerSize;
    this.byteBodyValues = new HashMap<>();
  }

  @Override
  public ByteBodyValueWrapper getByteBodyValue(int key) {
    unpackByteBodyIfNeeded();

    return byteBodyValues.get(key);
  }

  @Override
  public Map<Integer, ByteBodyValueWrapper> getByteBodyValues() {
    unpackByteBodyIfNeeded();

    return byteBodyValues;
  }

  @Override
  public MapValue getBody() {
    unpackIfNeeded();

    return super.getBody();
  }

  @Override
  public boolean isBodyEmpty() {
    // all body is empty or IPROTO_DATA is empty
    return packet[headerSize] == -128 || packet[headerSize + 2] == -128;
  }

  private void unpackIfNeeded() {
    if (body != null) {
      return;
    }
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packet, headerSize, bodySize)) {
      if (unpacker.hasNext()) {
        this.body = unpacker.unpackValue().asMapValue();
        this.bodyMap = convertMap(body);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void unpackByteBodyIfNeeded() {
    if (!byteBodyValues.isEmpty()) {
      return;
    }

    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packet, headerSize, bodySize)) {
      if (unpacker.hasNext()) {
        int sz = unpacker.unpackMapHeader();
        for (int i = 0; i < sz; i++) {
          int bodyKey = unpacker.unpackInt();
          int read = (int) unpacker.getTotalReadBytes();
          unpacker.skipValue();
          int end = (int) unpacker.getTotalReadBytes();
          byteBodyValues.put(
              bodyKey,
              new ByteBodyValueWrapper(
                  packet, headerSize + read, end - read
              )
          );
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Value getBodyValue(int key) {
    unpackIfNeeded();

    return super.getBodyValue(key);
  }

  @Override
  public long getSchemaVersion() {
    return this.schemaVersion;
  }
}
