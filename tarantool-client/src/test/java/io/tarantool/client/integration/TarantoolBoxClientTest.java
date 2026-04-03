/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.type.TypeReference;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;
import org.testcontainers.shaded.com.google.common.base.CaseFormat;

import static io.tarantool.client.box.TarantoolBoxSpace.WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_ERROR;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_SYNC_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_AUTH;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_CALL;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_DELETE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_EVAL;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_INSERT;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_PING;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_REPLACE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_SELECT;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_UPDATE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_UPSERT;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_FUNCTION_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TUPLE;
import static io.tarantool.mapping.BaseTarantoolJacksonMapping.objectMapper;
import io.tarantool.client.BaseOptions;
import io.tarantool.client.ClientType;
import io.tarantool.client.Options;
import io.tarantool.client.TarantoolSpace;
import io.tarantool.client.TarantoolVersion;
import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.client.box.TarantoolBoxSpace;
import io.tarantool.client.box.options.DeleteOptions;
import io.tarantool.client.box.options.SelectOptions;
import io.tarantool.client.box.options.UpdateOptions;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.client.operation.Operations;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.ByteBodyValueWrapper;
import io.tarantool.core.protocol.Handlers;
import io.tarantool.core.protocol.IProtoRequest;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.BaseTarantoolJacksonMapping;
import io.tarantool.mapping.NilErrorResponse;
import io.tarantool.mapping.SelectResponse;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.mapping.TupleMapper;
import io.tarantool.schema.NoSchemaException;
import io.tarantool.schema.Space;
import io.tarantool.schema.TarantoolSchemaFetcher;

@Timeout(value = 5)
public class TarantoolBoxClientTest extends BaseTest {

  private static TarantoolContainer<?> tt;
  public static final List<?> EMPTY_LIST = Collections.emptyList();
  private static Integer spacePersonId;
  private static TarantoolBoxClient client;
  private static TarantoolBoxClient clientWithoutFetcher;
  private static List<List<Object>> triplets;
  private static int tarantoolMajorVersion;
  private final TypeReference<Person> typeReferenceAsPersonClass = new TypeReference<Person>() {};
  private final TypeReference<List<?>> typeReferenceAsList = new TypeReference<List<?>>() {};

  @BeforeEach
  public void truncateSpaces() throws Exception {
    TarantoolContainerClientHelper.executeCommand(tt, "return box.space.person:truncate()");
    triplets = new ArrayList<>();
  }

  @BeforeAll
  public static void setUp() throws Exception {
    tt = TarantoolContainerClientHelper.createTarantoolContainer().withEnv(ENV_MAP);
    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);

    client =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withIgnoredPacketsHandler(
                (tag, index, packet) -> {
                  synchronized (triplets) {
                    triplets.add(Arrays.asList(tag, index, packet));
                  }
                })
            .build();

    clientWithoutFetcher =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withFetchSchema(false)
            .withIgnoredPacketsHandler(
                (tag, index, packet) -> {
                  synchronized (triplets) {
                    triplets.add(Arrays.asList(tag, index, packet));
                  }
                })
            .build();

    List<?> result =
        TarantoolContainerClientHelper.executeCommandDecoded(tt, "return box.space.person.id");
    spacePersonId = (Integer) result.get(0);

    try {
      tarantoolMajorVersion =
          Character.getNumericValue(System.getenv("TARANTOOL_VERSION").charAt(0));
    } catch (Exception e) {
      tarantoolMajorVersion = 2;
    }
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  public static Stream<Arguments> dataForNPETest() {
    return Stream.of(
        // insert
        Arguments.of((Consumer<TarantoolBoxSpace>) (space) -> space.insert(null).join(), "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.insert(null, BaseOptions.builder().build()).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>) (space) -> space.insert(null, Person.class).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.insert(null, BaseOptions.builder().build(), Person.class).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.insert(new Person(1, true, "Dima"), null, Person.class).join(),
            "options"),
        // select
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.select(EMPTY_LIST, null, Person.class).join(),
            "options"),
        // delete
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.delete(EMPTY_LIST, null, Person.class).join(),
            "options"),
        // replace
        Arguments.of((Consumer<TarantoolBoxSpace>) (space) -> space.replace(null).join(), "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.replace(null, BaseOptions.builder().build()).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>) (space) -> space.replace(null, Person.class).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.replace(null, BaseOptions.builder().build(), Person.class).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.replace(new Person(1, true, "Dima"), null, Person.class).join(),
            "options"),
        // update
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.update(null, (List<List<?>>) null).join(),
            "key"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.update(EMPTY_LIST, (List<List<?>>) null).join(),
            "operations"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) ->
                    space
                        .update(
                            Collections.emptyList(), Collections.emptyList(), null, Person.class)
                        .join(),
            "options"),
        // upsert
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.upsert(null, (List<List<?>>) null).join(),
            "tuple"),
        Arguments.of(
            (Consumer<TarantoolBoxSpace>)
                (space) -> space.upsert(EMPTY_LIST, (List<List<?>>) null).join(),
            "operations"));
  }

  @ParameterizedTest
  @MethodSource("dataForNPETest")
  public void testNPE(Consumer<TarantoolBoxSpace> consumer, String prefix) {
    TarantoolBoxSpace space = client.space(spacePersonId);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> consumer.accept(space));
    assertEquals(String.format("%s can't be null", prefix), ex.getMessage());
  }

  @Test
  public void testUserPassword() throws Exception {
    assertEquals(
        Collections.singletonList("api_user"), client.call("box.session.user").join().get());
    TarantoolBoxClient userA =
        TarantoolFactory.box()
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withUser("user_a")
            .withPassword("secret_a")
            .build();
    assertEquals(Collections.singletonList("user_a"), userA.call("box.session.user").join().get());
  }

  @Test
  public void testCallAndEval() {
    assertEquals(Collections.singletonList(10), client.eval("return 10").join().get());
    assertEquals(Collections.singletonList(true), client.call("return_true").join().get());
  }

  @Test
  public void testCallTimeoutWithIgnoredPacketsHandler() throws Exception {
    List<IProtoRequest> sentCallRequests = new ArrayList<>();
    List<IProtoRequest> timedOutRequests = new ArrayList<>();
    List<IProtoResponse> successResponses = new ArrayList<>();
    List<IProtoResponse> ignoredResponses = new ArrayList<>();
    List<List<Object>> localTriplets = new ArrayList<>();

    TarantoolBoxClient testClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withHandlers(
                Handlers.builder()
                    .onBeforeSend(
                        request -> {
                          // Filter only CALL requests (ignore auth, schema fetch, etc.)
                          if (request.getRequestType() == IPROTO_TYPE_CALL) {
                            synchronized (sentCallRequests) {
                              sentCallRequests.add(request);
                            }
                          }
                        })
                    .onSuccess(
                        response -> {
                          synchronized (successResponses) {
                            successResponses.add(response);
                          }
                        })
                    .onTimeout(
                        request -> {
                          synchronized (timedOutRequests) {
                            timedOutRequests.add(request);
                          }
                        })
                    .onIgnoredResponse(
                        response -> {
                          synchronized (ignoredResponses) {
                            ignoredResponses.add(response);
                          }
                        })
                    .build())
            .withIgnoredPacketsHandler(
                (tag, index, packet) -> {
                  synchronized (localTriplets) {
                    localTriplets.add(Arrays.asList(tag, index, packet));
                  }
                })
            .build();

    Options options = BaseOptions.builder().withTimeout(1_000L).build();
    Exception ex =
        assertThrows(
            CompletionException.class,
            () -> testClient.call("slow_echo", Arrays.asList(1, true), options).join());
    Throwable cause = ex.getCause();
    assertEquals(TimeoutException.class, cause.getClass());

    // Verify onBeforeSend was called for our CALL request
    assertEquals(
        1, sentCallRequests.size(), "onBeforeSend handler should be called once for CALL request");
    IProtoRequest sentRequest = sentCallRequests.get(0);
    assertEquals(IPROTO_TYPE_CALL, sentRequest.getRequestType());

    Thread.sleep(600);
    assertEquals(1, localTriplets.size());

    // Verify onSuccess was NOT called for timed out request
    // Filter success responses for our CALL request syncId only
    long sentSyncId = sentRequest.getSyncId();
    List<IProtoResponse> matchingSuccessResponses = new ArrayList<>();
    synchronized (successResponses) {
      for (IProtoResponse response : successResponses) {
        if (response.hasSyncId() && response.getSyncId() == sentSyncId) {
          matchingSuccessResponses.add(response);
        }
      }
    }
    assertEquals(
        0,
        matchingSuccessResponses.size(),
        "onSuccess should NOT be called for timed out request (syncId=" + sentSyncId + ")");

    // Verify onBeforeSend, onTimeout, onIgnoredResponse and ignored packets all have matching
    // syncId
    long timeoutSyncId = assertRequestHandler(timedOutRequests, "onTimeout");
    long ignoredResponseSyncId = assertIgnoredResponseHandler(ignoredResponses);
    long tripletSyncId = assertIgnoredPackets(localTriplets);
    assertEquals(sentSyncId, timeoutSyncId, "onBeforeSend and onTimeout should have same syncId");
    assertEquals(
        sentSyncId,
        ignoredResponseSyncId,
        "onBeforeSend and onIgnoredResponse should have same syncId");
    assertEquals(sentSyncId, tripletSyncId, "Request and triplet response syncId should match");

    testClient.close();
  }

  @Test
  public void testCallSuccessWithHandlers() throws Exception {
    List<IProtoRequest> sentCallRequests = new ArrayList<>();
    List<IProtoRequest> timedOutRequests = new ArrayList<>();
    List<IProtoResponse> successResponses = new ArrayList<>();
    List<IProtoResponse> ignoredResponses = new ArrayList<>();
    List<List<Object>> localTriplets = new ArrayList<>();

    TarantoolBoxClient testClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withHandlers(
                Handlers.builder()
                    .onBeforeSend(
                        request -> {
                          // Filter only CALL requests (ignore auth, schema fetch, etc.)
                          if (request.getRequestType() == IPROTO_TYPE_CALL) {
                            synchronized (sentCallRequests) {
                              sentCallRequests.add(request);
                            }
                          }
                        })
                    .onSuccess(
                        response -> {
                          synchronized (successResponses) {
                            successResponses.add(response);
                          }
                        })
                    .onTimeout(
                        request -> {
                          synchronized (timedOutRequests) {
                            timedOutRequests.add(request);
                          }
                        })
                    .onIgnoredResponse(
                        response -> {
                          synchronized (ignoredResponses) {
                            ignoredResponses.add(response);
                          }
                        })
                    .build())
            .withIgnoredPacketsHandler(
                (tag, index, packet) -> {
                  synchronized (localTriplets) {
                    localTriplets.add(Arrays.asList(tag, index, packet));
                  }
                })
            .build();

    // Call non-slow function that should succeed
    List<?> result = testClient.call("echo", Arrays.asList(1, true)).join().get();
    assertEquals(Arrays.asList(1, true), result);

    // Verify onBeforeSend was called for our CALL request
    assertEquals(
        1, sentCallRequests.size(), "onBeforeSend handler should be called once for CALL request");
    IProtoRequest sentRequest = sentCallRequests.get(0);
    assertEquals(IPROTO_TYPE_CALL, sentRequest.getRequestType());

    // Verify onSuccess was called for our CALL request
    long sentSyncId = sentRequest.getSyncId();
    List<IProtoResponse> matchingSuccessResponses = new ArrayList<>();
    synchronized (successResponses) {
      for (IProtoResponse response : successResponses) {
        if (response.hasSyncId() && response.getSyncId() == sentSyncId) {
          matchingSuccessResponses.add(response);
        }
      }
    }
    assertEquals(
        1,
        matchingSuccessResponses.size(),
        "onSuccess should be called for successful request (syncId=" + sentSyncId + ")");

    // Verify onTimeout was NOT called
    assertEquals(
        0, timedOutRequests.size(), "onTimeout should NOT be called for successful request");

    // Verify onIgnoredResponse was NOT called
    assertEquals(
        0,
        ignoredResponses.size(),
        "onIgnoredResponse should NOT be called for successful request");

    // Verify triplet handler was NOT called
    assertEquals(
        0,
        localTriplets.size(),
        "withIgnoredPacketsHandler should NOT be called for successful request");

    // Verify request body in onBeforeSend
    byte[] packetBytes = sentRequest.getPacket(MessagePack.newDefaultBufferPacker());
    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packetBytes);
    unpacker.unpackInt(); // Skip size prefix
    Value headerValue = unpacker.unpackValue();
    Value bodyValue = unpacker.unpackValue();
    MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
    packer.packValue(bodyValue);
    byte[] bodyBytes = packer.toByteArray();
    Map<Integer, Object> body =
        objectMapper.readValue(bodyBytes, new TypeReference<Map<Integer, Object>>() {});
    assertEquals("echo", body.get(0x22)); // IPROTO_FUNCTION_NAME
    assertEquals(Arrays.asList(1, true), body.get(0x21)); // IPROTO_TUPLE

    // Verify response body in onSuccess
    IProtoResponse successResponse = matchingSuccessResponses.get(0);
    assertEquals(
        sentSyncId,
        successResponse.getSyncId(),
        "onSuccess response should have same syncId as sent request");
    Map<Integer, Object> responseBody = new HashMap<>();
    Map<Integer, ByteBodyValueWrapper> byteBodyValues = successResponse.getByteBodyValues();
    for (Map.Entry<Integer, ByteBodyValueWrapper> entry : byteBodyValues.entrySet()) {
      responseBody.put(
          entry.getKey(), BaseTarantoolJacksonMapping.readValue(entry.getValue(), Object.class));
    }
    assertEquals(Arrays.asList(1, true), responseBody.get(IPROTO_DATA));

    testClient.close();
  }

  private long assertRequestHandler(List<IProtoRequest> requests, String handlerName)
      throws Exception {
    assertEquals(
        1, requests.size(), handlerName + " handler should receive exactly one CALL request");
    IProtoRequest request = requests.get(0);
    assertEquals(IPROTO_TYPE_CALL, request.getRequestType());

    byte[] packetBytes = request.getPacket(MessagePack.newDefaultBufferPacker());
    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packetBytes);
    unpacker.unpackInt(); // Skip size prefix

    Value headerValue = unpacker.unpackValue();
    Value bodyValue = unpacker.unpackValue();

    // Extract syncId from header (key 0x01)
    long syncId =
        headerValue
            .asMapValue()
            .map()
            .get(ValueFactory.newInteger(IPROTO_SYNC_ID))
            .asIntegerValue()
            .asLong();

    // Convert body to bytes and parse with objectMapper
    MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
    packer.packValue(bodyValue);
    byte[] bodyBytes = packer.toByteArray();
    Map<Integer, Object> body =
        objectMapper.readValue(bodyBytes, new TypeReference<Map<Integer, Object>>() {});

    assertEquals("slow_echo", body.get(0x22)); // IPROTO_FUNCTION_NAME
    assertEquals(Arrays.asList(1, true), body.get(0x21)); // IPROTO_TUPLE

    return syncId;
  }

  private long assertIgnoredResponseHandler(List<IProtoResponse> responses) throws Exception {
    assertEquals(
        1, responses.size(), "onIgnoredResponse handler should receive exactly one response");
    return assertIProtoResponse(responses.get(0));
  }

  private long assertIgnoredPackets(List<List<Object>> triplets) throws Exception {
    Set<String> tags = new HashSet<>();
    Set<Integer> indexes = new HashSet<>();
    long syncId = -1;

    for (List<Object> item : triplets) {
      tags.add((String) item.get(0));
      indexes.add((int) item.get(1));
      assertInstanceOf(IProtoResponse.class, item.get(2));
      syncId = assertIProtoResponse((IProtoResponse) item.get(2));
    }

    assertEquals(new HashSet<>(Collections.singletonList("default")), tags);
    assertEquals(Collections.singleton(0), indexes);

    return syncId;
  }

  private long assertIProtoResponse(IProtoResponse response) throws Exception {
    assertFalse(response.isError());
    assertTrue(response.hasSyncId());
    long syncId = response.getSyncId();
    assertTrue(syncId > 0);

    Map<Integer, Object> bodyAsObjects = new HashMap<>();
    Map<Integer, ByteBodyValueWrapper> byteBodyValues = response.getByteBodyValues();
    for (Map.Entry<Integer, ByteBodyValueWrapper> entry : byteBodyValues.entrySet()) {
      bodyAsObjects.put(
          entry.getKey(), BaseTarantoolJacksonMapping.readValue(entry.getValue(), Object.class));
    }
    assertEquals(byteBodyValues.size(), bodyAsObjects.size());
    assertEquals(Arrays.asList(1, true), bodyAsObjects.get(IPROTO_DATA));

    return syncId;
  }

  @Test
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testSchemaFetcher() throws Exception {
    TarantoolBoxClient customClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withFetchSchema(false)
            .build();
    Person person = new Person(1, true, "Dima");
    customClient.space(spacePersonId).insert(person).join();
    assertEquals(
        Collections.singletonList(person),
        unpackT(customClient.space(spacePersonId).select(EMPTY_LIST, Person.class).join()));
    Throwable cause =
        assertThrows(
                CompletionException.class,
                () -> customClient.space("person").select(EMPTY_LIST).join())
            .getCause();
    assertInstanceOf(IllegalArgumentException.class, cause);
    assertEquals(WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testDMLTupleExtension() {
    assertNotNull(client.getFetcher());
    assertNull(clientWithoutFetcher.getFetcher());

    // eval
    String expression =
        "return box.tuple.new("
            + "{'1', '1'}, {format = {{ 'id', type = 'string' }, { 'value', type = 'string' }} })";
    TarantoolResponse<List<?>> tupleFromEval = clientWithoutFetcher.eval(expression).join();
    Assertions.assertFalse(tupleFromEval.getFormats().isEmpty());
    List<List<String>> evalExpected = Collections.singletonList(Arrays.asList("1", "1"));
    assertEquals(evalExpected, unpackT((List<Tuple<List<?>>>) tupleFromEval.get()));
    tupleFromEval = client.eval(expression).join();
    Assertions.assertTrue(tupleFromEval.getFormats().isEmpty());
    assertEquals(evalExpected, tupleFromEval.get());

    Person person = new Person(1, true, "Dima");
    client.space(spacePersonId).insert(person).join();
    SelectResponse<List<Tuple<Person>>> selectResult =
        client.space(spacePersonId).select(EMPTY_LIST, Person.class).join();
    SelectResponse<List<Tuple<Person>>> selectWithoutFetcherResult =
        clientWithoutFetcher.space(spacePersonId).select(EMPTY_LIST, Person.class).join();

    // check response format existing
    assertTrue(selectResult.getFormats().isEmpty());
    assertFalse(selectWithoutFetcherResult.getFormats().isEmpty());

    // check data
    assertEquals(Collections.singletonList(person), unpackT(selectResult));
    assertEquals(Collections.singletonList(person), unpackT(selectWithoutFetcherResult));

    Tuple<Person> tuple = selectResult.get().get(0);
    Tuple<Person> tupleWithoutFetcher = selectWithoutFetcherResult.get().get(0);

    // check tuple format and format id existing
    assertTrue(tuple.getFormat().isEmpty());
    assertNull(tuple.getFormatId());
    assertFalse(tupleWithoutFetcher.getFormat().isEmpty());
    assertNotNull(tupleWithoutFetcher.getFormatId());
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testMappingWithFormat() {
    Person person = new Person(1, true, "Dima");
    client.space(spacePersonId).insert(person).join();

    SelectResponse<List<Tuple<Person>>> selectWithoutFetcherResult =
        clientWithoutFetcher.space(spacePersonId).select(EMPTY_LIST, Person.class).join();

    Tuple<Person> tupleWithoutFetcher = selectWithoutFetcherResult.get().get(0);

    // check format data
    List<io.tarantool.mapping.Field> format;
    Field[] fields = Person.class.getFields();

    // with fetcher, we can get format only by this fetcher
    TarantoolSchemaFetcher fetcher = client.getFetcher();
    Space spaceInfo = fetcher.getSpace("person");
    format = spaceInfo.getFormat();
    assertFormat(fields, format);

    // without fetcher, we have format data besides tuple
    format = tupleWithoutFetcher.getFormat();
    assertFormat(fields, format);

    // tupleExt:
    Person resultWithJsonFormatArray = tupleWithoutFetcher.get();
    PersonWithDifferentFieldsOrder resultWithFormatUsing =
        clientWithoutFetcher
            .space(spacePersonId)
            .select(EMPTY_LIST)
            .thenApply(mapSelectResponseToListOfPOJO(Tuple::getFormat))
            .join()
            .get(0);
    assertEquals(resultWithJsonFormatArray.getId(), resultWithFormatUsing.getId());
    assertEquals(resultWithJsonFormatArray.getName(), resultWithFormatUsing.getName());
    assertEquals(resultWithJsonFormatArray.getIsMarried(), resultWithFormatUsing.getIsMarried());
    // fetcher:
    PersonWithDifferentFieldsOrder resultWithFetcherFormatUsing =
        client
            .space(spacePersonId)
            .select(EMPTY_LIST)
            .thenApply(mapSelectResponseToListOfPOJO(t -> spaceInfo.getFormat()))
            .join()
            .get(0);
    assertEquals(resultWithJsonFormatArray.getId(), resultWithFetcherFormatUsing.getId());
    assertEquals(resultWithJsonFormatArray.getName(), resultWithFetcherFormatUsing.getName());
    assertEquals(
        resultWithJsonFormatArray.getIsMarried(), resultWithFetcherFormatUsing.getIsMarried());
  }

  private static Function<
          SelectResponse<List<Tuple<List<?>>>>, List<PersonWithDifferentFieldsOrder>>
      mapSelectResponseToListOfPOJO(
          Function<Tuple<List<?>>, List<io.tarantool.mapping.Field>> formatGetter) {
    return list -> {
      List<PersonWithDifferentFieldsOrder> result = new ArrayList<>();

      for (Tuple<List<?>> t : list.get()) {
        List<io.tarantool.mapping.Field> tupleFormat = formatGetter.apply(t);
        result.add(
            TupleMapper.mapToPojo(t.get(), tupleFormat, PersonWithDifferentFieldsOrder.class));
      }
      return result;
    };
  }

  private static void assertFormat(Field[] fields, List<io.tarantool.mapping.Field> format) {
    assertEquals(fields.length, format.size());
    for (int i = 0; i < format.size(); i++) {
      assertEquals(
          fields[i].getName(),
          CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, format.get(i).getName()));
    }
  }

  @Test
  public void testSpaceBySpaceNameAfterAddingNewSpace() throws Exception {
    TarantoolBoxClient customClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .build();
    assertEquals(EMPTY_LIST, customClient.space("person").select(EMPTY_LIST).join().get());
    client
        .eval("space = box.schema.space.create('space_from_java_code'); space:create_index('pri')")
        .join();
    List<?> result =
        assertDoesNotThrow(() -> customClient.space("space_from_java_code").select().join().get());
    assertEquals(EMPTY_LIST, result);
  }

  @Test
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testExceptionIfSchemaIsNotExist() throws Exception {
    TarantoolBoxClient customClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .build();
    String nonExistingSpaceName = "non-existing-space-name";
    NoSchemaException ex =
        assertThrows(NoSchemaException.class, () -> customClient.space(nonExistingSpaceName));
    assertEquals("No schema for space: " + nonExistingSpaceName, ex.getMessage());
  }

  @Test
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testExceptionIfIndexIsNotExist() throws Exception {
    TarantoolBoxClient customClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .build();
    String spaceName = "person";
    TarantoolBoxSpace space = customClient.space(spaceName);
    String nonExistingIndexName = "non-existing-index-name";
    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () ->
                space
                    .select(
                        EMPTY_LIST, SelectOptions.builder().withIndex(nonExistingIndexName).build())
                    .join());

    assertInstanceOf(NoSchemaException.class, ex.getCause());
    NoSchemaException cause = (NoSchemaException) ex.getCause();
    assertEquals(
        "No index " + nonExistingIndexName + " for space: " + spaceName, cause.getMessage());
  }

  public static Stream<Arguments> useSpaceName() {
    return Stream.of(Arguments.of(true), Arguments.of(false));
  }

  public static Stream<Arguments> useSpaceNameAndIndexName() {
    return Stream.of(
        Arguments.of(true, true),
        Arguments.of(true, false),
        Arguments.of(false, true),
        Arguments.of(false, false));
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  public void testSelect(Boolean useSpaceName, Boolean useIndexName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);
    final SelectOptions optionsWithIndexId = SelectOptions.builder().withIndex(0).build();
    final SelectOptions optionsWithIndexName = SelectOptions.builder().withIndex("pk").build();
    final SelectOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;

    doSelectRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testSelectWithoutFetcherAndWithSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {

    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final SelectOptions optionsWithIndexId = SelectOptions.builder().withIndex(0).build();
    final SelectOptions optionsWithIndexName = SelectOptions.builder().withIndex("pk").build();
    final SelectOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;

    doSelectRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testSelectWithoutFetcherAndWithoutSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {

    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final SelectOptions optionsWithIndexId = SelectOptions.builder().withIndex(0).build();
    final SelectOptions optionsWithIndexName = SelectOptions.builder().withIndex("pk").build();
    final SelectOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;

    if (!useIndexName && !useSpaceName) {
      doSelectRequestShouldBeSuccessful(testSpace, options);
    } else {
      Throwable cause =
          assertThrows(
                  CompletionException.class,
                  () -> doSelectRequestShouldBeSuccessful(testSpace, options))
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    }
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  public void testSelectPagination(Boolean useSpaceName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);

    Person firstPerson = new Person(1, true, "Dima");
    Person secondPerson = new Person(2, true, "Kolya");
    assertEquals(
        Collections.singletonList(firstPerson.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, 'Dima'})"));

    assertEquals(
        Collections.singletonList(secondPerson.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({2, true, 'Kolya'})"));

    SelectResponse<List<Tuple<List<?>>>> firstBatch =
        testSpace
            .select(EMPTY_LIST, SelectOptions.builder().fetchPosition().withLimit(1).build())
            .join();
    assertEquals(firstPerson.asList(), firstBatch.get().get(0).get());
    SelectResponse<List<Tuple<List<?>>>> secondBatch =
        testSpace
            .select(
                EMPTY_LIST,
                SelectOptions.builder()
                    .fetchPosition()
                    .withLimit(1)
                    .after(firstBatch.get().get(0).get())
                    .build())
            .join();
    assertEquals(1, firstBatch.get().size());
    assertEquals(secondPerson.asList(), secondBatch.get().get(0).get());
    secondBatch =
        testSpace
            .select(
                EMPTY_LIST,
                SelectOptions.builder()
                    .fetchPosition()
                    .withLimit(1)
                    .after(firstBatch.getPosition())
                    .build())
            .join();
    assertEquals(1, secondBatch.get().size());
    assertEquals(secondPerson.asList(), secondBatch.get().get(0).get());
  }

  public static Stream<Arguments> useIndexName() {
    return Stream.of(Arguments.of(true), Arguments.of(false));
  }

  @ParameterizedTest
  @MethodSource("useIndexName")
  public void testSelectIndex() throws Exception {
    List<? extends Serializable> firstTuple = Arrays.asList(1, true, "2");
    assertEquals(
        Collections.singletonList(firstTuple),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, '2'})"));
    List<? extends Serializable> secondTuple = Arrays.asList(2, true, "1");
    assertEquals(
        Collections.singletonList(secondTuple),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({2, true, '1'})"));

    client.eval("box.space.person:create_index('name_index', { parts = { 'name' } })").join();

    TarantoolBoxSpace testSpace = client.space("person");
    assertEquals(
        Arrays.asList(firstTuple, secondTuple),
        unpack(
            testSpace.select(EMPTY_LIST, SelectOptions.builder().withIndex("pk").build()).join()));

    assertEquals(
        Arrays.asList(secondTuple, firstTuple),
        unpack(
            testSpace
                .select(EMPTY_LIST, SelectOptions.builder().withIndex("name_index").build())
                .join()));

    client.eval("box.space.person.index['name_index']:drop()").join();
  }

  public void doSelectRequestShouldBeSuccessful(TarantoolBoxSpace testSpace, SelectOptions options)
      throws Exception {
    Person firstPerson = new Person(1, true, "Dima");
    Person secondPerson = new Person(2, true, "Kolya");
    assertEquals(
        Collections.singletonList(firstPerson.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, 'Dima'})"));
    assertEquals(
        Collections.singletonList(secondPerson.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({2, true, 'Kolya'})"));

    SelectResponse<List<Tuple<List<?>>>> selectResult =
        testSpace.select(EMPTY_LIST, options).join();
    assertNull(selectResult.getPosition());
    if (hasTupleExtension(testSpace)) {
      assertFalse(selectResult.getFormats().isEmpty());
      assertNotNull(selectResult.get().get(0).getFormatId());
      assertFalse(selectResult.get().get(0).getFormat().isEmpty());
    }

    assertEquals(Arrays.asList(firstPerson.asList(), secondPerson.asList()), unpack(selectResult));

    SelectResponse<List<Tuple<Person>>> selectWithTargetClass =
        testSpace.select(EMPTY_LIST, options, Person.class).join();
    assertNull(selectWithTargetClass.getPosition());
    if (hasTupleExtension(testSpace)) {
      assertFalse(selectWithTargetClass.getFormats().isEmpty());
      assertNotNull(selectWithTargetClass.get().get(0).getFormatId());
      assertFalse(selectWithTargetClass.get().get(0).getFormat().isEmpty());
    }
    assertEquals(Arrays.asList(firstPerson, secondPerson), unpackT(selectWithTargetClass));

    SelectResponse<Set<Tuple<Person>>> selectWithTargetTypeRef =
        testSpace.select(EMPTY_LIST, options, new TypeReference<Set<Tuple<Person>>>() {}).join();
    assertNull(selectWithTargetTypeRef.getPosition());
    if (hasTupleExtension(testSpace)) {
      assertFalse(selectWithTargetTypeRef.getFormats().isEmpty());
      assertNotNull(selectWithTargetClass.get().get(0).getFormatId());
      // for typeRef API we don't have format besides Tuples, only in TarantoolResponse
      assertTrue(selectWithTargetTypeRef.get().stream().findFirst().get().getFormat().isEmpty());
    }
    assertEquals(
        new HashSet<>(Arrays.asList(firstPerson, secondPerson)),
        new HashSet<>(
            selectWithTargetTypeRef.get().stream().map(Tuple::get).collect(Collectors.toSet())));
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  public void testInsert(Boolean useSpaceName) {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);
    doInsertRequestShouldBeSuccessful(testSpace);
  }

  @Test
  public void testNilError() {
    Exception ex =
        assertThrows(
            Exception.class,
            () ->
                client
                    .eval(
                        "return nil, 'error_from_tarantool'",
                        new TypeReference<NilErrorResponse<?, ?>>() {})
                    .join()
                    .get()
                    .get());
    assertEquals("error_from_tarantool", ex.getMessage());
  }

  @Test
  public void testNilComplexError() {
    Exception ex =
        assertThrows(
            Exception.class,
            () ->
                client
                    .eval(
                        "return nil, {res = 'error_from_tarantool', line = 3}",
                        new TypeReference<NilErrorResponse<?, ?>>() {})
                    .join()
                    .get()
                    .get());
    assertEquals(
        new HashMap<String, Object>() {
          {
            put("res", "error_from_tarantool");
            put("line", 3);
          }
        }.toString(),
        ex.getMessage());
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testInsertWithoutFetcherWithSpaceAndIndexFeature(Boolean useSpaceName) {
    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    doInsertRequestShouldBeSuccessful(testSpace);
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testInsertWithoutFetcherAndWithoutSpaceAndIndexFeature(Boolean useSpaceName) {
    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    if (!useSpaceName) {
      doInsertRequestShouldBeSuccessful(testSpace);
    } else {
      final Throwable cause =
          assertThrows(
                  CompletionException.class,
                  () -> testSpace.insert(new Person(999, true, "999")).join())
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    }
  }

  public boolean hasTupleExtension(TarantoolSpace space) {
    IProtoClient iprotoClient = space.getPool().get("default", 0).join();
    return iprotoClient.hasTupleExtension();
  }

  public void doInsertRequestShouldBeSuccessful(TarantoolBoxSpace testSpace) {
    List<?> firstPersonAsList = Arrays.asList(1, true, "1");
    List<?> secondPersonAsList = Arrays.asList(2, true, "1");
    List<?> thirdPersonAsList = Arrays.asList(3, true, "1");
    List<?> fourthPersonAsList = Arrays.asList(4, true, "1");

    Person fifthPersonAsClass = new Person(5, true, "1");
    Person sixthPersonAsClass = new Person(6, true, "1");
    Person seventhPersonAsClass = new Person(7, true, "1");
    Person eighthPersonAsClass = new Person(8, true, "1");

    // input as list
    Tuple<List<?>> response = testSpace.insert(firstPersonAsList).join();
    List<?> defaultResultWithInputAsList = response.get();

    Tuple<Person> entityClass = testSpace.insert(secondPersonAsList, Person.class).join();
    Person resultAsClassWithInputAsList = entityClass.get();
    TarantoolResponse<Tuple<List<?>>> typRefResp =
        testSpace.insert(thirdPersonAsList, typeReferenceAsList).join();

    List<?> resultWithTypeRefAsListAndInputAsList = typRefResp.get().get();
    Person resultWithTypeRefAsClassAndInputAsList =
        testSpace.insert(fourthPersonAsList, typeReferenceAsPersonClass).join().get().get();

    if (hasTupleExtension(testSpace)) {
      assertNotNull(response.getFormatId());
      assertFalse(response.getFormat().isEmpty());

      // entity class
      assertNotNull(entityClass.getFormatId());
      assertFalse(entityClass.getFormat().isEmpty());

      // typeRef
      assertFalse(typRefResp.getFormats().isEmpty());
      Tuple<List<?>> tuple = typRefResp.get();
      assertNotNull(tuple.getFormatId());
      assertTrue(tuple.getFormat().isEmpty());
    }
    assertEquals(firstPersonAsList, defaultResultWithInputAsList);
    assertEquals(secondPersonAsList, resultAsClassWithInputAsList.asList());
    assertEquals(thirdPersonAsList, resultWithTypeRefAsListAndInputAsList);
    assertEquals(fourthPersonAsList, resultWithTypeRefAsClassAndInputAsList.asList());

    // input as Person class
    List<?> defaultResultWithInputAsClass = testSpace.insert(fifthPersonAsClass).join().get();
    Person resultAsClassWithInputAsClass =
        testSpace.insert(sixthPersonAsClass, Person.class).join().get();
    List<?> resultWithTypeRefAsListAndInputAsClass =
        testSpace.insert(seventhPersonAsClass, typeReferenceAsList).join().get().get();
    Person resultWithTypeRefAsClassAndInputAsClass =
        testSpace.insert(eighthPersonAsClass, typeReferenceAsPersonClass).join().get().get();

    assertEquals(fifthPersonAsClass.asList(), defaultResultWithInputAsClass);
    assertEquals(sixthPersonAsClass, resultAsClassWithInputAsClass);
    assertEquals(seventhPersonAsClass.asList(), resultWithTypeRefAsListAndInputAsClass);
    assertEquals(eighthPersonAsClass, resultWithTypeRefAsClassAndInputAsClass);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  public void testDelete(Boolean useSpaceName, Boolean useIndexName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);
    final DeleteOptions optionsWithIndexId = DeleteOptions.builder().withIndex(0).build();
    final DeleteOptions optionsWithIndexName = DeleteOptions.builder().withIndex("pk").build();
    final DeleteOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    doDeleteRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testDeleteWithoutFetcherAndWithSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {

    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final DeleteOptions optionsWithIndexId = DeleteOptions.builder().withIndex(0).build();
    final DeleteOptions optionsWithIndexName = DeleteOptions.builder().withIndex("pk").build();
    final DeleteOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    doDeleteRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testDeleteWithoutFetcherAndWithoutSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {

    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final DeleteOptions optionsWithIndexId = DeleteOptions.builder().withIndex(0).build();
    final DeleteOptions optionsWithIndexName = DeleteOptions.builder().withIndex("pk").build();
    final DeleteOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    if (!useIndexName && !useSpaceName) {
      doDeleteRequestShouldBeSuccessful(testSpace, options);
    } else {
      Throwable cause =
          assertThrows(
                  CompletionException.class,
                  () -> doDeleteRequestShouldBeSuccessful(testSpace, options))
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    }
  }

  public void doDeleteRequestShouldBeSuccessful(TarantoolBoxSpace testSpace, DeleteOptions options)
      throws Exception {
    Person person = new Person(1, true, "Dima");
    List<?> key = Collections.singletonList(1);

    // simple
    assertEquals(
        Collections.singletonList(person.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, 'Dima'})"));

    Tuple<List<?>> responseAsListWithKeyOnly = testSpace.delete(key, options).join();
    List<?> resultAsListWithKeyOnly = responseAsListWithKeyOnly.get();

    assertEquals(person.asList(), resultAsListWithKeyOnly);

    // with entity Class
    assertEquals(
        Collections.singletonList(person.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, 'Dima'})"));

    Tuple<Person> responseAsClassWithKeyAndClass =
        testSpace.delete(key, options, Person.class).join();
    Person resultAsClassWithKeyAndClass = responseAsClassWithKeyAndClass.get();

    assertEquals(person, resultAsClassWithKeyAndClass);

    // with typeReference tuple as list
    assertEquals(
        Collections.singletonList(person.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, 'Dima'})"));

    TarantoolResponse<Tuple<List<?>>> responseAsListWithTypeRefAsList =
        testSpace.delete(key, options, typeReferenceAsList).join();
    List<?> resultAsListWithTypeRefAsList = responseAsListWithTypeRefAsList.get().get();

    assertEquals(person.asList(), resultAsListWithTypeRefAsList);

    // with typeReference tuple as class
    assertEquals(
        Collections.singletonList(person.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, 'Dima'})"));

    TarantoolResponse<Tuple<Person>> responseAsClassWithTypeRefAsClass =
        testSpace.delete(key, options, typeReferenceAsPersonClass).join();
    Person resultAsClassWithTypeRefAsClass = responseAsClassWithTypeRefAsClass.get().get();

    assertEquals(person, resultAsClassWithTypeRefAsClass);

    if (hasTupleExtension(testSpace)) {

      assertNotNull(responseAsListWithKeyOnly.getFormatId());
      assertFalse(responseAsListWithKeyOnly.getFormat().isEmpty());

      // entity class
      assertNotNull(responseAsClassWithKeyAndClass.getFormatId());
      assertFalse(responseAsClassWithKeyAndClass.getFormat().isEmpty());

      // typeRef
      assertFalse(responseAsListWithTypeRefAsList.getFormats().isEmpty());
      Tuple<List<?>> tuple = responseAsListWithTypeRefAsList.get();
      assertNotNull(tuple.getFormatId());
      assertTrue(tuple.getFormat().isEmpty());

      assertFalse(responseAsClassWithTypeRefAsClass.getFormats().isEmpty());
      Tuple<Person> tupleClass = responseAsClassWithTypeRefAsClass.get();
      assertNotNull(tupleClass.getFormatId());
      assertTrue(tupleClass.getFormat().isEmpty());
    }
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  public void testReplace(Boolean useSpaceName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);

    assertEquals(
        Collections.singletonList(Arrays.asList(1, true, "0")),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, '0'})"));
    doReplaceRequestShouldBeSuccessful(testSpace);
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testReplaceWithoutFetcherAndWithSpaceAndIndexFeature(Boolean useSpaceName)
      throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);

    assertEquals(
        Collections.singletonList(Arrays.asList(1, true, "0")),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, '0'})"));
    doReplaceRequestShouldBeSuccessful(testSpace);
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testReplaceWithoutFetcherAndWithoutSpaceAndIndexFeature(Boolean useSpaceName)
      throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);

    assertEquals(
        Collections.singletonList(Arrays.asList(1, true, "0")),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert({1, true, '0'})"));

    if (!useSpaceName) {
      doReplaceRequestShouldBeSuccessful(testSpace);
    } else {
      final Throwable cause =
          assertThrows(
                  CompletionException.class, () -> doInsertRequestShouldBeSuccessful(testSpace))
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    }
  }

  public void doReplaceRequestShouldBeSuccessful(TarantoolBoxSpace testSpace) {
    List<?> firstPersonAsList = Arrays.asList(1, true, "1");
    List<?> secondPersonAsList = Arrays.asList(1, true, "2");
    List<?> thirdPersonAsList = Arrays.asList(1, true, "3");
    List<?> fourthPersonAsList = Arrays.asList(1, true, "4");

    Person fifthPersonAsClass = new Person(1, true, "5");
    Person sixthPersonAsClass = new Person(1, true, "6");
    Person seventhPersonAsClass = new Person(1, true, "7");
    Person eightPersonAsClass = new Person(1, true, "8");

    // input as list
    List<?> resultAsList = testSpace.replace(firstPersonAsList).join().get();
    Person resultAsClassWithClass =
        testSpace.replace(secondPersonAsList, typeReferenceAsPersonClass).join().get().get();
    List<?> resultAsListWithTypeRefAsList =
        testSpace.replace(thirdPersonAsList, typeReferenceAsList).join().get().get();
    Person resultAsClassWithTypeRefAsClass =
        testSpace.replace(fourthPersonAsList, typeReferenceAsPersonClass).join().get().get();

    assertEquals(firstPersonAsList, resultAsList);
    assertEquals(secondPersonAsList, resultAsClassWithClass.asList());
    assertEquals(thirdPersonAsList, resultAsListWithTypeRefAsList);
    assertEquals(fourthPersonAsList, resultAsClassWithTypeRefAsClass.asList());

    // input as class
    List<?> resultAsListWithInputAsClass = testSpace.replace(fifthPersonAsClass).join().get();
    Person resultAsClassWithInputAsClassAndClass =
        testSpace.replace(sixthPersonAsClass, Person.class).join().get();
    List<?> resultAsListWithInputAsClassAndTypeRefAsList =
        testSpace.replace(seventhPersonAsClass, typeReferenceAsList).join().get().get();
    Person resultAsClassWithInputAsClassAndTypeRefAsClass =
        testSpace.replace(eightPersonAsClass, typeReferenceAsPersonClass).join().get().get();

    assertEquals(fifthPersonAsClass.asList(), resultAsListWithInputAsClass);
    assertEquals(sixthPersonAsClass, resultAsClassWithInputAsClassAndClass);
    assertEquals(seventhPersonAsClass.asList(), resultAsListWithInputAsClassAndTypeRefAsList);
    assertEquals(eightPersonAsClass, resultAsClassWithInputAsClassAndTypeRefAsClass);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  public void testUpdate(Boolean useSpaceName, Boolean useIndexName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);
    final UpdateOptions optionsWithIndexId = UpdateOptions.builder().withIndex(0).build();
    final UpdateOptions optionsWithIndexName = UpdateOptions.builder().withIndex("pk").build();
    final UpdateOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    doUpdateRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testUpdateWithoutFetcherAndWithSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {

    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final UpdateOptions optionsWithIndexId = UpdateOptions.builder().withIndex(0).build();
    final UpdateOptions optionsWithIndexName = UpdateOptions.builder().withIndex("pk").build();
    final UpdateOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    doUpdateRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testUpdateWithoutFetcherAndWithoutSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {

    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final UpdateOptions optionsWithIndexId = UpdateOptions.builder().withIndex(0).build();
    final UpdateOptions optionsWithIndexName = UpdateOptions.builder().withIndex("pk").build();
    final UpdateOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    if (!useSpaceName && !useIndexName) {
      doUpsertRequestShouldBeSuccessful(testSpace, options);
    } else {
      final Throwable cause =
          assertThrows(
                  CompletionException.class,
                  () -> doUpdateRequestShouldBeSuccessful(testSpace, options))
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    }
  }

  public void doUpdateRequestShouldBeSuccessful(TarantoolBoxSpace testSpace, UpdateOptions options)
      throws Exception {
    Person person = new Person(1, true, "0");

    assertEquals(
        Collections.singletonList(person.asList()),
        TarantoolContainerClientHelper.executeCommandDecoded(
            tt, "return box.space.person:insert ({1, true, '0'})"));

    List<?> key = Collections.singletonList(person.getId());

    // simple with non-typed operations
    List<?> resultAsList =
        testSpace
            .update(key, Collections.singletonList(Arrays.asList("=", "name", "1")), options)
            .join()
            .get();
    person.setName("1");
    assertEquals(person.asList(), resultAsList);

    // simple with typed operations
    resultAsList =
        testSpace
            .update(key, Operations.create().set("name", "Joe").set("is_married", false), options)
            .join()
            .get();
    person.setName("Joe");
    person.setIsMarried(false);
    assertEquals(person.asList(), resultAsList);

    // simple with typed operations (string splice)
    resultAsList =
        testSpace
            .update(key, Operations.create().stringSplice("name", 4, 0, " Satriani"), options)
            .join()
            .get();
    person.setName("Joe Satriani");
    person.setIsMarried(false);
    assertEquals(person.asList(), resultAsList);

    // with Class class and non-typed operations
    Person resultAsClassWithClass =
        testSpace
            .update(
                key,
                Collections.singletonList(Arrays.asList("=", "name", "2")),
                options,
                Person.class)
            .join()
            .get();
    person.setName("2");
    assertEquals(person, resultAsClassWithClass);

    // with Class class typed operations
    resultAsClassWithClass =
        testSpace
            .update(key, Operations.create().set("name", "Joe"), options, Person.class)
            .join()
            .get();
    person.setName("Joe");
    assertEquals(person, resultAsClassWithClass);

    // with typeReference with tuple as list and non typed operations
    List<?> resultAsListWithTypeRefAsList =
        testSpace
            .update(
                key,
                Collections.singletonList(Arrays.asList("=", "name", "3")),
                options,
                typeReferenceAsList)
            .join()
            .get()
            .get();
    person.setName("3");
    assertEquals(person.asList(), resultAsListWithTypeRefAsList);

    // with typeReference with tuple as list and typed operations
    resultAsListWithTypeRefAsList =
        testSpace
            .update(key, Operations.create().set("name", "Joe"), options, typeReferenceAsList)
            .join()
            .get()
            .get();
    person.setName("Joe");
    assertEquals(person.asList(), resultAsListWithTypeRefAsList);

    // with typeReference with tuple as class and non-typed operations
    Person resultAsClassWithTypeRefAsClass =
        testSpace
            .update(
                key,
                Collections.singletonList(Arrays.asList("=", "name", "4")),
                options,
                typeReferenceAsPersonClass)
            .join()
            .get()
            .get();
    person.setName("4");
    assertEquals(person, resultAsClassWithTypeRefAsClass);

    // with typeReference with tuple as class and typed operations
    resultAsClassWithTypeRefAsClass =
        testSpace
            .update(
                key, Operations.create().set("name", "Joe"), options, typeReferenceAsPersonClass)
            .join()
            .get()
            .get();
    person.setName("Joe");
    assertEquals(person, resultAsClassWithTypeRefAsClass);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @SuppressWarnings("rawtypes")
  public void testUpsert(Boolean useSpaceName, Boolean useIndexName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);
    final UpdateOptions optionsWithIndexName = UpdateOptions.builder().withIndex("pk").build();
    final UpdateOptions optionsWithIndexId = UpdateOptions.builder().withIndex(0).build();
    UpdateOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;

    doUpsertRequestShouldBeSuccessful(testSpace, options);
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  @SuppressWarnings("rawtypes")
  public void testUpsertWithoutFetcherAndWithoutSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final UpdateOptions optionsWithIndexName = UpdateOptions.builder().withIndex("pk").build();
    final UpdateOptions optionsWithIndexId = UpdateOptions.builder().withIndex(0).build();
    final UpdateOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    if (!useSpaceName && !useIndexName) {
      doUpsertRequestShouldBeSuccessful(testSpace, options);
    } else {
      Throwable cause =
          assertThrows(
                  CompletionException.class,
                  () -> doUpsertRequestShouldBeSuccessful(testSpace, options))
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    }
  }

  @ParameterizedTest
  @MethodSource("useSpaceNameAndIndexName")
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  @SuppressWarnings("rawtypes")
  public void testUpsertWithoutFetcherAndWithSpaceAndIndexFeature(
      Boolean useSpaceName, Boolean useIndexName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName
            ? clientWithoutFetcher.space("person")
            : clientWithoutFetcher.space(spacePersonId);
    final UpdateOptions optionsWithIndexName = UpdateOptions.builder().withIndex("pk").build();
    final UpdateOptions optionsWithIndexId = UpdateOptions.builder().withIndex(0).build();
    final UpdateOptions options = useIndexName ? optionsWithIndexName : optionsWithIndexId;
    if (useIndexName) {
      Throwable cause =
          assertThrows(
                  CompletionException.class,
                  () -> doUpsertRequestShouldBeSuccessful(testSpace, options))
              .getCause();
      assertInstanceOf(IllegalArgumentException.class, cause);
      assertEquals(
          WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0, cause.getMessage());
    } else {
      doUpsertRequestShouldBeSuccessful(testSpace, options);
    }
  }

  public void doUpsertRequestShouldBeSuccessful(TarantoolBoxSpace testSpace, UpdateOptions options)
      throws Exception {
    Person person = new Person(1, true, "Dima");
    testSpace
        .upsert(person, Collections.singletonList(Arrays.asList("=", 2, "DimaK")), options)
        .join();
    assertEquals(
        Collections.singletonList(person.asList()),
        ((List)
                TarantoolContainerClientHelper.executeCommandDecoded(
                    tt, "return box.space.person:select()"))
            .get(0));

    person.setName("DimaK");
    testSpace
        .upsert(person, Collections.singletonList(Arrays.asList("=", 2, "DimaK")), options)
        .join();
    assertEquals(
        Collections.singletonList(person.asList()),
        ((List)
                TarantoolContainerClientHelper.executeCommandDecoded(
                    tt, "return box.space.person:select()"))
            .get(0));

    TarantoolContainerClientHelper.executeCommandDecoded(tt, "return box.space.person:truncate()");

    Person otherPerson = new Person(2, false, "Thomas Sawer");
    testSpace
        .upsert(otherPerson, Operations.create().stringSplice("name", 7, 1, " \"Tom\" "), options)
        .join();
    assertEquals(
        Collections.singletonList(otherPerson.asList()),
        ((List)
                TarantoolContainerClientHelper.executeCommandDecoded(
                    tt, "return box.space.person:select()"))
            .get(0));

    otherPerson.setName("Tom");
    testSpace.upsert(otherPerson, Operations.create().set("name", "Tom"), options).join();
    assertEquals(
        Collections.singletonList(otherPerson.asList()),
        ((List)
                TarantoolContainerClientHelper.executeCommandDecoded(
                    tt, "return box.space.person:select()"))
            .get(0));
  }

  @ParameterizedTest
  @MethodSource("useSpaceName")
  public void testSelectWithArgs(Boolean useSpaceName) throws Exception {
    TarantoolBoxSpace testSpace =
        useSpaceName ? client.space("person") : client.space(spacePersonId);

    TarantoolContainerClientHelper.executeCommandDecoded(
        tt, "return box.space.person:insert({1, true, 'Dima'})");
    TarantoolContainerClientHelper.executeCommandDecoded(
        tt, "return box.space.person:insert({2, true, 'Roma'})");
    TarantoolContainerClientHelper.executeCommandDecoded(
        tt, "return box.space.person:insert({3, false, 'Kolya'})");
    Person dima = new Person(1, true, "Dima");
    Person roma = new Person(2, true, "Roma");
    Person kolya = new Person(3, false, "Kolya");
    List<List<?>> resultAsArray =
        Arrays.asList(
            Arrays.asList(dima.getId(), dima.getIsMarried(), dima.getName()),
            Arrays.asList(roma.getId(), roma.getIsMarried(), roma.getName()),
            Arrays.asList(kolya.getId(), kolya.getIsMarried(), kolya.getName()));
    List<?> resultAsEntity = Arrays.asList(dima, roma, kolya);
    Collections.reverse(resultAsEntity);

    SelectResponse<List<Tuple<List<?>>>> spaceResult = testSpace.select(EMPTY_LIST).join();
    assertNull(spaceResult.getPosition());
    assertEquals(resultAsArray, unpack(spaceResult));

    spaceResult =
        testSpace.select(EMPTY_LIST, SelectOptions.builder().fetchPosition().build()).join();
    assertArrayEquals(new byte[] {-93, 107, 81, 77}, spaceResult.getPosition());
    assertEquals(resultAsArray, unpack(spaceResult));

    SelectResponse<List<Tuple<Person>>> typedSelectResult =
        testSpace
            .select(
                EMPTY_LIST,
                SelectOptions.builder().withIterator(BoxIterator.REQ).withOffset(1).build(),
                Person.class)
            .join();
    assertEquals(resultAsEntity.subList(1, resultAsEntity.size()), unpackT(typedSelectResult));
    Collections.reverse(resultAsArray);
  }

  @Test
  void testClientType() {
    assertEquals(ClientType.BOX, client.getType());
  }

  @Test
  void testGetServerVersion() throws Exception {
    TarantoolBoxClient client =
        TarantoolFactory.box()
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withUser("api_user")
            .withPassword("secret")
            .build();

    TarantoolVersion version = client.getServerVersion().join();
    assertEquals(tarantoolMajorVersion, version.getMajor());
  }

  /**
   * Demonstrates distributed tracing using OpenTelemetry API with Handlers.
   *
   * <p>This test shows how to integrate Tarantool client with OpenTelemetry to track request
   * lifecycle:
   *
   * <ul>
   *   <li>onBeforeSend - creates a span and stores it by syncId
   *   <li>onSuccess - retrieves span, sets attributes and ends it
   *   <li>onTimeout - retrieves span, records exception and ends it
   *   <li>onIgnoredResponse - adds event for late response arrival
   * </ul>
   *
   * <p>Jaeger is started via TestContainer. To view traces, open the URL printed in console
   * (http://localhost:16686) after test starts running.
   */
  @Test
  @Timeout(5000)
  @Disabled
  public void testOpenTelemetryTracingWithJaeger() throws Exception {
    // Start Jaeger via TestContainer
    org.testcontainers.containers.GenericContainer<?> jaeger =
        new org.testcontainers.containers.GenericContainer<>("jaegertracing/all-in-one:1.50")
            .withExposedPorts(16686, 4317)
            .withEnv("COLLECTOR_OTLP_ENABLED", "true");
    jaeger.start();

    String jaegerUrl = "http://" + jaeger.getHost() + ":" + jaeger.getMappedPort(16686);
    String otlpEndpoint = "http://" + jaeger.getHost() + ":" + jaeger.getMappedPort(4317);

    System.out.println("========================================");
    System.out.println("Jaeger UI available at: " + jaegerUrl);
    System.out.println("OTLP endpoint: " + otlpEndpoint);
    System.out.println("========================================");

    // Set up OTLP exporter to send traces to Jaeger
    io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter spanExporter =
        io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpEndpoint)
            .build();

    io.opentelemetry.sdk.resources.Resource resource =
        io.opentelemetry.sdk.resources.Resource.builder()
            .put(
                io.opentelemetry.api.common.AttributeKey.stringKey("service.name"),
                "tarantool-java-sdk")
            .put(io.opentelemetry.api.common.AttributeKey.stringKey("service.version"), "2.0.0")
            .build();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    Tracer tracer = openTelemetry.getTracer("tarantool-test");

    // Maps to store active spans by syncId
    java.util.Map<Long, Span> requestSpans = new java.util.concurrent.ConcurrentHashMap<>();
    java.util.Map<Long, Span> timeoutSpans = new java.util.concurrent.ConcurrentHashMap<>();

    TarantoolBoxClient testClient =
        TarantoolFactory.box()
            .withUser(API_USER)
            .withPassword(CREDS.get(API_USER))
            .withHost(tt.getHost())
            .withPort(tt.getFirstMappedPort())
            .withHandlers(
                Handlers.builder()
                    .onBeforeSend(
                        request -> {
                          // Extract function name and arguments from request body
                          String functionName = extractFunctionName(request);
                          String args = extractArguments(request);

                          String requestTypeName = getRequestTypeName(request.getRequestType());

                          // 1. Main request span - lives until final result (success or late
                          // response)
                          Span requestSpan =
                              tracer
                                  .spanBuilder(
                                      "tarantool." + requestTypeName + ":" + request.getSyncId())
                                  .setAttribute("request.type", request.getRequestType())
                                  .setAttribute("request.type.name", requestTypeName)
                                  .setAttribute("sync.id", request.getSyncId())
                                  .setAttribute("tarantool.function", functionName)
                                  .setAttribute("tarantool.arguments", args)
                                  .startSpan();
                          requestSpans.put(request.getSyncId(), requestSpan);

                          // 2. Timeout span - created now, ended on timeout, removed on success
                          Span timeoutSpan =
                              tracer
                                  .spanBuilder(
                                      "tarantool."
                                          + requestTypeName
                                          + ".timeout:"
                                          + request.getSyncId())
                                  .setParent(
                                      io.opentelemetry.context.Context.current().with(requestSpan))
                                  .setAttribute("sync.id", request.getSyncId())
                                  .startSpan();
                          timeoutSpans.put(request.getSyncId(), timeoutSpan);
                        })
                    .onSuccess(
                        response -> {
                          // Remove timeout span (success happened, no timeout)
                          Span timeoutSpan = timeoutSpans.remove(response.getSyncId());
                          if (timeoutSpan != null) {
                            // Don't end it, just discard - timeout didn't happen
                          }

                          // End main request span
                          Span requestSpan = requestSpans.remove(response.getSyncId());
                          if (requestSpan != null) {
                            requestSpan.setAttribute("response.sync_id", response.getSyncId());
                            requestSpan.setStatus(StatusCode.OK);

                            // Parse and log response data
                            String responseData = extractResponseData(response);
                            requestSpan.setAttribute("response.data", responseData);

                            requestSpan.end();
                          }
                        })
                    .onTimeout(
                        request -> {
                          // End timeout span
                          Span timeoutSpan = timeoutSpans.remove(request.getSyncId());
                          if (timeoutSpan != null) {
                            timeoutSpan.addEvent("Request timed out");
                            timeoutSpan.setStatus(StatusCode.ERROR, "Request timed out");
                            timeoutSpan.setAttribute("error.type", "timeout");
                            timeoutSpan.recordException(new TimeoutException("Request timed out"));
                            timeoutSpan.end();
                          }
                        })
                    .onIgnoredResponse(
                        response -> {
                          // End main request span (late response arrived)
                          Span requestSpan = requestSpans.remove(response.getSyncId());
                          if (requestSpan != null) {
                            requestSpan.addEvent("Late response arrived");
                            requestSpan.setAttribute("response.sync_id", response.getSyncId());
                            requestSpan.setAttribute("late.response", true);

                            // Parse and log response data
                            String responseData = extractResponseData(response);
                            requestSpan.setAttribute("response.data", responseData);

                            requestSpan.end();
                          }
                        })
                    .build())
            .build();

    // Test successful request
    List<?> result = testClient.call("echo", Arrays.asList(42, "test")).join().get();
    assertEquals(Arrays.asList(42, "test"), result);

    // Wait for async callbacks and span export
    Thread.sleep(1000);

    System.out.println("Sent successful request trace to Jaeger");

    // Test timeout scenario
    Options timeoutOptions = BaseOptions.builder().withTimeout(100L).build();

    Exception ex =
        assertThrows(
            CompletionException.class,
            () -> testClient.call("slow_echo", Arrays.asList(1, true), timeoutOptions).join());
    assertEquals(TimeoutException.class, ex.getCause().getClass());

    // Wait for timeout handler and span export
    Thread.sleep(1000);

    System.out.println("Sent timeout error trace to Jaeger");

    // Print Jaeger URL again so user can view traces after test completes
    System.out.println("========================================");
    System.out.println("Test completed. View traces at: " + jaegerUrl);
    System.out.println("========================================");

    // Keep Jaeger running for a moment to allow viewing traces
    Thread.sleep(3000000);
    jaeger.stop();
    openTelemetry.close();
    testClient.close();
  }

  /** Helper method to get human-readable request type name. */
  private String getRequestTypeName(int requestType) {
    switch (requestType) {
      case IPROTO_TYPE_SELECT:
        return "SELECT";
      case IPROTO_TYPE_INSERT:
        return "INSERT";
      case IPROTO_TYPE_REPLACE:
        return "REPLACE";
      case IPROTO_TYPE_UPDATE:
        return "UPDATE";
      case IPROTO_TYPE_DELETE:
        return "DELETE";
      case IPROTO_TYPE_AUTH:
        return "AUTH";
      case IPROTO_TYPE_EVAL:
        return "EVAL";
      case IPROTO_TYPE_UPSERT:
        return "UPSERT";
      case IPROTO_TYPE_CALL:
        return "CALL";
      case IPROTO_TYPE_PING:
        return "PING";
      default:
        return "UNKNOWN(" + requestType + ")";
    }
  }

  /** Helper method to extract function name from CALL request. */
  private String extractFunctionName(IProtoRequest request) {
    try {
      byte[] packetBytes = request.getPacket(org.msgpack.core.MessagePack.newDefaultBufferPacker());
      org.msgpack.core.MessageUnpacker unpacker =
          org.msgpack.core.MessagePack.newDefaultUnpacker(packetBytes);
      unpacker.unpackInt(); // Skip size prefix
      unpacker.unpackValue(); // Skip header
      org.msgpack.value.Value bodyValue = unpacker.unpackValue();

      if (bodyValue.isMapValue()) {
        org.msgpack.value.Value functionNameValue =
            bodyValue.asMapValue().map().get(MP_IPROTO_FUNCTION_NAME);
        if (functionNameValue != null && functionNameValue.isStringValue()) {
          return functionNameValue.asStringValue().asString();
        }
      }
    } catch (Exception e) {
      // Ignore parsing errors
    }
    return "unknown";
  }

  /** Helper method to extract arguments from CALL request. */
  private String extractArguments(IProtoRequest request) {
    try {
      byte[] packetBytes = request.getPacket(org.msgpack.core.MessagePack.newDefaultBufferPacker());
      org.msgpack.core.MessageUnpacker unpacker =
          org.msgpack.core.MessagePack.newDefaultUnpacker(packetBytes);
      unpacker.unpackInt(); // Skip size prefix
      unpacker.unpackValue(); // Skip header
      org.msgpack.value.Value bodyValue = unpacker.unpackValue();

      if (bodyValue.isMapValue()) {
        org.msgpack.value.Value tupleValue = bodyValue.asMapValue().map().get(MP_IPROTO_TUPLE);
        if (tupleValue != null) {
          return tupleValue.toString();
        }
      }
    } catch (Exception e) {
      // Ignore parsing errors
    }
    return "[]";
  }

  /** Helper method to extract response data from IProtoResponse. */
  private String extractResponseData(IProtoResponse response) {
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("{");

      // Extract data from response body using Jackson mapping
      java.util.Map<Integer, io.tarantool.core.protocol.ByteBodyValueWrapper> byteBodyValues =
          response.getByteBodyValues();

      boolean first = true;
      for (java.util.Map.Entry<Integer, io.tarantool.core.protocol.ByteBodyValueWrapper> entry :
          byteBodyValues.entrySet()) {
        if (!first) {
          sb.append(", ");
        }
        first = false;

        String keyName = getResponseFieldName(entry.getKey());
        Object value =
            io.tarantool.mapping.BaseTarantoolJacksonMapping.readValue(
                entry.getValue(), Object.class);
        sb.append(keyName).append("=").append(value);
      }

      sb.append("}");
      return sb.toString();
    } catch (Exception e) {
      return "{error=" + e.getMessage() + "}";
    }
  }

  /** Helper method to get human-readable response field name. */
  private String getResponseFieldName(int fieldCode) {
    switch (fieldCode) {
      case IPROTO_DATA:
        return "data";
      case IPROTO_ERROR:
        return "error";
      default:
        return "field_" + fieldCode;
    }
  }
}
