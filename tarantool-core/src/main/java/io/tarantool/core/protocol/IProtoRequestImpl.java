/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.StringValue;
import org.msgpack.value.Value;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERROR_BASE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_REQUEST_TYPE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SYNC_ID;
import io.tarantool.core.protocol.requests.IProtoBaseRequest;

public class IProtoRequestImpl extends IProtoBaseRequest {

  private StringBuilder stringBuilder;
  protected MapValue header;
  protected MapValue body;
  protected Map<Integer, Value> headerMap;
  protected int requestType;
  protected Map<Integer, Value> bodyMap;

  public IProtoRequestImpl() {}

  public IProtoRequestImpl(MapValue header, MapValue body) {
    this.header = header;
    this.headerMap = convertMap(header);
    this.body = body;
    this.bodyMap = convertMap(body);
    if (headerMap.containsKey(IPROTO_SYNC_ID)) {
      this.syncId = headerMap.get(IPROTO_SYNC_ID).asIntegerValue().asLong();
    } else {
      this.syncId = -1;
    }
    this.requestType = headerMap.get(IPROTO_REQUEST_TYPE).asIntegerValue().asInt();
  }

  protected static Map<Integer, Value> convertMap(MapValue map) {
    return map.map().entrySet().stream()
        .collect(Collectors.toMap(e -> ((IntegerValue) e.getKey()).asInt(), Map.Entry::getValue));
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoRequestImpl(header = ")
          .append(getHeader().toJson())
          .append(", body = ")
          .append(getBody().toJson())
          .append(")");
    }
    return this.stringBuilder.toString();
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);

    packer.packValue(body);

    return getPacketFromBase(packer);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return packType(requestType);
  }

  @Override
  public MapValue getBody() {
    return body;
  }

  @Override
  public MapValue getHeader() {
    return header;
  }

  @Override
  public long getSyncId() {
    if (!hasSyncId()) {
      return -1L;
    }

    return this.syncId;
  }

  @Override
  public void setSyncId(long syncId) {
    this.syncId = syncId;
  }

  @Override
  public int getErrorCode() {
    return getRequestType() - IPROTO_ERROR_BASE;
  }

  @Override
  public Value getBodyValue(int key) {
    return bodyMap.get(key);
  }

  @Override
  public ArrayValue getBodyArrayValue(int key) {
    return getBodyValue(key).asArrayValue();
  }

  @Override
  public MapValue getBodyMapValue(int key) {
    return getBodyValue(key).asMapValue();
  }

  @Override
  public StringValue getBodyStringValue(int key) {
    return getBodyValue(key).asStringValue();
  }

  @Override
  public IntegerValue getBodyIntegerValue(int key) {
    return getBodyValue(key).asIntegerValue();
  }

  @Override
  public Value getHeaderValue(int key) {
    return headerMap.get(key);
  }

  @Override
  public IntegerValue getHeaderIntegerValue(int key) {
    return getHeaderValue(key).asIntegerValue();
  }

  @Override
  public boolean hasSyncId() {
    return this.syncId != -1;
  }

  @Override
  public int getRequestType() {
    return this.requestType;
  }
}
