/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Query Annotation do define Tarantool call or eval method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {

  /**
   * Specify the function name or eval string to invoke on the Tarantool instance, for example `my_query_function` or
   * `box.space.test:select` or `return my_query_function(1)`(if {@link Query#mode()} specified as EVAL). If this
   * annotation is specified, the method name will not be parsed into a query.
   *
   * @return the callable function name or eval string
   */
  String value() default "";

  /**
   * Specify mode about how to use query value. It could be a function name, or eval string.
   *
   * @return the mode how to use Query value
   */
  QueryMode mode() default QueryMode.CALL;
}
