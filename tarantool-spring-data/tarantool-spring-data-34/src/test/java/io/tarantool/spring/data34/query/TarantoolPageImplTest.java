/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static io.tarantool.spring.data34.utils.TarantoolTestSupport.PERSONS_COUNT;
import static io.tarantool.spring.data34.utils.TarantoolTestSupport.generatePersons;
import io.tarantool.spring.data.utils.GenericPerson;
import io.tarantool.spring.data34.utils.entity.Person;

class TarantoolPageImplTest {

  private static final List<Person> PERSONS = generatePersons(PERSONS_COUNT);

  private static final int DEFAULT_PAGE_SIZE_PER_TEST_CLASS = 10;

  private static final int DEFAULT_PAGE_NUMBER_PER_TEST_CLASS = 0;

  private static final Pageable FIRST_PAGE_PAGEABLE_PER_TEST_CLASS =
      new TarantoolPageRequest<>(DEFAULT_PAGE_NUMBER_PER_TEST_CLASS, DEFAULT_PAGE_SIZE_PER_TEST_CLASS, null);

  private static final long MULTIPLIER = 2L;

  private static final BiFunction<Integer, String, Long> GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE =
      (pageSize, operator) -> {
        switch (operator) {
          // pageSize > totalPageCount
          case ">": {
            return pageSize / MULTIPLIER;
          }
          // pageSize < totalPageCount
          case "<": {
            return MULTIPLIER * pageSize;
          }
          // pageSize == totalPageCount
          case "==": {
            return Long.valueOf(pageSize);
          }
          default:
            throw new IllegalArgumentException("The passed option isn't supported");
        }
      };

  private static final List<Person> EMPTY_CONTENT = Collections.emptyList();

  protected static Stream<Arguments> dataForTestConstructors() {
    TarantoolPageable<GenericPerson<?>> pageable =
        new TarantoolPageRequest<>(0, DEFAULT_PAGE_SIZE_PER_TEST_CLASS, null);

    long personsSize = PERSONS.size();

    return Stream.of(
        Arguments.of(
            new TarantoolPageImpl<>(PERSONS, pageable, personsSize),
            PERSONS.size() / DEFAULT_PAGE_SIZE_PER_TEST_CLASS,
            personsSize),
        Arguments.of(
            new TarantoolPageImpl<>(PERSONS),
            1,
            personsSize),
        Arguments.of(
            new TarantoolPageImpl<>(),
            1,
            0L));
  }

  @ParameterizedTest
  @MethodSource("dataForTestConstructors")
  void testConstructors(Page<GenericPerson<?>> page, int totalPages, long totalElements) {
    assertEquals(totalPages, page.getTotalPages());
    assertEquals(totalElements, page.getTotalElements());
  }

  static Stream<Arguments> dataForTestConstructorsThrowsIllegalArgumentException() {
    final class DummyTarantoolPageRequest implements Pageable {

      @Override
      public int getPageNumber() {
        return 0;
      }

      @Override
      public int getPageSize() {
        return 0;
      }

      @Override
      public long getOffset() {
        return 0L;
      }

      @Override
      public Sort getSort() {
        return null;
      }

      @Override
      public Pageable next() {
        return null;
      }

      @Override
      public Pageable previousOrFirst() {
        return null;
      }

      @Override
      public Pageable first() {
        return null;
      }

      @Override
      public Pageable withPage(int pageNumber) {
        return null;
      }

      @Override
      public boolean hasPrevious() {
        return false;
      }
    }

    Pageable dummyPageable = new DummyTarantoolPageRequest();

    return Stream.of(
        Arguments.of(
            null, null,
            "Content must not be null"),
        Arguments.of(
            Collections.emptyList(), null,
            "Pageable must not be null"),
        Arguments.of(
            Collections.emptyList(), dummyPageable,
            "Pageable must be TarantoolPageable<T> or Unpaged type"));
  }

  @ParameterizedTest
  @MethodSource("dataForTestConstructorsThrowsIllegalArgumentException")
  void testConstructors_throws_IllegalArgumentException(List content, Pageable pageable, String errorMsg) {
    assertThrows(IllegalArgumentException.class, () -> new TarantoolPageImpl<>(content, pageable, 0L), errorMsg);
  }

  static Stream<Arguments> dataForTestGetTotalPages() {
    return Stream.of(
        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), ">")),
            1),

        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "<")),
            MULTIPLIER),

        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "==")),
            1),
        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                Pageable.unpaged(),
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(0, ">")),
            1),

        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                Pageable.unpaged(),
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(0, "<")),
            1),

        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                Pageable.unpaged(),
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(0, "==")),
            1),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), ">")),
            PERSONS.size() / FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "<")),
            MULTIPLIER),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "==")),
            1),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                Pageable.unpaged(),
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(0, ">")),
            0),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                Pageable.unpaged(),
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(0, "<")),
            0),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                Pageable.unpaged(),
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(0, "==")),
            0));
  }

  @ParameterizedTest
  @MethodSource("dataForTestGetTotalPages")
  <T> void testGetTotalPages(Page<T> page, long totalPagesCount) {
    assertEquals(totalPagesCount, page.getTotalPages());
  }

  static Stream<Arguments> dataForTestGetTotalElements() {
    return Stream.of(
        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), ">")),
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize() / MULTIPLIER),

        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "<")),
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize() * MULTIPLIER),

        Arguments.of(
            new TarantoolPageImpl<>(EMPTY_CONTENT,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "==")),
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), ">")),
            PERSONS.size()),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "<")),
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize() * MULTIPLIER),

        Arguments.of(
            new TarantoolPageImpl<>(PERSONS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                GET_TOTAL_ELEMENTS_FOR_TEST_DATA_CASE.apply(FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize(), "==")),
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()));
  }

  @ParameterizedTest
  @MethodSource("dataForTestGetTotalElements")
  <T> void testGetTotalElements(Page<T> page, long totalElementCount) {
    assertEquals(totalElementCount, page.getTotalElements());
  }

  static Stream<Arguments> dataForTestHasNext() {
    return Stream.of(
        Arguments.of(
            false,
            new TarantoolPageImpl<>(PERSONS, FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize())),
        Arguments.of(
            true,
            new TarantoolPageImpl<>(PERSONS, FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                MULTIPLIER * FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize())),
        Arguments.of(
            false,
            new TarantoolPageImpl<>(PERSONS, Pageable.unpaged(),
                MULTIPLIER * FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize())));
  }

  @ParameterizedTest
  @MethodSource("dataForTestHasNext")
  <T> void testHasNext(boolean exceptedBool, Page<T> page) {
    assertEquals(exceptedBool, page.hasNext());
  }

  @ParameterizedTest
  @MethodSource("dataForTestHasNext")
  <T> void testIsLast(boolean notEqualBool, Page<T> page) {
    assertNotEquals(notEqualBool, page.isLast());
  }

  static Stream<Arguments> dataForTestMap() {
    List<String> exceptedNames = PERSONS.stream().map(Person::getName).collect(Collectors.toList());
    return Stream.of(
        Arguments.of(
            new TarantoolPageImpl<>(PERSONS, FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
                FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()),
            exceptedNames,
            (Function<Person, String>) Person::getName));
  }

  @ParameterizedTest
  @MethodSource("dataForTestMap")
  <T> void testMap(Page<T> page, List<String> exceptedNames, Function<T, String> mapFunction) {
    List<String> names = page.map(mapFunction).toList();
    assertEquals(exceptedNames, names);
  }

  static Stream<Arguments> dataForTestEqualsAndHashCode() {
    List<Page<Person>> equalPages = Arrays.asList(
        new TarantoolPageImpl<>(PERSONS, FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()),
        new TarantoolPageImpl<>(PERSONS, FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()),
        new TarantoolPageImpl<>(PERSONS, FIRST_PAGE_PAGEABLE_PER_TEST_CLASS,
            FIRST_PAGE_PAGEABLE_PER_TEST_CLASS.getPageSize()));

    final int ITERATION_COUNT = 100;

    return Stream.of(
        Arguments.of(equalPages, new TarantoolPageImpl<>(EMPTY_CONTENT, Pageable.unpaged(), 150), ITERATION_COUNT),
        Arguments.of(equalPages, null, ITERATION_COUNT));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEqualsAndHashCode")
  <T> void testEqualsAndHashCode(List<Page<T>> pages, Page<T> notEqualPage, int iterationCount) {
    for (int i = 0; i < iterationCount; i++) {
      for (Page<T> page : pages) {
        for (Page<T> otherPage : pages) {
          assertEquals(page, otherPage);
          assertEquals(page.hashCode(), otherPage.hashCode());
        }
        assertNotEquals(page, notEqualPage);
        if (notEqualPage != null) {
          assertNotEquals(page.hashCode(), notEqualPage.hashCode());
        }
      }
    }
  }
}
