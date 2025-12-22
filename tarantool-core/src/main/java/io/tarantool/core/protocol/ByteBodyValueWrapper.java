/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

/**
 * @author Artyom Dubinin
 */
public class ByteBodyValueWrapper {

  private byte[] packet;
  private int offset;
  private int valueLength;

  public ByteBodyValueWrapper(byte[] packet, int offset, int valueLength) {
    this.packet = packet;
    this.offset = offset;
    this.valueLength = valueLength;
  }

  public byte[] getPacket() {
    return packet;
  }

  public int getOffset() {
    return offset;
  }

  public int getValueLength() {
    return valueLength;
  }
}
