/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.box.options;

import java.time.Duration;

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

  /** Default request timeout. */
  Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

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

  /** Returns stream id of operation. */
  Long getStreamId();

  /** Returns timeout of operation im milliseconds. */
  @Deprecated
  long getTimeout();

  /** Returns timeout of operation. */
  Duration timeout();
}
