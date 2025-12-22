/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.universal;

/**
 * <p>Defines the contract for create operations in a universal Tarantool client interface.</p>
 * <p>This interface provides methods for creating objects in Tarantool spaces,
 * serving as a foundational component for building abstract APIs that can work with various
 * Tarantool client implementations without being tied to specific details.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface CreateFunctions extends CreateFunctionWithInputAsObject {
}
