/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.integration.crud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.util.Streamable;

import static io.tarantool.spring.data.ProxyTarantoolQueryEngine.unwrapTuples;
import static io.tarantool.spring.data31.query.TarantoolQueryCreator.INVALID_DATA_ACCESS_API_USAGE_EXCEPTION_MESSAGE_TEMPLATE;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.COMPLEX_PERSON_SPACE;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.PERSONS_COUNT;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.PERSON_SPACE;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.UNKNOWN_PERSON;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.generateAndInsertComplexPersons;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.generateAndInsertPersons;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.mapping.Tuple;
import io.tarantool.spring.data.query.Conditions;
import io.tarantool.spring.data.utils.GenericPerson;
import io.tarantool.spring.data.utils.GenericRepositoryMethods;
import io.tarantool.spring.data.utils.Pair;
import io.tarantool.spring.data31.query.TarantoolPageRequest;
import io.tarantool.spring.data31.query.TarantoolScrollPosition;
import io.tarantool.spring.data31.query.TarantoolWindowIterator;
import io.tarantool.spring.data31.utils.TarantoolTestSupport;
import io.tarantool.spring.data31.utils.core.ComplexPersonRepository;
import io.tarantool.spring.data31.utils.core.GenericPaginationMethods;
import io.tarantool.spring.data31.utils.core.PersonRepository;
import io.tarantool.spring.data31.utils.entity.ComplexPerson;
import io.tarantool.spring.data31.utils.entity.Person;

abstract class GenericRepositoryTest extends CrudConfigurations {

  @Autowired protected ApplicationContext context;

  protected static BiFunction<ApplicationContext, Integer, List<ComplexPerson>>
      complexPersonGenerateAndInsertFunction =
          (context, personCount) ->
              generateAndInsertComplexPersons(
                  personCount, context.getBean(TarantoolCrudClient.class));

  protected static BiFunction<ApplicationContext, Integer, List<Person>>
      personGenerateAndInsertFunction =
          (context, personCount) ->
              generateAndInsertPersons(personCount, context.getBean(TarantoolCrudClient.class));

  protected static Function<Integer, List<ComplexPerson>> complexPersonGenerateFunction =
      TarantoolTestSupport::generateComplexPersons;

  protected static Function<Integer, List<Person>> personGenerateFunction =
      TarantoolTestSupport::generatePersons;

  protected static final int INDEX_ID_VALUE = 50;

  protected static final Pair<String, ?> PERSON_INDEX_KEY = Pair.of("pk", INDEX_ID_VALUE);

  protected static final Pair<String, ?> PERSON_INITIAL_INDEX_KEY =
      Pair.of("pk", Collections.emptyList());

  protected static Stream<Arguments> dataForTestRepositoryWithFindByIs() {
    return Stream.of(
        Arguments.of(personGenerateAndInsertFunction, PersonRepository.class),
        Arguments.of(complexPersonGenerateAndInsertFunction, ComplexPersonRepository.class));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByIs")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByIs(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final PERSON person = persons.get(0);
    List<PERSON> foundPersons = repository.findByName(person.getName());

    foundPersons.sort(Comparator.comparing(PERSON::getId));
    assertEquals(1, foundPersons.size());
    assertEquals(person, foundPersons.get(0));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindById() {
    return dataForTestRepositoryWithFindByIs();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindById")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindById(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final PERSON person = persons.get(0);
    Optional<PERSON> foundPerson = repository.findById(person.generateFullKey());

    assertTrue(foundPerson.isPresent());
    assertEquals(person, foundPerson.get());
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindAllById() {
    return dataForTestRepositoryWithFindByIs();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindAllById")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindAllById(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    Iterable<PERSON> foundPersons =
        repository.findAllById(
            persons.stream().map(PERSON::generateFullKey).collect(Collectors.toList()));
    List<PERSON> foundPersonsAsList =
        StreamSupport.stream(foundPersons.spliterator(), false)
            .sorted(Comparator.comparing(PERSON::getId))
            .collect(Collectors.toList());

    assertEquals(persons, foundPersonsAsList);
  }

  protected static Stream<Arguments> dataForTestRepositoryWithCount() {
    return dataForTestRepositoryWithFindByIs();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithCount")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithCount(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    assertEquals(PERSONS_COUNT, context.getBean(repositoryClass).count());
  }

  protected static Stream<Arguments> dataForTestRepositoryWithCountBy() {
    return dataForTestRepositoryWithFindByIs();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithCountBy")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithCountBy(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final PERSON person = persons.get(0);
    assertEquals(1, repository.countByName(person.getName()));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithDelete() {
    return Stream.of(
        Arguments.of(
            personGenerateFunction,
            PersonRepository.class,
            PERSON_SPACE,
            (BiFunction<TarantoolCrudSpace, Object, ? extends GenericPerson<?>>)
                (space, key) -> {
                  Tuple<Person> tuple =
                      space.get(Collections.singletonList(key), Person.class).join();
                  if (tuple != null) {
                    return tuple.get();
                  }
                  return null;
                }),
        Arguments.of(
            complexPersonGenerateFunction,
            ComplexPersonRepository.class,
            COMPLEX_PERSON_SPACE,
            (BiFunction<TarantoolCrudSpace, Object, ? extends GenericPerson<?>>)
                (space, key) -> {
                  Tuple<ComplexPerson> tuple = space.get(key, ComplexPerson.class).join();
                  if (tuple != null) {
                    return tuple.get();
                  }
                  return null;
                }));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithDelete")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithDelete(
      Function<Integer, List<PERSON>> generateFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      String spaceName,
      BiFunction<TarantoolCrudSpace, Object, PERSON> spaceGetFunction) {

    final List<PERSON> persons = generateFunction.apply(PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    space.insertMany(persons).join();

    final PERSON deletingPerson = persons.get(0);

    assertEquals(deletingPerson, spaceGetFunction.apply(space, deletingPerson.generateFullKey()));
    // by object
    repository.delete(deletingPerson);
    assertNull(spaceGetFunction.apply(space, deletingPerson.generateFullKey()));

    space.insert(deletingPerson).join();
    assertEquals(deletingPerson, spaceGetFunction.apply(space, deletingPerson.generateFullKey()));

    // by id
    repository.deleteById(deletingPerson.generateFullKey());
    assertNull(spaceGetFunction.apply(space, deletingPerson.generateFullKey()));

    space.insert(deletingPerson).join();
    assertEquals(deletingPerson, spaceGetFunction.apply(space, deletingPerson.generateFullKey()));

    // by name
    repository.deleteByName(deletingPerson.getName());
    assertNull(spaceGetFunction.apply(space, deletingPerson.generateFullKey()));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithExistId() {
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            (BiConsumer<TarantoolCrudSpace, Object>)
                (space, key) -> space.delete(Collections.singletonList(key)).join(),
            PERSON_SPACE),
        Arguments.of(
            complexPersonGenerateAndInsertFunction,
            ComplexPersonRepository.class,
            (BiConsumer<TarantoolCrudSpace, Object>) (space, key) -> space.delete(key).join(),
            COMPLEX_PERSON_SPACE));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithExistId")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithExistId(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      BiConsumer<TarantoolCrudSpace, Object> deleteConsumer,
      String spaceName) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    final PERSON person = persons.get(0);

    assertTrue(repository.existsById(person.generateFullKey()));
    deleteConsumer.accept(space, person.generateFullKey());
    assertFalse(repository.existsById(person.generateFullKey()));
  }

  protected static Stream<Arguments> dataForTestQueryAnnotationAsCall() {
    return Stream.of(
        Arguments.of(PersonRepository.class), Arguments.of(ComplexPersonRepository.class));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestQueryAnnotationAsCall")
  <PERSON extends GenericPerson<ID>, ID> void testQueryAnnotationAsCall(
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    assertEquals(Arrays.asList(1, "hi", false), repository.getStatic().get());
  }

  protected static Stream<Arguments> dataForTestQueryAnnotationAsCallWithArgs() {
    return dataForTestQueryAnnotationAsCall();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestQueryAnnotationAsCallWithArgs")
  <PERSON extends GenericPerson<ID>, ID> void testQueryAnnotationAsCallWithArgs(
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    assertEquals(Arrays.asList("hello", true, 13), repository.echo("hello", true, 13).get());
  }

  protected static Stream<Arguments> dataForTestQueryAnnotationAsEval() {
    return dataForTestQueryAnnotationAsCall();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestQueryAnnotationAsEval")
  <PERSON extends GenericPerson<ID>, ID> void testQueryAnnotationAsEval(
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    assertEquals(Arrays.asList("hello", 123, true), repository.evalGetStatic().get());
  }

  protected static Stream<Arguments> dataForTestQueryAnnotationAsEvalWithArgs() {
    return dataForTestQueryAnnotationAsCall();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestQueryAnnotationAsEvalWithArgs")
  <PERSON extends GenericPerson<ID>, ID> void testQueryAnnotationAsEvalWithArgs(
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    assertEquals(
        Arrays.asList("hello", true, 13), repository.evalWithArgs("hello", true, 13).get());
  }

  protected static Stream<Arguments> dataForTestRepositoryWithExistName() {
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            (BiConsumer<TarantoolCrudSpace, Object>)
                (space, key) -> space.delete(Collections.singletonList(key)).join(),
            PERSON_SPACE),
        Arguments.of(
            complexPersonGenerateAndInsertFunction,
            ComplexPersonRepository.class,
            (BiConsumer<TarantoolCrudSpace, Object>) (space, key) -> space.delete(key).join(),
            COMPLEX_PERSON_SPACE));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithExistName")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithExistName(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      BiConsumer<TarantoolCrudSpace, Object> deleteConsumer,
      String spaceName) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    final PERSON person = persons.get(0);

    assertTrue(repository.existsByName(person.getName()));
    deleteConsumer.accept(space, person.generateFullKey());
    assertFalse(repository.existsByName(person.getName()));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindByLessThan() {
    return Stream.of(
        Arguments.of(personGenerateAndInsertFunction, PersonRepository.class),
        Arguments.of(complexPersonGenerateAndInsertFunction, ComplexPersonRepository.class));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByLessThan")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByLessThan(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int findLimiter = 10;
    final List<PERSON> foundPersons = repository.findAllByIdIsLessThan(findLimiter);

    foundPersons.sort(Comparator.comparing(PERSON::getId));

    assertEquals(findLimiter, foundPersons.size());
    assertEquals(persons.subList(0, findLimiter), foundPersons);
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindAllByIsMarriedIsTrue() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindAllByIsMarriedIsTrue")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindAllByIsMarriedIsTrue(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final List<PERSON> foundPersons = repository.findAllByIsMarriedIsTrue();
    assertEquals(PERSONS_COUNT / 3, foundPersons.size());
    assertTrue(foundPersons.stream().allMatch(GenericPerson::getIsMarried));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindAllByIsMarriedIsFalse() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindAllByIsMarriedIsFalse")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindAllByIsMarriedIsFalse(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final List<PERSON> foundPersons = repository.findAllByIsMarriedIsFalse();
    assertEquals(PERSONS_COUNT / 3, foundPersons.size());
    assertTrue(foundPersons.stream().noneMatch(GenericPerson::getIsMarried));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindAllByIsMarriedIsNull() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindAllByIsMarriedIsNull")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindAllByIsMarriedIsNull(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final List<PERSON> foundPersons = repository.findAllByIsMarriedIsNull();
    assertEquals(PERSONS_COUNT / 3 + 1, foundPersons.size());
    assertTrue(foundPersons.stream().allMatch(p -> p.getIsMarried() == null));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindAllByNameIsEmpty() {
    return Stream.of(
        Arguments.of(personGenerateFunction, PersonRepository.class),
        Arguments.of(complexPersonGenerateFunction, ComplexPersonRepository.class));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindAllByNameIsEmpty")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindAllByNameIsEmpty(
      Function<Integer, List<PERSON>> generateFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateFunction.apply(PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    PERSON person = persons.get(0);
    person.setName("");
    persons.set(0, person);

    repository.saveAll(persons);

    final List<PERSON> foundPersons = repository.findAllByNameIsEmpty();
    assertEquals(1, foundPersons.size());
    assertTrue(foundPersons.stream().allMatch(p -> p.getName().isEmpty()));
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindByLessThanEqual() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByLessThanEqual")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByLessThanEqual(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int findLimiter = 10;
    final List<PERSON> foundPersonsWithEqual = repository.findAllByIdLessThanEqual(findLimiter);

    foundPersonsWithEqual.sort(Comparator.comparing(PERSON::getId));

    assertEquals(findLimiter + 1, foundPersonsWithEqual.size());
    assertEquals(persons.subList(0, findLimiter + 1), foundPersonsWithEqual);
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindByGreaterThan() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByGreaterThan")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByGreaterThan(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int findLimiter = 90;
    final List<PERSON> foundPersons = repository.findAllByIdIsGreaterThan(findLimiter);

    foundPersons.sort(Comparator.comparing(PERSON::getId));

    assertEquals(PERSONS_COUNT - findLimiter - 1, foundPersons.size());
    assertEquals(persons.subList(findLimiter + 1, PERSONS_COUNT), foundPersons);
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindByGreaterThanEqual() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByGreaterThanEqual")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByGreaterThanEqual(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int findLimiter = 90;
    final List<PERSON> foundPersonsWithEqual = repository.findAllByIdGreaterThanEqual(findLimiter);

    foundPersonsWithEqual.sort(Comparator.comparing(PERSON::getId));

    assertEquals(PERSONS_COUNT - findLimiter, foundPersonsWithEqual.size());
    assertEquals(persons.subList(findLimiter, PERSONS_COUNT), foundPersonsWithEqual);
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindByBetween() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByBetween")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByBetween(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int startFindLimiter = 50;
    final int endFindLimiter = 90;
    final List<PERSON> foundPersons =
        repository.findAllByIdBetween(startFindLimiter, endFindLimiter);

    foundPersons.sort(Comparator.comparing(PERSON::getId));

    final int foundPersonsCount = endFindLimiter - startFindLimiter - 1;
    assertEquals(foundPersonsCount, foundPersons.size());
    assertEquals(persons.subList(startFindLimiter + 1, endFindLimiter), foundPersons);
  }

  protected static Stream<Arguments> dataForTestRepositoryWithFindByBetweenNegativeInts() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithFindByBetweenNegativeInts")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithFindByBetweenNegativeInts(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int startFindLimiter = -1;
    final int endFindLimiter = -10;
    final List<PERSON> foundPersons =
        repository.findAllByIdBetween(startFindLimiter, endFindLimiter);
    assertTrue(foundPersons.isEmpty());
  }

  protected static Stream<Arguments> dataForTestRepositoryIgnoreCase() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryIgnoreCase")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryIgnoreCase(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final String name = persons.get(0).getName();

    final InvalidDataAccessApiUsageException exception =
        assertThrows(
            InvalidDataAccessApiUsageException.class, () -> repository.findByNameIgnoreCase(name));
    final String EXCEPTION_MESSAGE =
        String.format(
            INVALID_DATA_ACCESS_API_USAGE_EXCEPTION_MESSAGE_TEMPLATE
                + "IgnoreCase isn't supported yet",
            Part.Type.SIMPLE_PROPERTY);
    assertEquals(EXCEPTION_MESSAGE, exception.getMessage());
  }

  protected static Stream<Arguments> dataForTestRepositoryWithTarantoolNamingNotation() {
    return dataForTestRepositoryWithFindByLessThan();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryWithTarantoolNamingNotation")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryWithTarantoolNamingNotation(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final List<PERSON> marriedPersons = repository.findByIsMarried(true);
    final List<PERSON> nonMarriedPersons = repository.findByIsMarried(false);
    final int expectedCount = PERSONS_COUNT / 3;
    assertEquals(expectedCount, marriedPersons.size());
    assertEquals(expectedCount, nonMarriedPersons.size());
  }

  protected static Stream<Arguments> dataForTestRepositoryNotEnoughArguments() {
    return dataForTestQueryAnnotationAsCall();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryNotEnoughArguments")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryNotEnoughArguments(
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    assertThrows(InvalidDataAccessApiUsageException.class, repository::findById);
  }

  protected static Stream<Arguments> dataForTestRepositorySave() {
    return dataForTestRepositoryWithDelete();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositorySave")
  <PERSON extends GenericPerson<ID>, ID> void testRepositorySave(
      Function<Integer, List<PERSON>> generateFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      String spaceName,
      BiFunction<TarantoolCrudSpace, Object, PERSON> getFunction) {

    List<PERSON> persons = generateFunction.apply(PERSONS_COUNT);
    GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    TarantoolCrudSpace space = client.space(spaceName);
    final PERSON person = persons.get(0);

    repository.save(person);

    PERSON gotPerson = getFunction.apply(space, person.generateFullKey());
    assertEquals(person, gotPerson);
  }

  protected static Stream<Arguments> dataForTestRepositorySaveAll() {
    Conditions conditions = new Conditions();
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            PERSON_SPACE,
            (Function<TarantoolCrudSpace, List<Person>>)
                (space) -> unwrapTuples(space.select(conditions, Person.class).join())),
        Arguments.of(
            complexPersonGenerateAndInsertFunction,
            ComplexPersonRepository.class,
            COMPLEX_PERSON_SPACE,
            (Function<TarantoolCrudSpace, List<ComplexPerson>>)
                (space) -> unwrapTuples(space.select(conditions, ComplexPerson.class).join())));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositorySaveAll")
  <PERSON extends GenericPerson<ID>, ID> void testRepositorySaveAll(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      String spaceName,
      Function<TarantoolCrudSpace, List<PERSON>> selectFunction) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    repository.saveAll(persons);

    List<PERSON> selectedPersons = selectFunction.apply(space);
    selectedPersons.sort(Comparator.comparing(GenericPerson::getId));

    assertEquals(persons, selectedPersons);
  }

  protected static Stream<Arguments> dataForTestDeleteAll() {
    return Stream.of(
        Arguments.of(personGenerateAndInsertFunction, PersonRepository.class, PERSON_SPACE),
        Arguments.of(
            complexPersonGenerateAndInsertFunction,
            ComplexPersonRepository.class,
            COMPLEX_PERSON_SPACE));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestDeleteAll")
  <PERSON extends GenericPerson<ID>, ID> void testDeleteAll(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      String spaceName) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    repository.saveAll(persons);
    assertEquals(PERSONS_COUNT, space.count(new Conditions()).join());
    assertDoesNotThrow(() -> repository.deleteAll());
    assertEquals(0, space.count(new Conditions()).join());

    repository.saveAll(persons);
    assertEquals(PERSONS_COUNT, space.count(new Conditions()).join());
    assertDoesNotThrow(() -> repository.deleteAll(persons));
    assertEquals(0, space.count(new Conditions()).join());

    repository.saveAll(persons);
    assertEquals(PERSONS_COUNT, space.count(new Conditions()).join());
    assertDoesNotThrow(
        () ->
            repository.deleteAllById(
                persons.stream().map(PERSON::generateFullKey).collect(Collectors.toList())));
    assertEquals(0, space.count(new Conditions()).join());
  }

  protected static Stream<Arguments> dataForTestRepositoryFindByAfter() {
    return Stream.of(
        Arguments.of(personGenerateAndInsertFunction, PersonRepository.class),
        Arguments.of(complexPersonGenerateAndInsertFunction, ComplexPersonRepository.class));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryFindByAfter")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryFindByAfter(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final PERSON person = persons.get(ThreadLocalRandom.current().nextInt(0, persons.size()));
    final List<PERSON> selectedPersons = repository.findByNameAfter(person.getName());
    selectedPersons.sort(Comparator.comparing(PERSON::getName));

    final List<PERSON> expectedList =
        persons.stream()
            .filter(p -> p.getName().compareTo(person.getName()) > 0)
            .limit(SelectOptions.DEFAULT_LIMIT)
            .sorted(Comparator.comparing(PERSON::getName))
            .collect(Collectors.toList());

    assertEquals(expectedList.size(), selectedPersons.size());
    assertEquals(expectedList, selectedPersons);
  }

  protected static Stream<Arguments> dataForTestRepositoryFindByAfterWithLimit() {
    return dataForTestRepositoryFindByAfter();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryFindByAfterWithLimit")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryFindByAfterWithLimit(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int LIMIT = 5;
    final PERSON person = persons.get(ThreadLocalRandom.current().nextInt(0, persons.size()));

    final List<PERSON> selectedPersons = repository.findFirst5ByIdAfter(person.getId());
    selectedPersons.sort(Comparator.comparing(PERSON::getId));

    final List<PERSON> selectedPersonsWithTopKeyword = repository.findTop5ByIdAfter(person.getId());
    selectedPersonsWithTopKeyword.sort(Comparator.comparing(PERSON::getId));

    final List<PERSON> expectedList =
        persons.stream()
            .filter(p -> p.getId().compareTo(person.getId()) > 0)
            .limit(LIMIT)
            .sorted(Comparator.comparing(PERSON::getId))
            .collect(Collectors.toList());

    assertEquals(expectedList.size(), selectedPersons.size());
    assertEquals(expectedList, selectedPersons);

    assertEquals(expectedList.size(), selectedPersonsWithTopKeyword.size());
    assertEquals(expectedList, selectedPersonsWithTopKeyword);
  }

  protected static Stream<Arguments> dataForTestRepositoryFindByBefore() {
    return dataForTestRepositoryFindByAfter();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryFindByBefore")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryFindByBefore(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final PERSON person = persons.get(ThreadLocalRandom.current().nextInt(0, persons.size()));

    final List<PERSON> selectedPersons = repository.findByNameBefore(person.getName());
    selectedPersons.sort(Comparator.comparing(PERSON::getName));

    final List<PERSON> expectedList =
        persons.stream()
            .filter(p -> p.getName().compareTo(person.getName()) < 0)
            .limit(SelectOptions.DEFAULT_LIMIT)
            .sorted(Comparator.comparing(PERSON::getName))
            .collect(Collectors.toList());

    assertEquals(expectedList.size(), selectedPersons.size());
    assertEquals(expectedList, selectedPersons);
  }

  protected static Stream<Arguments> dataForTestRepositoryFindByBeforeWithLimit() {
    return dataForTestRepositoryFindByAfter();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositoryFindByBeforeWithLimit")
  <PERSON extends GenericPerson<ID>, ID> void testRepositoryFindByBeforeWithLimit(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int LIMIT = 5;
    final PERSON person = persons.get(ThreadLocalRandom.current().nextInt(0, persons.size()));

    final List<PERSON> selectedPersons = repository.findFirst5ByIdBefore(person.getId());
    selectedPersons.sort(Comparator.comparing(PERSON::getId));

    final List<PERSON> selectedPersonsWithTopKeyword =
        repository.findTop5ByIdBefore(person.getId());
    selectedPersonsWithTopKeyword.sort(Comparator.comparing(PERSON::getId));

    // Because id (not fullscan)
    persons.sort(Comparator.comparing(PERSON::getId, (o1, o2) -> Integer.compare(o2, o1)));

    final List<PERSON> expectedList =
        persons.stream()
            .filter(p -> p.getId().compareTo(person.getId()) < 0)
            .limit(LIMIT)
            .sorted(Comparator.comparing(PERSON::getId))
            .collect(Collectors.toList());

    assertEquals(expectedList.size(), selectedPersons.size());
    assertEquals(expectedList, selectedPersons);

    assertEquals(expectedList.size(), selectedPersonsWithTopKeyword.size());
    assertEquals(expectedList, selectedPersonsWithTopKeyword);
  }

  protected static Stream<Arguments> dataForTestDeleteByAfter() {
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            PERSON_SPACE,
            (BiFunction<TarantoolCrudSpace, SelectOptions, List<Person>>)
                (space, options) ->
                    unwrapTuples(
                        space.select(Collections.emptyList(), options, Person.class).join())),
        Arguments.of(
            complexPersonGenerateAndInsertFunction,
            ComplexPersonRepository.class,
            COMPLEX_PERSON_SPACE,
            (BiFunction<TarantoolCrudSpace, SelectOptions, List<ComplexPerson>>)
                (space, options) ->
                    unwrapTuples(
                        space
                            .select(Collections.emptyList(), options, ComplexPerson.class)
                            .join())));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestDeleteByAfter")
  <PERSON extends GenericPerson<ID>, ID> void testDeleteByAfter(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      String spaceName,
      BiFunction<TarantoolCrudSpace, SelectOptions, List<PERSON>> selectFunction) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    persons.sort(Comparator.comparing(PERSON::getName));

    final int RANDOM_INDEX = ThreadLocalRandom.current().nextInt(0, PERSONS_COUNT);
    final SelectOptions options = SelectOptions.builder().withFirst(PERSONS_COUNT).build();
    final PERSON deletingAfterPerson = persons.get(RANDOM_INDEX);

    final List<PERSON> deletedPersons = repository.deleteByNameAfter(deletingAfterPerson.getName());

    List<PERSON> allPersonsInBaseAfterDeleted = selectFunction.apply(space, options);
    assertEquals(PERSONS_COUNT - deletedPersons.size(), allPersonsInBaseAfterDeleted.size());

    persons.removeAll(deletedPersons);
    allPersonsInBaseAfterDeleted.sort(Comparator.comparing(PERSON::getId));
    persons.sort(Comparator.comparing(PERSON::getId));
    assertEquals(persons, allPersonsInBaseAfterDeleted);
  }

  protected static Stream<Arguments> dataForTestDeleteByBefore() {
    return dataForTestDeleteByAfter();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestDeleteByBefore")
  <PERSON extends GenericPerson<ID>, ID> void testDeleteByBefore(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass,
      String spaceName,
      BiFunction<TarantoolCrudSpace, SelectOptions, List<PERSON>> selectFunction) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);
    final TarantoolCrudSpace space = client.space(spaceName);

    final SelectOptions options = SelectOptions.builder().withFirst(PERSONS_COUNT).build();
    final int RANDOM_INDEX = ThreadLocalRandom.current().nextInt(0, PERSONS_COUNT);

    final PERSON deletingBeforePerson = persons.get(RANDOM_INDEX);
    final List<PERSON> deletedPersons =
        repository.deleteByNameBefore(deletingBeforePerson.getName());

    List<PERSON> allPersonsInBaseAfterDeleted = selectFunction.apply(space, options);

    assertEquals(PERSONS_COUNT - deletedPersons.size(), allPersonsInBaseAfterDeleted.size());

    persons.removeAll(deletedPersons);
    allPersonsInBaseAfterDeleted.sort(Comparator.comparing(PERSON::getId));
    persons.sort(Comparator.comparing(PERSON::getId));
    assertEquals(persons, allPersonsInBaseAfterDeleted);
  }

  protected static Stream<Arguments> dataForTestCountByAfter() {
    return dataForTestRepositoryFindByAfter();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestCountByAfter")
  <PERSON extends GenericPerson<ID>, ID> void testCountByAfter(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    final int INDEX = ThreadLocalRandom.current().nextInt(0, PERSONS_COUNT);
    final int ID_AFTER = persons.get(INDEX).getId();

    final long count = repository.countByIdAfter(ID_AFTER);

    final int EXPECTED_COUNT = PERSONS_COUNT - ID_AFTER - 1;
    assertEquals(EXPECTED_COUNT, count);
  }

  protected static Stream<Arguments> dataForTestCountByBefore() {
    return dataForTestRepositoryFindByAfter();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestCountByBefore")
  <PERSON extends GenericPerson<ID>, ID> void testCountByBefore(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Class<GenericRepositoryMethods<PERSON, ID>> repositoryClass) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    final GenericRepositoryMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    persons.sort(Comparator.comparing(PERSON::getName));

    final int indexBefore = ThreadLocalRandom.current().nextInt(0, PERSONS_COUNT);
    final String name = persons.get(indexBefore).getName();
    final long count = repository.countByNameBefore(name);

    assertEquals(indexBefore, count);

    final long unknownNameCount = repository.countByNameBefore(UNKNOWN_PERSON.getName());
    assertEquals(0, unknownNameCount);
  }

  protected static <
          REPO extends GenericPaginationMethods<PERSON, ID>, PERSON extends GenericPerson<ID>, ID>
      Stream<Arguments> dataForTestRepositorySlicePagePageableEqual() {
    final int PAGE_SIZE = ThreadLocalRandom.current().nextInt(1, PERSONS_COUNT / 3 - 1);
    final String NAME = "name";
    final Pageable beginPageable = new TarantoolPageRequest<GenericPerson<?>>(PAGE_SIZE);

    BiFunction<ApplicationContext, Integer, List<Person>> generatePersonAndInsertFunction =
        (context, size) ->
            generateAndInsertPersons(
                size, context.getBean(TarantoolCrudClient.class), (person) -> person.setName(NAME));

    BiFunction<ApplicationContext, Integer, List<ComplexPerson>>
        generateComplexPersonAndInsertFunction =
            (context, size) ->
                generateAndInsertComplexPersons(
                    size,
                    context.getBean(TarantoolCrudClient.class),
                    (person) -> person.setName(NAME));

    Function<ApplicationContext, PersonRepository> personRepositoryFunc =
        (context) -> context.getBean(PersonRepository.class);

    Function<ApplicationContext, ComplexPersonRepository> complexPersonRepositoryFunc =
        (context) -> context.getBean(ComplexPersonRepository.class);

    BiFunction<REPO, Pageable, Slice<PERSON>> executionForFindAllByName =
        (repo, pageable) -> repo.findAllByName(NAME, pageable);

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByNameFunc =
        (persons) ->
            persons.stream()
                .filter(person -> NAME.equals(person.getName()))
                .sorted(Comparator.comparing(PERSON::getId))
                .collect(Collectors.toList())
                .subList(0, PAGE_SIZE);

    BiFunction<REPO, Pageable, Slice<PERSON>> executionForFindByName =
        (repo, pageable) -> repo.findPersonByName(NAME, pageable);

    List<List<?>> generateFunctionPairs =
        Arrays.asList(
            Arrays.asList(generatePersonAndInsertFunction, personRepositoryFunc),
            Arrays.asList(generateComplexPersonAndInsertFunction, complexPersonRepositoryFunc));

    List<?> executionFunctions = Arrays.asList(executionForFindAllByName, executionForFindByName);

    // add arguments set
    List<Arguments> arguments = new ArrayList<>();
    for (List<?> generateFunctionPair : generateFunctionPairs) {
      for (Object executionFunction : executionFunctions) {
        arguments.add(
            Arguments.of(
                generateFunctionPair.get(0),
                executionFunction,
                generateFunctionPair.get(1),
                expectedListForFindAllByNameFunc,
                beginPageable));
      }
    }

    return arguments.stream();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositorySlicePagePageableEqual")
  <REPO extends GenericPaginationMethods<PERSON, ID>, PERSON extends GenericPerson<ID>, ID>
      void testRepositorySlicePagePageableEqual(
          BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
          BiFunction<REPO, Pageable, Slice<PERSON>> executingRepositoryMethod,
          Function<ApplicationContext, REPO> giveRepositoryFunction,
          Function<List<PERSON>, List<PERSON>> expectedSlicePageContent,
          Pageable beginPageable) {

    final List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    Slice<PERSON> slicePage =
        executingRepositoryMethod.apply(giveRepositoryFunction.apply(context), beginPageable);

    assertEquals(beginPageable, slicePage.getPageable());
    assertEquals(expectedSlicePageContent.apply(persons), slicePage.getContent());
  }

  protected static Stream<Arguments> dataForTestRepositorySliceMethodWithoutPageable() {
    final String NAME = "name";
    return Stream.of(
        Arguments.of(PersonRepository.class, NAME),
        Arguments.of(ComplexPersonRepository.class, NAME));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositorySliceMethodWithoutPageable")
  <PERSON extends GenericPerson<ID>, ID> void testRepositorySliceMethodWithoutPageable(
      Class<GenericPaginationMethods<PERSON, ID>> repositoryClass, String name) {
    GenericPaginationMethods<PERSON, ID> repository = context.getBean(repositoryClass);

    assertEquals(Pageable.unpaged(), repository.findAllByName(name).getPageable());
    assertEquals(Pageable.unpaged(), repository.findAllByName(name).nextPageable());
    assertEquals(Pageable.unpaged(), repository.findAllByName(name).nextOrLastPageable());
    assertEquals(Pageable.unpaged(), repository.findAllByName(name).previousPageable());
    assertEquals(Pageable.unpaged(), repository.findAllByName(name).previousOrFirstPageable());
  }

  @SuppressWarnings("unchecked")
  protected static <
          PERSON extends GenericPerson<Object>, REPO extends GenericPaginationMethods<PERSON, ?>>
      Stream<Arguments> dataForTestRepositorySliceWithDifferentFields() {
    final int PAGE_SIZE = 10;

    Function<Slice<PERSON>, Pageable> nextPageableFunc = Slice::nextPageable;
    Function<Slice<PERSON>, Pageable> prevPageableFunc = Slice::previousPageable;
    Function<Slice<PERSON>, Pageable> nextOrLastPageableFunc = Slice::nextOrLastPageable;
    Function<Slice<PERSON>, Pageable> prevOrFirstPageableFunc = Slice::previousOrFirstPageable;
    BiFunction<Pageable, Pageable, Boolean> predicateForNextPageableFunc =
        (currentPageable, nextPageable) -> nextPageable.isPaged();

    BiFunction<Pageable, Pageable, Boolean> predicateForNextOrPageableFunc =
        (currentPageable, nextPageable) -> !currentPageable.equals(nextPageable);

    Function<ApplicationContext, PersonRepository> personRepositoryFunc =
        (context) -> context.getBean(PersonRepository.class);

    Function<ApplicationContext, ComplexPersonRepository> complexPersonRepositoryFunc =
        (context) -> context.getBean(ComplexPersonRepository.class);

    Pageable beginPageable = new TarantoolPageRequest<>(PAGE_SIZE);

    BiFunction<List<Object>, Integer, Integer> idExtractionFunction =
        (args, index) -> {
          List<PERSON> persons = (List<PERSON>) args.get(0);
          return persons.get(index).getId();
        };

    BiFunction<List<Object>, Integer, String> nameExtractionFunction =
        (args, index) -> {
          List<PERSON> persons = (List<PERSON>) args.get(0);
          return persons.get(index).getName();
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAllByIdLessThanEqualFunc =
        (repo, arguments) -> {
          int key = idExtractionFunction.apply(arguments, PERSONS_COUNT - 1);
          return repo.findAllByIdLessThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindPersonByIdLessThanEqualFunc =
        (repo, arguments) -> {
          int key = idExtractionFunction.apply(arguments, PERSONS_COUNT - 1);
          return repo.findPersonByIdLessThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAllByIdGreaterThanEqualFunc =
        (repo, arguments) -> {
          int key = idExtractionFunction.apply(arguments, 0);
          return repo.findAllByIdGreaterThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindPersonByIdGreaterThanEqualFunc =
        (repo, arguments) -> {
          int key = idExtractionFunction.apply(arguments, 0);
          return repo.findPersonByIdGreaterThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAllByIsMarriedLessThanEqual =
        (repo, arguments) ->
            repo.findAllByIsMarriedLessThanEqual(false, (Pageable) arguments.get(1));

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindPersonByIsMarriedLessThanEqual =
        (repo, arguments) ->
            repo.findPersonByIsMarriedLessThanEqual(false, (Pageable) arguments.get(1));

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAllByIsMarriedGreaterThanEqual =
        (repo, arguments) ->
            repo.findAllByIsMarriedGreaterThanEqual(null, (Pageable) arguments.get(1));

    BiFunction<REPO, List<Object>, Slice<PERSON>>
        executionForFindPersonByIsMarriedGreaterThanEqual =
            (repo, arguments) ->
                repo.findPersonByIsMarriedGreaterThanEqual(null, (Pageable) arguments.get(1));

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAllByNameLessThanEqual =
        (repo, arguments) -> {
          String key = nameExtractionFunction.apply(arguments, PERSONS_COUNT / 2);
          return repo.findAllByNameLessThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindPersonByNameLessThanEqual =
        (repo, arguments) -> {
          String key = nameExtractionFunction.apply(arguments, PERSONS_COUNT / 2);
          return repo.findPersonByNameLessThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAllByNameGreaterThanEqual =
        (repo, arguments) -> {
          String key = nameExtractionFunction.apply(arguments, PERSONS_COUNT / 2);
          return repo.findAllByNameGreaterThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindPersonByNameGreaterThanEqual =
        (repo, arguments) -> {
          String key = nameExtractionFunction.apply(arguments, PERSONS_COUNT / 2);
          return repo.findPersonByNameGreaterThanEqual(key, (Pageable) arguments.get(1));
        };

    BiFunction<REPO, List<Object>, Slice<PERSON>> executionForFindAll =
        (repo, arguments) -> repo.findAll((Pageable) arguments.get(1));

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByIdLessThanEqualFunc =
        (persons) ->
            persons.stream()
                .sorted(Comparator.comparing(PERSON::getId).reversed())
                .collect(Collectors.toList());

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByIdGreaterThanEqualFunc =
        (persons) -> persons;

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByIsMarriedGreaterThanEqual =
        (persons) ->
            persons.stream()
                .sorted(
                    Comparator.comparing(
                        PERSON::getIsMarried, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList());

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByIsMarriedLessThanEqual =
        (persons) ->
            persons.stream()
                .filter(person -> person.getIsMarried() == null || !person.getIsMarried())
                .sorted(
                    Comparator.comparing(
                            PERSON::getIsMarried, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Comparator.comparing(PERSON::getId).reversed()))
                .collect(Collectors.toList());

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByNameLessThanEqual =
        (persons) -> {
          String name = persons.get(persons.size() / 2).getName();
          return persons.stream()
              .filter(person -> person.getName().compareTo(name) <= 0)
              .sorted(Comparator.comparing(PERSON::getId))
              .collect(Collectors.toList());
        };

    Function<List<PERSON>, List<PERSON>> expectedListForFindAllByNameGreaterThanEqual =
        (persons) -> {
          String name = persons.get(persons.size() / 2).getName();
          return persons.stream()
              .filter(person -> person.getName().compareTo(name) >= 0)
              .sorted(Comparator.comparing(PERSON::getId))
              .collect(Collectors.toList());
        };

    Function<List<PERSON>, List<PERSON>> expectedListForFindAll = (persons) -> persons;

    List<List<?>> getNextPageableMethodFunctionPairs =
        Arrays.asList(
            Arrays.asList(nextPageableFunc, prevPageableFunc, predicateForNextPageableFunc),
            Arrays.asList(
                nextOrLastPageableFunc, prevOrFirstPageableFunc, predicateForNextOrPageableFunc));

    List<List<?>> generateFunctions =
        Arrays.asList(
            Arrays.asList(personGenerateAndInsertFunction, personRepositoryFunc),
            Arrays.asList(complexPersonGenerateAndInsertFunction, complexPersonRepositoryFunc));

    List<List<?>> executeExpectedFunctionPairs =
        Arrays.asList(
            Arrays.asList(
                executionForFindAllByIdLessThanEqualFunc,
                expectedListForFindAllByIdLessThanEqualFunc),
            Arrays.asList(
                executionForFindAllByIdGreaterThanEqualFunc,
                expectedListForFindAllByIdGreaterThanEqualFunc),
            Arrays.asList(
                executionForFindAllByIsMarriedGreaterThanEqual,
                expectedListForFindAllByIsMarriedGreaterThanEqual),
            Arrays.asList(
                executionForFindAllByIsMarriedLessThanEqual,
                expectedListForFindAllByIsMarriedLessThanEqual),
            Arrays.asList(
                executionForFindAllByNameLessThanEqual, expectedListForFindAllByNameLessThanEqual),
            Arrays.asList(
                executionForFindAllByNameGreaterThanEqual,
                expectedListForFindAllByNameGreaterThanEqual),
            Arrays.asList(
                executionForFindPersonByIdLessThanEqualFunc,
                expectedListForFindAllByIdLessThanEqualFunc),
            Arrays.asList(
                executionForFindPersonByIdGreaterThanEqualFunc,
                expectedListForFindAllByIdGreaterThanEqualFunc),
            Arrays.asList(
                executionForFindPersonByIsMarriedGreaterThanEqual,
                expectedListForFindAllByIsMarriedGreaterThanEqual),
            Arrays.asList(
                executionForFindPersonByIsMarriedLessThanEqual,
                expectedListForFindAllByIsMarriedLessThanEqual),
            Arrays.asList(
                executionForFindPersonByNameLessThanEqual,
                expectedListForFindAllByNameLessThanEqual),
            Arrays.asList(
                executionForFindPersonByNameGreaterThanEqual,
                expectedListForFindAllByNameGreaterThanEqual),
            Arrays.asList(executionForFindAll, expectedListForFindAll));

    // add arguments set
    List<Arguments> arguments = new ArrayList<>();
    for (List<?> generateFunctionPair : generateFunctions) {
      for (List<?> getNextPrevPageableFuncTriple : getNextPageableMethodFunctionPairs) {
        for (List<?> executeExpectedFunctionPair : executeExpectedFunctionPairs) {
          arguments.add(
              Arguments.of(
                  generateFunctionPair.get(0),
                  generateFunctionPair.get(1),
                  executeExpectedFunctionPair.get(0),
                  getNextPrevPageableFuncTriple.get(0),
                  getNextPrevPageableFuncTriple.get(1),
                  getNextPrevPageableFuncTriple.get(2),
                  executeExpectedFunctionPair.get(1),
                  beginPageable));
        }
      }
    }
    return arguments.stream();
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestRepositorySliceWithDifferentFields")
  <PERSON extends GenericPerson<ID>, ID> void testRepositorySliceWithDifferentFields(
      BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
      Function<ApplicationContext, GenericPaginationMethods<PERSON, ID>> giveRepositoryFunction,
      BiFunction<GenericPaginationMethods<PERSON, ID>, List<Object>, Slice<PERSON>>
          executingRepositoryMethod,
      Function<Slice<PERSON>, Pageable> nextPageableMethod,
      Function<Slice<PERSON>, Pageable> prevPageableMethod,
      BiFunction<Pageable, Pageable, Boolean> stopPaginationPredicateFunction,
      Function<List<PERSON>, List<PERSON>> expectedResultListMovingFunction,
      Pageable beginPageable) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);

    // moving forward
    Pageable nextPageable = beginPageable;
    List<Slice<PERSON>> forwardSlices = new ArrayList<>();
    Slice<PERSON> slice;
    do {
      slice =
          executingRepositoryMethod.apply(
              giveRepositoryFunction.apply(context), Arrays.asList(persons, nextPageable));
      forwardSlices.add(slice);
      nextPageable = nextPageableMethod.apply(slice);
    } while (stopPaginationPredicateFunction.apply(slice.getPageable(), nextPageable));

    List<PERSON> forwardMovingResultList =
        forwardSlices.stream().flatMap(Streamable::stream).collect(Collectors.toList());
    assertEquals(expectedResultListMovingFunction.apply(persons), forwardMovingResultList);

    // moving backward
    List<Slice<PERSON>> reverseSlices = new ArrayList<>();
    nextPageable = forwardSlices.get(forwardSlices.size() - 1).getPageable();
    do {
      slice =
          executingRepositoryMethod.apply(
              giveRepositoryFunction.apply(context), Arrays.asList(persons, nextPageable));
      reverseSlices.add(slice);
      nextPageable = prevPageableMethod.apply(slice);
    } while (stopPaginationPredicateFunction.apply(slice.getPageable(), nextPageable));

    List<PERSON> backwardMovingResultList =
        reverseSlices.stream()
            .sorted(Comparator.comparing(Slice::getNumber))
            .flatMap(Streamable::stream)
            .collect(Collectors.toList());

    assertEquals(expectedResultListMovingFunction.apply(persons), backwardMovingResultList);
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedForwardFuncForFindFirst5ByIsMarriedInitial(List<PERSON> persons) {
    return persons.stream()
        .filter(person -> Objects.equals(person.getIsMarried(), false))
        .sorted(Comparator.comparing(PERSON::getId))
        .toList();
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedBackwardFuncForFindFirst5ByIsMarriedInitial(List<PERSON> persons) {
    List<PERSON> result =
        new ArrayList<>(expectedForwardFuncForFindFirst5ByIsMarriedInitial(persons));
    Collections.reverse(result);
    return result;
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedForwardFuncForFindFirst10ByIsMarriedGreaterThanEqualInitial(
          List<PERSON> persons) {
    return persons.stream()
        .filter(
            person -> {
              if (person.getIsMarried() == null) {
                return false;
              }
              int compare = Boolean.compare(person.getIsMarried(), false);
              return compare >= 0;
            })
        .sorted(Comparator.comparing(PERSON::getId))
        .toList();
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedBackwardFuncForFindFirst10ByIsMarriedGreaterThanEqualInitial(
          List<PERSON> persons) {
    List<PERSON> result =
        new ArrayList<>(
            expectedForwardFuncForFindFirst10ByIsMarriedGreaterThanEqualInitial(persons));
    Collections.reverse(result);
    return result;
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedForwardFuncForFindFirst5ByIsMarriedIndexKey(List<PERSON> persons) {
    return persons.stream()
        .filter(person -> Objects.equals(person.getIsMarried(), false))
        .filter(person -> person.getId() >= (int) PERSON_INDEX_KEY.getSecond())
        .sorted(Comparator.comparing(PERSON::getId))
        .toList();
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedBackwardFuncForFindFirst5ByIsMarriedIndexKey(List<PERSON> persons) {
    List<PERSON> result =
        new ArrayList<>(
            persons.stream()
                .filter(person -> Objects.equals(person.getIsMarried(), false))
                .sorted(Comparator.comparing(PERSON::getId))
                .toList());
    Collections.reverse(result);
    return result;
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedForwardFuncForFindFirst10ByIsMarriedGreaterThanEqualIndexKey(
          List<PERSON> persons) {
    return persons.stream()
        .filter(
            person -> {
              if (person.getIsMarried() == null) {
                return false;
              }
              int compare = Boolean.compare(person.getIsMarried(), false);
              return compare >= 0;
            })
        .filter(person -> person.getId() >= (int) PERSON_INDEX_KEY.getSecond())
        .sorted(Comparator.comparing(PERSON::getId))
        .toList();
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedBackwardFuncForFindFirst10ByIsMarriedGreaterThanEqualIndexKey(
          List<PERSON> persons) {
    List<PERSON> result =
        new ArrayList<>(
            persons.stream()
                .filter(
                    person -> {
                      if (person.getIsMarried() == null) {
                        return false;
                      }
                      int compare = Boolean.compare(person.getIsMarried(), false);
                      return compare >= 0;
                    })
                .sorted(Comparator.comparing(PERSON::getId))
                .toList());
    Collections.reverse(result);
    return result;
  }

  protected static <
          PERSON extends GenericPerson<Object>, REPO extends GenericPaginationMethods<PERSON, ?>>
      Window<PERSON> executionFunctionForFindFirst5ByIsMarried(REPO repository, List<Object> args) {
    ScrollPosition scrollPosition = (ScrollPosition) args.get(0);
    return repository.findFirst5ByIsMarried(false, scrollPosition);
  }

  protected static <
          PERSON extends GenericPerson<Object>, REPO extends GenericPaginationMethods<PERSON, ?>>
      Window<PERSON> executionFunctionForFindFirst10ByIsMarriedGreaterThanEqual(
          REPO repository, List<Object> args) {
    ScrollPosition scrollPosition = (ScrollPosition) args.get(0);
    return repository.findFirst10ByIsMarriedGreaterThanEqual(false, scrollPosition);
  }

  protected static <
          PERSON extends GenericPerson<Object>, REPO extends GenericPaginationMethods<PERSON, ?>>
      Stream<Arguments> dataForTestRepositoryScrollForwardAndBackwardMoving() {

    ScrollPosition beginScrollPositionInitial =
        TarantoolScrollPosition.forward(PERSON_INITIAL_INDEX_KEY);
    ScrollPosition beginScrollPositionIndexKey = TarantoolScrollPosition.forward(PERSON_INDEX_KEY);

    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            (BiFunction<REPO, List<Object>, Window<PERSON>>)
                GenericRepositoryTest::executionFunctionForFindFirst5ByIsMarried,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest::expectedForwardFuncForFindFirst5ByIsMarriedInitial,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest::expectedBackwardFuncForFindFirst5ByIsMarriedInitial,
            beginScrollPositionInitial),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            (BiFunction<REPO, List<Object>, Window<PERSON>>)
                GenericRepositoryTest::executionFunctionForFindFirst5ByIsMarried,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest::expectedForwardFuncForFindFirst5ByIsMarriedIndexKey,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest::expectedBackwardFuncForFindFirst5ByIsMarriedIndexKey,
            beginScrollPositionIndexKey),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            (BiFunction<REPO, List<Object>, Window<PERSON>>)
                GenericRepositoryTest::executionFunctionForFindFirst10ByIsMarriedGreaterThanEqual,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest
                    ::expectedForwardFuncForFindFirst10ByIsMarriedGreaterThanEqualInitial,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest
                    ::expectedBackwardFuncForFindFirst10ByIsMarriedGreaterThanEqualInitial,
            beginScrollPositionInitial),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            (BiFunction<REPO, List<Object>, Window<PERSON>>)
                GenericRepositoryTest::executionFunctionForFindFirst10ByIsMarriedGreaterThanEqual,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest
                    ::expectedForwardFuncForFindFirst10ByIsMarriedGreaterThanEqualIndexKey,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest
                    ::expectedBackwardFuncForFindFirst10ByIsMarriedGreaterThanEqualIndexKey,
            beginScrollPositionIndexKey));
  }

  @ParameterizedTest
  @MethodSource("dataForTestRepositoryScrollForwardAndBackwardMoving")
  <PERSON extends GenericPerson<ID>, ID, REPO extends GenericPaginationMethods<PERSON, ID>>
      void testRepositoryScrollForwardAndBackwardMoving(
          BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
          Class<REPO> repositoryClass,
          BiFunction<REPO, List<Object>, Window<PERSON>> executingRepositoryMethod,
          Function<List<PERSON>, List<PERSON>> expectedForwardResultFunction,
          Function<List<PERSON>, List<PERSON>> expectedBackwardResultFunction,
          ScrollPosition beginScrollPosition) {

    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    REPO repository = context.getBean(repositoryClass);

    Window<PERSON> personWindow =
        executingRepositoryMethod.apply(repository, Collections.singletonList(beginScrollPosition));
    List<PERSON> forwardResult = new ArrayList<>();

    while (!personWindow.isEmpty() && personWindow.hasNext()) {
      personWindow.forEach(forwardResult::add);
      ScrollPosition nextPosition = personWindow.positionAt(personWindow.size() - 1);
      personWindow =
          executingRepositoryMethod.apply(repository, Collections.singletonList(nextPosition));
    }
    personWindow.forEach(forwardResult::add);

    assertEquals(expectedForwardResultFunction.apply(persons), forwardResult);

    TarantoolScrollPosition backwardScrollPosition =
        ((TarantoolScrollPosition) personWindow.positionAt(0)).reverse();
    List<PERSON> backwardResult = personWindow.getContent();
    Collections.reverse(backwardResult);

    personWindow =
        executingRepositoryMethod.apply(
            repository, Collections.singletonList(backwardScrollPosition));
    while (!personWindow.isEmpty() && personWindow.hasNext()) {
      personWindow.forEach(backwardResult::add);
      ScrollPosition nextPosition = personWindow.positionAt(personWindow.size() - 1);
      personWindow =
          executingRepositoryMethod.apply(repository, Collections.singletonList(nextPosition));
    }
    personWindow.forEach(backwardResult::add);

    assertEquals(expectedBackwardResultFunction.apply(persons), backwardResult);
  }

  protected static <PERSON extends GenericPerson<Object>>
      Stream<Arguments> dataForTestWindowIterator() {
    return Stream.of(
        Arguments.of(
            (BiFunction<ApplicationContext, Integer, List<PERSON>>)
                (context, size) -> Collections.emptyList(),
            PersonRepository.class,
            TarantoolScrollPosition.forward(PERSON_INITIAL_INDEX_KEY),
            (Function<List<PERSON>, List<PERSON>>) (persons) -> Collections.emptyList(),
            (Function<List<PERSON>, List<PERSON>>) (persons) -> Collections.emptyList()),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            TarantoolScrollPosition.forward(PERSON_INITIAL_INDEX_KEY),
            (Function<List<PERSON>, List<PERSON>>) (persons) -> persons,
            (Function<List<PERSON>, List<PERSON>>)
                (persons) -> {
                  Collections.reverse(persons);
                  return persons;
                }),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            TarantoolScrollPosition.forward(PERSON_INDEX_KEY),
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest::expectedWindowIteratorForwardFromSomeCursor,
            (Function<List<PERSON>, List<PERSON>>)
                GenericRepositoryTest::expectedWindowIteratorBackwardFromSomeCursor));
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedWindowIteratorForwardFromSomeCursor(List<PERSON> persons) {
    return persons.stream()
        .filter(person -> person.getId() >= (int) PERSON_INDEX_KEY.getSecond())
        .collect(Collectors.toList());
  }

  protected static <PERSON extends GenericPerson<ID>, ID>
      List<PERSON> expectedWindowIteratorBackwardFromSomeCursor(List<PERSON> persons) {
    return persons.stream()
        .filter(person -> person.getId() <= (int) PERSON_INDEX_KEY.getSecond())
        .sorted(Comparator.comparing(PERSON::getId).reversed())
        .collect(Collectors.toList());
  }

  @ParameterizedTest
  @MethodSource("dataForTestWindowIterator")
  <PERSON extends GenericPerson<ID>, ID, REPO extends GenericPaginationMethods<PERSON, ID>>
      void testWithWindowIteratorMoving(
          BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
          Class<REPO> repositoryClass,
          TarantoolScrollPosition beginScrollPosition,
          Function<List<PERSON>, List<PERSON>> expectedForwardResultFunction,
          Function<List<PERSON>, List<PERSON>> expectedBackwardResultFunction) {
    List<PERSON> persons = generateAndInsertFunction.apply(context, PERSONS_COUNT);
    REPO repository = context.getBean(repositoryClass);

    TarantoolWindowIterator<PERSON> personForwardIterator =
        TarantoolWindowIterator.of(
                scrollPosition ->
                    repository.findFirst10ByIsMarriedGreaterThanEqual(null, scrollPosition))
            .startingAt(beginScrollPosition);

    List<PERSON> forwardResult = new ArrayList<>();

    while (personForwardIterator.hasNext()) {
      forwardResult.add(personForwardIterator.next());
    }

    forwardResult.sort(Comparator.comparing(PERSON::getId));
    assertEquals(expectedForwardResultFunction.apply(persons), forwardResult);

    // go back
    List<PERSON> backwardResult = new ArrayList<>();

    TarantoolWindowIterator<PERSON> personBackwardIterator =
        TarantoolWindowIterator.of(
                scrollPosition ->
                    repository.findFirst10ByIsMarriedGreaterThanEqual(null, scrollPosition))
            .startingAt(beginScrollPosition.reverse());

    while (personBackwardIterator.hasNext()) {
      backwardResult.add(personBackwardIterator.next());
    }

    assertEquals(expectedBackwardResultFunction.apply(persons), backwardResult);
  }

  protected static Stream<Object> dataForTestUnsupportedRepositoryMethods() {
    return Stream.of(PersonRepository.class, ComplexPersonRepository.class);
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestUnsupportedRepositoryMethods")
  <PERSON extends GenericPerson<ID>, ID, REPO extends GenericPaginationMethods<PERSON, ID>>
      void testUnsupportedRepositoryMethods(Class<REPO> repositoryClass) {
    final REPO repository = context.getBean(repositoryClass);

    assertThrows(UnsupportedOperationException.class, repository::findAll);
    assertThrows(UnsupportedOperationException.class, () -> repository.findAll(Sort.unsorted()));
  }

  protected static Stream<Arguments> dataForTestPageableWithTrueCursorAndPageNumber() {
    final int PAGE_SIZE = 10;
    final int PAGES_COUNT = PERSONS_COUNT / PAGE_SIZE;
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            new TarantoolPageRequest<>(PAGE_SIZE),
            PAGES_COUNT,
            PAGES_COUNT),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            new TarantoolPageRequest<>(1, PAGE_SIZE, new Person(9, null, "User-9")),
            PAGES_COUNT - 1,
            PAGES_COUNT),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            new TarantoolPageRequest<>(2, PAGE_SIZE, new Person(19, null, "User-19")),
            PAGES_COUNT - 2,
            PAGES_COUNT));
  }

  private static <T> void doTestPageableWithCursorAndPageNumber(
      Pageable pageable,
      BiFunction<Integer, Pageable, Slice<T>> repositoryFunction,
      BiConsumer<List<Slice<T>>, List<Slice<T>>> assertionAction) {
    List<Slice<T>> forwardPages = new ArrayList<>();

    Slice<T> page = repositoryFunction.apply(0, pageable);

    forwardPages.add(page);

    while (page.hasNext()) {
      page = repositoryFunction.apply(0, page.nextOrLastPageable());
      forwardPages.add(page);
    }

    List<Slice<T>> backwardPages = new ArrayList<>();
    for (int i = forwardPages.size() - 1; i >= 0; i--) {
      Slice<T> currentPage = forwardPages.get(i);
      if (currentPage.getPageable().isPaged()) {
        page = currentPage;
        break;
      }
    }
    backwardPages.add(page);

    page = repositoryFunction.apply(0, page.previousOrFirstPageable());
    backwardPages.add(page);

    while (page.hasPrevious()) {
      page = repositoryFunction.apply(0, page.previousOrFirstPageable());
      backwardPages.add(page);
    }

    assertionAction.accept(forwardPages, backwardPages);
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestPageableWithTrueCursorAndPageNumber")
  <PERSON extends GenericPerson<ID>, ID, REPO extends GenericPaginationMethods<PERSON, ID>>
      void testPageableWithTrueCursorAndPageNumber(
          BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
          Class<REPO> repositoryClass,
          Pageable pageable,
          int expectedForwardPageCount,
          int expectedBackwardPageCount) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    REPO repository = context.getBean(repositoryClass);

    BiConsumer<List<Slice<PERSON>>, List<Slice<PERSON>>> assertAction =
        (forwardSlice, backwardSlice) -> {
          assertEquals(expectedForwardPageCount, forwardSlice.size());
          assertEquals(expectedBackwardPageCount, backwardSlice.size());

          forwardSlice.forEach(p -> assertTrue(p.getPageable().isPaged()));
          backwardSlice.forEach(p -> assertTrue(p.getPageable().isPaged()));
        };

    doTestPageableWithCursorAndPageNumber(
        pageable, repository::findPersonByIdGreaterThanEqual, assertAction);
    doTestPageableWithCursorAndPageNumber(
        pageable, repository::findAllByIdGreaterThanEqual, assertAction);
  }

  protected static Stream<Arguments> dataForTestPageablePageNumberLessCursor() {
    final int PAGE_SIZE = 10;
    final int PAGES_COUNT = PERSONS_COUNT / PAGE_SIZE;
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            // fourth page
            new TarantoolPageRequest<>(0, PAGE_SIZE, new Person(29, null, "User-29")),
            PAGES_COUNT - 2,
            PAGES_COUNT - 3,
            PAGES_COUNT - 3),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            // third page
            new TarantoolPageRequest<>(0, PAGE_SIZE, new Person(19, null, "User-19")),
            PAGES_COUNT - 1,
            PAGES_COUNT - 2,
            PAGES_COUNT - 2));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestPageablePageNumberLessCursor")
  <PERSON extends GenericPerson<ID>, ID, REPO extends GenericPaginationMethods<PERSON, ID>>
      void testPageablePageNumberLessCursor(
          BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
          Class<REPO> repositoryClass,
          Pageable pageable,
          int expectedForwardPageCountForPage,
          int expectedForwardPageCountForSlice,
          int expectedBackwardPageCount) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    REPO repository = context.getBean(repositoryClass);

    BiConsumer<List<Slice<PERSON>>, List<Slice<PERSON>>> assertAction =
        (forwardSlice, backwardSlice) -> {
          int expectedForwardPageCount = expectedForwardPageCountForSlice;
          Slice<?> firstPageable = forwardSlice.get(0);
          if (firstPageable instanceof Page) {
            expectedForwardPageCount = expectedForwardPageCountForPage;
          }

          assertEquals(expectedForwardPageCount, forwardSlice.size());
          assertEquals(expectedBackwardPageCount, backwardSlice.size());

          if (firstPageable instanceof Page) {
            assertTrue(forwardSlice.get(forwardSlice.size() - 1).getPageable().isUnpaged());
            forwardSlice
                .subList(0, forwardSlice.size() - 1)
                .forEach(p -> assertTrue(p.getPageable().isPaged()));
          } else {
            forwardSlice.forEach(p -> assertTrue(p.getPageable().isPaged()));
          }
          backwardSlice.forEach(p -> assertTrue(p.getPageable().isPaged()));
        };

    doTestPageableWithCursorAndPageNumber(
        pageable, repository::findPersonByIdGreaterThanEqual, assertAction);
    doTestPageableWithCursorAndPageNumber(
        pageable, repository::findAllByIdGreaterThanEqual, assertAction);
  }

  protected static Stream<Arguments> dataForTestPageablePageNumberGreaterCursor() {
    final int PAGE_SIZE = 10;
    final int PAGES_COUNT = PERSONS_COUNT / PAGE_SIZE;
    return Stream.of(
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            // second page
            new TarantoolPageRequest<>(1, PAGE_SIZE, null),
            PAGES_COUNT - 1,
            PAGES_COUNT,
            PAGES_COUNT,
            PAGES_COUNT + 1),
        Arguments.of(
            personGenerateAndInsertFunction,
            PersonRepository.class,
            // fourth page
            new TarantoolPageRequest<>(3, PAGE_SIZE, new Person(9, null, "User-9")),
            PAGES_COUNT - 3,
            PAGES_COUNT - 1,
            PAGES_COUNT - 1,
            PAGES_COUNT + 1));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestPageablePageNumberGreaterCursor")
  <PERSON extends GenericPerson<ID>, ID, REPO extends GenericPaginationMethods<PERSON, ID>>
      void testPageablePageNumberGreaterCursor(
          BiFunction<ApplicationContext, Integer, List<PERSON>> generateAndInsertFunction,
          Class<REPO> repositoryClass,
          Pageable pageable,
          int expectedForwardPageCountForPage,
          int expectedForwardPageCountForSlice,
          int expectedBackwardPageCountForPage,
          int expectedBackwardPageCountForSlice) {

    generateAndInsertFunction.apply(context, PERSONS_COUNT);
    REPO repository = context.getBean(repositoryClass);

    BiConsumer<List<Slice<PERSON>>, List<Slice<PERSON>>> assertAction =
        (forwardSlice, backwardSlice) -> {
          int expectedForwardPageCount = expectedForwardPageCountForPage;
          int expectedBackwardPageCount = expectedBackwardPageCountForPage;

          Slice<?> firstPageable = forwardSlice.get(0);
          if (!(firstPageable instanceof Page)) {
            expectedForwardPageCount = expectedForwardPageCountForSlice;
            expectedBackwardPageCount = expectedBackwardPageCountForSlice;
          }

          assertEquals(expectedForwardPageCount, forwardSlice.size());
          assertEquals(expectedBackwardPageCount, backwardSlice.size());

          forwardSlice.forEach(p -> assertTrue(p.getPageable().isPaged()));
          assertTrue(backwardSlice.get(backwardSlice.size() - 1).getPageable().isUnpaged());
          backwardSlice
              .subList(0, backwardSlice.size() - 1)
              .forEach(p -> assertTrue(p.getPageable().isPaged()));
        };

    doTestPageableWithCursorAndPageNumber(
        pageable, repository::findPersonByIdGreaterThanEqual, assertAction);
    doTestPageableWithCursorAndPageNumber(
        pageable, repository::findAllByIdGreaterThanEqual, assertAction);
  }

  @Test
  void testConcurrentDerivedMethods() {

    final byte threadCount = 3;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final List<CompletableFuture<List<Person>>> futures = new ArrayList<>(threadCount);
    final PersonRepository repository = context.getBean(PersonRepository.class);

    final List<Person> insertedPersons =
        personGenerateAndInsertFunction.apply(context, (int) threadCount);
    final Person firstPerson = insertedPersons.get(0);
    final Person secondPerson = insertedPersons.get(1);
    final Person thirdPerson = insertedPersons.get(2);

    futures.add(
        CompletableFuture.supplyAsync(
            () -> repository.findPersonById(firstPerson.getId()), executor));
    futures.add(
        CompletableFuture.supplyAsync(
            () -> repository.findByIsMarried(secondPerson.getIsMarried()), executor));
    futures.add(
        CompletableFuture.supplyAsync(
            () -> repository.findByName(thirdPerson.getName()), executor));

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[] {})).join();

    assertEquals(firstPerson, futures.get(0).join().get(0));
    assertEquals(secondPerson, futures.get(1).join().get(0));
    assertEquals(thirdPerson, futures.get(2).join().get(0));
  }
}
