/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

/**
 * A general functional interface for Consumer-like callbacks with three arguments.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 */
@FunctionalInterface
public interface TripleConsumer<A, B, C> {

  void accept(A a, B b, C c);
}
