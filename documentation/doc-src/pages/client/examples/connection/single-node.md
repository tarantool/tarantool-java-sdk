---
title: Подключение к одиночному узлу
---

Для того чтобы подключиться к одиночному узлу необходимо выполнить следующий код:

=== "tarantool-java-sdk"

    ```java title="Подключение к одному узлу Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionTJSDKExample.java:all"
    ```

    ```java title="Родительский класс с созданием контейнера"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```    
 
    ```java title="Класс, который создает контейнер"
    --8<-- "testcontainers/utils/TarantoolSingleNodeConfigUtils.java:create-single-instance"
    ```

=== "cartridge-driver"

    ```java title="Подключение к одному узлу Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionCartridgeDriverExample.java:all"
    ```

    ```java title="Родительский класс с созданием контейнера"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```    
 
    ```java title="Класс, который создает контейнер"
    --8<-- "testcontainers/utils/TarantoolSingleNodeConfigUtils.java:create-single-instance"
    ```
