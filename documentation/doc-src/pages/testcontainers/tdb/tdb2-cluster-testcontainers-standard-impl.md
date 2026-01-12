---
title: Стандартная реализация 
---

На странице приводится описание стандартной реализации интерфейса `TDBCluster` для `TDB 2.x`.

## Диаграмма классов

```puml
@startuml

!theme plain
top to bottom direction
skinparam linetype ortho

interface AutoCloseable << interface >> {
  + close(): void
}
class Builder {
  + withMigrationsDirectory(Path): Builder
  + withShardCount(int): Builder
  + withStartupTimeout(Duration): Builder
  + withRouterCount(int): Builder
  + withTDB2Configuration(Tarantool3Configuration): Builder
  + build(): TDB2ClusterImpl
  + withReplicaCount(int): Builder
}

interface Startable << interface >> {
  + stop(): void
  + getDependencies(): Set<Startable>
  + close(): void
  + start(): void
}

class TDB2ClusterImpl {
  + tcmContainer(): TCMContainer
  + storages(): Map<String, TarantoolContainer<?>>
  + nodes(): Map<String, TarantoolContainer<?>>
  + clusterName(): String
  + start(): void
  + routers(): Map<String, TarantoolContainer<?>>
  + etcdContainer(): EtcdContainer
  + stop(): void
  + builder(String): Builder
}

interface TDBCluster << interface >> {
  + clusterName(): String
  + tcmContainer(): TCMContainer
  + storages(): Map<String, TarantoolContainer<?>>
  + etcdContainer(): EtcdContainer
  + routers(): Map<String, TarantoolContainer<?>>
  + restart(long, TimeUnit): void
  + nodes(): Map<String, TarantoolContainer<?>>
}

Builder          o-[#820000,plain]-  TDB2ClusterImpl
Startable        -[#008200,plain]-^  AutoCloseable
TDB2ClusterImpl  -[#008200,dashed]-^  TDBCluster
TDBCluster       -[#008200,plain]-^  Startable
@enduml
```

Класс `TDB2ClusterImpl` позволяет создать объект, управляющий жизненным циклом кластера TDB 2.x,
удовлетворяющий [контракту](tdb-cluster-testcontainers-arch.md) `TDBCluster`.

## Описание реализации

### Конфигурирование кластера

Реализация следует следующему алгоритму при конфигурации кластера:

```puml
@startuml
(*) --> "Вызов <b>TDB2ClusterImpl::start()" as s1

s1 --> "Создать временную директорию
для экземпляра <b>TDB2ClusterImpl</b>" as s2

if "Передана конфигурация через\n<b>Tarantool3Configuration</b>?" then
  -r-> [нет]  "Сгенерировать конфигурацию
  на основе переданных <b>routerCount</b>,
  <b>shardCount</b>, <b>replicaCount</b>" as s3
else
  -d-> [да] "Записать файл конфигурации
  на основе объекта конфигурации во
  временную директорию" as s4
endif
s3 -r-> s4

s4 --> "Создать и настроить
контейнеры <b>etcd</b>, <b>TCM</b>" as s5

if "Успешно\nстартовали?" then
  -d->[нет] "**Выбросить исключение**" as exc1
else
  -d->[да] "Опубликовать конфигурацию
  кластера через <b>TCM</b> в <b>etcd</b>" as s6
endif

if "Конфигурация\nперенесена?" then
  -r->[нет] exc1
else
  -->[да] "Запустить контейнеры
  с узлами <b>Tarantool</b>" as s7
endif

if "Контейнеры\nзапущены?" then
  -r->[нет] exc1
else
  -->[да] "Инициализировать
  кластер" as s8
endif

if "Кластер\nинициализирован?" then
  -r->[нет] exc1
else
  -->[да] "Применить миграции" as s9
endif

if "Миграции\nприменены?" then
  -r->[нет] exc1
else
  -->[да] "Завершить старт"
  --> (*)
endif
exc1 --> (*)
@enduml
```

#### Обеспечение сохранения монтируемых данных

Согласно контракту `TDBCluster` при вызове метода `TDBCluster::restart(...)` монтируемые данные
должны сохраниться. `TDB2ClusterImpl` реализует этот механизм за счет использования контракта
`TarantoolContainer::stopWithSafeMount()`:

```puml
@startuml
start
:Вызов <b>TDB2ClusterImpl::restart(...)</b>;
package "TDB2ClusterImpl::restart" {
  :Вызов <b>Tarantool3Container::stopWithSafeMount()</b>
  у каждого узла <b>Tarantool</b> в кластере;
  :Выждать заданный delay;
  :Вызов <b>Tarantool3Container::start()</b>
  у каждого узла <b>Tarantool</b> в кластере;
}
stop
@enduml
```

Удаление монтируемых директорий происходит только при вызове метода `TDB2ClusterImpl::stop()`.
Конфигурирование контейнера происходит один раз при первом вызове `TDB2ClusterImpl::start()`.

### Привязка портов

Привязка внешних портов к компонентам кластера (контейнерам) происходит на этапе запуска экземпляра
`TDB2ClusterImpl`.
