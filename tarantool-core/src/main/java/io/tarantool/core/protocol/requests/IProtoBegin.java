/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.time.Duration;
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
import io.tarantool.core.TimeoutsUtils;
import io.tarantool.core.protocol.TransactionIsolationLevel;

public class IProtoBegin extends IProtoBaseRequest {

  // body fields
  private final Duration timeout;
  private final TransactionIsolationLevel level;

  /**
   * @deprecated Use {@link #IProtoBegin(Long, Duration, TransactionIsolationLevel)}
   */
  @Deprecated
  public IProtoBegin(Long streamId, long timeoutMs, TransactionIsolationLevel level) {
    this(streamId, Duration.ofMillis(timeoutMs), level);
  }

  public IProtoBegin(Long streamId, Duration timeout, TransactionIsolationLevel level) {
    TimeoutsUtils.checkTimeoutIsPositive(timeout);
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

    double totalSeconds = timeout.toNanos() / 1_000_000_000.0;
    packer.packDouble(totalSeconds); // value

    if (!level.equals(TransactionIsolationLevel.DEFAULT)) {
      packer.addPayload(RAW_IPROTO_TXN_ISOLATION); // key
      packer.packInt(level.ordinal()); // value
    }

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    double totalSeconds = timeout.toNanos() / 1_000_000_000.0;
    map.put(MP_IPROTO_TIMEOUT, ValueFactory.newFloat(totalSeconds));

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
