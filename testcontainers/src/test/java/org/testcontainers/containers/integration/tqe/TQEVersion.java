/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.testcontainers.containers.tqe.GrpcContainer.GrpcRole;
import org.testcontainers.containers.tqe.TQECluster;
import org.testcontainers.containers.tqe.TQEClusterImpl;
import org.testcontainers.containers.tqe.configuration.FileTQEConfigurator;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;
import org.testcontainers.utility.DockerImageName;

/**
 * Encapsulates all version-specific aspects of a TQE test: image, configs, gRPC role names,
 * builder/cluster factories, and the gRPC strategy.
 *
 * <p>Adding a new TQE version (e.g. TQE 4.x) is an Open/Closed-friendly change: add a new constant,
 * override the abstract methods — no test code needs to change.
 */
enum TQEVersion {
  TQE2("TQE 2.x", "publisher", GrpcRole.PRODUCER, false, TQE2GrpcTestStrategy.INSTANCE) {
    @Override
    public DockerImageName imageName() {
      return IMAGE_TQE2;
    }

    @Override
    public Path queueConfig() {
      return QUEUE_CONFIG_TQE2;
    }

    @Override
    public Path grpcConfig() {
      return GRPC_CONFIG_TQE2;
    }

    @Override
    public FileTQEConfigurator.Builder configuratorBuilder(Path queue, Set<Path> grpc) {
      return FileTQEConfigurator.tqe2Builder(imageName(), queue, grpc);
    }

    @Override
    public TQECluster createCluster(TQEConfigurator configurator) {
      return new TQEClusterImpl(configurator);
    }
  },

  TQE3("TQE 3.x", "producer", GrpcRole.PRODUCER, true, TQE3GrpcTestStrategy.INSTANCE) {
    @Override
    public DockerImageName imageName() {
      return IMAGE_TQE3;
    }

    @Override
    public Path queueConfig() {
      return QUEUE_CONFIG_TQE3;
    }

    @Override
    public Path grpcConfig() {
      return GRPC_CONFIG_TQE3;
    }

    @Override
    public FileTQEConfigurator.Builder configuratorBuilder(Path queue, Set<Path> grpc) {
      return FileTQEConfigurator.tqe3Builder(imageName(), queue, grpc);
    }

    @Override
    public TQECluster createCluster(TQEConfigurator configurator) {
      return new TQEClusterImpl(configurator);
    }
  };

  private static final DockerImageName IMAGE_TQE2 =
      DockerImageName.parse(
          System.getenv().getOrDefault("TARANTOOL_REGISTRY", "")
              + "tarantool/message-queue-ee:2.5.3");

  private static final DockerImageName IMAGE_TQE3 =
      DockerImageName.parse(
          System.getenv().getOrDefault("TARANTOOL_REGISTRY", "")
              + "tarantool/message-queue-ee:v3.5.0");

  private static final Path QUEUE_CONFIG_TQE2 =
      TQETestHelper.loadConfig("tqe2/simple-config/simple-queue.yml");
  private static final Path GRPC_CONFIG_TQE2 =
      TQETestHelper.loadConfig("tqe2/simple-config/simple-grpc.yml");
  private static final Path QUEUE_CONFIG_TQE3 =
      TQETestHelper.loadConfig("tqe3/simple-config/simple-queue.yml");
  private static final Path GRPC_CONFIG_TQE3 =
      TQETestHelper.loadConfig("tqe3/simple-config/simple-grpc.yml");

  private final String displayName;
  private final String producerRoleName;
  private final GrpcRole producerRole;
  private final boolean requiresConfigure;
  private final GrpcTestStrategy strategy;

  TQEVersion(
      String displayName,
      String producerRoleName,
      GrpcRole producerRole,
      boolean requiresConfigure,
      GrpcTestStrategy strategy) {
    this.displayName = displayName;
    this.producerRoleName = producerRoleName;
    this.producerRole = producerRole;
    this.requiresConfigure = requiresConfigure;
    this.strategy = strategy;
  }

  public String displayName() {
    return displayName;
  }

  public String producerRoleName() {
    return producerRoleName;
  }

  public GrpcRole producerRole() {
    return producerRole;
  }

  /**
   * Whether manual orchestration of {@code configurator.configure()} is required between starting
   * the queue and the gRPC containers. TQE 2.x auto-configures inside {@code
   * startTarantoolCluster()}; TQE 3.x defers it to {@code startGrpcEndpoints()}. Tests that drive
   * the configurator directly (without a cluster) use this flag.
   */
  public boolean requiresConfigure() {
    return requiresConfigure;
  }

  public GrpcTestStrategy strategy() {
    return strategy;
  }

  public abstract DockerImageName imageName();

  public abstract Path queueConfig();

  public abstract Path grpcConfig();

  public abstract FileTQEConfigurator.Builder configuratorBuilder(Path queue, Set<Path> grpc);

  public abstract TQECluster createCluster(TQEConfigurator configurator);

  static Stream<TQEVersion> all() {
    return Stream.of(values());
  }

  @Override
  public String toString() {
    return displayName;
  }
}
