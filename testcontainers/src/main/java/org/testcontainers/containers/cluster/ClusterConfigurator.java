/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.cluster;

import java.util.Map;

/**
 * Common configurator contract for single-router test clusters.
 *
 * @param <C> cluster container type
 */
public interface ClusterConfigurator<C extends ClusterContainer<?>> extends AutoCloseable {

  /** Returns cluster name. */
  String clusterName();

  /** Returns all cluster nodes participating in configuration. */
  Map<String, C> nodes();

  /** Applies cluster-specific configuration after container startup. */
  void configure();

  /** Returns true if configuration was already applied for current lifecycle. */
  boolean configured();

  @Override
  default void close() {}
}
