/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

public final class HelpersUtils {

  public static Throwable findRootCause(Throwable throwable) {
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }

    return rootCause;
  }
}
