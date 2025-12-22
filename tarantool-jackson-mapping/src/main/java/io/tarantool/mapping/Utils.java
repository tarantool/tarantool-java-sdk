/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

public class Utils {

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String byteToHex(byte item) {
    char[] hexChars = new char[2];
    int v = item & 0xFF;
    hexChars[0] = HEX_ARRAY[v >>> 4];
    hexChars[1] = HEX_ARRAY[v & 0x0F];
    return new String(hexChars);
  }
}
