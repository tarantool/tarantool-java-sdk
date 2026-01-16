/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.query;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.tarantool.spring.data.query.PaginationDirection;

/**
 * Abstract interface for pagination information in Tarantool.
 *
 * @param <T> domain class type.
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolPageable<T> extends Pageable {

  /**
   * Returns tuple cursor of domain type. Can be {@code null}. If the cursor is {@code null} then
   * the page is counted from the first tuple in Tarantool.
   *
   * @return tuple cursor of domain type.
   */
  @Nullable
  T getTupleCursor();

  /**
   * Returns the {@link TarantoolPageable} for previous page or the {@link TarantoolPageable} for
   * first page if the current one already is the first one. <b><i>Important: </i></b> this method
   * always returns {@link TarantoolPageable} c {@link PaginationDirection#BACKWARD}.
   *
   * @param tupleCursor tuple cursor of domain type for previous page.
   * @return {@link TarantoolPageable} instance.
   */
  TarantoolPageable<T> previousOrFirst(T tupleCursor);

  /**
   * Returns the {@link TarantoolPageable} requesting the next {@link
   * org.springframework.data.domain.Page}.
   *
   * <p><b><i>Important:</i></I></b> method always creates a new {@link TarantoolPageable} with the
   * same parameters Sort and page size. Pagination direction is {@link
   * PaginationDirection#FORWARD}, the page number always has value {@code n+1}, n is the current
   * page number.
   *
   * @param tupleCursor tuple cursor of domain type for next page.
   * @return {@link TarantoolPageable} object
   */
  TarantoolPageable<T> next(T tupleCursor);

  /**
   * Returns pagination direction by {@link PaginationDirection} enumeration.
   *
   * @return {@link PaginationDirection}.
   */
  PaginationDirection getPaginationDirection();

  /**
   * Returns a {@link TarantoolPageable} that points to the first page with {@link
   * PaginationDirection#FORWARD} pagination direction.
   *
   * @return {@link TarantoolPageable} which has {@code cursor == null}, page number is 0, the rest
   *     parameters are equivalent to the parameters of the current {@link TarantoolPageable}.
   */
  @NonNull
  TarantoolPageable<T> first();
}
