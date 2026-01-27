---
title: Connection to single instance Tarantool
---

To connect to a single instance, run the following code:

=== "tarantool-java-sdk"

    ```java title="Connection to single instance Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionTJSDKExample.java:all"
    ```

    ```java title="Parent abstract class to create docker container"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```    
 
    ```java title="Class to create container"
    --8<-- "testcontainers/utils/TarantoolSingleNodeConfigUtils.java:create-single-instance"
    ```

=== "cartridge-driver"

    ```java title="Connection to single instance Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionCartridgeDriverExample.java:all"
    ```

    ```java title="Parent abstract class to create docker container"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```    
 
    ```java title="Class to create container"
    --8<-- "testcontainers/utils/TarantoolSingleNodeConfigUtils.java:create-single-instance"
    ```
    