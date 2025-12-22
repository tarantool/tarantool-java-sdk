/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.utils;

import java.util.List;

public interface GenericListRepositoryMethods<T, ID> extends GenericRepositoryMethods<T, ID> {

  <S extends T> List<S> saveAll(Iterable<S> entities);

  List<T> findAll();

  List<T> findAllById(Iterable<ID> ids);
}
