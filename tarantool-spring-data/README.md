# Spring Data Tarantool

**Spring Data Tarantool** - модуль [Spring Data](https://spring.io/projects/spring-data), который
упрощает создание
приложений на базе [Spring](https://spring.io), использующих [Tarantool](https://www.tarantool.io) в
качестве хранилища
данных.

## Особенности

- Реализация CRUD методов для классов моделей данных, специфичных для работы с кластером Tarantool
  через библиотеку
  [tarantool/crud](https://github.com/tarantool/crud)
- Работа с хранимыми процедурами и скриптами LUA через динамические методы
- Возможность интеграции пользовательского кода репозитория
- Возможности гибкой настройки доступа к Tarantool (настройка через Java-объекты и файлы
  конфигурации
  (.properties / .yaml))
- Удобная интеграция с инфраструктурой Spring
- Автоматическая реализация интерфейса `Repository`, используя `@EnableTarantoolRepositories`, в том
  числе, включая
  поддержку пользовательских запросов

## Статус проекта

| Версия tarantool-java-sdk | Версия tarantool-spring-data |            Версия Spring Boot             |
|:------------------------:|:----------------------------:|:-----------------------------------------:|
|          1.0.0           |            1.0.0             |                  2.7.18                   |
|          1.1.x           |            1.1.x             |          2.7.18 / 3.1.10 / 3.2.4          |
|          1.2.x           |            1.2.x             |          2.7.18 / 3.1.10 / 3.2.4          |
|          1.3.x           |            1.3.x             | 2.7.18 / 3.1.10 / 3.2.4 / 3.3.11 / 3.4.5  |
|          1.4.x           |            1.4.x             | 2.7.18 / 3.1.10 / 3.2.4 / 3.3.13 / 3.4.10 |

### Версия Tarantool и поддерживаемые модули-клиенты

| Версия Tarantool | CRUD API (кластер) | BOX API (один экземпляр) |
|:----------------:|:------------------:|:------------------------:|
|      2.11.x      |         Да         |     Да (ограничено*)     |
|       3.x        |         Да         |     Да (ограничено*)     |

*Поддерживается только конфигурация и получение spring bean `TarantoolBoxClient`.

| Версия Tarantool Data Grid |     Repository API     |
|:--------------------------:|:----------------------:|
|            1.x             |    Да (ограничено*)    |
|            2.x             |    Да (ограничено*)    |

*В релизе 1.4.0 добавлен экспериментальная версия клиента для Tarantool Data Grid,
с поддержкой CRUD операций.


> [!TIP]
> - Больше о `crud` клиенте можно узнать:
    >
- Кластерный API на стороне
  tarantool ([`github.com/tarantool/crud`](https://github.com/tarantool/crud))
>   - Java прокси клиент к
      tarantool/crud ([`Java CRUD Client`](../tarantool-client/src/main/java/io/tarantool/client/crud)).
> - Больше о `box` клиенте можно узнать:
    >
- Внутренний модуль box для работы в Tarantool lua
  runtime [`Submodule box.space`](https://www.tarantool.io/en/doc/2.11/reference/reference_lua/box_space/).
>   - Имплементация box модуля для работы с инстансом Tarantool в
      Java [`Java BOX Client`](../tarantool-client/src/main/java/io/tarantool/client/box).  
      > Имеет схожий API c `Submodule box.space`, но не является им или прокси к нему.

> [!IMPORTANT]
> Работа c BOX API (`Java BOX Client`) на данным момент осуществляется **только** через интерфейс
> [`TarantoolBoxClient`](../tarantool-client/src/main/java/io/tarantool/client/box/TarantoolBoxClient.java)
> (можете добавить его как bean и работать в контексте Spring). Возможность работы
> через `Repository` и
> `@EnableTarantoolRepositories` на данный момент не реализована.

> [!IMPORTANT] 
> #### Spring Data 3.4.x
> Проверена работа 
> [repository fragments SPI](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2024.1-Release-Notes#repository-fragments-spi).
> Дополнительная информация по [ссылке](https://github.com/spring-projects/spring-data-commons/pull/3093).

## Начало работы с Spring Data Tarantool

- [Предварительные требования](doc/instructions/prepare.md)
- [Конфигурирование клиентов](doc/instructions/configuration.md)
- [Работа с Repository](./doc/instructions/tarantool_repository.md)
- [Работа с пользовательскими методами-запросами](./doc/instructions/derived_methods.md)
- [Работа с LUA и хранимыми процедурами](./doc/instructions/query_lua.md)
- [Пагинация. Pageable, Page, Slice](./doc/instructions/page-slice-pageable.md)
- [Пагинация. Scroll API (spring-data v3.1 и выше)](./doc/instructions/scroll-api.md)
- [Sort](./doc/instructions/sort.md)