/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.util.HashMap;
import java.util.Map;

public class Arguments {

  public static Map<String, String> get(final String baseImage) {
    return get(baseImage, null);
  }

  public static Map<String, String> get(final String baseImage, final String cartridgeSrcDir) {
    final Map<String, String> buildArgs = new HashMap<>();
    String registry = System.getenv("TARANTOOL_REGISTRY");
    if (registry != null && !registry.isEmpty()) {
      buildArgs.put(
          "IMAGE", registry.endsWith("/") ? registry + baseImage : registry + "/" + baseImage);
    }
    buildArgs.put("DOWNLOAD_HOST", System.getenv("DOWNLOAD_HOST"));
    buildArgs.put("SDK_PATH", System.getenv("SDK_PATH"));
    buildArgs.put("CLUSTER_SRC_DIR", "vshard_cluster");
    buildArgs.put("TARANTOOL_DB_PATH", System.getenv("TARANTOOL_DB_PATH"));
    if (cartridgeSrcDir != null) {
      buildArgs.put("CARTRIDGE_SRC_DIR", cartridgeSrcDir);
    }
    return buildArgs;
  }
}
