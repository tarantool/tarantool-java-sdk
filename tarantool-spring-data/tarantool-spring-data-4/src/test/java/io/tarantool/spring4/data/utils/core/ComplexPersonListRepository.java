/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring4.data.utils.core;

import org.springframework.data.repository.ListCrudRepository;

import io.tarantool.spring.data.utils.GenericListRepositoryMethods;
import io.tarantool.spring4.data.utils.entity.ComplexPerson;
import io.tarantool.spring4.data.utils.entity.CompositePersonKey;

public interface ComplexPersonListRepository
    extends ListCrudRepository<ComplexPerson, CompositePersonKey>,
        GenericListRepositoryMethods<ComplexPerson, CompositePersonKey> {}
