/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.core.annotation;

/**
 * Resolver to determine whether a domain class is marked with the {@code @IdClass} annotation.
 *
 * @author nikolai.belonogov
 */
public interface IdClassResolver {

  /**
   * Determine the {@literal IdClass type} to use for a given type.
   *
   * @param type must not be {@literal null}.
   * @return {@link Class} of composite id class.
   */
  Class<?> resolveIdClassType(Class<?> type);
}
