/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

public interface GrpcContainer<SELF extends GrpcContainer<SELF>>
    extends Container<SELF>, Startable {

  Path DEFAULT_TQE_DATA_DIR = Paths.get("/", "var", "lib", "tarantool");

  /** Returns roles of grpc instance */
  Set<GrpcRole> roles();

  /**
   * Returns TDG node name. Returning name is alias of TDG container in docker network.
   *
   * @return node name
   */
  String node();

  /**
   * Stops container without save mount data. After calling this method, the container instance
   * can't be restarted (method {@code Container::start()} must throw exception).
   * <b><i>Note:</i></b> method must be idempotent.
   */
  @Override
  void stop();

  Set<InetSocketAddress> grpcAddresses();

  Set<InetSocketAddress> grpcAddresses(String customHost);

  InetSocketAddress coreAddress();

  InetSocketAddress coreAddress(String customHost);

  /** Enum of grpc node roles. */
  enum GrpcRole {
    CONSUMER("consumer"),

    PUBLISHER("publisher");

    private final String role;

    private static final Map<String, GrpcRole> ROLES = new HashMap<>();

    static {
      for (GrpcRole value : values()) {
        ROLES.put(value.getRole(), value);
      }
    }

    GrpcRole(String role) {
      this.role = role;
    }

    public String getRole() {
      return this.role;
    }

    public static GrpcRole from(String role) {
      final GrpcRole grpcRole = ROLES.get(role);
      if (grpcRole == null) {
        throw new IllegalArgumentException("Unknown grpc role: " + role);
      }
      return grpcRole;
    }
  }
}
