/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Wrapper around a resource that remembers how to release it.
 *
 * <p>Lets the creator decide if a resource is owned (and therefore closed) or external.
 *
 * @param <T> type of resource
 */
public final class ManagedResource<T> implements AutoCloseable {
  private final T resource;
  private final Consumer<T> releaser;

  private ManagedResource(T resource, Consumer<T> releaser) {
    this.resource = Objects.requireNonNull(resource, "resource must not be null");
    this.releaser = releaser;
  }

  /**
   * Creates a managed resource that will be released via the provided action.
   *
   * @param resource resource instance
   * @param releaser action that releases the resource
   * @param <T> type of resource
   * @return managed resource instance
   */
  public static <T> ManagedResource<T> owned(T resource, Consumer<T> releaser) {
    return new ManagedResource<>(
        resource, Objects.requireNonNull(releaser, "releaser must not be null"));
  }

  /**
   * Creates a managed resource that is owned by someone else and therefore will not be closed here.
   *
   * @param resource resource instance
   * @param <T> type of resource
   * @return managed resource instance
   */
  public static <T> ManagedResource<T> external(T resource) {
    return new ManagedResource<>(resource, null);
  }

  public T get() {
    return resource;
  }

  @Override
  public void close() {
    if (releaser != null) {
      releaser.accept(resource);
    }
  }
}
