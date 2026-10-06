/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.time.Duration;

/**
 * Provides a contract for basic client options.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface Options {

  /** Default request timeout. */
  Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  /**
   * Returns timeout of operation.
   *
   * @return timeout value in milliseconds.
   */
  @Deprecated
  long getTimeout();

  /**
   * Returns timeout of operation.
   *
   * @return timeout value.
   */
  Duration timeout();

  /**
   * Returns stream id of operation.
   *
   * @return null - if stream id is null, otherwise - stream id value.
   */
  Long getStreamId();
}
