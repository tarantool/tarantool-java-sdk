/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.support;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.mockito.Mockito;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.MapValue;
import org.msgpack.value.ValueFactory;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.protocol.IProtoRawResponse;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.IProtoConstant;

/**
 * A stubbed {@link IProtoClient} whose ping answers are scripted by the test.
 *
 * <p>The heartbeat policy checks the availability of a node with the client of the connection view,
 * so the test must be able to decide what the node answers: a successful ping, a failed ping, a
 * ping that hangs forever, a ping with a specific response body, or a ping whose answer is
 * delivered by the test at a chosen moment.
 *
 * <p>The answers are taken from the queue in the order of their scripting. When the queue is empty,
 * the default answer is used: a successful ping for {@link #create()}, a failed ping for {@link
 * #failing(Throwable)}.
 */
public final class StubClient {

  private final IProtoClient client;
  private final Queue<Supplier<CompletableFuture<IProtoResponse>>> outcomes =
      new ConcurrentLinkedQueue<>();
  private final AtomicInteger pingCount = new AtomicInteger();
  private volatile Supplier<CompletableFuture<IProtoResponse>> defaultOutcome;

  private StubClient() {
    this.client = Mockito.mock(IProtoClient.class);
    this.defaultOutcome = StubClient::okResponse;
    Mockito.when(this.client.ping(Mockito.any(IProtoRequestOpts.class)))
        .thenAnswer(invocation -> nextOutcome());
    Mockito.when(this.client.ping()).thenAnswer(invocation -> nextOutcome());
  }

  /**
   * Creates a stub whose ping always succeeds.
   *
   * @return the stub instance
   */
  public static StubClient create() {
    return new StubClient();
  }

  /**
   * Creates a stub whose ping always fails with the given cause.
   *
   * @param cause the cause of the ping failure
   * @return the stub instance
   */
  public static StubClient failing(Throwable cause) {
    final StubClient stub = new StubClient();
    stub.defaultOutcome = () -> failedResponse(cause);
    return stub;
  }

  /**
   * Builds a response with an empty body, just like the one returned by {@code IPROTO_PING}.
   *
   * @return the response instance
   */
  public static IProtoResponse emptyBodyResponse() {
    final MapValue header =
        ValueFactory.newMap(
            ValueFactory.newInteger(IProtoConstant.IPROTO_REQUEST_TYPE),
            ValueFactory.newInteger(IProtoConstant.IPROTO_OK));
    try (final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packValue(header);
      final byte[] headerBytes = packer.toByteArray();
      final byte[] packet = new byte[headerBytes.length + 1];
      System.arraycopy(headerBytes, 0, packet, 0, headerBytes.length);
      // an empty map, the body of a successful IPROTO_PING response
      packet[headerBytes.length] = (byte) 0x80;
      return new IProtoRawResponse(header, packet, headerBytes.length);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to build a ping response", e);
    }
  }

  /**
   * Returns the stubbed client to be used as the client of a connection view.
   *
   * @return the stubbed client
   */
  public IProtoClient client() {
    return this.client;
  }

  /**
   * Makes the next ping succeed.
   *
   * @return this stub instance
   */
  public StubClient nextPingSucceeds() {
    this.outcomes.add(StubClient::okResponse);
    return this;
  }

  /**
   * Makes the next ping fail with the given cause.
   *
   * @param cause the cause of the ping failure
   * @return this stub instance
   */
  public StubClient nextPingFails(Throwable cause) {
    this.outcomes.add(() -> failedResponse(cause));
    return this;
  }

  /**
   * Makes the next ping hang: the returned future is never completed.
   *
   * @return this stub instance
   */
  public StubClient nextPingHangs() {
    this.outcomes.add(CompletableFuture::new);
    return this;
  }

  /**
   * Makes the next ping return the given response.
   *
   * @param response the response to return
   * @return this stub instance
   */
  public StubClient nextPingReturns(IProtoResponse response) {
    this.outcomes.add(() -> CompletableFuture.completedFuture(response));
    return this;
  }

  /**
   * Makes the next ping return the given future.
   *
   * <p>The future is returned as is, so the test keeps the control over the moment of the answer:
   * the heartbeat reacts to the ping only when the future is completed. A future that is never
   * completed makes the ping hang forever.
   *
   * @param future the future to return from the next ping
   * @return this stub instance
   */
  public StubClient nextPingReturnsFuture(CompletableFuture<IProtoResponse> future) {
    this.outcomes.add(() -> future);
    return this;
  }

  /**
   * Returns the count of the pings that have been sent to the stub.
   *
   * @return the count of the pings
   */
  public int pingCount() {
    return this.pingCount.get();
  }

  private static CompletableFuture<IProtoResponse> okResponse() {
    return CompletableFuture.completedFuture(emptyBodyResponse());
  }

  private static CompletableFuture<IProtoResponse> failedResponse(Throwable cause) {
    final CompletableFuture<IProtoResponse> failed = new CompletableFuture<>();
    failed.completeExceptionally(cause);
    return failed;
  }

  private CompletableFuture<IProtoResponse> nextOutcome() {
    this.pingCount.incrementAndGet();
    final Supplier<CompletableFuture<IProtoResponse>> outcome = this.outcomes.poll();
    if (outcome == null) {
      return this.defaultOutcome.get();
    }
    return outcome.get();
  }
}
