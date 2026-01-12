---
title: Standard Implementation
---

This page describes the standard implementation of the `TDBCluster` interface for `TDB 2.x`.

## Class Diagram

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

The `TDB2ClusterImpl` class allows you to create an object managing the lifecycle of a TDB 2.x cluster, satisfying the [contract](tdb-cluster-testcontainers-arch.md) of `TDBCluster`.

## Implementation Description

### Cluster Configuration

The implementation follows the following algorithm when configuring the cluster:

```puml
@startuml
(*) --> "Calling <b>TDB2ClusterImpl::start()" as s1

s1 --> "Create a temporary directory
for the <b>TDB2ClusterImpl</b> instance" as s2

if "Configuration passed via\n<b>Tarantool3Configuration</b>?" then
  -r-> [no]  "Generate configuration
  based on the passed <b>routerCount</b>,
  <b>shardCount</b>, <b>replicaCount</b>" as s3
else
  -d-> [yes] "Write the configuration file
  based on the configuration object to
  a temporary directory" as s4
endif
s3 -r-> s4

s4 --> "Create and configure
<b>etcd</b>, <b>TCM</b> containers" as s5

if "Successfully\nstarted?" then
  -d->[no] "**Throw an exception**" as exc1
else
  -d->[yes] "Publish the cluster
  configuration via <b>TCM</b> to <b>etcd</b>" as s6
endif

if "Configuration\ntransferred?" then
  -r->[no] exc1
else
  -->[yes] "Start the containers
  with <b>Tarantool</b> nodes" as s7
endif

if "Containers\nstarted?" then
  -r->[no] exc1
else
  -->[yes] "Initialize
  the cluster" as s8
endif

if "Cluster\ninitialized?" then
  -r->[no] exc1
else
  -->[yes] "Apply migrations" as s9
endif

if "Migrations\napplied?" then
  -r->[no] exc1
else
  -->[yes] "Complete startup"
  --> (*)
endif
exc1 --> (*)
@enduml
```

#### Ensuring Mounted Data Preservation

According to the `TDBCluster` contract, when calling the `TDBCluster::restart(...)` method, the mounted data must be preserved. `TDB2ClusterImpl` implements this mechanism by using the `TarantoolContainer::stopWithSafeMount()` contract:

```puml
@startuml
start
:Calling <b>TDB2ClusterImpl::restart(...)</b>;
package "TDB2ClusterImpl::restart" {
  :Call <b>Tarantool3Container::stopWithSafeMount()</b>
  for each <b>Tarantool</b> node in the cluster;
  :Wait for the specified delay;
  :Call <b>Tarantool3Container::start()</b>
  for each <b>Tarantool</b> node in the cluster;
}
stop
@enduml
```

Mounted directories are deleted only when calling the `TDB2ClusterImpl::stop()` method. Container configuration occurs only once during the first call to `TDB2ClusterImpl::start()`.

### Port Binding

External port binding to cluster components (containers) occurs at the `TDB2ClusterImpl` instance startup stage.
