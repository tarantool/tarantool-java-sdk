/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.utils.core;

import org.springframework.data.repository.ListCrudRepository;

import io.tarantool.spring.data.utils.GenericListRepositoryMethods;
import io.tarantool.spring.data32.utils.entity.Person;

public interface PersonListRepository
    extends ListCrudRepository<Person, Integer>, GenericListRepositoryMethods<Person, Integer> {}
