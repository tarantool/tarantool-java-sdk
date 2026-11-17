/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.availability.heartbeat.DefaultAvailabilityPolicy.Utils;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.ActivateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.InvalidateEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.KillEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.PongEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.SendPingEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StopEvent;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatClosedException;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatException;
import io.tarantool.pool.support.FakeTimer;
import io.tarantool.pool.support.RecordingPoolEntryView;
import io.tarantool.pool.support.StubClient;

/**
 * The unit tests of the {@link HeartbeatStateMachine}: the transitions between the states and the
 * completion of the lifecycle futures.
 *
 * <p>The machine is driven directly: the events are passed to it by hand, and the results are
 * observed through the state of the machine, the futures of the events and the recorded calls of
 * the connection view. The heartbeat itself is covered by the tests of the algorithm, here it is
 * only used as an observable side effect: the machine must schedule exactly one ping on the startup
 * and must cancel it on the shutdown.
 *
 * <p>The test is synchronous: the timer is a {@link FakeTimer}, so nothing happens until the test
 * fires the timeout by hand.
 */
@Timeout(value = 30)
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public class HeartbeatStateMachineTest {

  private static final String TAG = "node-a";
  private static final int IDX = 1;
  private static final Duration INITIAL_DELAY = Duration.ofMillis(250);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private FakeTimer timer;
  private RecordingPoolEntryView view;

  @BeforeEach
  void setUp() {
    this.timer = new FakeTimer();
    this.view = viewWith(StubClient.create());
  }

  /**
   * Checks that the startup moves the machine to the started state.
   *
   * <p>The startup is successful only if the future of the event is completed: the library waits
   * for it and restarts the policy if it fails.
   */
  @Test
  void testStartTransmitMachineToStartedState() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();

    machine.handle(new StartEvent(startFuture));

    Assertions.assertInstanceOf(StartedState.class, machine.state());
    Assertions.assertTrue(startFuture.isDone(), "The start future must be completed");
    Assertions.assertFalse(
        startFuture.isCompletedExceptionally(), "The startup must be successful");
  }

  /**
   * Checks that the startup schedules exactly one ping.
   *
   * <p>The heartbeat is the only thing the machine does, so the startup must not schedule the pings
   * twice and must not schedule them at all if the startup has failed.
   */
  @Test
  void testStartSchedulesExactlyOnePing() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    Assertions.assertEquals(1, timer.scheduledCount(), "Exactly one ping must be scheduled");
    Assertions.assertEquals(1, timer.pendingCount(), "The scheduled ping must be pending");
    Assertions.assertDoesNotThrow(startFuture::join);
  }

  /**
   * Checks that the first ping is scheduled with the initial delay.
   *
   * <p>The initial delay is the delay before the very first ping, so it must be applied to the
   * first scheduled timeout.
   */
  @Test
  void testStartSchedulesFirstPingAfterInitialDelay() {
    final HeartbeatStateMachine machine =
        machine(HeartbeatOpts.getDefault().withInitialDelay(INITIAL_DELAY));

    final CompletableFuture<Void> cf = new CompletableFuture<>();
    machine.handle(new StartEvent(cf));

    Assertions.assertDoesNotThrow(cf::join);
    Assertions.assertEquals(1, timer.scheduledCount());
    Assertions.assertEquals(
        INITIAL_DELAY.toMillis(),
        timer.lastScheduled().delayMillis(),
        "The first ping must be delayed by the initial delay");
  }

  /**
   * Checks that the startup clears the window of the pings and fills it with the connection.
   *
   * <p>The window accumulates the results of the pings, so the startup must start the counting from
   * scratch. The connection has just been established by the library, so it counts as the first
   * successful ping: the window holds the mark of the connection.
   */
  @Test
  void testStartFillsTheWindowWithTheConnection() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    final CompletableFuture<Void> cf = new CompletableFuture<>();
    machine.handle(new StartEvent(cf));

    Assertions.assertDoesNotThrow(cf::join);
    Assertions.assertEquals(
        1, machine.ctx().window.size(), "The window must hold the mark of the connection");
    final Integer first = machine.ctx().window.peekFirst();
    Assertions.assertNotNull(first);
    Assertions.assertEquals(0, first.intValue(), "The established connection must be a success");
    Assertions.assertEquals(0, machine.ctx().currentFailedPings);
    Assertions.assertEquals(0, machine.ctx().currentDeathPings);
  }

  /**
   * Checks that the shutdown of a machine that has not been started stops it.
   *
   * <p>The shutdown may come before the startup: the library stops the policy if its startup has
   * failed. The machine must not start the heartbeat then and must complete the close future.
   */
  @Test
  void testStopBeforeStartStopsTheMachine() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    machine.handle(new StopEvent(closeFuture, null));

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertSame(
        HeartbeatConstants.INVALIDATE_STATE, view.state(), "The connection must be invalidated");
    Assertions.assertTrue(closeFuture.isDone(), "The close future must be completed");
    Assertions.assertFalse(
        closeFuture.isCompletedExceptionally(), "The shutdown must be successful");
    Assertions.assertEquals(
        0, timer.scheduledCount(), "The machine must not start the heartbeat on the shutdown");
  }

  /**
   * Checks that the shutdown of a started machine stops the heartbeat.
   *
   * <p>The pending ping must be cancelled, otherwise the machine would keep the connection busy
   * after the shutdown.
   */
  @Test
  void testStopCancelsThePendingPing() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final CompletableFuture<Void> cf = new CompletableFuture<>();
    machine.handle(new StartEvent(cf));
    Assertions.assertEquals(1, timer.pendingCount(), "The ping must be scheduled");
    Assertions.assertDoesNotThrow(cf::join);

    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    machine.handle(new StopEvent(closeFuture, null));

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    Assertions.assertTrue(closeFuture.isDone(), "The close future must be completed");
    Assertions.assertFalse(closeFuture.isCompletedExceptionally());
    Assertions.assertEquals(0, timer.pendingCount(), "The pending ping must be cancelled");
  }

  /**
   * Checks that the repeated shutdown of the same machine is harmless.
   *
   * <p>The shutdown must be idempotent: the repeated shutdown must not fail and must not touch the
   * connection again.
   */
  @Test
  void testRepeatedStopIsIdempotent() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    final CompletableFuture<Void> cf = new CompletableFuture<>();
    machine.handle(new StartEvent(cf));

    Assertions.assertDoesNotThrow(cf::join);
    final CompletableFuture<Void> firstClose = new CompletableFuture<>();
    machine.handle(new StopEvent(firstClose, null));

    final CompletableFuture<Void> secondClose = new CompletableFuture<>();
    machine.handle(new StopEvent(secondClose, null));

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertTrue(firstClose.isDone());
    Assertions.assertTrue(secondClose.isDone(), "The repeated shutdown must complete the future");
    Assertions.assertFalse(secondClose.isCompletedExceptionally());
    Assertions.assertEquals(
        2, view.stateChanges().size(), "The connection must be touched only once");
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
  }

  /**
   * Checks that the startup of a stopped machine fails.
   *
   * <p>The machine is stopped forever: the library starts a new one instead of the stopped one, so
   * the startup of the stopped machine must fail instead of running a second heartbeat.
   */
  @Test
  void testStartAfterStopFails() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    final CompletableFuture<Void> startCf = new CompletableFuture<>();
    machine.handle(new StartEvent(startCf));

    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    machine.handle(new StopEvent(closeFuture, null));

    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    Assertions.assertDoesNotThrow(startCf::join);
    Assertions.assertDoesNotThrow(closeFuture::join);

    final Throwable cause = assertFails(startFuture);
    Assertions.assertInstanceOf(HeartbeatClosedException.class, cause);
    Assertions.assertEquals("Already closed!", cause.getMessage());
  }

  /**
   * Checks that the events of a running heartbeat are ignored before the startup.
   *
   * <p>The machine has no heartbeat yet, so the ping and the state events have nothing to do and
   * must be dropped silently.
   */
  @Test
  void testHeartbeatEventsAreIgnoredInNewState() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    machine.handle(SendPingEvent.INSTANCE);
    machine.handle(new PongEvent(StubClient.emptyBodyResponse(), null));
    machine.handle(ActivateEvent.INSTANCE);
    machine.handle(InvalidateEvent.INSTANCE);
    machine.handle(KillEvent.INSTANCE);

    Assertions.assertInstanceOf(NewState.class, machine.state());
    Assertions.assertTrue(view.calls().isEmpty(), "The connection must not be touched");
    Assertions.assertEquals(0, timer.scheduledCount(), "The machine must not start the heartbeat");
  }

  /**
   * Checks that the events of a running heartbeat are ignored after the shutdown.
   *
   * <p>The heartbeat is stopped, so the ping and the state events have nothing to do and must be
   * dropped silently.
   */
  @Test
  void testHeartbeatEventsAreIgnoredInStoppedState() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    machine.handle(new StopEvent(closeFuture, null));

    final int callsAfterStop = view.calls().size();

    machine.handle(SendPingEvent.INSTANCE);
    machine.handle(new PongEvent(StubClient.emptyBodyResponse(), null));
    machine.handle(ActivateEvent.INSTANCE);
    machine.handle(InvalidateEvent.INSTANCE);
    machine.handle(KillEvent.INSTANCE);

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertEquals(
        callsAfterStop, view.calls().size(), "The connection must not be touched");
    Assertions.assertEquals(0, timer.pendingCount(), "The machine must not schedule the pings");
  }

  /**
   * Checks that a null event is ignored.
   *
   * <p>The event is a part of the internal protocol of the machine, so a null one must not break
   * the machine.
   */
  @Test
  void testNullEventIsIgnored() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    Assertions.assertDoesNotThrow(() -> machine.handle(null));
    Assertions.assertDoesNotThrow(() -> machine.state().handle(null));

    Assertions.assertInstanceOf(NewState.class, machine.state());
    Assertions.assertTrue(view.calls().isEmpty(), "The connection must not be touched");
  }

  /**
   * Checks the names of the states.
   *
   * <p>The name of the state is used in the logs of the heartbeat, so it must be stable.
   */
  @Test
  void testStateNames() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());

    Assertions.assertEquals("HEARTBEAT_NEW", new NewState(machine).toString());
    Assertions.assertEquals("HEARTBEAT_STARTED", new StartedState(machine).toString());
    Assertions.assertEquals("HEARTBEAT_STOPPED", new StoppedState(machine).toString());
  }

  /**
   * Checks that the concurrent startups start the heartbeat once.
   *
   * <p>The library starts the policy once per connection, but the concurrent startups must be safe
   * anyway: the machine is shared by all of them, so only the first startup may schedule the ping
   * and all the start futures must be completed successfully.
   */
  @Test
  void testConcurrentStartsStartTheHeartbeatOnce() throws Exception {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final int starters = 16;
    final CountDownLatch ready = new CountDownLatch(starters);
    final CountDownLatch go = new CountDownLatch(1);
    final List<CompletableFuture<Void>> startFutures = new ArrayList<>();
    final ExecutorService executor = Executors.newFixedThreadPool(starters);
    try {
      for (int i = 0; i < starters; i++) {
        final CompletableFuture<Void> startFuture = new CompletableFuture<>();
        startFutures.add(startFuture);
        executor.submit(
            () -> {
              ready.countDown();
              go.await();
              machine.handle(new StartEvent(startFuture));
              return null;
            });
      }
      Assertions.assertTrue(ready.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
      go.countDown();
      for (final CompletableFuture<Void> startFuture : startFutures) {
        startFuture.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    Assertions.assertInstanceOf(StartedState.class, machine.state());
    Assertions.assertEquals(1, timer.scheduledCount(), "Only one heartbeat must be started");
    Assertions.assertEquals(1, timer.pendingCount(), "Only one ping must be scheduled");
  }

  /**
   * Checks that the shutdown preempts the events that are waiting in the queue.
   *
   * <p>The shutdown is a control event, so it is processed before the ordinary events even if they
   * have been queued earlier. The queued pong must not be handled by the stopped machine, so the
   * connection must not be killed.
   */
  @Test
  void testStopPreemptsTheQueuedEvents() throws Exception {
    final CountDownLatch pingStarted = new CountDownLatch(1);
    final CountDownLatch releasePing = new CountDownLatch(1);
    final HeartbeatOpts opts =
        HeartbeatOpts.getDefault()
            .withPingFunction(
                (client, requestOpts) -> {
                  pingStarted.countDown();
                  try {
                    releasePing.await();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  return CompletableFuture.completedFuture(StubClient.emptyBodyResponse());
                });
    final HeartbeatStateMachine machine = machine(opts);
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    // the ping blocks the machine, so the events below are queued
    final Thread pingThread = new Thread(() -> machine.handle(SendPingEvent.INSTANCE));
    pingThread.start();
    Assertions.assertTrue(pingStarted.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    machine.handle(new PongEvent(null, new ConnectionException("the connection is dead")));
    machine.handle(new StopEvent(closeFuture, null));

    releasePing.countDown();
    pingThread.join(TIMEOUT.toMillis());
    Assertions.assertFalse(pingThread.isAlive(), "The machine must process the queued events");

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertTrue(closeFuture.isDone(), "The close future must be completed");
    Assertions.assertEquals(
        0, view.reconnectCallsCount(), "The queued pong must not kill the stopped connection");
    Assertions.assertEquals(
        0, view.listener().heartbeatEventCount(HeartbeatEvent.KILL), "The kill must not happen");
  }

  /**
   * Checks that the activation is reported to the listener.
   *
   * <p>The connection has just been established by the library, so it is alive: the activation is
   * reported right after the startup, before the first ping. The repeated successful pings do not
   * report it again. The policy does not report the opening of the connection: the physical
   * connection is tracked by the pool entry.
   */
  @Test
  void testActivateEventIsEmittedOnce() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault().withWindowSize(2));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    Assertions.assertDoesNotThrow(startFuture::join);

    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), view.listener().events());
    Assertions.assertEquals(0, timer.firedCount(), "The activation must not wait for a ping");

    timer.fireNext();
    timer.fireNext();
    timer.fireNext();

    final RecordingPoolEntryView.RecordingPoolEventListener listener = view.listener();
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), listener.events());
    Assertions.assertEquals(1, listener.heartbeatEventCount(HeartbeatEvent.ACTIVATE));
    Assertions.assertEquals(
        0,
        listener.connectionOpenedCount(),
        "The policy must not report the opening of the connection");
    Assertions.assertEquals(
        Collections.singletonList(HeartbeatConstants.ACTIVE_STATE_NAME), view.stateChangeNames());
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, view.state());
  }

  /**
   * Checks that the activation is reported before the startup is reported to the library.
   *
   * <p>The library reports the opening of the connection after the startup, so the listener must
   * know about the activation before the startup is completed.
   */
  @Test
  void testActivateIsEmittedBeforeTheStartupIsReported() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    final AtomicBoolean activatedBeforeStartup = new AtomicBoolean(false);
    startFuture.whenComplete(
        (unused, error) ->
            activatedBeforeStartup.set(
                view.listener().heartbeatEventCount(HeartbeatEvent.ACTIVATE) == 1));

    machine.handle(new StartEvent(startFuture));

    Assertions.assertDoesNotThrow(startFuture::join);
    Assertions.assertTrue(
        activatedBeforeStartup.get(), "The activation must be reported before the startup");
  }

  /**
   * Checks that the policy reports no events of the physical connection.
   *
   * <p>The physical connection is tracked by the pool entry, so the policy reports only the
   * heartbeat events. The whole lifecycle is checked here: the activation, the invalidation, the
   * activation of the recovered connection and the kill with the requested restart.
   */
  @Test
  void testPolicyDoesNotReportTheConnectionEvents() {
    final RuntimeException noAnswer = new RuntimeException("the node does not answer");
    this.view = viewWith(StubClient.failing(noAnswer).nextPingFails(noAnswer).nextPingSucceeds());
    final HeartbeatStateMachine machine =
        machine(
            HeartbeatOpts.getDefault()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(2));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    Assertions.assertDoesNotThrow(startFuture::join);

    // the first ping fails, the second one succeeds and the next ones fail again
    timer.fireNext();
    timer.fireNext();
    timer.fireNext();
    timer.fireNext();
    timer.fireNext();

    final RecordingPoolEntryView.RecordingPoolEventListener listener = view.listener();
    Assertions.assertEquals(
        0,
        listener.connectionOpenedCount(),
        "The policy must not report the opening of the connection");
    Assertions.assertEquals(
        0,
        listener.connectionClosedCount(),
        "The policy must not report the closing of the connection");
    Assertions.assertEquals(
        0,
        listener.connectionFailedCount(),
        "The policy must not report the failure of the connection");
    Assertions.assertEquals(
        0,
        listener.reconnectScheduledCount(),
        "The policy must not report the scheduled reconnect");
    Assertions.assertEquals(
        Arrays.asList(
            "onHeartbeatEvent:ACTIVATE",
            "onHeartbeatEvent:INVALIDATE",
            "onHeartbeatEvent:ACTIVATE",
            "onHeartbeatEvent:INVALIDATE",
            "onHeartbeatEvent:KILL"),
        listener.events(),
        "The policy reports the heartbeat events only");
    Assertions.assertEquals(
        1, view.reconnectCallsCount(), "The policy asks the library to restart the connection");
  }

  /**
   * Checks that the invalidation is reported to the listener.
   *
   * <p>The connection is activated on the startup and invalidated later: both events are reported
   * once.
   */
  @Test
  void testInvalidateEventIsEmitted() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        machine(
            HeartbeatOpts.getDefault()
                .withWindowSize(2)
                .withInvalidationThreshold(1)
                .withDeathThreshold(100));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();
    timer.fireNext();
    timer.fireNext();

    final RecordingPoolEntryView.RecordingPoolEventListener listener = view.listener();
    Assertions.assertEquals(
        Arrays.asList("onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:INVALIDATE"),
        listener.events());
    Assertions.assertEquals(1, listener.heartbeatEventCount(HeartbeatEvent.INVALIDATE));
    Assertions.assertEquals(
        0,
        listener.connectionOpenedCount(),
        "The policy must not report the opening of the connection");
    Assertions.assertEquals(
        Arrays.asList(
            HeartbeatConstants.ACTIVE_STATE_NAME, HeartbeatConstants.INVALIDATE_STATE_NAME),
        view.stateChangeNames());
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
  }

  /**
   * Checks that the kill is reported to the listener and that the restart is requested after it.
   *
   * <p>The connection is activated on the startup, invalidated later and killed when the death
   * threshold is reached. The restart is requested only after the kill event has been delivered to
   * the listener.
   */
  @Test
  void testKillEventIsEmittedAndTheRestartIsRequested() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        machine(
            HeartbeatOpts.getDefault()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(1));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));

    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();
    timer.fireNext();

    final RecordingPoolEntryView.RecordingPoolEventListener listener = view.listener();
    Assertions.assertEquals(
        Arrays.asList(
            "onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:INVALIDATE", "onHeartbeatEvent:KILL"),
        listener.events());
    Assertions.assertEquals(1, listener.heartbeatEventCount(HeartbeatEvent.KILL));
    Assertions.assertEquals(
        Arrays.asList(
            HeartbeatConstants.ACTIVE_STATE_NAME,
            HeartbeatConstants.INVALIDATE_STATE_NAME,
            HeartbeatConstants.KILL_STATE_NAME),
        view.stateChangeNames());
    Assertions.assertSame(HeartbeatConstants.KILL_STATE, view.state());
    Assertions.assertEquals(
        Arrays.asList(
            "changeState:" + HeartbeatConstants.ACTIVE_STATE_NAME,
            "emit",
            "client",
            "changeState:" + HeartbeatConstants.INVALIDATE_STATE_NAME,
            "emit",
            "client",
            "changeState:" + HeartbeatConstants.KILL_STATE_NAME,
            "emit",
            "reconnect"),
        view.calls(),
        "The restart must be requested after the kill event has been delivered");
    Assertions.assertEquals(1, view.reconnectCallsCount(), "The restart must be requested once");
    Assertions.assertInstanceOf(StoppedState.class, machine.state());
  }

  /**
   * Checks that a null returned by the ping function is handled as a failed ping.
   *
   * <p>The ping function is a part of the contract with the user, and it must never return null. If
   * it does, the machine must not fail with an unexpected error: the broken ping is a ping error,
   * and the connection is killed through the regular kill event, exactly once.
   */
  @Test
  void testNullPingKillsTheConnection() {
    final HeartbeatStateMachine machine =
        machine(HeartbeatOpts.getDefault().withPingFunction((client, requestOpts) -> null));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertSame(HeartbeatConstants.KILL_STATE, view.state());
    Assertions.assertEquals(1, view.listener().heartbeatEventCount(HeartbeatEvent.KILL));
    Assertions.assertEquals(1, view.reconnectCallsCount(), "The restart must be requested once");
    Assertions.assertEquals(0, timer.pendingCount(), "The killed machine must not ping anymore");
  }

  /**
   * Checks that a null returned by the ping function does not leak the time measurement.
   *
   * <p>The measurement of the ping is started before the ping function is called. If the function
   * returns null, nobody completes the ping, so the machine must stop the measurement itself,
   * otherwise the metric would show a ping that lasts forever. The broken ping is counted as a
   * failed one.
   */
  @Test
  void testNullPingStopsTheTimeMeasurement() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final HeartbeatStateMachine machine =
        new HeartbeatStateMachine(
            this.view,
            HeartbeatOpts.getDefault().withPingFunction((client, requestOpts) -> null),
            Utils.configure(this.timer, registry));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();

    Assertions.assertEquals(
        0,
        registry.get(HeartbeatConstants.POOL_HEARTBEAT_TIME).longTaskTimer().activeTasks(),
        "The measurement of the broken ping must be stopped");
    Assertions.assertEquals(
        1.0,
        registry.get(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).counter().count(),
        "The broken ping must be counted as a failed one");
    Assertions.assertEquals(
        0.0, registry.get(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).counter().count());
  }

  /**
   * Checks that a failed startup does not leave a half-started machine behind.
   *
   * <p>The first ping is scheduled during the startup, and the timer may fail then. The machine has
   * already moved to the started state by that moment, so the error handler must roll it back: the
   * machine must be stopped, its context must be released, the connection must be invalidated and
   * the start future must fail with the cause of the error. The activation must not be reported,
   * because the startup has not been completed.
   */
  @Test
  void testFailedStartStopsTheMachine() {
    final HeartbeatStateMachine machine =
        machine(HeartbeatOpts.getDefault(), new BrokenTimer(this.timer, 0));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();

    machine.handle(new StartEvent(startFuture));

    final Throwable cause = assertFails(startFuture);
    Assertions.assertInstanceOf(HeartbeatException.class, cause);
    Assertions.assertEquals("the timer is broken", cause.getCause().getMessage());
    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertTrue(machine.ctx().window.isEmpty(), "The context must be released");
    Assertions.assertNull(machine.ctx().heartbeatTask, "No ping must be left in the context");
    Assertions.assertEquals(0, timer.pendingCount(), "The machine must not ping anymore");
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    Assertions.assertTrue(
        view.listener().events().isEmpty(), "The activation must not be reported");
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The failed startup is not a kill");

    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    machine.handle(new StopEvent(closeFuture, null));
    Assertions.assertDoesNotThrow(closeFuture::join);
  }

  /**
   * Checks that an unexpected error of a started machine kills the connection.
   *
   * <p>The startup is successful here, so the error handler cannot fail the start future. Instead,
   * it must treat the connection as dead: the kill is reported once and the restart is requested
   * once. The timer is broken when the machine schedules the second ping.
   */
  @Test
  void testUnexpectedErrorAfterStartKillsTheConnection() {
    final HeartbeatStateMachine machine =
        machine(HeartbeatOpts.getDefault(), new BrokenTimer(this.timer, 1));
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();

    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertSame(HeartbeatConstants.KILL_STATE, view.state());
    Assertions.assertEquals(1, view.listener().heartbeatEventCount(HeartbeatEvent.KILL));
    Assertions.assertEquals(1, view.reconnectCallsCount(), "The restart must be requested once");
    Assertions.assertEquals(0, timer.pendingCount(), "The killed machine must not ping anymore");
  }

  /**
   * Checks that the repeated startup of a started machine is completed without side effects.
   *
   * <p>The heartbeat is already running, so the repeated startup must only report the result of the
   * first one through its own future: it must not touch the connection and must not schedule one
   * more ping.
   */
  @Test
  void testRepeatedStartCompletesTheFutureWithoutSideEffects() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final CompletableFuture<Void> firstStart = new CompletableFuture<>();
    machine.handle(new StartEvent(firstStart));
    Assertions.assertDoesNotThrow(firstStart::join);
    final int callsAfterStart = view.calls().size();

    final CompletableFuture<Void> secondStart = new CompletableFuture<>();
    machine.handle(new StartEvent(secondStart));

    Assertions.assertTrue(secondStart.isDone(), "The repeated start must be completed");
    Assertions.assertFalse(secondStart.isCompletedExceptionally());
    Assertions.assertInstanceOf(StartedState.class, machine.state());
    Assertions.assertEquals(callsAfterStart, view.calls().size(), "The connection is not touched");
    Assertions.assertEquals(1, timer.scheduledCount(), "One more ping must not be scheduled");
  }

  /**
   * Checks that the shutdown requested from the start callback is processed after the startup.
   *
   * <p>The callback of the start future is called by the machine itself while it handles the start
   * event. The shutdown is queued then and must be processed right after the startup: the startup
   * stays successful, and the machine is stopped without a pending ping.
   */
  @Test
  void testStopFromTheStartCallbackIsProcessedAfterTheStartup() {
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault());
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    startFuture.whenComplete((unused, error) -> machine.handle(new StopEvent(closeFuture, null)));

    machine.handle(new StartEvent(startFuture));

    Assertions.assertTrue(startFuture.isDone());
    Assertions.assertFalse(startFuture.isCompletedExceptionally(), "The startup must succeed");
    Assertions.assertInstanceOf(StoppedState.class, machine.state());
    Assertions.assertTrue(closeFuture.isDone(), "The close future must be completed");
    Assertions.assertFalse(closeFuture.isCompletedExceptionally());
    Assertions.assertEquals(0, timer.pendingCount(), "The pending ping must be cancelled");
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
  }

  /**
   * Checks that the time measurement is stopped if the ping function throws an exception.
   *
   * <p>The measurement is started before the ping function is called, so the machine must stop it
   * itself if the ping has not been sent. The failed ping is counted as an error, but it does not
   * kill the connection.
   */
  @Test
  void testThrownPingStopsTheTimeMeasurement() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final HeartbeatOpts opts =
        HeartbeatOpts.getDefault()
            .withPingFunction(
                (client, requestOpts) -> {
                  throw new RuntimeException("cannot send the ping");
                });
    final HeartbeatStateMachine machine = machine(opts, registry);
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();

    Assertions.assertEquals(0, activeMeasurements(registry), "The measurement must be stopped");
    Assertions.assertEquals(1.0, errors(registry));
    Assertions.assertEquals(0.0, successes(registry));
    Assertions.assertInstanceOf(StartedState.class, machine.state());
  }

  /**
   * Checks that the time measurement is stopped if the ping fails.
   *
   * <p>The ping is sent, but the answer is an error: the measurement is finished with the answer,
   * and the ping is counted as an error.
   */
  @Test
  void testFailedPingStopsTheTimeMeasurement() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    this.view =
        viewWith(
            StubClient.create().nextPingFails(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault(), registry);
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();

    Assertions.assertEquals(0, activeMeasurements(registry), "The measurement must be stopped");
    Assertions.assertEquals(1.0, errors(registry));
    Assertions.assertEquals(0.0, successes(registry));
  }

  /**
   * Checks that the time measurement lasts while the ping is in flight.
   *
   * <p>The measurement is the metric of the ping duration, so it must be active until the answer is
   * received and must be stopped right after it.
   */
  @Test
  void testTimeMeasurementLastsWhilePingIsInFlight() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final CompletableFuture<IProtoResponse> answer = new CompletableFuture<>();
    this.view = viewWith(StubClient.create().nextPingReturnsFuture(answer));
    final HeartbeatStateMachine machine = machine(HeartbeatOpts.getDefault(), registry);
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);

    timer.fireNext();

    Assertions.assertEquals(1, activeMeasurements(registry), "The ping is in flight");
    Assertions.assertEquals(0.0, successes(registry));

    answer.complete(StubClient.emptyBodyResponse());

    Assertions.assertEquals(0, activeMeasurements(registry), "The measurement must be stopped");
    Assertions.assertEquals(1.0, successes(registry));
    Assertions.assertEquals(0.0, errors(registry));
  }

  private static int activeMeasurements(SimpleMeterRegistry registry) {
    return registry.get(HeartbeatConstants.POOL_HEARTBEAT_TIME).longTaskTimer().activeTasks();
  }

  private static double errors(SimpleMeterRegistry registry) {
    return registry.get(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).counter().count();
  }

  private static double successes(SimpleMeterRegistry registry) {
    return registry.get(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).counter().count();
  }

  /** Creates a machine over the view of the test with the given options. */
  private HeartbeatStateMachine machine(HeartbeatOpts opts) {
    return new HeartbeatStateMachine(this.view, opts, Utils.configure(this.timer, null));
  }

  /** Creates a machine over the view of the test that reports its metrics to the registry. */
  private HeartbeatStateMachine machine(HeartbeatOpts opts, SimpleMeterRegistry registry) {
    return new HeartbeatStateMachine(this.view, opts, Utils.configure(this.timer, registry));
  }

  /** Creates a machine over the view of the test that works with the given timer. */
  private HeartbeatStateMachine machine(HeartbeatOpts opts, Timer brokenTimer) {
    return new HeartbeatStateMachine(this.view, opts, Utils.configure(brokenTimer, null));
  }

  /**
   * A timer that works as the delegate for the given count of the timeouts and fails afterwards.
   */
  private static final class BrokenTimer implements Timer {

    private final Timer delegate;
    private final int workingTimeouts;
    private final AtomicInteger calls = new AtomicInteger();

    BrokenTimer(Timer delegate, int workingTimeouts) {
      this.delegate = delegate;
      this.workingTimeouts = workingTimeouts;
    }

    @Override
    public io.netty.util.Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
      if (this.calls.incrementAndGet() > this.workingTimeouts) {
        throw new RuntimeException("the timer is broken");
      }
      return this.delegate.newTimeout(task, delay, unit);
    }

    @Override
    public Set<io.netty.util.Timeout> stop() {
      return this.delegate.stop();
    }
  }

  /** Creates a view with the given client. */
  private RecordingPoolEntryView viewWith(StubClient stub) {
    return RecordingPoolEntryView.create(
        TAG, IDX, new DefaultNoopAvailabilityPolicy(), stub.client());
  }

  private static Throwable assertFails(CompletableFuture<Void> future) {
    final ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class, () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    return ex.getCause();
  }
}
