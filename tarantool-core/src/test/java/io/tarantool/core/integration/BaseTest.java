/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.IProtoClientImpl.DEFAULT_WATCHER_OPTS;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.connection.ConnectionFactory;

public abstract class BaseTest {

  protected static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

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

  protected Timer timerService = new HashedWheelTimer();

  protected ConnectionFactory factory = new ConnectionFactory(bootstrap, timerService);

  protected static ArrayValue decodeTuple(IProtoClient client, ArrayValue arrayValue)
      throws IOException {
    if (client.hasTupleExtension()) {
      List<Value> list = new ArrayList<>();
      for (Value value : arrayValue.list()) {
        byte[] data = value.asExtensionValue().getData();
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
          unpacker.unpackInt();
          list.add(unpacker.unpackValue());
        }
      }
      return ValueFactory.newArray(list);
    } else {
      return arrayValue;
    }
  }

  protected IProtoClient createClientAndConnect(
      InetSocketAddress address, boolean useTupleExtension) throws Exception {
    IProtoClient client =
        new IProtoClientImpl(
            factory,
            factory.getTimerService(),
            DEFAULT_WATCHER_OPTS,
            null,
            null,
            useTupleExtension);
    client.connect(address, 3_000).get();
    return client;
  }
}
