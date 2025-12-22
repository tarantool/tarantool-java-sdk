/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.utils;

import java.util.List;

public interface GenericListRepositoryMethods<T, ID> extends GenericRepositoryMethods<T, ID> {

  <S extends T> List<S> saveAll(Iterable<S> entities);

  List<T> findAll();

  List<T> findAllById(Iterable<ID> ids);
}
