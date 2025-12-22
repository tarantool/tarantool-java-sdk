/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@EqualsAndHashCode
public class Group {

  private Map<String, Object> app;
  private Map<String, Object> sharding;
  private Map<String, Object> replication;
  private Map<String, Replicaset> replicasets;
}
