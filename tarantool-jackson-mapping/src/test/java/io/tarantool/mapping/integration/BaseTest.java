/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.integration;

import java.util.HashMap;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import io.tarantool.core.connection.ConnectionFactory;


public abstract class BaseTest {

  protected static final String API_USER = "api_user";

  protected static final Map<String, String> CREDS = new HashMap<String, String>(){{
    put(API_USER, "secret");
  }};

  protected static final Map<String, String> ENV_MAP = new HashMap<String, String>(){{
    put("TARANTOOL_USER_NAME", API_USER);
    put("TARANTOOL_USER_PASSWORD", CREDS.get(API_USER));
  }};

  protected static final Bootstrap bootstrap = new Bootstrap()
      .group(new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()))
      .channel(NioSocketChannel.class)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.SO_KEEPALIVE, true)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
  protected static final Timer timerService = new HashedWheelTimer();
  protected static final ConnectionFactory factory = new ConnectionFactory(bootstrap, timerService);
}
