/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg.configuration.impl.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.io.file.PathUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.tdg.TDGContainer;
import org.testcontainers.containers.tdg.cartridge.Topology;
import org.testcontainers.containers.tdg.cartridge.Topology.Replicaset;
import org.testcontainers.containers.tdg.cartridge.Topology.Server;
import org.testcontainers.containers.tdg.configuration.TDGConfigurator;
import org.testcontainers.containers.utils.HttpHost;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.DockerImageName;

/**
 * This class is an implementation of the {@link TDGConfigurator} interface, using configuration
 * files as the basis for configuring the TDG cluster. Implementation allows to pass configuration
 * directory, which has configuration files like {@code topology.yml}, {@code vshard_groups.yml},
 * {@code config.yml}, {@code model.avsc} etc.
 *
 * <p><b><i>Note:</i></b> TDG supports the following configuration directory hierarchy:
 *
 * <pre>{@code
 * +- <root_config_folder>/
 * |  +-  topology.yml // (required)
 * |  +-  model.avsc // (not required)
 * |  +-  config.yml // (not required)
 * |  +-  vshard_groups.yml // (not required)
 * |  +-  src/ // (not required)
 * |  |   \-  ...
 * }</pre>
 */
public class TDGFileConfigurator implements TDGConfigurator, AutoCloseable {

  /*
  /**********************************************************
  /* Constants
  /**********************************************************
  */
  public static final String DEFAULT_TOPOLOGY_FILE_NAME = "topology.yml";

  public static final String DEFAULT_CONFIG_FILE_NAME = "config.yml";

  private static final Logger LOGGER = LoggerFactory.getLogger(TDGFileConfigurator.class);

  private static final String DEFAULT_ASSEMBLY_QUERY =
      "mutation boot ($replicasets: [EditReplicasetInput!]) { "
          + "cluster { edit_topology(replicasets: $replicasets) { replicasets { uuid } } } }";

  private static final String DEFAULT_BOOTSTRAP_QUERY =
      "{\"query\": \"mutation { bootstrap_vshard }\"}";

  private static final String DEFAULT_GRAPHQL_ENDPOINT = "/admin/api";

  private static final String DEFAULT_CONFIG_ENDPOINT = "/admin/config";

  /*
  /**********************************************************
  /* MAPPERS
  /**********************************************************
  */
  private static final YAMLMapper YAML_MAPPER;

  private static final ObjectMapper JSON_MAPPER;

  /*
  /**********************************************************
  /* Instance fields
  /**********************************************************
  */
  private final Path rootConfigPath;

  private final Path configFilePath;

  private final String clusterName;

  private final CloseableHttpClient httpClient;

  private final String assemblyQuery;

  private final Set<TDGNodeInfo> nodes;

  private final Network network;

  private boolean configured;

  static {
    YAML_MAPPER = new YAMLMapper();
    YAML_MAPPER
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(Include.NON_NULL)
        .setSerializationInclusion(Include.NON_EMPTY);

    JSON_MAPPER = new ObjectMapper();
    JSON_MAPPER
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(Include.NON_NULL)
        .setSerializationInclusion(Include.NON_EMPTY);
  }

  public TDGFileConfigurator(
      Path rootConfigPath, DockerImageName tdgImageName, String clusterName) {
    this.rootConfigPath = rootConfigPath;

    final Path topologyFilePath = rootConfigPath.resolve(DEFAULT_TOPOLOGY_FILE_NAME);
    if (!topologyFilePath.toFile().exists() || topologyFilePath.toFile().isDirectory()) {
      throw new IllegalArgumentException(
          "Topology file does not exist or is a directory. Please check your topology "
              + "file in root configuration directory.");
    }
    this.configFilePath = rootConfigPath.resolve(DEFAULT_CONFIG_FILE_NAME);
    this.clusterName = clusterName;
    this.assemblyQuery = getAssemblyRequest(topologyFilePath);
    this.network = Network.newNetwork();
    this.nodes = resolveContainersInfo(topologyFilePath, tdgImageName, clusterName, this.network);
    this.httpClient = HttpClients.createDefault();
  }

  @Override
  public String clusterName() {
    return this.clusterName;
  }

  @Override
  public Map.Entry<String, TDGContainer<?>> core() {
    final Optional<TDGNodeInfo> coreOpt =
        this.nodes.stream().filter(info -> info.hasRole("core")).findAny();
    if (!coreOpt.isPresent()) {
      throw new RuntimeException(
          String.format(
              "The cluster [cluster name='%s'] must have a node with the 'core' role.",
              this.clusterName));
    }
    return new AbstractMap.SimpleEntry<>(
        coreOpt.get().getContainer().node(), coreOpt.get().getContainer());
  }

  @Override
  public Map<String, TDGContainer<?>> nodes() {
    return this.nodes.stream()
        .collect(Collectors.toMap(info -> info.getContainer().node(), TDGNodeInfo::getContainer));
  }

  @Override
  public synchronized void configure() {
    if (this.configured) {
      LOGGER.warn(
          "Cluster [cluster name='{}'] is already configured. Skipping...", this.clusterName);
      return;
    }

    final TDGContainer<?> core = core().getValue();
    LOGGER.info(
        "'Assembling' step of cluster[cluster name='{}'] configuration is starting...",
        this.clusterName);
    assembly(core);

    LOGGER.info(
        "'Bootstrapping' step of cluster[cluster name='{}'] configuration is starting...",
        this.clusterName);
    bootstrap(core);

    LOGGER.info(
        "'Configuring' step of cluster[cluster name='{}'] configuration is starting...",
        this.clusterName);
    zipAndSendConfiguration(core);

    this.configured = true;
    LOGGER.info("Cluster[cluster name='{}'] is configured", this.clusterName);
  }

  @Override
  public synchronized boolean configured() {
    return this.configured;
  }

  /**
   * Applies assembly of TDG cluster, sending topology to container (node) that has a {@code core}
   * role via graphql mutation.
   *
   * @param core container that has a {@code core} role.
   */
  public void assembly(TDGContainer<?> core) {
    try {
      this.httpClient.execute(
          createHttpPost(this.assemblyQuery, core),
          response -> {
            final String responseAsString = EntityUtils.toString(response.getEntity());
            if (response.getCode() != HttpStatus.SC_OK || responseAsString.contains("exceptions")) {
              LOGGER.error("Failed to execute TDG assembly cluster: \n{}", responseAsString);
              throw new ContainerLaunchException(
                  "TDG assembly request failed. See logs for details.");
            }
            LOGGER.info("TDG assembly request succeeded: {}", responseAsString);
            return null;
          });
    } catch (Exception e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    }
  }

  /**
   * Compresses and sends configuration directory to container that has a {@code core} role.
   *
   * <p><b><i>Note:</i></b> implementations must use compression with {@code ZIP} format.
   *
   * <p><b><i>Note:</i></b> TDG supports the following configuration directory hierarchy:
   *
   * <pre>{@code
   * +- <your_config_folder>/
   * |  +-  model.avsc
   * |  +-  config.yml
   * |  +-  topology.yml
   * |  +-  vshard_groups.yml
   * |  +-  src/
   * |  |   \-  ...
   * }</pre>
   *
   * @param core container that has a {@code core} role.
   * @see <a href="https://www.tarantool.io/ru/tdg/latest/reference/config/">Tarantool Data Grid
   *     documentation</a>
   */
  public void zipAndSendConfiguration(TDGContainer<?> core) {
    if (!this.configFilePath.toFile().exists() || this.configFilePath.toFile().isDirectory()) {
      LOGGER.warn(
          "Configuration file '{}' does not exist or is a directory. Skipping zip and send"
              + " configuration...",
          this.configFilePath.toFile());
      return;
    }
    final Path tmpDir = Utils.createTempDirectory("tmp_config");
    final Path tmpZipConfig = tmpDir.resolve("config.zip");

    try {
      // zipping
      Utils.zipDirectory(this.rootConfigPath, tmpZipConfig);

      final String address = HttpHost.unsecure(core.httpMappedAddress()) + DEFAULT_CONFIG_ENDPOINT;
      final HttpPut httpPut = new HttpPut(address);
      final FileEntity entity =
          new FileEntity(tmpZipConfig.toFile(), ContentType.APPLICATION_OCTET_STREAM);
      httpPut.setEntity(entity);

      this.httpClient.execute(
          httpPut,
          response -> {
            final String responseAsString = EntityUtils.toString(response.getEntity());
            if (response.getCode() != HttpStatus.SC_OK || responseAsString.contains("exceptions")) {
              LOGGER.error("Failed to upload TDG cluster configuration: \n{}", responseAsString);
              throw new ContainerLaunchException(
                  "TDG cluster configuration upload failed. See logs for details.");
            }
            LOGGER.info("TDG cluster configuration upload succeeded: {}", responseAsString);
            return null;
          });
    } catch (Exception e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    } finally {
      try {
        PathUtils.deleteDirectory(tmpDir);
      } catch (IOException e) {
        LOGGER.error("Failed to delete temporary directory {}", tmpDir, e);
      }
    }
  }

  /**
   * Bootstrap TDG cluster using container that has {@code core} role via send graphql request.
   *
   * @param core container (node) tha has a {@code core} role.
   */
  public void bootstrap(TDGContainer<?> core) {
    try {
      this.httpClient.execute(
          createHttpPost(DEFAULT_BOOTSTRAP_QUERY, core),
          response -> {
            final String responseAsString = EntityUtils.toString(response.getEntity());
            if (response.getCode() != HttpStatus.SC_OK
                || responseAsString.contains("exceptions")
                || responseAsString.contains("errors")) {
              LOGGER.error("Failed to execute TDG bootstrap cluster: \n{}", responseAsString);
              throw new ContainerLaunchException(
                  "TDG bootstrap request failed. See logs for details.");
            }
            LOGGER.info("TDG bootstrap request succeeded: {}", responseAsString);
            return null;
          });
    } catch (Exception e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    }
  }

  @Override
  public void close() throws Exception {
    if (this.nodes != null) {
      nodes.parallelStream().forEach(TDGNodeInfo::close);
    }
    if (this.httpClient != null) {
      this.httpClient.close();
    }
    if (this.network != null) {
      this.network.close();
    }
  }

  private static HttpPost createHttpPost(String content, TDGContainer<?> core) {
    final String address = HttpHost.unsecure(core.httpMappedAddress()) + DEFAULT_GRAPHQL_ENDPOINT;
    final HttpPost httpPost = new HttpPost(address);
    httpPost.setHeader("Content-Type", "application/json");

    final HttpEntity stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
    httpPost.setEntity(stringEntity);
    return httpPost;
  }

  private static Set<TDGNodeInfo> resolveContainersInfo(
      Path topologyFilePath, DockerImageName imageName, String clusterName, Network network) {
    try {
      final Topology topology = YAML_MAPPER.readValue(topologyFilePath.toFile(), Topology.class);
      final Map<UUID, List<String>> serversByReplicasetUuid =
          topology.getServers().values().stream()
              .filter(s -> !s.getDisabled())
              .collect(
                  Collectors.groupingBy(
                      Server::getReplicasetUuid,
                      Collectors.mapping(Server::getUri, Collectors.toList())));

      final Set<TDGNodeInfo> containers = new HashSet<>();
      for (Entry<UUID, Replicaset> entry : topology.getReplicasets().entrySet()) {
        final List<String> uris = serversByReplicasetUuid.get(entry.getKey());
        if (uris == null) {
          throw new IllegalStateException("Can't find server for replicaset " + entry.getKey());
        }
        for (String uri : uris) {
          containers.add(
              new TDGNodeInfo(
                  entry.getValue(), entry.getKey(), uri, clusterName, imageName, network));
        }
      }
      return containers;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO переписать через pojo или использования graphql client с предгенерацией классов
  // https://netflix.github.io/dgs/advanced/java-client/
  private String getAssemblyRequest(Path topologyFilePath) {
    try {
      final Topology topology = YAML_MAPPER.readValue(topologyFilePath.toFile(), Topology.class);
      final Map<String, Object> request = new HashMap<>();
      request.put("query", DEFAULT_ASSEMBLY_QUERY);

      final Map<String, Object> variables = new HashMap<>();
      final List<Object> replicasets = new ArrayList<>();
      topology
          .getReplicasets()
          .forEach(
              (uuid, rs) -> {
                final Map<String, Object> replicaset = new HashMap<>();
                replicaset.put("alias", rs.getAlias());
                replicaset.put(
                    "roles",
                    rs.getRoles().entrySet().stream()
                        .filter(Entry::getValue)
                        .map(Entry::getKey)
                        .collect(Collectors.toList()));
                replicaset.put("uuid", uuid);
                final List<Object> joinServers = new ArrayList<>();
                topology.getServers().entrySet().stream()
                    .filter(e -> !e.getValue().getDisabled())
                    .forEach(
                        (e) -> {
                          if (Objects.equals(uuid, e.getValue().getReplicasetUuid())) {
                            joinServers.add(
                                new HashMap<String, Object>() {
                                  {
                                    put("uri", e.getValue().getUri());
                                    put("uuid", e.getKey());
                                  }
                                });
                          }
                        });
                replicaset.put("join_servers", joinServers);
                replicasets.add(replicaset);
              });
      variables.put("replicasets", replicasets);
      request.put("variables", variables);

      return JSON_MAPPER.writeValueAsString(request);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
