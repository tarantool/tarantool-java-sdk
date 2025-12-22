/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.benchmark;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.client.BaseOptions;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.protocol.TransactionIsolationLevel;
import io.tarantool.mapping.DatetimeExtensionModule;
import io.tarantool.mapping.Interval;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.InstanceConnectionGroup;

public class CustomBenchmarkRunner {

  public static final String ECHO_EXPRESSION = "return ...";
  public static final String YEAR = "2022";
  public static final String MONTH = "07";
  public static final String DAY = "01";
  public static final String HOUR = "08";
  public static final String MINUTES = "30";
  public static final String SECONDS = "05";
  public static final String NANOSECONDS = "000000123";
  public static final String T_2022_08_30_05 =
      String.format("%s-%s-%sT%s:%s:%s", YEAR, MONTH, DAY, HOUR, MINUTES, SECONDS);
  public static final String T_2022_08_30_05_000000123 =
      String.format("%s.%s", T_2022_08_30_05, NANOSECONDS);
  public static final LocalDateTime LOCAL_DATE_TIME =
      LocalDateTime.parse(T_2022_08_30_05_000000123);
  public static final long EPOCH_SECOND = LOCAL_DATE_TIME.toEpochSecond(ZoneOffset.UTC);
  public static final List<Object> ECHO_ARGS =
      Arrays.asList(
          "string",
          Long.MAX_VALUE,
          Integer.MAX_VALUE,
          Short.MAX_VALUE,
          Double.MAX_VALUE,
          Float.MAX_VALUE,
          true,
          false,
          null,
          new Interval().setYear(1).setMonth(200).setDay(-77),
          new ArrayList<>(
              Arrays.asList(
                  1,
                  "a",
                  null,
                  true,
                  new HashMap<Object, Object>() {
                    {
                      put("nestedArray", new ArrayList<>(Arrays.asList(1, "a", null, true)));
                    }
                  })),
          new HashMap<Object, Object>() {
            {
              put("a", 1);
              put("b", "3");
              put("99", true);
              put(
                  "nestedArray",
                  new ArrayList<>(
                      Arrays.asList(
                          1,
                          "a",
                          null,
                          true,
                          new HashMap<Object, Object>() {
                            {
                              put("hello", "world");
                            }
                          })));
            }
          },
          new BigDecimal("9223372036854775808"),
          UUID.randomUUID(),
          Instant.ofEpochSecond(EPOCH_SECOND));
  static final Logger log = LoggerFactory.getLogger(CustomBenchmarkRunner.class);
  private static final String host;
  private static final int port;
  private static long lastRps = 0;
  private static final AtomicInteger counter = new AtomicInteger(0);

  static {
    Map<String, String> env = System.getenv();
    host = env.getOrDefault("TARANTOOL_HOST", "localhost");
    port = Integer.parseInt(env.getOrDefault("TARANTOOL_PORT", "3301"));
  }

  private static TarantoolBoxClient createClient(Integer connections) throws Exception {
    log.debug("Attempting connect to Tarantool");

    return TarantoolFactory.box()
        .withGroups(
            Collections.singletonList(
                InstanceConnectionGroup.builder()
                    .withHost(host)
                    .withPort(port)
                    .withSize(connections)
                    .withTag("default")
                    .build()))
        .build();
  }

  public static void main(String[] args) throws Exception {
    List<Integer> nanosBetweenSending = new ArrayList<>();
    for (int rps = 5_000; rps <= 1_000_000; rps += 2_000) {
      nanosBetweenSending.add(getDelay(rps));
    }

    List<Integer> connectionsAmount = Arrays.asList(1, 2, 4);

    int durationInSeconds = 20;
    List<BiFunction<TarantoolBoxClient, Integer, Boolean>> tests =
        Arrays.asList(
            (client, nanos) -> beginReplaceCommit(client, nanos, durationInSeconds),
            (client, nanos) -> beginThreeReplaceCommit(client, nanos, durationInSeconds),
            (client, nanos) -> stubBeginReplaceStubCommit(client, nanos, durationInSeconds),
            (client, nanos) -> replace(client, nanos, durationInSeconds),
            (client, nanos) -> echoEval(client, nanos, durationInSeconds));

    for (BiFunction<TarantoolBoxClient, Integer, Boolean> test : tests) {
      for (Integer connections : connectionsAmount) {
        TarantoolBoxClient client = createClient(connections);
        for (Integer nanos : nanosBetweenSending) {
          Boolean errorHappened = test.apply(client, nanos);
          if (errorHappened) {
            break;
          }
        }
        client.close();
        System.gc();
        Thread.sleep(durationInSeconds);
      }
    }
  }

  private static int getDelay(int rps) {
    return (int) (DatetimeExtensionModule.NANOS_PER_SECOND / rps);
  }

  public static Boolean echoEval(TarantoolBoxClient client, Integer delay, Integer duration) {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    log.info("Start {} test", methodName);
    return runRequestsWithDelay(
        methodName, client, delay, duration, () -> client.eval(ECHO_EXPRESSION, ECHO_ARGS));
  }

  public static Boolean beginReplaceCommit(
      TarantoolBoxClient client, Integer delay, Integer duration) {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    IProtoClientPool pool = client.getPool();
    int connections = pool.getGroupSize(pool.getTags().get(0));
    if (connections > 1) {
      log.info("Can't start {}, connections={} more then 1", methodName, connections);
    } else {
      log.info("Start {} test", methodName);
    }

    IProtoClient iprotoClient = pool.get("default", 0).join();
    return runRequestsWithDelay(
        methodName,
        client,
        delay,
        duration,
        () -> {
          long streamId = iprotoClient.allocateStreamId();
          return iprotoClient
              .begin(streamId, 2_000, TransactionIsolationLevel.DEFAULT)
              .thenCompose(
                  r ->
                      client
                          .space("tmp")
                          .replace(
                              Arrays.asList(counter.getAndIncrement(), "artyom"),
                              BaseOptions.builder().withStreamId(streamId).build()))
              .thenCompose(r -> iprotoClient.commit(streamId));
        });
  }

  public static Boolean beginThreeReplaceCommit(
      TarantoolBoxClient client, Integer delay, Integer duration) {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    IProtoClientPool pool = client.getPool();
    int connections = pool.getGroupSize(pool.getTags().get(0));
    if (connections > 1) {
      log.info("Can't start {}, connections={} more then 1", methodName, connections);
    } else {
      log.info("Start {} test", methodName);
    }

    IProtoClient iprotoClient = pool.get("default", 0).join();
    return runRequestsWithDelay(
        methodName,
        client,
        delay,
        duration,
        () -> {
          long streamId = iprotoClient.allocateStreamId();
          return iprotoClient
              .begin(streamId, 2_000, TransactionIsolationLevel.DEFAULT)
              .thenCompose(
                  r ->
                      client
                          .space("tmp")
                          .replace(
                              Arrays.asList(counter.getAndIncrement(), "artyom"),
                              BaseOptions.builder().withStreamId(streamId).build()))
              .thenCompose(
                  r ->
                      client
                          .space("tmp")
                          .replace(
                              Arrays.asList(counter.getAndIncrement(), "artyom"),
                              BaseOptions.builder().withStreamId(streamId).build()))
              .thenCompose(
                  r ->
                      client
                          .space("tmp")
                          .replace(
                              Arrays.asList(counter.getAndIncrement(), "artyom"),
                              BaseOptions.builder().withStreamId(streamId).build()))
              .thenCompose(r -> iprotoClient.commit(streamId));
        });
  }

  public static Boolean stubBeginReplaceStubCommit(
      TarantoolBoxClient client, Integer delay, Integer duration) {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    IProtoClientPool pool = client.getPool();
    int connections = pool.getGroupSize(pool.getTags().get(0));
    if (connections > 1) {
      log.info("Can't start {}, connections={} more then 1", methodName, connections);
    } else {
      log.info("Start {} test", methodName);
    }

    return runRequestsWithDelay(
        methodName,
        client,
        delay,
        duration,
        () ->
            client
                .eval(ECHO_EXPRESSION, Collections.singletonList(123L))
                .thenCompose(
                    r ->
                        client
                            .space("tmp")
                            .replace(Arrays.asList(counter.getAndIncrement(), "artyom")))
                .thenCompose(r -> client.eval(ECHO_EXPRESSION, Collections.singletonList(124L))));
  }

  public static Boolean replace(TarantoolBoxClient client, Integer delay, Integer duration) {
    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    log.info("Start {} test", methodName);

    return runRequestsWithDelay(
        methodName,
        client,
        delay,
        duration,
        () -> client.space("tmp").replace(Arrays.asList(counter.getAndIncrement(), "artyom")));
  }

  private static boolean runRequestsWithDelay(
      String methodName,
      TarantoolBoxClient client,
      Integer delay,
      Integer duration,
      Supplier<CompletableFuture> supplier) {

    AtomicInteger counter = new AtomicInteger();
    AtomicBoolean errorHappened = new AtomicBoolean(false);

    long start = Instant.now().getEpochSecond();
    Thread thread = startClientThread(delay, supplier, counter, errorHappened);

    return controlLoop(client, delay, duration, methodName, counter, errorHappened, start, thread);
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
      TarantoolBoxClient client,
      Integer delay,
      Integer duration,
      String methodName,
      AtomicInteger counter,
      AtomicBoolean errorHappened,
      long start,
      Thread thread) {
    IProtoClientPool pool = client.getPool();
    int connections = pool.getGroupSize(pool.getTags().get(0));
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
