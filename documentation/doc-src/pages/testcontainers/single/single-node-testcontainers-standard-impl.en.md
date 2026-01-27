---
title: Standard Implementation of TarantoolContainer
---

## Standard Implementation of the `TarantoolContainer` Interface

This page describes the standard implementation of the `TarantoolContainer` interface.

### Class Diagram

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

The `Tarantool3Container` class allows you to create a `Tarantool 3.x` container object that satisfies the [contract](single-node-testcontainers-arch.md) of `TarantoolContainer`.

### Implementation Description

#### Mounting Directories and Files Location

The implementation ensures the following behavior when configuring the container:

```puml
@startuml
start
:Tarantool3Container::start();
if(Instance\nconfigured?) then (yes)
  :return <b>Tarantool3Container.start()</b>;
  stop
else(No)
  :Create a temporary
  mount directory
  on the host <b>mountDirectory</b>;
  :Mount directories
  <b>mountDirectory</b> on the host and
  <b>/data</b> in the container;
  if(Does the configuration file\nexist and is it a regular\nfile?) then(Yes)
    :Mount the path to the configuration file
    <b>pathToConfigFile</b>
    to the file <b>/data/configFileName</b>
    in the container;
    :Notify the client via
    <b>LOGGER::info(...)</b>, that the configuration file
    was copied with
    specifying <b>input</b> and <b>target</b>
    files;
  else(No)
    :Notify the client via
    <b>LOGGER::warn(...)</b>,
    that the configuration file
    is not set, null, or
    does not exist;
  endif
  if(Does the path to the migration directory\npoint to a regular file?) then(Yes)
    :Notify the client
    via <b>LOGGER::warn(...)</b>,
    that a path to
    a regular file was passed;
  else if(Does the path to the migration directory null?) then(Yes)
    :Notify the client
    via <b>LOGGER::warn(...)</b>,
    that the path to the migration directory
    does not exist or is null;
  else(No)
    :Mount the migration directory
    <b>migrationsDir</b>
    to the directory
    <b>/data/migrationsDirName</b>
    in the container;
    :Notify the client via
    <b>LOGGER::info(...)</b>, that
    the migration directory
    was copied with specifying
    <b>input</b> and <b>target</b> directories;
  endif
  if(Do etcd addresses\nwere passed?) then (Yes)
    :Use configuration
    from <b>etcd</b>. Set addresses and
    prefixes via variables
    <b>TT_CONFIG_ETCD_ENDPOINTS</b>,
    <b>TT_CONFIG_ETCD_PREFIX</b>;
  else(No)
    :Use configuration
    specified in the configuration
    file. Set the path to the configuration file
    via the variable
    <b>TT_CONFIG</b>;
endif
:return <b>Tarantool3Container::start()</b>;
stop
@enduml
```

#### Ensuring Mounted Data Preservation

According to the `TarantoolContainer` contract, when calling the `TarantoolContainer::stopWithSafeMount()` method and then calling `TarantoolContainer::start()` again, the mounted data must be preserved. `Tarantool3Container` implements this mechanism as follows:

=== "Tarantool3Container::start()"

    ```puml
    @startuml
    start
    if(Is the container closed via <b>Tarantool3Container::stop()</b>?) then (Yes)
      :Throw an exception;
      end
    else(No)
      if (Is the container already configured?) then(Yes)
      else (No)
        :Configure the container;
        :Mark that the container is configured;
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
    if(Is the container closed via Tarantool3Container::stop()?) then (Yes)
    else(No)
      :Stop the container;
    endif
    :return <b>Tarantool3Container::stopWithSafeMount()</b>;
    stop
    @enduml
    ```

=== "Tarantool3Container::stop()"

    ```puml
    @startuml
    start
    if(Is the container closed via Tarantool3Container::stop()?) then (Yes)
    else(No)
      :Delete mounted directories;
      :Stop the container;
      :Mark that the container is stopped;
    endif
    :return <b>Tarantool3Container::stop()</b>;
    stop
    @enduml
    ```

Mounted directories are deleted only when calling the `Tarantool3Container::stop()` method. Container configuration occurs only once during the first call to `Tarantool3Container::start()`.

### Port Binding

Port binding is not performed at the container configuration stage. Port binding is performed after the container starts `Tarantool3Container::start()`.
