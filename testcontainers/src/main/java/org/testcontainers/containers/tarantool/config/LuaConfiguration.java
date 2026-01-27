/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.tarantool.autogen.BoxCfg;

public class LuaConfiguration {

  private static final ObjectMapper OBJECT_MAPPER;

  private static final TypeReference<Map<String, Object>> TYPE_REFERENCE;

  private static final String CFG_CHUNK_START = "box.cfg{\n\t";

  private static final String CFG_CHUNK_END = "}";

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER
        .setSerializationInclusion(Include.NON_NULL)
        .setSerializationInclusion(Include.NON_EMPTY);
    OBJECT_MAPPER.registerModule(new Jdk8Module());
    TYPE_REFERENCE = new TypeReference<>() {};
  }

  public static void writeAsLuaScript(Path path, BoxCfg config) throws IOException {
    final byte[] configAsString = serializeConfig(config);
    Files.write(path, configAsString);
  }

  private static byte[] serializeConfig(BoxCfg config) {
    final Map<String, Object> configAsMap = OBJECT_MAPPER.convertValue(config, TYPE_REFERENCE);
    final StringBuilder sb = new StringBuilder(CFG_CHUNK_START);
    for (Map.Entry<String, Object> entry : configAsMap.entrySet()) {
      if (entry.getValue() != null) {
        sb.append(entry.getKey())
            .append("=")
            .append(serializeValue(entry.getValue()))
            .append(',')
            .append("\n\t");
      }
    }
    sb.delete(sb.length() - 2, sb.length());
    if (!configAsMap.isEmpty()) {
      sb.append('\n');
    }
    return sb.append(CFG_CHUNK_END).toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String serializeValue(Object value) {
    if (value == null) {
      return "nil";
    }

    if (value instanceof String | value instanceof Enum<?>) {
      return "'" + value + "'";
    }

    if (value instanceof Boolean) {
      return value.toString();
    }

    if (value instanceof BigDecimal) {
      return ((BigDecimal) value).stripTrailingZeros().toPlainString();
    }

    if (value instanceof Number) {
      return value.toString();
    }

    throw new IllegalArgumentException("Cannot serialize value of type " + value.getClass());
  }
}
