/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.core.mapping.model;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.util.Assert;

/**
 * Strategy class for entity with composite key to determine whether a given entity is to be
 * considered new.
 */
public class PersistentCompositeIdIsNewStrategy implements IsNewStrategy {

  /**
   * Create a new {@link PersistentCompositeIdIsNewStrategy} for the given entity.
   *
   * @param entity must not be {@literal null}.
   * @param idOnly check only id to determine
   */
  private PersistentCompositeIdIsNewStrategy(PersistentEntity<?, ?> entity, boolean idOnly) {
    // TODO сделать реализацию версионирования
    // TODO add idOnly support
    Assert.notNull(entity, "PersistentEntity must not be null");
  }

  /**
   * Create a new {@link PersistentCompositeIdIsNewStrategy} to only consider the identifier of the
   * given entity.
   *
   * @param entity must not be {@literal null}.
   * @return strategy to determine whether entity is new or not
   */
  public static PersistentCompositeIdIsNewStrategy forIdOnly(PersistentEntity<?, ?> entity) {
    return new PersistentCompositeIdIsNewStrategy(entity, true);
  }

  /**
   * Create a new {@link PersistentCompositeIdIsNewStrategy} to consider version properties before
   * falling back to the identifier.
   *
   * @param entity must not be {@literal null}.
   * @return strategy to determine whether entity is new or not
   */
  public static PersistentCompositeIdIsNewStrategy of(PersistentEntity<?, ?> entity) {
    return new PersistentCompositeIdIsNewStrategy(entity, false);
  }

  /**
   * Determine whether the current domain entity type object is new. Currently stub and always
   * returns false.
   *
   * @param entity must not be {@literal null}.
   * @return result of the question of entity is new or not
   */
  @Override
  public boolean isNew(Object entity) {
    return false;
  }
}
