/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;

import org.msgpack.jackson.dataformat.MessagePackExtensionType;

/**
 * @author Artyom Dubinin
 */
public interface TarantoolDeserializer<T> {

  T deserialize(MessagePackExtensionType ext) throws IOException;
}
