/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolCartridgeContainer;
import org.testcontainers.containers.TarantoolContainerOperations;
import org.testcontainers.containers.VshardClusterContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.tarantool.spring.data.utils.Constants.DEFAULT_PROPERTY_FILE_NAME;
import static io.tarantool.spring.data33.utils.TarantoolTestSupport.DEFAULT_TEST_PROPERTY_DIR;
import static io.tarantool.spring.data33.utils.TarantoolTestSupport.writeTestPropertiesYaml;
import io.tarantool.spring.data33.config.properties.TarantoolProperties;

@Testcontainers
@Timeout(5)
public abstract class BaseIntegrationTest {

  protected static TarantoolContainerOperations<?> clusterContainer;

  private static final String dockerRegistry =
      System.getenv().getOrDefault("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX", "");

  @BeforeAll
  protected static void beforeAll() throws IOException {
    configureContainer();
    writeConfigurationFile();
  }

  private static void configureContainer() {
    if (!isCartridgeAvailable()) {
      VshardClusterContainer vshardClusterContainer =
          new VshardClusterContainer(
              "vshard_cluster/Dockerfile",
              dockerRegistry + "vshard-cluster-java",
              "vshard_cluster/instances.yaml",
              "vshard_cluster/config.yaml",
              "tarantool/tarantool");

      if (!vshardClusterContainer.isRunning()) {
        vshardClusterContainer.withPrivilegedMode(true);
        vshardClusterContainer.start();
      }
      clusterContainer = vshardClusterContainer;
    } else {
      TarantoolCartridgeContainer cartridgeContainer =
          new TarantoolCartridgeContainer(
                  "Dockerfile",
                  dockerRegistry + "cartridge",
                  "cartridge/instances.yml",
                  "cartridge/replicasets.yml",
                  org.testcontainers.containers.Arguments.get("tarantool/tarantool"))
              .withStartupTimeout(Duration.ofMinutes(5))
              .withLogConsumer(
                  new Slf4jLogConsumer(LoggerFactory.getLogger(BaseIntegrationTest.class)));

      if (!cartridgeContainer.isRunning()) {
        cartridgeContainer.start();
      }
      clusterContainer = cartridgeContainer;
    }
  }

  private static void writeConfigurationFile() throws IOException {
    final TarantoolProperties properties = new TarantoolProperties();
    properties.setHost(clusterContainer.getHost());
    properties.setPort(clusterContainer.getPort());

    writeTestPropertiesYaml(DEFAULT_PROPERTY_FILE_NAME, properties);
  }

  @AfterAll
  protected static void afterAll() throws IOException {
    Files.deleteIfExists(DEFAULT_TEST_PROPERTY_DIR.resolve(DEFAULT_PROPERTY_FILE_NAME));
  }

  protected static String getHost() {
    return clusterContainer.getHost();
  }

  protected static int getPort() {
    return clusterContainer.getPort();
  }

  protected static boolean isCartridgeAvailable() {
    return System.getenv().getOrDefault("TARANTOOL_VERSION", "").matches("2.*");
  }
}
