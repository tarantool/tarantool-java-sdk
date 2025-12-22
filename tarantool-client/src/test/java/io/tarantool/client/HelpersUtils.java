/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.tarantool.balancer.exceptions.NoAvailableClientsException;
import io.tarantool.mapping.crud.CrudException;
import io.tarantool.core.connection.exceptions.ConnectionClosedException;
import io.tarantool.core.connection.exceptions.ConnectionException;
import io.tarantool.core.exceptions.BoxError;
import io.tarantool.core.exceptions.ShutdownException;

public final class HelpersUtils {

  private HelpersUtils() {
  }

  public static Integer DEFAULT_RETRYING_ATTEMPTS = 5;
  public static Integer DEFAULT_RETRYING_DELAY = 100;

  public static void retry(Runnable fn) {
    retry(DEFAULT_RETRYING_ATTEMPTS, DEFAULT_RETRYING_DELAY, fn);
  }

  public static void retry(Integer attempts, Integer delay, Runnable fn) {
    while (attempts > 0) {
      try {
        fn.run();
        return;
      } catch (AssertionError ignored) {
      }

      attempts = spendAttempt(attempts, delay);
    }
    fn.run();
  }

  public static <T> T retryConnectionException(Supplier<T> fn) {
    return retryConnectionException(DEFAULT_RETRYING_ATTEMPTS, DEFAULT_RETRYING_DELAY, fn);
  }

  public static <T> T retryConnectionException(Integer attempts, Integer delay, Supplier<T> fn) {
    while (attempts > 0) {
      boolean needToRetry = false;
      try {
        return fn.get();
      } catch (CrudException exception) {
        if (exception.getReason().getClassName().equals("UpsertError")) {
          needToRetry = true;
        }
      } catch (CompletionException exception) {
        Throwable causeException = exception.getCause();

        if (causeException instanceof IllegalStateException ||
            causeException instanceof ConnectionClosedException) {
          String message = causeException.getMessage();
          if (message.equals("Connection closed by shutdown") ||
              message.equals("Connection closed by server") ||
              message.equals("Connection closed by client")) {
            needToRetry = true;
          }
        } else if (causeException instanceof ShutdownException &&
            causeException.getMessage().equals("Request finished by shutdown")) {
          needToRetry = true;
        } else if (causeException instanceof BoxError &&
            (exception.getMessage().contains("variable 'crud' is not declared") ||
                exception.getMessage().contains("attempt to index field 'space' (a nil value)"))) {
          needToRetry = true;
        } else if (causeException instanceof TimeoutException &&
            causeException.getMessage().contains("Request timeout: IProtoEval")) {
          needToRetry = true;
        } else if (causeException instanceof ConnectionException &&
            causeException.getMessage().contains("Failed to send IProto message")) {
          needToRetry = true;
        } else if (causeException instanceof NoAvailableClientsException) {
          needToRetry = true;
        }
      }

      if (!needToRetry) {
        break;
      }
      attempts = spendAttempt(attempts, delay);
    }
    return fn.get();
  }

  private static Integer spendAttempt(Integer attempts, Integer delay) {
    --attempts;
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return attempts;
  }
}
