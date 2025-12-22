/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
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
