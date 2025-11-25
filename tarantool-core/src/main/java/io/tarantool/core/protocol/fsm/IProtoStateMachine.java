/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.fsm;

import io.tarantool.core.protocol.IProtoResponse;

/**
 * @author Ivan Bannikov
 */
public interface IProtoStateMachine {

  /** Run the request once (data request or watch subscription request). */
  void runOnce();

  /**
   * Process the received {@link IProtoResponse} message.
   *
   * @param msg {@link IProtoResponse} object
   * @return true if the message is processed, false otherwise
   */
  boolean process(IProtoResponse msg);

  /**
   * Destroy the current {@link IProtoStateMachine} object, freeing memory.
   *
   * @param ex the exception to throw when the current {@link IProtoStateMachine} object is
   *     destroyed.
   */
  default void kill(Throwable ex) {}

  default boolean hasNextAction() {
    return false;
  }

  default IProtoStateMachine next() {
    return null;
  }
}
