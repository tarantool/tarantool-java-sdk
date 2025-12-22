/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdContainer;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.containers.tarantool.Tarantool3WaitStrategy;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.testcontainers.containers.tarantool.config.tarantool3.HostPort;
import org.testcontainers.containers.tcm.TCMContainer;
import org.testcontainers.containers.tcm.config.TCMConfig;
import org.testcontainers.containers.utils.HttpHost;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.DockerImageName;

import io.tarantool.autogen.Tarantool3Configuration;

public class TDB2ClusterImpl implements TDBCluster {

  private static final Logger LOGGER = LoggerFactory.getLogger(TDB2ClusterImpl.class);

  public static final int DEFAULT_IPROTO_TARANTOOL_PORT = 3301;

  public static final String DEFAULT_TEST_SUPER_USER = TCMConfig.DEFAULT_TARANTOOL_USERNAME;

  public static final CharSequence DEFAULT_TEST_SUPER_USER_PWD = TCMConfig.DEFAULT_TARANTOOL_PASSWORD;

  public static final String TDB_PREFIX = TCMConfig.DEFAULT_TDB_PREFIX;

  private final String clusterName;

  private final String image;

  private final Network network;

  private final Path migrationsDirectory;

  private final Duration startupTimeout;

  private final String etcdContainerName;

  private final HttpHost etcdHttpAddress;

  private final String tcmContainerName;

  private final AtomicBoolean started = new AtomicBoolean();

  private final EtcdContainer etcd;

  private final Map<String, TarantoolContainer<?>> nodes;

  private String configuration;

  private final Tarantool3Configuration parsedConfiguration;

  private final TCMContainer tcm;

  private final Path pathToTarantoolConfig;

  private final Map<String, TarantoolContainer<?>> routers;

  private final Map<String, TarantoolContainer<?>> replicas;

  TDB2ClusterImpl(String image, Path migrationsDirectory, Duration startupTimeout, int routerCount, int shardCount,
      int replicaCount, String configuration) {
    this.clusterName = UUID.randomUUID().toString();
    this.etcdContainerName = "ETCD-" + this.clusterName;
    this.tcmContainerName = "TCM-" + this.clusterName;
    this.etcdHttpAddress = HttpHost.unsecure(etcdContainerName, Etcd.ETCD_CLIENT_PORT);
    this.image = image;
    this.network = Network.newNetwork();
    this.migrationsDirectory = migrationsDirectory;
    this.startupTimeout = startupTimeout;

    this.pathToTarantoolConfig = Utils.createTempDirectory(this.clusterName).resolve("config.yaml");
    this.configuration = configuration;
    if (configuration == null) {
      this.parsedConfiguration = ConfigurationUtils.generateSimpleConfiguration(routerCount, shardCount, replicaCount);
      this.configuration = ConfigurationUtils.writeAsString(this.parsedConfiguration);
    } else {
      this.parsedConfiguration = ConfigurationUtils.create(configuration);
    }

    final List<String> instancesNames = ConfigurationUtils.parseInstances(this.parsedConfiguration);
    this.nodes = prepareNodes(instancesNames, this.network, this.etcdHttpAddress, this.image, this.clusterName,
        this.startupTimeout, this.migrationsDirectory, this.pathToTarantoolConfig);

    this.etcd = getEtcdContainer();
    this.tcm = getTcmContainer();
    this.tcm.dependsOn(this.etcd);

    final List<String> routerInstances = ConfigurationUtils.findInstancesWithRole(this.parsedConfiguration,
        "roles.crud-router");
    this.routers = getNodeByPredicate(this.nodes, e -> routerInstances.contains(e.getKey()));
    this.replicas = getNodeByPredicate(this.nodes, e -> !routerInstances.contains(e.getKey()));
  }

  @Override
  public String clusterName() {
    return this.clusterName;
  }

  @Override
  public EtcdContainer etcdContainer() {
    return this.etcd;
  }

  @Override
  public TCMContainer tcmContainer() {
    return this.tcm;
  }

  @Override
  public Map<String, TarantoolContainer<?>> routers() {
    return this.routers;
  }

  @Override
  public Map<String, TarantoolContainer<?>> storages() {
    return this.replicas;
  }

  @Override
  public Map<String, TarantoolContainer<?>> nodes() {
    return this.nodes;
  }

  @Override
  public void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }

    try {
      Files.write(this.pathToTarantoolConfig, configuration.getBytes());
    } catch (IOException e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    }

    this.etcd.start();
    this.tcm.start();
    this.tcm.publishConfig();

    final CountDownLatch latch = new CountDownLatch(this.nodes.size());
    final AtomicReference<Exception> failedToStart = new AtomicReference<>();
    for (TarantoolContainer<?> container : this.nodes.values()) {
      new Thread(() -> {
        try {
          container.start();
        } catch (Exception e) {
          failedToStart.set(e);
        } finally {
          latch.countDown();
        }
      }).start();
    }
    try {
      boolean await = latch.await(this.startupTimeout.getSeconds(), TimeUnit.SECONDS);
      if (!await) {
        throw new ContainerLaunchException("Replicaset nodes doesn't started");
      }
    } catch (InterruptedException e) {
      throw new ContainerLaunchException(e.getMessage());
    }

    if (failedToStart.get() != null) {
      throw new ContainerLaunchException("Cluster failed to start", failedToStart.get());
    }

    waitUntilNodesIsReady(this.nodes, this.startupTimeout);

    final Entry<String, TarantoolContainer<?>> router = this.routers.entrySet().stream()
        .findFirst().orElseThrow(() -> new ContainerLaunchException("Can't find any router"));

    ConfigurationUtils.bootstrap(
        new HostPort(router.getKey(), DEFAULT_IPROTO_TARANTOOL_PORT),
        router.getValue(),
        this.startupTimeout,
        TCMConfig.DEFAULT_TARANTOOL_USERNAME,
        TCMConfig.DEFAULT_TARANTOOL_PASSWORD
    );

    waitUntilNodesIsReady(this.nodes, this.startupTimeout);

    applyMigrations(this.routers, this.migrationsDirectory, this.etcdHttpAddress);
    // binds here because etch external library container
    Utils.bindExposedPorts(this.etcd);
  }

  @Override
  public void stop() {
    if (!this.started.compareAndSet(true, true)) {
      return;
    }
    if (this.etcd != null) {
      this.etcd.close();
    }
    if (this.tcm != null) {
      this.tcm.close();
    }

    if (this.nodes != null && !this.nodes.isEmpty()) {
      this.nodes.values().parallelStream().forEach(TarantoolContainer::stop);
    }

    if (this.network != null) {
      this.network.close();
    }
    this.started.set(false);
  }

  private EtcdContainer getEtcdContainer() {
    return new EtcdContainer(Utils.resolveContainerImage("ETCD_IMAGE", Etcd.CONTAINER_IMAGE), this.etcdContainerName,
        new ArrayList<>())
        .withNetwork(this.network)
        .withStartupTimeout(this.startupTimeout)
        .withShouldMountDataDirectory(false)
        .withNetworkAliases(this.etcdContainerName)
        .withCreateContainerCmdModifier(cmd -> cmd.withName(this.etcdContainerName))
        .withPrivilegedMode(true);
  }

  private TCMContainer getTcmContainer() {
    return new TCMContainer(
        this.image,
        this.tcmContainerName,
        TCMConfig.builder().withEtcdAddress(this.etcdHttpAddress.string().toString()).build(),
        this.pathToTarantoolConfig
    )
        .withStartupTimeout(this.startupTimeout)
        .withNetwork(this.network)
        .withCreateContainerCmdModifier(cmd -> cmd.withName(this.tcmContainerName))
        .withPrivilegedMode(true);
  }

  public String getConfiguration() {
    return configuration;
  }

  private static void applyMigrations(Map<String, TarantoolContainer<?>> routers, Path migrationsDirectory,
      HttpHost etcdHttpAddress) {
    if (migrationsDirectory != null) {
      final TarantoolContainer<?> router = routers.values().iterator().next();
      try {
        Utils.execExceptionally(LOGGER, router.getContainerInfo(),
            "tt migrations publish failed", "tt", "migrations", "publish",
            etcdHttpAddress.string() + TDB_PREFIX,
            TarantoolContainer.DEFAULT_DATA_DIR.resolve(migrationsDirectory.getFileName()).toAbsolutePath().toString());
        Utils.execExceptionally(LOGGER, router.getContainerInfo(),
            "tt migrations apply failed", "tt", "migrations", "apply",
            etcdHttpAddress.string() + TDB_PREFIX);
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void waitUntilNodesIsReady(Map<String, TarantoolContainer<?>> nodes, Duration startupTimeout) {
    try {
      Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
        boolean isReady = true;
        for (Entry<String, TarantoolContainer<?>> e : nodes.entrySet()) {
          isReady = isReady &&
              e.getValue().execInContainer("/bin/sh", "-c",
                      "tools/client/wait_instance_ready.sh 1 " + e.getKey() + ":" + DEFAULT_IPROTO_TARANTOOL_PORT)
                  .getExitCode() == 0;
        }
        return isReady;
      });
    } catch (TimeoutException exc) {
      throw new ContainerLaunchException("Timed out waiting for cluster nodes to start", exc);
    }
  }

  private static Map<String, TarantoolContainer<?>> getNodeByPredicate(Map<String,
      TarantoolContainer<?>> nodes, Predicate<Entry<String, TarantoolContainer<?>>> predicate) {
    return nodes.entrySet().stream().filter(predicate)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<String, TarantoolContainer<?>> prepareNodes(List<String> instancesNames,
      Network network, HttpHost etcdHttpAddress, String image, String clusterName, Duration startupTimeout,
      Path migrationsDirectory, Path configPath) {
    final Map<String, TarantoolContainer<?>> nodes = new ConcurrentHashMap<>();

    instancesNames.forEach(instance -> {
      final String instanceNameAlias = instance + "-" + clusterName;
      final TarantoolContainer<Tarantool3Container> container = new Tarantool3Container(DockerImageName.parse(image),
          instance)
          .withConfigPath(configPath)
          .withNetwork(network)
          .withEnv("TT_CLI_USERNAME", DEFAULT_TEST_SUPER_USER)
          .withEnv("TT_CLI_PASSWORD", DEFAULT_TEST_SUPER_USER_PWD.toString())
          .withEtcdAddresses(etcdHttpAddress)
          .withEtcdPrefix(TDB_PREFIX)
          .withEnv("TT_CONFIG_ETCD_HTTP_REQUEST_TIMEOUT", "3")
          .withNetworkAliases(instanceNameAlias)
          .withCreateContainerCmdModifier(cmd -> cmd.withName(instanceNameAlias))
          .withStartupTimeout(startupTimeout)
          .withPrivilegedMode(true)
          .waitingFor(new Tarantool3WaitStrategy(instance, DEFAULT_TEST_SUPER_USER, DEFAULT_TEST_SUPER_USER_PWD));

      if (migrationsDirectory != null) {
        container.withMigrationsPath(migrationsDirectory);
      }
      nodes.put(instance, container);
    });
    return nodes;
  }

  public static Builder builder(String image) {
    return new Builder(image);
  }

  public static class Builder {

    private final String image;

    private Path migrationsDirectory;

    private Duration startupTimeout = Duration.ofMinutes(1);

    private String configuration;

    private int routerCount = 1;

    private int replicaCount = 1;

    private int shardCount = 1;

    Builder(String image) {
      this.image = image;
    }

    public Builder withRouterCount(int routerCount) {
      assert routerCount > 0;
      this.routerCount = routerCount;
      return this;
    }

    public Builder withReplicaCount(int replicaCount) {
      assert replicaCount > 0;
      this.replicaCount = replicaCount;
      return this;
    }

    public Builder withShardCount(int shardCount) {
      assert shardCount > 0;
      this.shardCount = shardCount;
      return this;
    }

    public Builder withMigrationsDirectory(Path migrationsDirectory) {
      this.migrationsDirectory = migrationsDirectory;
      return this;
    }

    public Builder withStartupTimeout(Duration startupTimeout) {
      assert startupTimeout != null;
      this.startupTimeout = startupTimeout;
      return this;
    }

    public Builder withTDB2Configuration(String configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder withTDB2Configuration(Tarantool3Configuration configuration) {
      this.configuration = ConfigurationUtils.writeAsString(configuration);
      return this;
    }

    public TDB2ClusterImpl build() {
      return new TDB2ClusterImpl(this.image, this.migrationsDirectory, this.startupTimeout, this.routerCount,
          this.shardCount, this.replicaCount, this.configuration);
    }
  }
}
