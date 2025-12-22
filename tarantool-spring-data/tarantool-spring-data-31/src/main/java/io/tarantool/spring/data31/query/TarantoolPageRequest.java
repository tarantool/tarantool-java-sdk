/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.query;

import java.io.Serial;
import java.util.Objects;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import static io.tarantool.spring.data.query.PaginationDirection.BACKWARD;
import static io.tarantool.spring.data.query.PaginationDirection.FORWARD;
import io.tarantool.spring.data.query.PaginationDirection;

/**
 * Basic implementation of {@link TarantoolPageable} for Tarantool.
 * <p><b><i>Important:</i></b> When specifying a cursor tuple, you must adhere to the following rules:</p>
 * <p>1. To specify a page starting from the beginning of {@code space} (the first element of the page is the first
 * element found in {@code space}), use {@code tupleCursor == null}.</p>
 * <p>2. To specify an arbitrary page (the first element of the page is the next/previous one from the given one
 * tuple cursor depending on the methods used and page number), use {@code tupleCursor == someTupleCursor} .</p>
 *
 * @param <T> domain entity type
 */
public final class TarantoolPageRequest<T> extends AbstractPageRequest implements TarantoolPageable<T> {

  @Serial
  private static final long serialVersionUID = -4541509938956089562L;

  private final Sort sort;

  private final T tupleCursor;

  private final PaginationDirection paginationDirection;

  /**
   * Creates a new unsorted {@link TarantoolPageRequest} from begin of {@code space}.
   *
   * @param pageSize the size of the page to be returned, must be greater than 0.
   */
  public TarantoolPageRequest(int pageSize) {
    this(0, pageSize, null);
  }

  /**
   * Creates a new unsorted {@link TarantoolPageRequest}.
   * <p><b><i>Important:</i></b> Use this method with caution! It is important to know the exact match of number
   * pages and the tuple after (before) which the page goes (without including this tuple in the page itself). A mistake
   * in compliance results in the appearance of blank pages with unpaged pageable even when using methods
   * {@link TarantoolPageImpl#previousOrFirstPageable()} / {@link TarantoolPageImpl#nextOrLastPageable()} and
   * {@link TarantoolSliceImpl#previousOrFirstPageable()} / {@link TarantoolSliceImpl#nextOrLastPageable()}.</p>
   *
   * @param page        zero-based page index (virtual), must not be negative.
   * @param size        the size of the page to be returned, must be greater than 0.
   * @param tupleCursor tuple cursor from which the page count begins. More: {@link TarantoolPageRequest}.
   */
  public TarantoolPageRequest(int page, int size, T tupleCursor) {
    this(page, size, Sort.unsorted(), tupleCursor, FORWARD);
  }

  /**
   * Private constructor to create an instance with a specific {@link PaginationDirection}.
   *
   * @param page        zero-based page index, must not be negative.
   * @param size        the size of the page to be returned, must be greater than 0.
   * @param sort        must not be {@literal null}, use {@link Sort#unsorted()} instead.
   * @param tupleCursor tuple cursor from which the page count begins. More: {@link TarantoolPageRequest}.
   * @param direction   direction of pagination.
   */
  TarantoolPageRequest(int page, int size, Sort sort, T tupleCursor, @NonNull PaginationDirection direction) {
    super(page, size);
    Assert.notNull(sort, "Sort must not be null");
    Assert.notNull(direction, "PaginationDirection must not be null");

    this.sort = sort;
    this.tupleCursor = tupleCursor;
    this.paginationDirection = direction;
  }

  /**
   * Non supported for Tarantool.
   */
  @Override
  @NonNull
  public TarantoolPageRequest<T> withPage(int pageNumber) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("method \"withPage(int pageNumber)\" unsupported");
  }

  @Override
  @NonNull
  public TarantoolPageable<T> first() {
    return new TarantoolPageRequest<>(getPageSize());
  }

  /**
   * Non supported for Tarantool. Use {@link #next(Object)}.
   */
  @Override
  @NonNull
  public TarantoolPageRequest<T> next() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("method \"next()\" unsupported, use next(T nextTupleCursor)");
  }

  @Override
  public TarantoolPageRequest<T> next(T tupleCursor) {
    return new TarantoolPageRequest<>(getPageNumber() + 1, getPageSize(), getSort(), tupleCursor, FORWARD);
  }

  /**
   * Non supported for Tarantool. Use {@link #previous(Object)}.
   */
  @Override
  @NonNull
  public TarantoolPageRequest<T> previous() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("method \"previous()\" unsupported, use previous(T prevTupleCursor)");
  }

  private TarantoolPageRequest<T> previous(T tupleCursor) {
    if (hasPrevious()) {
      return new TarantoolPageRequest<>(getPageNumber() - 1, getPageSize(), getSort(), tupleCursor, BACKWARD);
    }
    return this;
  }

  /**
   * Non supported for Tarantool. Use {@link #previousOrFirst(Object)}.
   */
  @Override
  @NonNull
  public TarantoolPageable<T> previousOrFirst() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "method \"previousOrFirst()\" unsupported, use previousOrFirst(T prevTupleCursor)");
  }

  @Override
  public TarantoolPageable<T> previousOrFirst(T tupleCursor) {
    if (hasPrevious()) {
      return previous(tupleCursor);
    }
    return new TarantoolPageRequest<>(0, getPageSize(), getSort(), null, BACKWARD);
  }

  @Override
  public PaginationDirection getPaginationDirection() {
    return this.paginationDirection;
  }

  @Override
  @NonNull
  public Sort getSort() {
    return this.sort;
  }

  @Override
  public T getTupleCursor() {
    return this.tupleCursor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TarantoolPageRequest<?> that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    return sort.equals(that.sort) && Objects.equals(tupleCursor, that.tupleCursor)
        && paginationDirection == that.paginationDirection;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), sort, tupleCursor, paginationDirection);
  }
}
