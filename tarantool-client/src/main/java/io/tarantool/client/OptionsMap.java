/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.Map;

public interface OptionsMap extends Options {

  /**
   * The method returns a map whose keys are the names of options, and whose values are the values
   * of options. Example: for options method can return follow map:
   *
   * <blockquote>
   *
   * <pre>{@code
   * {
   *     "bucket_id": 5 (number -> int | cdata -> null),
   *     ...
   *     "fields": ["field_name1", "field_name2", "field_name3", ...] (table -> list)
   * }
   *
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link Map} object. Keys - option name, values - option value.
   */
  Map<String, Object> getOptions();
}
