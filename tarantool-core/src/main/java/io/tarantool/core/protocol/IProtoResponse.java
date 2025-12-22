/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

import java.util.Map;

public interface IProtoResponse extends IProtoMessage {

  ByteBodyValueWrapper getByteBodyValue(int key);

  boolean isBodyEmpty();

  Map<Integer, ByteBodyValueWrapper> getByteBodyValues();

  long getSchemaVersion();
}
