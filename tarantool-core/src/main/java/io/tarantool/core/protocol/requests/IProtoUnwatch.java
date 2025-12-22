/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_UNWATCH;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_UNWATCH;

public class IProtoUnwatch extends IProtoWatch {

  public IProtoUnwatch(String key) {
    super(key);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_UNWATCH;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_UNWATCH;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder.append("IProtoUnwatch(key = ").append(eventKey).append(")");
    }
    return this.stringBuilder.toString();
  }
}
