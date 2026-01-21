/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.utils.core;

import org.springframework.data.repository.ListCrudRepository;

import io.tarantool.spring.data.utils.GenericListRepositoryMethods;
import io.tarantool.spring.data35.utils.entity.ComplexPerson;
import io.tarantool.spring.data35.utils.entity.CompositePersonKey;

public interface ComplexPersonListRepository
    extends ListCrudRepository<ComplexPerson, CompositePersonKey>,
        GenericListRepositoryMethods<ComplexPerson, CompositePersonKey> {}
