/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.utils.core;

import org.springframework.data.repository.PagingAndSortingRepository;

import io.tarantool.spring.data.utils.GenericRepositoryMethods;
import io.tarantool.spring.data33.utils.entity.ComplexPerson;
import io.tarantool.spring.data33.utils.entity.CompositePersonKey;

public interface ComplexPersonRepository
    extends PagingAndSortingRepository<ComplexPerson, CompositePersonKey>,
    GenericRepositoryMethods<ComplexPerson, CompositePersonKey>,
    GenericPaginationMethods<ComplexPerson, CompositePersonKey> {}
