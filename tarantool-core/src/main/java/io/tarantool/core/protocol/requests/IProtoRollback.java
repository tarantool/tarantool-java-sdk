/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.util.HashMap;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_ROLLBACK;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_ROLLBACK;

public class IProtoRollback extends IProtoBaseRequest {

  public IProtoRollback(Long streamId) {
    this.setStreamId(streamId);
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    return ValueFactory.newMap(new HashMap<>());
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_ROLLBACK;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_ROLLBACK;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoRollback(syncId = ")
          .append(getSyncId())
          .append(", streamId = ")
          .append(getStreamId())
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
