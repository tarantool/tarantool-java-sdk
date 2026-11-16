/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.availability.heartbeat.DefaultAvailabilityPolicy.Utils;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StartEvent;
import io.tarantool.pool.availability.heartbeat.PolicyEvent.StopEvent;
import io.tarantool.pool.support.FakeTimer;
import io.tarantool.pool.support.RecordingPoolEntryView;
import io.tarantool.pool.support.StubClient;

/**
 * The unit tests of the sliding window of the heartbeat: the counting of the failed pings and the
 * decisions taken on its base.
 *
 * <p>The window is the heart of the algorithm: it keeps the results of the last pings, so the
 * heartbeat invalidates the connection when the failed pings reach the invalidation threshold and
 * kills it when the death threshold is exceeded. The tests check the boundaries of the thresholds,
 * the sliding of the window and the reaction to the recovery of the connection.
 *
 * <p>The test is synchronous: the timer is a {@link FakeTimer}, so nothing happens until the test
 * fires the timeout by hand. One fired timeout is one ping, because the stub answers at once.
 */
@Timeout(value = 30)
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public class HeartbeatWindowTest {

  private static final String TAG = "node-a";
  private static final int IDX = 1;

  private FakeTimer timer;
  private RecordingPoolEntryView view;

  @BeforeEach
  void setUp() {
    this.timer = new FakeTimer();
    this.view = viewWith(StubClient.create());
  }

  /**
   * Checks that the established connection counts as the first successful ping.
   *
   * <p>The heartbeat is started right after the connection is established, so the connection itself
   * is the proof that the node is alive: the window starts with the success mark of the connection
   * and no extra ping is needed to confirm it.
   */
  @Test
  void testConnectionIsFirstPing() {
    final HeartbeatStateMachine machine = start(HeartbeatOpts.getDefault().withWindowSize(4));

    Assertions.assertEquals(
        1, machine.ctx().window.size(), "The window must hold the mark of the connection");
    Assertions.assertNotNull(machine.ctx().window.peekFirst());
    Assertions.assertEquals(
        0,
        machine.ctx().window.peekFirst().intValue(),
        "The established connection must be a success");
    Assertions.assertEquals(0, machine.ctx().currentFailedPings);
    Assertions.assertEquals(0, machine.ctx().currentDeathPings);
  }

  /**
   * Checks that the heartbeat makes no decision until the window is full.
   *
   * <p>The window is filled with the results of the pings, so the heartbeat may invalidate the
   * connection only when it knows the whole window: the failed pings of the incomplete window are
   * counted, but they are not a reason to invalidate the connection yet.
   */
  @Test
  void testNoDecisionUntilWindowIsFull() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(4)
                .withInvalidationThreshold(1)
                .withDeathThreshold(100));

    firePings(2);

    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"),
        this.view.listener().events(),
        "The incomplete window must not invalidate the connection");
    Assertions.assertEquals(2, machine.ctx().currentFailedPings, "The failures must be counted");

    firePings(1);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The full window must invalidate the connection");
  }

  /**
   * Checks the boundary of the invalidation threshold.
   *
   * <p>The connection is invalidated when the count of the failed pings in the window reaches the
   * threshold, so the threshold is reached exactly on the given ping.
   */
  @ParameterizedTest(name = "[{index}] the threshold {0} invalidates on the ping {1}")
  @CsvSource({"1, 3", "2, 3", "3, 3", "4, 4"})
  void testInvalidationThresholdBoundary(int threshold, int expectedPing) {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    start(
        HeartbeatOpts.getDefault()
            .withWindowSize(4)
            .withInvalidationThreshold(threshold)
            .withDeathThreshold(100));

    firePings(expectedPing - 1);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The connection must not be invalidated before the threshold is reached");

    firePings(1);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The connection must be invalidated when the threshold is reached");
  }

  /**
   * Checks that the connection is not invalidated when the threshold is out of the window.
   *
   * <p>The window keeps the results of the last pings only, so the threshold that is greater than
   * the size of the window can never be reached.
   */
  @Test
  void testUnreachableInvalidationThreshold() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(4)
                .withInvalidationThreshold(5)
                .withDeathThreshold(100));

    firePings(10);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The threshold is out of the window, so the connection must not be invalidated");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must not be killed");
    Assertions.assertEquals(4, machine.ctx().currentFailedPings, "The window holds the failures");
    Assertions.assertEquals(4, machine.ctx().window.size(), "The window must keep its size");
  }

  /**
   * Checks the boundary of the death threshold.
   *
   * <p>The connection is killed when the count of the invalidations exceeds the death threshold, so
   * the kill happens on the ping that exceeds it.
   */
  @ParameterizedTest(name = "[{index}] the threshold {0} kills on the ping {1}")
  @CsvSource({"1, 2", "2, 3", "3, 4"})
  void testKillThresholdBoundary(int threshold, int expectedPing) {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    start(
        HeartbeatOpts.getDefault()
            .withWindowSize(1)
            .withInvalidationThreshold(1)
            .withDeathThreshold(threshold));

    firePings(expectedPing - 1);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must not be killed before the threshold is exceeded");

    firePings(1);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must be killed when the threshold is exceeded");
  }

  /**
   * Checks that the counter of the deaths is reset when the connection comes back to life.
   *
   * <p>The counter grows on every invalidation and is reset when the connection is alive again, so
   * the connection that recovers before the death threshold is reached is not killed.
   */
  @Test
  void testDeathCounterIsResetWhenConnectionComesBackToLife() {
    final RuntimeException noAnswer = new RuntimeException("the node does not answer");
    this.view =
        viewWith(
            StubClient.failing(noAnswer)
                .nextPingFails(noAnswer)
                .nextPingFails(noAnswer)
                .nextPingSucceeds());
    start(
        HeartbeatOpts.getDefault()
            .withWindowSize(2)
            .withInvalidationThreshold(2)
            .withDeathThreshold(1));

    // the first two pings fail: the connection is invalidated
    firePings(2);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The failed pings must invalidate the connection");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection is alive");

    // the third ping succeeds: the connection is alive again and the counter of the deaths is reset
    firePings(1);

    Assertions.assertEquals(
        2,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.ACTIVATE),
        "The recovered connection must be activated again");

    // the failures of the next pings invalidate the connection again, but the counter of the
    // deaths has been reset, so the kill needs one more invalidation
    firePings(2);

    Assertions.assertEquals(
        2,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The connection must be invalidated again");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The reset counter of the deaths must not kill the recovered connection");

    firePings(1);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The second invalidation in a row must kill the connection");
  }

  /**
   * Checks that the window slides with the pings.
   *
   * <p>The oldest result leaves the window as the new one arrives, so a success pushes the old
   * failures out of the window and the connection becomes alive again.
   */
  @Test
  void testWindowSlides() {
    final RuntimeException noAnswer = new RuntimeException("the node does not answer");
    this.view =
        viewWith(
            StubClient.failing(noAnswer)
                .nextPingFails(noAnswer)
                .nextPingFails(noAnswer)
                .nextPingSucceeds());
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(2)
                .withInvalidationThreshold(2)
                .withDeathThreshold(100));

    firePings(2);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The failed pings must invalidate the connection");
    Assertions.assertEquals(2, machine.ctx().window.size(), "The window must be full");

    firePings(1);

    Assertions.assertEquals(
        2,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.ACTIVATE),
        "The success must push the old failure out of the window and activate the connection");
    Assertions.assertEquals(1, machine.ctx().currentFailedPings, "The old failure must leave");
    Assertions.assertEquals(2, machine.ctx().window.size(), "The window must keep its size");
  }

  /**
   * Checks that the window keeps the configured size.
   *
   * <p>The window is a sliding one, so it must not grow beyond the configured size.
   */
  @Test
  void testWindowKeepsItsSize() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(3)
                .withInvalidationThreshold(2)
                .withDeathThreshold(100));

    firePings(10);

    Assertions.assertEquals(3, machine.ctx().window.size(), "The window must keep its size");
    Assertions.assertEquals(3, machine.ctx().currentFailedPings);
  }

  /**
   * Checks that the terminal error kills the connection without waiting for the window.
   *
   * <p>The connection is dead, so there is nothing to wait for: the heartbeat kills the connection
   * at once.
   */
  @Test
  void testTerminalErrorBypassesWindow() {
    this.view = viewWith(StubClient.failing(new ConnectionException("the connection is dead")));
    start(
        HeartbeatOpts.getDefault()
            .withWindowSize(4)
            .withInvalidationThreshold(1)
            .withDeathThreshold(100));

    firePings(1);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The dead connection must not wait for the window");
    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The dead connection must be killed");
    Assertions.assertSame(HeartbeatConstants.KILL_STATE, this.view.state());
    Assertions.assertEquals(1, this.view.reconnectCallsCount(), "The restart must be requested");
  }

  /**
   * Checks that the error that is not terminal is counted as a failed ping.
   *
   * <p>The node may be busy or the packet may be lost, so such an error is not a proof that the
   * connection is dead: it is counted in the window as a failed ping.
   */
  @Test
  void testNonTerminalErrorIsCountedAsFailure() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(4)
                .withInvalidationThreshold(1)
                .withDeathThreshold(100));

    firePings(1);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must not be killed");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The incomplete window must not invalidate the connection");
    Assertions.assertEquals(1, machine.ctx().currentFailedPings, "The failure must be counted");
    Assertions.assertEquals(2, machine.ctx().window.size());
  }

  /**
   * Checks that the killed connection is not pinged anymore.
   *
   * <p>The library restarts the connection after the kill, so the heartbeat must stop at once.
   */
  @Test
  void testKillStopsHeartbeat() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(1));

    firePings(2);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must be killed");
    Assertions.assertEquals(
        0, this.timer.pendingCount(), "The heartbeat must be stopped after the kill");
    Assertions.assertInstanceOf(StoppedState.class, machine.state());
  }

  /**
   * Checks the numbers of the default options against the legacy heartbeat.
   *
   * <p>The legacy heartbeat invalidates the connection on the third failed ping and kills it on the
   * seventh one with the default options: the new heartbeat must behave the same way.
   */
  @Test
  void testDefaultOptionsMatchLegacyNumbers() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    start(HeartbeatOpts.getDefault());

    firePings(2);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The legacy heartbeat invalidates the connection on the third failed ping");

    firePings(1);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The legacy heartbeat invalidates the connection on the third failed ping");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must be alive on the third failed ping");

    firePings(4);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The legacy heartbeat kills the connection on the seventh failed ping");
    Assertions.assertEquals(
        Arrays.asList(
            "onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:INVALIDATE", "onHeartbeatEvent:KILL"),
        this.view.listener().events());
  }

  /**
   * Checks that the hanging ping does not pile up the pings.
   *
   * <p>The ping that hangs forever is a proof that the node does not answer, but the policy must
   * not send the next ping while the previous one is still in flight: the requests would pile up
   * and flood the connection. The timeout of the request is the concern of the client, so the
   * policy waits for the answer and decides nothing on its own.
   */
  @Test
  void testHangingPingDoesNotPileUpPings() {
    final StubClient stub = StubClient.create().nextPingHangs();
    this.view = viewWith(stub);
    final HeartbeatStateMachine machine = start(HeartbeatOpts.getDefault());

    firePings(1);

    Assertions.assertEquals(1, stub.pingCount(), "Only the hanging ping must be sent");
    Assertions.assertEquals(
        0,
        this.timer.pendingCount(),
        "The next ping must not be scheduled while the previous one is in flight");
    Assertions.assertEquals(1, this.timer.scheduledCount(), "The next ping must not be scheduled");
    Assertions.assertEquals(1, machine.ctx().window.size(), "The window must not grow");
    Assertions.assertEquals(
        0, machine.ctx().currentFailedPings, "The hanging ping is not a failure yet");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The connection must not be invalidated");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must not be killed");
    Assertions.assertEquals(
        0, this.view.reconnectCallsCount(), "The connection must not be restarted");
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, this.view.state());
  }

  /**
   * Checks that the answer that is not a success is counted as a failed ping.
   *
   * <p>The node may answer with an error or with an unexpected body. Such an answer is a proof that
   * the node is not healthy, but it is not a proof that the connection is dead, so it is counted in
   * the window as an ordinary failed ping.
   */
  @ParameterizedTest(name = "[{index}] {1}")
  @MethodSource("dataForTestFailedPingAnswers")
  void testFailedAnswerIsCountedAsFailedPing(IProtoResponse answer, String description) {
    this.view = viewWith(StubClient.create().nextPingReturns(answer));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(100));

    firePings(1);

    Assertions.assertEquals(1, machine.ctx().currentFailedPings, description);
    Assertions.assertEquals(
        1, this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE), description);
    Assertions.assertEquals(
        0, this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL), description);
  }

  /** The answers of the node that are not a success of the ping. */
  static Stream<Arguments> dataForTestFailedPingAnswers() {
    return Stream.of(
        Arguments.of(IProtoResponses.error(0x2a, "boom"), "the answer is an error"),
        Arguments.of(IProtoResponses.dataArray(ValueFactory.newBoolean(false)), "DATA=[false]"),
        Arguments.of(IProtoResponses.dataNotArray(), "DATA is not an array"),
        Arguments.of(IProtoResponses.withoutData(), "there is no DATA at all"));
  }

  /**
   * Checks that the pong that arrives after the shutdown is ignored.
   *
   * <p>The answer of the ping may arrive after the policy has been stopped: the stopped machine
   * must not react to it, otherwise it would kill the connection that the library has already
   * closed.
   */
  @Test
  void testPongAfterShutdownIsIgnored() {
    final CompletableFuture<IProtoResponse> answer = new CompletableFuture<>();
    this.view = viewWith(StubClient.create().nextPingReturnsFuture(answer));
    final HeartbeatStateMachine machine = start(HeartbeatOpts.getDefault());

    firePings(1);

    final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    machine.handle(new StopEvent(closeFuture, null));
    Assertions.assertDoesNotThrow(closeFuture::join);

    answer.completeExceptionally(new ConnectionException("the connection is dead"));

    Assertions.assertSame(
        HeartbeatConstants.INVALIDATE_STATE,
        this.view.state(),
        "The stopped connection must stay invalidated");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The stopped connection must not be killed");
    Assertions.assertEquals(
        0, this.view.reconnectCallsCount(), "The stopped connection must not be restarted");
    Assertions.assertEquals(0, machine.ctx().window.size(), "The shutdown must clear the window");
    Assertions.assertEquals(0, this.timer.pendingCount(), "The heartbeat must not be scheduled");
  }

  /**
   * Checks that the error of the ping function is counted as a failed ping.
   *
   * <p>The ping function is a part of the user configuration, so it may fail before the request is
   * even sent. Such an error is not a proof that the connection is dead, so it is counted in the
   * window as an ordinary failed ping.
   */
  @Test
  void testErrorOfPingFunctionIsCountedAsFailedPing() {
    final RuntimeException noAnswer = new RuntimeException("the node does not answer");
    this.view = viewWith(StubClient.create());
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withPingFunction(
                    (client, requestOpts) -> {
                      throw noAnswer;
                    })
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(100));

    firePings(1);

    Assertions.assertEquals(
        1, machine.ctx().currentFailedPings, "The error must be counted as a failure");
    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The connection must be invalidated");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The connection must not be killed");
  }

  /**
   * Checks that the terminal error of the ping function kills the connection at once.
   *
   * <p>The dead connection must not wait for the window: the kill is the only way to make the
   * library open a new one.
   */
  @Test
  void testTerminalErrorOfPingFunctionKillsConnection() {
    final ConnectionException dead = new ConnectionException("the connection is dead");
    this.view = viewWith(StubClient.create());
    start(
        HeartbeatOpts.getDefault()
            .withPingFunction(
                (client, requestOpts) -> {
                  throw dead;
                })
            .withWindowSize(4)
            .withInvalidationThreshold(1)
            .withDeathThreshold(100));

    firePings(1);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The dead connection must not wait for the window");
    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.KILL),
        "The dead connection must be killed");
    Assertions.assertSame(HeartbeatConstants.KILL_STATE, this.view.state());
    Assertions.assertEquals(1, this.view.reconnectCallsCount(), "The restart must be requested");
  }

  /**
   * Checks that the window reserves a slot for the established connection.
   *
   * <p>The mark of the connection occupies one of the slots of the window, so the heartbeat knows
   * the whole window one ping earlier than its size.
   */
  @ParameterizedTest(name = "[{index}] the window of {0} decides on the ping {1}")
  @CsvSource({"2, 1", "3, 2", "4, 3", "5, 4"})
  void testWindowReservesSlotForConnection(int windowSize, int expectedPing) {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    start(
        HeartbeatOpts.getDefault()
            .withWindowSize(windowSize)
            .withInvalidationThreshold(1)
            .withDeathThreshold(100));

    firePings(expectedPing - 1);

    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The incomplete window must not invalidate the connection");

    firePings(1);

    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The window must decide as soon as it is full");
  }

  /**
   * Checks that the window of the size one holds the result of the last ping only.
   *
   * <p>The mark of the connection leaves the window at the first ping, so the window of the size
   * one holds the result of the last ping only.
   */
  @Test
  void testWindowOfSizeOneHoldsLastPingOnly() {
    this.view = viewWith(StubClient.failing(new RuntimeException("the node does not answer")));
    final HeartbeatStateMachine machine =
        start(
            HeartbeatOpts.getDefault()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(100));

    firePings(1);

    Assertions.assertEquals(
        1, machine.ctx().window.size(), "The window must hold the last ping only");
    Assertions.assertEquals(
        1, machine.ctx().currentFailedPings, "The mark of the connection must leave the window");
    Assertions.assertEquals(
        1,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The full window must invalidate the connection at once");
  }

  /**
   * Checks that the heartbeat sends exactly one ping per interval.
   *
   * <p>Every fired timeout must send exactly one ping and take exactly one client, so the node is
   * not flooded and the client is not touched twice.
   */
  @Test
  void testHeartbeatSendsOnePingPerInterval() {
    final StubClient stub = StubClient.create();
    this.view = viewWith(stub);
    start(HeartbeatOpts.getDefault());

    firePings(5);

    Assertions.assertEquals(5, stub.pingCount(), "Every fired timeout must send exactly one ping");
    Assertions.assertEquals(
        5, this.view.clientCallsCount(), "The client must be taken once per ping");
    Assertions.assertEquals(1, this.timer.pendingCount(), "Exactly one ping must be pending");
    Assertions.assertEquals(
        0,
        this.view.listener().heartbeatEventCount(HeartbeatEvent.INVALIDATE),
        "The connection is alive");
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, this.view.state());
  }

  /** Starts the heartbeat with the given options and waits for the startup. */
  private HeartbeatStateMachine start(HeartbeatOpts opts) {
    final HeartbeatStateMachine machine = machine(opts);
    final CompletableFuture<Void> startFuture = new CompletableFuture<>();
    machine.handle(new StartEvent(startFuture));
    Assertions.assertDoesNotThrow(startFuture::join);
    return machine;
  }

  /** Fires the given count of the scheduled pings. */
  private void firePings(int count) {
    for (int i = 0; i < count; i++) {
      Assertions.assertNotNull(this.timer.fireNext(), "The ping must be scheduled");
    }
  }

  /** Creates a machine over the view of the test with the given options. */
  private HeartbeatStateMachine machine(HeartbeatOpts opts) {
    return new HeartbeatStateMachine(this.view, opts, Utils.configure(this.timer, null));
  }

  /** Creates a view with the given client. */
  private RecordingPoolEntryView viewWith(StubClient stub) {
    return RecordingPoolEntryView.create(
        TAG, IDX, new DefaultNoopAvailabilityPolicy(), stub.client());
  }
}
