/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.core.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.tarantool.spring.data31.utils.entity.ComplexPerson;
import io.tarantool.spring.data31.utils.entity.CompositePersonKey;
import io.tarantool.spring.data31.utils.entity.EntityWithAnnotationAsIdClass;
import io.tarantool.spring.data31.utils.entity.Person;

@Timeout(5)
class IdClassResolverTest {

  public static final DefaultIdClassResolver ID_CLASS_RESOLVER = DefaultIdClassResolver.INSTANCE;

  @Test
  void testResolveIdClassTypeWithNullEntity() {

    final IllegalArgumentException throwable = assertThrows(IllegalArgumentException.class,
        () -> ID_CLASS_RESOLVER.resolveIdClassType(null));
    final String EXCEPTION_MESSAGE = "Type for IdClass must be not null!";
    assertEquals(EXCEPTION_MESSAGE, throwable.getMessage());
  }

  @Test
  void testResolveIdClassTypeWithEntityWithoutAnnotation() {
    assertNull(ID_CLASS_RESOLVER.resolveIdClassType(Person.class));
  }

  @Test
  void testResolveIdClassTypeWithEntityWithAnnotation() {
    assertEquals(ID_CLASS_RESOLVER.resolveIdClassType(ComplexPerson.class), CompositePersonKey.class);
  }

  @Test
  void testResolveIdClassTypeWithCrossAnnotation() {
    final IllegalArgumentException throwable =
        assertThrows(IllegalArgumentException.class,
            () -> ID_CLASS_RESOLVER.resolveIdClassType(EntityWithAnnotationAsIdClass.class));

    assertEquals(DefaultIdClassResolver.ANNOTATION_TYPE_EXCEPTION, throwable.getMessage());
  }
}
