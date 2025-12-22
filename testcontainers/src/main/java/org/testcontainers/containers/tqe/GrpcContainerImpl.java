/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.testcontainers.containers.tarantool.config.tarantool3.HostPort;
import org.testcontainers.containers.tqe.configuration.grpc.ConsumerConfig;
import org.testcontainers.containers.tqe.configuration.grpc.GrpcConfiguration;
import org.testcontainers.containers.tqe.configuration.grpc.GrpcListen;
import org.testcontainers.containers.tqe.configuration.grpc.PublisherConfig;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class GrpcContainerImpl extends GenericContainer<GrpcContainerImpl>
    implements GrpcContainer<GrpcContainerImpl> {

  /*
  /**********************************************************
  /* Logger
  /**********************************************************
  */
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcContainerImpl.class);

  /*
  /**********************************************************
  /* Constants
  /**********************************************************
  */
  private static final String GRPC_ERROR_MSG =
      "Grpc node configuration is invalid. See logs for details.";

  /*
  /**********************************************************
  /* fields
  /**********************************************************
  */
  private final Path configPath;

  private final String node;

  private final Set<GrpcRole> roles;

  private final Duration startupTimeout;

  private int corePort;

  private final Set<Integer> grpcPorts;

  private boolean started;

  public GrpcContainerImpl(
      DockerImageName image, Path configPath, String node, Duration startupTimeout) {
    super(image);
    validateConfigPath(configPath);
    this.configPath = configPath;
    this.node = node;
    this.startupTimeout = startupTimeout;
    final GrpcConfiguration parsedConfig = ConfigurationUtils.readGrpcFromFile(configPath);
    this.roles = resolveRoles(parsedConfig, configPath);
    this.corePort = resolveCorePort(parsedConfig, configPath);
    this.grpcPorts = resolveGrpcPorts(parsedConfig, configPath);
  }

  @Override
  public synchronized void start() {
    if (this.started) {
      return;
    }
    LOGGER.info("Try start Grpc container [{}]...", this.node);
    super.start();
    this.started = true;
    LOGGER.info("Grpc container [{}] is started", this.node);
  }

  @Override
  public synchronized void stop() {
    if (!this.started) {
      return;
    }
    LOGGER.info("Grpc container [{}] is stopping", this.node);
    super.stop();
    this.started = false;
    LOGGER.info("Grpc container [{}] is stopped", this.node);
  }

  @Override
  protected void configure() {
    final Set<Integer> allExposedPorts = new LinkedHashSet<>(this.grpcPorts);
    allExposedPorts.add(this.corePort);
    withExposedPorts(allExposedPorts.toArray(new Integer[] {}));
    final String CONFIG_PATH_IN_CONTAINER =
        DEFAULT_TQE_DATA_DIR.toAbsolutePath().resolve(this.configPath.getFileName()).toString();
    withCopyFileToContainer(MountableFile.forHostPath(this.configPath), CONFIG_PATH_IN_CONTAINER);
    withNetworkAliases(this.node);
    withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(this.node));
    withCreateContainerCmdModifier(cmd -> cmd.withUser("root"));
    withPrivilegedMode(true);
    final int[] waitingPorts = getExposedPorts().stream().mapToInt(Integer::intValue).toArray();
    waitingFor(new HostPortWaitStrategy().forPorts(waitingPorts));
    withStartupTimeout(this.startupTimeout);
    withCommand("./message-queue-ee", "-config=%s".formatted(CONFIG_PATH_IN_CONTAINER));
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    Utils.bindExposedPorts(this);
  }

  @Override
  public String node() {
    return this.node;
  }

  @Override
  public Set<GrpcRole> roles() {
    return new LinkedHashSet<>(this.roles);
  }

  @Override
  public Set<InetSocketAddress> grpcAddresses() {
    return grpcAddresses(getHost());
  }

  @Override
  public Set<InetSocketAddress> grpcAddresses(String customHost) {
    return this.grpcPorts.stream()
        .map(port -> new InetSocketAddress(customHost, getMappedPort(port)))
        .collect(Collectors.toSet());
  }

  @Override
  public InetSocketAddress coreAddress() {
    return coreAddress(getHost());
  }

  @Override
  public InetSocketAddress coreAddress(String customHost) {
    return new InetSocketAddress(customHost, getMappedPort(this.corePort));
  }

  private static void validateConfigPath(Path configPath) {
    if (configPath == null
        || !Files.exists(configPath)
        || !Files.isRegularFile(configPath)
        || configPath.endsWith(".yml")) {
      LOGGER.error(
          "Invalid config file. Config path is null or not exists or not regular or not having"
              + " '.yml' extension: {}",
          configPath);
      throw new ContainerLaunchException(GRPC_ERROR_MSG);
    }
  }

  /** Gets 'core_port' parameter (required) from configuration. */
  private static int resolveCorePort(GrpcConfiguration config, Path configPath) {
    return config
        .getCorePort()
        .orElseThrow(
            () -> {
              LOGGER.error("'core_port' not specified in: {}", configPath);
              return new ContainerLaunchException(GRPC_ERROR_MSG);
            })
        .intValue();
  }

  /**
   * Gets all grpc ports from configuration. If 'grpc_port' parameter is not specified (deprecated),
   * finds ports in 'grpc_listen.[n].uri' parameters (required)
   */
  private static Set<Integer> resolveGrpcPorts(GrpcConfiguration config, Path configPath) {
    final Set<Integer> grpcPorts = new LinkedHashSet<>();
    config
        .getGrpcPort()
        .ifPresentOrElse(
            a ->
                LOGGER.warn(
                    "'grpc_port' parameter is deprecate. Use 'listen.uri' parameters! Skipping..."),
            () -> LOGGER.warn("'grpc_port' is not specified. Try parse listen parameter..."));

    final Set<GrpcListen> grpcListen = config.getGrpcListen().orElseGet(Set::of);
    final Set<Integer> listenPorts =
        grpcListen.stream()
            .filter(Objects::nonNull)
            .map(GrpcListen::getGrpcHost)
            .filter(Optional::isPresent)
            .map(s -> HostPort.parse(s.get()).getPort())
            .collect(Collectors.toSet());

    grpcPorts.addAll(listenPorts);

    if (grpcPorts.isEmpty()) {
      LOGGER.error(
          "No 'grpc_listen' uris parameters or grpc port parameter configuration specified in: {}",
          configPath);
      throw new ContainerLaunchException(GRPC_ERROR_MSG);
    }
    return grpcPorts;
  }

  /**
   * Gets all grpc roles for this node. At least one of roles must be specified in configuration
   * (required).
   */
  private static Set<GrpcRole> resolveRoles(GrpcConfiguration config, Path configPath) {
    final Optional<Boolean> isPublisher =
        config.getPublisher().flatMap(PublisherConfig::getEnabled);
    final Set<GrpcRole> roles = new LinkedHashSet<>();
    if (isPublisher.isPresent() && isPublisher.get()) {
      roles.add(GrpcRole.PUBLISHER);
      LOGGER.trace("Publisher role is enabled for: {}", configPath);
    }

    final Optional<Boolean> isConsumer = config.getConsumer().flatMap(ConsumerConfig::getEnabled);
    if (isConsumer.isPresent() && isConsumer.get()) {
      roles.add(GrpcRole.CONSUMER);
      LOGGER.trace("Consumer role is enabled for: {}", configPath);
    }

    if (roles.isEmpty()) {
      LOGGER.error("No roles configured for: {}", configPath);
      throw new ContainerLaunchException(GRPC_ERROR_MSG);
    }
    return roles;
  }
}
