/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.concurrent.CompletableFuture;

import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.state.Event;

abstract class PolicyEvent implements Event {

  private final String name;

  PolicyEvent(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }

  static final class StartEvent extends PolicyEvent {

    private final CompletableFuture<Void> startFuture;

    StartEvent(CompletableFuture<Void> startFuture) {
      super("HEARTBEAT_START_EVT");
      this.startFuture = startFuture;
    }

    CompletableFuture<Void> startFuture() {
      return this.startFuture;
    }
  }

  /** An event sent when a response to a ping request is received. */
  static final class PongEvent extends PolicyEvent {

    private final IProtoResponse response;

    private final Throwable ex;

    PongEvent(IProtoResponse response, Throwable ex) {
      super("HEARTBEAT_PONG_EVT");
      this.response = response;
      this.ex = ex;
    }

    IProtoResponse response() {
      return this.response;
    }

    Throwable exception() {
      return this.ex;
    }
  }

  /**
   * An event sent when the ping timer expires, indicating that a ping request should be dispatched.
   */
  static final class SendPingEvent extends PolicyEvent {
    static final PolicyEvent INSTANCE = new SendPingEvent();

    private SendPingEvent() {
      super("HEARTBEAT_SEND_PING_EVT");
    }
  }

  static final class StopEvent extends PolicyEvent implements Control {

    private final CompletableFuture<Void> closeFuture;

    private final Throwable cause;

    StopEvent(CompletableFuture<Void> closeFuture, Throwable cause) {
      super("HEARTBEAT_STOP_EVT");
      this.closeFuture = closeFuture;
      this.cause = cause;
    }

    CompletableFuture<Void> closeFuture() {
      return this.closeFuture;
    }

    Throwable cause() {
      return this.cause;
    }
  }

  /**
   * An event sent when the connection state must be changed to {@link
   * HeartbeatConstants#ACTIVE_STATE}. That is, to make the connection available to the load
   * balancer.
   */
  static final class ActivateEvent extends PolicyEvent implements Control {
    static final PolicyEvent INSTANCE = new ActivateEvent();

    private ActivateEvent() {
      super("HEARTBEAT_ACTIVATE_EVT");
    }
  }

  /**
   * An event sent when the connection state must be changed to {@link
   * HeartbeatConstants#INVALIDATE_STATE}. That is, this connection is no longer available to the
   * load balancer.
   */
  static final class InvalidateEvent extends PolicyEvent implements Control {
    static final PolicyEvent INSTANCE = new InvalidateEvent();

    private InvalidateEvent() {
      super("HEARTBEAT_INVALIDATE_EVT");
    }
  }

  /**
   * An event sent when the connection state changes to {@link HeartbeatConstants#KILL_STATE},
   * signaling that the connection must be reconnected.
   */
  static final class KillEvent extends PolicyEvent implements Control {

    static final PolicyEvent INSTANCE = new KillEvent();

    private KillEvent() {
      super("HEARTBEAT_KILL_EVT");
    }
  }

  /**
   * A marker interface. All event classes implementing this interface act as control signals and
   * have the highest priority over regular events.
   */
  private interface Control extends Event {

    @Override
    default boolean isControl() {
      return true;
    }
  }
}
