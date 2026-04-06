/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.cluster.cartridge;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testcontainers.containers.cluster.ClusterConfigurator;

/** Configures a cartridge-based cluster after container startup. */
public class CartridgeClusterConfigurator
    implements ClusterConfigurator<CartridgeClusterContainer> {

  public static final String DEFAULT_CLUSTER_NAME = "cartridge-cluster";
  private static final String ROUTER_NODE_NAME = "router";

  private final CartridgeClusterContainer container;
  private final String clusterName;
  private final AtomicBoolean configured;

  public CartridgeClusterConfigurator(CartridgeClusterContainer container) {
    this(container, DEFAULT_CLUSTER_NAME);
  }

  public CartridgeClusterConfigurator(CartridgeClusterContainer container, String clusterName) {
    this.container = container;
    this.clusterName = clusterName;
    this.configured = new AtomicBoolean(false);
  }

  @Override
  public String clusterName() {
    return this.clusterName;
  }

  @Override
  public Map<String, CartridgeClusterContainer> nodes() {
    return Collections.singletonMap(ROUTER_NODE_NAME, this.container);
  }

  @Override
  public void configure() {
    this.container.waitUntilRouterIsUp(
        CartridgeClusterContainer.TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS);
    this.container.retryingSetupTopology();
    this.container.waitUntilCartridgeIsHealthy(
        CartridgeClusterContainer.TIMEOUT_ROUTER_UP_CARTRIDGE_HEALTH_IN_SECONDS);
    this.container.bootstrapVshard();
    this.configured.set(true);
  }

  @Override
  public boolean configured() {
    return this.configured.get();
  }
}
