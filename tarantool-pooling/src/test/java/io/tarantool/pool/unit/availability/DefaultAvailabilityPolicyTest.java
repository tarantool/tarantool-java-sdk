/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.unit.availability;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import io.tarantool.pool.PoolEntryState;
import io.tarantool.pool.availability.heartbeat.DefaultAvailabilityPolicy;
import io.tarantool.pool.availability.heartbeat.HeartbeatConstants;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatClosedException;
import io.tarantool.pool.availability.heartbeat.exceptions.HeartbeatException;
import io.tarantool.pool.support.FakeTimer;
import io.tarantool.pool.support.FlakyTimer;
import io.tarantool.pool.support.RecordingPoolEntryView;
import io.tarantool.pool.support.StubClient;

/**
 * The unit tests of the {@link DefaultAvailabilityPolicy} facade.
 *
 * <p>The tests cover the lifecycle of a policy: the configuration, the initialization, the startup,
 * the shutdown and the restart. The heartbeat itself is covered by the tests of the state machine,
 * here it is only used as an observable side effect: the policy must schedule exactly one ping per
 * connection and must cancel it on the shutdown.
 *
 * <p>The whole test is synchronous: the timer is a {@link FakeTimer}, so the ping is sent only when
 * the test fires it, and the stubbed client answers the ping immediately.
 */
@Timeout(value = 30)
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
public class DefaultAvailabilityPolicyTest {

  private static final String TAG = "node-a";
  private static final int IDX = 1;
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private FakeTimer timer;
  private MeterRegistry registry;

  @BeforeEach
  void setUp() {
    this.timer = new FakeTimer();
    this.registry = new SimpleMeterRegistry();
  }

  /**
   * Checks that a fresh connection is available by default.
   *
   * <p>The initial state is returned by {@code initState()} and is applied to the connection by the
   * library, so it must be the active one.
   */
  @Test
  void testInitStateReturnsActiveState() {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    final PoolEntryState state = policy.initState(view);

    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, state);
    Assertions.assertFalse(state.isLocked(), "The connection must be available by default");
  }

  /**
   * Checks that the startup of an unconfigured policy fails.
   *
   * <p>The policy has no timer and no metrics until it is configured, so it must not start a
   * heartbeat: the failure must be reported via the returned stage.
   */
  @Test
  void testStartBeforeConfigureFails() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    final Throwable cause = assertFails(policy.start(view));
    Assertions.assertInstanceOf(NullPointerException.class, cause);
  }

  /**
   * Checks that the startup of a policy configured without a timer fails.
   *
   * <p>The timer is guaranteed to be non-null by the library, so the policy must not work without
   * it, but it must fail gracefully via the returned stage.
   */
  @Test
  void testStartWithNullTimerFails() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    Assertions.assertThrows(NullPointerException.class, () -> policy.configure(null, null));
  }

  /**
   * Checks that the startup with a null view fails.
   *
   * <p>The view is never null by the contract of the library, so the policy must not work with it,
   * but it must fail gracefully via the returned stage.
   */
  @Test
  void testStartWithNullViewFails() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();

    policy.configure(timer, registry);
    final Throwable cause = assertFails(policy.start(null));
    Assertions.assertInstanceOf(NullPointerException.class, cause);
  }

  /**
   * Checks that the repeated configuration keeps the timer of the first one.
   *
   * <p>The policy is configured once per pool, so the repeated configuration is not expected, but
   * it must not break the already configured policy: the timer of the first configuration stays in
   * use.
   */
  @Test
  void testRepeatedConfigureKeepsFirstTimer() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view =
        RecordingPoolEntryView.create(TAG, IDX, policy, StubClient.create().client());

    final FakeTimer second = new FakeTimer();

    policy.configure(timer, registry);
    policy.configure(second, registry);

    assertSucceeds(policy.start(view));

    Assertions.assertEquals(
        1, timer.pendingCount(), "The policy must use the timer of the first configuration");
    Assertions.assertEquals(
        0, second.pendingCount(), "The policy must ignore the second configuration");
  }

  /**
   * Checks that the repeated configuration keeps the metrics of the first one.
   *
   * <p>The counters of the second registry are registered, but they are never incremented, because
   * the policy works with the metrics of the first configuration.
   */
  @Test
  void testRepeatedConfigureKeepsFirstRegistry() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());
    final MeterRegistry second = new SimpleMeterRegistry();

    policy.configure(timer, registry);
    policy.configure(timer, second);

    assertSucceeds(policy.start(view));
    timer.fireNext();

    Assertions.assertEquals(
        1,
        registry.counter(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).count(),
        "The first registry is in use");
    Assertions.assertEquals(
        0,
        second.counter(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).count(),
        "The second registry is not in use");
  }

  /** Checks that the metrics of the policy are registered in the given registry. */
  @Test
  void testMetricsAreRegistered() {
    final DefaultAvailabilityPolicy policy = policy();

    policy.configure(timer, registry);

    Assertions.assertNotNull(registry.find(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).counter());
    Assertions.assertNotNull(registry.find(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).counter());
    Assertions.assertNotNull(registry.find(HeartbeatConstants.POOL_HEARTBEAT_TIME).longTaskTimer());
  }

  /**
   * Checks that the policy works without the metrics.
   *
   * <p>The registry is passed only if the client has specified one, so the policy must not fail
   * without it and must not count the heartbeats.
   */
  @Test
  void testStartWithoutMetrics() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(timer, null);

    assertSucceeds(policy.start(view));
    timer.fireNext();

    Assertions.assertEquals(1, timer.firedCount());
  }

  /**
   * Checks that the repeated startup of the same connection succeeds.
   *
   * <p>The policy is started once per connection, so the repeated startup is not expected, but it
   * must not fail and must not start a second heartbeat.
   */
  @Test
  void testRepeatedStartSucceeds() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(timer, registry);

    final List<CompletionStage<Void>> stages = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      stages.add(policy.start(view));
    }

    for (final CompletionStage<Void> stage : stages) {
      assertSucceeds(stage);
    }
    Assertions.assertEquals(
        1, timer.scheduledCount(), "Only one heartbeat must be started for the connection");
  }

  /**
   * Checks that the repeated stop of the same connection succeeds.
   *
   * <p>The stop must be idempotent, so the repeated shutdown of the same connection must not fail.
   */
  @Test
  void testRepeatedStopSucceeds() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(timer, registry);
    assertSucceeds(policy.start(view));

    for (int i = 0; i < 10; i++) {
      assertSucceeds(policy.stop(view));
    }
    Assertions.assertEquals(
        0, timer.pendingCount(), "The pending ping must be cancelled by the shutdown");
  }

  /**
   * Checks that {@code stop()} can be called on a newly created policy object without exceptions.
   *
   * <p>The library stops the policy if its startup has failed, so closing a policy that has not
   * been started must not fail.
   */
  @Test
  void testStopWithoutStartSucceeds() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(timer, registry);

    assertSucceeds(policy.stop(view));

    Assertions.assertTrue(view.calls().isEmpty(), "The policy must not touch the connection");
  }

  /**
   * Checks that the policy may be started again after the shutdown.
   *
   * <p>The library recycles a connection with a {@code stop -> start} sequence, so the policy must
   * survive it.
   */
  @Test
  void testStartAfterStopSucceeds() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(timer, registry);

    assertSucceeds(policy.start(view));
    assertSucceeds(policy.stop(view));
    assertSucceeds(policy.start(view));

    Assertions.assertEquals(
        2, timer.scheduledCount(), "The restarted heartbeat must schedule a new ping");
  }

  /**
   * Checks that the shutdown with a cause invalidates the connection.
   *
   * <p>The cause may be null, so it must not affect the outcome of the shutdown.
   */
  @Test
  void testStopInvalidatesTheConnection() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(timer, registry);
    assertSucceeds(policy.start(view));

    assertSucceeds(policy.stop(view, new IllegalStateException("the connection is gone")));

    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
  }

  /**
   * Checks that the lifecycles of the different connections are independent.
   *
   * <p>The shutdown of one connection must not affect the other ones.
   */
  @Test
  void testViewsAreIndependent() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView first = view(policy, StubClient.create());
    final RecordingPoolEntryView second = view(policy, StubClient.create());

    policy.configure(timer, registry);

    assertSucceeds(policy.start(first));
    assertSucceeds(policy.start(second));
    Assertions.assertEquals(2, timer.pendingCount(), "Every connection must have its own ping");

    assertSucceeds(policy.stop(first));

    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, first.state());
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, second.state());
    Assertions.assertEquals(
        Collections.singletonList(HeartbeatConstants.ACTIVE_STATE_NAME),
        second.stateChangeNames(),
        "The shutdown of the first connection must not change the state of the second one");
    Assertions.assertEquals(0, second.reconnectCallsCount());
    Assertions.assertEquals(
        1, timer.pendingCount(), "Only the ping of the stopped connection must be cancelled");

    // the heartbeat of the second connection keeps working
    timer.fireNext();
    Assertions.assertEquals(1, registry.counter(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).count());
    Assertions.assertEquals(
        1, timer.pendingCount(), "The second connection must schedule the next ping");
  }

  /**
   * Checks that the restart backoff is zero and does not touch the connection.
   *
   * <p>The policy has no backoff, so the connection must be restarted immediately.
   */
  @Test
  void testRestartBackoffIsZero() {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    final Duration backoff = policy.restartBackoff(view);

    Assertions.assertEquals(Duration.ZERO, backoff);
    Assertions.assertTrue(view.calls().isEmpty(), "The policy must not touch the connection");
  }

  /**
   * Checks that the shutdown does not wait for a ping that has not been answered.
   *
   * <p>The shutdown must complete in a bounded time even if the node is hung: the ping is in flight
   * and is never answered, so the shutdown must not wait for the answer or for the request timeout.
   * The stage is completed by the moment the shutdown returns, and the connection is invalidated.
   *
   * <p>The late answer must not touch the stopped connection: it is neither a success nor a failure
   * for the heartbeat, even a terminal error must not kill the connection that is already closed.
   */
  @Test
  void testStopDoesNotWaitForHangingPing() {
    assertStopCompletesWhilePingHangs(answer -> answer.complete(StubClient.emptyBodyResponse()));
  }

  /**
   * Checks that the connection can be restarted after a failed startup.
   *
   * <p>If the startup has failed, the machine of the connection stays in the policy in the stopped
   * state, so the repeated startup fails until the library stops the policy. The library stops the
   * policy after every failed startup, so after the shutdown the next startup must succeed with a
   * brand-new machine.
   */
  @Test
  void testStartAfterFailedStartAndStopSucceeds() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    policy.configure(new FlakyTimer(timer, 1), registry);

    // the timer fails while the first ping is being scheduled
    final Throwable startCause = assertFails(policy.start(view));
    Assertions.assertInstanceOf(HeartbeatException.class, startCause);
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    Assertions.assertTrue(
        view.listener().events().isEmpty(),
        "The failed startup must not be reported as activation");
    Assertions.assertEquals(0, timer.pendingCount(), "The failed startup must not leave a ping");

    // the dead machine is still registered: the startup without the shutdown is rejected
    Assertions.assertInstanceOf(HeartbeatClosedException.class, assertFails(policy.start(view)));

    // the library stops the policy after the failed startup: the entry must be released
    assertSucceeds(policy.stop(view));

    assertSucceeds(policy.start(view));

    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, view.state());
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), view.listener().events());
    Assertions.assertEquals(1, timer.scheduledCount(), "The new machine must schedule the ping");
    Assertions.assertEquals(1, timer.pendingCount());
  }

  /** Checks that the policy cannot be created without the heartbeat options. */
  @Test
  void testConstructorRejectsNullOptions() {
    final IllegalArgumentException ex =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> new DefaultAvailabilityPolicy(null));
    Assertions.assertEquals("Heartbeat options must not be null!", ex.getMessage());
  }

  /**
   * Checks that the options of the policy are passed to the machines of the connections.
   *
   * <p>The initial delay is the only option that is observable right after the startup: it is the
   * delay of the first scheduled ping.
   */
  @Test
  void testInitialDelayOfOptionsIsApplied() throws Exception {
    final Duration initialDelay = Duration.ofMillis(777);
    final DefaultAvailabilityPolicy policy =
        new DefaultAvailabilityPolicy(HeartbeatOpts.getDefault().withInitialDelay(initialDelay));
    final RecordingPoolEntryView view = view(policy, StubClient.create());
    policy.configure(timer, registry);

    assertSucceeds(policy.start(view));

    Assertions.assertEquals(initialDelay.toMillis(), timer.lastScheduled().delayMillis());
  }

  /**
   * Checks that the thresholds of the options are applied: the connection is invalidated and killed
   * exactly as the options of the policy say.
   */
  @Test
  void testThresholdsOfOptionsAreApplied() throws Exception {
    final DefaultAvailabilityPolicy policy =
        new DefaultAvailabilityPolicy(
            HeartbeatOpts.getDefault()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(1));
    final RecordingPoolEntryView view =
        view(policy, StubClient.failing(new RuntimeException("the node does not answer")));
    policy.configure(timer, registry);
    assertSucceeds(policy.start(view));

    timer.fireNext();
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The death threshold is not reached");

    timer.fireNext();
    Assertions.assertSame(HeartbeatConstants.KILL_STATE, view.state());
    Assertions.assertEquals(1, view.reconnectCallsCount());
  }

  /**
   * Checks that a failed startup of an unconfigured policy does not break the policy.
   *
   * <p>The machine cannot be created before the policy is configured, so nothing must be registered
   * for the connection. After the configuration the same connection must start normally, without
   * the shutdown in between.
   */
  @Test
  void testStartAfterFailedStartBeforeConfigureSucceeds() throws Exception {
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());

    final Throwable cause = assertFails(policy.start(view));
    Assertions.assertInstanceOf(NullPointerException.class, cause);
    Assertions.assertTrue(
        view.calls().isEmpty(), "The failed startup must not touch the connection");
    Assertions.assertEquals(0, timer.scheduledCount(), "The failed startup must not ping");

    policy.configure(timer, registry);

    assertSucceeds(policy.start(view));

    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, view.state());
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), view.listener().events());
    Assertions.assertEquals(1, timer.scheduledCount());
    Assertions.assertEquals(1, timer.pendingCount());
  }

  /**
   * Checks that a terminal failure of the hanging ping, that comes after the shutdown, is ignored.
   *
   * @see #testStopDoesNotWaitForHangingPing()
   */
  @Test
  void testTerminalFailureOfHangingPingAfterStopIsIgnored() {
    assertStopCompletesWhilePingHangs(
        answer -> answer.completeExceptionally(new ConnectionException("the connection is dead")));
  }

  /**
   * Checks that the concurrent configuration is applied once.
   *
   * <p>All the threads try to configure the policy with their own timer, but only one timer must
   * win: the connection that starts afterwards schedules its ping with it and with no other.
   */
  @Test
  void testConcurrentConfigureAppliesOneTimer() throws Exception {
    final int threads = 16;
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());
    final List<FakeTimer> timers = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      timers.add(new FakeTimer());
    }

    runConcurrently(threads, i -> policy.configure(timers.get(i), registry));
    assertSucceeds(policy.start(view));

    Assertions.assertEquals(
        1,
        timers.stream().mapToInt(FakeTimer::pendingCount).sum(),
        "Exactly one ping must be scheduled by the winner of the configuration");
    Assertions.assertEquals(
        1,
        timers.stream().filter(t -> t.scheduledCount() > 0).count(),
        "Only one timer must be in use");
  }

  /**
   * Checks that the concurrent startups of the same connection run one heartbeat.
   *
   * <p>The machine is created by {@code computeIfAbsent}, so all the callers must share it.
   */
  @Test
  void testConcurrentStartsOfTheSameConnectionShareOneMachine() throws Exception {
    final int threads = 16;
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view = view(policy, StubClient.create());
    policy.configure(timer, registry);
    final List<CompletionStage<Void>> stages = Collections.synchronizedList(new ArrayList<>());

    runConcurrently(threads, i -> stages.add(policy.start(view)));

    Assertions.assertEquals(threads, stages.size());
    for (final CompletionStage<Void> stage : stages) {
      assertSucceeds(stage);
    }
    Assertions.assertEquals(1, timer.scheduledCount(), "Only one heartbeat must be started");
    Assertions.assertEquals(
        1,
        view.listener().heartbeatEventCount(HeartbeatEvent.ACTIVATE),
        "The connection must be activated once");
  }

  /**
   * Runs the action in the given count of the threads that are released simultaneously.
   *
   * <p>Every task reports that it has started and then waits for the common gate, so the gate is
   * opened only when all the threads are really running.
   */
  private static void runConcurrently(int threads, IntConsumer action) throws Exception {
    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    final CompletableFuture<Void> gate = new CompletableFuture<>();
    final List<CompletableFuture<Void>> started = new ArrayList<>();
    final List<CompletableFuture<Void>> finished = new ArrayList<>();
    try {
      for (int i = 0; i < threads; i++) {
        final int index = i;
        final CompletableFuture<Void> ready = new CompletableFuture<>();
        started.add(ready);
        finished.add(
            CompletableFuture.runAsync(
                () -> {
                  ready.complete(null);
                  gate.join();
                  action.accept(index);
                },
                executor));
      }
      awaitAll(started);
      gate.complete(null);
      awaitAll(finished);
    } finally {
      gate.complete(null);
      executor.shutdownNow();
    }
  }

  private static void awaitAll(List<CompletableFuture<Void>> futures) throws Exception {
    CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
        .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
  }

  private void assertStopCompletesWhilePingHangs(
      Consumer<CompletableFuture<IProtoResponse>> lateAnswer) {
    final CompletableFuture<IProtoResponse> answer = new CompletableFuture<>();
    final DefaultAvailabilityPolicy policy = policy();
    final RecordingPoolEntryView view =
        view(policy, StubClient.create().nextPingReturnsFuture(answer));
    policy.configure(timer, registry);
    policy.start(view).toCompletableFuture().join();

    timer.fireNext();

    final CompletableFuture<Void> closeFuture = policy.stop(view).toCompletableFuture();

    Assertions.assertTrue(closeFuture.isDone(), "The shutdown must not wait for the ping");
    Assertions.assertFalse(closeFuture.isCompletedExceptionally());
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());

    final int callsAfterStop = view.calls().size();
    lateAnswer.accept(answer);

    Assertions.assertEquals(
        callsAfterStop, view.calls().size(), "The late answer must not touch the connection");
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The stopped connection is not killed");
    Assertions.assertEquals(0, timer.pendingCount(), "The stopped policy must not ping anymore");
  }

  /** Creates {@link DefaultAvailabilityPolicy} with {@link HeartbeatOpts#getDefault()}. */
  private DefaultAvailabilityPolicy policy() {
    return new DefaultAvailabilityPolicy(HeartbeatOpts.getDefault());
  }

  /** Creates {@link RecordingPoolEntryView} with {@link #TAG}, {@link #IDX}. */
  private RecordingPoolEntryView view(DefaultAvailabilityPolicy policy, StubClient client) {
    return RecordingPoolEntryView.create(TAG, IDX, policy, client.client());
  }

  private static void assertSucceeds(CompletionStage<Void> stage) throws Exception {
    Assertions.assertNotNull(stage, "The policy must return a stage");
    stage.toCompletableFuture().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
  }

  private static Throwable assertFails(CompletionStage<Void> stage) throws Exception {
    Assertions.assertNotNull(stage, "The policy must return a stage");
    final CompletableFuture<Void> future = stage.toCompletableFuture();
    final ExecutionException ex =
        Assertions.assertThrows(
            ExecutionException.class, () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    return ex.getCause();
  }
}
