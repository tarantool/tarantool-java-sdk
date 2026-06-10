/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import org.testcontainers.containers.tqe.GrpcContainer;
import org.testcontainers.containers.tqe.GrpcContainer.GrpcRole;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;

/**
 * Encapsulates the lifecycle of a {@link TQECluster} for a single test: builds the configurator,
 * creates the cluster, starts it, resolves gRPC containers by role, and exposes pre-bound {@link
 * TQEClient} instances. {@link #close()} stops the cluster and shuts down all gRPC channels.
 */
final class TQEClusterFixture implements AutoCloseable {

  private final TQEVersion version;
  private final TQEConfigurator configurator;
  private final TQECluster cluster;
  private final ManagedChannel publisherChannel;
  private final ManagedChannel consumerChannel;
  private final TQEClient publisherClient;
  private final TQEClient consumerClient;

  TQEClusterFixture(TQEVersion version) {
    this.version = version;
    this.configurator =
        version.configuratorBuilder(version.queueConfig(), Set.of(version.grpcConfig())).build();
    this.cluster = new TQEClusterImpl(configurator);
    this.cluster.start();
    this.publisherChannel = createReadyChannel(findByRole(version.producerRole()));
    this.consumerChannel = createReadyChannel(findByRole(GrpcRole.CONSUMER));
    this.publisherClient = version.client(publisherChannel);
    this.consumerClient = version.client(consumerChannel);
  }

  TQEVersion version() {
    return version;
  }

  TQEClient publisherClient() {
    return publisherClient;
  }

  TQEClient consumerClient() {
    return consumerClient;
  }

  void restart(long delayBefore, TimeUnit unitBefore, long delayAfter, TimeUnit unitAfter)
      throws InterruptedException {
    this.cluster.restart(delayBefore, unitBefore, delayAfter, unitAfter);
  }

  @Override
  public void close() {
    try {
      publisherChannel.shutdownNow();
      consumerChannel.shutdownNow();
    } finally {
      this.cluster.stop();
    }
  }

  private GrpcContainer<?> findByRole(GrpcRole role) {
    return this.cluster.grpc().values().stream()
        .filter(g -> g.roles().contains(role))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No gRPC container with role "
                        + role
                        + " in cluster "
                        + cluster.clusterName()));
  }

  private static ManagedChannel createReadyChannel(GrpcContainer<?> grpc) {
    InetSocketAddress address =
        grpc.grpcAddresses().stream()
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No gRPC address on container " + grpc.node()));
    return TQETestHelper.createReadyChannel(address);
  }
}
