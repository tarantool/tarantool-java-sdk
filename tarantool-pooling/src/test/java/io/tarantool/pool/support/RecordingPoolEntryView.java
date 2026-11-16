/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.support;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.tarantool.core.IProtoClient;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.PoolEntryView;
import io.tarantool.pool.PoolEventListener;
import io.tarantool.pool.availability.AvailabilityPolicy;
import io.tarantool.pool.availability.Metadata;

/**
 * A {@link PoolEntryView} that records every interaction made by a policy.
 *
 * <p>The view is intentionally hostile: it throws an exception when the policy passes {@code null}
 * to {@link #changeState(PoolEntryState)} or to {@link #emit(Consumer)}, because the real view
 * forbids such calls. Every violation is counted as well, so the test may report it even if the
 * policy has swallowed the exception.
 *
 * <p>The state changes, the emitted events and the reconnects are recorded in the order of their
 * occurrence, so the test may assert not only the counts but also the sequence of the actions of
 * the policy.
 *
 * <p>A fresh connection is closed, so the view starts in the {@link #CLOSED_STATE} and stays there
 * until the state is set with {@link #changeState(PoolEntryState)}. The test sets the state
 * returned by {@code AvailabilityPolicy#initState(view)} the same way the library does upon the
 * creation of a pool entry.
 */
public final class RecordingPoolEntryView implements PoolEntryView {

  /**
   * The state of a connection that has not been initialized by the policy yet: the connection is
   * closed, so it is not visible to the load balancer.
   */
  public static final PoolEntryState CLOSED_STATE = new PoolEntryState("CLOSED", true) {};

  private final Metadata metadata;
  private final IProtoClient client;
  private final RecordingPoolEventListener listener = new RecordingPoolEventListener();
  private final List<PoolEntryState> stateChanges = new CopyOnWriteArrayList<>();
  private final List<String> calls = new CopyOnWriteArrayList<>();
  private final AtomicInteger reconnectCalls = new AtomicInteger();
  private final AtomicInteger clientCalls = new AtomicInteger();
  private final AtomicInteger nullStateChanges = new AtomicInteger();
  private final AtomicInteger nullEmitCalls = new AtomicInteger();
  private volatile PoolEntryState state = CLOSED_STATE;

  private RecordingPoolEntryView(Metadata metadata, IProtoClient client) {
    this.metadata = metadata;
    this.client = client;
  }

  /**
   * Creates a view for a connection of a group with the given policy.
   *
   * @param tag the tag of the connection group
   * @param index the index of the connection within the group
   * @param policy the policy under test, exposed through the group settings
   * @param client the client the policy is expected to use to check the availability
   * @return the view instance
   */
  public static RecordingPoolEntryView create(
      String tag, int index, AvailabilityPolicy policy, IProtoClient client) {
    return new RecordingPoolEntryView(newMetadata(tag, index, policy), client);
  }

  private static Metadata newMetadata(String tag, int index, AvailabilityPolicy policy) {
    final InstanceConnectionGroup group =
        InstanceConnectionGroup.builder()
            .withHost(InstanceConnectionGroup.DEFAULT_HOST)
            .withPort(InstanceConnectionGroup.DEFAULT_PORT)
            .withSize(1)
            .withTag(tag)
            .withAvailabilityPolicy(policy)
            .build();
    return new Metadata(group, index) {};
  }

  @Override
  public void changeState(PoolEntryState newState) {
    if (newState == null) {
      this.nullStateChanges.incrementAndGet();
      throw new NullPointerException("The connection state must not be null");
    }
    this.stateChanges.add(newState);
    this.calls.add("changeState:" + newState.name());
    this.state = newState;
  }

  @Override
  public Metadata metadata() {
    return this.metadata;
  }

  /**
   * Returns the state of the connection.
   *
   * <p>Before a state is set the connection is closed, so the view reports {@link #CLOSED_STATE}.
   * Afterwards it reports the last state set with {@link #changeState(PoolEntryState)}.
   *
   * @return the current state of the connection
   */
  @Override
  public PoolEntryState state() {
    return this.state;
  }

  @Override
  public IProtoClient client() {
    this.clientCalls.incrementAndGet();
    this.calls.add("client");
    return this.client;
  }

  @Override
  public void reconnect() {
    this.reconnectCalls.incrementAndGet();
    this.calls.add("reconnect");
  }

  @Override
  public void emit(Consumer<PoolEventListener> action) {
    if (action == null) {
      this.nullEmitCalls.incrementAndGet();
      throw new NullPointerException("The action must not be null");
    }
    this.calls.add("emit");
    action.accept(this.listener);
  }

  /**
   * Returns the listener that receives all the events emitted by the policy.
   *
   * @return the recording listener
   */
  public RecordingPoolEventListener listener() {
    return this.listener;
  }

  /**
   * Returns the states the policy has set, in the order of their occurrence.
   *
   * @return the list of the state changes
   */
  public List<PoolEntryState> stateChanges() {
    return this.stateChanges;
  }

  /**
   * Returns the names of the states the policy has set, in the order of their occurrence.
   *
   * @return the list of the state names
   */
  public List<String> stateChangeNames() {
    final List<String> names = new CopyOnWriteArrayList<>();
    for (final PoolEntryState stateChange : this.stateChanges) {
      names.add(stateChange.name());
    }
    return names;
  }

  /**
   * Returns the calls the policy has made to the view, in the order of their occurrence.
   *
   * @return the list of the calls
   */
  public List<String> calls() {
    return this.calls;
  }

  /**
   * Returns the count of the {@link #reconnect()} calls.
   *
   * @return the count of the calls
   */
  public int reconnectCallsCount() {
    return this.reconnectCalls.get();
  }

  /**
   * Returns the count of the {@link #client()} calls.
   *
   * @return the count of the calls
   */
  public int clientCallsCount() {
    return this.clientCalls.get();
  }

  /**
   * Returns the count of the {@code changeState(null)} calls.
   *
   * @return the count of the invalid calls
   */
  public int nullStateChangesCount() {
    return this.nullStateChanges.get();
  }

  /**
   * Returns the count of the {@code emit(null)} calls.
   *
   * @return the count of the invalid calls
   */
  public int nullEmitCallsCount() {
    return this.nullEmitCalls.get();
  }

  /** A {@link PoolEventListener} that counts the events emitted by a policy. */
  public static final class RecordingPoolEventListener implements PoolEventListener {

    private final AtomicInteger connectionOpened = new AtomicInteger();
    private final AtomicInteger connectionClosed = new AtomicInteger();
    private final AtomicInteger connectionFailed = new AtomicInteger();
    private final AtomicInteger reconnectScheduled = new AtomicInteger();
    private final Map<HeartbeatEvent, AtomicInteger> heartbeatEvents =
        new EnumMap<>(HeartbeatEvent.class);
    private final List<String> events = new CopyOnWriteArrayList<>();

    /** Creates an empty listener. */
    public RecordingPoolEventListener() {
      for (final HeartbeatEvent event : HeartbeatEvent.values()) {
        this.heartbeatEvents.put(event, new AtomicInteger());
      }
    }

    @Override
    public void onConnectionOpened(String tag, int index) {
      this.connectionOpened.incrementAndGet();
      this.events.add("onConnectionOpened");
    }

    @Override
    public void onConnectionClosed(String tag, int index) {
      this.connectionClosed.incrementAndGet();
      this.events.add("onConnectionClosed");
    }

    @Override
    public void onConnectionFailed(String tag, int index, Throwable throwable) {
      this.connectionFailed.incrementAndGet();
      this.events.add("onConnectionFailed");
    }

    @Override
    public void onReconnectScheduled(String tag, int index, long delayMs) {
      this.reconnectScheduled.incrementAndGet();
      this.events.add("onReconnectScheduled");
    }

    @Override
    public void onHeartbeatEvent(String tag, int index, HeartbeatEvent event) {
      this.heartbeatEvents.get(event).incrementAndGet();
      this.events.add("onHeartbeatEvent:" + event.name());
    }

    /**
     * Returns the count of the {@code onConnectionOpened} calls.
     *
     * @return the count of the calls
     */
    public int connectionOpenedCount() {
      return this.connectionOpened.get();
    }

    /**
     * Returns the count of the {@code onConnectionClosed} calls.
     *
     * @return the count of the calls
     */
    public int connectionClosedCount() {
      return this.connectionClosed.get();
    }

    /**
     * Returns the count of the {@code onConnectionFailed} calls.
     *
     * @return the count of the calls
     */
    public int connectionFailedCount() {
      return this.connectionFailed.get();
    }

    /**
     * Returns the count of the {@code onReconnectScheduled} calls.
     *
     * @return the count of the calls
     */
    public int reconnectScheduledCount() {
      return this.reconnectScheduled.get();
    }

    /**
     * Returns the count of the events of the given kind.
     *
     * @param event the kind of the event
     * @return the count of the events
     */
    public int heartbeatEventCount(HeartbeatEvent event) {
      final AtomicInteger counter = this.heartbeatEvents.get(event);
      return counter == null ? 0 : counter.get();
    }

    /**
     * Returns the names of the received events, in the order of their occurrence.
     *
     * @return the list of the event names
     */
    public List<String> events() {
      return this.events;
    }
  }
}
