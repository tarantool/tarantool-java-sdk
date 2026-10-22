/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.balancer.unit;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.balancer.TarantoolDistributingRoundRobinBalancer;
import io.tarantool.balancer.TarantoolRoundRobinBalancer;
import io.tarantool.balancer.exceptions.NoAvailableClientsException;
import io.tarantool.core.IProtoClient;
import io.tarantool.pool.IProtoClientPool;

/**
 * Balancer behaviour when the pool reports available clients, but {@code get} returns {@code null}
 * for locked entries (the counter and the per-entry flag of the pool are not updated atomically).
 */
class LockedPoolBalancerTest {

  private static final List<String> TAGS = Arrays.asList("a", "b");
  private static final int GROUP_SIZE = 3;

  enum Kind {
    ROUND_ROBIN(TarantoolRoundRobinBalancer::new),
    DISTRIBUTING(TarantoolDistributingRoundRobinBalancer::new);

    final Function<IProtoClientPool, TarantoolBalancer> factory;

    Kind(Function<IProtoClientPool, TarantoolBalancer> factory) {
      this.factory = factory;
    }
  }

  /**
   * Pool stub: {@code hasAvailableClients} is always true (worst case of the counter being ahead of
   * the flags); {@code get} returns {@code null} unless the entry is listed as free.
   */
  private static IProtoClientPool pool(Set<String> freeEntries, IProtoClient client) {
    return (IProtoClientPool)
        Proxy.newProxyInstance(
            IProtoClientPool.class.getClassLoader(),
            new Class<?>[] {IProtoClientPool.class},
            (proxy, method, args) -> {
              switch (method.getName()) {
                case "getTags":
                  return TAGS;
                case "getGroupSize":
                  return GROUP_SIZE;
                case "hasAvailableClients":
                  return true;
                case "get":
                  String key = args[0] + ":" + args[1];
                  return freeEntries.contains(key)
                      ? CompletableFuture.completedFuture(client)
                      : null;
                default:
                  throw new UnsupportedOperationException(method.getName());
              }
            });
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void allLockedFailsWithNoAvailableClients(Kind kind) {
    TarantoolBalancer balancer = kind.factory.apply(pool(Collections.<String>emptySet(), null));

    CompletableFuture<IProtoClient> future =
        assertTimeoutPreemptively(Duration.ofSeconds(5), balancer::getNext);

    ExecutionException e = assertThrows(ExecutionException.class, future::get);
    assertInstanceOf(NoAvailableClientsException.class, e.getCause());
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void allButOneLockedReturnsTheFreeEntry(Kind kind) throws Exception {
    IProtoClient client =
        (IProtoClient)
            Proxy.newProxyInstance(
                IProtoClient.class.getClassLoader(),
                new Class<?>[] {IProtoClient.class},
                (proxy, method, args) -> {
                  if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                  }
                  if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                  }
                  return "toString".equals(method.getName()) ? "client" : null;
                });
    for (String tag : TAGS) {
      for (int idx = 0; idx < GROUP_SIZE; idx++) {
        TarantoolBalancer balancer =
            kind.factory.apply(pool(Collections.singleton(tag + ":" + idx), client));

        CompletableFuture<IProtoClient> future =
            assertTimeoutPreemptively(Duration.ofSeconds(5), balancer::getNext);

        assertEquals(client, future.get());
      }
    }
  }
}
