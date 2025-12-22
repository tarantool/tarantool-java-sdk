/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.Helper.isCartridgeAvailable;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.value.ValueFactory;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolCartridgeContainer;
import org.testcontainers.containers.TarantoolContainerOperations;
import org.testcontainers.containers.VshardClusterContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.tarantool.client.crud.ConditionOperator.EQ;
import io.tarantool.client.BaseOptions;
import io.tarantool.client.ClientType;
import io.tarantool.client.HelpersUtils;
import io.tarantool.client.Options;
import io.tarantool.client.crud.Condition;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.UpsertBatch;
import io.tarantool.client.crud.options.CountOptions;
import io.tarantool.client.crud.options.DeleteOptions;
import io.tarantool.client.crud.options.GetOptions;
import io.tarantool.client.crud.options.InsertManyOptions;
import io.tarantool.client.crud.options.InsertOptions;
import io.tarantool.client.crud.options.LenOptions;
import io.tarantool.client.crud.options.MinMaxOptions;
import io.tarantool.client.crud.options.Mode;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.client.crud.options.TruncateOptions;
import io.tarantool.client.crud.options.UpdateOptions;
import io.tarantool.client.crud.options.UpsertManyOptions;
import io.tarantool.client.factory.TarantoolFactory;
import io.tarantool.client.operation.Operations;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.mapping.crud.CrudBatchResponse;
import io.tarantool.mapping.crud.CrudError;
import io.tarantool.mapping.crud.CrudException;
import io.tarantool.pool.HeartbeatOpts;
import io.tarantool.pool.IProtoClientPool;
import io.tarantool.pool.InstanceConnectionGroup;

@Timeout(value = 10)
@Testcontainers
public class TarantoolCrudClientTest extends BaseTest {

  public static final String SECRET_CLUSTER_COOKIE = "secret-cluster-cookie";
  public static final String ADMIN = "admin";
  public static final int PERSON_COUNT = 1000;
  public static final Map<String, Object> OPTIONS = new HashMap<String, Object>() {{
    put("timeout", 2_000L);
  }};
  public static final Person STUB_PERSON = new Person(0, true, String.valueOf(0));
  private static TarantoolCartridgeContainer cartridgeContainer;
  private static VshardClusterContainer vshardClusterContainer;
  private static TarantoolContainerOperations clusterContainer;
  public static final String ROUTER_1 = "ROUTER_1";
  public static final String ROUTER_2 = "ROUTER_2";
  private static TarantoolCrudClient client;
  private static TarantoolCrudClient clientWithCrudHeartbeats;
  private static TarantoolCrudClient clientWithoutHeartbeats;
  private static final int PING_INTERVAL = 500;
  private static final int WINDOW_SIZE = 4;
  private static final int INVALID_PINGS = 2;
  private static final int DEATH_THRESHOLD = 4;

  private static final Person personInstance = new Person(1, true, "Roman");
  private static List<List<Object>> triplets;
  private static final BaseOptions baseOptions = BaseOptions.builder().build();
  private static final TypeReference<List<Tuple<Person>>> listPersonTypeRef = new TypeReference<List<Tuple<Person>>>() {};
  private static final TypeReference<Person> personTypeRef = new TypeReference<Person>() {};
  private static final TypeReference<List<Tuple<List<?>>>> listListTypeRef =
      new TypeReference<List<Tuple<List<?>>>>() {};
  private static final TypeReference<List<?>> listTypeRef = new TypeReference<List<?>>() {};
  private static TarantoolCrudClient clientWithoutTupleExtensionEnabled;

  private static final String dockerRegistry = System.getenv().getOrDefault("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX", "");

  @BeforeAll
  public static void setUp() throws Exception {
    if (!isCartridgeAvailable()) {
      vshardClusterContainer = new VshardClusterContainer(
          "vshard_cluster/Dockerfile",
          dockerRegistry + "vshard-cluster-java",
          "vshard_cluster/instances.yaml",
          "vshard_cluster/config.yaml",
          "tarantool/tarantool"
      );

      if (!vshardClusterContainer.isRunning()) {
        vshardClusterContainer.withPrivilegedMode(true);
        vshardClusterContainer.start();
      }
      clusterContainer = vshardClusterContainer;
    } else {
      cartridgeContainer = new TarantoolCartridgeContainer(
          "Dockerfile",
          dockerRegistry + "cartridge",
          "cartridge/instances.yml",
          "cartridge/replicasets.yml",
          org.testcontainers.containers.Arguments.get("tarantool/tarantool"))
          .withStartupTimeout(Duration.ofMinutes(5))
          .withLogConsumer(new Slf4jLogConsumer(
              LoggerFactory.getLogger(TarantoolCrudClientTest.class)));
      if (!cartridgeContainer.isRunning()) {
        cartridgeContainer.start();
      }
      clusterContainer = cartridgeContainer;
    }
    List<InstanceConnectionGroup> routers = Arrays.asList(
        InstanceConnectionGroup.builder()
            .withPort(clusterContainer.getMappedPort(3301))
            .withTag(ROUTER_1)
            .withUser(ADMIN)
            .withPassword(SECRET_CLUSTER_COOKIE)
            .build(),
        InstanceConnectionGroup.builder()
            .withPort(clusterContainer.getMappedPort(3302))
            .withTag(ROUTER_2)
            .withUser(ADMIN)
            .withPassword(SECRET_CLUSTER_COOKIE)
            .build());
    client = TarantoolFactory.crud()
        .withHost(clusterContainer.getHost())
        .withPort(clusterContainer.getPort())
        .withUser(ADMIN)
        .withPassword(SECRET_CLUSTER_COOKIE)
        .enableTupleExtension()
        .withIgnoredPacketsHandler((tag, index, packet) -> {
          synchronized (triplets) {
            triplets.add(Arrays.asList(tag, index, packet));
          }
        })
        .build();
    clientWithoutTupleExtensionEnabled = TarantoolFactory.crud()
        .withHost(clusterContainer.getHost())
        .withPort(clusterContainer.getPort())
        .withUser(ADMIN)
        .withPassword(SECRET_CLUSTER_COOKIE)
        .build();

    clientWithoutHeartbeats = TarantoolFactory.crud()
        .withGroups(routers)
        .build();

    clientWithCrudHeartbeats = TarantoolFactory.crud()
        .withGroups(routers)
        .withHeartbeat(
            HeartbeatOpts
                .getDefault()
                .withPingInterval(PING_INTERVAL)
                .withInvalidationThreshold(INVALID_PINGS)
                .withWindowSize(WINDOW_SIZE)
                .withDeathThreshold(DEATH_THRESHOLD)
                .withCrudHealthCheck())
        .build();
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    clusterContainer.executeCommand("return crud.truncate('person')");
    clusterContainer.executeCommand("return crud.truncate('nested_person')");
    clusterContainer.executeCommand("return crud_aux.init_module()");
    triplets = new ArrayList<>();
  }

  @Test
  public void testCallTimeoutWithIgnoredPacketsHandler() throws Exception {
    clusterContainer.executeCommand("return crud_aux.slow_api()");
    TarantoolCrudSpace person = client.space("person");
    Options options = BaseOptions.builder()
        .withTimeout(1_000L)
        .build();
    Exception ex = assertThrows(
        CompletionException.class,
        () -> person.select(options, new TypeReference<Person>() {}, Condition.create("==", "pk", 1)).join()
    );
    Throwable cause = ex.getCause();
    assertEquals(TimeoutException.class, cause.getClass());
    Thread.sleep(600);
    assertEquals(1, triplets.size());

    Set<String> tags = new HashSet<>();
    Set<Integer> indexes = new HashSet<>();
    for (List<Object> item : triplets) {
      tags.add((String) item.get(0));
      indexes.add((int) item.get(1));
      assertInstanceOf(IProtoResponse.class, item.get(2));
    }
    assertEquals(new HashSet<>(Collections.singletonList("default")), tags);
    assertEquals(Collections.singleton(0), indexes);
  }

  @Test
  public void testCrudIsNotDeclared() {
    TarantoolCrudSpace space = clientWithoutHeartbeats.space("person");
    TarantoolCrudSpace spaceWithHeartbeat = clientWithCrudHeartbeats.space("person");
    assertDoesNotThrow(() -> space.select(Collections.emptyList()).join());
    assertDoesNotThrow(() -> spaceWithHeartbeat.select(Collections.emptyList()).join());

    IProtoClientPool crudHeartbeatPool = clientWithCrudHeartbeats.getPool();
    IProtoClientPool pool = clientWithoutHeartbeats.getPool();
    assertEquals(2, crudHeartbeatPool.availableConnections());
    assertEquals(2, pool.availableConnections());

    IProtoClient firstRouter = pool.get(ROUTER_1, 0).join();
    firstRouter.eval(
        "rawset(_G, 'tmp_crud', crud); rawset(_G, 'crud', nil)", ValueFactory.emptyArray()).join();

    HelpersUtils.retry(100, 100, () -> {
      CompletionException ex = assertThrows(CompletionException.class, () -> {
        space.select(Collections.emptyList()).join();
        space.select(Collections.emptyList()).join();
      });
      Throwable cause = ex.getCause();
      assertInstanceOf(BoxError.class, cause);
      assertTrue(cause.getMessage().contains("Procedure 'crud.select' is not defined"));
      assertDoesNotThrow(() -> {
        spaceWithHeartbeat.select(Collections.emptyList()).join();
        spaceWithHeartbeat.select(Collections.emptyList()).join();
      });
    });
    assertEquals(1, crudHeartbeatPool.availableConnections());
    assertEquals(2, pool.availableConnections());

    firstRouter.eval(
        "rawset(_G, 'crud', rawget(_G, 'tmp_crud'))", ValueFactory.emptyArray()).join();

    HelpersUtils.retry(100, 100, () -> {
      assertDoesNotThrow(() -> {
        spaceWithHeartbeat.select(Collections.emptyList()).join();
        spaceWithHeartbeat.select(Collections.emptyList()).join();
      });
      assertDoesNotThrow(() -> {
        space.select(Collections.emptyList()).join();
        space.select(Collections.emptyList()).join();
      });
      assertEquals(2, crudHeartbeatPool.availableConnections());
      assertEquals(2, pool.availableConnections());
    });
  }

  @Test
  public void testSelectAndInsertGeneral() {
    TarantoolCrudSpace person = client.space("person");

    Person expected = STUB_PERSON;

    List<?> insertResult = person.insert(baseOptions, listTypeRef, expected, OPTIONS).join().get();
    assertEquals(expected.asList(), removeBucketId(insertResult));

    List<Tuple<Person>> selectResult = person.select(baseOptions, listPersonTypeRef, PkEquals(0)).join().get();
    assertEquals(Collections.singletonList(expected), unpackT(selectResult));
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "3.*")
  public void testDMLTupleExtension() {
    TarantoolCrudSpace person = client.space("person");
    TarantoolCrudSpace personWithoutTupleExt = clientWithoutTupleExtensionEnabled.space("person");

    //eval
    String expression = "return box.tuple.new("
        + "{'1', '1'}, {format = {{ 'id', type = 'string' }, { 'value', type = 'string' }} })";
    TarantoolResponse<List<?>> tupleFromEval = client.eval(expression).join();
    assertFalse(tupleFromEval.getFormats().isEmpty());
    List<List<String>> evalExpected = Collections.singletonList(Arrays.asList("1", "1"));
    assertEquals(
        evalExpected,
        unpackT((List<Tuple<List<?>>>) tupleFromEval.get())
    );
    tupleFromEval = clientWithoutTupleExtensionEnabled.eval(expression).join();
    assertTrue(tupleFromEval.getFormats().isEmpty());
    assertEquals(
        evalExpected,
        tupleFromEval.get()
    );

    // replace
    Person expected = STUB_PERSON;
    Tuple<List<?>> replaceResult = person.replace(baseOptions, listTypeRef, expected, OPTIONS).join();
    assertFalse(replaceResult.getFormat().isEmpty());
    assertEquals(expected.asList(), removeBucketId(replaceResult.get()));

    replaceResult = personWithoutTupleExt.replace(baseOptions, listTypeRef, expected, OPTIONS).join();
    assertTrue(replaceResult.getFormat().isEmpty());
    assertEquals(expected.asList(), removeBucketId(replaceResult.get()));

    // select
    TarantoolResponse<List<Tuple<Person>>> selectResult = person.select(baseOptions, listPersonTypeRef, PkEquals(0))
        .join();
    assertFalse(selectResult.getFormats().isEmpty());
    assertEquals(Collections.singletonList(expected), unpackT(selectResult.get()));

    selectResult = personWithoutTupleExt.select(baseOptions, listPersonTypeRef, PkEquals(0))
        .join();
    assertTrue(selectResult.getFormats().isEmpty());
    assertEquals(Collections.singletonList(expected), unpackT(selectResult.get()));
  }

  @Test
  public void testSelectAndInsertBasic() {
    TarantoolCrudSpace person = client.space("person");

    Person expected = STUB_PERSON;

    List<?> insertResult = person.insert(expected).join().get();
    assertEquals(expected.asList(), removeBucketId(insertResult));

    List<Tuple<List<?>>> selectResult = person.select(ConditionPkEquals(0)).join();
    assertEquals(Collections.singletonList(expected.asList()), listRemoveBucketId(unpack(selectResult)));
  }

  @Test
  public void testSelectAndInsertClassEntity() {
    TarantoolCrudSpace person = client.space("person");

    Person expected = STUB_PERSON;

    Person insertResult = person.insert(expected, Person.class).join().get();
    assertEquals(expected, insertResult);

    List<Tuple<Person>> selectResult = person.select(Collections.singletonList(ConditionPkEquals(0)), Person.class)
        .join();
    assertEquals(Collections.singletonList(expected), unpackT(selectResult));
  }

  @Test
  public void testSelectAndInsertTypeReference() {
    TarantoolCrudSpace person = client.space("person");

    Person expected = STUB_PERSON;

    Person insertResult = person.insert(expected, personTypeRef).join().get();
    assertEquals(expected, insertResult);

    List<Tuple<Person>> selectResult = person.select(Collections.singletonList(ConditionPkEquals(0)), listPersonTypeRef)
        .join().get();
    assertEquals(Collections.singletonList(expected), unpackT(selectResult));
  }

  private static List<List<? extends Serializable>> PkEquals(int id) {
    return Collections.singletonList(Arrays.asList("==", "pk", id));
  }

  private static Condition ConditionPkEquals(int id) {
    return Condition.create("==", "pk", id);
  }

  private static List<?> removeBucketId(List<?> basicInsertResult) {
    return basicInsertResult.subList(0, 3);
  }

  private static List<List<?>> listRemoveBucketId(List<List<?>> basicInsertResult) {
    return basicInsertResult.stream().map(l -> l.subList(0, 3)).collect(Collectors.toList());
  }

  @Test
  public void testSelectAndDuplicatedInsert() {
    TarantoolCrudSpace person = client.space("person");

    List<?> expectedResult = Arrays.asList(1, true, "Roman");

    Person duplicatedPerson = Person.builder()
        .id(1)
        .isMarried(true)
        .name("Roman")
        .build();

    List<?> insertResult = person.insert(duplicatedPerson).join().get();
    assertEquals(expectedResult, insertResult.subList(0, 3));

    Throwable ex = assertThrows(CompletionException.class, () -> {person.insert(duplicatedPerson).join();});
    Throwable cause = ex.getCause();
    assertEquals(CrudException.class, cause.getClass());

    Condition condition = Condition.builder().withOperator("==").withFieldIdentifier("pk").withValue(1).build();
    List<Tuple<List<?>>> selectResult = person.select(condition).join();
    assertEquals(expectedResult, selectResult.get(0).get().subList(0, 3));

    condition = Condition.create("==", "pk", 1);
    selectResult = person.select(condition).join();
    assertEquals(expectedResult, selectResult.get(0).get().subList(0, 3));

    List<Tuple<List<?>>> selectResultWithTypeRef = person.select(baseOptions,
        listListTypeRef, Collections.singletonList(Arrays.asList("==", "pk", 1))).join().get();
    assertEquals(expectedResult, selectResultWithTypeRef.get(0).get().subList(0, 3));
  }

  @Test
  public void testTruncate() {
    TarantoolCrudSpace person = client.space("person");

    List<?> expectedResult = Arrays.asList(1, true, "Roman");

    Person insertedPerson = Person.builder()
        .id(1)
        .isMarried(true)
        .name("Roman")
        .build();

    List<?> insertResult = person.insert(insertedPerson).join().get();
    assertEquals(expectedResult, insertResult.subList(0, 3));

    Condition condition = Condition.create("==", "pk", 1);
    List<Tuple<List<?>>> selectResult = person.select(condition).join();
    assertEquals(expectedResult, selectResult.get(0).get().subList(0, 3));

    Boolean truncateResult = person.truncate().join();
    assertTrue(truncateResult);

    selectResult = person.select(condition).join();
    assertEquals(Collections.emptyList(), selectResult);

    insertResult = person.insert(insertedPerson).join().get();
    assertEquals(expectedResult, insertResult.subList(0, 3));

    selectResult = person.select(condition).join();
    assertEquals(expectedResult, selectResult.get(0).get().subList(0, 3));

    truncateResult = person.truncate(baseOptions, Collections.emptyMap()).join();
    assertTrue(truncateResult);

    selectResult = person.select(condition).join();
    assertEquals(Collections.emptyList(), selectResult);
  }

  @Test
  public void testCount() {
    TarantoolCrudSpace person = client.space("person");

    Condition condition = Condition.create("=", "pk", Collections.emptyList());
    assertEquals(0, person.count(condition).join());

    person.insert(new Person(1, true, "Roman")).join();
    person.insert(new Person(2, true, "Roman")).join();

    assertEquals(2, person.count(Collections.singletonList(condition)).join());

    condition = Condition.create("==", "pk", Collections.singletonList(1));
    assertEquals(1, person.count(Collections.singletonList(condition)).join());

    List<?> conditionAsList = Arrays.asList("=", "pk", Collections.emptyList());
    assertEquals(2, person.count(baseOptions, Collections.singletonList(conditionAsList)).join());
  }

  @Test
  public void testLen() {
    TarantoolCrudSpace person = client.space("person");

    assertEquals(0, person.len().join());

    person.insert(new Person(1, true, "Roman")).join();
    person.insert(new Person(2, true, "Roman")).join();

    assertEquals(2, person.len().join());

    assertEquals(2, person.len(baseOptions, Collections.emptyMap()).join());
  }

  @Test
  public void testSelectAndInsertWithParameters() {
    TarantoolCrudSpace person = client.space("person");

    List<?> expectedResult = Arrays.asList(1, true, "Roman");

    Person insertedPerson = Person.builder()
        .id(1)
        .isMarried(true)
        .name("Roman")
        .build();

    List<?> insertResult = person.insert(insertedPerson).join().get();
    assertEquals(expectedResult, insertResult.subList(0, 3));

    expectedResult = Arrays.asList("Roman", 1);

    Condition condition = Condition.builder()
        .withOperator("==")
        .withFieldIdentifier("pk")
        .withValue(1)
        .build();

    Condition conditionWithConditionOperator = Condition.builder()
        .withOperator(EQ)
        .withFieldIdentifier("pk")
        .withValue(1)
        .build();

    SelectOptions selectOptions = SelectOptions.builder()
        .withFields("name")
        .build();

    List<Tuple<List<?>>> selectResult = person.select(Collections.singletonList(condition), selectOptions).join();
    assertEquals(expectedResult, selectResult.get(0).get());

    selectResult = person.select(Collections.singletonList(conditionWithConditionOperator), selectOptions).join();
    assertEquals(expectedResult, selectResult.get(0).get());

    List<?> conditionAsList = Arrays.asList("==", "pk", 1);
    Map<?, ?> options = Collections.singletonMap("fields", Collections.singletonList("name"));

    TarantoolResponse<List<Tuple<List<?>>>> selectResultWithTypeRef = person.select(
        baseOptions,
        listListTypeRef,
        Collections.singletonList(conditionAsList),
        options
    ).join();
    assertEquals(expectedResult, selectResultWithTypeRef.get().get(0).get());
  }

  @Test
  public void testReplace() {
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.insert('person', {0, true, '0'})"));

    TarantoolCrudSpace personSpace = client.space("person");

    Person insertedPerson = new Person(0, true, "0");

    Map<String, Object> options = new HashMap<String, Object>() {{
      put("timeout", 2_000L);
    }};

    TypeReference<Person> typeAsClass = new TypeReference<Person>() {};
    TypeReference<List<?>> typeAsList = new TypeReference<List<?>>() {};

    insertedPerson.setName("1");
    List<?> baseReplaceResult = personSpace.replace(baseOptions, typeAsList, insertedPerson, options).join().get();
    assertEquals(insertedPerson.asList(), baseReplaceResult.subList(0, 3));

    insertedPerson.setName("2");
    Person baseReplaceResultWithClass = personSpace.replace(baseOptions, typeAsClass, insertedPerson).join().get();
    assertEquals(insertedPerson, baseReplaceResultWithClass);

    insertedPerson.setName("3");
    Person baseReplaceResultWithTypeRefAsClass = personSpace.replace(baseOptions, typeAsClass, insertedPerson)
        .join().get();
    assertEquals(insertedPerson, baseReplaceResultWithTypeRefAsClass);

    insertedPerson.setName("4");
    List<?> baseReplaceResultWithTypeRefAsList = personSpace.replace(baseOptions, typeAsList, insertedPerson)
        .join().get();
    assertEquals(insertedPerson.asList(), baseReplaceResultWithTypeRefAsList.subList(0, 3));

    insertedPerson.setName("5");
    List<?> replaceResultAsList = personSpace.replace(insertedPerson).join().get();
    assertEquals(insertedPerson.asList(), replaceResultAsList.subList(0, 3));

    insertedPerson.setName("6");
    Person replaceResultAsClassWithClass = personSpace.replace(insertedPerson, Person.class).join().get();
    assertEquals(insertedPerson, replaceResultAsClassWithClass);

    // input argument as list
    insertedPerson.setName("7");
    baseReplaceResult = personSpace.replace(baseOptions, typeAsList, insertedPerson.asList(), options).join().get();
    assertEquals(insertedPerson.asList(), baseReplaceResult.subList(0, 3));

    insertedPerson.setName("8");
    baseReplaceResultWithClass = personSpace.replace(baseOptions, typeAsClass, insertedPerson.asList()).join().get();
    assertEquals(insertedPerson, baseReplaceResultWithClass);

    insertedPerson.setName("9");
    baseReplaceResultWithTypeRefAsClass = personSpace.replace(baseOptions, typeAsClass, insertedPerson.asList())
        .join().get();
    assertEquals(insertedPerson, baseReplaceResultWithTypeRefAsClass);

    insertedPerson.setName("10");
    baseReplaceResultWithTypeRefAsList = personSpace.replace(baseOptions, typeAsList, insertedPerson.asList())
        .join().get();
    assertEquals(insertedPerson.asList(), baseReplaceResultWithTypeRefAsList.subList(0, 3));

    insertedPerson.setName("11");
    replaceResultAsList = personSpace.replace(insertedPerson.asList()).join().get();
    assertEquals(insertedPerson.asList(), replaceResultAsList.subList(0, 3));

    insertedPerson.setName("12");
    replaceResultAsClassWithClass = personSpace.replace(insertedPerson.asList(), Person.class).join().get();
    assertEquals(insertedPerson, replaceResultAsClassWithClass);
  }

  @Test
  public void testInsertMany() {
    TarantoolCrudSpace personSpace = client.space("person");

    Person person0 = new Person(0, true, "0");
    Person person1 = new Person(1, true, "1");
    List<Person> persons = Arrays.asList(person0, person1);

    Map<String, Object> options = new HashMap<String, Object>() {{
      put("timeout", 2_000L);
    }};

    CrudBatchResponse<List<Tuple<List<?>>>> baseInsertManyResult = personSpace.insertMany(
        baseOptions, listListTypeRef, persons, options
    ).join().get();
    List<Tuple<List<?>>> baseRows = baseInsertManyResult.getRows();
    baseRows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRows.size());
    assertEquals(persons.get(0).asList(), baseRows.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), baseRows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<Person>>> baseInsertManyResultWithClass = personSpace.insertMany(baseOptions,
        listPersonTypeRef, persons).join().get();
    List<Tuple<Person>> baseRowsWithClass = baseInsertManyResultWithClass.getRows();
    baseRowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithClass.size());
    assertEquals(persons.get(0), baseRowsWithClass.get(0).get());
    assertEquals(persons.get(1), baseRowsWithClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<Person>>> baseInsertManyResultWithTypeRefAsClass = personSpace.insertMany(baseOptions,
        listPersonTypeRef, persons).join().get();
    List<Tuple<Person>> baseRowsWithTypeRefAsClass = baseInsertManyResultWithTypeRefAsClass.getRows();
    baseRowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithTypeRefAsClass.size());
    assertEquals(persons.get(0), baseRowsWithTypeRefAsClass.get(0).get());
    assertEquals(persons.get(1), baseRowsWithTypeRefAsClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<List<?>>>> baseInsertManyResultWithTypeRefAsList = personSpace.insertMany(baseOptions,
        listListTypeRef, persons).join().get();
    List<Tuple<List<?>>> baseRowsWithTypeRefAsList = baseInsertManyResultWithTypeRefAsList.getRows();
    baseRowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRowsWithTypeRefAsList.size());
    assertEquals(persons.get(0).asList(), baseRowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), baseRowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<List<?>>>> insertManyResult = personSpace.insertMany(persons).join();
    List<Tuple<List<?>>> rows = insertManyResult.getRows();
    rows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rows.size());
    assertEquals(persons.get(0).asList(), rows.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), rows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<List<?>>>> insertManyResultWithTypeRefAsList = personSpace.insertMany(persons,
        TarantoolCrudClientTest.listListTypeRef).join().get();
    List<Tuple<List<?>>> rowsWithTypeRefAsList = insertManyResultWithTypeRefAsList.getRows();
    rowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rowsWithTypeRefAsList.size());
    assertEquals(persons.get(0).asList(), rowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), rowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<Person>>> insertManyResultWithClass = personSpace.insertMany(persons, Person.class)
        .join();
    List<Tuple<Person>> rowsWithClass = insertManyResultWithClass.getRows();
    rowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithClass.size());
    assertEquals(persons.get(0), rowsWithClass.get(0).get());
    assertEquals(persons.get(1), rowsWithClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    CrudBatchResponse<List<Tuple<Person>>> insertManyResultWithTypeRefAsClass = personSpace.insertMany(persons,
        listPersonTypeRef).join().get();
    List<Tuple<Person>> rowsWithTypeRefAsClass = insertManyResultWithTypeRefAsClass.getRows();
    rowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithTypeRefAsClass.size());
    assertEquals(persons.get(0), rowsWithTypeRefAsClass.get(0).get());
    assertEquals(persons.get(1), rowsWithTypeRefAsClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    // input as list
    List<List<?>> personsAsList = Arrays.asList(Arrays.asList(0, true, "0"),
        Arrays.asList(1, true, "1"));

    baseInsertManyResult = personSpace.insertMany(baseOptions, listListTypeRef,
        personsAsList, options).join().get();
    baseRows = baseInsertManyResult.getRows();
    baseRows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRows.size());
    assertEquals(personsAsList.get(0), baseRows.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), baseRows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    baseInsertManyResultWithClass = personSpace.insertMany(baseOptions, listPersonTypeRef, personsAsList).join().get();
    baseRowsWithClass = baseInsertManyResultWithClass.getRows();
    baseRowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithClass.size());
    assertEquals(personsAsList.get(0), baseRowsWithClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), baseRowsWithClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    baseInsertManyResultWithTypeRefAsClass = personSpace.insertMany(baseOptions, listPersonTypeRef, personsAsList)
        .join().get();
    baseRowsWithTypeRefAsClass = baseInsertManyResultWithTypeRefAsClass.getRows();
    baseRowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithTypeRefAsClass.size());
    assertEquals(personsAsList.get(0), baseRowsWithTypeRefAsClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), baseRowsWithTypeRefAsClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    baseInsertManyResultWithTypeRefAsList = personSpace.insertMany(baseOptions, listListTypeRef, personsAsList)
        .join().get();
    baseRowsWithTypeRefAsList = baseInsertManyResultWithTypeRefAsList.getRows();
    baseRowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRowsWithTypeRefAsList.size());
    assertEquals(personsAsList.get(0), baseRowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), baseRowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    insertManyResult = personSpace.insertMany(personsAsList).join();
    rows = insertManyResult.getRows();
    rows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rows.size());
    assertEquals(personsAsList.get(0), rows.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), rows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    insertManyResultWithTypeRefAsList = personSpace.insertMany(personsAsList, listListTypeRef).join().get();
    rowsWithTypeRefAsList = insertManyResultWithTypeRefAsList.getRows();
    rowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rowsWithTypeRefAsList.size());
    assertEquals(personsAsList.get(0), rowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), rowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    insertManyResultWithClass = personSpace.insertMany(personsAsList, Person.class).join();
    rowsWithClass = insertManyResultWithClass.getRows();
    rowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithClass.size());
    assertEquals(personsAsList.get(0), rowsWithClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), rowsWithClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    insertManyResultWithTypeRefAsClass = personSpace.insertMany(personsAsList, listPersonTypeRef).join().get();
    rowsWithTypeRefAsClass = insertManyResultWithTypeRefAsClass.getRows();
    rowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithTypeRefAsClass.size());
    assertEquals(personsAsList.get(0), rowsWithTypeRefAsClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), rowsWithTypeRefAsClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));
  }

  public static Stream<Arguments> dataForTestInsertObject() {
    final TarantoolCrudSpace space = client.space("person");
    final PersonAsMap insertValue = new PersonAsMap(123, true, "name");

    return Stream.of(
        Arguments.of(
            (Supplier<List<?>>) () -> space.insertObject(insertValue).join().get(),
            insertValue.asList()
        ),
        Arguments.of(
            (Supplier<List<?>>) () ->
                space.insertObject(insertValue, InsertOptions.builder().build()).join().get(),
            insertValue.asList()
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> space.insertObject(insertValue.asMap()).join().get(),
            insertValue.asList()
        ),
        Arguments.of(
            (Supplier<List<?>>) () ->
                space.insertObject(insertValue.asMap(), InsertOptions.builder().build()).join().get(),
            insertValue.asList()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertObject")
  void testInsertObject(final Supplier<List<?>> executingMethod, final List<?> expectedValue) {
    final List<?> insertedValue = executingMethod.get();
    assertEquals(expectedValue, insertedValue.subList(0, expectedValue.size()));
  }

  @Test
  void testInsertObjectThrows() {
    final Throwable exception = assertThrows(CompletionException.class,
        () -> client.space("person").insertObject(personInstance).join());
    assertEquals(CrudException.class, exception.getCause().getClass());
  }

  public static Stream<Arguments> dataForTestInsertObjectMany() {
    final Comparator<List<?>> tupleComparator = Comparator.comparing(t -> (Integer) t.get(0));
    final TarantoolCrudSpace space = client.space("person");
    final PersonAsMap insertValue = new PersonAsMap(123, true, "name");
    final PersonAsMap secondInsertValue = new PersonAsMap(321, null, "true");
    final List<PersonAsMap> insertValues = Arrays.asList(insertValue, secondInsertValue);
    final List<List<?>> expectedValues =
        insertValues.stream().map(PersonAsMap::asList).sorted(tupleComparator).collect(Collectors.toList());

    return Stream.of(
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(space.insertObjectMany(insertValues).join(), tupleComparator),
            expectedValues
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(
                space.insertObjectMany(insertValues, InsertManyOptions.builder().build()).join(), tupleComparator),
            expectedValues
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(
                space.insertObjectMany(insertValues.stream().map(PersonAsMap::asMap)
                    .collect(Collectors.toList())).join(), tupleComparator),
            expectedValues
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(
                space.insertObjectMany(insertValues.stream().map(PersonAsMap::asMap).collect(Collectors.toList()),
                    InsertManyOptions.builder().build()).join(), tupleComparator
            ),
            expectedValues
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertObjectMany")
  void testInsertObjectMany(final Supplier<List<List<?>>> executingMethod, final List<List<?>> expectedValues) {
    final List<List<?>> insertedValues = executingMethod.get();
    assertEquals(expectedValues.size(), insertedValues.size());

    for (int i = 0; i < expectedValues.size(); i++) {
      final List<?> expectedValue = expectedValues.get(i);
      assertEquals(expectedValue, insertedValues.get(i).subList(0, expectedValue.size()));
    }
  }

  public static Stream<Arguments> dataForTestReplaceObject() {
    final TarantoolCrudSpace space = client.space("person");
    final PersonAsMap insertValue = new PersonAsMap(123, true, "name");

    return Stream.of(
        Arguments.of(
            (Supplier<List<?>>) () -> space.replaceObject(insertValue).join().get(),
            insertValue.asList()
        ),
        Arguments.of(
            (Supplier<List<?>>) () ->
                space.replaceObject(insertValue, InsertOptions.builder().build()).join().get(),
            insertValue.asList()
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> space.replaceObject(insertValue.asMap()).join().get(),
            insertValue.asList()
        ),
        Arguments.of(
            (Supplier<List<?>>) () ->
                space.replaceObject(insertValue.asMap(), InsertOptions.builder().build()).join().get(),
            insertValue.asList()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestReplaceObject")
  void testReplaceObject(final Supplier<List<?>> executingMethod, final List<?> expectedValue) {
    final List<?> insertedValue = executingMethod.get();
    assertEquals(expectedValue, insertedValue.subList(0, expectedValue.size()));
  }

  public static Stream<Arguments> dataForTestReplaceObjectMany() {
    final Comparator<List<?>> tupleComparator = Comparator.comparing(t -> (Integer) t.get(0));
    final TarantoolCrudSpace space = client.space("person");
    final PersonAsMap insertValue = new PersonAsMap(123, true, "name");
    final PersonAsMap secondInsertValue = new PersonAsMap(321, null, "true");
    final List<PersonAsMap> insertValues = Arrays.asList(insertValue, secondInsertValue);
    final List<List<?>> expectedValues =
        insertValues.stream().map(PersonAsMap::asList).sorted(tupleComparator).collect(Collectors.toList());

    return Stream.of(
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(space.replaceObjectMany(insertValues).join(), tupleComparator),
            expectedValues
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(
                space.replaceObjectMany(insertValues, InsertManyOptions.builder().build()).join(), tupleComparator),
            expectedValues
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(
                space.replaceObjectMany(insertValues.stream().map(PersonAsMap::asMap)
                    .collect(Collectors.toList())).join(), tupleComparator),
            expectedValues
        ),
        Arguments.of(
            (Supplier<List<?>>) () -> mapBatchToType(
                space.replaceObjectMany(insertValues.stream().map(PersonAsMap::asMap).collect(Collectors.toList()),
                    InsertManyOptions.builder().build()).join(), tupleComparator
            ),
            expectedValues
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestReplaceObjectMany")
  void testReplaceObjectMany(final Supplier<List<List<?>>> executingMethod, final List<List<?>> expectedValues) {
    final List<List<?>> insertedValues = executingMethod.get();
    assertEquals(expectedValues.size(), insertedValues.size());

    for (int i = 0; i < expectedValues.size(); i++) {
      final List<?> expectedValue = expectedValues.get(i);
      assertEquals(expectedValue, insertedValues.get(i).subList(0, expectedValue.size()));
    }
  }

  public static Stream<Arguments> dataForTestUpsertObject() {
    final PersonAsMap insertValue = new PersonAsMap(123, true, "name");

    List<Object[]> testCases = Arrays.asList(
        new Object[] { new PersonAsMap(123, true, "name"), "name", "Kolya"},
        new Object[] { new PersonAsMap(123, true, "name"), "is_married", null}
    );

    return testCases.stream()
        .flatMap(tc -> prepareForTestUpsertObject(
            insertValue, (PersonAsMap) tc[0], (String) tc[1], tc[2]
            )
        );
  }

  public static Stream<Arguments> prepareForTestUpsertObject(PersonAsMap origin, PersonAsMap expected, String updField,
      Object newFieldValue) {
    final TarantoolCrudSpace space = client.space("person");

    final Supplier<List<List<?>>> selectSupplier =
        () -> space.select().join().stream().map(Tuple::get).collect(Collectors.toList());

    final List<List<?>> operationsAsList = Collections.singletonList(Arrays.asList("=", updField, newFieldValue));
    final Operations operationsAsClass = Operations.create().set(updField, newFieldValue);

    final Options options = BaseOptions.builder().withTimeout(3_000L).build();
    final UpdateOptions updateOptions = UpdateOptions.builder().build();

    return Stream.of(
        Arguments.of(
            (Runnable) () -> space.upsertObject(options, origin, operationsAsList).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(options, origin.asMap(), operationsAsList).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin, operationsAsList).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin.asMap(), operationsAsList).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin, operationsAsClass).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin.asMap(), operationsAsClass).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin, operationsAsList, updateOptions).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin.asMap(), operationsAsList, updateOptions).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin, operationsAsClass, updateOptions).join(),
            selectSupplier,
            expected.asList()
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObject(origin.asMap(), operationsAsClass, updateOptions).join(),
            selectSupplier,
            expected.asList()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpsertObject")
  void testUpsertObject(final Runnable executingMethod, final Supplier<List<List<?>>> selectSupplier,
      final List<?> expectedValue) {
    executingMethod.run();
    final List<List<?>> insertedTuples = selectSupplier.get();
    assertEquals(1, insertedTuples.size());

    for (final List<?> insertedValue : insertedTuples) {
      assertEquals(expectedValue, insertedValue.subList(0, expectedValue.size()));
    }
  }

  public static Stream<Arguments> dataForTestUpsertObjectMany() {
    final Comparator<List<?>> tupleComparator = Comparator.comparing(t -> (Integer) t.get(0));
    final TarantoolCrudSpace space = client.space("person");
    final PersonAsMap insertValue = new PersonAsMap(123, true, "name");
    final PersonAsMap secondInsertValue = new PersonAsMap(321, null, "true");
    final List<PersonAsMap> insertValues = Arrays.asList(insertValue, secondInsertValue);
    final List<List<?>> expectedValues =
        insertValues.stream().map(PersonAsMap::asList).sorted(tupleComparator).collect(Collectors.toList());

    final List<List<?>> operations = Collections.singletonList(Arrays.asList("=", "name", "Kolya"));
    final Options options = BaseOptions.builder().withTimeout(3_000L).build();
    final UpsertManyOptions upsertManyOptions = UpsertManyOptions.builder().build();
    final Operations operationsAsClass = Operations.create().set("name", "Kolya");
    final Supplier<List<List<?>>> selectSupplier = () -> space.select().join().stream().map(Tuple::get)
        .sorted(tupleComparator).collect(Collectors.toList());

    final List<?> ops = Arrays.asList(Arrays.asList(insertValue, operations),
        Arrays.asList(secondInsertValue, operations));
    final List<?> opsAsMap = Arrays.asList(Arrays.asList(insertValue.asMap(), operations),
        Arrays.asList(secondInsertValue.asMap(), operations));

    final UpsertBatch upsertBatch = UpsertBatch.create()
        .add(insertValue, operationsAsClass).add(secondInsertValue, operationsAsClass);
    final UpsertBatch upsertBatchAsMap = UpsertBatch.create()
        .add(insertValue.asMap(), operationsAsClass).add(secondInsertValue.asMap(), operationsAsClass);

    return Stream.of(
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(options, ops).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(options, opsAsMap).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(ops).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(opsAsMap).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(ops, upsertManyOptions).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(opsAsMap, upsertManyOptions).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(upsertBatch).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(upsertBatchAsMap).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(upsertBatch, upsertManyOptions).join(),
            selectSupplier,
            expectedValues
        ),
        Arguments.of(
            (Runnable) () -> space.upsertObjectMany(upsertBatchAsMap, upsertManyOptions).join(),
            selectSupplier,
            expectedValues
        )
    );
  }

  @ParameterizedTest
  @MethodSource("dataForTestUpsertObjectMany")
  void testUpsertObjectMany(final Runnable executingMethod, final Supplier<List<List<?>>> selectSupplier,
      final List<List<?>> expectedValues) {
    executingMethod.run();
    final List<List<?>> insertedTuples = selectSupplier.get();

    assertEquals(expectedValues.size(), insertedTuples.size());
    for (int i = 0; i < expectedValues.size(); i++) {
      final List<?> expectedValue = expectedValues.get(i);
      assertEquals(expectedValue, insertedTuples.get(i).subList(0, expectedValue.size()));
    }
  }

  @Test
  public void testReplaceMany() {
    assertDoesNotThrow(
        () -> clusterContainer.executeCommand("return crud.insert_many('person', {{0, true, '0'}, {1, " +
            "true, '1'}})"));

    TarantoolCrudSpace personSpace = client.space("person");

    Person firstPerson = new Person(0, true, "0");
    Person secondPerson = new Person(1, true, "1");
    List<Person> persons = Arrays.asList(firstPerson, secondPerson);

    Map<String, Object> options = new HashMap<String, Object>() {{
      put("timeout", 2_000L);
    }};

    firstPerson.setName("00");
    secondPerson.setName("10");
    CrudBatchResponse<List<Tuple<List<?>>>> baseReplaceResult = personSpace.replaceMany(baseOptions,
        listListTypeRef, persons, options).join().get();
    List<Tuple<List<?>>> baseRows = baseReplaceResult.getRows();
    baseRows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRows.size());
    assertEquals(persons.get(0).asList(), baseRows.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), baseRows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("01");
    secondPerson.setName("11");
    CrudBatchResponse<List<Tuple<Person>>> baseReplaceResultWithClass = personSpace.replaceMany(baseOptions,
        listPersonTypeRef, persons).join().get();
    List<Tuple<Person>> baseRowsWithClass = baseReplaceResultWithClass.getRows();
    baseRowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithClass.size());
    assertEquals(persons.get(0), baseRowsWithClass.get(0).get());
    assertEquals(persons.get(1), baseRowsWithClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("02");
    secondPerson.setName("12");
    CrudBatchResponse<List<Tuple<Person>>> baseReplaceResultWithTypeRefAsClass = personSpace.replaceMany(baseOptions,
        listPersonTypeRef, persons).join().get();
    List<Tuple<Person>> baseRowsWithTypeRefAsClass = baseReplaceResultWithTypeRefAsClass.getRows();
    baseRowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithTypeRefAsClass.size());
    assertEquals(persons.get(0), baseRowsWithTypeRefAsClass.get(0).get());
    assertEquals(persons.get(1), baseRowsWithTypeRefAsClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("03");
    secondPerson.setName("13");
    CrudBatchResponse<List<Tuple<List<?>>>> baseReplaceResultWithTypeRefAsList = personSpace.replaceMany(baseOptions,
        listListTypeRef, persons).join().get();
    List<Tuple<List<?>>> baseRowsWithTypeRefAsList = baseReplaceResultWithTypeRefAsList.getRows();
    baseRowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRowsWithTypeRefAsList.size());
    assertEquals(persons.get(0).asList(), baseRowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), baseRowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("04");
    secondPerson.setName("14");
    CrudBatchResponse<List<Tuple<List<?>>>> replaceResult = personSpace.replaceMany(persons).join();
    List<Tuple<List<?>>> rows = replaceResult.getRows();
    rows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rows.size());
    assertEquals(persons.get(0).asList(), rows.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), rows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("05");
    secondPerson.setName("15");
    CrudBatchResponse<List<Tuple<List<?>>>> replaceResultWithTypeRefAsList = personSpace.replaceMany(persons,
        listListTypeRef).join().get();
    List<Tuple<List<?>>> rowsWithTypeRefAsList = replaceResultWithTypeRefAsList.getRows();
    rowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rowsWithTypeRefAsList.size());
    assertEquals(persons.get(0).asList(), rowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(persons.get(1).asList(), rowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("06");
    secondPerson.setName("16");
    CrudBatchResponse<List<Tuple<Person>>> replaceResultWithClass = personSpace.replaceMany(persons, Person.class)
        .join();
    List<Tuple<Person>> rowsWithClass = replaceResultWithClass.getRows();
    rowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithClass.size());
    assertEquals(persons.get(0), rowsWithClass.get(0).get());
    assertEquals(persons.get(1), rowsWithClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    firstPerson.setName("07");
    secondPerson.setName("17");
    CrudBatchResponse<List<Tuple<Person>>> replaceResultWithTypeRefAsClass = personSpace.replaceMany(persons,
        listPersonTypeRef).join().get();
    List<Tuple<Person>> rowsWithTypeRefAsClass = replaceResultWithTypeRefAsClass.getRows();
    rowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithTypeRefAsClass.size());
    assertEquals(persons.get(0), rowsWithTypeRefAsClass.get(0).get());
    assertEquals(persons.get(1), rowsWithTypeRefAsClass.get(1).get());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    // input as list
    List<List<?>> personsAsList = Arrays.asList(Arrays.asList(0, true, "0"),
        Arrays.asList(1, true, "1"));

    personsAsList.set(0, Arrays.asList(0, true, "08"));
    personsAsList.set(1, Arrays.asList(1, true, "18"));
    baseReplaceResult = personSpace.replaceMany(baseOptions, listListTypeRef, personsAsList, options).join().get();
    baseRows = baseReplaceResult.getRows();
    baseRows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRows.size());
    assertEquals(personsAsList.get(0), baseRows.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), baseRows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "09"));
    personsAsList.set(1, Arrays.asList(1, true, "19"));
    baseReplaceResultWithClass = personSpace.replaceMany(baseOptions, listPersonTypeRef, personsAsList).join().get();
    baseRowsWithClass = baseReplaceResultWithClass.getRows();
    baseRowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithClass.size());
    assertEquals(personsAsList.get(0), baseRowsWithClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), baseRowsWithClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "010"));
    personsAsList.set(1, Arrays.asList(1, true, "110"));
    baseReplaceResultWithTypeRefAsClass = personSpace.replaceMany(baseOptions, listPersonTypeRef, personsAsList)
        .join().get();
    baseRowsWithTypeRefAsClass = baseReplaceResultWithTypeRefAsClass.getRows();
    baseRowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, baseRowsWithTypeRefAsClass.size());
    assertEquals(personsAsList.get(0), baseRowsWithTypeRefAsClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), baseRowsWithTypeRefAsClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "011"));
    personsAsList.set(1, Arrays.asList(1, true, "111"));
    baseReplaceResultWithTypeRefAsList = personSpace.replaceMany(baseOptions, listListTypeRef, personsAsList)
        .join().get();
    baseRowsWithTypeRefAsList = baseReplaceResultWithTypeRefAsList.getRows();
    baseRowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, baseRowsWithTypeRefAsList.size());
    assertEquals(personsAsList.get(0), baseRowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), baseRowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "012"));
    personsAsList.set(1, Arrays.asList(1, true, "112"));
    replaceResult = personSpace.replaceMany(personsAsList).join();
    rows = replaceResult.getRows();
    rows.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rows.size());
    assertEquals(personsAsList.get(0), rows.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), rows.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "013"));
    personsAsList.set(1, Arrays.asList(1, true, "113"));
    replaceResultWithTypeRefAsList = personSpace.replaceMany(personsAsList, listListTypeRef).join().get();
    rowsWithTypeRefAsList = replaceResultWithTypeRefAsList.getRows();
    rowsWithTypeRefAsList.sort(Comparator.comparing(p -> (int) (Object) (p.get().get(0))));
    assertEquals(2, rowsWithTypeRefAsList.size());
    assertEquals(personsAsList.get(0), rowsWithTypeRefAsList.get(0).get().subList(0, 3));
    assertEquals(personsAsList.get(1), rowsWithTypeRefAsList.get(1).get().subList(0, 3));
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "014"));
    personsAsList.set(1, Arrays.asList(1, true, "114"));
    replaceResultWithClass = personSpace.replaceMany(personsAsList, Person.class).join();
    rowsWithClass = replaceResultWithClass.getRows();
    rowsWithClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithClass.size());
    assertEquals(personsAsList.get(0), rowsWithClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), rowsWithClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));

    personsAsList.set(0, Arrays.asList(0, true, "015"));
    personsAsList.set(1, Arrays.asList(1, true, "115"));
    replaceResultWithTypeRefAsClass = personSpace.replaceMany(personsAsList, listPersonTypeRef).join().get();
    rowsWithTypeRefAsClass = replaceResultWithTypeRefAsClass.getRows();
    rowsWithTypeRefAsClass.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(2, rowsWithTypeRefAsClass.size());
    assertEquals(personsAsList.get(0), rowsWithTypeRefAsClass.get(0).get().asList());
    assertEquals(personsAsList.get(1), rowsWithTypeRefAsClass.get(1).get().asList());
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));
  }

  @Test
  public void testUpsert() {
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.insert('person', {0, true, '0'})"));

    TarantoolCrudSpace personSpace = client.space("person");

    assertEquals(1, personSpace.select().join().size());

    Person oldPerson = new Person(0, true, "1");
    Person newPerson = new Person(1, true, "2");

    Options options = BaseOptions.builder()
        .withTimeout(2_000)
        .build();

    Executable checkPersons = () -> {
      List<Tuple<Person>> persons = personSpace.select(baseOptions, listPersonTypeRef).join().get();
      persons.sort(Comparator.comparing(person -> person.get().getId()));
      assertEquals(2, persons.size());
      assertEquals(oldPerson.getName(), persons.get(0).get().getName());
      assertEquals(newPerson.getName(), persons.get(1).get().getName());
      assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.truncate('person')"));
    };

    personSpace.upsert(options, oldPerson,
        Collections.singletonList(Arrays.asList("=", "name", oldPerson.getName()))).join();
    personSpace.upsert(options, newPerson,
        Collections.singletonList(Arrays.asList("=", "name", oldPerson.getName()))).join();
    assertDoesNotThrow(checkPersons);

    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.insert('person', {0, true, '0'})"));

    personSpace.upsert(options, oldPerson, Operations.create().set("name", oldPerson.getName())).join();
    personSpace.upsert(options, newPerson, Operations.create().set("name", oldPerson.getName())).join();
    assertDoesNotThrow(checkPersons);

    // not generic
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.insert('person', {0, true, '0'})"));
    assertEquals(1, personSpace.select().join().size());

    personSpace.upsert(oldPerson,
        Collections.singletonList(Arrays.asList("=", "name", oldPerson.getName()))).join();
    personSpace.upsert(newPerson,
        Collections.singletonList(Arrays.asList("=", "name", oldPerson.getName()))).join();
    assertDoesNotThrow(checkPersons);

    personSpace.upsert(oldPerson, Operations.create().set("name", oldPerson.getName())).join();
    personSpace.upsert(newPerson, Operations.create().set("name", oldPerson.getName())).join();
    assertDoesNotThrow(checkPersons);

    // input tuple as list
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.insert('person', {0, true, '0'})"));
    assertEquals(1, personSpace.select().join().size());

    List<?> oldPersonAsList = oldPerson.asList();
    List<?> newPersonAsList = newPerson.asList();

    personSpace.upsert(options, oldPersonAsList,
        Collections.singletonList(Arrays.asList("=", "name", oldPerson.getName()))).join();
    personSpace.upsert(options, newPersonAsList,
        Collections.singletonList(Arrays.asList("=", "name", newPerson.getName()))).join();
    assertDoesNotThrow(checkPersons);

    personSpace.upsert(options, oldPersonAsList, Operations.create().set("name", oldPerson.getName())).join();
    personSpace.upsert(options, newPersonAsList, Operations.create().set("name", newPerson.getName())).join();
    assertDoesNotThrow(checkPersons);

    // input tuple as list and not generic
    assertDoesNotThrow(() -> clusterContainer.executeCommand("return crud.insert('person', {0, true, '0'})"));
    assertEquals(1, personSpace.select().join().size());

    personSpace.upsert(oldPersonAsList,
        Collections.singletonList(Arrays.asList("=", "name", oldPerson.getName()))).join();
    personSpace.upsert(newPersonAsList,
        Collections.singletonList(Arrays.asList("=", "name", newPerson.getName()))).join();
    assertDoesNotThrow(checkPersons);

    personSpace.upsert(oldPersonAsList, Operations.create().set("name", oldPersonAsList.get(2))).join();
    personSpace.upsert(newPersonAsList, Operations.create().set("name", newPersonAsList.get(2))).join();
    assertDoesNotThrow(checkPersons);
  }

  @Test
  public void testUpsertMany() {
    TarantoolCrudSpace personSpace = client.space("person");
    String PREPARE = "crud.truncate('person'); " +
        "return crud.insert_many('person', {{1, true, '11'}, {2, true, '22'}})";
    List<Person> persons = Arrays.asList(
        new Person(1, true, "One"),
        new Person(2, true, "Second"),
        new Person(3, true, "Third"),
        new Person(4, true, "Fourth")
    );
    UpsertManyOptions options = UpsertManyOptions
        .builder()
        .withTimeout(2_000)
        .build();
    List<?> batchOpsAsList = persons
        .stream()
        .map(p -> Arrays.asList(p, Collections.singletonList(Arrays.asList("=", "name", p.getName()))))
        .collect(Collectors.toList());
    List<?> batchTuplesAsListOpsAsList = persons
        .stream()
        .map(p -> Arrays.asList(p.asList(), Collections.singletonList(Arrays.asList("=", "name", p.getName()))))
        .collect(Collectors.toList());
    UpsertBatch upsertBatch = persons
        .stream()
        .reduce(
            UpsertBatch.create(),
            (batch, p) -> batch.add(p, Operations.create().set("name", p.getName())),
            (batch1, batch2) -> {
              batch1.addAll(batch2);
              return batch1;
            }
        );
    List<ThrowingSupplier<List<CrudError>>> actions = Arrays.asList(
        () -> personSpace.upsertMany(batchOpsAsList).join(),
        () -> personSpace.upsertMany(batchTuplesAsListOpsAsList).join(),
        () -> personSpace.upsertMany(options, batchOpsAsList).join(),
        () -> personSpace.upsertMany(options, batchTuplesAsListOpsAsList).join(),
        () -> personSpace.upsertMany(batchOpsAsList, options).join(),
        () -> personSpace.upsertMany(batchTuplesAsListOpsAsList, options).join(),
        () -> personSpace.upsertMany(upsertBatch).join(),
        () -> personSpace.upsertMany(upsertBatch, options).join()
    );

    for (ThrowingSupplier<List<CrudError>> upsertManyAction : actions) {
      assertDoesNotThrow(() -> clusterContainer.executeCommand(PREPARE));
      assertEquals(2, personSpace.select().join().size());

      assertNull(assertDoesNotThrow(upsertManyAction), "crud.upsertMany returned errors");

      List<Tuple<Person>> selectedPersons = personSpace.select(baseOptions, listPersonTypeRef).join().get();
      selectedPersons.sort(Comparator.comparing(person -> person.get().getId()));
      assertEquals(persons.size(), selectedPersons.size());
      for (int i = 0; i < selectedPersons.size(); i++) {
        assertEquals(persons.get(i).getName(), selectedPersons.get(i).get().getName());
      }
    }
  }

  public static Stream<Arguments> dataForGetTest() {

    List<?> keyAsList = Collections.singletonList(0);
    PersonKey keyAsClass = new PersonKey(0);

    Person resultAsClass = new Person(0, true, "0");
    List<?> resultAsList = Arrays.asList(0, true, "0");

    GetOptions baseOptions = GetOptions.builder().build();

    int begin = 0;
    int end = resultAsList.size();

    TarantoolCrudSpace space = client.space("person");

    Runnable prepareAction = () -> {
      assertEquals(resultAsList, space.insert(resultAsList).join().get().subList(begin, end));
    };

    return Stream.of(
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsList).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsClass).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsList, baseOptions).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsClass, baseOptions).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsList, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsClass, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsList, baseOptions, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsClass, baseOptions, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsList, listTypeRef).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsClass, listTypeRef).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsList, personTypeRef).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsClass, personTypeRef).join().get()),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsList, baseOptions, listTypeRef).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.get(keyAsClass, baseOptions, listTypeRef).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsList, baseOptions, personTypeRef).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.get(keyAsClass, baseOptions, personTypeRef).join().get()));
  }

  @ParameterizedTest
  @MethodSource("dataForGetTest")
  public void testGet(Runnable prepareAction, Object result, Supplier<Object> callingMethod) {
    prepareAction.run();
    assertEquals(result, callingMethod.get());
  }

  public static Stream<Arguments> dataForDeleteTest() {

    Person resultAsClass = new Person(0, true, "0");
    List<?> resultAsList = Arrays.asList(0, true, "0");

    List<?> keyAsList = Collections.singletonList(0);
    PersonKey keyAsClass = new PersonKey(0);

    DeleteOptions baseOptions = DeleteOptions.builder().build();

    int begin = 0;
    int end = resultAsList.size();

    TarantoolCrudSpace space = client.space("person");

    Runnable prepareAction = () -> {
      assertEquals(resultAsList, space.insert(resultAsList).join().get().subList(begin, end));
    };

    Runnable finalAction = () -> {
      assertTrue(space.select(Collections.emptyList()).join().isEmpty());
    };

    return Stream.of(
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsList).join().get().subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsClass).join().get().subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsList, baseOptions).join().get().subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsClass, baseOptions).join().get().subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsList, Person.class).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsClass, Person.class).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsList, baseOptions, Person.class).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsClass, baseOptions, Person.class).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsList, listTypeRef).join().get().subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsClass, listTypeRef).join().get().subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsList, personTypeRef).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsClass, personTypeRef).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsList, baseOptions, listTypeRef).join().get()
                .subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.delete(keyAsClass, baseOptions, listTypeRef).join().get()
                .subList(begin, end),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsList, baseOptions, personTypeRef).join().get(),
            finalAction),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.delete(keyAsClass, baseOptions, personTypeRef).join().get(),
            finalAction));
  }

  @ParameterizedTest
  @MethodSource("dataForDeleteTest")
  public void testDelete(Runnable prepareAction, Object result, Supplier<Object> callingMethod,
      Runnable finalAction) {
    prepareAction.run();

    assertEquals(result, callingMethod.get());

    finalAction.run();
  }

  @Test
  public void testMinMax() {
    TarantoolCrudSpace person = client.space("person");
    List<Person> insertedPersons = Arrays.asList(new Person(0, true, "0"),
        new Person(1, true, "1"));

    for (Person obj : insertedPersons) {
      assertEquals(obj, person.insert(obj, Person.class).join().get());
    }

    Map<String, Object> options = new HashMap<String, Object>() {{
      put("timeout", 2_000L);
    }};

    TypeReference<List<?>> typeReferenceAsList = new TypeReference<List<?>>() {};
    TypeReference<Person> typeReferenceAsClass = new TypeReference<Person>() {};

    List<?> baseMinResult = person.min(baseOptions, listTypeRef, "pk", options).join().get();
    Person baseMinResultWithClass = person.min(baseOptions, personTypeRef, "pk").join().get();
    Person baseMinResultWithTypeRefAsClass = person.min(baseOptions, typeReferenceAsClass, "pk").join().get();
    List<?> baseMinResultWithTypeRefAsList = person.min(baseOptions, typeReferenceAsList, "pk").join().get();
    List<?> minResult = person.min("pk").join().get();
    Person minResultWithClass = person.min("pk", Person.class).join().get();
    Person minResultWithTypeRefAsClass = person.min("pk", typeReferenceAsClass).join().get();
    List<?> minResultWithTypeRefASList = person.min("pk", typeReferenceAsList).join().get();

    List<?> baseMaxResult = person.max(baseOptions, listTypeRef, "pk", options).join().get();
    Person baseMaxResultWithClass = person.max(baseOptions, personTypeRef, "pk").join().get();
    Person baseMaxResultWithTypeRefAsClass = person.max(baseOptions, typeReferenceAsClass, "pk").join().get();
    List<?> baseMaxResultWithTypeRefAsList = person.max(baseOptions, typeReferenceAsList, "pk").join().get();
    List<?> maxResult = person.max("pk").join().get();
    Person maxResultWithClass = person.max("pk", Person.class).join().get();
    Person maxResultWithTypeRefAsClass = person.max("pk", typeReferenceAsClass).join().get();
    List<?> maxResultWithTypeRefASList = person.max("pk", typeReferenceAsList).join().get();

    assertEquals(insertedPersons.get(0).asList(), baseMinResult.subList(0, 3));
    assertEquals(insertedPersons.get(0), baseMinResultWithClass);
    assertEquals(insertedPersons.get(0), baseMinResultWithTypeRefAsClass);
    assertEquals(insertedPersons.get(0).asList(), baseMinResultWithTypeRefAsList.subList(0, 3));
    assertEquals(insertedPersons.get(0).asList(), minResult.subList(0, 3));
    assertEquals(insertedPersons.get(0), minResultWithClass);
    assertEquals(insertedPersons.get(0), minResultWithTypeRefAsClass);
    assertEquals(insertedPersons.get(0).asList(), minResultWithTypeRefASList.subList(0, 3));

    assertEquals(insertedPersons.get(1).asList(), baseMaxResult.subList(0, 3));
    assertEquals(insertedPersons.get(1), baseMaxResultWithClass);
    assertEquals(insertedPersons.get(1), baseMaxResultWithTypeRefAsClass);
    assertEquals(insertedPersons.get(1).asList(), baseMaxResultWithTypeRefAsList.subList(0, 3));
    assertEquals(insertedPersons.get(1).asList(), maxResult.subList(0, 3));
    assertEquals(insertedPersons.get(1), maxResultWithClass);
    assertEquals(insertedPersons.get(1), maxResultWithTypeRefAsClass);
    assertEquals(insertedPersons.get(1).asList(), maxResultWithTypeRefASList.subList(0, 3));
  }

  public static Stream<Arguments> dataForUpdateTest() {
    List<Object[]> testCases = Arrays.asList(
        new Object[] { new Person(1, true, "Фёдор"), "name", "Фёдор"},
        new Object[] { new Person(1, false, "Roman"), "is_married", Boolean.FALSE},
        new Object[] { new Person(1, null, "Roman"), "is_married", null}
    );

    return testCases.stream()
        .flatMap(tc -> prepareDataForUpdateTest(
            personInstance, (Person) tc[0], (String) tc[1], tc[2]
        )
    );
  }

  public static Stream<Arguments> prepareDataForUpdateTest(Person origin, Person expected, String updField,
      Object newFieldValue) {
    TarantoolCrudSpace space = client.space("person");

    List<?> keyAsList = Collections.singletonList(origin.getId());
    PersonKey keyAsClass = new PersonKey(origin.getId());

    // after update
    Person resultAsClass = expected;
    List<?> resultAsList = Arrays.asList(expected.getId(), expected.getIsMarried(), expected.getName());

    Operations operationsAsClass = Operations.create().set(updField, newFieldValue);
    List<List<?>> operationsAsList = Collections.singletonList(Arrays.asList("=", updField, newFieldValue));

    Options genericOptions = BaseOptions.builder().build();
    UpdateOptions baseUpdateOptions = UpdateOptions.builder().build();

    int begin = 0;
    int end = resultAsList.size();

    Runnable prepareAction = () -> {
      assertEquals(origin, space.insert(origin, Person.class).join().get());
    };

    return Stream.of(
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass).join().get().subList(begin, end)),

        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, baseUpdateOptions).join().get()
                .subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, baseUpdateOptions).join().get()
                .subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, baseUpdateOptions).join().get()
                .subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, baseUpdateOptions).join().get()
                .subList(begin, end)),

        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, Person.class).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, Person.class).join().get()),

        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, baseUpdateOptions, Person.class)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, baseUpdateOptions, Person.class)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, baseUpdateOptions, Person.class)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, baseUpdateOptions, Person.class)
                .join().get()),

        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, listTypeRef).join().get()
                .subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, listTypeRef).join().get()
                .subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, listTypeRef).join().get()
                .subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, listTypeRef).join().get()
                .subList(begin, end)),

        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, personTypeRef).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, personTypeRef).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, personTypeRef).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, personTypeRef).join().get()),

        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, baseUpdateOptions, listTypeRef)
                .join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, baseUpdateOptions, listTypeRef)
                .join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, baseUpdateOptions, listTypeRef)
                .join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, baseUpdateOptions, listTypeRef)
                .join().get().subList(begin, end)),

        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsList, baseUpdateOptions, personTypeRef)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsList, baseUpdateOptions, personTypeRef)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsList, operationsAsClass, baseUpdateOptions, personTypeRef)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(keyAsClass, operationsAsClass, baseUpdateOptions, personTypeRef)
                .join().get()),

        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsList, operationsAsList)
                .join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsList, operationsAsClass)
                .join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsClass, operationsAsList)
                .join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsClass, operationsAsClass)
                .join().get().subList(begin, end)),

        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsList, operationsAsList,
                OPTIONS).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsList, operationsAsClass,
                OPTIONS).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsClass, operationsAsList,
                OPTIONS).join().get().subList(begin, end)),
        Arguments.of(
            prepareAction,
            resultAsList,
            (Supplier<Object>) () -> space.update(genericOptions, listTypeRef, keyAsClass, operationsAsClass,
                OPTIONS).join().get().subList(begin, end)),

        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsList, operationsAsList)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsList, operationsAsClass)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsClass, operationsAsList)
                .join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsClass, operationsAsClass)
                .join().get()),

        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsList, operationsAsList,
                OPTIONS).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsList, operationsAsClass,
                OPTIONS).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsClass, operationsAsList,
                OPTIONS).join().get()),
        Arguments.of(
            prepareAction,
            resultAsClass,
            (Supplier<Object>) () -> space.update(genericOptions, personTypeRef, keyAsClass, operationsAsClass,
                OPTIONS).join().get()));
  }

  @ParameterizedTest
  @MethodSource("dataForUpdateTest")
  public void testUpdate(Runnable prepareAction, Object result, Supplier<Object> callingMethod) {
    prepareAction.run();
    assertEquals(result, callingMethod.get());
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> selectOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            null,
            1000,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            null,
            Arrays.asList("id", "name"),
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "after",
            null,
            Arrays.asList(0, false, "name", 1000),
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withAfter(optionValue).build()),
        Arguments.of(
            "batch_size",
            null,
            100,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withBatchSize((Integer) optionValue).build()),
        Arguments.of(
            "bucket_id",
            null,
            100,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withBucketId((Integer) optionValue).build()),
        Arguments.of(
            "first",
            SelectOptions.DEFAULT_LIMIT,
            100,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withFirst((Integer) optionValue).build()),
        Arguments.of(
            "mode",
            Mode.WRITE.value(),
            Mode.READ,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withMode((Mode) optionValue).build()),
        Arguments.of(
            "yield_every",
            null,
            100,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withYieldEvery((Integer) optionValue).build()),
        Arguments.of(
            "fullscan",
            null,
            true,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().fullscan().build()),
        Arguments.of(
            "force_map_call",
            null,
            true,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().forceMapCall().build()),
        Arguments.of(
            "prefer_replica",
            null,
            true,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().preferReplica().build()),
        Arguments.of(
            "balance",
            null,
            true,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().balance().build()),
        Arguments.of(
            "fetch_latest_metadata",
            null,
            true,
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().fetchLatestMetadata().build()),
        Arguments.of(
            "vshard_router",
            null,
            "default",
            (Function<Object, SelectOptions>) (optionValue) ->
                SelectOptions.builder().withVshardRouter((String) optionValue).build())
    );
  }

  @ParameterizedTest
  @MethodSource("selectOptions")
  public void testSelectParameters(String optName, Object defaultOptSentValue, Object optionValue,
      Function<Object, SelectOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.select().join());
    List<?> result = client.eval("return crud_select_opts").join().get();
    assertEquals(defaultOptSentValue, ((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .select(Collections.emptyList(), SelectOptions.builder().build())
            .join()
    );
    result = client.eval("return crud_select_opts").join().get();
    assertEquals(defaultOptSentValue, ((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable = () -> {
      person.select(Collections.emptyList(), getOptions.apply(optionValue)).join();
    };
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable, "Incorrect usage of option " + optName);

    result = client.eval("return crud_select_opts").join().get();
    Object expectedResult = optionValue;
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_select_opts = {}").join().get();

    assertDoesNotThrow(() -> {
      person
          .select(baseOptions,
              listListTypeRef, Collections.emptyList(), getOptions.apply(optionValue).getOptions())
          .join().get();
    }, "Incorrect usage of option " + optName);

    result = client.eval("return crud_select_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @Test
  public void testSelectWrongParameters() {
    TarantoolCrudSpace person = client.space("person");
    Throwable ex = assertThrows(CompletionException.class, () -> {
      person
          .select(
              Collections.emptyList(),
              SelectOptions.builder().withOption("coolOption", "coolValue").build()
          )
          .join();
    });
    CrudException cause = (CrudException) ex.getCause();
    assertEquals(CompletionException.class, ex.getClass());
    assertEquals(CrudException.class, cause.getClass());
    assertTrue(cause.getMessage().matches("SelectError:.*" +
        "unexpected argument opts.coolOption to nil"));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> getOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            null,
            1000,
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            null,
            Arrays.asList("id", "name"),
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "bucket_id",
            null,
            100,
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().withBucketId((Integer) optionValue).build()),
        Arguments.of(
            "mode",
            Mode.WRITE.value(),
            Mode.READ,
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().withMode((Mode) optionValue).build()),
        Arguments.of(
            "prefer_replica",
            null,
            true,
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().preferReplica().build()),
        Arguments.of(
            "balance",
            null,
            true,
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().balance().build()),
        Arguments.of(
            "fetch_latest_metadata",
            null,
            true,
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().fetchLatestMetadata().build()),
        Arguments.of(
            "vshard_router",
            null,
            "default",
            (Function<Object, GetOptions>) (optionValue) ->
                GetOptions.builder().withVshardRouter((String) optionValue).build())
    );
  }

  @ParameterizedTest
  @MethodSource("getOptions")
  public void testGetParameters(String optName, Object defaultOptSentValue, Object optionValue,
      Function<Object, GetOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");
    List<?> key = Collections.singletonList(1);
    // without all
    assertDoesNotThrow(() ->
        person
            .get(key)
            .join()
    );
    List<?> result = client.eval("return crud_get_opts").join().get();
    assertEquals(defaultOptSentValue, ((HashMap<?, ?>) result.get(0)).get(optName));

    // without default
    assertDoesNotThrow(() ->
        person
            .get(key, GetOptions.builder().build())
            .join()
    );
    result = client.eval("return crud_get_opts").join().get();
    assertEquals(defaultOptSentValue, ((HashMap<?, ?>) result.get(0)).get(optName));

    // specified
    Executable executable = () -> person.get(key, getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_get_opts").join().get();
    Object expectedResult = optionValue;
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_get_opts = {}").join().get();

    // specified generic
    assertDoesNotThrow(() ->
        person
            .get(baseOptions, personTypeRef, key, getOptions.apply(optionValue).getOptions())
            .join()
    );
    result = client.eval("return crud_get_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> deleteOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, DeleteOptions>) (optionValue) ->
                DeleteOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            Arrays.asList("id", "name"),
            (Function<Object, DeleteOptions>) (optionValue) ->
                DeleteOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "bucket_id",
            100,
            (Function<Object, DeleteOptions>) (optionValue) ->
                DeleteOptions.builder().withBucketId((Integer) optionValue).build()),
        Arguments.of(
            "fetch_latest_metadata",
            true,
            (Function<Object, DeleteOptions>) (optionValue) ->
                DeleteOptions.builder().fetchLatestMetadata().build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, DeleteOptions>) (optionValue) ->
                DeleteOptions.builder().withVshardRouter((String) optionValue).build()),
        Arguments.of(
            "noreturn",
            true,
            (Function<Object, DeleteOptions>) (optionValue) ->
                DeleteOptions.builder().withNoReturn().build())
    );
  }

  @ParameterizedTest
  @MethodSource("deleteOptions")
  public void testDeleteParameters(String optName, Object optionValue,
      Function<Object, DeleteOptions> deleteOptions) {
    TarantoolCrudSpace person = client.space("person");
    List<Integer> key = Collections.singletonList(1);

    assertDoesNotThrow(() ->
        person
            .delete(key)
            .join()
    );
    List<?> result = client.eval("return crud_delete_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .delete(key, DeleteOptions.builder().build())
            .join()
    );
    result = client.eval("return crud_delete_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.delete(key, deleteOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_delete_opts").join().get();
    Object expectedResult = optionValue;
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_delete_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .delete(baseOptions, personTypeRef, key, deleteOptions.apply(optionValue).getOptions())
            .join()
    );
    result = client.eval("return crud_delete_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> updateParameters() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, UpdateOptions>) (optionValue) ->
                UpdateOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            Arrays.asList("id", "name"),
            (Function<Object, UpdateOptions>) (optionValue) ->
                UpdateOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "bucket_id",
            100,
            (Function<Object, UpdateOptions>) (optionValue) ->
                UpdateOptions.builder().withBucketId((Integer) optionValue).build()),
        Arguments.of(
            "fetch_latest_metadata",
            true,
            (Function<Object, UpdateOptions>) (optionValue) ->
                UpdateOptions.builder().fetchLatestMetadata().build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, UpdateOptions>) (optionValue) ->
                UpdateOptions.builder().withVshardRouter((String) optionValue).build()),
        Arguments.of(
            "noreturn",
            true,
            (Function<Object, UpdateOptions>) (optionValue) ->
                UpdateOptions.builder().withNoReturn().build())
    );
  }

  @ParameterizedTest
  @MethodSource("updateParameters")
  public void testUpdateParameters(String optName, Object optionValue,
      Function<Object, UpdateOptions> updateOptions) {
    TarantoolCrudSpace person = client.space("person");
    List<Integer> key = Collections.singletonList(1);
    List<List<?>> operations = Collections.singletonList(Arrays.asList(
        "=",
        "name",
        "DimaK"
    ));

    assertDoesNotThrow(() ->
        person
            .update(key, Collections.emptyList())
            .join()
    );
    List<?> result = client.eval("return crud_update_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .update(key, operations, UpdateOptions.builder().build())
            .join()
    );
    result = client.eval("return crud_update_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.update(key, operations, updateOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_update_opts").join().get();
    Object expectedResult = optionValue;
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_update_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .update(
                baseOptions,
                personTypeRef,
                key,
                operations,
                updateOptions.apply(optionValue).getOptions()
            )
            .join()
    );
    result = client.eval("return crud_update_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @ParameterizedTest
  @MethodSource("updateParameters")
  public void testUpsertParameters(String optName, Object optionValue,
      Function<Object, UpdateOptions> updateOptions) {
    TarantoolCrudSpace person = client.space("person");
    List<List<?>> operations = Collections.singletonList(Arrays.asList(
        "=",
        "name",
        "DimaK"
    ));

    assertDoesNotThrow(() ->
        person
            .upsert(personInstance.asList(), operations)
            .join()
    );
    List<?> result = client.eval("return crud_upsert_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .upsert(
                personInstance.asList(),
                operations,
                UpdateOptions.builder().build()
            )
            .join()
    );
    result = client.eval("return crud_upsert_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.upsert(personInstance.asList(), operations, updateOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_upsert_opts").join().get();
    Object expectedResult = optionValue;
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_upsert_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .upsert(
                baseOptions,
                personInstance.asList(),
                operations,
                updateOptions.apply(optionValue).getOptions()
            )
            .join()
    );
    result = client.eval("return crud_upsert_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> insertOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, InsertOptions>) (optionValue) ->
                InsertOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            Arrays.asList("id", "name"),
            (Function<Object, InsertOptions>) (optionValue) ->
                InsertOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "noreturn",
            true,
            (Function<Object, InsertOptions>) (optionValue) ->
                InsertOptions.builder().withNoReturn().build()),
        Arguments.of(
            "bucket_id",
            123,
            (Function<Object, InsertOptions>) (optionValue) ->
                InsertOptions.builder().withBucketId((Integer) optionValue).build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, InsertOptions>) (optionValue) ->
                InsertOptions.builder().withVshardRouter((String) optionValue).build()),
        Arguments.of(
            "fetch_latest_metadata",
            true,
            (Function<Object, InsertOptions>) (optionValue) ->
                InsertOptions.builder().fetchLatestMetadata().build())
    );
  }

  @ParameterizedTest
  @MethodSource("insertOptions")
  public void testInsertParameters(String optName, Object optionValue, Function<Object, InsertOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() ->
        person
            .insert(Arrays.asList(1, true, "Roman"))
            .join().get()
    );
    List<?> result = client.eval("return crud_insert_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .insert(
                Arrays.asList(2, true, "Roman"),
                InsertOptions.builder().build()
            )
            .join().get()
    );
    result = client.eval("return crud_insert_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.insert(Arrays.asList(3, true, "Roman"), getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable, "Incorrect usage of " + optName);
    result = client.eval("return crud_insert_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_insert_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .insert(
                baseOptions,
                listTypeRef,
                Arrays.asList(4, true, "Roman"),
                getOptions.apply(optionValue).getOptions()
            )
            .join()
    );
    result = client.eval("return crud_insert_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @ParameterizedTest
  @MethodSource("insertOptions")
  public void testReplaceParameters(String optName, Object optionValue, Function<Object, InsertOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.replace(personInstance.asList()).join().get());
    List<?> result = client.eval("return crud_replace_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .replace(
                personInstance.asList(),
                InsertOptions.builder().build()
            )
            .join().get()
    );
    result = client.eval("return crud_replace_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.replace(personInstance.asList(), getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_replace_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_replace_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .replace(
                baseOptions,
                listTypeRef,
                personInstance.asList(),
                getOptions.apply(optionValue).getOptions()
            )
            .join()
    );
    result = client.eval("return crud_replace_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> insertManyOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            Arrays.asList("id", "name"),
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "noreturn",
            true,
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().withNoReturn().build()),
        Arguments.of(
            "stop_on_error",
            true,
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().stopOnError().build()),
        Arguments.of(
            "rollback_on_error",
            true,
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().rollbackOnError().build()),
        Arguments.of(
            "vshard_router",
            Collections.singletonList("r2"),
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().withVshardRouter((List<String>) optionValue).build()),
        Arguments.of(
            "fetch_latest_metadata",
            true,
            (Function<Object, InsertManyOptions>) (optionValue) ->
                InsertManyOptions.builder().fetchLatestMetadata().build())
    );
  }

  @ParameterizedTest
  @MethodSource("insertManyOptions")
  public void testInsertManyParameters(String optName, Object optionValue,
      Function<Object, InsertManyOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");
    List<?> result;

    assertDoesNotThrow(() ->
        person
            .insertMany(
                Arrays.asList(
                    Arrays.asList(1, true, "Roman"),
                    Arrays.asList(2, true, "Roman"),
                    Arrays.asList(3, true, "Roman")
                )
            )
            .join()
    );
    result = client.eval("return crud_insert_many_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .insertMany(
                Arrays.asList(
                    Arrays.asList(4, true, "Ivan"),
                    Arrays.asList(5, true, "Artyom"),
                    Arrays.asList(6, true, "Nikolay")
                ),
                InsertManyOptions.builder().build()
            )
            .join()
    );
    result = client.eval("return crud_insert_many_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .insertMany(
                Arrays.asList(
                    Arrays.asList(7, true, "Igor"),
                    Arrays.asList(8, true, "Andrey"),
                    Arrays.asList(9, true, "Aleksandr")
                ),
                getOptions.apply(optionValue)
            )
            .join()
    );

    result = client.eval("return crud_insert_many_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_insert_many_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .insertMany(
                baseOptions, listListTypeRef,
                Arrays.asList(
                    Arrays.asList(10, true, "Foma"),
                    Arrays.asList(12, true, "Mikhail"),
                    Arrays.asList(13, true, "Aleksey")
                ),
                getOptions.apply(optionValue).getOptions()
            )
            .join().get()
    );
    result = client.eval("return crud_insert_many_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @ParameterizedTest
  @MethodSource("insertManyOptions")
  public void testReplaceManyParameters(String optName, Object optionValue,
      Function<Object, InsertManyOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() ->
        person
            .replaceMany(
                Arrays.asList(
                    Arrays.asList(1, true, "Roman"),
                    Arrays.asList(2, true, "Roman"),
                    Arrays.asList(3, true, "Roman")
                )
            )
            .join()
    );
    List<?> result = client.eval("return crud_replace_many_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .replaceMany(
                Arrays.asList(
                    Arrays.asList(4, true, "Ivan"),
                    Arrays.asList(5, true, "Artyom"),
                    Arrays.asList(6, true, "Nikolay")
                ),
                InsertManyOptions.builder().build()
            )
            .join()
    );
    result = client.eval("return crud_replace_many_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .replaceMany(
                Arrays.asList(
                    Arrays.asList(7, true, "Igor"),
                    Arrays.asList(8, true, "Andrey"),
                    Arrays.asList(9, true, "Aleksandr")
                ),
                getOptions.apply(optionValue)
            )
            .join()
    );
    result = client.eval("return crud_replace_many_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_replace_many_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .replaceMany(
                baseOptions, listListTypeRef,
                Arrays.asList(
                    Arrays.asList(10, true, "Foma"),
                    Arrays.asList(12, true, "Mikhail"),
                    Arrays.asList(13, true, "Aleksey")
                ),
                getOptions.apply(optionValue).getOptions()
            )
            .join().get()
    );
    result = client.eval("return crud_replace_many_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> upsertManyOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            Arrays.asList("id", "name"),
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "noreturn",
            true,
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().withNoReturn().build()),
        Arguments.of(
            "stop_on_error",
            true,
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().stopOnError().build()),
        Arguments.of(
            "rollback_on_error",
            true,
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().rollbackOnError().build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().withVshardRouter((String) optionValue).build()),
        Arguments.of(
            "fetch_latest_metadata",
            true,
            (Function<Object, UpsertManyOptions>) (optionValue) ->
                UpsertManyOptions.builder().fetchLatestMetadata().build())
    );
  }

  @ParameterizedTest
  @MethodSource("upsertManyOptions")
  public void testUpsertManyParameters(String optName, Object optionValue,
      Function<Object, UpsertManyOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");
    List<List<String>> operation = Collections.singletonList(
        Arrays.asList(
            "=",
            "name",
            "DimaK"
        )
    );
    List<List<List<?>>> tuples = Arrays.asList(
        Arrays.asList(personInstance.asList(), operation),
        Arrays.asList(personInstance.toBuilder().id(2).build().asList(), operation)
    );

    assertDoesNotThrow(() ->
        person
            .upsertMany(tuples)
            .join()
    );
    List<?> result = client.eval("return crud_upsert_many_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .upsertMany(
                tuples,
                UpsertManyOptions.builder().build()
            )
    );
    result = client.eval("return crud_upsert_many_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() -> person.upsertMany(tuples, getOptions.apply(optionValue)).join());
    result = client.eval("return crud_upsert_many_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_upsert_many_opts = {}").join().get();

    assertDoesNotThrow(
        () -> person.upsertMany(baseOptions, tuples, getOptions.apply(optionValue).getOptions()).join());
    result = client.eval("return crud_upsert_many_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  public static Stream<Arguments> truncateOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, TruncateOptions>) (optionValue) ->
                TruncateOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, TruncateOptions>) (optionValue) ->
                TruncateOptions.builder().withVshardRouter((String) optionValue).build())
    );
  }

  @ParameterizedTest
  @MethodSource("truncateOptions")
  public void testTruncateParameters(String optName, Object optionValue,
      Function<Object, TruncateOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.truncate().join());
    List<?> result = client.eval("return crud_truncate_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() -> person.truncate(TruncateOptions.builder().build()).join());
    result = client.eval("return crud_truncate_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable = () -> person.truncate(getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_truncate_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_truncate_opts = {}").join().get();

    assertDoesNotThrow(() -> person.truncate(baseOptions, getOptions.apply(optionValue).getOptions()).join());
    result = client.eval("return crud_truncate_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  public static Stream<Arguments> lenOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, LenOptions>) (optionValue) ->
                LenOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, LenOptions>) (optionValue) ->
                LenOptions.builder().withVshardRouter((String) optionValue).build())
    );
  }

  @ParameterizedTest
  @MethodSource("lenOptions")
  public void testLenParameters(String optName, Object optionValue, Function<Object, LenOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.len().join());
    List<?> result = client.eval("return crud_len_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() -> person.len(LenOptions.builder().build()).join());
    result = client.eval("return crud_len_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable = () -> person.len(getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_len_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_len_opts = {}").join().get();

    assertDoesNotThrow(() -> person.len(baseOptions, getOptions.apply(optionValue).getOptions()).join());
    result = client.eval("return crud_len_opts").join().get();
    assertEquals(optionValue, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  public static Stream<Arguments> countOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            null,
            1000,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "bucket_id",
            null,
            100,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().withBucketId((Integer) optionValue).build()),
        Arguments.of(
            "mode",
            Mode.WRITE.value(),
            Mode.READ,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().withMode((Mode) optionValue).build()),
        Arguments.of(
            "yield_every",
            null,
            100,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().withYieldEvery((Integer) optionValue).build()),
        Arguments.of(
            "fullscan",
            null,
            true,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().fullscan().build()),
        Arguments.of(
            "force_map_call",
            null,
            true,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().forceMapCall().build()),
        Arguments.of(
            "prefer_replica",
            null,
            true,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().preferReplica().build()),
        Arguments.of(
            "balance",
            null,
            true,
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().balance().build()),
        Arguments.of(
            "vshard_router",
            null,
            "default",
            (Function<Object, CountOptions>) (optionValue) ->
                CountOptions.builder().withVshardRouter((String) optionValue).build())
    );
  }

  @ParameterizedTest
  @MethodSource("countOptions")
  public void testCountParameters(String optName, Object defaultOptSentValue, Object optionValue,
      Function<Object, CountOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.count().join());
    List<?> result = client.eval("return crud_count_opts").join().get();
    assertEquals(defaultOptSentValue, ((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() ->
        person
            .count(Collections.emptyList(), CountOptions.builder().build())
            .join()
    );
    result = client.eval("return crud_count_opts").join().get();
    assertEquals(defaultOptSentValue, ((HashMap<?, ?>) result.get(0)).get(optName));

    Executable count =
        () -> person.count(Collections.emptyList(), getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, count);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(count);
    result = client.eval("return crud_count_opts").join().get();
    Object expectedResult = optionValue;
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_count_opts = {}").join().get();

    assertDoesNotThrow(() ->
        person
            .count(
                baseOptions,
                personTypeRef,
                Collections.emptyList(),
                getOptions.apply(optionValue).getOptions()
            )
            .join()
    );
    result = client.eval("return crud_count_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @SuppressWarnings("unchecked")
  public static Stream<Arguments> minMaxOptions() {
    return Stream.of(
        Arguments.of(
            "timeout",
            1000,
            (Function<Object, MinMaxOptions>) (optionValue) ->
                MinMaxOptions.builder().withCrudTimeout((Integer) optionValue).build()),
        Arguments.of(
            "fields",
            Arrays.asList("id", "name"),
            (Function<Object, MinMaxOptions>) (optionValue) ->
                MinMaxOptions.builder().withFields((List<String>) optionValue).build()),
        Arguments.of(
            "vshard_router",
            "default",
            (Function<Object, MinMaxOptions>) (optionValue) ->
                MinMaxOptions.builder().withVshardRouter((String) optionValue).build())
    );
  }

  @ParameterizedTest
  @MethodSource("minMaxOptions")
  public void testMinParameters(String optName, Object optionValue, Function<Object, MinMaxOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.min("pk").join());
    List<?> result = client.eval("return crud_min_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() -> person.min("pk", MinMaxOptions.builder().build()).join());
    result = client.eval("return crud_min_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.min("pk", getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_min_opts").join().get();
    Object expectedResult = optionValue;
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_min_opts = {}").join().get();

    assertDoesNotThrow(
        () -> person.min(baseOptions, personTypeRef, "pk", getOptions.apply(optionValue).getOptions()).join());
    result = client.eval("return crud_min_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @ParameterizedTest
  @MethodSource("minMaxOptions")
  public void testMaxParameters(String optName, Object optionValue, Function<Object, MinMaxOptions> getOptions) {
    TarantoolCrudSpace person = client.space("person");

    assertDoesNotThrow(() -> person.max("pk").join());
    List<?> result = client.eval("return crud_max_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    assertDoesNotThrow(() -> person.max("pk", MinMaxOptions.builder().build()).join());
    result = client.eval("return crud_max_opts").join().get();
    assertNull(((HashMap<?, ?>) result.get(0)).get(optName));

    Executable executable =
        () -> person.max("pk", getOptions.apply(optionValue)).join();
    if (optName.equals("vshard_router") && !isCartridgeAvailable()) {
      CompletionException ex = assertThrows(CompletionException.class, executable);
      Throwable cause = ex.getCause();
      assertInstanceOf(CrudException.class, cause);
      assertTrue(cause.getMessage().contains("Vshard groups are supported only in Tarantool Cartridge"));
      return;
    }
    assertDoesNotThrow(executable);
    result = client.eval("return crud_max_opts").join().get();
    Object expectedResult = optionValue;
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
    client.eval("crud_max_opts = {}").join().get();

    assertDoesNotThrow(
        () -> person.max(baseOptions, personTypeRef, "pk", getOptions.apply(optionValue).getOptions()).join());
    result = client.eval("return crud_max_opts").join().get();
    if (optName.equals("mode")) {
      expectedResult = ((Mode) optionValue).value();
    }
    assertEquals(expectedResult, ((HashMap<?, ?>) result.get(0)).get(optName));
  }

  @Test
  void testClientType() {
    assertEquals(ClientType.CRUD, client.getType());
  }

  @Test
  void testFirstDefaultLimit() {
    // prepare
    final TarantoolCrudSpace space = client.space("person");
    final List<Person> persons = new ArrayList<>();

    insertPersons(persons, space);

    // test
    List<Tuple<Person>> selectedPersons = space.select(Collections.emptyList(), SelectOptions.builder().build(),
        Person.class).join();

    assertEquals(SelectOptions.DEFAULT_LIMIT, selectedPersons.size());
    selectedPersons.sort(Comparator.comparing(person -> person.get().getId()));
    assertEquals(persons.subList(0, SelectOptions.DEFAULT_LIMIT), unpackT(selectedPersons));
  }

  @Test
  void testAfterWithDefaultLimit() {
    // prepare
    final TarantoolCrudSpace space = client.space("person");
    final List<Person> persons = new ArrayList<>();

    insertPersons(persons, space);

    final int AFTER_POSITION = 9;
    final Person AFTER_TUPLE = persons.get(AFTER_POSITION);

    final SelectOptions options = SelectOptions.builder()
        .withAfter(AFTER_TUPLE)
        .build();

    List<Tuple<Person>> selectedPerson = space.select(Collections.emptyList(), options, Person.class).join();
    selectedPerson.sort(Comparator.comparing(person -> person.get().getId()));

    final int END_SELECT_POSITION = AFTER_POSITION + SelectOptions.DEFAULT_LIMIT + 1;
    assertEquals(persons.subList(AFTER_POSITION + 1, END_SELECT_POSITION), unpackT(selectedPerson));
  }

  private static void insertPersons(List<Person> persons, TarantoolCrudSpace space) {
    for (int i = 0; i < PERSON_COUNT; i++) {
      Person person = new Person(i, true, String.valueOf(i));
      persons.add(person);
      assertEquals(person, space.insert(person, Person.class).join().get());
    }
  }

  @Test
  void testAfterWithCustomLimit() {
    // prepare
    final TarantoolCrudSpace space = client.space("person");
    final List<Person> persons = new ArrayList<>();

    insertPersons(persons, space);

    final int AFTER_POSITION = 9;
    final Person AFTER_TUPLE = persons.get(AFTER_POSITION);
    final int CUSTOM_LIMIT = 2 * SelectOptions.DEFAULT_LIMIT;

    final SelectOptions options = SelectOptions.builder()
        .withAfter(AFTER_TUPLE)
        .withFirst(CUSTOM_LIMIT)
        .build();

    List<Tuple<Person>> selectedPerson = space.select(Collections.emptyList(), options, Person.class).join();
    selectedPerson.sort(Comparator.comparing(person -> person.get().getId()));

    final int END_SELECT_POSITION = AFTER_POSITION + 1 + CUSTOM_LIMIT;
    assertEquals(persons.subList(AFTER_POSITION + 1, END_SELECT_POSITION), unpackT(selectedPerson));
  }

  @Test
  void testBeforeWithDefaultLimit() {
    // prepare
    final TarantoolCrudSpace space = client.space("person");
    final List<Person> persons = new ArrayList<>();

    insertPersons(persons, space);

    final int BEFORE_POSITION = PERSON_COUNT - 1;
    final Person BEFORE_TUPLE = persons.get(BEFORE_POSITION);

    final SelectOptions options = SelectOptions.builder()
        .withFirst(-1 * SelectOptions.DEFAULT_LIMIT)
        .withAfter(BEFORE_TUPLE)
        .build();

    List<Tuple<Person>> selectedPerson = space.select(Collections.emptyList(), options, Person.class).join();
    selectedPerson.sort(Comparator.comparing(person -> person.get().getId()));

    final int START_SELECT_INDEX = BEFORE_POSITION - SelectOptions.DEFAULT_LIMIT;
    assertEquals(persons.subList(START_SELECT_INDEX, BEFORE_POSITION), unpackT(selectedPerson));
  }

  @Test
  void testBeforeWithCustomLimit() {
    // prepare
    final TarantoolCrudSpace space = client.space("person");
    final List<Person> persons = new ArrayList<>();

    insertPersons(persons, space);

    // test
    final int BEFORE_POSITION = PERSON_COUNT - 1;
    final Person BEFORE_TUPLE = persons.get(BEFORE_POSITION);
    final int CUSTOM_LIMIT = -2 * SelectOptions.DEFAULT_LIMIT;

    final SelectOptions options = SelectOptions.builder()
        .withAfter(BEFORE_TUPLE)
        .withFirst(CUSTOM_LIMIT)
        .build();

    List<Tuple<Person>> selectedPerson = space.select(Collections.emptyList(), options, Person.class).join();
    selectedPerson.sort(Comparator.comparing(person -> person.get().getId()));

    assertEquals(persons.subList(BEFORE_POSITION + CUSTOM_LIMIT, BEFORE_POSITION), unpackT(selectedPerson));
  }

  protected static Stream<Arguments> dataForTestNestedPerson() {

    final int ITERATION_COUNT = 3;
    final Map<String, Object> BUYS = new HashMap<String, Object>() {{
      put("fruit1", "fruit1");
    }};

    List<Person> husbands = Arrays.asList(
        new Person(0, true, "Sonya"),
        new Person(1, true, "Anna"),
        new Person(2, true, "Galya"));

    List<Child> children = Arrays.asList(
        new Child(0, "Kostya"),
        new Child(1, "Kolya"),
        new Child(2, "Artem"));

    List<NestedPerson> persons = new ArrayList<>();

    for (int i = 0; i < ITERATION_COUNT; i++) {
      persons.add(
          new NestedPerson(
              i,
              String.valueOf(i),
              Collections.singletonList(husbands.get(i)),
              BUYS,
              husbands.get(i),
              children.get(i)));
    }

    return Stream.of(
        Arguments.of(
            Collections.singletonList(Condition.create(EQ, "husband[3]", "Anna")),
            Collections.singletonList(persons.get(1)),
            persons),
        Arguments.of(
            Collections.singletonList(Condition.create(EQ, "friends[1][3]", "Anna")),
            Collections.singletonList(persons.get(1)),
            persons),
        Arguments.of(
            Collections.singletonList(Condition.create(EQ, "child.name", "Kolya")),
            Collections.singletonList(persons.get(1)),
            persons),
        Arguments.of(
            Collections.singletonList(Condition.create(EQ, "buys.fruit1", "fruit1")),
            persons,
            persons));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestNestedPerson")
  void testNestedPerson(List<Condition> conditions, List<NestedPerson> expectedPersons, List<NestedPerson> persons) {
    TarantoolCrudSpace space = client.space("nested_person");

    space.insertMany(persons, NestedPerson.class).join();

    List<NestedPerson> selectedPersons = space.select(conditions, NestedPerson.class).join()
        .stream().map(Tuple::get).sorted(Comparator.comparing(NestedPerson::getId)).collect(Collectors.toList());

    assertEquals(expectedPersons, selectedPersons);
  }

  private static <T> List<T> mapBatchToType(CrudBatchResponse<List<Tuple<T>>> batchResponse,
      Comparator<T> tupleComparator) {
    return batchResponse.getRows().stream().map(Tuple::get).sorted(tupleComparator).collect(Collectors.toList());
  }
}
