/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.cluster.vshard;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testcontainers.containers.cluster.ClusterConfigurator;

/** Configures a vshard-based cluster after container startup. */
public class VshardClusterConfigurator implements ClusterConfigurator<VshardClusterContainer> {

  public static final String DEFAULT_CLUSTER_NAME = "vshard-cluster";
  private static final String ROUTER_NODE_NAME = "router";

  private final VshardClusterContainer container;
  private final String clusterName;
  private final AtomicBoolean configured;

  public VshardClusterConfigurator(VshardClusterContainer container) {
    this(container, DEFAULT_CLUSTER_NAME);
  }

  public VshardClusterConfigurator(VshardClusterContainer container, String clusterName) {
    this.container = container;
    this.clusterName = clusterName;
    this.configured = new AtomicBoolean(false);
  }

  @Override
  public String clusterName() {
    return this.clusterName;
  }

  @Override
  public Map<String, VshardClusterContainer> nodes() {
    return Collections.singletonMap(ROUTER_NODE_NAME, this.container);
  }

  @Override
  public void configure() {
    this.container.waitUntilRouterIsUp(VshardClusterContainer.TIMEOUT_ROUTER_HEALTH_IN_SECONDS);
    this.container.waitUntilVshardIsBootstrapped(
        VshardClusterContainer.TIMEOUT_VSHARD_BOOTSTRAP_IN_SECONDS);
    this.container.waitUntilCrudIsUp(VshardClusterContainer.TIMEOUT_CRUD_HEALTH_IN_SECONDS);
    this.configured.set(true);
  }

  @Override
  public boolean configured() {
    return this.configured.get();
  }
}
