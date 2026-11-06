/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.Objects;

import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.msgpack.value.impl.ImmutableBooleanValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.IProtoConstant;

public final class PingResult {

  private static final Logger LOGGER = LoggerFactory.getLogger(PingResult.class);

  public static boolean hasSuccessPingResult(IProtoResponse result, String identity) {
    if (result == null) {
      LOGGER.trace("{}: ping response is null", identity);
      return false;
    }

    if (result.isError()) {
      LOGGER.trace("{}: ping response is error", identity);
      return false;
    }

    if (result.isBodyEmpty()) {
      LOGGER.trace("{}: ping response has empty body", identity);
      return true;
    }

    final Value data = result.getBodyValue(IProtoConstant.IPROTO_DATA);
    if (data == null || !data.isArrayValue()) {
      LOGGER.trace(
          "{}: ping response has no IPROTO_DATA key or IPROTO_DATA value is not array", identity);
      return false;
    }

    final ArrayValue arr = data.asArrayValue();
    if (arr.size() > 1) {
      LOGGER.trace(
          "{}: ping response has IPROTO_DATA array with {} elements in body", arr.size(), identity);
      return false;
    }

    return arr.size() == 0 || Objects.equals(arr.get(0), ImmutableBooleanValueImpl.TRUE);
  }
}
