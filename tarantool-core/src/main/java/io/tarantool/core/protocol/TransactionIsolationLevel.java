/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

public enum TransactionIsolationLevel {
  /** Take isolation level from global default_isolation_level. */
  DEFAULT,

  /** Allow to read committed, but not confirmed changes. */
  READ_COMMITTED,

  /** Allow to read only confirmed changes. */
  READ_CONFIRMED,

  /** Determine isolation level automatically. */
  BEST_EFFORT,

  /** Allow to read only the changes confirmed on any cluster node. */
  LINEARIZABLE;
}
