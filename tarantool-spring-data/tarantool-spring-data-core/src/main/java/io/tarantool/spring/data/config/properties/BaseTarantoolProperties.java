/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.config.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.netty.handler.flush.FlushConsolidationHandler;

import static io.tarantool.client.TarantoolClient.DEFAULT_CONNECTION_THREADS_NUMBER;
import static io.tarantool.client.TarantoolClient.DEFAULT_CONNECTION_TIMEOUT;
import static io.tarantool.client.TarantoolClient.DEFAULT_GRACEFUL_SHUTDOWN;
import static io.tarantool.client.TarantoolClient.DEFAULT_RECONNECT_AFTER;
import static io.tarantool.client.TarantoolClient.DEFAULT_TAG;
import static io.tarantool.client.box.TarantoolBoxClient.DEFAULT_FETCH_SCHEMA;
import static io.tarantool.client.box.TarantoolBoxClient.DEFAULT_IGNORE_OLD_SCHEMA_VERSION;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_PASSWORD;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_USERNAME;
import static io.tarantool.core.protocol.requests.IProtoAuth.DEFAULT_AUTH_TYPE;
import static io.tarantool.pool.HeartbeatOpts.DEFAULT_DEATH_THRESHOLD;
import static io.tarantool.pool.HeartbeatOpts.DEFAULT_INVALIDATION_THRESHOLD;
import static io.tarantool.pool.HeartbeatOpts.DEFAULT_PING_INTERVAL;
import static io.tarantool.pool.HeartbeatOpts.DEFAULT_WINDOW_SIZE;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_CONNECTION_NUMBER;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_HOST;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_PORT;
import io.tarantool.balancer.BalancerMode;
import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.core.protocol.requests.IProtoAuth;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.InstanceConnectionGroup;

/**
 * Configuration properties for Tarantool.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public class BaseTarantoolProperties {

  /**
   * Host name.
   */
  private String host = DEFAULT_HOST;

  /**
   * Password for user.
   */
  private String password = DEFAULT_CRUD_PASSWORD;

  /**
   * Host port.
   */
  private int port = DEFAULT_PORT;

  /**
   * Username with which the connection is made.
   */
  private String userName = DEFAULT_CRUD_USERNAME;

  /**
   * List of connection groups. It is a list of N connections to one node.
   */
  private List<PropertyInstanceConnectionGroup> connectionGroups;

  /**
   * Number of threads provided by netty to serve connections.
   */
  private int eventLoopThreadsCount = DEFAULT_CONNECTION_THREADS_NUMBER;

  /**
   * If true, then
   * <a href="https://www.tarantool.io/en/doc/latest/dev_guide/internals/iproto/graceful_shutdown/">graceful
   * shutdown</a>
   * protocol is enabled.
   */
  private boolean isGracefulShutdownEnabled = DEFAULT_GRACEFUL_SHUTDOWN;

  /**
   * If specified, heartbeat facility will be run with the passed HeartbeatOpts options.
   */
  private PropertyHeartbeatOpts heartbeat;

  /**
   * Connect timeout.
   */
  private long connectTimeout = DEFAULT_CONNECTION_TIMEOUT;

  /**
   * Time after which reconnect occurs.
   */
  private long reconnectAfter = DEFAULT_RECONNECT_AFTER;

  /**
   * If true, then use io.tarantool.schema.TarantoolSchemaFetcher. Only for BOX client.
   */
  private boolean fetchSchema = DEFAULT_FETCH_SCHEMA;

  /**
   * If false, then client can raise exception on getting old schema version. Only for BOX client. Ignored if
   * fetchSchema is false.
   */
  private boolean ignoreOldSchemaVersion = DEFAULT_IGNORE_OLD_SCHEMA_VERSION;

  /**
   * Type of TarantoolBalancer used in client.
   */
  private BalancerMode balancerMode = BalancerMode.DEFAULT_BALANCER_MODE;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseTarantoolProperties)) {
      return false;
    }
    BaseTarantoolProperties that = (BaseTarantoolProperties) o;
    return getPort() == that.getPort() &&
        getEventLoopThreadsCount() == that.getEventLoopThreadsCount() &&
        isGracefulShutdownEnabled() == that.isGracefulShutdownEnabled() &&
        getConnectTimeout() == that.getConnectTimeout() &&
        getReconnectAfter() == that.getReconnectAfter() &&
        isFetchSchema() == that.isFetchSchema() &&
        isIgnoreOldSchemaVersion() == that.isIgnoreOldSchemaVersion() &&
        Objects.equals(getHost(), that.getHost()) &&
        Objects.equals(getPassword(), that.getPassword()) &&
        Objects.equals(getUserName(), that.getUserName()) &&
        Objects.equals(getConnectionGroups(), that.getConnectionGroups()) &&
        Objects.equals(getHeartbeat(), that.getHeartbeat()) &&
        getBalancerMode() == that.getBalancerMode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getHost(),
        getPassword(),
        getPort(),
        getUserName(),
        getConnectionGroups(),
        getEventLoopThreadsCount(),
        isGracefulShutdownEnabled(),
        getHeartbeat(),
        getConnectTimeout(),
        getReconnectAfter(),
        isFetchSchema(),
        isIgnoreOldSchemaVersion(),
        getBalancerMode());
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getEventLoopThreadsCount() {
    return eventLoopThreadsCount;
  }

  public void setEventLoopThreadsCount(int eventLoopThreadsCount) {
    this.eventLoopThreadsCount = eventLoopThreadsCount;
  }

  public boolean isGracefulShutdownEnabled() {
    return isGracefulShutdownEnabled;
  }

  public void setGracefulShutdownEnabled(boolean gracefulShutdownEnabled) {
    this.isGracefulShutdownEnabled = gracefulShutdownEnabled;
  }

  public long getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(long connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public long getReconnectAfter() {
    return reconnectAfter;
  }

  public void setReconnectAfter(long reconnectAfter) {
    this.reconnectAfter = reconnectAfter;
  }

  public boolean isFetchSchema() {
    return fetchSchema;
  }

  public void setFetchSchema(boolean fetchSchema) {
    this.fetchSchema = fetchSchema;
  }

  public boolean isIgnoreOldSchemaVersion() {
    return ignoreOldSchemaVersion;
  }

  public void setIgnoreOldSchemaVersion(boolean ignoreOldSchemaVersion) {
    this.ignoreOldSchemaVersion = ignoreOldSchemaVersion;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public List<PropertyInstanceConnectionGroup> getConnectionGroups() {
    return connectionGroups;
  }

  public List<InstanceConnectionGroup> getInstanceConnectionGroups() {
    if (connectionGroups != null) {
      List<InstanceConnectionGroup> connectionGroups = new ArrayList<>();
      for (PropertyInstanceConnectionGroup group : this.connectionGroups) {
        connectionGroups.add(group.getInstanceConnectionGroup());
      }
      return connectionGroups;
    }
    return null;
  }

  public void setConnectionGroups(List<PropertyInstanceConnectionGroup> connectionGroups) {
    this.connectionGroups = connectionGroups;
  }

  public PropertyHeartbeatOpts getHeartbeat() {
    return heartbeat;
  }

  public HeartbeatOpts getHeartbeatOpts() {
    if (getHeartbeat() != null) {
      return getHeartbeat().getHeartbeatOpts();
    }
    return null;
  }

  public void setHeartbeat(PropertyHeartbeatOpts propertyHeartbeatOptsOpts) {
    this.heartbeat = propertyHeartbeatOptsOpts;
  }

  public BalancerMode getBalancerMode() {
    return balancerMode;
  }

  public Class<? extends TarantoolBalancer> getBalancerClass() {
    return getBalancerMode().getBalancerClass();
  }

  public void setBalancerMode(BalancerMode balancerMode) {
    this.balancerMode = balancerMode;
  }

  /**
   * This class represents a set of properties for FlushConsolidationHandler class
   */
  public static class PropertyFlushConsolidationHandler {

    /**
     * Number of flushes after which an explicit flush will occur.
     */
    private int explicitFlushAfterFlushes = FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES;

    /**
     * Consolidate when no read in progress.
     */
    private boolean consolidateWhenNoReadInProgress = false;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PropertyFlushConsolidationHandler)) {
        return false;
      }
      PropertyFlushConsolidationHandler that = (PropertyFlushConsolidationHandler) o;
      return explicitFlushAfterFlushes == that.explicitFlushAfterFlushes &&
          consolidateWhenNoReadInProgress == that.consolidateWhenNoReadInProgress;
    }

    @Override
    public int hashCode() {
      return Objects.hash(explicitFlushAfterFlushes, consolidateWhenNoReadInProgress);
    }

    public int getExplicitFlushAfterFlushes() {
      return explicitFlushAfterFlushes;
    }

    public void setExplicitFlushAfterFlushes(int explicitFlushAfterFlushes) {
      this.explicitFlushAfterFlushes = explicitFlushAfterFlushes;
    }

    public boolean isConsolidateWhenNoReadInProgress() {
      return consolidateWhenNoReadInProgress;
    }

    public void setConsolidateWhenNoReadInProgress(boolean consolidateWhenNoReadInProgress) {
      this.consolidateWhenNoReadInProgress = consolidateWhenNoReadInProgress;
    }

    public FlushConsolidationHandler getFlushConsolidationHandler() {
      return new FlushConsolidationHandler(this.explicitFlushAfterFlushes, this.consolidateWhenNoReadInProgress);
    }
  }

  /**
   * This class represents a set of properties for HeartbeatOpts class. Heartbeat is a task executing regular pings to
   * Tarantool node and analyzing results if pings are successful or not. When a certain number of failed pings is
   * reached, the heartbeat puts a controlled connection into <b>INVALIDATED</b> state. Invalidated connection is not
   * returned to outer clients but heartbeat continues to ping.For example, isn't useful for following cases:
   * <ul>
   *  <li>In case of tarantool client overload. When too many requests are run and buffers overflow on client or
   *  Tarantool sides it can help to unload connection - after some successful pings connection will be returned
   *  back to
   *  <b>ACTIVATED</b> state.</li>
   *  <li>In cases of network or Tarantool outages. For example when node becomes stuck due to problem with TX thread
   *  (infinite loops, etc) heartbeat will receive failures for each ping to invalidated connections and after several
   *  attempts it will move connection to <b>KILLED</b> state and reconnect task will be run.</li>
   * </ul> Heartbeat doesn't react to any ping failure immediately, because it can lead to problems with balancing and
   * unstable work. Instead of it heartbeat uses sliding window to make reactions smoother.  Heartbeat analyzes last N
   * pings and makes decision. For example, windowSize is 4, invalidationThreshold is 2 and deathThreshold is 4. It
   * means, that heartbeat after starting the heartbeat will wait for 4 pings and then make the first decision. If
   * a count of failed pings is at least 2 then heartbeat will invalidate this connection. Each failed ping in this
   * state will be counted for further comparison with death threshold. If a count of failed pings within the
   * window becomes lower and death threshold is not reached, connection is moved back to activated state and dead
   * pings counter resets. If the death threshold is reached, the connection will be closed and reconnect task will
   * be run.
   *
   * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
   */
  public static class PropertyHeartbeatOpts {

    /**
     * Interval between pings in milliseconds.
     */
    private long pingInterval = DEFAULT_PING_INTERVAL;
    /**
     * Count of failed pings within window.
     */
    private int invalidationThreshold = DEFAULT_INVALIDATION_THRESHOLD;
    /**
     * Total count of pings which should be done. When heartbeat starts it waits for accumulating window and then this
     * windows slides for each subsequent ping request. For example, size of window is 4, and it means that after start
     * heartbeat should execute 4 ping requests and after some decision depending on results of ping will be made.
     */
    private int windowSize = DEFAULT_WINDOW_SIZE;
    /**
     * Count of failures to move connection to KILLED state from INVALIDATED. After invalidation heartbeat will continue
     * to ping this connection and results of pings will be analyzed.  If this amount of failed pings is reached then
     * connection will be killed and reopened.
     */
    private int deathThreshold = DEFAULT_DEATH_THRESHOLD;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PropertyHeartbeatOpts)) {
        return false;
      }
      PropertyHeartbeatOpts propertyHeartbeatOpts = (PropertyHeartbeatOpts) o;
      return pingInterval == propertyHeartbeatOpts.pingInterval &&
          invalidationThreshold == propertyHeartbeatOpts.invalidationThreshold &&
          windowSize == propertyHeartbeatOpts.windowSize &&
          deathThreshold == propertyHeartbeatOpts.deathThreshold;
    }

    @Override
    public int hashCode() {
      return Objects.hash(pingInterval, invalidationThreshold, windowSize, deathThreshold);
    }

    public HeartbeatOpts getHeartbeatOpts() {
      return HeartbeatOpts.getDefault()
          .withDeathThreshold(this.deathThreshold)
          .withWindowSize(this.windowSize)
          .withPingInterval(this.pingInterval)
          .withInvalidationThreshold(invalidationThreshold);
    }

    public long getPingInterval() {
      return pingInterval;
    }

    public void setPingInterval(long pingInterval) {
      this.pingInterval = pingInterval;
    }

    public int getInvalidationThreshold() {
      return invalidationThreshold;
    }

    public void setInvalidationThreshold(int invalidationThreshold) {
      this.invalidationThreshold = invalidationThreshold;
    }

    public int getWindowSize() {
      return windowSize;
    }

    public void setWindowSize(int windowSize) {
      this.windowSize = windowSize;
    }

    public int getDeathThreshold() {
      return deathThreshold;
    }

    public void setDeathThreshold(int deathThreshold) {
      this.deathThreshold = deathThreshold;
    }
  }

  public static class PropertyInstanceConnectionGroup {

    /**
     * Host name.
     */
    private String host = DEFAULT_HOST;
    /**
     * Password for user.
     */
    private String password = DEFAULT_CRUD_PASSWORD;
    /**
     * Host port.
     */
    private int port = DEFAULT_PORT;
    /**
     * Connection group size.
     */
    private int connectionGroupSize = DEFAULT_CONNECTION_NUMBER;
    /**
     * Tag of group.
     */
    private String tag = DEFAULT_TAG;
    /**
     * Username with which the connection is made.
     */
    private String userName = DEFAULT_CRUD_USERNAME;
    /**
     * Type of authentication in Tarantool.
     */
    private IProtoAuth.AuthType authType = DEFAULT_AUTH_TYPE;
    /**
     * Default netty flush handler.
     */
    private PropertyFlushConsolidationHandler flushConsolidationHandler;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PropertyInstanceConnectionGroup)) {
        return false;
      }
      PropertyInstanceConnectionGroup that = (PropertyInstanceConnectionGroup) o;
      return port == that.port && connectionGroupSize == that.connectionGroupSize &&
          Objects.equals(host, that.host) && Objects.equals(password, that.password) &&
          Objects.equals(tag, that.tag) && Objects.equals(userName, that.userName) &&
          authType == that.authType &&
          Objects.equals(flushConsolidationHandler, that.flushConsolidationHandler);
    }

    @Override
    public int hashCode() {
      return Objects.hash(host,
          password,
          port,
          connectionGroupSize,
          tag,
          userName,
          authType,
          flushConsolidationHandler);
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public int getConnectionGroupSize() {
      return connectionGroupSize;
    }

    public void setConnectionGroupSize(int connectionGroupSize) {
      this.connectionGroupSize = connectionGroupSize;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public IProtoAuth.AuthType getAuthType() {
      return authType;
    }

    public void setAuthType(IProtoAuth.AuthType authType) {
      this.authType = authType;
    }

    public void setFlushConsolidationHandler(PropertyFlushConsolidationHandler flushConsolidationHandler) {
      this.flushConsolidationHandler = flushConsolidationHandler;
    }

    public PropertyFlushConsolidationHandler getFlushConsolidationHandler() {
      return flushConsolidationHandler;
    }

    public InstanceConnectionGroup getInstanceConnectionGroup() {
      InstanceConnectionGroup.Builder builder =
          InstanceConnectionGroup.builder()
              .withUser(userName)
              .withPassword(password)
              .withSize(connectionGroupSize)
              .withPort(port)
              .withHost(host)
              .withAuthType(authType)
              .withTag(tag);

      if (flushConsolidationHandler != null) {
        builder.withFlushConsolidationHandler(flushConsolidationHandler.getFlushConsolidationHandler());
      }

      return builder.build();
    }
  }
}
