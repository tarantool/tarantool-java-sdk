/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.Utils;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.PathUtils;

/** Base implementation of {@link TDGContainer} interface. */
public class TDGContainerImpl extends GenericContainer<TDGContainerImpl>
    implements TDGContainer<TDGContainerImpl> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TDGContainerImpl.class);

  private static final int DEFAULT_DELAY_AFTER_START_IN_SECONDS = 1;

  // We should not close the network inside this class because we cannot close the network before
  // the container in it
  private final Network network;

  private final String node;

  private final String advertiseUri;

  private final String[] aliases;

  private final int exposedHttpPort;

  private final int exposedIProtoPort;

  private final Path dataPath;

  private final Duration startupTimeout;

  private boolean configured;

  private boolean isClosed;

  TDGContainerImpl(
      DockerImageName dockerImageName,
      Network network,
      String node,
      Path dataPath,
      String advertiseUri,
      String[] aliases,
      Duration startupTimeout) {
    super(dockerImageName);
    this.network = network;
    this.exposedHttpPort = DEFAULT_HTTP_PORT;
    this.exposedIProtoPort =
        advertiseUri == null
            ? TarantoolContainer.DEFAULT_TARANTOOL_PORT
            : Integer.parseInt(advertiseUri.split(":")[1]);
    this.node = node;
    this.dataPath = dataPath;
    this.advertiseUri = advertiseUri;
    this.aliases = aliases;
    this.startupTimeout = startupTimeout;
    this.isClosed = false;
  }

  @Override
  public synchronized void start() {
    if (this.isClosed) {
      throw new IllegalStateException("Container is already closed. Please create new container");
    }
    LOGGER.info("Try start TDG container [{}]...", this.node);
    super.start();
    try {
      TimeUnit.SECONDS.sleep(DEFAULT_DELAY_AFTER_START_IN_SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    LOGGER.info("TDG container [{}] is started", this.node);
  }

  @Override
  public synchronized void stop() {
    if (this.isClosed) {
      LOGGER.warn(
          "TDG container [{}] is already stopped without save mount. Skipping...", this.node);
      return;
    }
    LOGGER.info("TDG container [{}] is stopping...", this.node);
    PathUtils.recursiveDeleteDir(this.dataPath.toAbsolutePath());
    super.stop();
    this.isClosed = true;
    LOGGER.info("TDG container [{}] is stopped", this.node);
  }

  @Override
  public synchronized void stopWithSafeMount() {
    if (this.isClosed) {
      return;
    }
    LOGGER.info("TDG container [{}] is stopping with save mount...", this.node);
    super.stop();
    LOGGER.info("TDG container [{}] is stopped with save mount", this.node);
  }

  @Override
  public String node() {
    return this.node;
  }

  @Override
  public synchronized InetSocketAddress httpMappedAddress() {
    return new InetSocketAddress(getHost(), getMappedPort(this.exposedHttpPort));
  }

  @Override
  public synchronized InetSocketAddress iprotoMappedAddress() {
    return new InetSocketAddress(getHost(), getMappedPort(this.exposedIProtoPort));
  }

  @Override
  protected void configure() {
    try {
      if (this.configured) {
        LOGGER.warn("TDG container [{}] is already configured. Skipping...", this.node);
        return;
      }

      LOGGER.info("TDG container [{}] is configuring...", this.node);
      if (this.startupTimeout != null) {
        withStartupTimeout(this.startupTimeout);
      }

      withExposedPorts(this.exposedHttpPort, this.exposedIProtoPort);
      withNetwork(this.network);
      addFileSystemBind(
          this.dataPath.toAbsolutePath().toString(),
          DEFAULT_TDG_DATA_DIR.toAbsolutePath().toString(),
          BindMode.READ_WRITE,
          SelinuxContext.SHARED);
      withNetworkAliases(this.node);
      withNetworkAliases(this.aliases);
      if (this.advertiseUri != null) {
        withNetworkAliases(this.advertiseUri.split(":")[0]);
      }
      withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(this.node));
      withEnv("TT_INSTANCE_NAME", this.node);
      withEnv(
          "TARANTOOL_ADVERTISE_URI",
          this.advertiseUri == null
              ? this.node + ":" + TarantoolContainer.DEFAULT_TARANTOOL_PORT
              : this.advertiseUri);
      withCreateContainerCmdModifier(cmd -> cmd.withName(this.node).withUser("root"));
      withPrivilegedMode(true);

      LOGGER.info("TDG container [{}] is configured", this.node);
      this.configured = true;
    } catch (Exception e) {
      throw new ContainerLaunchException(e.getMessage(), e);
    }
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    Utils.bindExposedPorts(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TDGContainerImpl that = (TDGContainerImpl) o;
    return Objects.equals(network, that.network)
        && Objects.equals(node, that.node)
        && Objects.equals(advertiseUri, that.advertiseUri)
        && Objects.deepEquals(aliases, that.aliases)
        && Objects.equals(dataPath, that.dataPath)
        && Objects.equals(startupTimeout, that.startupTimeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        network,
        node,
        advertiseUri,
        Arrays.hashCode(aliases),
        dataPath,
        startupTimeout);
  }

  /**
   * Creates builder to instantiate {@link TDGContainerImpl}.
   *
   * @param image image name of {@code TDG} which must be reached by docker client
   * @return builder of {@link TDGContainerImpl}
   */
  public static Builder builder(DockerImageName image) {
    return new Builder(image);
  }

  public static class Builder {

    /** Default node name prefix, when {@link #withNode(String)} isn't called. */
    private static final String DEFAULT_TDG_PREFIX_NAME = "tdg";

    private static final Pattern URI_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]*:\\d+$");

    private final DockerImageName image;

    private Network network;

    private UUID uuid;

    private String node;

    private String advertiseUri;

    private String[] aliases = new String[0];

    private Path dataPath;

    private Duration startupTimeout;

    public Builder(DockerImageName image) {
      this.image = image;
      this.uuid = UUID.randomUUID();
    }

    /**
     * Sets docker network for TDG container
     *
     * @param network docker network
     */
    public Builder withNetwork(Network network) {
      this.network = network;
      return this;
    }

    /**
     * Sets {@code TARANTOOL_ADVERTISE_URI} parameter value. Must be in the following format: {@code
     * hostname:port}.
     *
     * @param advertiseUri {@code TARANTOOL_ADVERTISE_URI} parameter value
     */
    public Builder withAdvertiseUri(String advertiseUri) {
      if (!URI_PATTERN.matcher(advertiseUri).matches()) {
        throw new IllegalArgumentException(
            "Invalid URI: '"
                + advertiseUri
                + "'. Must be in the format of '<hostname>:<port>'. "
                + "Check pattern: "
                + URI_PATTERN.pattern());
      }
      this.advertiseUri = advertiseUri;
      return this;
    }

    /**
     * Sets aliases for TDG container in docker network.
     *
     * @param aliases aliases of container
     */
    public Builder withAliases(String... aliases) {
      this.aliases = aliases;
      return this;
    }

    /**
     * Sets container name that is using like a container name and container alias. {@code node} is
     * using as the hostname in the {@code TARANTOOL_ADVERTISE_URI} parameter if the {@link
     * #withAdvertiseUri(String)} method is not called.
     *
     * @param node TDG node name
     */
    public Builder withNode(String node) {
      this.node = node;
      return this;
    }

    /**
     * Sets the path on the host that is mounted with the {@link TDGContainer#DEFAULT_TDG_DATA_DIR}
     * path in the container.
     *
     * @param dataPath path on host
     */
    public Builder withDataPath(Path dataPath) {
      this.dataPath = dataPath;
      return this;
    }

    /**
     * Sets container startup timeout
     *
     * @param startupTimeout timeout
     */
    public Builder withStartupTimeout(Duration startupTimeout) {
      this.startupTimeout = startupTimeout;
      return this;
    }

    public String node() {
      return this.node;
    }

    public TDGContainerImpl build() {
      this.network = this.network == null ? Network.newNetwork() : this.network;
      this.uuid = this.uuid == null ? UUID.randomUUID() : this.uuid;
      this.node = this.node == null ? DEFAULT_TDG_PREFIX_NAME + "-" + this.uuid : this.node;
      this.dataPath = this.dataPath == null ? Utils.createTempDirectory(this.node) : this.dataPath;

      return new TDGContainerImpl(
          this.image,
          this.network,
          this.node,
          this.dataPath,
          this.advertiseUri,
          this.aliases,
          this.startupTimeout);
    }
  }
}
