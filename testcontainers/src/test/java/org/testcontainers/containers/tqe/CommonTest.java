/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.utility.DockerImageName;

public abstract class CommonTest {

  @TempDir
  protected static Path TEST_TEMP_DIR;

  protected static final DockerImageName IMAGE_NAME =
      DockerImageName.parse(System.getenv().getOrDefault("TARANTOOL_REGISTRY", "") +
          "tarantool/message-queue-ee:2.5.3");

  protected static final Path SIMPLE_GRPC_CONFIG;
  protected static final Path SIMPLE_QUEUE_CONFIG;

  static {
    try {
      SIMPLE_GRPC_CONFIG = Paths.get(Objects.requireNonNull(
          CommonTest.class.getClassLoader().getResource("tqe/simple-config/simple-grpc.yml")).toURI()
      );
      SIMPLE_QUEUE_CONFIG = Paths.get(Objects.requireNonNull(
          CommonTest.class.getClassLoader().getResource("tqe/simple-config/simple-queue.yml")).toURI()
      );
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
