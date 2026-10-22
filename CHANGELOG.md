# Changelog

## [Unreleased]

## [1.7.3] - 2026-10-30

### Pool

- Introduced `AvailabilityPolicy`, `PoolEntryView`, and `PoolEntryState` abstractions
  to lay the groundwork for custom node availability strategies, moving away
  from explicit heartbeat checks in the future:
  - Added `AvailabilityPolicy` interface with a strictly managed lifecycle.
  - Added `PoolEntryView` to expose safe, non-blocking connection controls.
  - Added `PoolEntryState` abstract class to support custom immutable state values.

### Dependencies
- Updated dependencies:

  Compile:
    * Bump cartridge-java version from 0.13.0 to 0.14.3 in documentation module
    * Bump commons-codec version from 1.22.0 to 1.23.0
    * Bump commons-io version from 2.21.0 to 2.22.0
    * Bump commons-lang3 version from 3.19.0 to 3.21.0
    * Bump guava version from 33.5.0-jre to 33.7.2-jre
    * Bump httpclient5 version from 5.5.2 to 5.6.5
    * Bump httpcore5 version from 5.4.3 to 5.4.4
    * Bump jackson version from 2.21.6 to 2.22.4
    * Bump jetcd version from 0.8.6 to 0.8.7
    * Bump lombok version from 1.18.44 to 1.18.48
    * Bump micrometer version from 1.16.6 to 1.17.2
    * Bump netty version from 4.2.16.Final to 4.2.19.Final
    * Bump opentelemetry version from 1.62.0 to 1.67.0
    * Bump slf4j version from 2.0.17 to 2.0.20
    * Bump spring-boot version from 4.0.6 to 4.0.9 in tarantool-spring-data-40
    * Pin spring-data-keyvalue to 4.0.8 in tarantool-spring-data-40

  Tests:
    * Bump grpc version from 1.77.0 to 1.85.1
    * Bump instancio version from 5.5.1 to 5.6.0
    * Bump junit-jupiter version from 5.14.3 to 5.14.4

  Maven:
    * Bump central-publishing-maven-plugin from 0.10.0 to 0.12.0
    * Bump jacoco-maven-plugin from 0.8.14 to 0.8.16
    * Bump maven-compiler-plugin from 3.15.0 to 3.16.0
    * Bump maven-enforcer-plugin from 3.6.2 to 3.6.3
    * Bump maven-surefire-plugin from 3.5.5 to 3.6.1
    * Bump protobuf-maven-plugin from 5.1.4 to 5.1.11
    * Bump spotbugs version from 4.9.8.3 to 4.10.5
    * Bump spotbugs-maven-plugin from 4.9.8.3 to 4.10.5.0

### Bug fixes

- Fixed a bug: the timeout in IProtoBegin was being specified in milliseconds (as a `long`), but
  the [protocol](https://www.tarantool.io/ru/doc/latest/reference/internals/iproto/streams/#iproto-begin)
  requires the timeout to be passed in seconds (as a `double`).
- Fixed a bug where the `Connection#connect()` method would leave the connection in a `CONNECTING` 
  state if a failure occurred before the channel was created (for example, when an 
  invalid—non-positive—timeout value was provided). As a result, all subsequent calls to this 
  method would return `null` instead of a `CompletableFuture`. The method now always returns a 
  `CompletableFuture` object, which either completes successfully (upon receipt of a greeting) or 
  terminates with an error. Providing not positive timeout value throws an exception.
- Fixed connection close callbacks executing before the connection state becomes closed.
- Fixed greeting object remaining non-null after connection closure.
- Fixed the `VshardClusterContainer` readiness check: it read a `replicas` field that
  `vshard.router.info()` does not return, so the container could be reported ready while a
  replicaset master was still in the vshard handshake, and the first CRUD request failed with
  `VHANDSHAKE_NOT_COMPLETE`. The check now makes a synchronous `callrw` to every replicaset master,
  which reopens a router connection that vshard closed after a failed handshake and waits for the
  new one, requires every master to be `available`, and logs the replica and bucket states.
- Fixed a bug when a server that accepts the TCP connection but does not send the greeting was
  reported as "Connection closed by server". The connect future is now failed only with
  `TimeoutException`, and no `CLOSE_BY_REMOTE` event is fired. Previously the connection pool
  handled every such attempt twice, so the reconnect chains multiplied while the server stayed
  silent. The greeting timer of an attempt that the server closed no longer closes the channel of
  the next attempt.
- Fixed reconnect chains multiplying when a server (or a proxy in front of it) accepts the TCP
  connection and closes it before the greeting: the close was reported both by the failed connect
  future and by a `CLOSE_BY_REMOTE` event, and the pool handled each report. Close events are now
  fired only for a connection that has received the greeting.
- Fixed a leak when graceful shutdown is enabled: every connect attempt added one more
  `box.shutdown` callback to the same watcher, so a client that kept reconnecting accumulated
  callbacks, and a `box.shutdown` event after a successful connect ran the callback once per
  previous attempt. The callback is now added once per client.
- Fixed the `pool.connect.time` timer of the connection pool: it was stopped only when an
  attempt succeeded, so every failed attempt left an active sample behind.
- Fixed `reconnectAfter` being ignored for the connections created with the client: the client
  builder set it on the pool after the connections had been created with the default of 1000 ms,
  and the pool did not pass the new value to existing connections. `withReconnectAfter` and the
  `reconnect-after` property of Spring Data now take effect.

### Documentation

- Fixed broken links in `tarantool-spring-data/README.md` and `testcontainers/README.md`:
  they pointed to `doc/` directories that do not exist and now point to the pages under
  `documentation/doc-src/pages`.
- Documented that the spring-data `Repository` layer needs a cluster with `crud` and `vshard`,
  and listed three ways to get one.
- Explained in the spring-data configuration example that `auth-type` must match
  `security.auth_type` of the cluster and that `pap-sha256` requires Enterprise Edition and TLS.
- Added the "Limitations and workarounds" page: no batch delete and no `IN` condition in `crud`,
  examples of parallel requests instead, and derived-method keywords that are not supported.
- Added a table that maps Spring Boot minor lines to `tarantool-spring-data-*` modules.

## [1.7.2] - 2026-09-25

### Client

- Support ISO 8601 duration parsing and formatting for `Interval`

### Testcontainers

- Accept `grpc_options.tls` in the TQE gRPC configuration

### Bug fixes

- Make `IProtoResponseImpl.toString()` idempotent — repeated calls no longer duplicate
  the output
- Handle `ER_AUTH_REQUIRED` for watcher registration. Watchers (including the automatic
  `box.shutdown` watcher created when `gracefulShutdown` is enabled) are now deferred when
  the server rejects their registration before authentication and re-registered after
  `authorize()` completes. Tarantool EE builds with option `security.disable_guest: true`
  reject pre-authentication watcher registration with `ER_AUTH_REQUIRED`; since 1.7.0 that
  watcher error failed the whole connect procedure and put the connection into an endless
  reconnect loop.

## [1.7.1] - 2026-08-31

### Dependencies
- Updated dependencies:

  Compile:
  * Bump jsonschema2pojo version from 1.2.2 to 1.3.3
  * Bump jackson version from 2.21.3 to 2.21.6
  * Bump micrometer version from 1.16.5 to 1.16.6
  * Bump netty version from 4.2.15.Final to 4.2.16.Final
  * Bump org.apache.httpcomponents.core5 version from 5.4.2 to 5.4.3
  * Pin `spring-data-keyvalue` to 3.5.13 in `tarantool-spring-data-35` 
    (parent defaults it to `${spring.boot.version}` = 3.5.16, which does not exist on Maven Central)
  * Bump spring-boot version from 3.5.13 to 3.5.16 in tarantool-spring-data-35

### Bug fixes

- Remove the root-level `logback.xml` from the `tarantool-core` runtime JAR.
  Logback auto-discovered it as application-level config in consumer apps
  (e.g. Spring Boot with `logback-spring.xml`), overriding the host's logging
  configuration. [#109](https://github.com/tarantool/tarantool-java-sdk/issues/109)
- Pass the peer address to the SSL engine, so that a TLS connection can verify the server
  certificate against the host it was opened for. Endpoint identification is enabled by default
  since netty 4.2, and without the address the JDK aborted every handshake with
  `Hostname or IP address is undefined`, which made `withSslContext` unusable.

## [1.7.0] - 2026-06-29

### Spring-data

- Add `tarantool-spring-data-40` module with support for Spring Boot 4.0.x and Spring Data 4.0.x

### Bug fixes

- Fix flaky `ConnectionPoolReconnectsTest` assertions on active connection count by polling 
  `box.stat.net().CONNECTIONS.current` from Lua until it holds the same value across 
  5 consecutive reads at 100ms intervals, instead of a single read that races with the IProto worker

### Testcontainers

- Add TQE 3.x (message-queue-ee 3.x) integration on top of the
  existing TQE 2.x support; consolidate the TQE 2.x / 3.x test
  surface into a single parameterized suite.
- Add constructor/builder parameters to supply the initial Lua script as a string or as a file path, 
  and optional additional script paths copied into the container data directory 
  (`Tarantool2Container`, `CartridgeClusterContainer`, `VshardClusterContainer`); 
  simplify bundled `server.lua` accordingly.
- Upgrade TQE to v3.5.0.
- Extract `ObjectMapper` to a static field in the test `tdg.Utils` helper to avoid recreating it on every `sendUsers`/`getUsers` call.
- Wait for vshard storages to complete the handshake (`vshard.router.info()` reports every replica as `available` and no unreachable buckets) before declaring a `VshardClusterContainer` ready, preventing intermittent `VHANDSHAKE_NOT_COMPLETE` CRUD failures right after bootstrap.

### Client

- Append a default opts map to the varargs `upsert`/`upsertObject` calls so the request reaches the CRUD rock as the 4-arg form; CRUD 1.5.0+'s conditions parser rejects the legacy 3-arg form with `ParseConditionError`.

### Documentation

- Document supported Java types for Tarantool data mapping in `tuple_pojo_mapping` docs (RU/EN), 
  including Tarantool extension types (`decimal`, `uuid`, `datetime`, `interval`, `tuple`) and related mapping notes.
- Document Jackson MsgPack deserialization: integers, `bin`/`str` vs `byte[]`/`String`, floating-point vs `decimal`; 
  reference `jackson-dataformat-msgpack` for defaults and type coercion.

### Dependencies
- Updated dependencies:

  Compile:
    * Bump jackson version from 2.18.4 to 2.21.3
    * Bump msgpack version from 0.9.10 to 0.9.12
    * Bump netty version from 4.2.7.Final to 4.2.15.Final
    * Bump httpclient5 version from 5.5 to 5.5.2
    * Bump httpcore5 version from 5.3.4 to 5.4.2
    * Bump spring-boot version from 3.5.8 to 3.5.13 in tarantool-spring-data-35
    * Add spring-boot version 4.0.6 in tarantool-spring-data-40
    * Bump micrometer version from 1.16.4 to 1.16.5

  Tests:
    * Bump testcontainers version from 2.0.1 to 2.0.5
    * Bump junit-jupiter version from 5.13.4 to 5.14.4
    * Bump logback version from 1.5.21 to 1.5.32 in spring-data module
    * Bump grpc version from 1.76.0 to 1.77.0
    * Bump protobuf version from 3.25.8 to 3.25.9
    * Bump commons-io version from 2.20.0 to 2.21.0
    * Bump commons-codec version from 1.19.0 to 1.22.0
    * Bump opentelemetry version from 1.48.0 to 1.62.0

  Maven: 
    * Bump Maven version from 3.9.11 to 3.9.16
    * Bump maven-enforcer-plugin from 3.6.1 to 3.6.2
    * Bump maven-release-plugin from 3.1.1 to 3.3.1
    * Bump maven-compiler-plugin from 3.14.0 to 3.15.0
    * Bump maven-surefire-plugin from 3.5.4 to 3.5.5
    * Bump maven-pmd-plugin from 3.27.0 to 3.28.0
    * Bump spotbugs-maven-plugin from 4.9.8.1 to 4.9.8.3
    * Bump maven-resources-plugin from 3.3.1 to 3.5.0
    * Bump maven-source-plugin from 3.3.1 to 3.4.0
    * Bump maven-wrapper from 3.3.2 to 3.3.4
    * Bump jacoco-maven-plugin from 0.8.13 to 0.8.14
    * Bump maven-antrun-plugin from 3.1.0 to 3.2.0
    * Bump central-publishing-maven-plugin from 0.9.0 to 0.10.0

## [1.6.0] - 2026-04-01

### BOM Module

- Add `tarantool-java-sdk-bom` module providing centralized dependency management for all Tarantool Java SDK modules and their dependencies

### Testcontainers

- Remove dependency on `testcontainers-java-tarantool` library
- Update all integration tests to use new container implementation

### Bug fixes

- Fix a memory leak due to the use of a local thread pool

### Spring-data
- Add `tarantool-spring-data-35` module with support for Spring Boot 3.5.x and Spring Data 3.5

### Client
- Add `TupleMapper` utility class for easy tuple-to-POJO mapping using field format
- Update CRUD operations (select, insert, get) to propagate format metadata from `CrudResponse`
- Add `Handlers` class with 4 lifecycle callbacks for request/response tracing:
  - `onBeforeSend`: called before sending request
  - `onSuccess`: called on successful response
  - `onTimeout`: called when request times out
  - `onIgnoredResponse`: called when response arrives after timeout
- Handlers are configured at client level via `withHandlers()` builder method

### Dependencies
- Updated dependencies:
  Compile:
    * Bump spring-boot version from 3.4.11 to 3.4.13 in tarantool-spring-data-34

## [1.5.0] - 2025-12-25

- Change deprecated API to actual variant:
    - Netty:
        - `NioEventLoopGroup` &rarr; `MultiThreadIoEventLoopGroup`
    - Spring-data:
        - `KeyValuePersistentEntity#setFallbackKeySpaceResolver` &rarr;
          `KeyValuePersistentEntity#setKeySpaceResolver`
        - `ClassTypeInformation#from` &rarr; `TypeInformation#of`
        - `MockBean` &rarr; `MockitoBean`
    - Yaml:
        - `Representer#Representer` &rarr; `Representer#Representer(new DumperOptions())`
    - testcontainers:
        - `ExecInContainerPattern`:
          `execInContainer(InspectContainerResponse containerInfo, String... command)` &rarr;
          `execInContainer(DockerClient dockerClient, InspectContainerResponse containerInfo, String... command)`
        - `Tarantool3Configuration` &rarr; `Tarantool3Configuration` (autogenerated)

### Breaking changes

- Remove old `Tarantool3Configuration` class from `testcontainers` module. Add new autogenerated
  `Tarantool3Configuration` class.

### Pooling & Client API
- Added PoolEventListener support across pooling and client builders, including lifecycle callbacks.

### Testcontainers
- Updated artifactId from `junit-jupiter` to `testcontainers-junit-jupiter` across all modules
- Removed unused Jakarta validation and el dependencies
- Cleanup test dependencies

### Client
- Migrated from JUnit 4 assertions to JUnit 5 assertions in TarantoolBoxClientTest
- Added ManagedResource to provide single closing point of external resources.

### Internal Changes
- Changed import for ArrayUtils from shaded testcontainers package to direct Apache Commons Lang3 package in VshardClusterContainer
- Restructured dependency declarations in parent pom.xml, moving commons-lang3 scope declaration and reordering lombok dependency

### Dependencies
- Updated dependencies:

  Compile:
    * Bump netty version from 4.2.6.Final to 4.2.7.Final
    * Bump micrometer version from 1.14.11 to 1.14.12
    * Bump spring-boot version from 3.4.10 to 3.4.11 in tarantool-spring-data-34
    * Bump logback version from 1.5.18 to 1.5.21 in spring-data module

  Tests:
    * Bump testcontainers from 1.21.3 to 2.0.1
    * Bump testcontainers-java-tarantool from 1.4.0 to v1.5.0
    * Bump logback-classic from 1.3.15 to 1.3.16
    * Bump jetcd version from 0.8.5 to 0.8.6
    * Bump grpc-stub version from 1.75.0 to 1.76.0
    * Bump instancio-core version from 5.5.0 to 5.5.1
    * Bump commons-io version from 2.16.1 to 2.20.0
    * Bump junit-jupiter version from 5.13.3 to 5.13.4

  Maven:
    * Bump maven-surefire-plugin from 3.5.3 to 3.5.4
    * Bump spotbugs-maven-plugin from 4.9.3.2 to 4.9.8.1
