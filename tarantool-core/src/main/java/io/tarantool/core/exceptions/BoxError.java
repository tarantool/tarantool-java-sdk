/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import java.util.List;

import org.msgpack.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.protocol.IProtoMessage;
import io.tarantool.core.protocol.requests.IProtoConstant;

public class BoxError extends RuntimeException {

  private static final long serialVersionUID = -407469439059293836L;
  private static final Logger LOGGER = LoggerFactory.getLogger(BoxError.class);

  private final int code;
  private final BoxErrorValue error;
  private final String err24;

  public static BoxError fromIProtoMessage(IProtoMessage packet) {
    final Value rawError = packet.getBodyValue(IProtoConstant.IPROTO_ERROR);
    if (rawError == null || rawError.isNilValue()) {
      LOGGER.error("IPROTO_ERROR must not be null!");
      throw new NullPointerException("IPROTO_ERROR must not be null!");
    }
    final BoxErrorValue err = BoxErrorValue.from(rawError);

    final String err24 = extractErr24(packet.getBodyValue(IProtoConstant.IPROTO_ERROR_24));
    return new BoxError(packet.getErrorCode(), err24, err);
  }

  private BoxError(int code, String err24, BoxErrorValue error) {
    this.code = code;
    this.err24 = err24;
    this.error = error;
  }

  @Override
  public String getMessage() {
    return "BoxError{code='"
        + this.code
        + "', message='"
        + this.err24
        + "', stack="
        + this.error.stack()
        + '}';
  }

  /**
   * @deprecated use {@link #tarantoolMessage()} instead this.
   */
  @Deprecated
  public String getTarantoolMessage() {
    return tarantoolMessage();
  }

  /** Returns {@link IProtoConstant#IPROTO_ERROR_24} value. */
  public String tarantoolMessage() {
    return this.err24;
  }

  /**
   * @deprecated use {@link #stack()} instead this.
   */
  @Deprecated
  public List<BoxErrorStackItem> getTarantoolStack() {
    return this.error.stack();
  }

  /** Returns {@link IProtoConstant#MP_ERROR_STACK} errors stack. */
  public List<BoxErrorStackItem> stack() {
    return this.error.stack();
  }

  /**
   * @deprecated use {@link #errorCode()} instead this.
   */
  @Deprecated
  public int getErrorCode() {
    return errorCode();
  }

  /** Returns error code from response. */
  public int errorCode() {
    return this.code;
  }

  private static String extractErr24(Value rawErr24) {
    if (rawErr24 == null || rawErr24.isNilValue()) {
      LOGGER.error("ERROR_24 must not be null! Current: '{}'", rawErr24);
      throw new NullPointerException("ERROR_24 must not be null!");
    }

    if (!rawErr24.isStringValue()) {
      LOGGER.error("IPROTO_ERROR_24 must be String. Current: '{}'", rawErr24);
      throw new IllegalArgumentException("IPROTO_ERROR_24 must be String!");
    }
    return rawErr24.asStringValue().asString();
  }
}
