/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.integration.crud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.keyvalue.core.UncategorizedKeyValueException;

import static io.tarantool.spring.data.ProxyTarantoolCrudKeyValueAdapter.POTENTIAL_PERFORMANCE_ISSUES_EXCEPTION_MESSAGE;
import static io.tarantool.spring.data.ProxyTarantoolQueryEngine.unwrapTuples;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.COMPLEX_PERSON_SPACE;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.PERSONS_COUNT;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.PERSON_SPACE;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.generateAndInsertComplexPersons;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.generateAndInsertPersons;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.generateComplexPersons;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.generatePersons;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.mapping.crud.CrudException;
import io.tarantool.spring.data34.core.TarantoolTemplate;
import io.tarantool.spring.data34.utils.entity.ComplexPerson;
import io.tarantool.spring.data34.utils.entity.ComplexPersonWithIncorrectPK;
import io.tarantool.spring.data34.utils.entity.ComplexPersonWithJsonFormatVariantPK;
import io.tarantool.spring.data34.utils.entity.CompositePersonKey;
import io.tarantool.spring.data34.utils.entity.CompositePersonKeyWithJsonFormat;
import io.tarantool.spring.data34.utils.entity.Person;

abstract class GenericTarantoolTemplateTest extends CrudConfigurations {

  @Autowired
  protected TarantoolTemplate tarantoolTemplate;

  @Test
  void testGetAllOf() {
    final Throwable throwable =
        assertThrows(UncategorizedKeyValueException.class, () -> tarantoolTemplate.findAll(Person.class)).getCause();

    assertEquals(UnsupportedOperationException.class, throwable.getClass());
    assertEquals(POTENTIAL_PERFORMANCE_ISSUES_EXCEPTION_MESSAGE, throwable.getMessage());
  }

  static Stream<Arguments> dataForInsert() {

    Person simpleKeyPerson = generatePersons(1).get(0);
    ComplexPerson compositeKeyPerson = generateComplexPersons(1).get(0);

    Function<TarantoolCrudClient, ?> selectActionForSimpleKeyPerson =
        (client) -> unwrapTuples(client.space(PERSON_SPACE).select(Collections.emptyList(), Person.class).join());

    Function<TarantoolCrudClient, List<?>> selectActionForComplexKeyPerson =
        (client) -> unwrapTuples(client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList(),
            ComplexPerson.class).join());

    return Stream.of(
        Arguments.of(
            simpleKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.insert(simpleKeyPerson),
            selectActionForSimpleKeyPerson),
        Arguments.of(
            compositeKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.insert(compositeKeyPerson),
            selectActionForComplexKeyPerson));
  }

  @ParameterizedTest
  @MethodSource("dataForInsert")
  void testKVTemplateInsert(Object result, Function<KeyValueTemplate, Object> callingMethod,
      Function<TarantoolCrudClient, List<?>> selectAction) {
    final int SIZE = 1;
    assertEquals(result, callingMethod.apply(tarantoolTemplate));

    List<?> selectResult = selectAction.apply(client);
    assertEquals(SIZE, selectResult.size());
    assertEquals(result, selectResult.get(0));
  }

  static Stream<Arguments> dataForInsertById() {

    Person simpleKeyPerson = generatePersons(1).get(0);
    ComplexPerson compositeKeyPerson = generateComplexPersons(1).get(0);

    Integer simpleKey = simpleKeyPerson.getId();
    CompositePersonKey compositeKey = new CompositePersonKey(compositeKeyPerson.getId(),
        compositeKeyPerson.getSecondId());

    Function<TarantoolCrudClient, ?> selectActionForSimpleKeyPerson =
        (client) -> unwrapTuples(client.space(PERSON_SPACE).select(Collections.emptyList(), Person.class).join());

    Function<TarantoolCrudClient, List<?>> selectActionForComplexKeyPerson =
        (client) -> unwrapTuples(client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList(),
            ComplexPerson.class).join());

    return Stream.of(
        Arguments.of(
            simpleKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.insert(simpleKey, simpleKeyPerson),
            selectActionForSimpleKeyPerson),
        Arguments.of(
            compositeKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.insert(compositeKey, compositeKeyPerson),
            selectActionForComplexKeyPerson));
  }

  @ParameterizedTest
  @MethodSource("dataForInsertById")
  void testKVTemplateInsertById(Object result, Function<KeyValueTemplate, Object> callingMethod,
      Function<TarantoolCrudClient, List<?>> selectAction) {
    final int SIZE = 1;
    assertEquals(result, callingMethod.apply(tarantoolTemplate));

    List<?> selectResult = selectAction.apply(client);
    assertEquals(SIZE, selectResult.size());
    assertEquals(result, selectResult.get(0));
  }

  static Stream<Arguments> dataForUpdate() {

    Person simpleKeyPerson = generatePersons(1).get(0);
    ComplexPerson compositeKeyPerson = generateComplexPersons(1).get(0);

    Function<TarantoolCrudClient, ?> selectActionForSimpleKeyPerson =
        (client) -> unwrapTuples(client.space(PERSON_SPACE).select(Collections.emptyList(), Person.class).join());

    Function<TarantoolCrudClient, List<?>> selectActionForComplexKeyPerson =
        (client) -> unwrapTuples(
            client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList(), ComplexPerson.class).join());

    Consumer<TarantoolCrudClient> prepareActionForSimpleKeyPerson = (client) -> {
      Person insertedPerson = client.space(PERSON_SPACE).insert(simpleKeyPerson, Person.class).join().get();
      assertEquals(simpleKeyPerson, insertedPerson);
    };

    Consumer<TarantoolCrudClient> prepareActionForCompositeKeyPerson = (client) -> {
      ComplexPerson insertedPerson =
          client.space(COMPLEX_PERSON_SPACE).insert(compositeKeyPerson, ComplexPerson.class).join().get();
      assertEquals(compositeKeyPerson, insertedPerson);
    };

    return Stream.of(
        Arguments.of(
            prepareActionForSimpleKeyPerson,
            simpleKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.update(simpleKeyPerson),
            selectActionForSimpleKeyPerson),
        Arguments.of(
            prepareActionForCompositeKeyPerson,
            compositeKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.update(compositeKeyPerson),
            selectActionForComplexKeyPerson));
  }

  @ParameterizedTest
  @MethodSource("dataForUpdate")
  void testKVTemplateUpdate(Consumer<TarantoolCrudClient> prepareAction, Object result,
      Function<KeyValueTemplate, Object> callingMethod,
      Function<TarantoolCrudClient, List<?>> selectAction) {
    prepareAction.accept(client);

    final int SIZE = 1;
    assertEquals(result, callingMethod.apply(tarantoolTemplate));

    List<?> selectResult = selectAction.apply(client);
    assertEquals(SIZE, selectResult.size());
    assertEquals(result, selectResult.get(0));
  }

  static Stream<Arguments> dataForUpdateById() {

    Person simpleKeyPerson = generatePersons(1).get(0);
    ComplexPerson compositeKeyPerson = generateComplexPersons(1).get(0);

    Integer simpleKey = simpleKeyPerson.getId();
    CompositePersonKey compositeKey = new CompositePersonKey(compositeKeyPerson.getId(),
        compositeKeyPerson.getSecondId());

    Function<TarantoolCrudClient, ?> selectActionForSimpleKeyPerson =
        (client) -> unwrapTuples(client.space(PERSON_SPACE).select(Collections.emptyList(), Person.class).join());

    Function<TarantoolCrudClient, List<?>> selectActionForComplexKeyPerson =
        (client) -> unwrapTuples(client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList(),
            ComplexPerson.class).join());

    Consumer<TarantoolCrudClient> prepareActionForSimpleKeyPerson = (client) -> {
      Person insertedPerson = client.space(PERSON_SPACE).insert(simpleKeyPerson, Person.class).join().get();
      assertEquals(simpleKeyPerson, insertedPerson);
    };

    Consumer<TarantoolCrudClient> prepareActionForCompositeKeyPerson = (client) -> {
      ComplexPerson insertedPerson =
          client.space(COMPLEX_PERSON_SPACE).insert(compositeKeyPerson, ComplexPerson.class).join().get();
      assertEquals(compositeKeyPerson, insertedPerson);
    };

    return Stream.of(
        Arguments.of(
            prepareActionForSimpleKeyPerson,
            simpleKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.update(simpleKey, simpleKeyPerson),
            selectActionForSimpleKeyPerson),
        Arguments.of(
            prepareActionForCompositeKeyPerson,
            compositeKeyPerson,
            (Function<KeyValueTemplate, Object>) (kv) -> kv.update(compositeKey, compositeKeyPerson),
            selectActionForComplexKeyPerson));
  }

  @ParameterizedTest
  @MethodSource("dataForUpdateById")
  void testKVTemplateUpdateById(Consumer<TarantoolCrudClient> prepareAction, Object result,
      Function<KeyValueTemplate, Object> callingMethod,
      Function<TarantoolCrudClient, List<?>> selectAction) {
    prepareAction.accept(client);

    final int SIZE = 1;
    assertEquals(result, callingMethod.apply(tarantoolTemplate));

    List<?> selectResult = selectAction.apply(client);
    assertEquals(SIZE, selectResult.size());
    assertEquals(result, selectResult.get(0));
  }

  static Stream<Arguments> dataForDelete() {

    Runnable simpleKeyPrepareAction = () -> generatePersons(PERSONS_COUNT);
    Runnable compositeKeyPrepareAction = () -> generateComplexPersons(PERSONS_COUNT);

    Consumer<TarantoolCrudClient> simpleKeyFinalAction =
        (client) -> assertTrue(client.space(PERSON_SPACE).select(Collections.emptyList()).join().isEmpty());

    Consumer<TarantoolCrudClient> compositeKeyFinalAction =
        (client) -> assertTrue(client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList()).join().isEmpty());

    return Stream.of(
        Arguments.of(
            simpleKeyPrepareAction,
            (Consumer<KeyValueTemplate>) (kv) -> kv.delete(Person.class),
            simpleKeyFinalAction),
        Arguments.of(
            compositeKeyPrepareAction,
            (Consumer<KeyValueTemplate>) (kv) -> kv.delete(ComplexPerson.class),
            compositeKeyFinalAction));
  }

  @ParameterizedTest
  @MethodSource("dataForDelete")
  void testKVTemplateDelete(Runnable prepareAction, Consumer<KeyValueTemplate> callingMethod,
      Consumer<TarantoolCrudClient> finalAction) {
    prepareAction.run();
    callingMethod.accept(tarantoolTemplate);
    finalAction.accept(client);
  }

  static Stream<Arguments> dataForDeleteEntities() {

    Person simpleKeyPerson = new Person(0, true, "0");
    ComplexPerson compositeKeyPerson = new ComplexPerson(0, UUID.randomUUID(), true, "0");

    Consumer<TarantoolCrudClient> simpleKeyPrepareAction = (client) -> {
      Person insertedPerson = client.space(PERSON_SPACE).insert(simpleKeyPerson, Person.class).join().get();
      assertEquals(simpleKeyPerson, insertedPerson);
    };

    Consumer<TarantoolCrudClient> compositeKeyPrepareAction = (client) -> {
      ComplexPerson insertedPerson =
          client.space(COMPLEX_PERSON_SPACE).insert(compositeKeyPerson, ComplexPerson.class).join().get();
      assertEquals(compositeKeyPerson, insertedPerson);
    };

    Consumer<TarantoolCrudClient> simpleKeyFinalAction =
        (client) -> assertTrue(client.space(PERSON_SPACE).select(Collections.emptyList()).join().isEmpty());

    Consumer<TarantoolCrudClient> compositeKeyFinalAction =
        (client) -> assertTrue(client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList()).join().isEmpty());

    return Stream.of(
        Arguments.of(
            simpleKeyPrepareAction,
            (Consumer<KeyValueTemplate>) (kv) -> kv.delete(simpleKeyPerson),
            simpleKeyFinalAction),
        Arguments.of(
            compositeKeyPrepareAction,
            (Consumer<KeyValueTemplate>) (kv) -> kv.delete(compositeKeyPerson),
            compositeKeyFinalAction));
  }

  @ParameterizedTest
  @MethodSource("dataForDeleteEntities")
  void testKVTemplateDeleteEntities(Consumer<TarantoolCrudClient> prepareAction,
      Consumer<KeyValueTemplate> callingMethod,
      Consumer<TarantoolCrudClient> finalAction) {
    prepareAction.accept(client);
    callingMethod.accept(tarantoolTemplate);
    finalAction.accept(client);
  }

  static Stream<Arguments> dataForDeleteEntitiesById() {

    Person simpleKeyPerson = new Person(0, true, "0");
    ComplexPerson compositeKeyPerson = new ComplexPerson(0, UUID.randomUUID(), true, "0");

    Integer simpleKey = simpleKeyPerson.getId();
    CompositePersonKey compositeKey = new CompositePersonKey(compositeKeyPerson.getId(),
        compositeKeyPerson.getSecondId());

    Consumer<TarantoolCrudClient> simpleKeyPrepareAction = (client) -> {
      Person insertedPerson = client.space(PERSON_SPACE).insert(simpleKeyPerson, Person.class).join().get();
      assertEquals(simpleKeyPerson, insertedPerson);
    };

    Consumer<TarantoolCrudClient> compositeKeyPrepareAction = (client) -> {
      ComplexPerson insertedPerson =
          client.space(COMPLEX_PERSON_SPACE).insert(compositeKeyPerson, ComplexPerson.class).join().get();
      assertEquals(compositeKeyPerson, insertedPerson);
    };

    Consumer<TarantoolCrudClient> simpleKeyFinalAction =
        (client) -> assertTrue(client.space(PERSON_SPACE).select(Collections.emptyList()).join().isEmpty());

    Consumer<TarantoolCrudClient> compositeKeyFinalAction =
        (client) -> assertTrue(client.space(COMPLEX_PERSON_SPACE).select(Collections.emptyList()).join().isEmpty());

    return Stream.of(
        Arguments.of(
            simpleKeyPrepareAction,
            (Consumer<KeyValueTemplate>) (kv) -> kv.delete(simpleKey, Person.class),
            simpleKeyFinalAction),
        Arguments.of(
            compositeKeyPrepareAction,
            (Consumer<KeyValueTemplate>) (kv) -> kv.delete(compositeKey, ComplexPerson.class),
            compositeKeyFinalAction));
  }

  @ParameterizedTest
  @MethodSource("dataForDeleteEntitiesById")
  void testKVTemplateDeleteEntitiesById(Consumer<TarantoolCrudClient> prepareAction,
      Consumer<KeyValueTemplate> callingMethod,
      Consumer<TarantoolCrudClient> finalAction) {
    prepareAction.accept(client);
    callingMethod.accept(tarantoolTemplate);
    finalAction.accept(client);
  }

  static Stream<Arguments> dataForCount() {

    Consumer<TarantoolCrudClient> simpleKeyPrepareAction =
        (client) -> generateAndInsertPersons(PERSONS_COUNT, client);

    Consumer<TarantoolCrudClient> compositeKeyPrepareAction =
        (client) -> generateAndInsertComplexPersons(PERSONS_COUNT, client);

    return Stream.of(
        Arguments.of(
            PERSONS_COUNT,
            simpleKeyPrepareAction,
            (Function<KeyValueTemplate, Long>) (kv) -> kv.count(Person.class)),
        Arguments.of(
            PERSONS_COUNT,
            compositeKeyPrepareAction,
            (Function<KeyValueTemplate, Long>) (kv) -> kv.count(ComplexPerson.class)));
  }

  @ParameterizedTest
  @MethodSource("dataForCount")
  void testCount(long expectedCount,
      Consumer<TarantoolCrudClient> prepareAction,
      Function<KeyValueTemplate, Long> callingMethod) {
    prepareAction.accept(client);
    assertEquals(expectedCount, callingMethod.apply(tarantoolTemplate));
  }

  @Test
  void testCompositeKeyWithJsonFormat() {
    ComplexPersonWithJsonFormatVariantPK complexPersonWithJsonFormatVariantPK =
        new ComplexPersonWithJsonFormatVariantPK(0, UUID.randomUUID(), null, "0");

    assertEquals(complexPersonWithJsonFormatVariantPK, tarantoolTemplate.insert(complexPersonWithJsonFormatVariantPK));

    CompositePersonKeyWithJsonFormat key =
        new CompositePersonKeyWithJsonFormat(complexPersonWithJsonFormatVariantPK.getId(),
            complexPersonWithJsonFormatVariantPK.getSecondId());
    Optional<ComplexPersonWithJsonFormatVariantPK> foundPerson =
        tarantoolTemplate.findById(key, ComplexPersonWithJsonFormatVariantPK.class);
    assertTrue(foundPerson.isPresent());
    assertEquals(complexPersonWithJsonFormatVariantPK, foundPerson.get());
  }

  @Test
  void testCompositeKeyWithoutJsonFormat() {
    ComplexPersonWithIncorrectPK complexPersonWithIncorrectPK =
        new ComplexPersonWithIncorrectPK(0, UUID.randomUUID(), null, "0");

    Throwable exception = assertThrows(UncategorizedKeyValueException.class,
        () -> tarantoolTemplate.insert(complexPersonWithIncorrectPK)).getRootCause();
    assertInstanceOf(CrudException.class, exception);
  }

  public static Stream<Arguments> dataForTestCompositeKeyWithConcurrency() {
    final List<Integer> threadsCount = Arrays.asList(1, 2, 3);
    final List<Integer> tuplesCount = Arrays.asList(20, 120, 360, 1200, 3000);
    final List<Arguments> totalArguments = new ArrayList<>();

    for (final Integer threadCount : threadsCount) {
      for (final Integer tupleCount : tuplesCount) {
        totalArguments.add(Arguments.of(threadCount, tupleCount));
      }
    }

    return totalArguments.stream();
  }

  @DisplayName("Checking the insertion of an entity with a composite key")
  @ParameterizedTest(autoCloseArguments = false, name = "thread count: {0}, tuples count: {1}")
  @MethodSource("dataForTestCompositeKeyWithConcurrency")
  void testCompositeKeyWithConcurrency(int threadsCount, int tuplesCount) {
    final ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
    final List<CompletableFuture<ComplexPerson>> futures = new ArrayList<>();
    final List<ComplexPerson> tuples = generateComplexPersons(tuplesCount);

    for (final ComplexPerson tuple : tuples) {
      futures.add(CompletableFuture.supplyAsync(() -> this.tarantoolTemplate.insert(tuple), executor));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).join();

    for (int i = 0; i < tuplesCount; i++) {
      assertEquals(tuples.get(i), futures.get(i).join());
    }
  }
}
