---
title: Подключение к класетру Tarantool через crud
---

Следующий пример демонстрирует подключение к кластеру `Tarantool` через маршрутизаторы с
использованием модуля `crud`:

=== "tarantool-java-sdk"

    ```title="Подключенние к кластеру при помощи TJSDK"
    --8<-- "client/TarantoolDBClusterConnectionTJSDKExample.java:all"
    ```

    ```title="Абстрактный класс для создания кластера в docker"
    --8<-- "client/TarantoolDBClusterConnectionAbstractExample.java:all"
    ```

=== "cartridge-driver"

    ```title="Подключенние к кластеру при помощи Cartridge java"
    --8<-- "client/TarantoolDBClusterConnectionCartridgeDriverExample.java:all"
    ```

    ```title="Абстрактный класс для создания кластера в docker"
    --8<-- "client/TarantoolDBClusterConnectionAbstractExample.java:all"
    ```
