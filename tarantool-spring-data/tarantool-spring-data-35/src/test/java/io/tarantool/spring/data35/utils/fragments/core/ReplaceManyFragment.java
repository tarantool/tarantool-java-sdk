/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.utils.fragments.core;

import java.util.List;

public interface ReplaceManyFragment<T> {

  List<T> replaceMany(List<T> tuples);
}
