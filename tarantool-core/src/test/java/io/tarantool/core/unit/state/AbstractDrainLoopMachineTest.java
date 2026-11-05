/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.unit.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import io.tarantool.core.state.AbstractDrainLoopMachine;
import io.tarantool.core.state.Event;

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(Lifecycle.PER_METHOD)
class AbstractDrainLoopMachineTest {

  private TestStateMachine stateMachine;

  @BeforeEach
  void setUp() {
    stateMachine = new TestStateMachine();
  }

  @Test
  void testSequentialProcessing() {
    final Event event1 = Mockito.mock(Event.class);
    final Event event2 = Mockito.mock(Event.class);

    this.stateMachine.handle(event1);
    this.stateMachine.handle(event2);

    final List<Event> processed = this.stateMachine.processedEvents();
    Assertions.assertEquals(2, processed.size());
    Assertions.assertSame(event1, processed.get(0));
    Assertions.assertSame(event2, processed.get(1));
    Assertions.assertEquals(0, this.stateMachine.errorCount());
  }

  @Test
  void testRecoveryAfterException() {
    final Event event1 = Mockito.mock(Event.class);
    final Event event2 = Mockito.mock(Event.class);
    final Event event3 = Mockito.mock(Event.class);

    this.stateMachine.handle(event1);
    this.stateMachine.throwOnEvent = new RuntimeException("Simulation error");

    this.stateMachine.handle(event2);
    this.stateMachine.handle(event3);

    Assertions.assertEquals(1, this.stateMachine.errorCount());
    final List<Event> processed = this.stateMachine.processedEvents();
    Assertions.assertEquals(2, processed.size());
    Assertions.assertSame(event1, processed.get(0));
    Assertions.assertSame(event3, processed.get(1));
  }

  @Test
  void testConcurrentDrainLoop() throws InterruptedException {
    int threadCount = 10;
    int eventsPerThread = 10000;
    int totalEvents = threadCount * eventsPerThread;

    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(threadCount);

    // Генерируем пачку потоков, которые будут одновременно бомбардировать машину событиями
    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < eventsPerThread; j++) {
                this.stateMachine.handle(Mockito.mock(Event.class));
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              finishLatch.countDown();
            }
          });
    }

    startLatch.countDown(); // Даем старт всем потокам одновременно
    boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    Assertions.assertTrue(completed);
    Assertions.assertEquals(totalEvents, stateMachine.processedEvents().size());
    Assertions.assertEquals(0, stateMachine.errorCount());
  }

  @Test
  void testEventSerialization() throws InterruptedException {
    final Event event1 = Mockito.mock(Event.class);
    final Event event2 = Mockito.mock(Event.class);

    final CountDownLatch inProcessEvent1 = new CountDownLatch(1);
    final CountDownLatch proceedEvent1 = new CountDownLatch(1);
    final AtomicInteger activeThreadsInHandler = new AtomicInteger(0);

    this.stateMachine.onProcessHook =
        () -> {
          activeThreadsInHandler.incrementAndGet();
          inProcessEvent1.countDown();
          try {
            proceedEvent1.await(2, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            activeThreadsInHandler.decrementAndGet();
          }
        };

    final Thread t1 = new Thread(() -> stateMachine.handle(event1));
    t1.start();

    Assertions.assertTrue(inProcessEvent1.await(1, TimeUnit.SECONDS));

    final long startTime = System.currentTimeMillis();
    this.stateMachine.handle(event2);
    final long duration = System.currentTimeMillis() - startTime;

    Assertions.assertTrue(duration < 500);

    proceedEvent1.countDown();
    t1.join(2000);

    Assertions.assertEquals(2, stateMachine.processedEvents().size());
  }

  @Test
  void testControlEventsPriority() throws InterruptedException {
    final Event blockerEvent = Mockito.mock(Event.class);
    final Event regularEvent = Mockito.mock(Event.class);
    final Event controlEvent = Mockito.mock(Event.class);

    Mockito.when(blockerEvent.isControl()).thenReturn(false);
    Mockito.when(regularEvent.isControl()).thenReturn(false);
    Mockito.when(controlEvent.isControl()).thenReturn(true);

    final CountDownLatch inProcessBlocker = new CountDownLatch(1);
    final CountDownLatch resumeBlocker = new CountDownLatch(1);

    this.stateMachine.onProcessHook =
        () -> {
          if (inProcessBlocker.getCount() > 0) {
            inProcessBlocker.countDown();
            try {
              resumeBlocker.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        };

    final Thread workerThread = new Thread(() -> this.stateMachine.handle(blockerEvent));
    workerThread.start();

    Assertions.assertTrue(inProcessBlocker.await(1, TimeUnit.SECONDS));

    this.stateMachine.onProcessHook = null;

    this.stateMachine.handle(regularEvent);
    this.stateMachine.handle(controlEvent);

    resumeBlocker.countDown();
    workerThread.join(2000);

    final List<Event> processed = this.stateMachine.processedEvents();

    Assertions.assertEquals(3, processed.size());
    Assertions.assertSame(blockerEvent, processed.get(0));
    Assertions.assertSame(
        controlEvent, processed.get(1), "Control event must bypass regular event due to priority");
    Assertions.assertSame(regularEvent, processed.get(2));

    Assertions.assertEquals(0, this.stateMachine.errorCount());
  }

  private static class TestStateMachine extends AbstractDrainLoopMachine<Event> {

    private final List<Event> processedEvents = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private volatile Runnable onProcessHook = null;
    private volatile RuntimeException throwOnEvent = null;

    @Override
    protected void processEvent(Event event) {
      if (throwOnEvent != null) {
        final RuntimeException ex = throwOnEvent;
        throwOnEvent = null;
        throw ex;
      }
      if (onProcessHook != null) {
        onProcessHook.run();
      }
      processedEvents.add(event);
    }

    @Override
    protected void onHandleError(Throwable error) {
      this.errorCount.incrementAndGet();
    }

    public List<Event> processedEvents() {
      return this.processedEvents;
    }

    public int errorCount() {
      return this.errorCount.get();
    }
  }
}
