---
title: Стандартная реализация TarantoolContainer
---

## Стандартная реализация интерфейса `TarantoolContainer`

На странице приводится описание стандартной реализации интерфейса `TarantoolContainer`.

### Диаграмма классов

```puml
@startuml

!theme plain
top to bottom direction
skinparam linetype ortho

class GenericContainer<SELF> {
  // some methods
}

class Tarantool3Container {
  + Tarantool3Container(DockerImageName dockerImageName, String node)
  + withMigrationsPath(Path): Tarantool3Container
  + withEtcdPrefix(String): Tarantool3Container
  + stop(): void
  + stopWithSafeMount(): void
  + mappedAddress(): InetSocketAddress
  + internalAddress(): InetSocketAddress
  + withEtcdAddresses(HttpHost[]): Tarantool3Container
  + withConfigPath(Path): Tarantool3Container
  + node(): String
  + start(): void
}
interface TarantoolContainer<SELF> << interface >> {
  + DEFAULT_TARANTOOL_PORT: int
  + DEFAULT_DATA_DIR: Path
  + restart(long, TimeUnit): void
  + withMigrationsPath(Path): TarantoolContainer<SELF>
  + internalAddress(): InetSocketAddress
  + stopWithSafeMount(): void
  + withConfigPath(Path): TarantoolContainer<SELF>
  + mappedAddress(): InetSocketAddress
  + node(): String
}


Tarantool3Container               -[#000082,plain]-^  GenericContainer
Tarantool3Container               -[#008200,dashed]-^  TarantoolContainer
@enduml
```

Класс `Tarantool3Container` позволяет создать объект контейнера `Tarantool 3.x`, удовлетворяющий
[контракту](./single-node-testcontainers-arch.md) `TarantoolContainer`.

### Описание реализации

#### Расположение монтируемых директорий и файлов

Реализация обеспечивает следующее поведение при конфигурировании контейнера:

```puml
@startuml
start
:Tarantool3Container::start();
if(Экземпляр\nсконфигурирован?) then (да)
  :return <b>Tarantool3Container.start()</b>;
  stop
else(нет)
  :Создать временную
  монтируемую директорию
  на хосте <b>mountDirectory</b>;
  :Смонитровать директории
  <b>mountDirectory</b> на хосте и
  <b>/data</b> в контейнере;
  if(Путь к файлу конфигурации\nсуществует и это регулярный\nфайл?) then(да)
    :Смонитровать путь к файлу
    конфигураци <b>pathToConfigFile</b>
    к файлу <b>/data/configFileName</b>
    в контейнере;
    :Оповестить клиента через
    <b>LOGGER::info(...)</b>, что файл
    конфигурации скопирован с
    указанием <b>input</b> и <b>target</b>
    файлов;
  else(нет)
    :Оповестить клиента через
    <b>LOGGER::warn(...)</b>,
    что файл конфигурации
    не задан, null, или
    не существует;
  endif
  if(Путь к директории\nc миграциями -\nрегулярный файл?) then(да)
    :Оповестить клиента
    через <b>LOGGER::warn(...)</b>,
    что передан путь к
    регулярному файлу;
  else if(Путь к директории с миграциями null?) then(да)
    :Оповестить клиента
    через <b>LOGGER::warn(...)</b>,
    что путь к директории
    с миграциями не
    существует или null;
  else(нет)
    :Смонтировать директорию
    к миграциям <b>migrationsDir</b>
    к директории
    <b>/data/migrationsDirName</b>
    в контейнере;
    :Оповестить клиента через
    <b>LOGGER::info(...)</b>, что
    директория с миграциями
    скопирована с указанием
    <b>input</b> и <b>target</b> директорий;
  endif
  if(Переданы ли адреса к etcd?) then (да)
    :Использовать конфигурацию
    из <b>etcd</b>. Задать адреса и
    префиксы через переменные
    <b>TT_CONFIG_ETCD_ENDPOINTS</b>,
    <b>TT_CONFIG_ETCD_PREFIX</b>;
  else(нет)
    :Использовать конфигурацию,
    заданную в конфигурационном
    файле. Задать путь к файлу
    конфигурации через переменную
    <b>TT_CONFIG</b>;
endif
:return <b>Tarantool3Container::start()</b>;
stop
@enduml
```

#### Обеспечение сохранения монтируемых данных

Согласно контракту `TarantoolContainer` при вызове метода `TarantoolContainer::stopWithSafeMount()`
и последующего вызова `TarantoolContainer::start()` монтируемые данные должны сохраниться.
`Tarantool3Container` реализует этот механизм следующим образом:

=== "Tarantool3Container::start()"

    ```puml
    @startuml
    start
    if(Контейнер закрыт через <b>Tarantool3Container::stop()</b>?) then (да)
      :Выбросить исключение;
      end
    else(нет)
      if (Контейнер уже сконфигурирован?) then(да)
      else (no)
        :Сконфигурировать контейнер;
        :Обозначить, что контейнер сконфигурирован;
      endif
    endif
    :return <b>Tarantool3Container::start()</b>;
    stop
    @enduml
    ```

=== "Tarantool3Container::stopWithSafeMount()"

    ```puml
    @startuml
    start
    if(Контейнер закрыт через Tarantool3Container::stop()?) then (да)
    else(нет)
      :Остановить контейнер;
    endif
    :return <b>Tarantool3Container::stopWithSafeMount()</b>;
    stop
    @enduml
    ```

=== "Tarantool3Container::stop()"

    ```puml
    @startuml
    start
    if(Контейнер закрыт через Tarantool3Container::stop()?) then (да)
    else(нет)
      :Удалить монтируемые директории;
      :Остановить контейнер;
      :Обозначит, что контейнер остановлен;
    endif
    :return <b>Tarantool3Container::stop()</b>;
    stop
    @enduml
    ```

Удаление монтируемых директорий происходит только при вызове метода `Tarantool3Container::stop()`.
Конфигурирование контейнера происходит один раз при первом вызове `Tarantool3Container::start()`.

### Привязка портов

На этапе конфигурирования контейнера привязка портов не производится. Привязка портов производится
после запуска контейнера `Tarantool3Container::start()`.
