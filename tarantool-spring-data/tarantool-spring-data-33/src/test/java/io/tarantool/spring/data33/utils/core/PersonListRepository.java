/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.utils.core;

import org.springframework.data.repository.ListCrudRepository;

import io.tarantool.spring.data.utils.GenericListRepositoryMethods;
import io.tarantool.spring.data33.utils.entity.Person;

public interface PersonListRepository
    extends ListCrudRepository<Person, Integer>,
    GenericListRepositoryMethods<Person, Integer> {}
