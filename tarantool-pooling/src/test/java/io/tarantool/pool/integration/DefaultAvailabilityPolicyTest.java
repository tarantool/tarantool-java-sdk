/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.integration;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.pool.HeartbeatEvent;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.availability.heartbeat.DefaultAvailabilityPolicy;
import io.tarantool.pool.availability.heartbeat.HeartbeatConstants;
import io.tarantool.pool.support.RecordingPoolEntryView;

/**
 * The integration tests of the {@link DefaultAvailabilityPolicy} against a real Tarantool node.
 *
 * <p>The policy is driven directly: the view is a {@link RecordingPoolEntryView} over a real
 * client, so the tests observe both sides of the heartbeat: the events and the states reported to
 * the view, and the pings that really reached the server. The pings are counted by the server: the
 * ping function calls the {@code inc()} procedure of the test server, so the count does not depend
 * on the client side metrics.
 *
 * <p>The tests share the call counter and the pipe lock of the first node, so they must not run
 * concurrently.
 */
@TestInstance(Lifecycle.PER_METHOD)
@Execution(ExecutionMode.SAME_THREAD)
public class DefaultAvailabilityPolicyTest extends BasePoolTest {

  private static final Duration DEFAULT_AWAIT_TIMEOUT = Duration.ofSeconds(20);

  /** The interval of the pings of the tests. The request timeout of the ping equals to it. */
  private static final long PING_INTERVAL_MS = 250;

  private static final String TAG = "node-a";
  private static final int IDX = 1;

  /** The ping that is visible on the server side: it increments the counter of the calls. */
  private static final BiFunction<
          IProtoClient, IProtoRequestOpts, CompletableFuture<IProtoResponse>>
      COUNTING_PING = (client, opts) -> client.eval("inc()", ValueFactory.emptyArray(), opts);

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

  private static String host1;
  private static int port1;

  private static TarantoolContainer<?> tt1;
  private static TarantoolContainer<?> tt2;

  private static final Timer TIMER = new HashedWheelTimer();

  private final List<IProtoClient> clients = new ArrayList<>();

  /** The shutdowns of the policies of the test: they must not ping after the test is over. */
  private final List<Runnable> cleanups = new ArrayList<>();

  /**
   * The port of the test proxy of the node. The proxy forwards the data as is until the pipe is
   * locked with {@code lock_pipe(true)}: then the requests of the client are lost, so the node
   * looks like a hung one while the connection stays open.
   */
  private static final int PROXY_PORT = 3305;

  @BeforeAll
  public static void beforeAll() {
    tt1 = TarantoolContainerClientHelper.createTarantoolContainer(PROXY_PORT).withEnv(ENV_MAP);
    tt2 = TarantoolContainerClientHelper.createTarantoolContainer(PROXY_PORT).withEnv(ENV_MAP);
    tt1.start();
    tt2.start();
    TarantoolContainerClientHelper.execInitScript(tt1);
    TarantoolContainerClientHelper.execInitScript(tt2);

    host1 = tt1.getHost();
    port1 = tt1.getMappedPort(PROXY_PORT);
    host2 = tt2.getHost();
    port2 = tt2.getMappedPort(PROXY_PORT);
  }

  @AfterAll
  static void tearDown() {
    tt1.stop();
    tt2.stop();
    TIMER.stop();
  }

  @BeforeEach
  void setUp() {
    for (final TarantoolContainer<?> node : Arrays.asList(tt1, tt2)) {
      execLua(node, "lock_pipe(false)");
      execLua(node, "reset_call_counter()");
    }
  }

  @AfterEach
  void closeClients() throws Exception {
    // the pipe must never stay locked for the next test
    execLua(tt1, "lock_pipe(false)");
    execLua(tt2, "lock_pipe(false)");
    try {
      // stop the policies first: otherwise a failed test leaves the pings running on the timer
      this.cleanups.forEach(Runnable::run);
    } finally {
      this.cleanups.clear();
      for (final IProtoClient client : this.clients) {
        client.close();
      }
      this.clients.clear();
    }
  }

  /*
   **********************************************************
   * Lifecycle
   **********************************************************
   */

  /**
   * Checks that the startup on a live node activates the connection immediately.
   *
   * <p>The connection has just been established by the library, so it is alive: the activation is
   * reported right after the startup and does not wait for the first ping. The initial delay is
   * long here, so the server must not see any ping at that moment.
   */
  @Test
  void testStartOnLiveNodeActivatesImmediately() {
    final DefaultAvailabilityPolicy policy = policy(Duration.ofSeconds(30));
    final RecordingPoolEntryView view = newView(policy);

    policy.start(view).toCompletableFuture().join();

    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, view.state());
    Assertions.assertEquals(
        Collections.singletonList(HeartbeatConstants.ACTIVE_STATE_NAME), view.stateChangeNames());
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), view.listener().events());
    Assertions.assertEquals(0, serverPings(), "The activation must not wait for a ping");

    policy.stop(view).toCompletableFuture().join();
  }

  /**
   * Checks that the shutdown invalidates the connection and stops the pings.
   *
   * <p>The pings are counted by the server, so the check does not depend on the state of the
   * policy: after the shutdown the counter must stay the same for several ping intervals.
   */
  @Test
  void testStopInvalidatesConnectionAndStopsPings() throws Exception {
    final DefaultAvailabilityPolicy policy = policy(Duration.ofMillis(PING_INTERVAL_MS));
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();
    awaitServerPings(3);

    final CompletableFuture<Void> closeFuture = policy.stop(view).toCompletableFuture();

    Assertions.assertDoesNotThrow(
        () -> closeFuture.get(DEFAULT_AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());

    // a ping that was in flight at the moment of the shutdown may still reach the server
    Thread.sleep(PING_INTERVAL_MS);
    final int pingsAfterStop = serverPings();
    Thread.sleep(4 * PING_INTERVAL_MS);

    Assertions.assertEquals(
        pingsAfterStop, serverPings(), "The pings must not be sent after the shutdown");
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"),
        view.listener().events(),
        "The shutdown must not be reported as a heartbeat event");
  }

  /**
   * Checks that the concurrent startups of the same connection run one heartbeat.
   *
   * <p>The library starts the policy once per connection, but the concurrent startups must be safe
   * anyway. If every startup ran its own heartbeat, the server would receive a hundred times more
   * pings than one heartbeat sends, so the count of the pings is bounded from above.
   */
  @Test
  void testConcurrentStartsRunOneHeartbeat() throws Exception {
    final int starters = 100;
    final long observationMs = 2000;
    final DefaultAvailabilityPolicy policy = policy(Duration.ofMillis(PING_INTERVAL_MS));
    final RecordingPoolEntryView view = newView(policy);

    final CountDownLatch go = new CountDownLatch(1);
    final ExecutorService executor = Executors.newFixedThreadPool(16);
    final List<Future<CompletableFuture<Void>>> started = new ArrayList<>();
    try {
      for (int i = 0; i < starters; i++) {
        started.add(
            executor.submit(
                () -> {
                  go.await();
                  return policy.start(view).toCompletableFuture();
                }));
      }
      go.countDown();
      for (final Future<CompletableFuture<Void>> future : started) {
        Assertions.assertDoesNotThrow(
            () ->
                future
                    .get(DEFAULT_AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .get(DEFAULT_AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
      }
    } finally {
      executor.shutdownNow();
    }

    Thread.sleep(observationMs);
    final int pings = serverPings();
    policy.stop(view).toCompletableFuture().join();

    // the nominal count is observationMs / PING_INTERVAL_MS - 1 = 7
    Assertions.assertTrue(
        pings >= 4 && pings <= 10,
        "One heartbeat must send about 7 pings, but the server got " + pings);
    Assertions.assertEquals(
        1,
        view.listener().events().stream().filter("onHeartbeatEvent:ACTIVATE"::equals).count(),
        "The connection must be activated once");
  }

  /**
   * Checks that the shutdown of a policy that has not been started completes normally.
   *
   * <p>The library stops the policy if the startup of a connection has failed, so the shutdown
   * without the startup is a regular case. The connection must not be touched then.
   */
  @Test
  void testStopWithoutStartCompletes() {
    final DefaultAvailabilityPolicy policy = policy(Duration.ofMillis(PING_INTERVAL_MS));
    final RecordingPoolEntryView view = newView(policy);

    final CompletableFuture<Void> closeFuture = policy.stop(view).toCompletableFuture();

    Assertions.assertDoesNotThrow(closeFuture::join);
    Assertions.assertTrue(view.calls().isEmpty(), "The connection must not be touched");
  }

  /**
   * Checks that the repeated shutdowns complete without errors and touch the connection once.
   *
   * <p>The first shutdown stops the heartbeat, the others must be harmless: all the futures are
   * completed successfully, and the connection is invalidated only once.
   */
  @Test
  void testRepeatedStopsCompleteWithoutErrors() {
    final DefaultAvailabilityPolicy policy = policy(Duration.ofMillis(PING_INTERVAL_MS));
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();

    final List<CompletableFuture<Void>> closeFutures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      closeFutures.add(policy.stop(view).toCompletableFuture());
    }

    for (final CompletableFuture<Void> closeFuture : closeFutures) {
      Assertions.assertDoesNotThrow(closeFuture::join);
    }
    Assertions.assertEquals(
        Arrays.asList(
            HeartbeatConstants.ACTIVE_STATE_NAME, HeartbeatConstants.INVALIDATE_STATE_NAME),
        view.stateChangeNames(),
        "The connection must be invalidated only once");
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
  }

  /**
   * Checks that the policy may be restarted with a new connection many times.
   *
   * <p>The library creates a new connection after a kill and starts the same policy again. Every
   * cycle must activate the connection and must run exactly one heartbeat. If a stopped heartbeat
   * kept running, the count of the pings of the later cycles would grow, so it is bounded from
   * above.
   */
  @Test
  void testRestartCyclesActivateEveryTime() throws Exception {
    final long cycleMs = 1500;
    final DefaultAvailabilityPolicy policy = policy(Duration.ofMillis(PING_INTERVAL_MS));

    for (int cycle = 1; cycle <= 3; cycle++) {
      execLua(tt1, "reset_call_counter()");
      final IProtoClient client = connectedClient();
      final RecordingPoolEntryView view = viewOf(policy, client);

      policy.start(view).toCompletableFuture().join();

      Assertions.assertSame(
          HeartbeatConstants.ACTIVE_STATE, view.state(), "The cycle " + cycle + " must activate");
      Assertions.assertEquals(
          Collections.singletonList("onHeartbeatEvent:ACTIVATE"),
          view.listener().events(),
          "The cycle " + cycle + " must report the activation once");

      Thread.sleep(cycleMs);
      policy.stop(view).toCompletableFuture().join();
      final int pings = serverPings();
      client.close();

      // the nominal count is cycleMs / PING_INTERVAL_MS - 1 = 5
      Assertions.assertTrue(
          pings >= 2 && pings <= 7,
          "The cycle " + cycle + " must send about 5 pings, but the server got " + pings);
      Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    }
  }

  /**
   * Checks that the shutdown completes in a bounded time while the node is hung.
   *
   * <p>The pipe is locked, so the ping that is in flight is never answered. The shutdown must not
   * wait for the answer or for the request timeout: the stage is completed at once, the connection
   * is invalidated, the shutdown is not reported as a kill. After the node comes back to life the
   * stopped policy must not send any ping.
   */
  @Test
  void testStopCompletesWhileNodeIsHung() throws Exception {
    final DefaultAvailabilityPolicy policy = policy(Duration.ofMillis(PING_INTERVAL_MS));
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();
    awaitServerPings(1);

    execLua(tt1, "lock_pipe(true)");
    // the next ping is sent into the locked pipe and stays unanswered
    Thread.sleep(PING_INTERVAL_MS + PING_INTERVAL_MS / 2);

    final CompletableFuture<Void> closeFuture = policy.stop(view).toCompletableFuture();

    Assertions.assertDoesNotThrow(() -> closeFuture.get(1, TimeUnit.SECONDS));
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The shutdown is not a kill");
    Assertions.assertEquals(
        0, view.listener().heartbeatEventCount(HeartbeatEvent.KILL), "The shutdown is not a kill");

    execLua(tt1, "lock_pipe(false)");
    Thread.sleep(PING_INTERVAL_MS);
    final int pingsAfterStop = serverPings();
    Thread.sleep(4 * PING_INTERVAL_MS);

    Assertions.assertEquals(
        pingsAfterStop, serverPings(), "The stopped policy must not ping the revived node");
  }

  /**
   * Checks that the shutdown of an invalidated connection completes while the node is hung.
   *
   * <p>The connection is already invalidated by the heartbeat, and the node keeps being hung, so
   * the heartbeat is in the state it would be killed from. The shutdown must complete at once, and
   * the connection must stay invalidated. The death threshold is huge, so the connection cannot be
   * killed before the shutdown.
   */
  @Test
  void testStopCompletesForInvalidatedConnectionWhileNodeIsHung() {
    final DefaultAvailabilityPolicy policy =
        policy(Duration.ofMillis(PING_INTERVAL_MS), Integer.MAX_VALUE);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();
    awaitServerPings(1);

    execLua(tt1, "lock_pipe(true)");
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(100))
        .until(() -> HeartbeatConstants.INVALIDATE_STATE.equals(view.state()));

    final CompletableFuture<Void> closeFuture =
        policy.stop(view, new IllegalStateException("the node is hung")).toCompletableFuture();

    Assertions.assertDoesNotThrow(() -> closeFuture.get(1, TimeUnit.SECONDS));
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, view.state());
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The shutdown is not a kill");
    Assertions.assertEquals(0, view.listener().heartbeatEventCount(HeartbeatEvent.KILL));
  }

  /*
   **********************************************************
   * State transitions on a real node
   **********************************************************
   */

  /**
   * Checks that the connection is activated again after the node comes back to life.
   *
   * <p>The pipe is locked, so the pings are lost and the connection is invalidated. After the pipe
   * is unlocked the pings are answered again, and the window must fill up with the successes: the
   * connection must be activated once more. The death threshold is huge, so the connection cannot
   * be killed on the way.
   */
  @Test
  void testConnectionRecoversAfterTheNodeComesBack() {
    final DefaultAvailabilityPolicy policy =
        policy(
            countingOpts()
                .withWindowSize(2)
                .withInvalidationThreshold(1)
                .withDeathThreshold(Integer.MAX_VALUE),
            null);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();

    execLua(tt1, "lock_pipe(true)");
    awaitState(view, HeartbeatConstants.INVALIDATE_STATE);

    execLua(tt1, "lock_pipe(false)");
    awaitState(view, HeartbeatConstants.ACTIVE_STATE);

    Assertions.assertEquals(
        Arrays.asList(
            "onHeartbeatEvent:ACTIVATE",
            "onHeartbeatEvent:INVALIDATE",
            "onHeartbeatEvent:ACTIVATE"),
        view.listener().events(),
        "The connection must be invalidated and activated once");
    Assertions.assertEquals(
        Arrays.asList(
            HeartbeatConstants.ACTIVE_STATE_NAME,
            HeartbeatConstants.INVALIDATE_STATE_NAME,
            HeartbeatConstants.ACTIVE_STATE_NAME),
        view.stateChangeNames());
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The recovered node is not killed");
  }

  /**
   * Checks that a node that stays hung is invalidated and then killed.
   *
   * <p>The pipe is locked and never unlocked while the heartbeat decides. The connection must be
   * invalidated first, then killed when the death threshold is exceeded, and the restart must be
   * requested exactly once. The killed connection must not ping anymore.
   */
  @Test
  void testHangingNodeIsInvalidatedAndThenKilled() throws Exception {
    final DefaultAvailabilityPolicy policy =
        policy(
            countingOpts().withWindowSize(1).withInvalidationThreshold(1).withDeathThreshold(2),
            null);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();

    execLua(tt1, "lock_pipe(true)");
    awaitState(view, HeartbeatConstants.KILL_STATE);

    Assertions.assertEquals(
        Arrays.asList(
            "onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:INVALIDATE", "onHeartbeatEvent:KILL"),
        view.listener().events());
    Assertions.assertEquals(1, view.reconnectCallsCount(), "The restart must be requested once");

    execLua(tt1, "lock_pipe(false)");
    Thread.sleep(PING_INTERVAL_MS);
    final int pingsAfterKill = serverPings();
    Thread.sleep(4 * PING_INTERVAL_MS);

    Assertions.assertEquals(
        pingsAfterKill, serverPings(), "The killed connection must not ping anymore");
    Assertions.assertEquals(1, view.reconnectCallsCount(), "The restart must not be repeated");
  }

  /**
   * Checks that a closed connection is killed at once.
   *
   * <p>The client is closed, so the next ping fails with a terminal error. The connection must be
   * killed without the invalidation step, even though the thresholds of the window are far from
   * being reached.
   */
  @Test
  void testClosedConnectionIsKilledImmediately() throws Exception {
    final DefaultAvailabilityPolicy policy =
        policy(countingOpts().withDeathThreshold(Integer.MAX_VALUE), null);
    final IProtoClient client = connectedClient();
    final RecordingPoolEntryView view = viewOf(policy, client);
    policy.start(view).toCompletableFuture().join();

    this.clients.remove(client);
    client.close();
    awaitState(view, HeartbeatConstants.KILL_STATE);

    Assertions.assertEquals(
        Arrays.asList("onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:KILL"),
        view.listener().events(),
        "A terminal error must kill the connection without the invalidation");
    Assertions.assertEquals(1, view.reconnectCallsCount(), "The restart must be requested once");
  }

  /**
   * Checks that the default ping function keeps a live connection active.
   *
   * <p>Every other test pings with {@link #COUNTING_PING}, so this one is the only one that covers
   * the ping function and the check of the answer that the library uses by default.
   */
  @Test
  void testDefaultPingFunctionKeepsTheConnectionActive() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final DefaultAvailabilityPolicy policy =
        policy(
            HeartbeatOpts.getDefault()
                .withPingInterval(PING_INTERVAL_MS)
                .withInitialDelay(Duration.ofMillis(PING_INTERVAL_MS)),
            registry);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();

    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(50))
        .until(() -> registry.counter(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).count() >= 3);

    Assertions.assertEquals(
        0, registry.counter(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).count(), "No failed pings");
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, view.state());
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), view.listener().events());
    Assertions.assertEquals(0, view.reconnectCallsCount());
  }

  /**
   * Checks that an error answer of the server is a failed ping, but not a dead connection.
   *
   * <p>The ping reaches the node and the node answers with an error. It is neither a timeout nor a
   * broken connection: the connection must be invalidated, the failures must be counted, and the
   * connection must not be killed, because the death threshold is huge and the error is not
   * terminal.
   */
  @Test
  void testServerErrorAnswerCountsAsFailedPing() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final DefaultAvailabilityPolicy policy =
        policy(
            countingOpts()
                .withPingFunction(
                    (client, opts) ->
                        client.eval("fail_by_box_error()", ValueFactory.emptyArray(), opts))
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(Integer.MAX_VALUE),
            registry);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();

    awaitState(view, HeartbeatConstants.INVALIDATE_STATE);
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(50))
        .until(() -> registry.counter(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).count() >= 3);

    Assertions.assertEquals(
        0, registry.counter(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).count(), "No answer is OK");
    Assertions.assertEquals(
        Arrays.asList("onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:INVALIDATE"),
        view.listener().events(),
        "The error answers must invalidate the connection, but not kill it");
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The error answer is not terminal");
  }

  /**
   * Checks that the heartbeats of the connections to different nodes are independent.
   *
   * <p>One policy serves both connections and uses one timer. The first node hangs: its connection
   * must be invalidated, while the connection to the second node must stay active and keep pinging.
   * The shutdown of the first connection must not affect the second one either.
   */
  @Test
  void testHeartbeatsOfDifferentNodesAreIndependent() {
    final DefaultAvailabilityPolicy policy =
        policy(
            countingOpts()
                .withWindowSize(1)
                .withInvalidationThreshold(1)
                .withDeathThreshold(Integer.MAX_VALUE),
            null);
    final RecordingPoolEntryView first = newView(policy);
    final RecordingPoolEntryView second = viewOf(policy, connectedClient(host2, port2), IDX + 1);
    policy.start(first).toCompletableFuture().join();
    policy.start(second).toCompletableFuture().join();
    awaitServerPings(tt1, 1);
    awaitServerPings(tt2, 1);

    execLua(tt1, "lock_pipe(true)");
    awaitState(first, HeartbeatConstants.INVALIDATE_STATE);

    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, second.state());
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"),
        second.listener().events(),
        "The healthy node must not get any events");
    final int secondPings = serverPings(tt2);
    awaitServerPings(tt2, secondPings + 2);

    policy.stop(first).toCompletableFuture().join();

    final int pingsAfterStop = serverPings(tt2);
    awaitServerPings(tt2, pingsAfterStop + 2);
    Assertions.assertSame(HeartbeatConstants.INVALIDATE_STATE, first.state());
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, second.state());
    Assertions.assertEquals(
        Collections.singletonList("onHeartbeatEvent:ACTIVATE"), second.listener().events());
    Assertions.assertEquals(0, second.reconnectCallsCount());
  }

  /**
   * Checks that the metrics agree with what the server has seen.
   *
   * <p>Every ping is counted by the server when it arrives and by the policy when the answer is
   * handled, so after the shutdown the counts may differ only by the ping that was in flight at
   * that moment. No time measurement may stay active.
   */
  @Test
  void testMetricsAreConsistentWithTheServer() throws Exception {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final DefaultAvailabilityPolicy policy = policy(countingOpts(), registry);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();
    awaitServerPings(5);

    policy.stop(view).toCompletableFuture().join();

    // the answer of the ping that was in flight at the shutdown arrives later and is ignored
    Thread.sleep(2 * PING_INTERVAL_MS);
    final double successes = registry.counter(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS).count();
    final int pings = serverPings();
    Assertions.assertTrue(successes >= 4, "At least four pings must be counted: " + successes);
    Assertions.assertTrue(
        pings - successes >= 0 && pings - successes <= 1,
        "The metrics must agree with the server: " + successes + " vs " + pings);
    Assertions.assertEquals(
        0, registry.counter(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).count(), "No failed pings");
    Assertions.assertEquals(0, activeMeasurements(registry), "No measurement must stay active");
  }

  /**
   * Checks that the time measurement of a hung ping is finished after the shutdown.
   *
   * <p>The ping is sent into the locked pipe and is never answered, so its measurement is active.
   * The shutdown does not wait for the answer, but the measurement must be finished by the request
   * timeout of the ping: otherwise the metric would show a ping that lasts forever.
   */
  @Test
  void testMeasurementOfHangingPingIsFinishedAfterStop() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final DefaultAvailabilityPolicy policy =
        policy(countingOpts().withDeathThreshold(Integer.MAX_VALUE), registry);
    final RecordingPoolEntryView view = newView(policy);
    policy.start(view).toCompletableFuture().join();
    awaitServerPings(2);

    execLua(tt1, "lock_pipe(true)");
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(20))
        .until(() -> activeMeasurements(registry) == 1);

    policy.stop(view).toCompletableFuture().join();

    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(50))
        .until(() -> activeMeasurements(registry) == 0);
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The shutdown is not a kill");
  }

  /**
   * Checks that the late answer of a stopped machine does not affect the restarted connection.
   *
   * <p>The library restarts the policy on the same view, while the ping of the old machine may
   * still be in flight. The ping is sent into the locked pipe, so it is lost and ends by the
   * request timeout, which is much later than the restart. The old machine is stopped by then, so
   * the failure must be ignored by it: it must neither be counted, nor change the state of the view
   * that already belongs to the new machine, nor kill the new connection.
   *
   * <p>The timeline (the ping interval, which is also the request timeout, is 2 seconds):
   *
   * <ol>
   *   <li>the pipe is locked before the startup, so the first ping of the old machine is lost;
   *   <li>the pipe is unlocked, and the old machine is replaced with the new one at once;
   *   <li>the old ping fails by the timeout while the new machine is running: between its first and
   *       its second pings.
   * </ol>
   */
  @Test
  void testLateAnswerOfStoppedMachineDoesNotAffectRestartedConnection() throws Exception {
    final long pingIntervalMs = 2000;
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final DefaultAvailabilityPolicy policy =
        policy(
            countingOpts().withPingInterval(pingIntervalMs).withDeathThreshold(Integer.MAX_VALUE),
            registry);
    // the connection is established before the pipe is locked, otherwise the authorization is lost
    final RecordingPoolEntryView view = newView(policy);

    execLua(tt1, "lock_pipe(true)");
    policy.start(view).toCompletableFuture().join();
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(20))
        .until(() -> activeMeasurements(registry) == 1);
    // the ping was sent not later than now, so its timeout cannot be later than this moment
    final long lateAnswerDeadline =
        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(pingIntervalMs + PING_INTERVAL_MS);

    execLua(tt1, "lock_pipe(false)");
    policy.stop(view).toCompletableFuture().join();
    policy.start(view).toCompletableFuture().join();

    // the new machine works: its first and second pings reach the node, the second one is sent
    // after the old ping has failed
    awaitServerPings(2);
    final long remainingMs = TimeUnit.NANOSECONDS.toMillis(lateAnswerDeadline - System.nanoTime());
    if (remainingMs > 0) {
      Thread.sleep(remainingMs);
    }

    Assertions.assertEquals(
        Arrays.asList("onHeartbeatEvent:ACTIVATE", "onHeartbeatEvent:ACTIVATE"),
        view.listener().events(),
        "Every machine activates the connection once, and nothing else is reported");
    Assertions.assertEquals(
        Arrays.asList(
            HeartbeatConstants.ACTIVE_STATE_NAME,
            HeartbeatConstants.INVALIDATE_STATE_NAME,
            HeartbeatConstants.ACTIVE_STATE_NAME),
        view.stateChangeNames(),
        "The late answer must not change the state of the view");
    Assertions.assertSame(HeartbeatConstants.ACTIVE_STATE, view.state());
    Assertions.assertEquals(0, view.reconnectCallsCount(), "The new connection is not killed");
    Assertions.assertEquals(
        0, view.listener().heartbeatEventCount(HeartbeatEvent.KILL), "The kill must not happen");
    Assertions.assertEquals(
        0,
        registry.counter(HeartbeatConstants.POOL_HEARTBEAT_ERRORS).count(),
        "The failure of the old ping must be ignored by the stopped machine");

    policy.stop(view).toCompletableFuture().join();
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(50))
        .until(() -> activeMeasurements(registry) == 0);
  }

  /*
   **********************************************************
   * Helpers
   **********************************************************
   */

  /** Creates a configured policy that pings with {@link #COUNTING_PING}. */
  private static DefaultAvailabilityPolicy policy(Duration initialDelay) {
    return policy(initialDelay, HeartbeatOpts.DEFAULT_DEATH_THRESHOLD);
  }

  /** Creates a configured policy that pings with {@link #COUNTING_PING}. */
  private static DefaultAvailabilityPolicy policy(Duration initialDelay, int deathThreshold) {
    final DefaultAvailabilityPolicy policy =
        new DefaultAvailabilityPolicy(
            HeartbeatOpts.getDefault()
                .withPingInterval(PING_INTERVAL_MS)
                .withInitialDelay(initialDelay)
                .withDeathThreshold(deathThreshold)
                .withPingFunction(COUNTING_PING));
    policy.configure(TIMER, null);
    return policy;
  }

  /** Creates a view over a new authorized connection to the first node. */
  private RecordingPoolEntryView newView(DefaultAvailabilityPolicy policy) {
    return viewOf(policy, connectedClient());
  }

  /** Creates a view over the given client. The policy is stopped for the view after the test. */
  private RecordingPoolEntryView viewOf(DefaultAvailabilityPolicy policy, IProtoClient client) {
    return viewOf(policy, client, IDX);
  }

  /** Creates a view with the given index over the given client. */
  private RecordingPoolEntryView viewOf(
      DefaultAvailabilityPolicy policy, IProtoClient client, int index) {
    final RecordingPoolEntryView view = RecordingPoolEntryView.create(TAG, index, policy, client);
    this.cleanups.add(() -> policy.stop(view).toCompletableFuture().join());
    return view;
  }

  /** Creates a configured policy with the given options. */
  private static DefaultAvailabilityPolicy policy(HeartbeatOpts opts, MeterRegistry registry) {
    final DefaultAvailabilityPolicy policy = new DefaultAvailabilityPolicy(opts);
    policy.configure(TIMER, registry);
    return policy;
  }

  /** The options that ping with {@link #COUNTING_PING} every {@link #PING_INTERVAL_MS}. */
  private static HeartbeatOpts countingOpts() {
    return HeartbeatOpts.getDefault()
        .withPingInterval(PING_INTERVAL_MS)
        .withInitialDelay(Duration.ofMillis(PING_INTERVAL_MS))
        .withPingFunction(COUNTING_PING);
  }

  /** Returns the count of the heartbeat time measurements that are in progress. */
  private static int activeMeasurements(MeterRegistry registry) {
    return registry.get(HeartbeatConstants.POOL_HEARTBEAT_TIME).longTaskTimer().activeTasks();
  }

  /** Waits until the connection gets the given state. */
  private static void awaitState(RecordingPoolEntryView view, Object expected) {
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(50))
        .until(() -> expected.equals(view.state()));
  }

  /** Creates a connected and authorized client. The client is closed after the test. */
  private IProtoClient connectedClient() {
    return connectedClient(host1, port1);
  }

  /** Creates a connected and authorized client of the given node. */
  private IProtoClient connectedClient(String host, int port) {
    final IProtoClient client =
        new IProtoClientImpl(new ConnectionFactory(bootstrap, TIMER), TIMER);
    client.connect(new InetSocketAddress(host, port), Duration.ofSeconds(10).toMillis()).join();
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    this.clients.add(client);
    return client;
  }

  /** Returns the count of the pings that have reached the first node since the last reset. */
  private int serverPings() {
    return serverPings(tt1);
  }

  /** Returns the count of the pings that have reached the node since the last reset. */
  private int serverPings(TarantoolContainer<?> node) {
    try {
      final List<?> result =
          TarantoolContainerClientHelper.executeCommandDecoded(node, "return get_call_counter()");
      return ((Number) result.get(0)).intValue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Waits until the first node has received at least the given count of the pings. */
  private void awaitServerPings(int expected) {
    awaitServerPings(tt1, expected);
  }

  /** Waits until the node has received at least the given count of the pings. */
  private void awaitServerPings(TarantoolContainer<?> node, int expected) {
    Awaitility.await()
        .atMost(DEFAULT_AWAIT_TIMEOUT)
        .pollInterval(Duration.ofMillis(100))
        .until(() -> serverPings(node) >= expected);
  }
}
