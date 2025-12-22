/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import static io.tarantool.spring.data31.utils.TarantoolTestSupport.PERSONS_COUNT;
import static io.tarantool.spring.data31.utils.TarantoolTestSupport.generatePersons;
import io.tarantool.spring.data31.utils.entity.Person;

public class TarantoolSliceTest {

  private static final int PAGE_SIZE = 10;

  private static final int PAGE_NUMBER = 0;

  private static final Person SOME_CURSOR = new Person(0, true, "0");

  private static List<Person> PERSONS;

  private static final Pageable SOME_PAGEABLE =
      new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR);

  @BeforeAll
  static void setUp() {
    PERSONS = generatePersons(PERSONS_COUNT);
  }

  static Stream<Arguments> dataForTestConstructorsShouldThrow() {
    Pageable tarantoolPageable = new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR);
    Pageable notTarantoolPageable = Pageable.ofSize(PAGE_SIZE);
    return Stream.of(
        Arguments.of(null, tarantoolPageable, false),
        Arguments.of(null, tarantoolPageable, true),
        Arguments.of(PERSONS, null, false),
        Arguments.of(PERSONS, null, true),
        Arguments.of(PERSONS, notTarantoolPageable, false),
        Arguments.of(PERSONS, notTarantoolPageable, true));
  }

  @ParameterizedTest
  @MethodSource("dataForTestConstructorsShouldThrow")
  public <T> void testConstructorsShouldThrow(List<T> content, Pageable pageable, boolean hasNext) {
    assertThrows(
        IllegalArgumentException.class, () -> new TarantoolSliceImpl<>(content, pageable, hasNext));
  }

  static Stream<Executable> dataForTestConstructorsDoesntThrow() {
    return Stream.of(
        () -> new TarantoolSliceImpl<>(PERSONS),
        () -> new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true),
        () -> new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, false));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestConstructorsDoesntThrow")
  public void testConstructorsDoesntThrow(Executable runTestCase) {
    assertDoesNotThrow(runTestCase);
  }

  static Stream<Arguments> dataForTestGetNumber() {
    return Stream.of(
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), SOME_PAGEABLE.getPageNumber()),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), true), 0));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestGetNumber")
  public <T> void testGetNumber(Slice<T> slice, int expectedPageNumber) {
    assertEquals(expectedPageNumber, slice.getNumber());
  }

  static Stream<Arguments> dataForTestGetSize() {
    return Stream.of(
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), SOME_PAGEABLE.getPageSize()),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), PERSONS.size()),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true),
            SOME_PAGEABLE.getPageSize()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), 0));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestGetSize")
  public <T> void testGetSize(Slice<T> slice, int expectedSize) {
    assertEquals(expectedSize, slice.getSize());
  }

  static Stream<Arguments> dataForTestGetNumberOfElements() {
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), PERSONS.size()),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), PERSONS.size()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true), 0),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), 0));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestGetNumberOfElements")
  public <T> void testGetNumberOfElements(Slice<T> slice, int expectedNumberOfElements) {
    assertEquals(expectedNumberOfElements, slice.getNumberOfElements());
  }

  static Stream<Arguments> dataForTestGetContent() {
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), PERSONS),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), PERSONS),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true),
            Collections.emptyList()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), Collections.emptyList()));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestGetContent")
  public <T> void testGetContent(Slice<T> slice, List<T> expectedContent) {
    assertEquals(expectedContent, slice.getContent());
  }

  static Stream<Arguments> dataForTestHasContent() {
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), true),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), true),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true), false),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), false));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestHasContent")
  <T> void testHasContent(Slice<T> slice, boolean expectedHasContent) {
    assertEquals(expectedHasContent, slice.hasContent());
  }

  static Stream<Arguments> dataForTestGetSort() {
    return Stream.of(
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), SOME_PAGEABLE.getSort()),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), Sort.unsorted()));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestGetSort")
  <T> void testGetSort(Slice<T> slice, Sort expectedSort) {
    assertEquals(expectedSort, slice.getSort());
  }

  static Stream<Arguments> dataForTestIsFirst() {
    final int MIDDLE_PAGE_NUMBER = 2;
    return Stream.of(
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS, new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR), true),
            true),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS,
                new TarantoolPageRequest<>(MIDDLE_PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR),
                true),
            false),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), true), true),
        Arguments.of(
            new TarantoolSliceImpl<>(
                Collections.emptyList(),
                new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR),
                true),
            true),
        Arguments.of(
            new TarantoolSliceImpl<>(
                Collections.emptyList(),
                new TarantoolPageRequest<>(MIDDLE_PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR),
                true),
            true),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), true), true));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestIsFirst")
  <T> void testIsFirst(Slice<T> slice, boolean expectedIsFirst) {
    assertEquals(expectedIsFirst, slice.isFirst());
  }

  static Stream<Arguments> dataForTestIsLast() {
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), false),
        // last imitation
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, false), true),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), true), false),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), false), true),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true), true),
        // last imitation
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, false), true),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), true), true),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), false), true));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestIsLast")
  <T> void testIsLast(Slice<T> slice, boolean expectedIsLast) {
    assertEquals(expectedIsLast, slice.isLast());
  }

  static Stream<Arguments> dataForTestHasNext() {
    return Stream.of(
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, new TarantoolPageRequest<>(PAGE_SIZE), true), true),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, new TarantoolPageRequest<>(PAGE_SIZE), false), false),
        Arguments.of(
            new TarantoolSliceImpl<>(
                Collections.emptyList(), new TarantoolPageRequest<>(PAGE_SIZE), true),
            false),
        Arguments.of(
            new TarantoolSliceImpl<>(
                Collections.emptyList(), new TarantoolPageRequest<>(PAGE_SIZE), false),
            false));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestHasNext")
  <T> void testHasNext(Slice<T> slice, boolean expectedHasNext) {
    assertEquals(expectedHasNext, slice.hasNext());
  }

  static Stream<Arguments> dataForTestHasPrevious() {
    final int MIDDLE_PAGE_NUMBER = 2;
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true), false),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS,
                new TarantoolPageRequest<>(MIDDLE_PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR),
                false),
            true),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), true), false),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), false), false),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true), false),
        Arguments.of(
            new TarantoolSliceImpl<>(
                Collections.emptyList(),
                new TarantoolPageRequest<>(MIDDLE_PAGE_NUMBER, PAGE_SIZE, SOME_CURSOR),
                false),
            false),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), true), false),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), false), false));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestHasPrevious")
  <T> void testHasPrevious(Slice<T> slice, boolean expectedHasPrevious) {
    assertEquals(expectedHasPrevious, slice.hasPrevious());
  }

  static Stream<Arguments> dataForTestGetPageable() {
    Pageable pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, pageable, true), pageable),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), pageable, true), pageable),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS, pageable, false), pageable),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), pageable, false), pageable),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), true), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), true),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, Pageable.unpaged(), false), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), Pageable.unpaged(), false),
            Pageable.unpaged()),
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), Pageable.unpaged()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), Pageable.unpaged()));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestGetPageable")
  <T> void testGetPageable(Slice<T> slice, Pageable expectedPageable) {
    assertEquals(expectedPageable, slice.getPageable());
  }

  static Stream<Arguments> dataForTestNextPageable() {
    Pageable pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), Pageable.unpaged()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), pageable, true),
            new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE - 1))),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), pageable, true), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), pageable, false),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), pageable, false), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(0, PAGE_SIZE),
                new TarantoolPageRequest<>(4, PAGE_SIZE, null),
                true),
            new TarantoolPageRequest<>(5, PAGE_SIZE, PERSONS.get(PAGE_SIZE - 1))));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestNextPageable")
  <T> void testNextPageable(Slice<T> slice, Pageable expectedNextPageable) {
    assertEquals(expectedNextPageable, slice.nextPageable());
  }

  static Stream<Arguments> dataForTestPreviousPageable() {
    Pageable firstPagePageable = new TarantoolPageRequest<>(PAGE_SIZE);
    Pageable secondPagePageable =
        new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE - 1));

    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), Pageable.unpaged()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), firstPagePageable, true),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), firstPagePageable, true),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), firstPagePageable, false),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), firstPagePageable, false),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(PAGE_SIZE, 2 * PAGE_SIZE), secondPagePageable, true),
            // to take into account the direction of pagination use previousOrFirst method
            new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE))
                .previousOrFirst(PERSONS.get(PAGE_SIZE))),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), secondPagePageable, true),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(PAGE_SIZE, 2 * PAGE_SIZE), secondPagePageable, false),
            // to take into account the direction of pagination use previousOrFirst method
            new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE))
                .previousOrFirst(PERSONS.get(PAGE_SIZE))),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), secondPagePageable, false),
            Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(0, PAGE_SIZE),
                new TarantoolPageRequest<>(4, PAGE_SIZE, null),
                true),
            // to take into account the direction of pagination use previousOrFirst method
            new TarantoolPageRequest<>(4, PAGE_SIZE, PERSONS.get(PAGE_SIZE))
                .previousOrFirst(PERSONS.get(0))));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestPreviousPageable")
  <T> void testPreviousPageable(Slice<T> slice, Pageable expectedPrevPageable) {
    assertEquals(expectedPrevPageable, slice.previousPageable());
  }

  static Stream<Arguments> dataForTestNextOrLastPageable() {
    Pageable pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), Pageable.unpaged()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), pageable, true),
            new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE - 1))),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), pageable, true), pageable),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), pageable, false), pageable),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList(), pageable, false), pageable),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(0, PAGE_SIZE),
                new TarantoolPageRequest<>(4, PAGE_SIZE, null),
                true),
            new TarantoolPageRequest<>(5, PAGE_SIZE, PERSONS.get(PAGE_SIZE - 1))),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(0, PAGE_SIZE),
                new TarantoolPageRequest<>(4, PAGE_SIZE, null),
                false),
            new TarantoolPageRequest<>(4, PAGE_SIZE, null)));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestNextOrLastPageable")
  <T> void testNextOrLastPageable(Slice<T> slice, Pageable expectedNextOrLastPageable) {
    assertEquals(expectedNextOrLastPageable, slice.nextOrLastPageable());
  }

  static Stream<Arguments> dataForTestPreviousOrFirstPageable() {
    Pageable firstPagePageable = new TarantoolPageRequest<>(PAGE_SIZE);
    Pageable secondPagePageable =
        new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE - 1));

    return Stream.of(
        Arguments.of(new TarantoolSliceImpl<>(PERSONS), Pageable.unpaged()),
        Arguments.of(new TarantoolSliceImpl<>(Collections.emptyList()), Pageable.unpaged()),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), firstPagePageable, true),
            firstPagePageable),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), firstPagePageable, true),
            firstPagePageable),
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS.subList(0, PAGE_SIZE), firstPagePageable, false),
            firstPagePageable),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), firstPagePageable, false),
            firstPagePageable),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(PAGE_SIZE, 2 * PAGE_SIZE), secondPagePageable, true),
            // to take into account the direction of pagination use previousOrFirst method
            new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE))
                .previousOrFirst(PERSONS.get(PAGE_SIZE))),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), secondPagePageable, true),
            secondPagePageable),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(PAGE_SIZE, 2 * PAGE_SIZE), secondPagePageable, false),
            // to take into account the direction of pagination use previousOrFirst method
            new TarantoolPageRequest<>(1, PAGE_SIZE, PERSONS.get(PAGE_SIZE))
                .previousOrFirst(PERSONS.get(PAGE_SIZE))),
        Arguments.of(
            new TarantoolSliceImpl<>(Collections.emptyList(), secondPagePageable, false),
            secondPagePageable),
        Arguments.of(
            new TarantoolSliceImpl<>(
                PERSONS.subList(0, PAGE_SIZE),
                new TarantoolPageRequest<>(4, PAGE_SIZE, null),
                true),
            // to take into account the direction of pagination use previousOrFirst method
            new TarantoolPageRequest<>(4, PAGE_SIZE, PERSONS.get(PAGE_SIZE))
                .previousOrFirst(PERSONS.get(0))));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestPreviousOrFirstPageable")
  <T> void testPreviousOrFirstPageable(Slice<T> slice, Pageable expectedPrevOrFirstPageable) {
    assertEquals(expectedPrevOrFirstPageable, slice.previousOrFirstPageable());
  }

  static Stream<Arguments> dataForTestMap() {

    Pageable pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    List<String> expectedNames = PERSONS.stream().map(Person::getName).collect(Collectors.toList());
    return Stream.of(
        Arguments.of(
            new TarantoolSliceImpl<>(PERSONS, pageable, true),
            (Function<Person, String>) Person::getName,
            expectedNames,
            new TarantoolSliceImpl<>(expectedNames, pageable, true)));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestMap")
  <T, U> void testMap(
      Slice<T> slice,
      Function<T, U> mapper,
      List<U> expectedMappedList,
      Slice<U> expectedMappedSlice) {
    assertEquals(expectedMappedSlice, slice.map(mapper));
    assertEquals(expectedMappedList, slice.map(mapper).getContent());
  }

  protected static Stream<Arguments> dataForTestEquals() {

    return Stream.of(
        Arguments.of(
            Arrays.asList(
                new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true),
                new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true),
                new TarantoolSliceImpl<>(PERSONS, SOME_PAGEABLE, true)),
            new TarantoolSliceImpl<>(Collections.emptyList(), SOME_PAGEABLE, true)));
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestEquals")
  <T> void testEquals(List<Slice<T>> equalPageableList, Slice<T> notEqualPageable) {

    assertFalse(equalPageableList.isEmpty());
    final int COUNT = 10;

    for (int i = 0; i < COUNT; i++) {
      for (Slice<T> pageable : equalPageableList) {
        for (Slice<T> secondPageable : equalPageableList) {
          assertEquals(pageable, secondPageable);
        }
        assertNotEquals(pageable, null);
        assertNotEquals(pageable, notEqualPageable);
      }
    }
  }

  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("dataForTestEquals")
  <T> void testHashCode(List<Slice<T>> equalPageableList, Slice<T> notEqualPageable) {
    for (Slice<T> pageable : equalPageableList) {
      for (Slice<T> secondPageable : equalPageableList) {
        assertEquals(pageable.hashCode(), secondPageable.hashCode());
      }
      assertNotEquals(pageable.hashCode(), notEqualPageable.hashCode());
    }
  }
}
