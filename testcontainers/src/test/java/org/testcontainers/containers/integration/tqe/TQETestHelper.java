/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;

final class TQETestHelper {

  static final Path TEST_TEMP_DIR;

  static {
    try {
      TEST_TEMP_DIR = Files.createTempDirectory("tqe-test-");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private TQETestHelper() {}

  static Path loadConfig(String resourcePath) {
    try {
      return Paths.get(
          Objects.requireNonNull(TQETestHelper.class.getClassLoader().getResource(resourcePath))
              .toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  static ManagedChannel createReadyChannel(InetSocketAddress address) {
    return Unreliables.retryUntilSuccess(
        60,
        TimeUnit.SECONDS,
        () -> {
          ManagedChannel ch =
              ManagedChannelBuilder.forAddress(address.getHostName(), address.getPort())
                  .usePlaintext()
                  .maxInboundMessageSize(16 * 1024 * 1024)
                  .keepAliveTime(30, TimeUnit.SECONDS)
                  .keepAliveTimeout(5, TimeUnit.SECONDS)
                  .keepAliveWithoutCalls(true)
                  .build();

          ch.getState(true);
          Unreliables.retryUntilTrue(
              5,
              TimeUnit.SECONDS,
              () -> {
                io.grpc.ConnectivityState state = ch.getState(false);
                if (state == io.grpc.ConnectivityState.READY) {
                  return true;
                }
                ch.resetConnectBackoff();
                Thread.sleep(100);
                return false;
              });
          return ch;
        });
  }
}
