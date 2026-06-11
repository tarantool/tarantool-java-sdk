/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

public enum TransactionIsolationLevel {
  DEFAULT,
  READ_COMMITTED,
  READ_CONFIRMED,
  BEST_EFFORT;
}
