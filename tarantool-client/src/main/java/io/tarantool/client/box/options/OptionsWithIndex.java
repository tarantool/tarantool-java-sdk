/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.box.options;

/**
 * Represents a contract for classes that implement options that work with the index.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see <a
 *     href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_index/">Tarantool
 *     documentation</a>
 */
public interface OptionsWithIndex {

  /**
   * Returns the id of the index.
   *
   * @return index id.
   */
  int getIndexId();

  /**
   * Returns the name of the index.
   *
   * @return index name.
   */
  String getIndexName();
}
