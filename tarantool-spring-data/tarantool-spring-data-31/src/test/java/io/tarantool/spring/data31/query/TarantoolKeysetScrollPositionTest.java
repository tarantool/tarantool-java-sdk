/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.ScrollPosition;

import static io.tarantool.spring.data.query.PaginationDirection.BACKWARD;
import static io.tarantool.spring.data.query.PaginationDirection.FORWARD;
import io.tarantool.spring.data.query.PaginationDirection;
import io.tarantool.spring.data.utils.Pair;

class TarantoolKeysetScrollPositionTest {

  private static final Pair<String, ?> INDEX_KEY = Pair.of("pk", 123);

  private static final Pair<String, ?> INDEX_KEY_WITH_NULL_VALUE = Pair.of("pk", null);

  static Stream<Executable> dataForTestTarantoolScrollPositionCreateDoesntThrow() {
    return Stream.of(
        () -> TarantoolScrollPosition.forward(INDEX_KEY),
        () -> TarantoolScrollPosition.forward(INDEX_KEY_WITH_NULL_VALUE),
        () -> TarantoolScrollPosition.backward(INDEX_KEY),
        () -> TarantoolScrollPosition.backward(INDEX_KEY_WITH_NULL_VALUE));
  }

  @ParameterizedTest
  @MethodSource("dataForTestTarantoolScrollPositionCreateDoesntThrow")
  void testTarantoolScrollPositionCreateDoesntThrow(Executable scrollPositionCreateAction) {
    assertDoesNotThrow(scrollPositionCreateAction);
  }

  static Stream<Executable> dataForTestTarantoolScrollPositionCreateThrows() {
    return Stream.of(
        () -> TarantoolScrollPosition.forward(null),
        () -> TarantoolScrollPosition.backward(null));
  }

  @ParameterizedTest
  @MethodSource("dataForTestTarantoolScrollPositionCreateThrows")
  void testTarantoolScrollPositionCreateThrows(Executable scrollPositionCreateAction) {
    assertThrows(IllegalArgumentException.class, scrollPositionCreateAction);
  }

  static Stream<Arguments> dataForTestEqualsAndHashCode() {

    List<ScrollPosition> equalScrollPositionsWithKey = Arrays.asList(
        TarantoolScrollPosition.forward(INDEX_KEY),
        TarantoolScrollPosition.forward(INDEX_KEY));

    TarantoolScrollPosition notEqualScrollPosition = TarantoolScrollPosition.forward(INDEX_KEY_WITH_NULL_VALUE);

    final int ITERATION_COUNT = 100;

    return Stream.of(
        Arguments.of(equalScrollPositionsWithKey, notEqualScrollPosition, ITERATION_COUNT),
        Arguments.of(equalScrollPositionsWithKey, null, ITERATION_COUNT));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEqualsAndHashCode")
  void testEqualsAndHashCode(List<ScrollPosition> scrollPositions, ScrollPosition notEqualScrollPosition,
      int iterationCount) {
    for (int i = 0; i < iterationCount; i++) {
      for (ScrollPosition scrollPosition : scrollPositions) {
        for (ScrollPosition otherScrollPosition : scrollPositions) {
          assertEquals(scrollPosition, otherScrollPosition);
          assertEquals(scrollPosition.hashCode(), otherScrollPosition.hashCode());
        }
        assertNotEquals(scrollPosition, notEqualScrollPosition);
        if (notEqualScrollPosition != null) {
          assertNotEquals(scrollPosition.hashCode(), notEqualScrollPosition.hashCode());
        }
      }
    }
  }

  static Stream<Arguments> dataForTestIsInitial() {
    var startingIndex = Pair.of("pk", Collections.emptyList());
    var complexIndex = Pair.of("pk", Arrays.asList(1, UUID.randomUUID()));
    Object nonNullCursor = Arrays.asList(1, 2, 3, 4);
    return Stream.of(
        Arguments.of(
            TarantoolScrollPosition.backward(INDEX_KEY),
            false),
        Arguments.of(
            TarantoolScrollPosition.forward(INDEX_KEY),
            false),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, FORWARD, null),
            false),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, BACKWARD, null),
            false),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, FORWARD, nonNullCursor),
            false),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, BACKWARD, nonNullCursor),
            false),

        Arguments.of(
            TarantoolScrollPosition.backward(startingIndex),
            true),
        Arguments.of(
            TarantoolScrollPosition.forward(startingIndex),
            true),
        Arguments.of(
            new TarantoolKeysetScrollPosition(startingIndex, FORWARD, null),
            true),
        Arguments.of(
            new TarantoolKeysetScrollPosition(startingIndex, BACKWARD, null),
            true),
        Arguments.of(
            new TarantoolKeysetScrollPosition(startingIndex, FORWARD, nonNullCursor),
            false),
        Arguments.of(
            new TarantoolKeysetScrollPosition(startingIndex, BACKWARD, nonNullCursor),
            false),

        Arguments.of(
            TarantoolScrollPosition.backward(complexIndex),
            false),
        Arguments.of(
            TarantoolScrollPosition.forward(complexIndex),
            false));
  }

  @ParameterizedTest
  @MethodSource("dataForTestIsInitial")
  void testIsInitial(ScrollPosition scrollPosition, boolean expectedIsInitial) {
    assertEquals(scrollPosition.isInitial(), expectedIsInitial);
  }

  static Stream<Arguments> dataForTestGetCursor() {
    var cursor = String.valueOf(2);
    return Stream.of(
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, FORWARD, null),
            null),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, FORWARD, cursor),
            cursor));
  }

  @ParameterizedTest
  @MethodSource("dataForTestGetCursor")
  void testGetCursor(TarantoolKeysetScrollPosition scrollPosition, Object expectedCursor) {
    assertEquals(expectedCursor, scrollPosition.getCursor());
  }

  static Stream<Arguments> dataForTestGetKeyIndex() {
    return Stream.of(
        Arguments.of(
            TarantoolScrollPosition.forward(INDEX_KEY),
            INDEX_KEY),
        Arguments.of(
            TarantoolScrollPosition.backward(INDEX_KEY),
            INDEX_KEY));
  }

  @ParameterizedTest
  @MethodSource("dataForTestGetKeyIndex")
  void testGetKeyIndex(TarantoolKeysetScrollPosition scrollPosition, Pair<String, ?> expectedIndexKey) {
    assertEquals(expectedIndexKey, scrollPosition.getIndexKey());
  }

  static Stream<Arguments> dataForTestGetDirection() {
    return Stream.of(
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, FORWARD, null),
            FORWARD),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, BACKWARD, String.valueOf(2)),
            BACKWARD));
  }

  @ParameterizedTest
  @MethodSource("dataForTestGetDirection")
  void testGetDirection(TarantoolKeysetScrollPosition scrollPosition, PaginationDirection expectedDirection) {
    assertEquals(expectedDirection, scrollPosition.getDirection());
  }

  static Stream<Arguments> dataForTestReverse() {
    Object someKey = String.valueOf(1);
    var indexKeyAfterReverse = Pair.of(INDEX_KEY.getFirst(), INDEX_KEY.getSecond());

    return Stream.of(
        Arguments.of(
            TarantoolScrollPosition.forward(INDEX_KEY),
            TarantoolScrollPosition.backward(indexKeyAfterReverse)),
        Arguments.of(
            new TarantoolKeysetScrollPosition(INDEX_KEY, FORWARD, someKey),
            new TarantoolKeysetScrollPosition(indexKeyAfterReverse, BACKWARD, someKey)));
  }

  @ParameterizedTest
  @MethodSource("dataForTestReverse")
  void testGetReverse(TarantoolScrollPosition scrollPosition, TarantoolKeysetScrollPosition expectedReversedPosition) {
    assertEquals(expectedReversedPosition, scrollPosition.reverse());
  }

  static Stream<Arguments> dataForTestIsScrollsBackward() {
    return Stream.of(
        Arguments.of(
            TarantoolScrollPosition.forward(Pair.of("pk", Collections.emptyList())),
            false),
        Arguments.of(
            TarantoolScrollPosition.backward(Pair.of("pk", Collections.emptyList())),
            true));
  }

  @ParameterizedTest
  @MethodSource("dataForTestIsScrollsBackward")
  void testIsScrollsBackward(TarantoolScrollPosition position, boolean expectedIsScrollsBackward) {
    assertEquals(expectedIsScrollsBackward, position.isScrollsBackward());
  }
}
