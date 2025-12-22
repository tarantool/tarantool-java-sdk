/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.StringValue;
import org.msgpack.value.Value;

public interface IProtoRequest extends IProtoMessage {

  void setSyncId(long syncId);

  byte[] getPacket(MessageBufferPacker packer) throws Exception;

  @Override
  default boolean isOutOfBand() {
    return false;
  }

  @Override
  default boolean isError() {
    return false;
  }

  @Override
  default int getErrorCode() {
    return -1;
  }

  @Override
  default Value getBodyValue(int key) {
    return null;
  }

  @Override
  default ArrayValue getBodyArrayValue(int key) {
    return null;
  }

  @Override
  default MapValue getBodyMapValue(int key) {
    return null;
  }

  @Override
  default StringValue getBodyStringValue(int key) {
    return null;
  }

  @Override
  default IntegerValue getBodyIntegerValue(int key) {
    return null;
  }

  @Override
  default Value getHeaderValue(int key) {
    return null;
  }

  @Override
  default IntegerValue getHeaderIntegerValue(int key) {
    return null;
  }
}
