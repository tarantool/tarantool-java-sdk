/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.nio.file.Path;

/**
 * Utils class for path normalization.
 *
 * @author Vladimir Rogach
 */
public class PathUtils {

  private PathUtils() {}

  /**
   * Removes leading slash under windows from "/C:/work" so the path will match the format expected
   * by docker.
   *
   * @param path to any file
   * @return normalized path for docker
   */
  public static String normalizePath(String path) {
    String result;
    if (path.startsWith("/") && path.length() > 3 && path.charAt(2) == ':') {
      result = path.substring(1);
    } else {
      result = path;
    }
    return result.replace('\\', '/');
  }

  public static String normalizePath(Path path) {
    return normalizePath(path.toString());
  }
}
