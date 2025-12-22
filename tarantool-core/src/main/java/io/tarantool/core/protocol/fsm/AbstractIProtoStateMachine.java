/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.fsm;

import java.util.Map;

import io.tarantool.core.connection.Connection;
import io.tarantool.core.exceptions.RunOnceAlreadyCalledException;
import io.tarantool.core.protocol.IProtoRequest;

public abstract class AbstractIProtoStateMachine implements IProtoStateMachine {

  protected final Connection connection;
  protected final IProtoRequest request;
  protected final Map<Long, IProtoStateMachine> fsmRegistry;

  protected boolean calledOnce;

  protected AbstractIProtoStateMachine(
      Connection connection, IProtoRequest request, Map<Long, IProtoStateMachine> fsmRegistry) {
    this.connection = connection;
    this.fsmRegistry = fsmRegistry;
    this.request = request;
  }

  protected abstract void start();

  @Override
  public void runOnce() {
    if (calledOnce) {
      throw new RunOnceAlreadyCalledException();
    }
    calledOnce = true;
    start();
  }
}
