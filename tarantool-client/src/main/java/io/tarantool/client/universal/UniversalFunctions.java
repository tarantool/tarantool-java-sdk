/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.universal;

/**
 * Defines the contract for universal operations in a Tarantool client interface.
 *
 * <p>This interface combines create, update, and delete operations into a single unified contract,
 * providing a comprehensive abstraction layer that enables building flexible and reusable APIs that
 * can work with various Tarantool client implementations without being tied to specific details.
 *
 * <p><b>WARNING:</b> This is an experimental API intended for advanced use cases only.
 *
 * <p>It is recommended to use the native API of the specific client implementation instead, as this
 * universal API may not fully expose all capabilities of individual client implementations.
 *
 * <p>This API should be used at your own risk - full understanding of its behavior and limitations
 * cannot be guaranteed. Not all client implementations may support all methods defined in this
 * interface.
 *
 * <p>This universal API is best suited for simple operations when you need to work with multiple
 * client implementations and want to maintain a consistent interface.
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public interface UniversalFunctions extends CreateFunctions, UpdateFunctions, DeleteFunctions {}
