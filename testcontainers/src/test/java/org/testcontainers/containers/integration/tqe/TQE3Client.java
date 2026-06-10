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
import tarantool.queue_ee.Consumer.SubscriptionRequest;
import tarantool.queue_ee.Consumer.SubscriptionStreamRequest;
import tarantool.queue_ee.Consumer.SubscriptionStreamResponse;
import tarantool.queue_ee.ConsumerServiceGrpc;
import tarantool.queue_ee.ConsumerServiceGrpc.ConsumerServiceStub;
import tarantool.queue_ee.ProducerGrpc;
import tarantool.queue_ee.ProducerGrpc.ProducerBlockingStub;
import tarantool.queue_ee.ProducerOuterClass.ProduceMessage;
import tarantool.queue_ee.ProducerOuterClass.ProduceRequest;

/** TQE 3.x gRPC API: {@code produce} + bidirectional client-streaming subscribe. */
final class TQE3Client implements TQEClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ManagedChannel channel;

  TQE3Client(ManagedChannel channel) {
    this.channel = channel;
  }

  @Override
  public void publish(List<User> users, String queue) throws Exception {
    ProducerBlockingStub producer = ProducerGrpc.newBlockingStub(channel);
    ProduceRequest.Builder requestBuilder = ProduceRequest.newBuilder().setQueue(queue);
    for (User user : users) {
      requestBuilder.addMessages(
          ProduceMessage.newBuilder()
              .setPayload(ByteString.copyFrom(MAPPER.writeValueAsBytes(user))));
    }
    producer.produce(requestBuilder.build());
  }

  @Override
  public void subscribe(String queue, Consumer<User> resultAcceptor) {
    ConsumerServiceStub consumer = ConsumerServiceGrpc.newStub(channel);
    StreamObserver<SubscriptionStreamRequest> requestsStream =
        consumer.subscribe(
            new StreamObserver<>() {
              @Override
              public void onNext(SubscriptionStreamResponse response) {
                response.getNotifications().getNotificationsList().stream()
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
    requestsStream.onNext(
        SubscriptionStreamRequest.newBuilder()
            .setSubscribeRequest(SubscriptionRequest.newBuilder().setCursor("").setQueue(queue))
            .build());
  }
}
