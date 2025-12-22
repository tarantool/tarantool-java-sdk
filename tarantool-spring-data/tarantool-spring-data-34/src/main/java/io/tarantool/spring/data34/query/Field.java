/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Allows adding some metadata to the class fields relevant for storing them in the Tarantool space
 *
 * @author Artyom Dubinin
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Field {

  /**
   * The target Tarantool space field for storing the marked class field. Alias for {@link #name()}.
   *
   * @return the name of a field in space
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The target Tarantool space field for storing the marked class field. Alias for {@link
   * #value()}.
   *
   * @return the name of a field in space
   */
  @AliasFor("value")
  String name() default "";

  /**
   * The order in which fields shall be stored in the tuple. Has to be a positive integer.
   *
   * @return the order the field shall have in the tuple or -1 if undefined.
   */
  int order() default -1;
}
