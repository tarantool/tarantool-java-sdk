/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring4.data.utils.entity;

import java.util.UUID;

import io.tarantool.spring.data.mapping.model.CompositeKey;

public class CompositeKeyWithFieldInvalidOrder implements CompositeKey {

  private UUID secondId;

  private Integer id;
}
