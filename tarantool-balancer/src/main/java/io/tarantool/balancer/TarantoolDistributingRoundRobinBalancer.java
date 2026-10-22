/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
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

public class TarantoolDistributingRoundRobinBalancer implements TarantoolBalancer {

  private static final Logger log =
      LoggerFactory.getLogger(TarantoolDistributingRoundRobinBalancer.class);
  private final IProtoClientPool pool;
  private final int[] tagsIndices;
  private final int[] groupSizes;
  private final List<String> tags;
  private int tagIndex;
  private final int tagsCount;

  /** Total number of connections; one full pass over them is the upper bound of attempts. */
  private final int totalSize;

  private final ReentrantLock balancerLock = new ReentrantLock();

  public TarantoolDistributingRoundRobinBalancer(IProtoClientPool pool) {
    this.pool = pool;

    tagIndex = 0;
    tags = pool.getTags();
    tagsCount = tags.size();
    tagsIndices = new int[tagsCount];
    groupSizes = new int[tagsCount];
    int total = 0;
    for (int i = 0; i < tagsCount; i++) {
      tagsIndices[i] = 0;
      groupSizes[i] = pool.getGroupSize(tags.get(i));
      total += groupSizes[i];
    }
    totalSize = total;
  }

  @Override
  public CompletableFuture<IProtoClient> getNext() {
    int attempts = totalSize;
    while (attempts-- > 0 && pool.hasAvailableClients()) {
      String tag;
      int connectionIndex;

      balancerLock.lock();
      try {
        connectionIndex = nextIndex();
        tag = nextTag();
      } finally {
        balancerLock.unlock();
      }

      CompletableFuture<IProtoClient> clientFuture = pool.get(tag, connectionIndex);
      if (clientFuture == null) {
        continue;
      }
      return clientFuture
          .handle(
              (connect, exc) -> {
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
  public void close() throws Exception {
    pool.close();
  }

  @Override
  public IProtoClientPool getPool() {
    return pool;
  }

  private int nextIndex() {
    int idx = tagsIndices[tagIndex]++;
    if (tagsIndices[tagIndex] >= groupSizes[tagIndex]) {
      tagsIndices[tagIndex] = 0;
    }

    return idx;
  }

  private String nextTag() {
    String tag = tags.get(tagIndex++);
    if (tagIndex >= tagsCount) {
      tagIndex = 0;
    }

    return tag;
  }
}
