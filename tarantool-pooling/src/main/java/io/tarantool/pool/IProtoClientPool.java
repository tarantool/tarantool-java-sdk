/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.pool.exceptions.PoolClosedException;

/**
 * Interface for connection pooling.
 *
 * <p>All connections in pools are divided into groups with associated metadata: tags, host, ports,
 * credentials and count, and in this API contract such groups can be managed: added, deleted,
 * reduced or expanded. An implementation of pool is responsible to create connection instances when
 * new groups added or old groups increased, close and remove connection instances when groups
 * deleted or reduced. This management is being done by {@link
 * io.tarantool.pool.IProtoClientPool#setGroups} method.
 *
 * <p>Outer clients need information about connections created in pool and want to know if pool can
 * supply alive connections or not. To obtain information about connection groups and their size
 * methods {@link io.tarantool.pool.IProtoClientPool#getTags} and {@link
 * io.tarantool.pool.IProtoClientPool#getGroupSize} should be used. To know are there any available
 * connections method {@link io.tarantool.pool.IProtoClientPool#hasAvailableClients} is used.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public interface IProtoClientPool {

  /**
   * Sets new instance groups configuration for pool.
   *
   * <p>Update connection groups in pool. For groups with tags not existing in pool but presented in
   * new configuration it will add them to pool configuration and initialize {@link
   * io.tarantool.pool.PoolEntry} items with connections. Groups existing in pool but not presented
   * in a new list will be deleted and correspondent connections will be closed. Groups with the
   * same tags but different addresses or credentials will be replaced and all connections will be
   * closed and a new connection with new addresses or credentials will be initialized. Groups with
   * the same tags, addresses and credentials will be aligned to a new instances groups
   * configuration - connections will be closed or initialized and added to pool.
   *
   * @param groups a list of {@link io.tarantool.pool.InstanceConnectionGroup} instances.
   */
  void setGroups(List<InstanceConnectionGroup> groups);

  /**
   * Returns a list of strings, when each string is a instance group tag.
   *
   * <p>It needed for outer clients e.g. balancer to known about connections in pool.
   *
   * @return list of string tags
   * @see io.tarantool.pool.IProtoClientPool#getTags
   */
  List<String> getTags();

  /**
   * Returns a size of correspondent connection group.
   *
   * @param tag name of group
   * @return size of corresponded connection group
   * @throws NoSuchElementException in case when group with passed tag does not exist
   */
  int getGroupSize(String tag) throws NoSuchElementException;

  /**
   * Returns a future with iproto client.
   *
   * <p>For just initialized but never connected clients connecting is lazy - method returns a
   * future which may be completed successfully by already connected client or exceptionally in case
   * of some error. If success connect won't be repeated again, this method will return completed
   * future with connected client. Otherwise pool will mark this connection as unavailable and start
   * reconnection task so subsequent calls will return {@code null} value until connection will be
   * done. In case of invalidated or killed client it also returns {@code null} value.
   *
   * @param tag a string tag for connection group
   * @param index an index of connection within connection group
   * @return future for iproto client or null in case of client invalidation
   * @throws IndexOutOfBoundsException in case when passed index is out of bounds of group
   *     connections
   * @throws NoSuchElementException in case when group with passed tag does not exist
   * @throws PoolClosedException in case when pool is closed
   */
  CompletableFuture<IProtoClient> get(String tag, int index)
      throws NoSuchElementException, IndexOutOfBoundsException, PoolClosedException;

  /**
   * Returns flag whether clients are available in pool or not.
   *
   * <p>When all clients are invalidated or killed in pool due to different reasons this method
   * returns {@code false}. When at least one connection is alive and available it will return
   * {@code true}.
   *
   * @return flag signaling about ability of pool to give alive connections
   */
  boolean hasAvailableClients();

  /**
   * Get how many connections are available
   *
   * @return connection count
   */
  int availableConnections();

  /**
   * Closes all connections in pool.
   *
   * <p>Also, it stops all run heartbeats and reconnection tasks.
   *
   * @throws Exception the exception
   */
  void close() throws Exception;

  /**
   * Executes passed function over all clients in pool.
   *
   * <p>It is not guaranteed that all clients will be connected and valid at this moment. Now it is
   * used for special purposes related to watchers, that can be executed in offline mode.
   *
   * @param action callback which will be executed on each client
   */
  void forEach(Consumer<IProtoClient> action);

  /**
   * Set timeout for connection in milliseconds.
   *
   * <p>Set timeout for all non-connected clients. When connection process takes more than set
   * timeout it will be interrupted by timeout exception. Takes effect over all pool.
   *
   * @param timeout timeout
   */
  void setConnectTimeout(long timeout);

  /**
   * Returns connect timeout in milliseconds.
   *
   * <p>Getter for timeout property.
   *
   * @return timeout in milliseconds
   * @throws IllegalArgumentException when timeout is zero or negative
   */
  long getConnectTimeout() throws IllegalArgumentException;

  /**
   * Set period in milliseconds for reconnect tasks.
   *
   * <p>When reconnect task for broken connection is run it will be repeated after this period.
   *
   * @param reconnectAfter reconnectAfter
   * @throws IllegalArgumentException when passed value is zero or negative
   */
  void setReconnectAfter(long reconnectAfter) throws IllegalArgumentException;

  /**
   * Returns reconnect period in milliseconds
   *
   * <p>Getter for reconnectAfter property
   *
   * @return period of reconnect task in milliseconds
   */
  long getReconnectAfter();

  /**
   * Returns connection factory.
   *
   * <p>Getter for factory.
   *
   * @return ConnectionFactory instance
   */
  ConnectionFactory getFactory();
}
