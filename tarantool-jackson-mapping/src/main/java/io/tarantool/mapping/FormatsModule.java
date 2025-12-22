/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.msgpack.jackson.dataformat.MessagePackKeySerializer;

public class FormatsModule {

  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-formats");

  static {
    INSTANCE.addKeySerializer(Object.class, new MessagePackKeySerializer());
  }

  public FormatsModule() {
  }
}
