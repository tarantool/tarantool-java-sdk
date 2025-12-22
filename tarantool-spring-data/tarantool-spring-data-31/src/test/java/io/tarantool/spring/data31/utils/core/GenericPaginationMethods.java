/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.utils.core;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;

public interface GenericPaginationMethods<T, ID> {

  Slice<T> findAllByName(String name);

  Slice<T> findAllByName(String name, Pageable pageable);

  Slice<T> findAllByNameGreaterThanEqual(String name, Pageable pageable);

  Slice<T> findAllByNameLessThanEqual(String name, Pageable pageable);

  Slice<T> findAllByIdLessThanEqual(int id, Pageable pageable);

  Slice<T> findAllByIdGreaterThanEqual(int id, Pageable pageable);

  Slice<T> findAllByIsMarriedLessThanEqual(Boolean id, Pageable pageable);

  Slice<T> findAllByIsMarriedGreaterThanEqual(Boolean id, Pageable pageable);

  Page<T> findPersonByName(String name, Pageable pageable);

  Page<T> findPersonByNameGreaterThanEqual(String name, Pageable pageable);

  Page<T> findPersonByNameLessThanEqual(String name, Pageable pageable);

  Page<T> findPersonByIdLessThanEqual(int id, Pageable pageable);

  Page<T> findPersonByIdGreaterThanEqual(int id, Pageable pageable);

  Page<T> findPersonByIsMarriedLessThanEqual(Boolean id, Pageable pageable);

  Page<T> findPersonByIsMarriedGreaterThanEqual(Boolean id, Pageable pageable);

  Window<T> findFirst5ByIsMarried(Boolean isMarried, ScrollPosition scrollPosition);

  Window<T> findFirst10ByIsMarriedGreaterThanEqual(Boolean isMarried, ScrollPosition scrollPosition);

  List<T> findAll(Sort sort);

  Page<T> findAll(Pageable pageable);

  List<T> findAll();
}
