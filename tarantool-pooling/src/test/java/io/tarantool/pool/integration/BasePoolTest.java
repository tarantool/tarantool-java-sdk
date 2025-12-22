/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.opentest4j.AssertionFailedError;
import org.testcontainers.containers.TarantoolContainer;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;

public class BasePoolTest {

  protected static final String API_USER = "api_user";

  protected static final Map<String, String> CREDS =
      new HashMap<String, String>() {
        {
          put(API_USER, "secret");
        }
      };

  protected static final Map<String, String> ENV_MAP =
      new HashMap<String, String>() {
        {
          put("TARANTOOL_USER_NAME", API_USER);
          put("TARANTOOL_USER_PASSWORD", CREDS.get(API_USER));
        }
      };

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

  protected static final int MAX_CONNECTION_COUNT = 20;
  protected static final int MIN_CONNECTION_COUNT = 10;

  protected static String host1;
  protected static int port1;
  protected static int count1;
  protected static String host2;
  protected static int port2;
  protected static int count2;

  protected static void generateCounts() {
    count1 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
    count2 = ThreadLocalRandom.current().nextInt(MIN_CONNECTION_COUNT, MAX_CONNECTION_COUNT + 1);
  }

  protected void execLua(TarantoolContainer container, String command) {
    try {
      container.executeCommandDecoded(command);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected int getActiveConnectionsCount(TarantoolContainer tt) {
    try {
      List<? extends Object> result =
          tt.executeCommandDecoded("return box.stat.net().CONNECTIONS.current");
      return (Integer) result.get(0) - 1;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected MeterRegistry createMetricsRegistry() {
    MeterRegistry metricsRegistry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);
    LongTaskTimer.builder("request.timer")
        .description("Latency of requests to Tarantool")
        .register(metricsRegistry);
    Counter.builder("request.counter")
        .description("Number of requests to Tarantool")
        .register(metricsRegistry);
    Counter.builder("response.success")
        .description("Number of successful responses")
        .register(metricsRegistry);
    Counter.builder("response.errors")
        .description("Number of error responses")
        .register(metricsRegistry);
    Counter.builder("response.ignored")
        .description("Number of ignored IProto packets")
        .register(metricsRegistry);
    return metricsRegistry;
  }

  protected IProtoClientPool createPool(HeartbeatOpts heartbeatOpts) {
    return createPool(heartbeatOpts, null);
  }

  protected IProtoClientPool createPool(
      HeartbeatOpts heartbeatOpts, MeterRegistry metricsRegistry) {
    ManagedResource<Timer> timerResource =
        ManagedResource.owned(new HashedWheelTimer(), Timer::stop);
    ConnectionFactory factory = new ConnectionFactory(bootstrap, timerResource.get());
    IProtoClientPool pool =
        new IProtoClientPoolImpl(
            factory, timerResource, true, heartbeatOpts, null, metricsRegistry);
    pool.setGroups(
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost(host1)
                .withPort(port1)
                .withSize(count1)
                .withTag("node-a")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost(host2)
                .withPort(port2)
                .withSize(count2)
                .withTag("node-b")
                .build()));
    return pool;
  }

  @SuppressWarnings("rawtypes")
  protected List<IProtoClient> getConnects(IProtoClientPool pool, String tag, int count) {
    List<IProtoClient> clients = new ArrayList<>();
    List<CompletableFuture<IProtoClient>> futures = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      futures.add(pool.get(tag, i));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return clients;
  }

  @SuppressWarnings("rawtypes")
  protected boolean pingClients(List<IProtoClient> clients) {
    List<CompletableFuture<?>> futures = new ArrayList<>();
    for (IProtoClient client : clients) {
      assertTrue(client.isConnected());
      client.authorize(API_USER, CREDS.get(API_USER)).join();
      futures.add(client.ping());
    }
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    } catch (CompletionException e) {
      return false;
    }
    return true;
  }

  protected void waitFor(String failMessage, Duration acceptableDuration, Runnable cb)
      throws Exception {
    long attempts = acceptableDuration.compareTo(Duration.ofSeconds(3)) > 0 ? 100L : 10L;
    float k = (float) acceptableDuration.toMillis() / attempts / attempts;
    for (int i = 1; i <= attempts; i++) {
      try {
        cb.run();
        return;
      } catch (AssertionFailedError | Exception e) {
        long timeout = (long) (k * (2L * i - 1));
        Thread.sleep(timeout);
      }
    }
    fail(failMessage);
  }
}
