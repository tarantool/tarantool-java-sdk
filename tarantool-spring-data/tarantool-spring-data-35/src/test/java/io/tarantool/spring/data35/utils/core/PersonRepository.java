/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.utils.core;

import org.springframework.data.repository.PagingAndSortingRepository;

import io.tarantool.spring.data.utils.GenericRepositoryMethods;
import io.tarantool.spring.data35.utils.entity.Person;
import io.tarantool.spring.data35.utils.fragments.core.ReplaceManyFragment;

public interface PersonRepository
    extends PagingAndSortingRepository<Person, Integer>,
        GenericRepositoryMethods<Person, Integer>,
        GenericPaginationMethods<Person, Integer>,
        ReplaceManyFragment<Person> {}
