/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import java.util.List;
import java.util.stream.Collectors;

import org.msgpack.value.Value;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERROR;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERROR_24;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_STACK;
import io.tarantool.core.protocol.IProtoMessage;

public class BoxError extends RuntimeException {

  private static final long serialVersionUID = -407469439059293836L;

  private final int code;
  private final String tarantoolMessage;
  private final List<BoxErrorStackItem> stack;

  private StringBuilder sb;

  public static BoxError fromIProtoMessage(IProtoMessage packet) {
    return new BoxError(
        packet.getErrorCode(),
        packet.getBodyStringValue(IPROTO_ERROR_24).asString(),
        packet
            .getBodyMapValue(IPROTO_ERROR)
            .map()
            .get(MP_ERROR_STACK)
            .asArrayValue()
            .list()
            .stream()
            .map(Value::asMapValue)
            .map(BoxErrorStackItem::fromStruct)
            .collect(Collectors.toList()));
  }

  private BoxError(int code, String tarantoolMessage, List<BoxErrorStackItem> stack) {
    this.code = code;
    this.tarantoolMessage = tarantoolMessage;
    this.stack = stack;
  }

  @Override
  public String getMessage() {
    if (sb == null) {
      sb =
          new StringBuilder("BoxError{")
              .append("code=")
              .append(code)
              .append(", message='")
              .append(tarantoolMessage)
              .append('\'')
              .append(", stack=")
              .append(stack)
              .append('}');
    }
    return this.sb.toString();
  }

  public String getTarantoolMessage() {
    return tarantoolMessage;
  }

  public List<BoxErrorStackItem> getTarantoolStack() {
    return this.stack;
  }

  public int getErrorCode() {
    return this.code;
  }
}
