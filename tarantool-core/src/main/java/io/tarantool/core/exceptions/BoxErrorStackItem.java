/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.msgpack.value.ValueFactory.MapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.protocol.requests.IProtoConstant;

public class BoxErrorStackItem {

  private static final Logger LOGGER = LoggerFactory.getLogger(BoxErrorStackItem.class);

  /**
   * Type that implies source, as in {@code error_object.base_type}, for example {@code
   * “ClientError”}
   *
   * @apiNote never null! Unknown type is empty string.
   */
  private final String type;

  /**
   * Line number in source code file, as in {@code error_object.trace}.
   *
   * @apiNote never null! Unknown line is 0!
   */
  private final long line;

  /**
   * Source code file where error was caught, as in {@code error_object.trace}.
   *
   * @apiNote never null! Unknown file is empty string.
   */
  private final String file;

  /**
   * Text of reason, as in {@code error_object.message}.
   *
   * @apiNote never null! Unknown message is empty string!
   */
  private final String message;

  /**
   * Ordinal number of the error, as in {@code error_object.errno}. Not to be confused with {@code
   * MP_ERROR_ERRCODE}.
   *
   * @apiNote never null! Unknown errNo is 0!
   */
  private final long errNo;

  /**
   * Number of the error as defined in errcode.h, as in {@code error_object.code}, which can also be
   * retrieved with the {@code C} function {@code box_error_code()}. The value here will be the same
   * as the lower part of the Response-Code-Indicator value.
   *
   * @apiNote never null! Unknown code is 0!
   */
  private final long code;

  /**
   * Additional fields depending on error type. For example, if {@code MP_ERROR_TYPE} is {@code
   * "AccessDeniedError”}, then {@code MP_ERROR_FIELDS} will include {@code “object_type”}, {@code
   * “object_name”}, {@code “access_type”}. This field will be omitted from the response body if
   * there are no additional fields available.
   *
   * @apiNote may be null!
   */
  private final MapValue fields;

  public static BoxErrorStackItem fromStruct(MapValue struct) {
    if (struct == null || struct.map().isEmpty()) {
      LOGGER.error("IPROTO_STACK object must have any fields! Current: 'null'");
      throw new IllegalArgumentException(
          "IPROTO_STACK object must have any fields! Current: 'null'");
    }

    final Map<Value, Value> rawStackObject = struct.map();

    final String type =
        parseString(rawStackObject.get(IProtoConstant.MP_ERROR_TYPE), "MP_ERROR_TYPE");
    final String file =
        parseString(rawStackObject.get(IProtoConstant.MP_ERROR_FILE), "MP_ERROR_FILE");
    final String message =
        parseString(rawStackObject.get(IProtoConstant.MP_ERROR_MESSAGE), "MP_ERROR_MESSAGE");
    final long line = parseLong(rawStackObject.get(IProtoConstant.MP_ERROR_LINE), "MP_ERROR_LINE");
    final long errNo =
        parseLong(rawStackObject.get(IProtoConstant.MP_ERROR_ERRNO), "MP_ERROR_ERRNO");
    final long code =
        parseLong(rawStackObject.get(IProtoConstant.MP_ERROR_ERRCODE), "MP_ERROR_ERRCODE");
    final MapValue fields = parseFields(rawStackObject.get(IProtoConstant.MP_ERROR_FIELDS));

    return BoxErrorStackItem.builder()
        .withType(type)
        .withLine(line)
        .withFile(file)
        .withMessage(message)
        .withErrNo(errNo)
        .withCode(code)
        .withFields(fields)
        .build();
  }

  private BoxErrorStackItem(
      String type, long line, String file, String message, long errNo, long code, MapValue fields) {
    this.type = type;
    this.file = file;
    this.message = message;
    this.errNo = errNo;
    this.code = code;
    this.line = line;
    this.fields = fields;
  }

  /** Use {@link #type()} instead this. */
  @Deprecated
  public String getType() {
    return this.type;
  }

  public String type() {
    return this.type;
  }

  /** Use {@link #line()} instead this. */
  @Deprecated
  public long getLine() {
    return this.line;
  }

  public long line() {
    return this.line;
  }

  /** Use {@link #file()} instead this. */
  @Deprecated
  public String getFile() {
    return file;
  }

  public String file() {
    return this.file;
  }

  /** Use {@link #message()} instead this. */
  @Deprecated
  public String getMessage() {
    return this.message;
  }

  public String message() {
    return this.message;
  }

  /** Use {@link #errNo()} instead this. */
  @Deprecated
  public long getErrno() {
    return this.errNo;
  }

  public long errNo() {
    return this.errNo;
  }

  /** Use {@link #code()} instead this. */
  @Deprecated
  public long getCode() {
    return this.code;
  }

  public long code() {
    return this.code;
  }

  /** Use {@link #fields()} instead this. */
  @Deprecated
  public MapValue getDetails() {
    return this.fields;
  }

  public MapValue fields() {
    return this.fields;
  }

  @Override
  public String toString() {
    return "BoxErrorStackItem{"
        + "type='"
        + this.type
        + "', line='"
        + this.line
        + "', file='"
        + this.file
        + "', message='"
        + this.message
        + "', errno='"
        + this.errNo
        + "', code='"
        + this.code
        + "', fields='"
        + this.fields
        + "'}";
  }

  public MapValue serialize() throws IOException {
    final MapBuilder builder =
        ValueFactory.newMapBuilder()
            .put(IProtoConstant.MP_ERROR_TYPE, ValueFactory.newString(this.type))
            .put(IProtoConstant.MP_ERROR_FILE, ValueFactory.newString(this.file))
            .put(IProtoConstant.MP_ERROR_MESSAGE, ValueFactory.newString(this.message))
            .put(IProtoConstant.MP_ERROR_LINE, ValueFactory.newInteger(this.line))
            .put(IProtoConstant.MP_ERROR_ERRNO, ValueFactory.newInteger(this.errNo))
            .put(IProtoConstant.MP_ERROR_ERRCODE, ValueFactory.newInteger(this.code));
    if (this.fields != null) {
      builder.put(IProtoConstant.MP_ERROR_FIELDS, this.fields);
    }
    return builder.build();
  }

  private static MapValue parseFields(Value raw) {
    if (raw == null || raw.isNilValue()) {
      return null;
    }
    if (!raw.isMapValue()) {
      LOGGER.error("MP_ERROR_FIELDS must be map! Current: '{}'", raw);
      throw new IllegalArgumentException("MP_ERROR_FIELDS must be map!");
    }
    return raw.asMapValue();
  }

  private static String parseString(Value raw, String fieldName) {
    if (raw == null || raw.isNilValue()) {
      LOGGER.error("'{}' must not be null! Current: '{}'", fieldName, raw);
      throw new NullPointerException(fieldName + " must not be null!. Current: " + raw);
    }

    if (!raw.isStringValue()) {
      LOGGER.error("'{}' must be String! Current: '{}'", fieldName, raw);
      throw new IllegalArgumentException(fieldName + " must be String!");
    }
    return raw.asStringValue().asString();
  }

  private static long parseLong(Value raw, String fieldName) {
    if (raw == null || raw.isNilValue()) {
      LOGGER.error("'{}' must not be null! Current: '{}'", fieldName, raw);
      throw new NullPointerException(fieldName + " must not be null!. Current: " + raw);
    }

    if (!raw.isIntegerValue()) {
      LOGGER.error("'{}' must be Long! Current: '{}'", fieldName, raw);
      throw new IllegalArgumentException(fieldName + " must be Long!");
    }
    return raw.asIntegerValue().asLong();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    /**
     * Type that implies source, as in {@code error_object.base_type}, for example {@code
     * “ClientError”}
     *
     * @apiNote never null! Unknown type is empty string.
     */
    private String type = "";

    /**
     * Line number in source code file, as in {@code error_object.trace}.
     *
     * @apiNote never null! Unknown line is 0!
     */
    private long line = 0L;

    /**
     * Source code file where error was caught, as in {@code error_object.trace}.
     *
     * @apiNote never null! Unknown file is empty string.
     */
    private String file = "";

    /**
     * Text of reason, as in {@code error_object.message}.
     *
     * @apiNote never null! Unknown message is empty string!
     */
    private String message = "";

    /**
     * Ordinal number of the error, as in {@code error_object.errno}. Not to be confused with {@code
     * MP_ERROR_ERRCODE}.
     *
     * @apiNote never null! Unknown errNo is 0!
     */
    private long errNo = 0L;

    /**
     * Number of the error as defined in errcode.h, as in {@code error_object.code}, which can also
     * be retrieved with the {@code C} function {@code box_error_code()}. The value here will be the
     * same as the lower part of the Response-Code-Indicator value.
     *
     * @apiNote never null! Unknown code is 0!
     */
    private long code = 0L;

    /**
     * Additional fields depending on error type. For example, if {@code MP_ERROR_TYPE} is {@code
     * "AccessDeniedError”}, then {@code MP_ERROR_FIELDS} will include {@code “object_type”}, {@code
     * “object_name”}, {@code “access_type”}. This field will be omitted from the response body if
     * there are no additional fields available.
     *
     * @apiNote may be null!
     */
    private MapValue fields = null;

    public Builder withType(String type) {
      this.type = Objects.requireNonNull(type, "type must not be null!");
      return this;
    }

    public Builder withLine(long line) {
      if (line < 0) {
        throw new IllegalArgumentException("line must be >= 0");
      }
      this.line = line;
      return this;
    }

    public Builder withFile(String file) {
      this.file = Objects.requireNonNull(file, "file must not be null!");
      return this;
    }

    public Builder withMessage(String message) {
      this.message = Objects.requireNonNull(message, "message must not be null!");
      return this;
    }

    public Builder withErrNo(long errNo) {
      if (errNo < 0) {
        throw new IllegalArgumentException("errNo must be >= 0");
      }
      this.errNo = errNo;
      return this;
    }

    public Builder withCode(long code) {
      if (code < 0) {
        throw new IllegalArgumentException("code must be >= 0");
      }
      this.code = code;
      return this;
    }

    public Builder withFields(MapValue fields) {
      this.fields = fields;
      return this;
    }

    public BoxErrorStackItem build() {
      return new BoxErrorStackItem(
          this.type, this.line, this.file, this.message, this.errNo, this.code, this.fields);
    }
  }
}
