/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.util.Arrays;

import org.msgpack.value.ArrayValue;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_REPLACE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_REPLACE;

public class IProtoReplace extends IProtoInsert {

  public IProtoReplace(
      final Integer spaceId, final String spaceName, ArrayValue tuple, Long streamId) {
    super(spaceId, spaceName, tuple, streamId);
  }

  public IProtoReplace(final Integer spaceId, final String spaceName, byte[] tuple, Long streamId) {
    super(spaceId, spaceName, tuple, streamId);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_REPLACE;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_REPLACE;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoReplace(syncId = ")
          .append(getSyncId())
          .append(", tuple = ")
          .append(tuple != null ? tuple : Arrays.toString(rawTuple))
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
