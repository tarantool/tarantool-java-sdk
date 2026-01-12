---
title: Подключение к одиночному узлу
---

Для того чтобы подключиться к одиночному узлу необходимо выполнить следующий код:

=== "tarantool-java-sdk"

    ```java title="Подключение к одному узлу Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionTJSDKExample.java:tarantool-single-instance-tjsdk"
    ```

    ```java title="Родительский класс с созданием контейнера"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:tarantool-single-instance-abstract"
    ```    
 
    ```java title="Класс, который создает контейнер"
    --8<-- "testcontainers/utils/TarantoolSingleNodeConfigUtils.java:create-single-instance"
    ```

=== "cartridge-driver"

    ```java title="Подключение к одному узлу Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionCartridgeDriverExample.java:tarantool-single-instance-cartridge-driver"
    ```

    ```java title="Родительский класс с созданием контейнера"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:tarantool-single-instance-abstract"
    ```    
 
    ```java title="Класс, который создает контейнер"
    --8<-- "testcontainers/utils/TarantoolSingleNodeConfigUtils.java:create-single-instance"
    ```
