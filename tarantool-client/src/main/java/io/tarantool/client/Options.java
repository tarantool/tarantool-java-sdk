/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client;

/**
 * Provides a contract for basic client options.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface Options {

  /**
   * Returns timeout of operation.
   *
   * @return timeout value in milliseconds.
   */
  long getTimeout();

  /**
   * Returns stream id of operation.
   *
   * @return null - if stream id is null, otherwise - stream id value.
   */
  Long getStreamId();
}
