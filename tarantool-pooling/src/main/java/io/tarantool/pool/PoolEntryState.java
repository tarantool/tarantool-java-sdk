/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

/**
 * An interface representing connection availability. Available connections are provided to the
 * balancer object for requests, while unavailable ones are not.
 *
 * @implSpec Instance objects of this class must be immutable. Their internal state must remain
 *     constant from the beginning of their lifetime until their destruction.
 */
public abstract class PoolEntryState {

  private final String name;

  private final boolean isLocked;

  protected PoolEntryState(String name, boolean isLocked) {
    this.name = name;
    this.isLocked = isLocked;
  }

  /**
   * The name of the state.
   *
   * @return not null and not blank.
   */
  public String name() {
    return this.name;
  }

  /**
   * Returns whether the connection is blocked from being provided to the balancer in this state.
   *
   * @return true if the connection must be hidden from the load balancer, false otherwise.
   */
  public boolean isLocked() {
    return this.isLocked;
  }
}
