/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.utils.core;

import org.springframework.data.repository.PagingAndSortingRepository;

import io.tarantool.spring.data.utils.GenericRepositoryMethods;
import io.tarantool.spring.data32.utils.entity.ComplexPerson;
import io.tarantool.spring.data32.utils.entity.CompositePersonKey;

public interface ComplexPersonRepository
    extends PagingAndSortingRepository<ComplexPerson, CompositePersonKey>,
        GenericRepositoryMethods<ComplexPerson, CompositePersonKey>,
        GenericPaginationMethods<ComplexPerson, CompositePersonKey> {}
