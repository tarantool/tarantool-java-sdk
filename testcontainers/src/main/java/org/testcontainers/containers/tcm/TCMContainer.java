/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tcm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.tcm.config.TCMConfig;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.MountableFile;

public class TCMContainer extends GenericContainer<TCMContainer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCMContainer.class);

  private static final Path TCM_CONFIG_PATH = Paths.get("/", "app", "tarantooldb", "tcm.yaml");

  private static final String TNT_CONFIG_NAME = "config.yaml";

  private static final Path TARANTOOL_CONFIG_PATH =
      Paths.get("/", "app", "tarantooldb", TNT_CONFIG_NAME);

  private static final String WAIT_ETCD_READY_SCRIPT = "tools/client/wait_etcd_ready.sh";

  private static final String WAIT_CONFIG_READY_SCRIPT = "tools/client/wait_config_ready.sh";

  private static final String TT_COMMAND = "tt";

  private final AtomicBoolean configured = new AtomicBoolean();

  private final String nodeName;

  private final TCMConfig tcmConfig;

  private final Path tarantoolConfigPath;

  private final String etcdAddress;

  private final String prefix;

  private final YAMLMapper mapper;

  private Path tempConfigDirectory;

  public TCMContainer(
      String image, String nodeName, TCMConfig tcmConfig, Path tarantoolConfigPath) {
    super(Objects.requireNonNull(image, "image must not be null"));

    this.nodeName = Objects.requireNonNull(nodeName, "nodeName must not be null");
    this.tcmConfig = Objects.requireNonNull(tcmConfig, "tcmConfig must not be null");
    this.tarantoolConfigPath =
        Objects.requireNonNull(tarantoolConfigPath, "tarantoolConfigPath must not be null");
    this.prefix =
        this.tcmConfig
            .getInitialSettings()
            .cluster()
            .getStorageConnection()
            .getEtcdConnection()
            .getPrefix();
    this.etcdAddress = this.tcmConfig.getStorage().getEtcd().getEndpoints().get(0);
    this.mapper = new YAMLMapper().enable(Feature.INDENT_ARRAYS_WITH_INDICATOR);
    this.mapper.setSerializationInclusion(Include.NON_NULL);
  }

  @Override
  protected void configure() {
    if (!configured.compareAndSet(false, true)) {
      return;
    }

    withCopyFileToContainer(
        MountableFile.forHostPath(tarantoolConfigPath), TARANTOOL_CONFIG_PATH.toString());

    this.tempConfigDirectory = Utils.createTempDirectory(UUID.randomUUID().toString());
    final Path pathToConfigurationFile = this.tempConfigDirectory.resolve("tcm-config.yaml");

    try {
      this.mapper.writeValue(pathToConfigurationFile.toFile(), this.tcmConfig);
    } catch (IOException e) {
      throw new ContainerLaunchException("Error writing TCM config to file", e);
    }

    withCopyFileToContainer(
        MountableFile.forHostPath(pathToConfigurationFile), TCM_CONFIG_PATH.toString());
    withCommand("./tcm -c " + TCM_CONFIG_PATH);
    withNetworkAliases(this.nodeName);
    withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(this.nodeName));
    withExposedPorts(this.tcmConfig.getHttp().getPort());
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    Utils.bindExposedPorts(this);
  }

  @Override
  public void close() {
    try {
      super.close();
    } finally {
      deleteDataDirectory(this.tempConfigDirectory);
    }
  }

  private static void deleteDataDirectory(Path dir) {
    if (dir == null || !Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }

    try (Stream<Path> stream = Files.walk(dir)) {
      stream
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  LOGGER.error("Error deleting file {}", path, e);
                }
              });
    } catch (IOException e) {
      LOGGER.error("Error walking directory {}", dir, e);
    }
  }

  public void publishConfig() {
    try {
      Utils.execExceptionally(
          LOGGER,
          this.getContainerInfo(),
          "TCM can't resolve etcd host",
          WAIT_ETCD_READY_SCRIPT,
          "25",
          this.etcdAddress);
      Utils.execExceptionally(
          LOGGER,
          this.getContainerInfo(),
          "TCM can't publish cluster config",
          TT_COMMAND,
          "cluster",
          "publish",
          this.etcdAddress + this.prefix,
          TNT_CONFIG_NAME,
          "--force");
      Utils.execExceptionally(
          LOGGER,
          this.getContainerInfo(),
          "TCM can't apply cluster configuration",
          WAIT_CONFIG_READY_SCRIPT,
          "25",
          this.etcdAddress + this.prefix);
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("Error publishing TCM config", e);
    }
  }

  public TCMConfig getConfig() {
    return this.tcmConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TCMContainer)) return false;
    TCMContainer that = (TCMContainer) o;
    return Objects.equals(nodeName, that.nodeName)
        && Objects.equals(tcmConfig, that.tcmConfig)
        && Objects.equals(tarantoolConfigPath, that.tarantoolConfigPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeName, tcmConfig, tarantoolConfigPath);
  }
}
