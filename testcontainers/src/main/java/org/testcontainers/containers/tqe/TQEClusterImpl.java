/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tqe.configuration.TQEConfigurator;
import org.testcontainers.lifecycle.Startable;

public class TQEClusterImpl implements TQECluster {

  private static final Logger LOGGER = LoggerFactory.getLogger(TQEClusterImpl.class);

  private final TQEConfigurator configurator;

  private boolean isClosed;

  public TQEClusterImpl(TQEConfigurator configurator) {
    this.configurator = configurator;
  }

  @Override
  public synchronized void start() {
    if (this.isClosed) {
      throw new IllegalStateException("Container is already closed. Please create new container");
    }
    LOGGER.info(
        "TQE cluster [name = {}, queue = {}, grpc = {}] is starting...",
        this.configurator.clusterName(),
        this.queue().size(),
        this.grpc().size());

    startParallel(this.configurator.queue(), this.configurator);
    startParallel(this.configurator.grpc(), this.configurator);
    if (this.configurator.isConfigured()) {
      LOGGER.warn(
          "TQE cluster [name = {}, queue = {}, grpc = {}] already configured",
          this.configurator.clusterName(),
          this.configurator.queue().size(),
          this.configurator.grpc().size());
      return;
    }

    this.configurator.configure();
  }

  @Override
  public synchronized void stop() {
    if (this.configurator != null) {
      try {
        this.configurator.close();
        this.isClosed = true;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void startParallel(
      Map<String, ? extends Startable> containers, TQEConfigurator configurator) {

    final Executor executor = Executors.newFixedThreadPool(containers.size());
    final List<CompletableFuture<?>> futures = new ArrayList<>(containers.size());
    final CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

    containers.forEach(
        (name, c) ->
            futures.add(
                CompletableFuture.runAsync(
                    () -> {
                      try {
                        c.start();
                      } catch (Exception e) {
                        LOGGER.error("Error starting TQE container [container_name='{}']", name, e);
                        errors.add(e);
                      }
                    },
                    executor)));

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[] {})).join();

    if (!errors.isEmpty()) {
      throw new ContainerLaunchException(
          "TQE cluster[cluster_name='"
              + configurator.clusterName()
              + "'] cannot start. See logs for details.");
    }
  }

  @Override
  public String clusterName() {
    return this.configurator.clusterName();
  }

  @Override
  public Map<String, TarantoolContainer<?>> queue() {
    return this.configurator.queue();
  }

  @Override
  public Map<String, GrpcContainer<?>> grpc() {
    return this.configurator.grpc();
  }
}
