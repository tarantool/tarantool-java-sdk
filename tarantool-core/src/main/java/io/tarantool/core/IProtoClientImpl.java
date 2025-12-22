/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.util.Timer;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.Value;
import org.msgpack.value.impl.ImmutableBooleanValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.tarantool.core.IProtoFeature.DML_TUPLE_EXTENSION;
import static io.tarantool.core.IProtoFeature.PROTOCOL_VERSION;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EVENT;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EVENT_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EVENT_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_FEATURES;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_VERSION;
import io.tarantool.core.connection.Connection;
import io.tarantool.core.connection.ConnectionCloseEvent;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.core.connection.Greeting;
import io.tarantool.core.exceptions.ClientException;
import io.tarantool.core.exceptions.ShutdownException;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoRequest;
import io.tarantool.core.protocol.IProtoRequestOpts;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.TransactionIsolationLevel;
import io.tarantool.core.protocol.fsm.IProtoStateMachine;
import io.tarantool.core.protocol.fsm.RequestStateMachine;
import io.tarantool.core.protocol.fsm.WatcherStateMachine;
import io.tarantool.core.protocol.requests.IProtoAuth;
import io.tarantool.core.protocol.requests.IProtoBegin;
import io.tarantool.core.protocol.requests.IProtoCall;
import io.tarantool.core.protocol.requests.IProtoCommit;
import io.tarantool.core.protocol.requests.IProtoDelete;
import io.tarantool.core.protocol.requests.IProtoEval;
import io.tarantool.core.protocol.requests.IProtoExecute;
import io.tarantool.core.protocol.requests.IProtoId;
import io.tarantool.core.protocol.requests.IProtoInsert;
import io.tarantool.core.protocol.requests.IProtoPing;
import io.tarantool.core.protocol.requests.IProtoPrepare;
import io.tarantool.core.protocol.requests.IProtoReplace;
import io.tarantool.core.protocol.requests.IProtoRollback;
import io.tarantool.core.protocol.requests.IProtoSelect;
import io.tarantool.core.protocol.requests.IProtoUnwatch;
import io.tarantool.core.protocol.requests.IProtoUpdate;
import io.tarantool.core.protocol.requests.IProtoUpsert;
import io.tarantool.core.protocol.requests.IProtoWatchOnce;
import io.tarantool.core.protocol.requests.SelectAfterMode;

public class IProtoClientImpl implements IProtoClient {

  public static final IProtoAuth.AuthType DEFAULT_AUTH_TYPE = IProtoAuth.AuthType.CHAP_SHA1;
  public static final IProtoRequestOpts DEFAULT_REQUEST_OPTS = IProtoRequestOpts
      .empty()
      .withRequestTimeout(5000);
  public static final WatcherOptions DEFAULT_WATCHER_OPTS = WatcherOptions
      .builder()
      .build();
  private static final Set<IProtoFeature> FEATURES_SET_ENUM = EnumSet.allOf(IProtoFeature.class);
  private static final List<Integer> FEATURES_LIST_INT = Arrays.stream(IProtoFeature.values())
      .map(Enum::ordinal)
      .collect(Collectors.toList());
  private static final int DML_TUPLE_EXTENSION_INT = IProtoFeature.DML_TUPLE_EXTENSION.ordinal();
  private static final int CALL_RET_TUPLE_EXTENSION_INT = IProtoFeature.CALL_RET_TUPLE_EXTENSION.ordinal();
  private static final String CALL_CONNECT_BEFORE_GETTING_SERVER_DETAILS =
      "Call connect before getting server details";
  private static final String SHUTDOWN_EVENT_KEY = "box.shutdown";
  private static final int DEFAULT_IDLE_TIMEOUT = 1_000;
  private static final Logger log = LoggerFactory.getLogger(IProtoClientImpl.class);
  private final AtomicLong streamIdSequence;
  private final AtomicLong syncIdSequence;
  protected final Connection connection;
  private final List<String> toUnwatch;
  protected final Map<Long, IProtoStateMachine> fsmRegistry;
  private final Map<String, Watcher> watchers;
  protected final Timer timerService;
  private final WatcherOptions watcherOpts;
  private CompletableFuture<Integer> serverProtocolVersion;
  private CompletableFuture<EnumSet<IProtoFeature>> serverFeatures;
  private Set<IProtoFeature> clientFeaturesEnum;
  private List<Integer> clientFeaturesList;
  private LongTaskTimer requestTimer;
  private Counter requestCounter;
  private Counter responseSuccessCounter;
  private Counter responseErrorCounter;
  private Counter ignoredResponsesCounter;
  private Consumer<IProtoResponse> ignoredPacketsHandler;

  public IProtoClientImpl(ConnectionFactory factory,
      Timer timerService) {
    this(factory, timerService, DEFAULT_WATCHER_OPTS, null, null, false);
  }

  public IProtoClientImpl(ConnectionFactory factory,
      Timer timerService,
      WatcherOptions watcherOpts,
      MeterRegistry metricsRegistry,
      FlushConsolidationHandler flushConsolidationHandler,
      boolean useTupleExtension) {
    if (metricsRegistry != null) {
      requestTimer = metricsRegistry.get("request.timer").longTaskTimer();
      requestCounter = metricsRegistry.get("request.counter").counter();
      responseSuccessCounter = metricsRegistry.get("response.success").counter();
      responseErrorCounter = metricsRegistry.get("response.errors").counter();
      ignoredResponsesCounter = metricsRegistry.get("response.ignored").counter();
    }
    this.connection = factory
        .create(flushConsolidationHandler, this::handleIdleTimeout)
        .listen(this::handleMessage)
        .onClose(ConnectionCloseEvent.CLOSE_BY_REMOTE, this::handleClose)
        .onClose(ConnectionCloseEvent.CLOSE_BY_CLIENT, this::handleClose);
    this.fsmRegistry = new ConcurrentHashMap<>();
    this.watchers = new ConcurrentHashMap<>();
    this.syncIdSequence = new AtomicLong(0);
    this.streamIdSequence = new AtomicLong(0);
    this.timerService = timerService;
    this.toUnwatch = new ArrayList<>();
    if (watcherOpts == null) {
      this.watcherOpts = DEFAULT_WATCHER_OPTS;
    } else {
      this.watcherOpts = watcherOpts;
    }
    this.ignoredPacketsHandler = (packet) -> {};

    clientFeaturesEnum = FEATURES_SET_ENUM;
    clientFeaturesList = FEATURES_LIST_INT;
    if (!useTupleExtension) {
      clientFeaturesEnum = FEATURES_SET_ENUM.stream()
          .filter(
              feature -> !feature.equals(IProtoFeature.DML_TUPLE_EXTENSION) &&
                  !feature.equals(IProtoFeature.CALL_RET_TUPLE_EXTENSION)
          )
          .collect(Collectors.toSet());
      clientFeaturesList = FEATURES_LIST_INT.stream()
          .filter(
              feature -> !feature.equals(DML_TUPLE_EXTENSION_INT) &&
                  !feature.equals(CALL_RET_TUPLE_EXTENSION_INT)
          )
          .collect(Collectors.toList());
    }
    clientFeaturesEnum = Collections.unmodifiableSet(clientFeaturesEnum);

    serverProtocolVersion = new CompletableFuture<>();
    serverFeatures = new CompletableFuture<>();
    serverProtocolVersion.completeExceptionally(new ClientException(CALL_CONNECT_BEFORE_GETTING_SERVER_DETAILS));
    serverFeatures.completeExceptionally(new ClientException(CALL_CONNECT_BEFORE_GETTING_SERVER_DETAILS));
  }

  @Override
  public CompletableFuture<Void> connect(InetSocketAddress address, long timeoutMs) {
    return connect(address, timeoutMs, true);
  }

  @Override
  public CompletableFuture<Void> connect(InetSocketAddress address, long timeoutMs, boolean gracefulShutdown) {
    if (gracefulShutdown) {
      // it does not send watch message if connection is not connected,
      // it sends immediately after successful connect
      watch(SHUTDOWN_EVENT_KEY, this::shutdownEventCallback);
    }

    serverProtocolVersion = new CompletableFuture<>();
    serverFeatures = new CompletableFuture<>();
    return connection
        .connect(address, timeoutMs)
        .thenRun(() -> {
          updateWatchers();
          updateServerInfo();
        });
  }

  @Override
  public CompletableFuture<IProtoResponse> authorize(String user, String password) {
    return authorize(user, password, DEFAULT_REQUEST_OPTS, DEFAULT_AUTH_TYPE);
  }

  @Override
  public CompletableFuture<IProtoResponse> authorize(String user, String password, IProtoRequestOpts opts) {
    return authorize(user, password, opts, DEFAULT_AUTH_TYPE);
  }

  @Override
  public CompletableFuture<IProtoResponse> authorize(String user, String password, IProtoAuth.AuthType authType) {
    return authorize(user, password, DEFAULT_REQUEST_OPTS, authType);
  }

  @Override
  public CompletableFuture<IProtoResponse> authorize(String user, String password, IProtoRequestOpts opts,
      IProtoAuth.AuthType authType) {
    Optional<Greeting> greeting = connection.getGreeting();
    if (!greeting.isPresent()) {
      CompletableFuture<IProtoResponse> promise = new CompletableFuture<>();
      promise.completeExceptionally(new ClientException("No greeting, connect firstly!"));
      return promise;
    }
    return runRequest(new IProtoAuth(
        user,
        password,
        greeting.get().getSalt(),
        authType
    ), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> select(int spaceId,
      int indexId,
      ArrayValue key,
      int limit,
      int offset,
      BoxIterator iterator) {
    return select(spaceId, null, indexId, null, key, limit, offset, iterator,
        false, null, null, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> select(int spaceId, int indexId, byte[] key, int limit, int offset,
      BoxIterator iterator) {
    return select(spaceId, null, indexId, null, key, limit, offset, iterator,
        false, null, null, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> select(Integer spaceId,
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
      IProtoRequestOpts opts) {
    return runRequest(new IProtoSelect(
        spaceId,
        spaceName,
        indexId,
        indexName,
        limit,
        offset,
        iterator.getCode(),
        key,
        opts.getStreamId(),
        fetchPosition,
        after,
        afterMode
    ), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> select(Integer spaceId,
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
      IProtoRequestOpts opts) {
    return runRequest(new IProtoSelect(
        spaceId,
        spaceName,
        indexId,
        indexName,
        limit,
        offset,
        iterator.getCode(),
        key,
        opts.getStreamId(),
        fetchPosition,
        after,
        afterMode
    ), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> insert(int spaceId, ArrayValue tuple) {
    return insert(spaceId, null, tuple, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> insert(int spaceId, byte[] tuple) {
    return insert(spaceId, null, tuple, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> insert(Integer spaceId,
      String spaceName,
      ArrayValue tuple,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoInsert(spaceId, spaceName, tuple, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> insert(Integer spaceId,
      String spaceName,
      byte[] tuple,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoInsert(spaceId, spaceName, tuple, opts.getStreamId()), opts);
  }


  @Override
  public CompletableFuture<IProtoResponse> replace(int spaceId, ArrayValue tuple) {
    return replace(spaceId, null, tuple, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> replace(int spaceId, byte[] tuple) {
    return replace(spaceId, null, tuple, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> replace(Integer spaceId,
      String spaceName,
      ArrayValue tuple,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoReplace(spaceId, spaceName, tuple, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> replace(Integer spaceId,
      String spaceName,
      byte[] tuple,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoReplace(spaceId, spaceName, tuple, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> delete(int spaceId,
      int indexId,
      ArrayValue key) {
    return delete(spaceId, null, indexId, null, key, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> delete(int spaceId, int indexId, byte[] key) {
    return delete(spaceId, null, indexId, null, key, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> delete(Integer spaceId,
      String spaceName,
      Integer indexId,
      String indexName,
      ArrayValue key,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoDelete(spaceId, spaceName, indexId, indexName, key, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> delete(Integer spaceId,
      String spaceName,
      Integer indexId,
      String indexName,
      byte[] key,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoDelete(spaceId, spaceName, indexId, indexName, key, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> update(int spaceId,
      int indexId,
      ArrayValue key,
      ArrayValue operations) {
    return update(spaceId, null, indexId, null, key, operations, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> update(int spaceId, int indexId, byte[] key, byte[] operations) {
    return update(spaceId, null, indexId, null, key, operations, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> update(Integer spaceId,
      String spaceName,
      Integer indexId,
      String indexName,
      ArrayValue key,
      ArrayValue operations,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoUpdate(spaceId, spaceName, indexId, indexName, key, operations, opts.getStreamId()),
        opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> update(Integer spaceId,
      String spaceName,
      Integer indexId,
      String indexName, byte[] key, byte[] operations,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoUpdate(spaceId, spaceName, indexId, indexName, key, operations, opts.getStreamId()),
        opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> upsert(int spaceId,
      int indexBase,
      ArrayValue tuple,
      ArrayValue operations) {
    return upsert(spaceId, null, indexBase, tuple, operations, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> upsert(int spaceId, int indexBase, byte[] tuple, byte[] operations) {
    return upsert(spaceId, null, indexBase, tuple, operations, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> upsert(Integer spaceId,
      String spaceName,
      Integer indexBaseId,
      ArrayValue tuple,
      ArrayValue operations,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoUpsert(spaceId, spaceName, indexBaseId, tuple, operations, opts.getStreamId()),
        opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> upsert(Integer spaceId,
      String spaceName,
      Integer indexBaseId,
      byte[] tuple,
      byte[] operations,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoUpsert(spaceId, spaceName, indexBaseId, tuple, operations, opts.getStreamId()),
        opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> call(String function, ArrayValue args) {
    return call(function, args, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> call(String function, byte[] args) {
    return call(function, args, null, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> call(String function, ArrayValue args, IProtoRequestOpts opts) {
    return runRequest(new IProtoCall(function, args, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> call(
      String function,
      byte[] args,
      byte[] formats,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoCall(function, args, formats, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> eval(String expression, ArrayValue args) {
    return eval(expression, args, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> eval(String expression, byte[] args) {
    return eval(expression, args, null, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> eval(String expression,
      ArrayValue args,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoEval(expression, args, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> eval(
      String expression,
      byte[] args,
      byte[] formats,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoEval(expression, args, formats, opts.getStreamId()), opts);
  }

  @Override
  public long allocateStreamId() {
    return streamIdSequence.updateAndGet(n -> n == Long.MAX_VALUE ? 1 : n + 1);
  }

  @Override
  public CompletableFuture<IProtoResponse> begin(Long streamId, long streamTimeout, TransactionIsolationLevel level) {
    return begin(streamId, streamTimeout, level, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> begin(Long streamId,
      long streamTimeout,
      TransactionIsolationLevel level,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoBegin(streamId, streamTimeout, level), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> commit(Long streamId) {
    return commit(streamId, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> commit(Long streamId, IProtoRequestOpts opts) {
    return runRequest(new IProtoCommit(streamId), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> rollback(Long streamId) {
    return rollback(streamId, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> rollback(Long streamId, IProtoRequestOpts opts) {
    return runRequest(new IProtoRollback(streamId), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(long statementId,
      ArrayValue sqlBind,
      ArrayValue options) {
    return execute(statementId, sqlBind, options, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(long statementId, byte[] sqlBind, byte[] options) {
    return execute(statementId, sqlBind, options, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(long statementId,
      ArrayValue sqlBind,
      ArrayValue options,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoExecute(statementId, sqlBind, options, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(long statementId, byte[] sqlBind, byte[] options,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoExecute(statementId, sqlBind, options, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(String statementText,
      ArrayValue sqlBind,
      ArrayValue options) {
    return execute(statementText, sqlBind, options, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(String statementText, byte[] sqlBind, byte[] options) {
    return execute(statementText, sqlBind, options, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(String statementText,
      ArrayValue sqlBind,
      ArrayValue options,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoExecute(statementText, sqlBind, options, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> execute(String statementText, byte[] sqlBind, byte[] options,
      IProtoRequestOpts opts) {
    return runRequest(new IProtoExecute(statementText, sqlBind, options, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> ping() {
    return runRequest(new IProtoPing(), DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> ping(IProtoRequestOpts opts) {
    return runRequest(new IProtoPing(), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> prepare(String statementText) {
    return prepare(statementText, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> prepare(String statementText, IProtoRequestOpts opts) {
    return runRequest(new IProtoPrepare(statementText, opts.getStreamId()), opts);
  }

  @Override
  public CompletableFuture<IProtoResponse> id(int protocolVersion, List<Integer> features) {
    return id(protocolVersion, features, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> id(int protocolVersion, List<Integer> features, IProtoRequestOpts opts) {
    return runRequest(new IProtoId(protocolVersion, features), opts);
  }

  /**
   * If key exists then add new callback to watcher for this key, otherwise create new watcher with passed callback.
   * When client is offline it just stores callbacks and watchers and will send IPROTO_WATCH requests after connecting.
   * When client is online it will send subscription requests immediately.
   */
  @Override
  public void watch(String key, Consumer<IProtoResponse> callback) {
    watchers.compute(key, (k, watcher) -> {
      if (watcher == null) {
        watcher = new Watcher();
      }
      watcher.addRawCallback(callback);
      return watcher;
    });
    if (isConnected()) {
      updateWatchers();
    }
  }

  @Override
  public CompletableFuture<IProtoResponse> watchOnce(String key) {
    return watchOnce(key, DEFAULT_REQUEST_OPTS);
  }

  @Override
  public CompletableFuture<IProtoResponse> watchOnce(String key, IProtoRequestOpts opts) {
    return runRequest(new IProtoWatchOnce(key), opts);
  }

  /*
   * Remove watcher for this key. When client is offline it just marks this
   * key as unwatched and will send IPROTO_UNWATCH requests after connecting.
   * When client is online it will send unsubscription requests immediately.
   * */
  @Override
  public void unwatch(String key) {
    watchers.computeIfPresent(key, (k, watcher) -> {
      fsmRegistry.remove(watcher.getSyncId());
      toUnwatch.add(k);
      return watcher;
    });
    if (isConnected()) {
      updateWatchers();
    }
  }

  @Override
  public void close() throws Exception {
    log.info("close connection {}", connection);
    connection.close();
  }

  @Override
  public IProtoClient onClose(ConnectionCloseEvent event,
      BiConsumer<IProtoClient, Throwable> callback) {
    connection.onClose(event, (c, exc) -> callback.accept(this, exc));
    return this;
  }

  @Override
  public IProtoClient onIgnoredPacket(Consumer<IProtoResponse> handler) {
    if (handler == null) {
      throw new IllegalArgumentException("handler for ignored packets should not be null");
    }
    this.ignoredPacketsHandler = handler;
    return this;
  }

  @Override
  public boolean isConnected() {
    return connection.isConnected();
  }

  @Override
  public Integer getClientProtocolVersion() {
    return PROTOCOL_VERSION;
  }

  @Override
  public Integer getServerProtocolVersion() {
    return serverProtocolVersion.join();
  }

  @Override
  public Set<IProtoFeature> getClientFeatures() {
    return clientFeaturesEnum;
  }

  @Override
  public Set<IProtoFeature> getServerFeatures() {
    return Collections.unmodifiableSet(serverFeatures.join());
  }

  @Override
  public boolean isFeatureEnabled(IProtoFeature feature) {
    final Set<IProtoFeature> features = serverFeatures.join();
    return features.contains(feature);
  }

  @Override
  public boolean hasTupleExtension() {
    return getClientFeatures().contains(DML_TUPLE_EXTENSION) &&
        getServerFeatures().contains(DML_TUPLE_EXTENSION);
  }

  @Override
  public void pause() {
    this.connection.pause();
  }

  @Override
  public void resume() {
    this.connection.resume();
  }

  @Override
  public void setIdleTimeout(int idleTimeout) {
    if (idleTimeout <= 0) {
      log.warn("idleTimeout must be greater than 0");
      this.connection.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
    }
    this.connection.setIdleTimeout(idleTimeout);
  }

  @Override
  public boolean isPaused() {
    return connection.isPaused();
  }

  private synchronized void updateWatchers() {
    for (Map.Entry<String, Watcher> watcherEntry : watchers.entrySet()) {
      Watcher watcher = watchers.get(watcherEntry.getKey());
      WatcherStateMachine fsm = watcher.getStateContext();
      if (fsm == null) {
        long syncId = allocateSyncIds(1);
        fsm = new WatcherStateMachine(
            watcherEntry.getKey(),
            syncId,
            watcher,
            connection,
            watcherOpts,
            timerService
        );
        watcher.setStateContext(fsm);
        watcher.setSyncId(syncId);
        fsmRegistry.put(syncId, fsm);
        fsm.runOnce();
      }
    }
    for (String key : new ArrayList<>(toUnwatch)) {
      long syncId = allocateSyncIds(1);
      IProtoUnwatch request = new IProtoUnwatch(key);
      request.setSyncId(syncId);
      connection
          .send(request)
          .whenComplete((v, exc) -> {
            if (exc != null) {
              log.warn("could not send unwatch packet: %s", exc);
              return;
            }
            toUnwatch.remove(key);
          });
    }
  }

  protected long allocateSyncIds(int count) {
    // n + count < 0 is a check for overflow
    return syncIdSequence.updateAndGet(n -> (n + count) < 0 ? count : (n + count));
  }

  private void handleMessage(IProtoResponse message) {
    log.debug("IProtoClientImpl:handleMessage() - \"{}\"", message);
    long syncId = message.getSyncId();
    if (syncId == -1L) {
      if (message.getRequestType() == IPROTO_EVENT) {
        String key = message.getBodyStringValue(IPROTO_EVENT_KEY).asString();
        if (!watchers.containsKey(key)) {
          processIgnoredResponse(message);
          log.error("Client doesn't watch this key {}", key);
          return;
        }
        syncId = watchers.get(key).getSyncId();
      } else {
        processIgnoredResponse(message);
        log.error("Client cannot handle this message: {}", message);
        return;
      }
    }
    IProtoStateMachine fsm = fsmRegistry.get(syncId);
    if (fsm == null) {
      processIgnoredResponse(message);
    } else if (fsm.process(message)) {
      fsmRegistry.remove(syncId);
      if (fsm.hasNextAction() && fsm.next() != null) {
        fsm.next().runOnce();
      }
    }
  }

  private void shutdownEventCallback(IProtoResponse response) {
    Value v = response.getBodyValue(IPROTO_EVENT_DATA);
    if (!v.equals(ImmutableBooleanValueImpl.TRUE)) {
      return;
    }
    try {
      connection.shutdownClose();
      failAllRequests(new ShutdownException());
    } catch (Exception e) {
      throw new ShutdownException("Shutdown process failed");
    }
  }

  private void handleClose(Connection conn, Throwable exc) {
    failAllRequests(exc);
    watchers.forEach((key, watcher) -> {
      fsmRegistry.remove(watcher.getSyncId());
      watcher.setStateContext(null);
    });
  }

  private CompletableFuture<IProtoResponse> runRequest(IProtoRequest request, IProtoRequestOpts opts) {
    // init request metrics
    LongTaskTimer.Sample currentRequest = null;
    if (requestTimer != null) {
      currentRequest = requestTimer.start();
      requestCounter.increment();
    }

    // init request context
    CompletableFuture<IProtoResponse> resultPromise = new CompletableFuture<>();
    long syncId = allocateSyncIds(1);
    RequestStateMachine stateContext = new RequestStateMachine(connection, syncId, request, resultPromise, opts,
        fsmRegistry, timerService);

    // when completed stop timeout timer and metrics
    LongTaskTimer.Sample finalCurrentRequest = currentRequest;
    CompletableFuture<IProtoResponse> promiseWithStoppers =
        resultPromise.whenComplete((IProtoResponse resp, Throwable ex) -> {
          if (responseErrorCounter != null) {
            if (finalCurrentRequest != null) {
              finalCurrentRequest.stop();
            }
            if (ex != null) {
              responseErrorCounter.increment();
            } else {
              responseSuccessCounter.increment();
            }
          }
        });

    stateContext.runOnce();
    log.debug("Request \"{}\" created", request);

    return promiseWithStoppers;
  }

  private void failAllRequests(Throwable ex) {
    fsmRegistry.values().forEach(a -> a.kill(ex));
    fsmRegistry.clear();
  }

  private void processIgnoredResponse(IProtoResponse message) {
    ignoredPacketsHandler.accept(message);
    if (ignoredResponsesCounter != null) {
      ignoredResponsesCounter.increment();
    }
  }

  private void updateServerInfo() {
    id(PROTOCOL_VERSION, clientFeaturesList).whenComplete(
        (response, ex) -> {
          if (ex != null) {
            this.serverProtocolVersion.completeExceptionally(ex);
            this.serverFeatures.completeExceptionally(ex);
          }
          this.serverProtocolVersion.complete(response.getBodyIntegerValue(IPROTO_VERSION).asInt());
          ArrayValue rawFeatures = response.getBodyArrayValue(IPROTO_FEATURES);
          EnumSet<IProtoFeature> serverFeatures = EnumSet.noneOf(IProtoFeature.class);
          // Since there are a small number of features, the complexity is linear
          for (Value rawFeature : rawFeatures) {
            int featureNumber = rawFeature.asIntegerValue().asInt();
            if (featureNumber < FEATURES_SET_ENUM.size()) {
              serverFeatures.add(IProtoFeature.valueOf(featureNumber));
            }
          }
          this.serverFeatures.complete(serverFeatures);
        }
    );
  }

  private void handleIdleTimeout(Connection connection, Throwable exc) {
    if (exc == null || !connection.isConnected()) {
      return;
    }

    try {
      log.info("Connection {} closed via idle timeout", connection);
      this.close();
    } catch (Exception e) {
      log.warn("Cannot close client", e);
    }
  }
}
