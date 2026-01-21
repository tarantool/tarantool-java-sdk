/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data35.core.mapping;

import java.lang.reflect.Field;
import java.util.Collection;

import org.springframework.data.mapping.PersistentProperty;

/** The interface of the identifier class that will contain information about the composite key. */
public interface Identifier<P extends PersistentProperty<P>> {

  /**
   * Add a {@link PersistentProperty} tagged with {@code Id}.
   *
   * @param property {@link PersistentProperty} tagged {@code Id}
   */
  void addPart(P property);

  /**
   * Return the parts of a composite key that are {@link PersistentProperty}.
   *
   * @return parts of a composite key
   */
  Collection<P> getParts();

  /**
   * Return a list {@link Field} of fields that are annotated {@code @Id}. Unlike method {@link
   * #getParts()} this method returns an array {@link Field}, where {@link Field} is taken from each
   * element of the collection returned by the {@link #getParts()} method. Necessary in order not to
   * write polluting code.
   *
   * @return fields of a composite key
   */
  Field[] getFields();
}
