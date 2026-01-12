---
title: Пул соединений
---

## Пул соединений (IProtoClientPool)

В `Tarantool Java SDK` пул соединений представляет собой набор подключений к узлам Tarantool,
объединенных в логические группы. Эти группы создаются с помощью экземпляров класса
`InstanceConnectionGroup`, который содержит необходимые метаданные. Более подробно о
[InstanceConnectionGroup](instance-connection-group.md).

При создании `TarantoolCrudClient` или `TarantoolBoxClient` создается один экземпляр
`IprotoClientPool`. Этот пул, на основе переданных ему `InstanceConnectionGroup`, создает внутри
себя экземпляры `PoolEntry`, которые управляют жизненным циклом своего соединения (процесс
подключения/отключения, [heartbeat](heartbeat.md)). Каждый `EntryPool` связан только с одной
логической группой. Все `EntryPool` в одной логической группе содержат свои собственные соединения к
одному и тому же узлу.

<figure markdown="span" id="connection-pool-1">
<figcaption>Место <b>IProtoClientPool</b> в архитектуре Java-клиента</figcaption>
![](../../../assets/drawio/client/connection-pool.drawio)
</figure>

`IProtoClientPool` следует рассматривать как контейнер, предназначенный для хранения набора
соединений. При выполнении запросов `IProtoClientPool` действует как объект, предоставляющий
балансировщику активные соединения (не более). Более подробную информацию о выборе соединения при
запросах можно найти в разделе о [балансировщиках](balancer.md).

???+ warning "Важно"

    Фактическое установление соединения в `IProtoClientPool` происходит, когда балансировщик впервые
    выбирает это соединение из пула.
