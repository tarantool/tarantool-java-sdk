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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_BEGIN;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TIMEOUT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TXN_ISOLATION;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TIMEOUT;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TXN_ISOLATION;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_BEGIN;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_ONE_ITEM;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_TWO_ITEMS;
import io.tarantool.core.protocol.TransactionIsolationLevel;

public class IProtoBegin extends IProtoBaseRequest {

  // body fields
  private final long timeout;
  private final TransactionIsolationLevel level;

  public IProtoBegin(Long streamId, long timeout, TransactionIsolationLevel level) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("timeout should be greater than 0");
    }
    this.timeout = timeout;
    this.level = level;
    this.setStreamId(streamId);
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);

    int argsCounter = 1;
    if (!level.equals(TransactionIsolationLevel.DEFAULT)) {
      argsCounter++;
    }

    if (argsCounter == 2) {
      packer.addPayload(RAW_MAP_HEADER_WITH_TWO_ITEMS);
    } else {
      packer.addPayload(RAW_MAP_HEADER_WITH_ONE_ITEM);
    }

    packer.addPayload(RAW_IPROTO_TIMEOUT); // key
    packer.packDouble(timeout); // value

    if (!level.equals(TransactionIsolationLevel.DEFAULT)) {
      packer.addPayload(RAW_IPROTO_TXN_ISOLATION); // key
      packer.packInt(level.ordinal()); // value
    }

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_TIMEOUT, ValueFactory.newInteger(timeout));

    if (!level.equals(TransactionIsolationLevel.DEFAULT)) {
      map.put(MP_IPROTO_TXN_ISOLATION, ValueFactory.newInteger(level.ordinal()));
    }
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_BEGIN;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_BEGIN;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoBegin(syncId = ")
          .append(getSyncId())
          .append(", timeout = ")
          .append(timeout)
          .append(", level = ")
          .append(level)
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
