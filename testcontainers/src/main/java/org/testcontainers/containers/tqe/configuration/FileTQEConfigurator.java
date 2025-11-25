/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.containers.tarantool.Tarantool3WaitStrategy;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.testcontainers.containers.tarantool.config.tarantool3.HostPort;
import org.testcontainers.containers.tarantool.config.tarantool3.ParameterExtractor;
import org.testcontainers.containers.tcm.config.TCMConfig;
import org.testcontainers.containers.tqe.GrpcContainer;
import org.testcontainers.containers.tqe.GrpcContainerImpl;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.iproto.Iproto;
import io.tarantool.autogen.iproto.advertise.Advertise;
import io.tarantool.autogen.iproto.advertise.peer.Peer;
import io.tarantool.autogen.iproto.listen.Listen;

/**
 * Base implementation of {@link TQEConfigurator} that configure TQE cluster using configuration
 * files. All other implementations should extend this class.
 */
public class FileTQEConfigurator implements TQEConfigurator {

  /*
  /**********************************************************
  /* LOGGER
  /**********************************************************
  */
  private static final Logger LOGGER = LoggerFactory.getLogger(FileTQEConfigurator.class);

  /*
  /**********************************************************
  /* Constants
  /**********************************************************
  */
  private static final String CONFIGURATOR_ERROR_MSG =
      "An error occurred when configuring the TQE cluster. See logs for details.";

  private static final String TQE_ROUTER_ROLE = "app.roles.api";

  /*
  /**********************************************************
  /* Parameter extractors
  /**********************************************************
  */

  private final DockerImageName image;

  private final Path queueConfig;

  private final Collection<Path> grpcConfigs;

  private final String clusterName;

  private final Duration startupTimeout;

  private final Duration bootstrapTimeout;

  private final Map<String, TarantoolContainer<?>> queue;

  private final Map<String, GrpcContainer<?>> grpc;

  private final Set<String> routerNames;

  private final Tarantool3Configuration queueParsedConfig;

  private final Network network;

  private boolean configured;

  private FileTQEConfigurator(
      DockerImageName image,
      Path queueConfig,
      Collection<Path> grpcConfigs,
      String clusterName,
      Duration startupTimeout,
      Duration bootstrapTimeout) {
    this.queueConfig = queueConfig;
    this.grpcConfigs = grpcConfigs;
    this.clusterName = clusterName;
    this.bootstrapTimeout = bootstrapTimeout;
    this.routerNames = new LinkedHashSet<>(1);
    this.image = image;
    this.startupTimeout = startupTimeout;
    this.network = Network.newNetwork();
    this.queueParsedConfig = ConfigurationUtils.readFromFile(this.queueConfig);
    this.queue = initQueue(this.network, this.image, this.clusterName, this.startupTimeout);
    this.grpc =
        initGrpc(
            this.grpcConfigs,
            this.network,
            this.image,
            this.clusterName,
            this.queue,
            this.startupTimeout);
  }

  private Map<String, GrpcContainer<?>> initGrpc(
      Collection<Path> grpcConfigs,
      Network network,
      DockerImageName image,
      String clusterName,
      Map<String, TarantoolContainer<?>> queue,
      Duration startupTimeout) {
    Map<String, GrpcContainer<?>> grpc = new LinkedHashMap<>();
    int i = 1;
    for (Path grpcConfig : grpcConfigs) {
      final String grpcName = "grpc-" + i;
      final String containerName = grpcName + "-" + clusterName;
      GrpcContainerImpl container =
          new GrpcContainerImpl(image, grpcConfig, grpcName, startupTimeout)
              .withNetwork(network)
              .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName))
              .withNetworkAliases(containerName)
              .dependsOn(queue.values());
      grpc.put(grpcName, container);
    }
    return grpc;
  }

  private Map<String, TarantoolContainer<?>> initQueue(
      Network network, DockerImageName image, String clusterName, Duration startupTimeout) {

    final List<String> instances = ConfigurationUtils.parseInstances(this.queueParsedConfig);
    final Map<String, TarantoolContainer<?>> nodes = new LinkedHashMap<>(instances.size());

    this.routerNames.addAll(
        ConfigurationUtils.findInstancesWithRole(this.queueParsedConfig, TQE_ROUTER_ROLE));
    if (this.routerNames.isEmpty()) {
      LOGGER.error("At least one container must have the 'router' and '{}' roles", TQE_ROUTER_ROLE);
      throw new ContainerLaunchException(CONFIGURATOR_ERROR_MSG);
    }

    final Map<String, HostPort> advertiseClientUris =
        resolveAdvertiseClientUris(this.queueParsedConfig);
    final Map<String, HostPort> advertisePeerUris =
        resolveAdvertisePeerUris(this.queueParsedConfig);
    final Map<String, List<HostPort>> listenUri = resolveListenUris(this.queueParsedConfig);
    if (listenUri.size() != instances.size()) {
      LOGGER.error("All nodes should have 'listen.uri' parameter!");
      throw new ContainerLaunchException(CONFIGURATOR_ERROR_MSG);
    }

    for (String instance : instances) {
      final String containerName = instance + "-" + clusterName;
      final Tarantool3Container container =
          new Tarantool3Container(image, instance)
              .withNetwork(network)
              .withConfigPath(queueConfig)
              .withNetworkAliases(containerName)
              .withCreateContainerCmdModifier(cmd -> cmd.withUser("root").withName(containerName))
              .waitingFor(
                  new Tarantool3WaitStrategy(
                      instance,
                      TCMConfig.DEFAULT_TARANTOOL_USERNAME,
                      TCMConfig.DEFAULT_TARANTOOL_PASSWORD))
              .withStartupTimeout(startupTimeout)
              .withCommand("tarantool");

      final HostPort advertiseClientUri = advertiseClientUris.get(instance);
      if (advertiseClientUri != null) {
        setAliasAsNeedFromUri(container, instance, advertiseClientUri);
        setExposedPorts(container, advertiseClientUri);
      }

      final HostPort advertisePeerUri = advertisePeerUris.get(instance);
      if (advertisePeerUri != null) {
        setAliasAsNeedFromUri(container, instance, advertisePeerUri);
      }

      setAliasAsNeedFromUri(
          container,
          instance,
          listenUri.getOrDefault(instance, Collections.emptyList()).toArray(new HostPort[] {}));

      setExposedPorts(
          container,
          listenUri.getOrDefault(instance, Collections.emptyList()).toArray(new HostPort[] {}));
      nodes.put(instance, container);
    }
    return nodes;
  }

  private static void setExposedPorts(Tarantool3Container container, HostPort... uris) {
    if (uris == null) {
      return;
    }
    container.withExposedPorts(Arrays.stream(uris).map(HostPort::getPort).toArray(Integer[]::new));
  }

  private static Map<String, HostPort> resolveAdvertiseClientUris(Tarantool3Configuration config) {
    final var advertiseParameterExtractor =
        new ParameterExtractor<>(
            c -> c.getIproto().flatMap(Iproto::getAdvertise).flatMap(Advertise::getClient),
            g -> g.getIproto().flatMap(i -> i.getAdvertise()).flatMap(a -> a.getClient()),
            r -> r.getIproto().flatMap(i -> i.getAdvertise()).flatMap(a -> a.getClient()),
            in -> in.getIproto().flatMap(i -> i.getAdvertise()).flatMap(a -> a.getClient()),
            (glAddr, gAddr, rAddr, iAddr) -> {
              if (iAddr.isPresent()) {
                return iAddr;
              }

              if (rAddr.isPresent()) {
                return rAddr;
              }

              if (gAddr.isPresent()) {
                return gAddr;
              }

              return glAddr;
            });

    return advertiseParameterExtractor.getParameter(config).entrySet().stream()
        .map(e -> new SimpleEntry<>(e.getKey(), HostPort.parse(e.getValue())))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private static Map<String, HostPort> resolveAdvertisePeerUris(Tarantool3Configuration config) {
    final var advertisePeerUriExtractor =
        new ParameterExtractor<>(
            c ->
                c.getIproto()
                    .flatMap(Iproto::getAdvertise)
                    .flatMap(Advertise::getPeer)
                    .flatMap(Peer::getUri),
            g ->
                g.getIproto()
                    .flatMap(i -> i.getAdvertise())
                    .flatMap(a -> a.getPeer())
                    .flatMap(p -> p.getUri()),
            r ->
                r.getIproto()
                    .flatMap(i -> i.getAdvertise())
                    .flatMap(a -> a.getPeer())
                    .flatMap(p -> p.getUri()),
            in ->
                in.getIproto()
                    .flatMap(i -> i.getAdvertise())
                    .flatMap(a -> a.getPeer())
                    .flatMap(p -> p.getUri()),
            (glAddr, gAddr, rAddr, iAddr) -> {
              if (iAddr.isPresent()) {
                return iAddr;
              }

              if (rAddr.isPresent()) {
                return rAddr;
              }

              if (gAddr.isPresent()) {
                return gAddr;
              }

              return glAddr;
            });

    return advertisePeerUriExtractor.getParameter(config).entrySet().stream()
        .map(e -> new SimpleEntry<>(e.getKey(), HostPort.parse(e.getValue())))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private static Map<String, List<HostPort>> resolveListenUris(Tarantool3Configuration config) {
    final var listenUris =
        new ParameterExtractor<>(
            c ->
                c.getIproto()
                    .flatMap(Iproto::getListen)
                    .flatMap(
                        ll ->
                            Optional.of(
                                ll.stream()
                                    .map(Listen::getUri)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList()))),
            g ->
                g.getIproto()
                    .flatMap(i -> i.getListen())
                    .flatMap(
                        ll ->
                            Optional.of(
                                ll.stream()
                                    .map(l -> l.getUri())
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList()))),
            r ->
                r.getIproto()
                    .flatMap(i -> i.getListen())
                    .flatMap(
                        ll ->
                            Optional.of(
                                ll.stream()
                                    .map(l -> l.getUri())
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList()))),
            in ->
                in.getIproto()
                    .flatMap(i -> i.getListen())
                    .flatMap(
                        ll ->
                            Optional.of(
                                ll.stream()
                                    .map(l -> l.getUri())
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList()))),
            (glAddr, gAddr, rAddr, iAddr) -> {
              final List<String> allAddresses = new ArrayList<>();
              glAddr.ifPresent(allAddresses::addAll);
              gAddr.ifPresent(allAddresses::addAll);
              rAddr.ifPresent(allAddresses::addAll);
              iAddr.ifPresent(allAddresses::addAll);
              return allAddresses.isEmpty() ? Optional.empty() : Optional.of(allAddresses);
            });

    return listenUris.getParameter(config).entrySet().stream()
        .map(
            e ->
                new SimpleEntry<>(
                    e.getKey(),
                    e.getValue().stream().map(HostPort::parse).collect(Collectors.toList())))
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private static void setAliasAsNeedFromUri(
      TarantoolContainer<?> container, String instance, HostPort... uris) {
    if (uris == null) {
      return;
    }

    for (HostPort uri : uris) {
      final String host = uri.getHost();
      if (host.trim().isEmpty()) {
        LOGGER.error("Invalid URI (hostname): '{}' for instance: {}", uri, instance);
        throw new ContainerLaunchException(CONFIGURATOR_ERROR_MSG);
      }
      container.withNetworkAliases(host);
    }
  }

  @Override
  public String clusterName() {
    return this.clusterName;
  }

  @Override
  public Map<String, TarantoolContainer<?>> queue() {
    return this.queue;
  }

  @Override
  public Map<String, GrpcContainer<?>> grpc() {
    return this.grpc;
  }

  @Override
  public synchronized void configure() {
    final Entry<String, TarantoolContainer<?>> router =
        this.queue.entrySet().stream()
            .filter(e -> this.routerNames.contains(e.getKey()))
            .findFirst()
            .orElseThrow(
                () -> {
                  LOGGER.error("Can't find any router");
                  return new ContainerLaunchException(CONFIGURATOR_ERROR_MSG);
                });

    final HostPort hostForRouter =
        resolveAdvertiseClientUris(this.queueParsedConfig).entrySet().stream()
            .filter(e -> Objects.equals(e.getKey(), router.getKey()))
            .map(Entry::getValue)
            .findFirst()
            .orElseGet(
                () ->
                    resolveListenUris(this.queueParsedConfig).entrySet().stream()
                        .filter(e -> Objects.equals(e.getKey(), router.getKey()))
                        .filter(e -> !e.getValue().isEmpty())
                        .map(e -> e.getValue().get(0))
                        .findFirst()
                        .orElseThrow(() -> new ContainerLaunchException(CONFIGURATOR_ERROR_MSG)));

    ConfigurationUtils.bootstrap(
        hostForRouter,
        router.getValue(),
        this.bootstrapTimeout,
        TCMConfig.DEFAULT_TARANTOOL_USERNAME,
        TCMConfig.DEFAULT_TARANTOOL_PASSWORD);
    this.configured = true;
  }

  @Override
  public synchronized boolean isConfigured() {
    return this.configured;
  }

  @Override
  public synchronized void close() {
    queue().values().parallelStream().forEach(Startable::close);
    grpc().values().parallelStream().forEach(Startable::close);
    if (this.network != null) {
      this.network.close();
    }
  }

  public static Builder builder(
      DockerImageName image, Path queueConfig, Collection<Path> grpcConfigs) {
    return new Builder(image, queueConfig, grpcConfigs);
  }

  public static class Builder {

    private static final String DEFAULT_CLUSTER_NAME_PREFIX = "tqe-test";
    private final Path queueConfig;
    private final Collection<Path> grpcConfigs;
    private final DockerImageName image;
    private String clusterName;
    private Duration startupTimeout;
    private Duration bootstrapTimeout;

    public Builder(DockerImageName image, Path queueConfig, Collection<Path> grpcConfigs) {
      if (queueConfig == null || !Files.isRegularFile(queueConfig)) {
        LOGGER.error("Queue config file is invalid (null or not regular): {})", queueConfig);
        throw new IllegalArgumentException(CONFIGURATOR_ERROR_MSG);
      }

      if (grpcConfigs == null || grpcConfigs.isEmpty()) {
        LOGGER.error("Grpc config file is invalid (null or empty): {})", grpcConfigs);
        throw new IllegalArgumentException(CONFIGURATOR_ERROR_MSG);
      }

      grpcConfigs.forEach(
          grpcConfig -> {
            if (!Files.isRegularFile(grpcConfig)) {
              LOGGER.error("Grpc config file is invalid (not regular): {})", grpcConfig);
              throw new IllegalArgumentException(CONFIGURATOR_ERROR_MSG);
            }
          });
      this.image = image;
      this.queueConfig = queueConfig;
      this.grpcConfigs = new LinkedHashSet<>(grpcConfigs);
    }

    public Builder withClusterName(String clusterName) {
      final String defaultClusterName;
      if (clusterName == null || clusterName.trim().isEmpty()) {
        defaultClusterName = DEFAULT_CLUSTER_NAME_PREFIX + "-" + UUID.randomUUID();
        LOGGER.warn(
            "Cluster name is invalid (null or blank): {}. Set default cluster name: '{}'",
            clusterName,
            defaultClusterName);
      } else {
        defaultClusterName = clusterName;
      }
      this.clusterName = defaultClusterName;
      return this;
    }

    public Builder withStartupTimeout(Duration startupTimeout) {
      this.startupTimeout = startupTimeout;
      return this;
    }

    public Builder withBootstrapTimeout(Duration bootstrapTimeout) {
      this.bootstrapTimeout = bootstrapTimeout;
      return this;
    }

    public FileTQEConfigurator build() {
      final String clusterName =
          this.clusterName == null || this.clusterName.trim().isEmpty()
              ? DEFAULT_CLUSTER_NAME_PREFIX + "-" + UUID.randomUUID()
              : this.clusterName;
      final Duration startupTimeout =
          this.startupTimeout == null ? DEFAULT_STARTUP_TIMEOUT : this.startupTimeout;
      final Duration bootstrapTimeout =
          this.bootstrapTimeout == null ? DEFAULT_BOOTSTRAP_TIMEOUT : this.bootstrapTimeout;
      return new FileTQEConfigurator(
          this.image,
          this.queueConfig,
          this.grpcConfigs,
          clusterName,
          startupTimeout,
          bootstrapTimeout);
    }
  }
}
