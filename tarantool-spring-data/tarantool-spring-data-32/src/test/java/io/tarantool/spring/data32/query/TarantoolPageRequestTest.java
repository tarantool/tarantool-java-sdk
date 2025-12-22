/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.query;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static io.tarantool.spring.data.query.PaginationDirection.FORWARD;
import io.tarantool.spring.data.query.PaginationDirection;
import io.tarantool.spring.data.utils.GenericPerson;
import io.tarantool.spring.data32.utils.entity.Person;

class TarantoolPageRequestTest {

  private static final int PAGE_SIZE = 10;

  private static final Person CURSOR = new Person(0, true, "0");

  private static final int PAGE_NUMBER = 0;

  protected static Stream<Arguments> dataForTestConstructors() {
    return Stream.of(
        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>)
                () -> new TarantoolPageRequest<>(PAGE_SIZE),
            PAGE_NUMBER,
            PAGE_SIZE,
            Sort.unsorted(),
            null,
            FORWARD),

        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>)
                () -> new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR),
            PAGE_NUMBER,
            PAGE_SIZE,
            Sort.unsorted(),
            CURSOR,
            FORWARD),
        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>)
                () -> new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, null),
            PAGE_NUMBER,
            PAGE_SIZE,
            Sort.unsorted(),
            null,
            FORWARD));
  }

  @ParameterizedTest
  @MethodSource("dataForTestConstructors")
  void testConstructors(Supplier<TarantoolPageable<GenericPerson<?>>> constructorGenerator,
      int pageNumber, int size, Sort sort, GenericPerson<?> cursor, PaginationDirection direction) {
    TarantoolPageable<GenericPerson<?>> pageable = constructorGenerator.get();

    assertEquals(size, pageable.getPageSize());
    assertEquals(pageNumber, pageable.getPageNumber());
    assertEquals(sort, pageable.getSort());
    assertEquals(cursor, pageable.getTupleCursor());
    assertEquals(direction, pageable.getPaginationDirection());
  }

  protected static Stream<Arguments> dataTestGetSort() {
    return Stream.of(
        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>) () -> new TarantoolPageRequest<>(PAGE_SIZE),
            Sort.unsorted()),
        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>)
                () -> new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR),
            Sort.unsorted()));
  }

  @ParameterizedTest
  @MethodSource("dataTestGetSort")
  void testGetSort(Supplier<TarantoolPageable<GenericPerson<?>>> constructorGenerator, Sort sort) {
    Pageable pageable = constructorGenerator.get();
    assertEquals(sort, pageable.getSort());
  }

  protected static Stream<Arguments> dataForTestGetTupleCursor() {
    return Stream.of(
        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>) () -> new TarantoolPageRequest<>(PAGE_SIZE),
            null),
        Arguments.of(
            (Supplier<TarantoolPageable<GenericPerson<?>>>)
                () -> new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR),
            CURSOR));
  }

  @ParameterizedTest
  @MethodSource("dataForTestGetTupleCursor")
  void testGetTupleCursor(Supplier<TarantoolPageable<GenericPerson<?>>> constructorGenerator, GenericPerson<?> person) {
    TarantoolPageable<GenericPerson<?>> pageable = constructorGenerator.get();
    assertEquals(person, pageable.getTupleCursor());
  }

  protected static Stream<Arguments> dataForTestNext() {
    final Person NEXT_CURSOR = new Person(CURSOR.getId() + PAGE_SIZE, CURSOR.getIsMarried(), CURSOR.getName());
    return Stream.of(
        Arguments.of(
            new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR),
            CURSOR,
            NEXT_CURSOR));
  }

  @ParameterizedTest
  @MethodSource("dataForTestNext")
  void testNext(TarantoolPageable<GenericPerson<?>> pageable, GenericPerson<?> cursor, GenericPerson<?> nextCursor) {
    assertEquals(cursor, pageable.getTupleCursor());

    TarantoolPageable<GenericPerson<?>> nextPageable = pageable.next(nextCursor);
    assertEquals(nextCursor, nextPageable.getTupleCursor());

    assertEquals(pageable.getPageSize(), nextPageable.getPageSize());
    assertEquals(pageable.getPageNumber() + 1, nextPageable.getPageNumber());
    assertEquals(pageable.getSort(), nextPageable.getSort());
    assertEquals(pageable.getOffset() + PAGE_SIZE, nextPageable.getOffset());
  }

  @Test
  void testNextUnsupported() {
    TarantoolPageable<Person> pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    assertThrows(UnsupportedOperationException.class, pageable::next);
  }

  protected static Stream<Arguments> dataTestGetPaginationDirection() {
    final Person PREV_CURSOR = new Person(CURSOR.getId(), CURSOR.getIsMarried(), CURSOR.getName());
    final Person NEXT_CURSOR = new Person(PREV_CURSOR.getId() + PAGE_SIZE, CURSOR.getIsMarried(), CURSOR.getName());
    return Stream.of(
        Arguments.of(
            new TarantoolPageRequest<>(1, PAGE_SIZE, NEXT_CURSOR),
            NEXT_CURSOR,
            PREV_CURSOR));
  }

  @ParameterizedTest
  @MethodSource("dataTestGetPaginationDirection")
  void testGetPaginationDirection(TarantoolPageable<GenericPerson<?>> pageable, GenericPerson<?> nextCursor,
      GenericPerson<?> prevCursor) {

    assertEquals(FORWARD, pageable.getPaginationDirection());

    TarantoolPageable<GenericPerson<?>> prevPageable = pageable.previousOrFirst(prevCursor);
    assertEquals(PaginationDirection.BACKWARD, prevPageable.getPaginationDirection());

    TarantoolPageable<GenericPerson<?>> nextPageable = prevPageable.next(nextCursor);
    assertEquals(FORWARD, nextPageable.getPaginationDirection());
    assertEquals(pageable, nextPageable);
  }

  protected static Stream<Arguments> dataTestFirst() {
    final Person CURSOR = new Person(2 * PAGE_SIZE, true, "name");
    return Stream.of(
        Arguments.of(
            new TarantoolPageRequest<>(1, PAGE_SIZE, CURSOR),
            new TarantoolPageRequest<>(0, PAGE_SIZE, null)));
  }

  @ParameterizedTest
  @MethodSource("dataTestFirst")
  void testFirst(TarantoolPageable<GenericPerson<?>> pageable,
      TarantoolPageable<GenericPerson<?>> exceptedFirstPageable) {
    assertEquals(exceptedFirstPageable, pageable.first());
  }

  @Test
  void testWithPage() {
    TarantoolPageable<Person> pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    assertThrows(UnsupportedOperationException.class, () -> pageable.withPage(100));
  }

  protected static Stream<Arguments> dataTestPreviousOrFirst() {
    final Person CURSOR = new Person(PAGE_SIZE, true, "name");
    return Stream.of(
        Arguments.of(
            new TarantoolPageRequest<>(1, PAGE_SIZE, CURSOR),
            new Person(CURSOR.getId() - PAGE_SIZE, CURSOR.getIsMarried(), CURSOR.getName())));
  }

  @ParameterizedTest
  @MethodSource("dataTestPreviousOrFirst")
  void testPreviousOrFirst(TarantoolPageable<GenericPerson<?>> pageable, GenericPerson<?> prevCursor) {

    TarantoolPageable<GenericPerson<?>> prevPageable = pageable.previousOrFirst(prevCursor);

    assertNotEquals(pageable, prevPageable);
    assertEquals(pageable.getPageSize(), prevPageable.getPageSize());
    assertEquals(pageable.getOffset() - PAGE_SIZE, prevPageable.getOffset());
    assertEquals(pageable.getSort(), prevPageable.getSort());
    assertEquals(pageable.getPageNumber() - 1, prevPageable.getPageNumber());

    // here will be first with null cursor
    TarantoolPageable<GenericPerson<?>> firstPageable = prevPageable.previousOrFirst(prevCursor);
    assertNull(firstPageable.getTupleCursor());
  }

  @Test
  void testPreviousOrFirstUnsupported() {
    TarantoolPageable<Person> pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    assertThrows(UnsupportedOperationException.class, pageable::previousOrFirst);
  }

  @Test
  void testPreviousUnsupported() {
    AbstractPageRequest pageable = new TarantoolPageRequest<>(PAGE_SIZE);
    assertThrows(UnsupportedOperationException.class, pageable::previous);
  }

  protected static Stream<Arguments> dataForTestEquals() {
    return Stream.of(
        Arguments.of(
            Arrays.asList(
                new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR),
                new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR),
                new TarantoolPageRequest<>(PAGE_NUMBER, PAGE_SIZE, CURSOR)
            ),
            new TarantoolPageRequest<>(PAGE_SIZE)
        ));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEquals")
  void testEquals(List<TarantoolPageable<GenericPerson<?>>> equalPageableList,
      TarantoolPageable<GenericPerson<?>> notEqualPageable) {

    assertTrue(equalPageableList.size() > 0);
    final int COUNT = 10;

    for (int i = 0; i < COUNT; i++) {
      for (TarantoolPageable<GenericPerson<?>> pageable : equalPageableList) {
        for (TarantoolPageable<GenericPerson<?>> secondPageable : equalPageableList) {
          assertEquals(pageable, secondPageable);
        }
        assertNotEquals(pageable, null);
        assertNotEquals(pageable, notEqualPageable);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("dataForTestEquals")
  void testHashCode(List<TarantoolPageable<GenericPerson<?>>> equalPageableList,
      TarantoolPageable<GenericPerson<?>> notEqualPageable) {

    for (TarantoolPageable<GenericPerson<?>> pageable : equalPageableList) {
      for (TarantoolPageable<GenericPerson<?>> secondPageable : equalPageableList) {
        assertEquals(pageable.hashCode(), secondPageable.hashCode());
      }
      assertNotEquals(pageable.hashCode(), notEqualPageable.hashCode());
    }
  }
}
