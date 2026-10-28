/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability;

import io.tarantool.pool.InstanceConnectionGroup;

/**
 * Metadata about connection.
 *
 * @see AvailabilityPolicy
 */
public abstract class Metadata {

  private final InstanceConnectionGroup group;
  private final int index;

  public Metadata(InstanceConnectionGroup group, int index) {
    this.group = group;
    this.index = index;
  }

  /**
   * The {@link InstanceConnectionGroup} object specified for the current connection. Never null.
   */
  public InstanceConnectionGroup settings() {
    return this.group;
  }

  /**
   * Returns the connection index within the connection group created via {@link
   * InstanceConnectionGroup}.
   *
   * @see InstanceConnectionGroup.Builder#withSize(int)
   */
  public int index() {
    return this.index;
  }
}
