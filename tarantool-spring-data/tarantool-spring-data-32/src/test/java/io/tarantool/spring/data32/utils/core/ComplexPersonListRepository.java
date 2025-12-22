/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data32.utils.core;

import org.springframework.data.repository.ListCrudRepository;

import io.tarantool.spring.data.utils.GenericListRepositoryMethods;
import io.tarantool.spring.data32.utils.entity.ComplexPerson;
import io.tarantool.spring.data32.utils.entity.CompositePersonKey;

public interface ComplexPersonListRepository
    extends ListCrudRepository<ComplexPerson, CompositePersonKey>,
    GenericListRepositoryMethods<ComplexPerson, CompositePersonKey> {}
