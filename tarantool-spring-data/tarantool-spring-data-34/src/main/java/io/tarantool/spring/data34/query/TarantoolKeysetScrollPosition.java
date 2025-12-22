/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static io.tarantool.spring.data.query.PaginationDirection.BACKWARD;
import static io.tarantool.spring.data.query.PaginationDirection.FORWARD;
import io.tarantool.spring.data.query.PaginationDirection;
import io.tarantool.spring.data.utils.Pair;

final class TarantoolKeysetScrollPosition implements TarantoolScrollPosition {

  private final Pair<String, ?> indexKey;

  private final PaginationDirection direction;

  private final Object cursor;

  TarantoolKeysetScrollPosition(Pair<String, ?> indexKey, PaginationDirection direction, @Nullable Object cursor) {

    Assert.notNull(direction, "PaginationDirection must not be null");
    Assert.notNull(indexKey, "indexKey must not be null");

    this.indexKey = indexKey;
    this.direction = direction;
    this.cursor = cursor;
  }

  /**
   * Creates a new {@link TarantoolKeysetScrollPosition} from a key set and {@link PaginationDirection}.
   *
   * @param indexKey must not be {@literal null}.
   * @return will never be {@literal null}.
   */
  static TarantoolScrollPosition forward(Pair<String, ?> indexKey) {
    return new TarantoolKeysetScrollPosition(indexKey, FORWARD, null);
  }

  static TarantoolScrollPosition backward(Pair<String, ?> indexKey) {
    return new TarantoolKeysetScrollPosition(indexKey, BACKWARD, null);
  }

  /**
   * Returns whether the current scroll position is the initial one (from begin or from end) (see
   * {@link PaginationDirection}).
   *
   * @return {@link Boolean} object.
   */
  @Override
  public boolean isInitial() {
    if (indexKey.getSecond() instanceof List<?> startingList) {
      return startingList.isEmpty() && cursor == null;
    }
    return false;
  }

  @Override
  public TarantoolScrollPosition reverse() {
    Pair<String, ?> newIndexKey = Pair.of(indexKey.getFirst(), indexKey.getSecond());
    return new TarantoolKeysetScrollPosition(newIndexKey, direction.reverse(), cursor);
  }

  @Override
  public boolean isScrollsBackward() {
    return direction.equals(BACKWARD);
  }

  /**
   * Return the cursor relative to which the data is being found.
   *
   * @return returns cursor
   */
  Object getCursor() {
    return cursor;
  }

  /**
   * @return the scroll direction.
   */
  PaginationDirection getDirection() {
    return direction;
  }

  /**
   * @return the indexKey.
   */
  Pair<String, ?> getIndexKey() {
    return indexKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TarantoolKeysetScrollPosition that)) {
      return false;
    }
    return indexKey.equals(that.indexKey) && direction == that.direction && Objects.equals(cursor, that.cursor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indexKey, direction, cursor);
  }

  @Override
  public String toString() {
    return String.format("TarantoolKeysetScrollPosition [%s, %s, %s]", direction, indexKey, cursor);
  }
}
