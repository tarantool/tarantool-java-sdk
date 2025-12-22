/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_WATCH_ONCE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_WATCH_ONCE;

public class IProtoWatchOnce extends IProtoWatch {

  public IProtoWatchOnce(String eventKey) {
    super(eventKey);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_WATCH_ONCE;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_WATCH_ONCE;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder.append("IProtoWatchOnce(key = ").append(eventKey).append(")");
    }
    return this.stringBuilder.toString();
  }
}
