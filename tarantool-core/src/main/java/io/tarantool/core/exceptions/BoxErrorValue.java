/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.exceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.msgpack.core.MessagePacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.protocol.requests.IProtoConstant;

/**
 * Representation of an {@code MP_ERROR} object in Tarantool.
 *
 * @see <a
 *     href="https://www.tarantool.io/en/doc/latest/reference/internals/msgpack_extensions/#the-error-type">Documentation</a>
 */
public final class BoxErrorValue {

  private static final Logger LOGGER = LoggerFactory.getLogger(BoxErrorValue.class);

  /**
   * @see BoxErrorStackItem
   */
  private final List<BoxErrorStackItem> stack;

  /**
   * Creates {@link BoxErrorValue} from list of stack items.
   *
   * @param stack stack items. Never null and empty!
   */
  public BoxErrorValue(List<BoxErrorStackItem> stack) {
    this.stack = Objects.requireNonNull(stack, "stack must not be null!");
    if (stack.isEmpty()) {
      throw new IllegalArgumentException("MP_ERROR must have at least 1 stack object!");
    }
  }

  /**
   * Parses an {@code MP_ERROR} object from a msgpack map.
   *
   * @param error the {@code MP_ERROR} object represented as a msgpack map. May be null or nil.
   * @return the parsed {@code MP_ERROR} instance or {@code null} if the value is null or nil
   */
  public static BoxErrorValue from(Value error) {

    if (error == null || error.isNilValue()) {
      return null;
    }
    return new BoxErrorValue(parseStack(error));
  }

  public void serialize(MessagePacker packer) throws IOException {
    final List<Value> rawStack = new ArrayList<>(this.stack.size());
    for (BoxErrorStackItem stackItem : this.stack) {
      Objects.requireNonNull(stackItem, "Stack item must not be null!");
      rawStack.add(stackItem.serialize());
    }
    packer.packMapHeader(1);
    packer.packValue(IProtoConstant.MP_ERROR_STACK);
    packer.packValue(ValueFactory.newArray(rawStack));
  }

  /**
   * @see BoxErrorStackItem
   */
  public List<BoxErrorStackItem> stack() {
    return this.stack;
  }

  private static List<BoxErrorStackItem> parseStack(Value errorValue) {
    if (!errorValue.isMapValue()) {
      LOGGER.error("IPROTO_ERROR must be map. Current: '{}'", errorValue);
      throw new IllegalArgumentException("IPROTO_ERROR must be map");
    }

    final Map<Value, Value> errorMap = errorValue.asMapValue().map();
    final Value rawStack = errorMap.get(IProtoConstant.MP_ERROR_STACK);
    if (rawStack == null || rawStack.isNilValue()) {
      throw new NullPointerException("MP_ERROR_STACK must not be null!");
    }

    if (!rawStack.isArrayValue()) {
      LOGGER.error("IPROTO_STACK must be array! Current: '{}'", rawStack);
      throw new IllegalArgumentException("IPROTO_STACK must be array!");
    }

    final ArrayValue stackArray = rawStack.asArrayValue();
    final List<BoxErrorStackItem> result = new ArrayList<>(stackArray.size());
    for (Value item : stackArray) {
      if (item == null || item.isNilValue()) {
        throw new NullPointerException("Stack item must not be null!");
      }
      if (!item.isMapValue()) {
        LOGGER.error("IPROTO_STACK objects must be map! Current: '{}'", item);
        throw new IllegalArgumentException("IPROTO_STACK objects must be map!");
      }
      result.add(BoxErrorStackItem.fromStruct(item.asMapValue()));
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("IPROTO_STACK must have at least 1 stack item!");
    }
    return Collections.unmodifiableList(result);
  }
}
