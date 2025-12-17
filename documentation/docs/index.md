---
title: О проекте
hide:
  - toc
  - navigation 
---

# Tarantool Java SDK

**`Tarantool Java SDK`** - набор библиотек для взаимодействия
с [Tarantool :simple-github:](https://github.com/tarantool/tarantool) из `Java`.  
Для работы с `Tarantool` используется фреймворк [Netty ](https://netty.io) для асинхронного сетевого
взаимодействия и библиотека [MessagePack](https://github.com/msgpack/msgpack-java) для сериализации
и десериализации данных.

Библиотеки обеспечивают полную поддержку всего протокола Tarantool (IProto). Они предоставляют Box
API, для работы с отдельными серверами Tarantool. Crud API[^1] для работы с кластерами (Tarantool
DB, Tarantool EE), а также интерфейсы `Spring Data` для интеграции
со [Spring :simple-spring:](https://spring.io)

[^1]: CRUD API — является прокси к API [tarantool/crud](https://github.com/tarantool/crud)

## Загрузка артефактов из Maven-central

Для того чтобы загрузить артефакты из `maven-central` необходимо сделать следующие шаги:

- Установить [maven :simple-apachemaven:](https://maven.apache.org)
- Создать `maven` проект
- Добавить зависимости в `pom.xml` проекта:
    ```xml
    <dependency>
        <groupId>io.tarantool</groupId>
        <artifactId>tarantool-client</artifactId>
        <version>${tarantool-java-sdk.version}</version>
    </dependency>
    ```
