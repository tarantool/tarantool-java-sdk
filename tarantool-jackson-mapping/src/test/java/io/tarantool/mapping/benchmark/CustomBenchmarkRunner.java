/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.benchmark;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.msgpack.value.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.mapping.DatetimeExtensionModule;
import io.tarantool.mapping.TarantoolJacksonMapping;

public class CustomBenchmarkRunner {

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
  protected static final Timer timerService = new HashedWheelTimer();
  protected static final ConnectionFactory factory = new ConnectionFactory(bootstrap, timerService);
  static final Logger log = LoggerFactory.getLogger(CustomBenchmarkRunner.class);
  public static final byte[] STUB_ARGS =
      TarantoolJacksonMapping.toValue(Arrays.asList("1", "2", 3));
  private static long lastRps = 0;
  private static int clientIdx = 0;
  private static final String host;
  private static final int port;
  private static List<IProtoClient> clients;

  static {
    Map<String, String> env = System.getenv();
    host = env.getOrDefault("TARANTOOL_HOST", "localhost");
    port = Integer.parseInt(env.getOrDefault("TARANTOOL_PORT", "3301"));
  }

  private static void createClients(Integer connections)
      throws ExecutionException, InterruptedException {
    log.debug("Attempting connect to Tarantool");

    clients = new ArrayList<>();
    for (int i = 0; i < connections; i++) {
      IProtoClient client = new IProtoClientImpl(factory, factory.getTimerService());
      client
          .connect(new InetSocketAddress(host, port), 3_000)
          .get(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
      log.debug(
          "Successfully connected to Tarantool, version = {}",
          client.eval("return _TARANTOOL", ValueFactory.emptyArray()).get());
      clients.add(client);
    }
  }

  public static void main(String[] args) throws Exception {
    List<Integer> nanosBetweenSending = new ArrayList<>();
    for (int rps = 50_000; rps <= 1_000_000; rps += 10_000) {
      nanosBetweenSending.add(getDelay(rps));
    }

    List<Integer> connectionsAmount = Arrays.asList(1, 2, 4, 8, 16);

    int durationInSeconds = 60;
    List<BiFunction<Integer, Integer, Boolean>> tests =
        Collections.singletonList(
            (connections, nanos) ->
                simpleCallAndJacksonMapping(connections, nanos, durationInSeconds));

    for (BiFunction<Integer, Integer, Boolean> test : tests) {
      for (Integer connections : connectionsAmount) {
        createClients(connections);
        for (Integer nanos : nanosBetweenSending) {
          Boolean errorHappened = test.apply(connections, nanos);
          if (errorHappened) {
            break;
          }
        }
        closeClients();
        System.gc();
        Thread.sleep(durationInSeconds);
      }
    }
  }

  private static int getDelay(int rps) {
    return (int) (DatetimeExtensionModule.NANOS_PER_SECOND / rps);
  }

  private static void closeClients() {
    clients.forEach(
        (c) -> {
          try {
            c.close();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    clients.clear();
  }

  public static Boolean simpleCallAndJacksonMapping(
      int connections, Integer delay, Integer duration) {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    return runRequestsWithDelay(
        methodName,
        connections,
        delay,
        duration,
        () ->
            getClient(connections)
                .eval(
                    "return return_one_tuple()",
                    STUB_ARGS,
                    null,
                    IProtoRequestOpts.empty().withRequestTimeout(2_000))
                .thenApply(TarantoolJacksonMapping::readResponse));
  }

  private static IProtoClient getClient(int connections) {
    if (clientIdx == connections) {
      clientIdx = 0;
    }
    return clients.get(clientIdx++);
  }

  private static boolean runRequestsWithDelay(
      String methodName,
      int connections,
      Integer delay,
      Integer duration,
      Supplier<CompletableFuture> supplier) {

    AtomicInteger counter = new AtomicInteger();
    AtomicBoolean errorHappened = new AtomicBoolean(false);

    long start = Instant.now().getEpochSecond();
    Thread thread = startClientThread(delay, supplier, counter, errorHappened);

    return controlLoop(
        connections, delay, duration, methodName, counter, errorHappened, start, thread);
  }

  private static Thread startClientThread(
      Integer delay,
      Supplier<CompletableFuture> supplier,
      AtomicInteger counter,
      AtomicBoolean errorHappened) {
    Thread thread =
        new Thread(
            () -> {
              while (true) {
                if (errorHappened.get()) {
                  break;
                }
                long dstart = System.nanoTime();
                supplier
                    .get()
                    .whenComplete(
                        (r, ex) -> {
                          if (ex != null) {
                            log.info(ex.toString());
                            errorHappened.set(true);
                          }
                          counter.getAndIncrement();
                        });
                if (Thread.currentThread().isInterrupted()) {
                  break;
                }
                waitDelay(dstart, delay);
              }
            });
    thread.setName("client_thread");
    thread.start();
    return thread;
  }

  private static boolean controlLoop(
      int connections,
      Integer delay,
      Integer duration,
      String methodName,
      AtomicInteger counter,
      AtomicBoolean errorHappened,
      long start,
      Thread thread) {
    while (true) {
      if (errorHappened.get()) {
        log.info(
            "error - {}(connections = {}, delay = {}, duration = {})",
            methodName,
            connections,
            delay,
            duration);
        thread.interrupt();
        lastRps = 0;
        return true;
      }
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      long gap = Instant.now().getEpochSecond() - start;
      if (gap != 0) {
        long rps = counter.get() / gap;
        log.debug("{}, intermediate RPS: {}", methodName, rps);
        if (errorHappened.get() || counter.get() == 0) {
          log.info(
              "error - {}(connections = {}, delay = {}, duration = {})",
              methodName,
              connections,
              delay,
              duration);
          thread.interrupt();
          lastRps = 0;
          return true;
        }
        if (gap > duration) {
          log.info(
              "success - {}(connections = {}, delay = {}, duration = {})" + ", RPS: {}",
              methodName,
              connections,
              delay,
              duration,
              rps);
          thread.interrupt();
          if (lastRps > rps) {
            lastRps = 0;
            return true;
          }
          lastRps = rps;
          return false;
        }
      }
    }
  }

  private static void waitDelay(Long dstart, Integer delay) {
    while (dstart + delay >= System.nanoTime())
      ;
  }
}
