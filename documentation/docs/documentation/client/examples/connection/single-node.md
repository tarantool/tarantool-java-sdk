---
title: Подключение к одиночному узлу
---

Для того чтобы подключиться к одиночному узлу необходимо выполнить следующий код:

=== "tarantool-java-sdk"

    ```java title="Подключение к одному узлу Tarantool"
    --8<-- "src/client/simple/connection/SingleNodeConnectionNewConnector.java:new-simple-connection"
    ```

    ```java title="Родительский класс с созданием контейнера"
    --8<-- "src/client/simple/connection/SingleNodeConnection.java:"
    ```    
 
    ```java title="Класс, который создает контейнер"
    --8<-- "src/testcontainers/single/CreateSingleNode.java:create-single-node"
    ```

=== "cartridge-java"

    ```java title="Подключение к одному узлу Tarantool"
    --8<-- "src/client/simple/connection/SingleNodeConnectionCartridgeJava.java:old-simple-connection"
    ```

    ```java title="Родительский класс с созданием контейнера"
    --8<-- "src/client/simple/connection/SingleNodeConnection.java:"
    ```    
 
    ```java title="Класс, который создает контейнер"
    --8<-- "src/testcontainers/single/CreateSingleNode.java:create-single-node"
    ```
    