/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.Test;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.protocol.IProtoResponseImpl;

/**
 * @author Artyom Dubinin
 * @author Ivan Bannikov
 */
public class ConnectionTest {

  @Test
  public void test_connectionFactory_shouldWorkCorrectly_withoutConnect() {
    Bootstrap bootstrap =
        new Bootstrap()
            .group(new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()))
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

    Timer timerService = new HashedWheelTimer();
    ConnectionImpl connection = new ConnectionImpl(bootstrap, timerService);
    assertFalse(connection.getGreeting().isPresent());
    assertFalse(connection.isConnected());

    Map<Value, Value> headerMap = new HashMap<>();
    headerMap.put(ValueFactory.newInteger(0x00), ValueFactory.newInteger(0x00));
    headerMap.put(ValueFactory.newInteger(0x01), ValueFactory.newInteger(0x00));
    MapValue header = ValueFactory.newMap(headerMap);
    MapValue body = ValueFactory.newMap(new HashMap<Value, Value>());
    Exception ex =
        assertThrows(
            IllegalStateException.class,
            () -> connection.send(new IProtoResponseImpl(header, body)));
    assertEquals("Connection is not established", ex.getMessage());
    connection.close();
  }
}
