---
title: Подключение к нескольким узлам
---

## Подключение к нескольким узлам Tarantool

Прежде чем подключать Java-клиент к нескольким узлам Tarantool, необходимо определиться с форматом
подключения. На выбор есть два варианта:

* `TarantoolCrudClient` - высокоуровневый клиент для работы с vshard кластером Tarantool c
  возможностью подключения к нескольким маршрутизаторами (router). Использует вызовы методов модуля
  crud для манипулирования данными. Подробнее о [crud](https://github.com/tarantool/crud) и
  [vshard](https://github.com/tarantool/vshard).
* `TarantoolBoxClient` - высокоуровневый клиент для работы с одиночными узлами Tarantool с
  возможностью подключения к набору узлов вне зависимости от топологии. В случае использования
  `crud/vshard` кластера попытки внести изменения на узлы с ролью `storage` могут привести к
  нарушению работы кластера.

???+ warning "Важно"

    Для подробного описания crud и box API в Tarantool Java SDK обратитесь к javadoc
    `TarantoolBoxClient` и `TarantoolCrudClient`

Для подключения к Tarantool вне зависимости от режима (кластерный или одиночный), требуются
следующие данные:

* `Доменные имена` или `адреса узлов` (например, `localhost`,`127.0.0.1`)
* `Порты`, на которых прослушиваются подключения к Tarantool (например, `3301`)
* `Имена пользователей`, от имени которых идет взаимодействие клиента с Tarantool
  (например, `admin`, `user` и т.д.)
* `Пароли`, соответствующие именам пользователей

### Подключение к нескольким узлам (маршрутизаторам) Tarantool через TarantoolCrudClient API

???+ warning "Важно"

    В данном формате подключения взаимодействие с кластером Tarantool производится 
    **ИСКЛЮЧИТЕЛЬНО** через узлы-маршрутизаторы

Рассмотрим пример со следующей топологией кластера Tarantool:

<figure id="vshard-topology">
  <figcaption>Пример топологии vshard-кластера</figcaption>
  <img src="../../../../assets/images/client/few-nodes-crud.svg" alt="">
</figure>  

Для того чтобы настроить `TarantoolCrudClient`, необходимо воспользоваться API
`TarantoolCrudClientBuilder`:

```java
final TarantoolCrudClientBuilder crudClientBuilder = TarantoolFactory.crud();
```

Далее следует настроить группы соединений к узлам. Группа соединений настраивается
через API `InstanceConnectionGroup.Builder`. Следующий код позволяет настроить группу соединений к
узлу-маршрутизатору `Router 1 (replicaset-1)` (см. <a href="#vshard-topology">топологию</a>):

```java
final InstanceConnectionGroup firstRouterConnectionGroup = InstanceConnectionGroup.builder()
    .withAuthType(AuthType.CHAP_SHA1)
    .withHost("localhost")
    .withPort(3301)
    .withUser("seller-user")
    .withPassword("pwd-1")
    .withSize(2)
    .withTag("router-1")
    .build();
```

???+ note "Заметка"

    Для более подробного описания класса `InstanceConnectionGroup` можете обратиться к
    соответствующей [странице](./instance-connection-group.md) документации. Также изучите класс 
    `InstanceConnectionGroup` в Javadoc

Создадим экземпляр `InstanceConnectionGroup` для второго маршрутизатора `Router 2 (replicaset-2)`
(см. <a href="#vshard-topology">топологию</a>):

```java
final InstanceConnectionGroup secondRouterConnectionGroup = InstanceConnectionGroup.builder()
    .withPort(3302)
    .withUser("user-1182")
    .withPassword("pwd-2")
    .withSize(2)
    .withTag("router-2")
    .build();
```

Далее добавим созданные ранее экземпляры `InstanceConnectionGroup` в `TarantoolCrudClientBuilder`:

```java
final List<InstanceConnectionGroup> connectionGroupsList = Arrays.asList(firstRouterConnectionGroup,
    secondRouterConnectionGroup); // (1)!

final TarantoolCrudClient crudClient = crudClientBuilder.withGroups(connectionGroupsList)
    .build(); // (2)!
```  

1. Позволяет задать список ранее созданных экземпляров `InstanceConnectionGroup`, которые отражают
   настройки групп подключений к соответствующим маршрутизаторам
2. Создаем экземпляр класса `TarantoolCrudClient`. Клиент является ленивым и подключение к
   доступному узлу происходит при первом вызове методов манипуляции данных

Для доступа к методам манипулирования данными следует получить экземпляр
`TarantoolCrudSpace` через API `TarantoolCrudClient`:

```java
// Позволяет получить экземпляр TarantoolCrudSpace. Space имеет имя 'person'
final TarantoolCrudSpace personSpace = crudClient.space("person"); 
```

С помощью экземпляров `TarantoolCrudSpace` производится работа с данными конкретного space. В
примере это space с именем `person`.

???+ note "Заметка"

    Распределение запросов между узлами производится по правилам балансировки. Обратитесь к 
    [разделу](./balancer.md), чтобы узнать больше.

### Подключение к нескольким узлам Tarantool через TarantoolBoxClient API

В общем случае экземпляры `TarantoolBoxClient` предназначены для работы с одиночным узлом Tarantool,
но есть сценарии, в которых `TarantoolBoxSpace` можно использовать при работе с несколькими узлами:

<figure id="box-client-topology">
  <figcaption>Выборка данных с нескольких реплик одного шарда</figcaption>
  <img src="../../../../assets/images/client/few-replicas-box.svg" alt="">
</figure>  

В приведенном примере одна из реплик `replica-1` отказывает. Необходимо, чтобы Java-клиент продолжал
выборку данных, переключившись на реплику `replica-2`.

Для того чтобы настроить подключение к двум узлам-репликам (`replica-1`, `replica-2`), необходимо
воспользоваться `TarantoolBoxClientBuilder` API:

```java
final TarantoolBoxClientBuilder boxBuilder = TarantoolFactory.box();
```

Далее необходимо настроить группы подключений к узлам-репликам, аналогично тому, как это
демонстрируется в секции "[Подключение к нескольким узлам-маршрутизаторам Tarantool через
TarantoolCrudClient API](./connection-to-multiple-nodes.md):

```java
final InstanceConnectionGroup firstReplicaConnectionGroup = InstanceConnectionGroup.builder()
    .withUser("user-1298")
    .withPassword("pwd-1")
    .withPort(3301)
    .withSize(2)
    .withTag("replica-1")
    .build();

final InstanceConnectionGroup secondReplicaConnectionGroup = InstanceConnectionGroup.builder()
    .withUser("storage-user")
    .withPassword("pwd-2")
    .withPort(3302)
    .withSize(2)
    .withTag("replica-2")
    .build();
```

Добавим созданные ранее экземпляры `InstanceConnectionGroup` в `TarantoolBoxClientBuilder`:

```java
final List<InstanceConnectionGroup> connectionGroupsList = Arrays.asList(
    firstReplicaConnectionGroup,
    secondReplicaConnectionGroup);

final TarantoolBoxClient boxClient = boxClientBuilder.withGroups(connectionGroupsList)
    .build();
```

Для доступа к методам манипулирования данными следует получить экземпляр
`TarantoolBoxSpace` через API `TarantoolBoxClient`:

```java
final TarantoolBoxSpace personSpace = boxClient.space("person");
```

С помощью экземпляров `TarantoolBoxSpace` производится работа с данными конкретного space. В
примере это space с именем `person`.

???+ warning "Важно"

    Таким образом, при отказе одной из реплик, Java-клиент переключится на вторую активную реплику 
    в соответствии с правилами балансировки. Обратитесь к [разделу](./balancer.md), 
    чтобы узнать больше
