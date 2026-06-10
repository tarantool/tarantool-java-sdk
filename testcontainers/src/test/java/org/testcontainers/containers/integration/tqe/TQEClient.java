/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.util.List;
import java.util.function.Consumer;

import org.testcontainers.containers.utils.pojo.User;

/**
 * Version-specific gRPC client for publishing and subscribing to a TQE queue. TQE 2.x uses {@code
 * PublisherServiceGrpc} + unidirectional streaming; TQE 3.x uses {@code ProducerGrpc} +
 * bidirectional streaming.
 *
 * <p>The gRPC channel is bound at construction time and is intentionally not part of the method
 * signatures: it is an internal transport detail, not part of the client contract.
 */
interface TQEClient {

  /**
   * Publishes a batch of users to the given queue. Synchronous: throws on failure.
   *
   * @param users the messages to publish
   * @param queue the target queue name
   */
  void publish(List<User> users, String queue) throws Exception;

  /**
   * Starts a subscription on the given queue. Messages are delivered asynchronously and forwarded
   * to {@code resultAcceptor} as they arrive. This call only kicks off the subscription; callers
   * should retry/poll until enough messages have been received.
   *
   * @param queue the queue to subscribe to
   * @param resultAcceptor callback invoked once per received message
   */
  void subscribe(String queue, Consumer<User> resultAcceptor);
}
