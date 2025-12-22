/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.configuration.impl.file;

import java.util.Objects;
import java.util.UUID;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.containers.tdg.TDGContainerImpl;
import org.testcontainers.containers.tdg.cartridge.Topology.Replicaset;
import org.testcontainers.utility.DockerImageName;

class TDGNodeInfo implements AutoCloseable {

  private final UUID replicasetUuid;

  private final Replicaset replicaset;

  private final TDGContainer<?> container;

  public TDGNodeInfo(
      Replicaset replicaset,
      UUID replicasetUuid,
      String nodeUri,
      String clusterName,
      DockerImageName image,
      Network network) {
    this.replicaset = replicaset;
    this.replicasetUuid = replicasetUuid;
    this.container = createNode(replicaset, nodeUri, clusterName, image, network);
  }

  public TDGContainer<?> getContainer() {
    return this.container;
  }

  public boolean hasRole(String role) {
    return this.replicaset.hasRole(role);
  }

  private static TDGContainer<?> createNode(
      Replicaset replicaset,
      String nodeUri,
      String clusterName,
      DockerImageName image,
      Network network) {
    final String hostname = nodeUri.split(":")[0];

    // <relicaset-alias>-<advertise-host>-<cluster-name>
    final String containerName = replicaset.getAlias() + "-" + hostname + "-" + clusterName;

    return TDGContainerImpl.builder(image)
        .withNetwork(network)
        .withAdvertiseUri(nodeUri)
        .withAliases(hostname, containerName)
        .withNode(containerName)
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TDGNodeInfo that = (TDGNodeInfo) o;
    return Objects.equals(replicasetUuid, that.replicasetUuid)
        && Objects.equals(replicaset, that.replicaset)
        && Objects.equals(container, that.container);
  }

  @Override
  public int hashCode() {
    return Objects.hash(replicasetUuid, replicaset, container);
  }

  @Override
  public void close() {
    if (this.container != null) {
      this.container.close();
    }
  }
}
