/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.springframework.data.domain.Window;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * The class is a complete analogue of {@link org.springframework.data.support.WindowIterator} for
 * Tarantool.
 *
 * <p>Example of usage:
 *
 * <blockquote>
 *
 * <pre>{@code
 * TarantoolScrollPosition initialScrollPosition =
 *     TarantoolScrollPosition.forward(Pair.of("pk", Collections.emptyList()));
 *
 * TarantoolWindowIterator<Person> personIterator =
 *     TarantoolWindowIterator.of(scrollPosition -> repository.findFirst10ByAge(10, scrollPosition))
 *         .startingAt(initialScrollPosition);
 *
 * List<Person> result = new ArrayList<>();
 *
 * while (personIterator.hasNext()) {
 *   result.add(personIterator.next());
 * }
 * }</pre>
 *
 * </blockquote>
 *
 * @param <T> domain class type.
 */
public class TarantoolWindowIterator<T> implements Iterator<T> {

  private final Function<TarantoolScrollPosition, Window<T>> windowFunction;

  private TarantoolScrollPosition currentPosition;

  private @Nullable Window<T> currentWindow;

  private @Nullable Iterator<T> currentIterator;

  /**
   * Entrypoint to create a new {@link TarantoolWindowIterator} for the given windowFunction.
   *
   * @param windowFunction must not be {@literal null}.
   * @param <T> domain class type.
   * @return new instance of {@link TarantoolWindowIteratorBuilder}.
   */
  public static <T> TarantoolWindowIteratorBuilder<T> of(
      Function<TarantoolScrollPosition, Window<T>> windowFunction) {
    return new TarantoolWindowIteratorBuilder<>(windowFunction);
  }

  TarantoolWindowIterator(
      Function<TarantoolScrollPosition, Window<T>> windowFunction,
      TarantoolScrollPosition position) {

    this.windowFunction = windowFunction;
    this.currentPosition = position;
  }

  @Override
  public boolean hasNext() {

    // use while loop instead of recursion to fetch the next window.
    do {
      if (currentWindow == null) {
        currentWindow = windowFunction.apply(currentPosition);
      }

      if (currentIterator == null) {
        if (currentWindow != null) {
          currentIterator = currentWindow.iterator();
        }
      }

      if (currentIterator != null) {

        if (currentIterator.hasNext()) {
          return true;
        }

        if (currentWindow != null && currentWindow.hasNext()) {

          currentPosition = getNextPosition(currentWindow);
          currentIterator = null;
          currentWindow = null;
          continue;
        }
      }

      return false;
    } while (true);
  }

  @Override
  public T next() {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return currentIterator.next();
  }

  private static TarantoolScrollPosition getNextPosition(Window<?> window) {
    return (TarantoolScrollPosition) window.positionAt(window.size() - 1);
  }

  /** Builder API to construct a {@link TarantoolWindowIterator}. */
  public static class TarantoolWindowIteratorBuilder<T> {

    private final Function<TarantoolScrollPosition, Window<T>> windowFunction;

    TarantoolWindowIteratorBuilder(Function<TarantoolScrollPosition, Window<T>> windowFunction) {

      Assert.notNull(windowFunction, "WindowFunction must not be null");

      this.windowFunction = windowFunction;
    }

    /**
     * Create a {@link TarantoolWindowIterator} given {@link TarantoolScrollPosition}.
     *
     * @param position {@link TarantoolScrollPosition} instance. Must be not null.
     * @return {@link TarantoolWindowIterator} instance.
     */
    public TarantoolWindowIterator<T> startingAt(TarantoolScrollPosition position) {

      Assert.notNull(position, "TarantoolScrollPosition must not be null");

      return new TarantoolWindowIterator<>(windowFunction, position);
    }
  }
}
