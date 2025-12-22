/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.utils.core;

import org.springframework.data.repository.PagingAndSortingRepository;

import io.tarantool.spring.data.utils.GenericRepositoryMethods;
import io.tarantool.spring.data27.utils.entity.Person;

public interface PersonRepository
    extends PagingAndSortingRepository<Person, Integer>, GenericRepositoryMethods<Person, Integer>,
    GenericPaginationMethods<Person, Integer> {}
