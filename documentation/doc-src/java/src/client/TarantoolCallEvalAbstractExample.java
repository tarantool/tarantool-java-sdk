/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package client;

// --8<-- [start:all]

import java.io.IOException;

import testcontainers.utils.TarantoolSingleNodeConfigUtils;

public abstract class TarantoolCallEvalAbstractExample
    extends TarantoolSingleInstanceConnectionAbstractExample {

  protected static String HELLO_WORLD_FUNCTION = "hello_world_function";
  protected static String HELLO_WORLD_FUNCTION_RETURNS = "hello world";
  protected static String SOME_MAP_FUNCTION = "some_map_function";

  protected static final String LUA_FUNCTION =
      String.format(
          """
          %s = function()
            return '%s'
          end

          %s = function(name, age)
            return {
                name = name,
                age = age
            }
          end
          """,
          HELLO_WORLD_FUNCTION, HELLO_WORLD_FUNCTION_RETURNS, SOME_MAP_FUNCTION);

  protected void loadFunctions() throws IOException, InterruptedException {
    final String command =
        "echo \"%s\" | tt connect %s:%s@localhost:3301"
            .formatted(
                LUA_FUNCTION,
                TarantoolSingleNodeConfigUtils.LOGIN,
                TarantoolSingleNodeConfigUtils.PWD);
    CONTAINER.execInContainer("/bin/sh", "-c", command);
  }

  protected record TestUser(String name, Integer age) {}
}

// --8<-- [end:all]
