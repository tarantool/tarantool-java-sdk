/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.mapping.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/** This interface must be implemented by classes that are composite keys of domain entities. */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public interface CompositeKey {}
