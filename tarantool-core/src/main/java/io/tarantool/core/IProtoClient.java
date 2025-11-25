/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.msgpack.value.ArrayValue;

import static io.tarantool.core.protocol.requests.IProtoAuth.AuthType;
import io.tarantool.core.connection.ConnectionCloseEvent;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.TransactionIsolationLevel;
import io.tarantool.core.protocol.requests.SelectAfterMode;

public interface IProtoClient {

  CompletableFuture<Void> connect(InetSocketAddress address, long timeoutMs);

  CompletableFuture<Void> connect(
      InetSocketAddress address, long timeoutMs, boolean gracefulShutdown);

  CompletableFuture<IProtoResponse> authorize(String user, String password);

  CompletableFuture<IProtoResponse> authorize(String user, String password, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> authorize(String user, String password, AuthType authType);

  CompletableFuture<IProtoResponse> authorize(
      String user, String password, IProtoRequestOpts opts, AuthType authType);

  CompletableFuture<IProtoResponse> select(
      int spaceId, int indexId, ArrayValue key, int limit, int offset, BoxIterator iterator);

  CompletableFuture<IProtoResponse> select(
      int spaceId, int indexId, byte[] key, int limit, int offset, BoxIterator iterator);

  CompletableFuture<IProtoResponse> select(
      Integer spaceId,
      String spaceName,
      Integer indexId,
      String indexName,
      ArrayValue key,
      int limit,
      int offset,
      BoxIterator iterator,
      boolean fetchPosition,
      byte[] after,
      SelectAfterMode afterMode,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> select(
      Integer spaceId,
      String spaceName,
      Integer indexId,
      String indexName,
      byte[] key,
      int limit,
      int offset,
      BoxIterator iterator,
      boolean fetchPosition,
      byte[] after,
      SelectAfterMode afterMode,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> insert(int spaceId, ArrayValue tuple);

  CompletableFuture<IProtoResponse> insert(int spaceId, byte[] tuple);

  CompletableFuture<IProtoResponse> insert(
      Integer spaceId, String spaceName, ArrayValue tuple, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> insert(
      Integer spaceId, String spaceName, byte[] tuple, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> replace(int spaceId, ArrayValue tuple);

  CompletableFuture<IProtoResponse> replace(int spaceId, byte[] tuple);

  CompletableFuture<IProtoResponse> replace(
      Integer spaceId, String spaceName, ArrayValue tuple, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> replace(
      Integer spaceId, String spaceName, byte[] tuple, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> update(
      int spaceId, int indexId, ArrayValue key, ArrayValue operations);

  CompletableFuture<IProtoResponse> update(int spaceId, int indexId, byte[] key, byte[] operations);

  CompletableFuture<IProtoResponse> update(
      Integer spaceId,
      final String spaceName,
      Integer indexId,
      final String indexName,
      ArrayValue key,
      ArrayValue operations,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> update(
      Integer spaceId,
      final String spaceName,
      Integer indexId,
      final String indexName,
      byte[] key,
      byte[] operations,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> upsert(
      int spaceId, int indexBase, ArrayValue tuple, ArrayValue operations);

  CompletableFuture<IProtoResponse> upsert(
      int spaceId, int indexBase, byte[] tuple, byte[] operations);

  CompletableFuture<IProtoResponse> upsert(
      Integer spaceId,
      final String spaceName,
      Integer indexBaseId,
      ArrayValue tuple,
      ArrayValue operations,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> upsert(
      Integer spaceId,
      final String spaceName,
      Integer indexBaseId,
      byte[] tuple,
      byte[] operations,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> delete(int spaceId, int indexId, ArrayValue key);

  CompletableFuture<IProtoResponse> delete(int spaceId, int indexId, byte[] key);

  CompletableFuture<IProtoResponse> delete(
      Integer spaceId,
      final String spaceName,
      Integer indexId,
      final String indexName,
      ArrayValue key,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> delete(
      Integer spaceId,
      final String spaceName,
      Integer indexId,
      final String indexName,
      byte[] key,
      IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> call(String function, ArrayValue args);

  CompletableFuture<IProtoResponse> call(String function, byte[] args);

  CompletableFuture<IProtoResponse> call(String function, ArrayValue args, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> call(
      String function, byte[] args, byte[] formats, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> eval(String expression, ArrayValue args);

  CompletableFuture<IProtoResponse> eval(String expression, byte[] args);

  CompletableFuture<IProtoResponse> eval(
      String expression, ArrayValue args, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> eval(
      String expression, byte[] args, byte[] formats, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> execute(
      long statementId, ArrayValue sqlBind, ArrayValue options);

  CompletableFuture<IProtoResponse> execute(long statementId, byte[] sqlBind, byte[] options);

  CompletableFuture<IProtoResponse> execute(
      long statementId, ArrayValue sqlBind, ArrayValue options, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> execute(
      long statementId, byte[] sqlBind, byte[] options, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> execute(
      String statementText, ArrayValue sqlBind, ArrayValue options);

  CompletableFuture<IProtoResponse> execute(String statementText, byte[] sqlBind, byte[] options);

  CompletableFuture<IProtoResponse> execute(
      String statementText, ArrayValue sqlBind, ArrayValue options, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> execute(
      String statementText, byte[] sqlBind, byte[] options, IProtoRequestOpts opts);

  long allocateStreamId();

  CompletableFuture<IProtoResponse> begin(
      Long streamId, long timeout, TransactionIsolationLevel level);

  CompletableFuture<IProtoResponse> begin(
      Long streamId, long timeout, TransactionIsolationLevel level, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> commit(Long streamId);

  CompletableFuture<IProtoResponse> commit(Long streamId, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> rollback(Long streamId);

  CompletableFuture<IProtoResponse> rollback(Long streamId, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> id(int protocolVersion, List<Integer> features);

  CompletableFuture<IProtoResponse> id(
      int protocolVersion, List<Integer> features, IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> ping();

  CompletableFuture<IProtoResponse> ping(IProtoRequestOpts opts);

  CompletableFuture<IProtoResponse> prepare(String statementText);

  CompletableFuture<IProtoResponse> prepare(String statementText, IProtoRequestOpts opts);

  void watch(String key, Consumer<IProtoResponse> callback);

  CompletableFuture<IProtoResponse> watchOnce(String key);

  CompletableFuture<IProtoResponse> watchOnce(String key, IProtoRequestOpts opts);

  void unwatch(String key);

  void close() throws Exception;

  IProtoClient onClose(ConnectionCloseEvent event, BiConsumer<IProtoClient, Throwable> handler);

  IProtoClient onIgnoredPacket(Consumer<IProtoResponse> handler);

  boolean isConnected();

  Integer getClientProtocolVersion();

  Integer getServerProtocolVersion();

  Set<IProtoFeature> getClientFeatures();

  Set<IProtoFeature> getServerFeatures();

  boolean isFeatureEnabled(IProtoFeature feature);

  boolean hasTupleExtension();

  void pause();

  void resume();

  void setIdleTimeout(int idleTimeout);

  boolean isPaused();
}
