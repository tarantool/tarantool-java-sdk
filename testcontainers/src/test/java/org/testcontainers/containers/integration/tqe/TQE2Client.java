/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.testcontainers.containers.utils.pojo.User;
import tarantool.queue_ee.v2.Consumer.SubscriptionNotifications;
import tarantool.queue_ee.v2.Consumer.SubscriptionRequest;
import tarantool.queue_ee.v2.ConsumerServiceGrpc;
import tarantool.queue_ee.v2.ConsumerServiceGrpc.ConsumerServiceStub;
import tarantool.queue_ee.v2.Publisher.BatchRequestMessage;
import tarantool.queue_ee.v2.Publisher.PublishBatchRequest;
import tarantool.queue_ee.v2.PublisherServiceGrpc;
import tarantool.queue_ee.v2.PublisherServiceGrpc.PublisherServiceBlockingStub;

/** TQE 2.x gRPC API: {@code publishBatch} + unidirectional server-streaming subscribe. */
final class TQE2Client implements TQEClient {

  static final TQE2Client INSTANCE = new TQE2Client();

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public void publish(ManagedChannel channel, List<User> users, String queue) throws Exception {
    PublisherServiceBlockingStub pService = PublisherServiceGrpc.newBlockingStub(channel);
    PublishBatchRequest.Builder requestBuilder = PublishBatchRequest.newBuilder();
    for (User user : users) {
      requestBuilder.addMessages(
          BatchRequestMessage.newBuilder()
              .setPayload(ByteString.copyFrom(MAPPER.writeValueAsBytes(user))));
    }
    pService.publishBatch(requestBuilder.setQueue(queue).build());
  }

  @Override
  public void subscribe(ManagedChannel channel, String queue, Consumer<User> resultAcceptor) {
    ConsumerServiceStub cService = ConsumerServiceGrpc.newStub(channel);
    cService.subscribe(
        SubscriptionRequest.newBuilder().setCursor("").setQueue(queue).build(),
        new StreamObserver<>() {
          @Override
          public void onNext(SubscriptionNotifications value) {
            value.getNotificationsList().stream()
                .map(
                    n -> {
                      try {
                        return MAPPER.readValue(
                            n.getMessage().getPayload().toByteArray(), User.class);
                      } catch (IOException e) {
                        throw new RuntimeException(e);
                      }
                    })
                .forEach(resultAcceptor);
          }

          @Override
          public void onError(Throwable t) {
            throw new RuntimeException("Stream observer received error", t);
          }

          @Override
          public void onCompleted() {}
        });
  }
}
