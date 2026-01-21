/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.utility.DockerImageName;
import testcontainers.utils.TarantoolSingleNodeConfigUtils;

public abstract class TarantoolSingleInstanceConnectionAbstractExample {

  protected static final String TARANTOOL_TAG = "3.6.0";

  @TempDir protected static Path TEMP_DIR;

  private static final DockerImageName image =
      DockerImageName.parse("tarantool/tarantool:" + TARANTOOL_TAG);

  protected static TarantoolContainer<Tarantool3Container> CONTAINER;

  @BeforeAll
  static void beforeAll() throws IOException {
    CONTAINER = createSingleNodeContainer(TEMP_DIR);
    CONTAINER.start();
  }

  @AfterAll
  static void afterAll() {
    CONTAINER.stop();
  }

  protected static TarantoolContainer<Tarantool3Container> createSingleNodeContainer(Path tempPath)
      throws IOException {
    final Path pathToConfig = TarantoolSingleNodeConfigUtils.createConfig(tempPath);
    return new Tarantool3Container(image, "test-node").withConfigPath(pathToConfig);
  }
}
// --8<-- [end:all]
