/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

/**
 * <p>Provides a contract for basic client options.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface Options {

  /**
   * <p> Returns timeout of operation.</p>
   *
   * @return timeout value in milliseconds.
   */
  long getTimeout();

  /**
   * <p> Returns stream id of operation.</p>
   *
   * @return null - if stream id is null, otherwise - stream id value.
   */
  Long getStreamId();
}
