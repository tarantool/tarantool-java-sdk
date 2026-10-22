/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.tarantool.core.connection.ConnectionFactory;

/** Lock and unlock of one pool entry called from several threads at once. */
public class PoolEntryLockTest {

  private static final int THREADS = 8;
  private static final int ITERATIONS = 20_000;

  private EventLoopGroup group;
  private Timer timer;

  @BeforeEach
  public void setUp() {
    group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
  }

  @AfterEach
  public void tearDown() throws Exception {
    timer.stop();
    group.shutdownGracefully().get();
  }

  @Test
  public void test_concurrentLockUnlock_shouldKeepUnavailableCountConsistent() throws Exception {
    IProtoClientPoolImpl.ConnectionStatistics statistics =
        new IProtoClientPoolImpl.ConnectionStatistics();
    PoolEntry entry = createEntry(statistics);

    CyclicBarrier start = new CyclicBarrier(THREADS);
    List<Thread> workers = new ArrayList<>();
    for (int t = 0; t < THREADS; t++) {
      Thread worker =
          new Thread(
              () -> {
                try {
                  start.await();
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
                for (int i = 0; i < ITERATIONS; i++) {
                  if (i % 2 == 0) {
                    entry.lock();
                  } else {
                    entry.unlock();
                  }
                }
              });
      workers.add(worker);
      worker.start();
    }
    for (Thread worker : workers) {
      worker.join();
    }

    // The entry counts as unavailable exactly while it is locked; without an atomic flag two
    // threads could both pass the check and change the counter twice.
    assertEquals(entry.isLocked() ? 1 : 0, statistics.unavailable());
  }

  private PoolEntry createEntry(IProtoClientPoolImpl.ConnectionStatistics statistics) {
    Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class);
    InstanceConnectionGroup instanceGroup =
        InstanceConnectionGroup.builder()
            .withHost(InetAddress.getLoopbackAddress().getHostAddress())
            .withPort(3301)
            .withSize(1)
            .withTag("lock")
            .build();
    return new PoolEntry(
        new ConnectionFactory(bootstrap, timer),
        timer,
        instanceGroup,
        0,
        false,
        1000,
        1000,
        null,
        null,
        statistics,
        null,
        null,
        null,
        false,
        null,
        null);
  }
}
