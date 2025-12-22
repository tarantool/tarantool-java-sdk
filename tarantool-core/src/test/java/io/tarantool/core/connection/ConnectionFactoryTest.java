/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Artyom Dubinin
 * @author Ivan Bannikov
 */
public class ConnectionFactoryTest {

  @Test
  public void test_connectionFactory_shouldReturnDifferentConnection() {
    Bootstrap bootstrap = new Bootstrap()
        .group(new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()))
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
    Timer timerService = new HashedWheelTimer();
    ConnectionFactory connectionFactory = new ConnectionFactory(bootstrap, timerService);
    Connection firstConnection = connectionFactory.create();
    Connection secondConnection = connectionFactory.create();
    Assertions.assertEquals(firstConnection, firstConnection);
    Assertions.assertNotEquals(firstConnection, secondConnection);

    timerService.stop();
  }
}
