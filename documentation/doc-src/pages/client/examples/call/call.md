---
title: Работа с хранимыми процедурами
---

Следующие пример демонстрируют работу с хранимыми процедурами из java:

=== "tarantool-java-sdk"

    ```title="Работа с хранимыми процедурами из java"
    --8<-- "client/TarantoolCallTJSDKExample.java:all"
    ```

    ```title="Абстрактный класс для создания контейнера Tarantool"
    --8<-- "client/TarantoolCallEvalAbstractExample.java:all"
    ```

    ```title="Абстрактный класс для создания контейнера Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```

=== "cartridge-driver"

    ```title="Работа с хранимыми процедурами из java"
    --8<-- "client/TarantoolCallCartridgeDriverExample.java:all"
    ```

    ```title="Абстрактный класс для создания контейнера Tarantool"
    --8<-- "client/TarantoolCallEvalAbstractExample.java:all"
    ```

    ```title="Абстрактный класс для создания контейнера Tarantool"
    --8<-- "client/TarantoolSingleInstanceConnectionAbstractExample.java:all"
    ```

???+ note "Заметка"

    `tarantool-java-sdk` позволяет преобразовывать возвращаемые значения хранимых процедур в pojo, 
    которые имеют поля с типами по умолчанию, автоматически. В `cartridge-driver` для этого 
    требуется реализация конвертеров для каждого из пользовательских типов pojo, и передача их в 
    методы `#call(...)`.

