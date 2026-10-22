---
title: Spring Data
hide:
  - toc
---

**Spring Data Tarantool** - модуль [Spring Data](https://spring.io/projects/spring-data), который
упрощает создание приложений на базе [Spring :simple-spring:](https://spring.io),
использующих [Tarantool](https://www.tarantool.io) в качестве хранилища данных.

## Особенности

- Реализация CRUD методов для классов моделей данных, специфичных для работы с кластером Tarantool
  через библиотеку [tarantool/crud](https://github.com/tarantool/crud)
- Работа с хранимыми процедурами и скриптами Lua через динамические методы
- Возможность интеграции пользовательского кода репозитория
- Возможности гибкой настройки доступа к Tarantool
    - через spring-bean
    - через файлы конфигурации (`.properties`/`.yaml`)
- Удобная интеграция с инфраструктурой Spring
- Автоматическая реализация интерфейса `Repository`, используя `@EnableTarantoolRepositories`, в том
  числе, включая поддержку пользовательских запросов

## Статус проекта

| Версия tarantool-java-sdk | Версия tarantool-spring-data |                     Версия Spring Boot                      |
|:-------------------------:|:----------------------------:|:-----------------------------------------------------------:|
|           1.5.x           |            1.5.x             |         2.7.18 / 3.1.12 / 3.2.12 / 3.3.13 / 3.4.11          |
|           1.6.x           |            1.6.x             |     2.7.18 / 3.1.12 / 3.2.12 / 3.3.13 / 3.4.13 / 3.5.8      |
|           1.7.x           |            1.7.x             | 2.7.18 / 3.1.12 / 3.2.12 / 3.3.13 / 3.4.13 / 3.5.13 / 4.0.6 |

Модуль выбирается по минорной линии Spring Boot приложения. В таблице выше — версии Spring Boot, с
которыми модули собраны и протестированы; другие патч-версии той же линии в CI не проверяются, более
новые минорные линии не проверяются вовсе.

| Линия Spring Boot |           Модуль            |
|:-----------------:|:---------------------------:|
|       2.7.x       | `tarantool-spring-data-27`  |
|       3.1.x       | `tarantool-spring-data-31`  |
|       3.2.x       | `tarantool-spring-data-32`  |
|       3.3.x       | `tarantool-spring-data-33`  |
|       3.4.x       | `tarantool-spring-data-34`  |
|       3.5.x       | `tarantool-spring-data-35`  |
|       4.0.x       | `tarantool-spring-data-40`  |

### Версия Tarantool и поддерживаемые модули-клиенты

| Версия Tarantool | CRUD API (кластер) | BOX API (один экземпляр) |
|:----------------:|:------------------:|:------------------------:|
|      2.11.x      |         Да         |     Да (ограничено*)     |
|       3.x        |         Да         |     Да (ограничено*)     |

???+ note "Заметка"

    Поддерживается только конфигурация и получение spring bean `TarantoolBoxClient`.

| Версия Tarantool Data Grid |  Repository API  |
|:--------------------------:|:----------------:|
|            1.x             | Да (ограничено*) |
|            2.x             | Да (ограничено*) |

???+ note "Заметка"

    В релизе 1.4.0 добавлен экспериментальная версия клиента для Tarantool Data Grid,
    с поддержкой CRUD операций.

## Предварительные требования перед работой с модулей Spring Data Tarantool

### Наличие базы данных Tarantool

Для работы с модулем, необходим доступ к запущенному Tarantool. Инструкции по установке и запуску
Tarantool можно найти
здесь: [`github`](https://github.com/tarantool/tarantool?tab=readme-ov-file#tarantool).

???+ warning "Для `Repository` нужен кластер с `crud` и `vshard`"

    Слой репозиториев (`@EnableTarantoolRepositories`) привязан к `TarantoolCrudClient` и вызывает
    хранимые процедуры `crud.*`, которые даёт модуль [`crud`](https://github.com/tarantool/crud)
    поверх `vshard`. В обычном образе `tarantool/tarantool` с Docker Hub нет ни того, ни другого,
    поэтому вызовы `Repository` завершаются ошибкой `Procedure 'crud.select' is not defined`.
    Поднимите кластер (роутер с `crud` и хранилища с `vshard`) одним из способов:

    - образ Tarantool DB (TDB): `crud` и `vshard` в нём уже есть;
    - модуль [tarantool-java-sdk/testcontainers](../testcontainers/tdb/index.md): запускает кластер
      TDB из теста;
    - собственный образ: пример на основе `tarantool/tarantool` с рокспеком `crud` и скриптами
      `router.lua`/`storage.lua` лежит в репозитории, каталог
      `tarantool-shared-resources/vshard_cluster`.

???+ warning "Важно"

    - Данный модуль рассчитан на работу с Tarantool версий `2.11.x` и `3.x` . С отличиями версий 
      можно ознакомиться в [`официальной документации`](https://www.tarantool.io/en/doc/latest/).
    - Работа с более низкими версиями Tarantool возможна, но не гарантируется

???+ note "Заметка"

    В целях ознакомления или рассмотрения примеров, удобно использовать
    [`docker`](https://www.tarantool.io/en/download/os-installation/docker-hub/) и/или
    [`testcontainers`](https://testcontainers.com) в качестве среды для запуска Tarantool.

???+ note "Заметка"

    Также стоит ознакомиться с модулем 
    [tarantool-java-sdk/testcontainers](../testcontainers/index.md), который позволит уменьшить 
    временные затраты на использование `Tarantool` с `testcontainers` в Java.

### Загрузка библиотеки на рабочую машину

Подключите модуль в своем проекте следующим образом:

```xml

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>4.0.6</version>
  </dependency>
  <dependency>
    <groupId>io.tarantool</groupId>
    <artifactId>tarantool-spring-data-40</artifactId>
    <version>${tarantool-spring-data.version}</version>
  </dependency>
</dependencies>
```
