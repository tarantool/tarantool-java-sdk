/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol;

/** Tarantool iterator types for data selection. Used in {@code box.select} requests. */
public enum BoxIterator {

  /**
   * Matches keys equal to the provided search key (ASC order).
   *
   * <p>Binary value: {@code 0}
   *
   * @since Supported in all versions.
   */
  EQ(0),

  /**
   * Matches keys equal to the provided search key (DESC order).
   *
   * <p>Binary value: {@code 1}
   *
   * @since Supported in all versions.
   */
  REQ(1),

  /**
   * Iterates over all tuples in the index. The search key is ignored.
   *
   * <p>Binary value: {@code 2}
   *
   * @since Supported in all versions.
   */
  ALL(2),

  /**
   * Matches keys strictly less than the provided search key ({@code key < x}).
   *
   * <p>Binary value: {@code 3}
   *
   * @since Supported in all versions.
   */
  LT(3),

  /**
   * Matches keys less than or equal to the provided search key ({@code key <= x}).
   *
   * <p>Binary value: {@code 4}
   *
   * @since Supported in all versions.
   */
  LE(4),

  /**
   * Matches keys greater than or equal to the provided search key ({@code key >= x}).
   *
   * <p>Binary value: {@code 5}
   *
   * @since Supported in all versions.
   */
  GE(5),

  /**
   * Matches keys strictly greater than the provided search key ({@code key > x}).
   *
   * <p>Binary value: {@code 6}
   *
   * @since Supported in all versions.
   */
  GT(6),

  /**
   * Matches keys where all bits from the search key are set (<b>Bitset index only</b>).
   *
   * <p>Binary value: {@code 7}
   *
   * @since Supported in all versions.
   */
  BITS_ALL_SET(7),

  /**
   * Matches keys where at least one bit from the search key is set (<b>Bitset index only</b>).
   *
   * <p>Binary value: {@code 8}
   *
   * @since Supported in all versions.
   */
  BITS_ANY_SET(8),

  /**
   * Matches keys where all bits from the search key are not set (<b>Bitset index only</b>).
   *
   * <p>Binary value: {@code 9}
   *
   * @since Supported in all versions.
   */
  BITS_ALL_NOT_SET(9),

  /**
   * Matches keys that overlap with the provided search key (<b>RTREE index only</b>).
   *
   * <p>Binary value: {@code 10}
   *
   * @since Supported in all versions.
   */
  OVERLAPS(10),

  /**
   * Iterates over tuples as they move away from the specified point {@code x} (<b>RTREE neighbor
   * search</b>).
   *
   * <p>Binary value: {@code 11}
   *
   * @since Supported in all versions.
   */
  NEIGHBOR(11),

  /**
   * Next Prefix iterator.
   *
   * <p>Binary value: {@code 12}
   *
   * <p>Performs a prefix-based search on the last part of the key (if it is a string, ASC order).
   * It skips all keys sharing the same prefix as the search key and starts the sequential scan from
   * the first key whose prefix is strictly greater than the search key.
   *
   * <p>Degrades to {@link #GT} if the last part of the key is not a string. <b>Works only in {@code
   * memtx} tree indexes</b>.
   *
   * @since Tarantool 3.2.0
   */
  NP(12),

  /**
   * Previous Prefix iterator.
   *
   * <p>Binary value: {@code 13}
   *
   * <p>Performs a prefix-based search on the last part of the key (if it is a string, DESC order).
   * It looks for the last key whose prefix is strictly less than the search key, and then proceeds
   * with a sequential reverse-order scan.
   *
   * <p>Degrades to {@link #LT} if the last part of the key is not a string. <b>Works only in {@code
   * memtx} tree indexes</b>.
   *
   * @since Tarantool 3.2.0
   */
  PP(13);

  private final int code;

  BoxIterator(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
