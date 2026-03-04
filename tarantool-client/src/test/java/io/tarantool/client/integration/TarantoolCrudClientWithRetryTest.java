/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.testcontainers.containers.Helper.isCartridgeAvailable;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolCartridgeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.mapping.Tuple;

@Timeout(value = 5)
@Testcontainers
public class TarantoolCrudClientWithRetryTest {

  public class OperationsRepeater<T> {

    private final Supplier<CompletableFuture<T>> operation;
    private final Timer timer;
    private final CompletableFuture<T> future;
    private final long delay;
    private int attempts;

    public OperationsRepeater(
        Supplier<CompletableFuture<T>> operation, long delay, int attempts, Timer timer) {
      this.operation = operation;
      this.future = new CompletableFuture<>();
      this.timer = timer;
      this.delay = delay;
      this.attempts = attempts;
    }

    public CompletableFuture<T> run() {
      execute();
      return future;
    }

    private void execute() {
      operation
          .get()
          .whenComplete(
              (result, exc) -> {
                if (exc == null) {
                  future.complete(result);
                  return;
                }

                // also here we can analyze exception
                if (--attempts == 0) {
                  future.completeExceptionally(exc);
                  return;
                }

                timer.newTimeout((handler) -> execute(), delay, TimeUnit.MILLISECONDS);
              });
    }
  }

  private static final TarantoolCartridgeContainer tt =
      new TarantoolCartridgeContainer(
              "cartridge/Dockerfile",
              System.getenv().getOrDefault("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX", "")
                  + "cartridge",
              "cartridge/instances.yml",
              "cartridge/replicasets.yml",
              org.testcontainers.containers.Arguments.get("tarantool/tarantool"))
          .withStartupTimeout(Duration.ofMinutes(5))
          .withLogConsumer(
              new Slf4jLogConsumer(
                  LoggerFactory.getLogger(TarantoolCrudClientWithRetryTest.class)));

  private static TarantoolCrudClient client;
  private static final Person personInstance = new Person(1, true, "Roman");
  private static final Timer timerService = new HashedWheelTimer();

  @BeforeAll
  public static void setUp() throws Exception {
    if (isCartridgeAvailable()) {
      if (!tt.isRunning()) {
        tt.start();
      }
      client =
          TarantoolFactory.crud()
              .withHost(tt.getHost())
              .withPort(tt.getPort())
              .withUser("admin")
              .withPassword("secret-cluster-cookie")
              .build();
    }
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    tt.executeCommand("return crud.truncate('person')");
    client.call("crud_aux.break_api").get();
  }

  @AfterEach
  public void tearDown() throws Exception {
    client.call("crud_aux.unwrap_api").get();
    client.call("crud_aux.reset_counters").get();
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "2.*")
  public void testSuccessRetry() throws Exception {
    TarantoolCrudSpace person = client.space("person");
    OperationsRepeater<Person> repeater =
        new OperationsRepeater<>(
            () -> person.insert(personInstance, Person.class).thenApply(Tuple::get),
            1000,
            3,
            timerService);

    assertDoesNotThrow(() -> repeater.run().join());

    client.call("crud_aux.unwrap_api").get();
    assertEquals(
        personInstance, person.get(Collections.singletonList(1), Person.class).join().get());
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "2.*")
  public void testFailedRetry() {
    TarantoolCrudSpace person = client.space("person");
    OperationsRepeater<Person> repeater =
        new OperationsRepeater<>(
            () -> person.insert(personInstance, Person.class).thenApply(Tuple::get),
            1000,
            2,
            timerService);

    Throwable ex = assertThrows(CompletionException.class, () -> repeater.run().join());
    assertEquals(BoxError.class, ex.getCause().getClass());
  }
}
