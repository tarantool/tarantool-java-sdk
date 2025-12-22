/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.utils.entity;

import java.util.UUID;

import io.tarantool.spring.data.mapping.model.CompositeKey;

public class CompositeKeyWithFieldInvalidOrder implements CompositeKey {

  private UUID secondId;

  private Integer id;
}
