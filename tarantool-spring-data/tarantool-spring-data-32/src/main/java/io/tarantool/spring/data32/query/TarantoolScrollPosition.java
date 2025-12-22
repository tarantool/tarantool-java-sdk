/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.query;

import org.springframework.data.domain.ScrollPosition;

import io.tarantool.spring.data.utils.Pair;

/**
 * Use keyset pagination adapted for Tarantool crud module..
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see #forward(Pair)
 * @see #backward(Pair)
 * @see #reverse()
 */
public sealed interface TarantoolScrollPosition extends ScrollPosition
    permits TarantoolKeysetScrollPosition {

  /**
   * Creates a new {@link ScrollPosition} based on the key index scrolling forward.
   *
   * @param indexKey pair, where the first element is the name of the index on which you want to
   *     perform pagination. Second element is the value of the indexed field from which you want to
   *     paginate (exclusive). To perform pagination from the beginning of the index, pass an empty
   *     list as the second element of the pair. <b>indexKey must be not {@code null}</b>.
   * @return new {@link TarantoolScrollPosition} instance.
   */
  static TarantoolScrollPosition forward(Pair<String, ?> indexKey) {
    return TarantoolKeysetScrollPosition.forward(indexKey);
  }

  /**
   * Creates a new {@link ScrollPosition} based on the key index scrolling backward.
   *
   * @param indexKey pair, where the first element is the name of the index on which you want to
   *     perform pagination. Second element is the value of the indexed field from which you want to
   *     paginate (exclusive). To perform pagination from the end of the index, pass an empty list
   *     as the second element of the pair. <b>indexKey must be not {@code null}</b>.
   * @return new {@link TarantoolScrollPosition} instance.
   */
  static TarantoolScrollPosition backward(Pair<String, ?> indexKey) {
    return TarantoolKeysetScrollPosition.backward(indexKey);
  }

  /**
   * Return {@link TarantoolScrollPosition} with the same parameters, but with the opposite
   * pagination direction. If the current {@link TarantoolScrollPosition} was set from the beginning
   * of the index, the method will return the {@link TarantoolScrollPosition} set from the end of
   * the index. Returns the {@link TarantoolScrollPosition} set from the beginning of the index if
   * the current {@link TarantoolScrollPosition} is set from the end of the index.
   *
   * <p><b><i>Important:</i></b> when this method is called, the condition specified by the index in
   * the current {@link TarantoolScrollPosition} is overridden to unconditional (i.e. it will be
   * searched to the end or beginning of the index)
   *
   * @return {@link TarantoolScrollPosition} instance.
   */
  TarantoolScrollPosition reverse();

  /**
   * Return {@code true} if scrolling has backward motion. This means that the current {@link
   * TarantoolScrollPosition} has the direction of movement {@link
   * io.tarantool.spring.data.query.PaginationDirection#BACKWARD}. Return {@code false} if scrolling
   * has forward motion ({@link io.tarantool.spring.data.query.PaginationDirection#FORWARD}).
   *
   * @return {@link Boolean} primitive.
   */
  boolean isScrollsBackward();
}
