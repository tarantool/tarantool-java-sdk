/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.tarantool.client.BaseOptions;
import io.tarantool.client.TarantoolClient;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.box.TarantoolBoxSpace;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoFeature;
import io.tarantool.core.connection.exceptions.ConnectionClosedException;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.pool.exceptions.PoolClosedException;

@Timeout(value = 5)
@Testcontainers
public class TarantoolClientTest extends BaseTest {

  @Container
  private static final TarantoolContainer tt = new TarantoolContainer()
      .withEnv(ENV_MAP);
  private static TarantoolClient client;
  private static char tarantoolVersion;
  private static Integer serverVersion;

  @BeforeAll
  public static void setUp() throws Exception {
    client = getClientAndConnect();
    client.getPool().forEach(c -> c.authorize(API_USER, CREDS.get(API_USER)).join());
    serverVersion = client.getPool().get("default", 0).join().getServerProtocolVersion();
  }

  private static TarantoolClient getClientAndConnect() throws Exception {
    return TarantoolFactory.box()
        .withUser(API_USER)
        .withPassword(CREDS.get(API_USER))
        .withHost(tt.getHost())
        .withPort(tt.getPort())
        .build();
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    tt.executeCommand("return box.space.test:truncate()");
    tt.executeCommand("return box.space.space_a:truncate()");
    tt.executeCommand("return box.space.space_b:truncate()");
    tt.executeCommand("return box.space.person:truncate()");

    client = getClientAndConnect();
    tarantoolVersion = System.getenv("TARANTOOL_VERSION").charAt(0);
  }

  @Test
  public void testCall() {
    Object output = client.call("return_number").join().get();
    assertEquals(Collections.singletonList(2008), output);

    // with expectedClass
    output = client.call("return_number", Integer.class).join().get();
    assertEquals(Collections.singletonList(2008), output);

    output = client.call("return_number", Long.class).join().get();
    assertEquals(Collections.singletonList(2008L), output);

    // with expected TypeReference
    output = client.call("return_number", new TypeReference<List<Integer>>() {
    }).join().get();
    assertEquals(Collections.singletonList(2008), output);

    output = client.call("return_number", new TypeReference<List<Long>>() {
    }).join().get();
    assertEquals(Collections.singletonList(2008L), output);
  }

  @Test
  public void testCallWithArgs() {
    List<?> input;
    Object output = client.call("echo_with_wrapping", Person.class).join().get();
    assertEquals(Collections.singletonList(new Person(null, null, null)), output);

    input = Arrays.asList(1, true, "IvanB");
    output = client.call("echo_with_wrapping", input).join().get();
    assertEquals(Collections.singletonList(input), output);

    // with expectedClass
    output = client.call("echo_with_wrapping", input, Person.class).join().get();
    assertEquals(Collections.singletonList(new Person(1, true, "IvanB")), output);

    // with expected TypeReference
    output = client.call("echo_with_wrapping", input, new TypeReference<List<Person>>() {
    }).join().get();
    assertEquals(Collections.singletonList(new Person(1, true, "IvanB")), output);

    output = client.call("echo", input, new TypeReference<Person>() {
    }).join().get();
    assertEquals(new Person(1, true, "IvanB"), output);
  }

  @Test
  public void testCallWithArgsAndOpts() {
    List<?> input = Arrays.asList(2, null, "IvanD");
    Object output = client.call(
        "echo_with_wrapping",
        input,
        BaseOptions.builder().withTimeout(100).build()
    ).join().get();
    assertEquals(Collections.singletonList(input), output);

    // with expectedClass
    output = client.call(
        "echo_with_wrapping",
        input,
        BaseOptions.builder().withTimeout(100).build(),
        Person.class
    ).join().get();
    assertEquals(Collections.singletonList(new Person(2, null, "IvanD")), output);

    // with expected TypeReference
    output = client.call(
        "echo_with_wrapping",
        input,
        null, BaseOptions.builder().withTimeout(100).build(),
        new TypeReference<List<Person>>() {}
    ).join().get();
    assertEquals(Collections.singletonList(new Person(2, null, "IvanD")), output);

    output = client.call(
        "echo",
        input,
        null, BaseOptions.builder().withTimeout(100).build(),
        new TypeReference<Person>() {}
    ).join().get();
    assertEquals(new Person(2, null, "IvanD"), output);
  }

  @Test
  public void testEval() {
    Object output = client.eval("return 2008").join().get();
    assertEquals(Collections.singletonList(2008), output);

    // with expectedClass
    output = client.eval("return 2008", Integer.class).join().get();
    assertEquals(Collections.singletonList(2008), output);

    output = client.eval("return 2008", Long.class).join().get();
    assertEquals(Collections.singletonList(2008L), output);

    // with expected TypeReference
    output = client.eval("return 2008", new TypeReference<List<Integer>>() {
    }).join().get();
    assertEquals(Collections.singletonList(2008), output);

    output = client.eval("return 2008", new TypeReference<List<Long>>() {
    }).join().get();
    assertEquals(Collections.singletonList(2008L), output);
  }

  @Test
  public void testEvalWithArgs() {
    List<?> input = Arrays.asList(1, true, "IvanB");
    // we use table packing because it's more popular than multi return value
    Object output = client.eval("return {...}", input).join().get();
    assertEquals(Collections.singletonList(input), output);

    // with expectedClass
    output = client.eval("return {...}", input, Person.class).join().get();
    assertEquals(Collections.singletonList(new Person(1, true, "IvanB")), output);

    // with expected TypeReference
    output = client.eval("return {...}", input, new TypeReference<List<Person>>() {
    }).join().get();
    assertEquals(Collections.singletonList(new Person(1, true, "IvanB")), output);

    output = client.eval("return ...", input, new TypeReference<Person>() {
    }).join().get();
    assertEquals(new Person(1, true, "IvanB"), output);

    output = client.eval("return ...", input, new TypeReference<List<?>>() {
    }).join().get();
    assertEquals(input, output);
  }

  @Test
  public void testEvalWithArgsAndOpts() {
    List<?> input = Arrays.asList(2, null, "IvanD");
    Object output = client.eval("return {...}", input, BaseOptions.builder().withTimeout(100).build()).join().get();
    assertEquals(Collections.singletonList(input), output);

    // with expectedClass
    output = client.eval("return {...}", input, BaseOptions.builder().withTimeout(100).build(), Person.class)
        .join().get();
    assertEquals(Collections.singletonList(new Person(2, null, "IvanD")), output);

    // with expected TypeReference
    output = client.eval(
        "return {...}",
        input,
        null, BaseOptions.builder().withTimeout(100).build(),
        new TypeReference<List<Person>>() {}
    ).join().get();
    assertEquals(Collections.singletonList(new Person(2, null, "IvanD")), output);

    output = client.eval(
        "return ...",
        input,
        null, BaseOptions.builder().withTimeout(100).build(),
        new TypeReference<Person>() {}
    ).join().get();
    assertEquals(new Person(2, null, "IvanD"), output);
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testEvalArgsTupleExt() {
    Person person = new Person(3, false, "Kolya");
    HashMap<Integer, List<io.tarantool.mapping.Field>> formats = new HashMap<Integer, List<io.tarantool.mapping.Field>>() {{
      put(1, Arrays.asList(
          new io.tarantool.mapping.Field().setName("id").setType("integer"),
          new io.tarantool.mapping.Field().setName("is_married").setType("boolean"),
          new io.tarantool.mapping.Field().setName("name").setType("string")
      ));
    }};
    List<Tuple<Person>> input = Collections.singletonList(new Tuple<>(person, 1));
    TarantoolResponse<List<Object>> result = client.eval(
        "return (...):tomap()",
        input,
        formats, BaseOptions.builder().build(),
        new TypeReference<List<Object>>() {}
    ).join();
    HashMap<String, Object> expected = new HashMap<String, Object>() {{
      put("1", 3);
      put("2", false);
      put("3", "Kolya");
      put("is_married", false);
      put("name", "Kolya");
      put("id", 3);
    }};
    assertEquals(Collections.singletonList(expected), result.get());
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testCallArgsTupleExt() {
    Person person = new Person(3, false, "Kolya");
    HashMap<Integer, List<io.tarantool.mapping.Field>> formats = new HashMap<Integer, List<io.tarantool.mapping.Field>>() {{
      put(1, Arrays.asList(
          new io.tarantool.mapping.Field().setName("id").setType("integer"),
          new io.tarantool.mapping.Field().setName("is_married").setType("boolean"),
          new io.tarantool.mapping.Field().setName("name").setType("string")
      ));
    }};
    List<Tuple<Person>> input = Collections.singletonList(new Tuple<>(person, 1));
    TarantoolResponse<List<Object>> result = client.call(
        "echo_to_map",
        input,
        formats, BaseOptions.builder().build(),
        new TypeReference<List<Object>>() {}
    ).join();
    HashMap<String, Object> expected = new HashMap<String, Object>() {{
      put("1", 3);
      put("2", false);
      put("3", "Kolya");
      put("is_married", false);
      put("name", "Kolya");
      put("id", 3);
    }};
    assertEquals(Collections.singletonList(expected), result.get());
  }

  @Test
  public void testServerInformation() {
    Set<IProtoFeature> serverFeatures = EnumSet.allOf(IProtoFeature.class);
    int serverFeatureAmount = 4;

    if (tarantoolVersion == '3') {
      serverFeatureAmount = 7;
    }
    IProtoClient iprotoClient = client.getPool().get("default", 0).join();
    if (tarantoolVersion != '3') {
      serverFeatures = EnumSet.range(IProtoFeature.STREAMS, IProtoFeature.PAGINATION);
    }
    assertEquals(serverFeatureAmount, iprotoClient.getServerProtocolVersion());
    assertEquals(serverFeatures, iprotoClient.getServerFeatures());
  }

  @Test
  public void testWatchAndUnwatch() throws InterruptedException {
    List<Object> eventsKey1 = new ArrayList<>();
    List<Object> eventsKey2 = new ArrayList<>();

    client.watch("key1", v -> eventsKey1.add(v.get()));
    client.watch("key2", v -> eventsKey2.add(v.get()));

    client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
    Thread.sleep(100);

    client.unwatch("key1");
    client.unwatch("key2");
    client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
    Thread.sleep(100);

    assertEquals(Collections.singletonList("myEvent"), eventsKey1);
    assertEquals(Collections.singletonList(Arrays.asList(1, 2, 3)), eventsKey2);
  }

  @Test
  public void testWatchAndUnwatchWithClassAsTargetType() throws InterruptedException {
    List<String> eventsKey1 = new ArrayList<>();
    List<Person> eventsKey2 = new ArrayList<>();

    client.watch("string_event", value -> eventsKey1.add(value.get()), String.class);
    client.watch("secret_person", value -> eventsKey2.add(value.get()), Person.class);

    client.eval("box.broadcast('string_event', 'myEvent'); " +
        "box.broadcast('secret_person', {1, true, 'JohnWick'})");
    Thread.sleep(100);

    assertEquals(Collections.singletonList("myEvent"), eventsKey1);
    assertEquals(Collections.singletonList(new Person(1, true, "JohnWick")), eventsKey2);
  }

  @Test
  public void testWatchAndUnwatchWithTypeRefAsTargetType() throws InterruptedException {
    List<Map<String, List<Person>>> eventsKey = new ArrayList<>();

    client.watch("mega_event", value -> eventsKey.add(value.get()), new TypeReference<Map<String, List<Person>>>() {});

    client.eval("box.broadcast('mega_event', { agents = {{1, true, 'Wick'}, {007, false, 'Bond'}} })");
    Thread.sleep(100);

    HashMap<String, List<Person>> map = new HashMap<>();
    map.put("agents", Arrays.asList(
        new Person(1, true, "Wick"),
        new Person(007, false, "Bond")
    ));
    List<Map<String, List<Person>>> expected = Collections.singletonList(map);
    assertEquals(expected, eventsKey);
  }

  @Test
  public void testWatchOnce() throws InterruptedException {
    if (serverVersion < 6) {
      CompletionException ex = assertThrows(CompletionException.class, () -> client.watchOnce("key1").join());
      assertTrue(ex.getCause().getMessage().matches(
          "Tarantool doesn't support watch once feature. Need iproto version >= 6, got [1-5]"
      ));
      return;
    }
    assertEquals(0, client.watchOnce("k1").join().get().size());
    assertEquals(0, client.watchOnce("k2").join().get().size());

    client.eval("box.broadcast('k1', 'myEvent'); box.broadcast('k2', {1, 2, 3})");
    Thread.sleep(100);
    assertEquals(
        Collections.singletonList("myEvent"),
        client.watchOnce("k1").join().get()
    );
    assertArrayEquals(
        "myEvent".toCharArray(),
        client.watchOnce("k1", char[].class).join().get().get(0)
    );
    assertEquals(
        Collections.singletonList(Arrays.asList(1, 2, 3)),
        client.watchOnce("k2").join().get()
    );
    assertEquals(
        Collections.singletonList(new HashSet<>(Arrays.asList(1, 2, 3))),
        client.watchOnce("k2", Set.class).join().get()
    );
    assertEquals(
        Collections.singletonList(Arrays.asList(1L, 2L, 3L)),
        client.watchOnce("k2", new TypeReference<List<List<Long>>>() {}).join().get()
    );

    client.eval("box.broadcast('k1', 1)");
    Thread.sleep(100);
    assertEquals(
        Collections.singletonList(1),
        client.watchOnce("k1").join().get()
    );
    assertEquals(
        Collections.singletonList(Arrays.asList(1, 2, 3)),
        client.watchOnce("k2").join().get()
    );
  }

  @Test
  public void testClose() throws Exception {
    client.close();

    assertThrows(PoolClosedException.class, () -> client.eval("return true").join());
    assertThrows(PoolClosedException.class, () -> client.call("return_true").join());

    assertThrows(PoolClosedException.class, () -> client.ping().join());

    assertDoesNotThrow(() -> client.watch("key1", v -> {}));
    assertDoesNotThrow(() -> client.unwatch("key1"));
  }

  @Test
  public void testEvalAfterClientClosing() throws Exception {
    CompletableFuture<?> future = client.eval("return true");
    client.close();

    try {
      future.join();
    } catch (Exception ex) {
      assertEquals(ConnectionClosedException.class, ex.getCause().getClass());
      assertEquals("Connection closed by client", ex.getCause().getMessage());
    }
  }

  @Test
  public void testManyEvalAfterClientClosing() throws Exception {
    int completedFutures = 0;
    int uncompletedFutures = 0;
    List<CompletableFuture<TarantoolResponse<List<?>>>> futures = new ArrayList<>();
    String EVAL_EXPR_WITH_SLEEP = "fiber = require('fiber'); fiber.sleep(0.5); return true;";
    long N = 10_000;
    for (int i = 0; i < N; i++) {
      futures.add(client.eval(EVAL_EXPR_WITH_SLEEP));
    }

    client.close();

    for (CompletableFuture<TarantoolResponse<List<?>>> future : futures) {
      try {
        assertEquals(Collections.singletonList(true), future.get().get());
        completedFutures++;
      } catch (Exception ex) {
        assertEquals(ConnectionClosedException.class, ex.getCause().getClass());
        assertEquals("Connection closed by client", ex.getCause().getMessage());
        uncompletedFutures++;
      }
    }
    assertEquals(0, completedFutures);
    assertTrue(uncompletedFutures > 0);
  }

  @Test
  public void testCountRequestsAndResponses() {
    double requestAmount = TarantoolFactory.getRequestAmount();
    double responseSuccessAmount = TarantoolFactory.getResponseSuccessAmount();
    double responseErrorAmount = TarantoolFactory.getResponseErrorAmount();

    client.call("print", Collections.singletonList("1"));
    client.eval("return 1").join();
    try {
      client.eval("return unknownTable[1]").join();
    } catch (Throwable ignored) {
    }
    try {
      client.eval("return sleep(999)").join();
    } catch (Throwable ignored) {
    }

    assertEquals(4, TarantoolFactory.getRequestAmount() - requestAmount);
    assertEquals(2, TarantoolFactory.getResponseSuccessAmount() - responseSuccessAmount);
    assertEquals(2, TarantoolFactory.getResponseErrorAmount() - responseErrorAmount);
  }

  @Test
  void testCloseConnectionFromFewThreads() throws Exception {
    final TarantoolBoxClient testClient = TarantoolFactory.box()
        .withUser(API_USER)
        .withPassword(CREDS.get(API_USER))
        .withHost(tt.getHost())
        .withPort(tt.getPort())
        .build();

    final ExecutorService pool = Executors.newFixedThreadPool(100);
    final int closeCount = 1_000_000;
    for (int i = 0; i < closeCount; i++) {
      pool.execute(() -> {
        assertDoesNotThrow(testClient::close);
        assertTrue(testClient::isClosed);
      });
    }
    assertTrue(testClient::isClosed);
  }

  @Test
  void testIdempotency() throws Exception {
    final TarantoolBoxClient testClient = TarantoolFactory.box()
        .withUser(API_USER)
        .withPassword(CREDS.get(API_USER))
        .withHost(tt.getHost())
        .withPort(tt.getPort())
        .build();

    final int closeCount = 100;
    for (int i = 0; i < closeCount; i++) {
      assertDoesNotThrow(testClient::close);
      assertTrue(testClient::isClosed);
    }
  }

  @Test
  void testAutoCloseable() throws Exception {
    final TarantoolBoxSpace space;
    final Person person = new Person(0, true, "first");
    try (final TarantoolBoxClient testClient = TarantoolFactory.box()
        .withUser(API_USER)
        .withPassword(CREDS.get(API_USER))
        .withHost(tt.getHost())
        .withPort(tt.getPort())
        .build()) {
      space = testClient.space("person");
      assertEquals(person, space.insert(person, Person.class).join().get());
    }
    assertThrows(PoolClosedException.class, () -> space.insert(person).join());
  }

  @Test
  void testAutoCloseableWithInvalidRequest() throws Exception {
    final TarantoolBoxSpace space;
    try (final TarantoolBoxClient testClient = TarantoolFactory.box()
        .withUser(API_USER)
        .withPassword(CREDS.get(API_USER))
        .withHost(tt.getHost())
        .withPort(tt.getPort())
        .build()) {
      space = testClient.space("person");
      final Person person = new Person(0, true, "first");
      assertEquals(person, space.insert(person, Person.class).join().get());
      // duplicate
      space.insert(person).join();
    } catch (CompletionException exception) {
      assertEquals(BoxError.class, exception.getCause().getClass());
    }
  }
}
