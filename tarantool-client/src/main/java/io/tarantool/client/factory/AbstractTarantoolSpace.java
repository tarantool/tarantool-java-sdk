/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import io.tarantool.mapping.JacksonMappingException;
import io.tarantool.mapping.TarantoolJacksonMapping;

/**
 * Abstract class for all implementation.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
abstract class AbstractTarantoolSpace {

  /**
   * The method extracts tuple from the returned list with single tuple. If the list is empty or
   * null, then null will be returned.
   *
   * @param multiReturnResultList the result of operations as a list of tuples
   * @param <T> data type of the tuple representation (custom type)
   * @return a tuple as custom type or null
   * @throws JacksonMappingException if for some reason a list with many tuples is returned
   */
  protected <T> T getFirstOrNullForReturnAsClass(List<T> multiReturnResultList)
      throws JacksonMappingException {
    if (multiReturnResultList == null || multiReturnResultList.isEmpty()) {
      return null;
    }

    if (multiReturnResultList.size() == 1) {
      return multiReturnResultList.get(0);
    }

    throw new JacksonMappingException(
        "This method should return one tuple or null, but it returned "
            + multiReturnResultList.size());
  }

  /**
   * The method extracts tuple from the returned list with single tuple. If the list is empty or
   * null, then null will be returned.
   *
   * @param multiReturnResultList the result of operations as a list of tuples
   * @return a tuple as list of Java objects or null
   * @throws JacksonMappingException if for some reason a list with many tuples is returned
   */
  protected List<?> getFirstOrNullForReturnAsList(List<List<?>> multiReturnResultList)
      throws JacksonMappingException {
    if (multiReturnResultList == null || multiReturnResultList.isEmpty()) {
      return null;
    }

    if (multiReturnResultList.size() == 1) {
      return multiReturnResultList.get(0);
    }

    throw new JacksonMappingException(
        "This method should return one tuple or null, but it returned "
            + multiReturnResultList.size());
  }

  /**
   * The method allows to create a single parameterized type from those specified in the input
   * arguments.
   *
   * @param externalType a type that will be parameterized by the {@code internalType} type
   * @param internalType type with which the type {@code externalType} will be parameterized
   * @param <T> type of {@code internalType}
   * @param <E> type of {@code externalType}
   * @return {@link JavaType} parameterized type
   */
  protected <T, E> JavaType wrapIntoType(Class<E> externalType, TypeReference<T> internalType) {
    return TarantoolJacksonMapping.wrapIntoType(externalType, internalType);
  }

  /**
   * The method allows to create a single parameterized type from those specified in the input
   * arguments.
   *
   * @param externalType a type that will be parameterized by the {@code internalType} type
   * @param internalType type with which the type {@code externalType} will be parameterized
   * @param <E> type of {@code externalType}
   * @return {@link JavaType} parameterized type
   */
  protected <E> JavaType wrapIntoType(Class<E> externalType, JavaType internalType) {
    return TarantoolJacksonMapping.wrapIntoType(externalType, internalType);
  }

  /**
   * Same as {@link #wrapIntoType(Class, TypeReference)}.
   *
   * @param externalType a type that will be parameterized by the {@code internalType} type
   * @param internalType type with which the type {@code externalType} will be parameterized
   * @param <T> type of {@code internalType}
   * @param <E> type of {@code externalType}
   * @return {@link JavaType} parameterized type
   */
  protected <T, E> JavaType wrapIntoType(Class<E> externalType, Class<T> internalType) {
    return TarantoolJacksonMapping.wrapIntoType(externalType, internalType);
  }
}
