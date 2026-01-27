---
title: Interacting with stored procedures
---

The following example demonstrates interacting with stored procedures from java:

=== "tarantool-java-sdk"

    ```title="Interacting with stored procedures from java"
    --8<-- "client/TarantoolCallTJSDKExample.java:all"
    ```

    ```title="Abstract class to create Tarantool container"
    --8<-- "client/TarantoolCallEvalAbstractExample.java:all"
    ```

    ```title="Abstract class to create Tarantool container"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```

=== "cartridge-driver"

    ```title="Interacting with stored procedures from java"
    --8<-- "client/TarantoolCallCartridgeDriverExample.java:all"
    ```

    ```title="Abstract class to create Tarantool container"
    --8<-- "client/TarantoolCallEvalAbstractExample.java:all"
    ```

    ```title="Abstract class to create Tarantool container"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```

???+ note

    `tarantool-java-sdk` allows you to convert stored procedure return values to pojos, which have 
    fields with default types, automatically. In `cartridge-driver` for this requires implementing 
    converters for each of the custom pojo types, and passing them to `#call(...)` methods .

