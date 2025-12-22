/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.msgpack.value.ValueFactory;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;

/**
 * <p>This class represents a set of options used in heartbeats.</p>
 *
 * <p>Heartbeat is a task executing regular pings to Tarantool node and analyzing results if pings are successful or
 * not. When a certain number of failed pings is reached, the heartbeat puts a controlled connection into
 * <b>INVALIDATED</b> state. Invalidated connection is not returned to outer clients but heartbeat continues to ping.
 * For example, isn't useful for following cases:
 * <ul>
 *  <li>In case of tarantool client overload. When too many requests are run and buffers overflow on client or
 *  Tarantool sides it can help to unload connection - after some successful pings connection will be returned back to
 *  <b>ACTIVATED</b> state.</li>
 *  <li>In cases of network or Tarantool outages. For example when node becomes stuck due to problem with TX thread
 *  (infinite loops, etc) heartbeat will receive failures for each ping to invalidated connections and after several
 *  attempts it will move connection to <b>KILLED</b> state and reconnect task will be run.</li>
 * </ul>
 *
 * <p>Heartbeat doesn't react to any ping failure immediately, because it can lead to problems with balancing and
 * unstable work. Instead of it heartbeat uses sliding window to make reactions smoother.  Heartbeat analyzes last N
 * pings and makes decision.</p>
 *
 * <p>For example, {@link #windowSize} is 4, {@link #invalidationThreshold} is 2 and {@link #deathThreshold} is 4. It
 * means, that heartbeat after starting the heartbeat will wait for 4 pings and then make the first decision. If a count
 * of failed pings is at least 2 then heartbeat will invalidate this connection. Each failed ping in this state will be
 * counted for further comparison with death threshold. If a count of failed pings within the window becomes lower and
 * death threshold is not reached, connection is moved back to activated state and dead pings counter resets. If the
 * death threshold is reached, the connection will be closed and reconnect task will be run.</p>
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
public final class HeartbeatOpts {

  /**
   * <p>Default value for {@link #pingInterval}.</p>
   */
  public static final long DEFAULT_PING_INTERVAL = 3_000L;

  /**
   * <p>Default value for {@link #invalidationThreshold}.</p>
   */
  public static final int DEFAULT_INVALIDATION_THRESHOLD = 2;

  /**
   * <p>Default value for {@link #windowSize}.</p>
   */
  public static final int DEFAULT_WINDOW_SIZE = 4;

  /**
   * <p>Default value for {@link #deathThreshold}.</p>
   */
  public static final int DEFAULT_DEATH_THRESHOLD = 4;

  /**
   * <p>Interval between pings in milliseconds.</p>
   *
   * <p><i><b>Default</b></i>: {@code 3000}.</p>
   */
  private long pingInterval;

  /**
   * <p>Count of failed pings within window.</p>
   *
   * <p><i><b>Default</b></i>: {@code 2}.</p>
   */
  private int invalidationThreshold;

  /**
   * <p>Total count of pings which should be done.</p>
   * <p>
   * When heartbeat starts it waits for accumulating window and then this windows slides for each subsequent ping
   * request. For example, size of window is 4 and it means that after start heartbeat should execute 4 ping requests
   * and after some decision depending on results of ping will be made.
   *
   * <p><i><b>Default</b></i>: {@code 4}.</p>
   */
  private int windowSize;

  /**
   * <p>Count of failures to move connection to KILLED state from INVALIDATED.</p>
   * <p>
   * After invalidation heartbeat will continue to ping this connection and results of pings will be analyzed.  If this
   * amount of failed pings is reached then connection will be killed and reopened.
   *
   * <p><i><b>Default</b></i>: {@code 4}.</p>
   */
  private int deathThreshold;

  /**
   * <p>Function that send IProto packet that will be sent to tarantool.
   * The response of this packet will be considered as pong result.</p>
   * <p>
   * If tarantool raise exception or timeout happened then we accept it as failed ping. Also we check response. If the
   * response is not empty and doesn't consist true message inside we mark it as failed ping. For example you can use
   * such function:
   * <blockquote><pre>{@code
   *      HeartbeatOpts.getDefault().withPingFunction(
   *             (client, heartbeatPingOpts)
   *                      -> client.call("health", ValueFactory.emptyArray(), heartbeatPingOpts)
   *      );
   * }</pre></blockquote>
   * where health function will be your function in lua.
   *
   * <p><i><b>Default</b></i>: IPROTO_PING with iproto options as request timeout.</p>
   */
  private BiFunction<IProtoClient, IProtoRequestOpts, CompletableFuture<IProtoResponse>> pingFunction;

  /**
   * <p>Static method for getting default heartbeat options.</p>
   *
   * @return instance of {@link io.tarantool.pool.HeartbeatOpts}
   */
  public static HeartbeatOpts getDefault() {
    return new HeartbeatOpts();
  }

  /**
   * Private constructor for {@link io.tarantool.pool.HeartbeatOpts}.
   */
  private HeartbeatOpts() {
    pingInterval = DEFAULT_PING_INTERVAL;
    invalidationThreshold = DEFAULT_INVALIDATION_THRESHOLD;
    windowSize = DEFAULT_WINDOW_SIZE;
    deathThreshold = DEFAULT_DEATH_THRESHOLD;
    pingFunction = IProtoClient::ping;
  }

  /**
   * Set {@link #pingInterval} value.
   *
   * @param interval a new value of interval in milliseconds
   * @return instance of {@link io.tarantool.pool.HeartbeatOpts}
   * @throws IllegalArgumentException when {@code "interval"} is zero or negative number
   */
  public HeartbeatOpts withPingInterval(long interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("ping interval should positive number");
    }
    pingInterval = interval;
    return this;
  }

  /**
   * Set {@link #invalidationThreshold} value.
   *
   * @param count a new value or percent if invalid ping within window
   * @return instance of {@link io.tarantool.pool.HeartbeatOpts}
   * @throws IllegalArgumentException when {@code "percent"} is zero or negative
   */
  public HeartbeatOpts withInvalidationThreshold(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("a count of failed pings should be more than 0");
    }
    invalidationThreshold = count;
    return this;
  }

  /**
   * Set {@link #deathThreshold} value.
   *
   * @param count a new value of death pings
   * @return instance of {@link io.tarantool.pool.HeartbeatOpts}
   * @throws IllegalArgumentException when {@code "count"} is zero or negative
   */
  public HeartbeatOpts withDeathThreshold(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("count of death pings should be positive number");
    }
    deathThreshold = count;
    return this;
  }

  /**
   * Set {@link #windowSize}.
   *
   * @param count a total count of pings in window
   * @return instance of {@link io.tarantool.pool.HeartbeatOpts}
   * @throws IllegalArgumentException when {@code "count"} is zero or negative
   */
  public HeartbeatOpts withWindowSize(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("count of death pings should be positive number");
    }
    windowSize = count;
    return this;
  }

  /**
   * Predefined function that could be helpful to determine whether crud role is up or not. For example you can use such
   * function:
   * <blockquote><pre>{@code
   *      HeartbeatOpts.getDefault().withCrudHealthCheck();
   * }</pre></blockquote>
   *
   * @return the heartbeat opts
   */
  public HeartbeatOpts withCrudHealthCheck() {
    return withPingFunction(
        (client, heartbeatPingOpts) ->
            client.eval(
                "return rawget(_G, 'crud') ~= nil",
                ValueFactory.emptyArray(),
                heartbeatPingOpts)
    );
  }

  /**
   * Set {@link #pingFunction}.
   *
   * @param pingFunction function that receive IPROTO client and should return tarantool response
   * @return instance of {@link io.tarantool.pool.HeartbeatOpts}
   */
  public HeartbeatOpts withPingFunction(
      BiFunction<IProtoClient, IProtoRequestOpts, CompletableFuture<IProtoResponse>> pingFunction) {
    this.pingFunction = pingFunction;
    return this;
  }

  /**
   * Getter for {@link #pingInterval}.
   *
   * @return {@link #pingInterval} value
   */
  public long getPingInterval() {
    return pingInterval;
  }

  /**
   * Getter for {@link #invalidationThreshold}
   *
   * @return {@link #invalidationThreshold} value
   */
  public int getInvalidationThreshold() {
    return invalidationThreshold;
  }

  /**
   * Getter for {@link #windowSize}
   *
   * @return {@link #windowSize} value
   */
  public int getWindowSize() {
    return windowSize;
  }

  /**
   * Getter for {@link #deathThreshold}
   *
   * @return {@link #deathThreshold} value
   */
  public int getDeathThreshold() {
    return deathThreshold;
  }

  /**
   * Getter for {@link #pingFunction}
   *
   * @return {@link #pingFunction} value
   */
  public BiFunction<IProtoClient, IProtoRequestOpts, CompletableFuture<IProtoResponse>> getPingFunction() {
    return pingFunction;
  }
}
