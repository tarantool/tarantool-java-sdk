/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.util.List;
import java.util.function.Consumer;

import io.grpc.ManagedChannel;
import org.testcontainers.containers.utils.pojo.User;

/**
 * Version-specific gRPC client for publishing and subscribing to a TQE queue. TQE 2.x uses {@code
 * PublisherServiceGrpc} + unidirectional streaming; TQE 3.x uses {@code ProducerGrpc} +
 * bidirectional streaming.
 */
interface TQEClient {

  /**
   * Publishes a batch of users to the given queue over the channel. Synchronous: throws on failure.
   */
  void publish(ManagedChannel channel, List<User> users, String queue) throws Exception;

  /**
   * Starts a subscription on the given queue. Messages are delivered asynchronously and forwarded
   * to {@code resultAcceptor} as they arrive. This call only kicks off the subscription; callers
   * should retry/poll until enough messages have been received.
   */
  void subscribe(ManagedChannel channel, String queue, Consumer<User> resultAcceptor);
}
