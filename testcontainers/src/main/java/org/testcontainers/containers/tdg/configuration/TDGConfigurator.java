/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.configuration;

import java.util.Map;

import org.testcontainers.containers.tdg.TDGContainer;

/** Interface to represent classes that configure {@code Tarantool Data} Grid cluster */
public interface TDGConfigurator extends AutoCloseable {

  /**
   * Returns node (container) that has a {@code core} role.
   *
   * @return key is name of container (node) , value is container (node) that has a {@code core}
   *     role. Never null.
   */
  Map.Entry<String, TDGContainer<?>> core();

  /**
   * Returns cluster name.
   *
   * @return name of cluster. Never blank or null.
   */
  String clusterName();

  /**
   * Returns all containers(nodes) of TDG cluster.
   *
   * @return key is name of container (node), value is container (node).
   */
  Map<String, TDGContainer<?>> nodes();

  /**
   * Configures cluster.
   *
   * <p><b><i>Note:</i></b> before calling the method, nodes of cluster must be started.
   *
   * <p><b><i>Note:</i></b> method must be idempotent.
   *
   * <p><b><i>Note:</i></b> the cluster configuration life cycle should be implementation-driven,
   * but should include three main consistent phases:
   *
   * <ol>
   *   <li><b><i>Assembling:</i></b>
   *       <p>assembly is the cluster configuration step where the cluster topology is loaded onto
   *       one of the TDG nodes. After the topology is loaded, the nodes are divided into replica
   *       sets. Each replica set performs specific roles. Learn more: <a
   *       href="https://www.tarantool.io/ru/tdg/latest/reference/config/">Tarantool Data Grid
   *       documentation</a>
   *   <li><b><i>Bootstrapping:</i></b>
   *       <p>bootstrapping is the stage at which the `bootstrap_vshard` command is executed on the
   *       node with the `vshard-router` role
   *   <li><b><i>Configuring:</i></b>
   *       <p>at the configuration stage, the configuration and data model are loaded through the
   *       node with the `core` role.
   * </ol>
   */
  void configure();

  /**
   * Returns a value indicating whether the cluster is configured.
   *
   * @return true - cluster is configured, false - cluster is not configured.
   */
  boolean configured();
}
