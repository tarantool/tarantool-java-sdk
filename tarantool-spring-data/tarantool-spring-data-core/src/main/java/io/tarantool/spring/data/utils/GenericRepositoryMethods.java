/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.utils;

import java.util.List;
import java.util.Optional;

import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.spring.data.query.Query;
import io.tarantool.spring.data.query.QueryMode;

public interface GenericRepositoryMethods<T, ID> {

  List<T> findByName(String name);

  List<T> findAllByIdIsLessThan(int id);

  List<T> findAllByIdLessThanEqual(int id);

  void findByNameIgnoreCase(String name);

  List<T> findAllByIsMarriedIsTrue();

  List<T> findAllByIsMarriedIsFalse();

  List<T> findAllByNameIsEmpty();

  List<T> findAllByIsMarriedIsNull();

  List<T> findAllByIdIsGreaterThan(int id);

  List<T> findAllByIdGreaterThanEqual(int id);

  List<T> findAllByIdBetween(int startId, int endId);

  List<T> findByIsMarried(Boolean isMarried);

  // TODO: make spring data to distinguish return type and unwrap TarantoolResponse if needed
  @Query("echo")
  TarantoolResponse<List<?>> echo(String stringArg, Boolean booleanArg, Integer intArg);

  @Query("get_static")
  TarantoolResponse<List<?>> getStatic();

  @Query(value = "return 'hello', 123, true", mode = QueryMode.EVAL)
  TarantoolResponse<List<?>> evalGetStatic();

  @Query(value = "return ...", mode = QueryMode.EVAL)
  TarantoolResponse<List<?>> evalWithArgs(String stringArg, Boolean booleanArg, Integer intArg);

  void findById(); // wrong declaration for test exceptions. Don't touch :)

  int countByName(String name);

  List<T> deleteByName(String name);

  boolean existsByName(String name);

  List<T> findByNameAfter(String name);

  List<T> findFirst5ByIdAfter(Integer id);

  List<T> findTop5ByIdAfter(Integer id);

  List<T> findByNameBefore(String name);

  List<T> findFirst5ByIdBefore(Integer id);

  List<T> findTop5ByIdBefore(Integer id);

  List<T> deleteByNameAfter(String name);

  List<T> deleteByNameBefore(String name);

  long countByIdAfter(Integer id);

  long countByNameBefore(String name);

  long count();

  void delete(T entity);

  void deleteById(ID id);

  boolean existsById(ID id);

  <S extends T> Iterable<S> saveAll(Iterable<S> entities);

  <S extends T> S save(S entity);

  void deleteAll(Iterable<? extends T> entities);

  void deleteAllById(Iterable<? extends ID> ids);

  void deleteAll();

  Optional<T> findById(ID id);

  Iterable<T> findAllById(Iterable<ID> ids);

  List<T> findPersonById(ID id);
}
