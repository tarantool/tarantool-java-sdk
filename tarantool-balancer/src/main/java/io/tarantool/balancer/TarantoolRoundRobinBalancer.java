/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.balancer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.balancer.exceptions.NoAvailableClientsException;
import io.tarantool.core.IProtoClient;
import io.tarantool.pool.IProtoClientPool;

public class TarantoolRoundRobinBalancer implements TarantoolBalancer {

  private static final Logger log = LoggerFactory.getLogger(TarantoolRoundRobinBalancer.class);
  private final IProtoClientPool pool;
  private final List<String> tags;
  private String currentTag;
  private int currentTagIndex;
  private int currentConnectionIndex;
  private final ReentrantLock balancerLock = new ReentrantLock();

  public TarantoolRoundRobinBalancer(IProtoClientPool pool) {
    this.pool = pool;
    this.tags = pool.getTags();
    this.currentTagIndex = 0;
    this.currentConnectionIndex = 0;
    this.currentTag = this.tags.get(currentTagIndex);
  }

  @Override
  public CompletableFuture<IProtoClient> getNext() {
    while (pool.hasAvailableClients()) {
      String tag;
      int connectionIndex;

      balancerLock.lock();
      try {
        if (currentConnectionIndex >= pool.getGroupSize(currentTag)) {
          currentConnectionIndex = 0;
          currentTagIndex++;
          if (currentTagIndex >= tags.size()) {
            currentTagIndex = 0;
          }
          currentTag = tags.get(currentTagIndex);
        }
        tag = currentTag;
        connectionIndex = currentConnectionIndex++;
      } finally {
        balancerLock.unlock();
      }

      CompletableFuture<IProtoClient> clientFuture = pool.get(tag, connectionIndex);
      if (clientFuture == null) {
        continue;
      }

      return clientFuture
          .handle((connect, exc) -> {
            if (exc != null) {
              log.warn("taking {}:{} failed", tag, connectionIndex, exc);
              return getNext();
            }
            return CompletableFuture.completedFuture(connect);
          })
          .thenCompose(x -> x);
    }

    CompletableFuture<IProtoClient> future = new CompletableFuture<>();
    future.completeExceptionally(new NoAvailableClientsException());
    return future;
  }

  @Override
  public IProtoClientPool getPool() {
    return pool;
  }

  @Override
  public void close() throws Exception {
    pool.close();
  }
}
