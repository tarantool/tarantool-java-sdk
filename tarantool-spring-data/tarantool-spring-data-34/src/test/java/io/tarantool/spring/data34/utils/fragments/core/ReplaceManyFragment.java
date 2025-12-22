/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.utils.fragments.core;

import java.util.List;

public interface ReplaceManyFragment<T> {

  List<T> replaceMany(List<T> tuples);
}
