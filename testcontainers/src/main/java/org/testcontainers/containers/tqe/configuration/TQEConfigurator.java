/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration;

import java.time.Duration;
import java.util.Map;

import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tqe.GrpcContainer;

/**
 * Interface to configure TQE cluster.
 */
public interface TQEConfigurator extends AutoCloseable {

  /**
   * Default tqe containers startup timeout.
   */
  Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofSeconds(10);

  Duration DEFAULT_BOOTSTRAP_TIMEOUT = Duration.ofSeconds(5);

  /**
   * Returns name of TQE cluster
   */
  String clusterName();

  /**
   * Returns map of keys are queue instance names and values are queue Tarantool node containers
   */
  Map<String, TarantoolContainer<?>> queue();

  /**
   * Returns map. of Keys are names of grpc instance names, values are tqe grpc instance containers.
   */
  Map<String, GrpcContainer<?>> grpc();

  /**
   * Configures all containers of TQE cluster
   */
  void configure();

  /**
   * Returns true if all containers of TQE cluster are configured and ready to start.
   */
  boolean isConfigured();
}
