/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import java.util.Map;

import org.msgpack.value.MapValue;
import org.msgpack.value.Value;

import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_ERRCODE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_ERRNO;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_FIELDS;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_FILE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_LINE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_MESSAGE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_ERROR_TYPE;

public class BoxErrorStackItem {

  private final String type;
  private final long line;
  private final String file;
  private final String message;
  private final long errno;
  private final long code;
  private final MapValue details;

  private StringBuilder sb;

  public static BoxErrorStackItem fromStruct(MapValue struct) {
    Map<Value, Value> mapStruct = struct.map();
    Value fields = mapStruct.get(MP_ERROR_FIELDS);
    return new BoxErrorStackItem(
        mapStruct.get(MP_ERROR_TYPE).asStringValue().asString(),
        mapStruct.get(MP_ERROR_LINE).asIntegerValue().asLong(),
        mapStruct.get(MP_ERROR_FILE).asStringValue().asString(),
        mapStruct.get(MP_ERROR_MESSAGE).asStringValue().asString(),
        mapStruct.get(MP_ERROR_ERRNO).asIntegerValue().asLong(),
        mapStruct.get(MP_ERROR_ERRCODE).asIntegerValue().asLong(),
        fields != null ? fields.asMapValue() : null
    );
  }

  private BoxErrorStackItem(String type,
      long line,
      String file,
      String message,
      long errno,
      long code,
      MapValue details) {
    this.type = type;
    this.line = line;
    this.file = file;
    this.message = message;
    this.errno = errno;
    this.code = code;
    this.details = details;
  }

  public String getType() {
    return type;
  }

  public long getLine() {
    return line;
  }

  public String getFile() {
    return file;
  }

  public String getMessage() {
    return message;
  }

  public long getErrno() {
    return errno;
  }

  public long getCode() {
    return code;
  }

  public MapValue getDetails() {
    return details;
  }

  @Override
  public String toString() {
    if (sb == null) {
      sb = new StringBuilder("BoxErrorStackItem{")
          .append("type='").append(type).append('\'')
          .append(", line=").append(line)
          .append(", file='").append(file).append('\'')
          .append(", message='").append(message).append('\'')
          .append(", errno=").append(errno)
          .append(", code=").append(code)
          .append(", details=").append(details)
          .append('}');
    }
    return sb.toString();
  }
}
