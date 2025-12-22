/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.utils;


import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analogue of the {@code Pair} class in Spring Data, but allowing {@code null} values for the second element.
 *
 * @param <S>
 * @param <T>
 */
public final class Pair<S, T> {

  private final S first;
  private final T second;

  private Pair(S first, T second) {

    if (first == null) {
      throw new IllegalArgumentException("First must not be null");
    }
    this.first = first;
    this.second = second;
  }

  /**
   * Creates a new {@link Pair} for the given elements.
   *
   * @param first  must not be {@literal null}.
   * @param second can be {@literal null}.
   * @return {@link Pair} instance.
   */
  public static <S, T> Pair<S, T> of(S first, T second) {
    return new Pair<>(first, second);
  }

  /**
   * Returns the first element of the {@link Pair}.
   *
   * @return first element.
   */
  public S getFirst() {
    return first;
  }

  /**
   * Returns the second element of the {@link Pair}.
   *
   * @return second element. Can be {@code null}.
   */
  public T getSecond() {
    return second;
  }

  /**
   * A collector to create a {@link Map} from a {@link Stream} of {@link Pair}s.
   *
   * @return {@link  Collector} instance.
   */
  public static <S, T> Collector<Pair<S, T>, ?, Map<S, T>> toMap() {
    return Collectors.toMap(Pair::getFirst, Pair::getSecond);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Pair)) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return first.equals(pair.first) && Objects.equals(second, pair.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }

  @Override
  public String toString() {
    return String.format("%s->%s", this.first, this.second);
  }
}
