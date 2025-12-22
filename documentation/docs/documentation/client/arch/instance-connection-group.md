---
title: Группа соединений
---

## InstanceConnectionGroup

`InstanceConnectionGroup` - класс, агрегирующий параметры для настройки группы соединений к узлу
Tarantool.

### Концепция групп соединений

<figure id="instance-connection-group-img-anchor">
    <figcaption>Место <b>InstanceConnectionGroup</b> в архитектуре Java-клиента</figcaption>
    <img src="../../../../assets/drawio/client/instance-connection-group.drawio" alt="">
</figure>

Высокоуровневые клиенты `TarantoolCrudClient`/ `TarantoolBoxClient` осуществляют взаимодействие с
узлами `Tarantool` посредством логически разделенных групп соединений. Каждая группа
представляет собой набор соединений (`IProtoClient`) к одному узлу Tarantool.

При создании высокоуровневого клиента объект класса `IProtoClientPool` создает группы соединений на
основе метаданных, переданных в объектах класса `InstanceConnectionGroup`. Одна логическая группа
соединений создается на основе метаданных одного объекта класса `InstanceConnectionGroup`.

Использование нескольких соединений в рамках одной группы позволяет повысить производительность
работы Java-клиента, особенно в ситуациях, когда операции над данными выполняются параллельно. Выбор
соединения для выполнения запроса определяется правилами балансировки ([подробнее](./balancer.md)).

### Описание параметров

<table>
    <tr align="center">
        <td>Имя параметра</td>
        <td>Описание</td>
        <td>Значение по умолчанию</td>
    </tr>
    <tr>
        <td id="host" align="center">host</td>
        <td>Адрес узла, к которому должна подключиться группа соединений</td>
        <td>localhost</td>
    </tr>
    <tr>
        <td id="port" align="center">port</td>
        <td>Порт, на котором узел Tarantool ожидает соединения. Вместе с 
            <a href="#host">host</a> составляет полный адрес узла Tarantool
        </td>
        <td>3301</td>
    </tr>
    <tr>
        <td id="user" align="center">user</td>
        <td>Имя пользователя, с помощью которого будет производиться аутентификация клиента при 
            подключении к узлу Tarantool
        </td>
        <td>user</td>
    </tr>
    <tr>
        <td align="center">password</td>
        <td>Пароль для <a href="#user">пользователя</a></td>
        <td>""</td>
    </tr>
    <tr>
        <td align="center">size</td>
        <td>Количество соединений к одному узлу (<a href="#instance-connection-group-img-anchor">
            Netty connection</a>)
        </td>
        <td>1</td>
    </tr>
    <tr>
        <td align="center">tag</td>
        <td>Тег-имя группы подключения. Необходим для идентификации группы в пуле соединений, логах</td>
        <td>user:host:port</td>
    </tr>
    <tr>
        <td align="center">flushConsolidationHandler</td>
        <td><a href="https://netty.io/4.1/api/io/netty/handler/flush/FlushConsolidationHandler.html">
            Подробнее</a>
        </td>
        <td>null</td>
    </tr>
    <tr>
        <td align="center">authType</td>
        <td>Тип алгоритма аутентификации Java-клиента. Подробнее смотрите Javadoc класса 
            <code>AuthType</code> и официальную 
            <a href="https://www.tarantool.io/en/doc/latest/reference/configuration/configuration_reference/#confval-security.auth_type">
            документацию Tarantool</a>
        </td>
        <td>AuthType.CHAP_SHA1</td>
    </tr>
</table>

Значения для <a href="#host">host</a> и <a href="#port">port</a> задаются в соответствии
с [InetSocketAddress](https://docs.oracle.com/javase/8/docs/api/java/net/InetSocketAddress.html).

### Использование

Для создания экземпляра класса `InstanceConnectionGroup` нужно воспользоваться
`InstanceConnectionGroup.Builder`:

```java
final InstanceConnectionGroup connectionGroup = InstanceConnectionGroup.builder()
    .withHost("localhost")
    .withPort(3301)
    .withUser("user2581-test")
    .withPassword("pwd-1")
    .withSize(3)
    .withTag("Node-1")
    .withAuthType(AuthType.CHAP_SHA1)
    .build();
```

Далее группа добавляется в `TarantoolCrudClientBuilder` или `TarantoolBoxClientBuilder`:

```java
final TarantoolCrudClient crudClient = TarantoolFactory.crud()
    .withGroups(Arrays.asList(connectionGroup))
    .build();
```
