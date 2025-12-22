/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.slash.errors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The class implements error that comes in response from Tarantool.
 *
 * <p>Special errors that use tarantool/errors library.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see <a href="https://github.com/tarantool/errors">errors</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TarantoolSlashErrors {

  /** Description of the error. */
  protected String err;

  /** Full description of the error. */
  protected String str;

  /** Line number where the error occurred. */
  protected int line;

  /** Path to the file where the error occurred. */
  protected String file;

  /** Stack trace. */
  protected String stack;

  /** Name of error class. */
  @JsonSetter("class_name")
  protected String className;

  /** Creates a {@link TarantoolSlashErrors} object. */
  public TarantoolSlashErrors() {}

  /**
   * Returns value of err field.
   *
   * @return {@link #err} value.
   */
  public String getErr() {
    return err;
  }

  /**
   * Returns value of str field.
   *
   * @return {@link #str} value.
   */
  public String getStr() {
    return str;
  }

  /**
   * Returns value of line field.
   *
   * @return {@link #line} value.
   */
  public int getLine() {
    return line;
  }

  /**
   * Returns value of file field.
   *
   * @return {@link #file} value.
   */
  public String getFile() {
    return file;
  }

  /**
   * Returns value of stack field.
   *
   * @return {@link #stack} value.
   */
  public String getStack() {
    return stack;
  }

  /**
   * Returns value of className field.
   *
   * @return {@link #className} value.
   */
  public String getClassName() {
    return className;
  }

  @Override
  public String toString() {
    return "TarantoolSlashErrors{"
        + "err='"
        + err
        + '\''
        + ", str='"
        + str
        + '\''
        + ", line="
        + line
        + ", file='"
        + file
        + '\''
        + ", stack='"
        + stack
        + '\''
        + ", className='"
        + className
        + '\''
        + '}';
  }
}
