/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.utils.core;

import org.springframework.data.repository.ListCrudRepository;

import io.tarantool.spring.data.utils.GenericListRepositoryMethods;
import io.tarantool.spring.data31.utils.entity.Person;

public interface PersonListRepository
    extends ListCrudRepository<Person, Integer>, GenericListRepositoryMethods<Person, Integer> {}
