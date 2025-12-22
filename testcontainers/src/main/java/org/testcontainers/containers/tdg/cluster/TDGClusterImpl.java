/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.cluster;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.containers.tdg.TDGContainerImpl;
import org.testcontainers.containers.tdg.configuration.TDGConfigurator;

/**
 * Base implementation of {@link TDGCluster} that uses {@link TDGConfigurator} to configure TDG nodes. Helps to
 * manipulate {@link TDGContainer}.
 */
public class TDGClusterImpl implements TDGCluster {

  private static final Logger LOGGER = LoggerFactory.getLogger(TDGClusterImpl.class);

  private final TDGConfigurator configurator;

  private final Map<String, TDGContainer<?>> nodes;

  private final Duration startupTimeout;

  private boolean isClosed;

  public TDGClusterImpl(TDGConfigurator configurator) {
    this(configurator,  Duration.ofMinutes(1));
  }

  public TDGClusterImpl(TDGConfigurator configurator, Duration startupTimeout) {
    this.configurator = configurator;
    this.nodes = new HashMap<>();
    this.nodes.putAll(this.configurator.nodes());
    this.isClosed = false;
    this.startupTimeout = startupTimeout;
  }

  @Override
  public Map<String, TDGContainer<?>> nodes() {
    return this.nodes;
  }

  @Override
  public String clusterName() {
    return this.configurator.clusterName();
  }

  @Override
  public synchronized void start() {
    if (this.isClosed) {
      throw new IllegalStateException("TDG cluster already closed. Please, create new TDG cluster instance");
    }
    LOGGER.info("TDG cluster [name = {}, count = {}] is starting...", this.configurator.clusterName(),
        this.nodes.size());
    final CountDownLatch latch = new CountDownLatch(this.nodes.size());
    final AtomicReference<Exception> failedToStart = new AtomicReference<>();

    for (TDGContainer<?> container : this.nodes.values()) {
      new Thread(() -> {
        try {
          container.start();
        } catch (Exception e) {
          LOGGER.error("Error starting TDG container [container_name='{}']", container.node(), e);
          failedToStart.set(e);
        } finally {
          latch.countDown();
        }
      }).start();
    }

    try {
      latch.await(this.startupTimeout.getSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new ContainerLaunchException(
          "TDG cluster[cluster_name='" + this.configurator.clusterName() + "'] cannot start. See logs for details.");
    }

    if (failedToStart.get() != null) {
      throw new ContainerLaunchException("Cluster failed to start", failedToStart.get());
    }

    if (this.configurator.configured()) {
      LOGGER.warn("TDG cluster [name = {}, count = {}] already configured", this.configurator.clusterName(),
            this.nodes.size());
      return;
    }

    this.configurator.configure();
  }

  @Override
  public synchronized void stop() {
    if (this.isClosed) {
      return;
    }
    LOGGER.info("TDG cluster is stopping");
    if (this.configurator != null) {
      try {
        this.configurator.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    this.isClosed = true;
    LOGGER.info("TDG cluster is stopped");
  }
}
