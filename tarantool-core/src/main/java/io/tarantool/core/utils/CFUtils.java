/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.utils;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/** A class containing utility methods to work with {@link CompletionStage}. */
public final class CFUtils {

  private CFUtils() {}

  public static <T> void linkFuture(CompletionStage<T> root, CompletionStage<T> linkable) {
    if (linkable == null) {
      return;
    }

    root.whenComplete(
        (r, e) -> {
          if (e == null) {
            linkable.toCompletableFuture().complete(r);
          } else {
            final Throwable unwrap = unwrap(e);
            linkable.toCompletableFuture().completeExceptionally(unwrap);
          }
        });
  }

  public static Throwable unwrap(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    Throwable current = throwable;
    while ((current instanceof CompletionException || current instanceof ExecutionException)
        && current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }
}
