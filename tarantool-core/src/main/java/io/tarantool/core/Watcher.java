/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.msgpack.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EVENT_DATA;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.fsm.WatcherStateMachine;

/**
 * @author Artyom Dubinin
 */
public class Watcher implements Consumer<IProtoResponse> {

  private static final Logger log = LoggerFactory.getLogger(Watcher.class);

  private final List<Consumer<IProtoResponse>> callbacks;
  private WatcherStateMachine context;
  private long syncId;

  public Watcher() {
    this.callbacks = new ArrayList<>();
  }

  public void addRawCallback(Consumer<IProtoResponse> callback) {
    if (callback != null) {
      callbacks.add(callback);
    }
  }

  public void setStateContext(WatcherStateMachine context) {
    this.context = context;
  }

  public WatcherStateMachine getStateContext() {
    return context;
  }

  public void setSyncId(long syncId) {
    this.syncId = syncId;
  }

  public long getSyncId() {
    return syncId;
  }

  @Override
  public void accept(IProtoResponse message) {
    Value data = message.getBodyValue(IPROTO_EVENT_DATA);
    if (data != null && !data.isNilValue()) {
      for (Consumer<IProtoResponse> callback : callbacks) {
        try {
          callback.accept(message);
        } catch (Exception e) {
          log.error("watcher callback error: %s", e);
        }
      }
    }
  }
}
