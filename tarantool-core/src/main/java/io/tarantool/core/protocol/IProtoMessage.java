/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import org.msgpack.value.ArrayValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.StringValue;
import org.msgpack.value.Value;

public interface IProtoMessage {

  MapValue getBody() throws Exception;

  MapValue getHeader() throws Exception;

  long getSyncId();

  boolean hasSyncId();

  int getRequestType();

  boolean isOutOfBand();

  boolean isError();

  int getErrorCode();

  Value getHeaderValue(int key);

  IntegerValue getHeaderIntegerValue(int key);

  Value getBodyValue(int key);

  ArrayValue getBodyArrayValue(int key);

  MapValue getBodyMapValue(int key);

  StringValue getBodyStringValue(int key);

  IntegerValue getBodyIntegerValue(int key);
}
