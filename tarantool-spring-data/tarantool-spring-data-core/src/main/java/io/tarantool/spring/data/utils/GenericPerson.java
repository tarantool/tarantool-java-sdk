/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.utils;

public interface GenericPerson<ID> {

  Integer getId();

  Boolean getIsMarried();

  String getName();

  void setId(Integer id);

  void setName(String name);

  void setIsMarried(Boolean isMarried);

  ID generateFullKey();
}
