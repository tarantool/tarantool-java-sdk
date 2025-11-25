/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.core.mapping.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.AbstractMappingContext;

import io.tarantool.spring.data27.core.mapping.TarantoolMappingContext;
import io.tarantool.spring.data27.utils.entity.ComplexPerson;
import io.tarantool.spring.data27.utils.entity.CompositePersonKey;

class CompositeIdPropertyAccessorTest {

  @Test
  void testGetIdentifier() {
    final AbstractMappingContext<?, ?> mappingContext = new TarantoolMappingContext<>();

    final Set<Class<?>> entitySet =
        new HashSet<Class<?>>() {
          {
            add(ComplexPerson.class);
          }
        };
    mappingContext.setInitialEntitySet(entitySet);
    mappingContext.initialize();

    final ComplexPerson target = new ComplexPerson(0, UUID.randomUUID(), true, "0");

    final PersistentEntity<?, ?> persistentEntity =
        mappingContext.getRequiredPersistentEntity(ComplexPerson.class);
    final IdentifierAccessor identifierAccessor = persistentEntity.getIdentifierAccessor(target);

    final CompositePersonKey expectedCompositeKey =
        new CompositePersonKey(target.getId(), target.getSecondId());

    assertNotNull(identifierAccessor.getIdentifier());
    Object identifier = assertDoesNotThrow(identifierAccessor::getRequiredIdentifier);
    assertEquals(expectedCompositeKey, identifier);
  }
}
