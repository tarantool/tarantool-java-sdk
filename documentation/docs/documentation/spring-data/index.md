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
- Работа с хранимыми процедурами и скриптами LUA через динамические методы
- Возможность интеграции пользовательского кода репозитория
- Возможности гибкой настройки доступа к Tarantool
    - через spring-bean
    - через файлы конфигурации (`.properties`/`.yaml`)
- Удобная интеграция с инфраструктурой Spring
- Автоматическая реализация интерфейса `Repository`, используя `@EnableTarantoolRepositories`, в том
  числе, включая поддержку пользовательских запросов

## Статус проекта

| Версия tarantool-java-sdk | Версия tarantool-spring-data |                Версия Spring Boot                 |
|:-------------------------:|:----------------------------:|:-------------------------------------------------:|
|           1.5.x           |            1.5.x             | 2.7.18 / 3.1.10 / 3.2.4 / 3.3.13 / 3.4.10 / 3.5.7 |

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
    <version>3.5.7</version>
  </dependency>
  <dependency>
    <groupId>io.tarantool</groupId>
    <artifactId>tarantool-spring-data-35</artifactId>
    <version>${tarantool-spring-data.version}</version>
  </dependency>
</dependencies>
```
