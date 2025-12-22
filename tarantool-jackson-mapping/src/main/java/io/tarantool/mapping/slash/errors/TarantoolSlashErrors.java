/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.slash.errors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * <p>The class implements error that comes in response from Tarantool.</p>
 * <p>Special errors that use tarantool/errors library</p>.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see <a href="https://github.com/tarantool/errors">errors</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TarantoolSlashErrors {

  /**
   * <p>Description of the error.</p>
   */
  protected String err;

  /**
   * <p>Full description of the error.</p>
   */
  protected String str;

  /**
   * <p>Line number where the error occurred.</p>
   */
  protected int line;

  /**
   * <p>Path to the file where the error occurred.</p>
   */
  protected String file;

  /**
   * <p>Stack trace.</p>
   */
  protected String stack;

  /**
   * <p>Name of error class.</p>
   */
  @JsonSetter("class_name")
  protected String className;

  /**
   * <p>Creates a {@link TarantoolSlashErrors} object.</p>
   */
  public TarantoolSlashErrors() {
  }

  /**
   * <p>Returns value of err field.</p>
   *
   * @return {@link #err} value.
   */
  public String getErr() {
    return err;
  }

  /**
   * <p>Returns value of str field.</p>
   *
   * @return {@link #str} value.
   */
  public String getStr() {
    return str;
  }

  /**
   * <p>Returns value of line field.</p>
   *
   * @return {@link #line} value.
   */
  public int getLine() {
    return line;
  }

  /**
   * <p>Returns value of file field.</p>
   *
   * @return {@link #file} value.
   */
  public String getFile() {
    return file;
  }

  /**
   * <p>Returns value of stack field.</p>
   *
   * @return {@link #stack} value.
   */
  public String getStack() {
    return stack;
  }

  /**
   * <p>Returns value of className field.</p>
   *
   * @return {@link #className} value.
   */
  public String getClassName() {
    return className;
  }

  @Override
  public String toString() {
    return "TarantoolSlashErrors{" +
        "err='" + err + '\'' +
        ", str='" + str + '\'' +
        ", line=" + line +
        ", file='" + file + '\'' +
        ", stack='" + stack + '\'' +
        ", className='" + className + '\'' +
        '}';
  }
}
