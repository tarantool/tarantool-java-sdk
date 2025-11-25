/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data.core.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Annotation that allows you to mark domain classes that contain a composite key. */
@Target({TYPE})
@Retention(RUNTIME)
public @interface IdClass {

  /**
   * Primary key class
   *
   * @return class that contains all ID fields (multipart primary key)
   */
  Class<?> value();
}
