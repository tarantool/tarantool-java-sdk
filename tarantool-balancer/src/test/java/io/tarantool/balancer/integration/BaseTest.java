/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.balancer.integration;

import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.pool.HeartbeatOpts;

public abstract class BaseTest {

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
  protected static final Timer timerService = new HashedWheelTimer();
  protected static final ManagedResource<Timer> timerResource =
      ManagedResource.external(timerService);
  protected static ConnectionFactory factory = new ConnectionFactory(bootstrap, timerService);

  protected static final int MAX_CONNECTION_COUNT = 20;
  protected static final int MIN_CONNECTION_COUNT = 10;

  protected static int count1;
  protected static int count2;

  protected static final String API_USER = "api_user";

  protected static final Map<String, String> CREDS =
      new HashMap<String, String>() {
        {
          put(API_USER, "secret");
        }
      };

  protected static final Map<String, String> ENV_MAP =
      new HashMap<String, String>() {
        {
          put("TARANTOOL_USER_NAME", API_USER);
          put("TARANTOOL_USER_PASSWORD", CREDS.get(API_USER));
        }
      };

  protected static final int PING_INTERVAL = 500;
  protected static final int WINDOW_SIZE = 4;
  protected static final long TIMER_ERROR_MS = 100;
  protected static final int INVALID_PINGS = 2;
  protected static final long INVALIDATION_TIMEOUT =
      ((long) (INVALID_PINGS + 1) * (PING_INTERVAL + TIMER_ERROR_MS));

  /** Longer wait for invalidation (e.g. Tarantool 3 with more connections). */
  protected static final long EXTENDED_INVALIDATION_TIMEOUT = INVALIDATION_TIMEOUT * 3;

  protected static final long RESTORE_TIMEOUT =
      (WINDOW_SIZE + 1) * (PING_INTERVAL + TIMER_ERROR_MS);
  protected static final ArrayValue emptyArgs = ValueFactory.emptyArray();
  protected static final HeartbeatOpts HEARTBEAT_OPTS =
      HeartbeatOpts.getDefault()
          .withPingInterval(PING_INTERVAL)
          .withInvalidationThreshold(INVALID_PINGS)
          .withWindowSize(WINDOW_SIZE);
}
