/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.util.List;
import java.util.Set;

import io.grpc.ManagedChannel;
import org.testcontainers.containers.utils.pojo.User;

/**
 * Encapsulates version-specific gRPC API for publishing and subscribing. TQE 2.x uses {@code
 * PublisherServiceGrpc} + unidirectional streaming. TQE 3.x uses {@code ProducerGrpc} +
 * bidirectional streaming.
 */
interface GrpcTestStrategy {

  /**
   * Publishes a batch of users to the given queue over the channel. Synchronous: throws on failure.
   */
  void publish(ManagedChannel channel, List<User> users, String queue) throws Exception;

  /**
   * Starts a subscription on the given queue. Messages are delivered asynchronously and added to
   * {@code result} as they arrive. This call only kicks off the subscription; callers should
   * retry/poll until {@code result} reaches the expected size.
   */
  void subscribe(ManagedChannel channel, String queue, Set<User> result);
}
