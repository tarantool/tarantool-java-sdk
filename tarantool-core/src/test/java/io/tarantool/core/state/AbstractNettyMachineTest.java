/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import io.netty.channel.EventLoop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AbstractNettyMachineTest {

  private EventLoop mockLoop;
  private TestNettyMachine machine;

  @BeforeEach
  void setUp() {
    mockLoop = Mockito.mock(EventLoop.class);
    machine = new TestNettyMachine(mockLoop);
  }

  @Test
  void testHandleInEventLoop() {
    Mockito.when(mockLoop.inEventLoop()).thenReturn(true);
    final Event event = Mockito.mock(Event.class);

    machine.handle(event);

    Assertions.assertEquals(1, machine.processedEvents.size());
    Assertions.assertSame(event, machine.processedEvents.get(0));
    Mockito.verify(mockLoop, Mockito.never()).execute(Mockito.any(Runnable.class));
  }

  @Test
  void testHandleOutsideEventLoop() {
    Mockito.when(mockLoop.inEventLoop()).thenReturn(false);
    final Event event = Mockito.mock(Event.class);

    machine.handle(event);

    Assertions.assertEquals(0, machine.processedEvents.size());

    final ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    Mockito.verify(mockLoop, Mockito.times(1)).execute(runnableCaptor.capture());

    runnableCaptor.getValue().run();

    Assertions.assertEquals(1, machine.processedEvents.size());
    Assertions.assertSame(event, machine.processedEvents.get(0));
  }

  @Test
  void testRecoveryAfterThrowable() {
    Mockito.when(mockLoop.inEventLoop()).thenReturn(true);
    final Event event = Mockito.mock(Event.class);
    final RuntimeException expectedException = new RuntimeException("Сбой бизнес-логики");

    machine.onProcessHook =
        () -> {
          throw expectedException;
        };

    Assertions.assertDoesNotThrow(() -> machine.handle(event));

    Assertions.assertEquals(1, machine.caughtErrors.size());
    Assertions.assertSame(expectedException, machine.caughtErrors.get(0));
    Assertions.assertEquals(0, machine.processedEvents.size());
  }

  @Test
  void testHandleRejectedExecutionException() {
    Mockito.when(mockLoop.inEventLoop()).thenReturn(false);
    Mockito.doThrow(new RejectedExecutionException("Loop is shutdown"))
        .when(mockLoop)
        .execute(Mockito.any(Runnable.class));

    final Event event = Mockito.mock(Event.class);

    Assertions.assertDoesNotThrow(() -> machine.handle(event));

    // Проверяем, что сработал наш новый безопасный обработчик отказа и контекст события НЕ потерян
    Assertions.assertEquals(1, machine.rejectedEvents.size());
    Assertions.assertSame(event, machine.rejectedEvents.get(0));
    Assertions.assertEquals(0, machine.processedEvents.size());
  }

  @Test
  void testFailSafeHandlerProtectsAgainstDoubleFault() {
    Mockito.when(mockLoop.inEventLoop()).thenReturn(true);
    final Event event = Mockito.mock(Event.class);

    machine.onProcessHook =
        () -> {
          throw new RuntimeException("First error");
        };
    TestNettyMachine brokenMachine =
        new TestNettyMachine(mockLoop) {
          @Override
          protected void processEvent(Event event) {
            throw new RuntimeException("First error");
          }

          @Override
          protected void onHandleError(Throwable error) {
            throw new RuntimeException("Second error onError inside");
          }
        };

    Assertions.assertDoesNotThrow(() -> brokenMachine.handle(event));
  }

  private static class TestNettyMachine extends AbstractNettyMachine<Event> {

    final List<Event> processedEvents = Collections.synchronizedList(new ArrayList<>());
    final List<Throwable> caughtErrors = Collections.synchronizedList(new ArrayList<>());
    final List<Event> rejectedEvents = Collections.synchronizedList(new ArrayList<>());

    volatile Runnable onProcessHook = null;

    protected TestNettyMachine(EventLoop loop) {
      super(loop);
    }

    @Override
    protected void processEvent(Event event) {
      if (onProcessHook != null) {
        onProcessHook.run();
      }
      processedEvents.add(event);
    }

    @Override
    protected void onHandleError(Throwable error) {
      caughtErrors.add(error);
    }

    @Override
    protected void onHandleRejectedException(Event event, RejectedExecutionException e) {
      rejectedEvents.add(event);
    }
  }
}
