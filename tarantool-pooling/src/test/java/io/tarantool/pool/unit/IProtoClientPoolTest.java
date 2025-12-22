/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.pool.exceptions.PoolClosedException;

public class IProtoClientPoolTest {

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

  private ManagedResource<Timer> timerResource;
  protected ConnectionFactory factory;
  protected IProtoClientPool pool;

  @BeforeEach
  public void setUp() {
    timerResource = ManagedResource.owned(new HashedWheelTimer(), Timer::stop);
    factory = new ConnectionFactory(bootstrap, timerResource.get());
    pool = new IProtoClientPoolImpl(factory, timerResource);
  }

  @Test
  public void testConnectionTimeoutGetterAndSetter() {
    long timeout = 1000;
    pool.setConnectTimeout(timeout);
    assertEquals(timeout, pool.getConnectTimeout());
  }

  @Test
  public void testConnectionTimeoutWithInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> pool.setConnectTimeout(-1));
    assertThrows(IllegalArgumentException.class, () -> pool.setConnectTimeout(0));
  }

  @Test
  public void testReconnectAfterGetterAndSetter() {
    long reconnectAfter = 2000;
    pool.setReconnectAfter(reconnectAfter);
    assertEquals(reconnectAfter, pool.getReconnectAfter());
  }

  @Test
  public void testReconnectAfterWithInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> pool.setReconnectAfter(-1));
    assertThrows(IllegalArgumentException.class, () -> pool.setReconnectAfter(0));
  }

  @Test
  public void testHasAvailableClientsInitially() {
    assertFalse(pool.hasAvailableClients());
  }

  @Test
  public void testAvailableConnectionsInitially() {
    assertEquals(0, pool.availableConnections());
  }

  @Test
  public void testGetTagsWhenNoGroups() {
    List<String> tags = pool.getTags();
    assertNotNull(tags);
    assertTrue(tags.isEmpty());
  }

  @Test
  public void testGetTagsReturnsCorrectTags() {
    List<InstanceConnectionGroup> groups =
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(2)
                .withTag("node-1")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3302)
                .withSize(3)
                .withTag("node-2")
                .build());

    pool.setGroups(groups);

    List<String> tags = pool.getTags();
    assertEquals(2, tags.size());
    assertTrue(tags.contains("node-1"));
    assertTrue(tags.contains("node-2"));
  }

  @Test
  public void testGetGroupSizeWithNonExistentTag() {
    assertThrows(NoSuchElementException.class, () -> pool.getGroupSize("non-existent-tag"));
  }

  @Test
  public void testPoolGetWithNonExistentTag() {
    assertThrows(NoSuchElementException.class, () -> pool.get("non-existent-tag", 0));
  }

  @Test
  public void testPoolGetWithValidTagButInvalidIndex() {
    List<InstanceConnectionGroup> groups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(2)
                .withTag("node-1")
                .build());

    pool.setGroups(groups);

    // Valid tag, but index out of bounds
    assertThrows(IndexOutOfBoundsException.class, () -> pool.get("node-1", 5));
  }

  @Test
  public void testPoolGetWithValidParametersReturnsFuture() {
    List<InstanceConnectionGroup> groups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(1)
                .withTag("node-1")
                .build());

    pool.setGroups(groups);

    CompletableFuture<IProtoClient> future = pool.get("node-1", 0);
    assertNotNull(future);
  }

  @Test
  public void testForEachExecutesActionOnAllClients() {
    List<InstanceConnectionGroup> groups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(2)
                .withTag("node-1")
                .build());

    pool.setGroups(groups);

    final int[] counter = {0};
    Consumer<IProtoClient> countingConsumer = client -> counter[0]++;

    pool.forEach(countingConsumer);

    assertEquals(2, counter[0]);
  }

  @Test
  public void testGetFactory() {
    assertEquals(factory, pool.getFactory());
  }

  @Test
  public void testClosePool() throws Exception {
    List<InstanceConnectionGroup> groups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(2)
                .withTag("node-1")
                .build());

    pool.setGroups(groups);

    // Pool should work before closing
    assertEquals(2, pool.getGroupSize("node-1"));

    pool.close();

    // After closing, getting a client should throw PoolClosedException
    assertThrows(PoolClosedException.class, () -> pool.get("node-1", 0));
  }

  @Test
  public void testSetGroupsWhenRemovingGroupEntirely() {
    List<InstanceConnectionGroup> initialGroups =
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(3)
                .withTag("node-1")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3302)
                .withSize(2)
                .withTag("node-2")
                .build());

    pool.setGroups(initialGroups);

    assertEquals(3, pool.getGroupSize("node-1"));
    assertEquals(2, pool.getGroupSize("node-2"));

    // Remove one group entirely
    List<InstanceConnectionGroup> reducedGroups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(3)
                .withTag("node-1")
                .build());

    pool.setGroups(reducedGroups);

    assertEquals(3, pool.getGroupSize("node-1"));
    assertThrows(NoSuchElementException.class, () -> pool.getGroupSize("node-2"));
  }

  @Test
  public void testSetGroupsWithInvalidIndexAfterResize() {
    List<InstanceConnectionGroup> initialGroups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(3)
                .withTag("node-1")
                .build());

    pool.setGroups(initialGroups);

    List<InstanceConnectionGroup> reducedGroups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(1)
                .withTag("node-1")
                .build());

    pool.setGroups(reducedGroups);

    assertEquals(1, pool.getGroupSize("node-1"));

    assertThrows(IndexOutOfBoundsException.class, () -> pool.get("node-1", 1));
  }

  @Test
  public void testSetGroupsExpandAndShrinkMultipleTimes() {
    final String tag = "node-1";
    final int port = 3301;
    final String host = "localhost";

    // Test sizes in sequence: start with 2, expand to 5, shrink to 3, shrink to 1
    int[] sizes = {2, 5, 3, 1};

    for (int size : sizes) {
      List<InstanceConnectionGroup> groups =
          Collections.singletonList(
              InstanceConnectionGroup.builder()
                  .withHost(host)
                  .withPort(port)
                  .withSize(size)
                  .withTag(tag)
                  .build());

      pool.setGroups(groups);
      assertEquals(size, pool.getGroupSize(tag));
    }
  }

  @Test
  public void testSetGroupsWithEmptyListRemovesAllGroups() {
    List<InstanceConnectionGroup> initialGroups =
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(2)
                .withTag("node-1")
                .build());

    pool.setGroups(initialGroups);
    assertEquals(1, pool.getTags().size());

    pool.setGroups(Collections.emptyList());

    assertEquals(0, pool.getTags().size());
    assertThrows(NoSuchElementException.class, () -> pool.getGroupSize("node-1"));
  }

  @Test
  public void testAvailableConnectionsAfterSettingGroups() {
    List<InstanceConnectionGroup> groups =
        Arrays.asList(
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3301)
                .withSize(3)
                .withTag("node-1")
                .build(),
            InstanceConnectionGroup.builder()
                .withHost("localhost")
                .withPort(3302)
                .withSize(2)
                .withTag("node-2")
                .build());

    pool.setGroups(groups);

    assertEquals(5, pool.availableConnections());
    assertTrue(pool.hasAvailableClients());
  }
}
