/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type SliceQueueCfg struct {
	// StorageSize defines the number of messages retained in the queue.
	StorageSize int `mapstructure:"storage_size"`
}
 */
public class SliceQueueCfg {

  @JsonProperty("storage_size")
  private final Integer storageSize;

  @JsonCreator
  public SliceQueueCfg(@JsonProperty("storage_size") Integer storageSize) {
    this.storageSize = storageSize;
  }

  public Optional<Integer> getStorageSize() {
    return Optional.ofNullable(storageSize);
  }
}
