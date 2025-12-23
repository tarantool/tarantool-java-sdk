/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client.simple.connection;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.utility.DockerImageName;
import testcontainers.single.CreateSingleNode;

public abstract class SingleNodeConnection {

  @TempDir protected static Path TEMP_DIR;

  private static final DockerImageName image = DockerImageName.parse("tarantool/tarantool:3.4.1");

  protected static TarantoolContainer<Tarantool3Container> CONTAINER;

  @BeforeAll
  static void beforeAll() throws IOException {
    CONTAINER = createSingleNodeContainer(TEMP_DIR);
    CONTAINER.start();
  }

  protected abstract void simpleConnection();

  protected static TarantoolContainer<Tarantool3Container> createSingleNodeContainer(Path tempPath)
      throws IOException {
    final Path pathToConfig = CreateSingleNode.createSingleNodeConfig(tempPath);
    return new Tarantool3Container(image, "test-node").withConfigPath(pathToConfig);
  }
}
