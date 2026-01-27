---
title: Примеры
---

## Пример 1. Запуск узла Tarantool 3.x

### Шаг 1. Определение конфигурации Tarantool 3.x

Создадим конфигурационный файл `config.yaml` по произвольному пути (воспользуемся средствами
`JUnit` - `@TempDir`, для создания временной директории):

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.Credentials;
import io.tarantool.autogen.credentials.users.Users;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;
import io.tarantool.autogen.groups.Groups;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.Replicasets;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.Instances;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.Iproto;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.listen.Listen;

public class TestClass {

  private static final String NODE = "test-node";

  private static final CharSequence PWD = "secret";

  private static final String LOGIN = "test-user";

  @TempDir
  private static Path tempDir;

  @Test
  public void test() throws IOException {
    final Path pathToConfigFile = Files.createFile(tempDir.resolve("config.yaml"));

    final Credentials credentials = Credentials.builder()
        .withUsers(Users.builder()
            .withAdditionalProperty(LOGIN, UsersProperty.builder()
                .withRoles(Collections.singletonList("super"))
                .withPassword(PWD.toString())
                .build())
            .build())
        .build();

    final Iproto iproto = Iproto.builder()
        .withListen(List.of(Listen.builder().withUri("0.0.0.0:3301").build()))
        .build();

    final InstancesProperty instance = InstancesProperty.builder()
        .withIproto(iproto).build();

    final ReplicasetsProperty replicaset = ReplicasetsProperty.builder()
        .withInstances(Instances.builder()
            .withAdditionalProperty(NODE, instance)
            .build())
        .build();

    final GroupsProperty group = GroupsProperty.builder()
        .withReplicasets(Replicasets.builder()
            .withAdditionalProperty("test-rs", replicaset)
            .build())
        .build();

    final Tarantool3Configuration configuration = Tarantool3Configuration.builder()
        .withGroups(Groups.builder()
            .withAdditionalProperty("test-group", group)
            .build())
        .withCredentials(credentials)
        .build();

    ConfigurationUtils.writeToFile(configuration, pathToConfigFile);
  }
}
```

В результате, получили конфигурационный файл по пути `tmpDir/config.yaml` со следующим содержимым:

```yaml
---
credentials:
  users:
    test-user:
      password: "secret"
      roles:
        - "super"
groups:
  test-group:
    replicasets:
      test-rs:
        instances:
          test-node:
            iproto:
              listen:
                - uri: "0.0.0.0:3301"
```

### Шаг 2. Создание и запуск контейнера

Создание файла конфигурации из `Шаг 1. Определение конфигурации Tarantool 3.x` было вынесено в
отдельный метод:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.testcontainers.containers.tarantool.config.tarantool3.Tarantool3Configuration;
import org.testcontainers.containers.tarantool.config.tarantool3.credentials.Credentials;
import org.testcontainers.containers.tarantool.config.tarantool3.groups.Group;
import org.testcontainers.containers.tarantool.config.tarantool3.groups.Instance;
import org.testcontainers.containers.tarantool.config.tarantool3.groups.Replicaset;
import org.testcontainers.containers.tarantool.config.tarantool3.iproto.Listen;

public class TestClass {

  private static final String NODE = "test-node";

  private static final CharSequence PWD = "secret";

  private static final String LOGIN = "test-user";

  @TempDir
  private static Path tempDir;

  @Test
  public void test() throws IOException {
    final Path pathToConfigFile = createSimpleTestConfigFile(tempDir);
  }

}
```

Создадим контейнер для `Tarantool 3.6.0` и запустим его:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.tarantool.Tarantool3Container;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.tarantool.config.ConfigurationUtils;
import org.testcontainers.utility.DockerImageName;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.Credentials;
import io.tarantool.autogen.credentials.users.Users;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;
import io.tarantool.autogen.groups.Groups;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.Replicasets;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.Instances;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.Iproto;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.iproto.listen.Listen;

public class TestClass {

  private static final String NODE = "test-node";

  private static final CharSequence PWD = "secret";

  private static final String LOGIN = "test-user";

  @TempDir
  private static Path tempDir;

  @Test
  public void test() throws IOException, InterruptedException {
    final Path pathToConfigFile = createSimpleTestConfigFile(tempDir);

    final DockerImageName image = DockerImageName.parse("tarantool/tarantool:3.6.0");

    // NODE должен соответствовать instance name в конфигурационном файле
    try (TarantoolContainer<Tarantool3Container> container = new Tarantool3Container(image, NODE)
        .withConfigPath(pathToConfigFile)) {
      container.start();

      final String COMMAND_FORMAT = "echo \"box.cfg.instance_name\" | tt connect %s:%s@localhost:3301 -x lua";
      final String result = container.execInContainer("/bin/sh", "-c",
          String.format(COMMAND_FORMAT, LOGIN, PWD)).getStdout();

      Assertions.assertEquals("\"" + container.node() + "\";\n", result);
    } // call container.stop()
  }

  private static Path createSimpleTestConfigFile(Path tempDir) throws IOException {
    final Path pathToConfigFile = Files.createFile(tempDir.resolve("config.yaml"));

    final Credentials credentials = Credentials.builder()
        .withUsers(Users.builder()
            .withAdditionalProperty(LOGIN, UsersProperty.builder()
                .withRoles(Collections.singletonList("super"))
            .withPassword(PWD.toString())
            .build())
        .build())
        .build();

    final Iproto iproto = Iproto.builder()
        .withListen(List.of(Listen.builder().withUri("0.0.0.0:3301").build()))
        .build();

    final InstancesProperty instance = InstancesProperty.builder()
        .withIproto(iproto).build();

    final ReplicasetsProperty replicaset = ReplicasetsProperty.builder()
        .withInstances(Instances.builder()
            .withAdditionalProperty(NODE, instance)
            .build())
        .build();

    final GroupsProperty group = GroupsProperty.builder()
        .withReplicasets(Replicasets.builder()
            .withAdditionalProperty("test-rs", replicaset)
            .build())
        .build();

    final Tarantool3Configuration configuration = Tarantool3Configuration.builder()
        .withGroups(Groups.builder()
            .withAdditionalProperty("test-group", group)
            .build())
        .withCredentials(credentials)
        .build();

    ConfigurationUtils.writeToFile(configuration, pathToConfigFile);
    return pathToConfigFile;
  }
}
```

В результате работы вы получите аналогичные логи:

```
15:15:31.769 [main] INFO  tc.tarantool/tarantool:3.6.0 - Creating container for image: tarantool/tarantool:3.6.0
15:15:32.185 [main] INFO  tc.tarantool/tarantool:3.6.0 - Container tarantool/tarantool:3.6.0 is starting: eb542dfac096dead65b1f00e39f1926d4dfc8267dd0e8b3a7f4ecae62d02d306
15:15:32.382 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: started
15:15:32.395 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.364 [1] main/104/interactive main.cc:497 I> Tarantool 3.6.0-0-g9a006b00642 Linux-aarch64-RelWithDebInfo
15:15:32.395 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.364 [1] main/104/interactive main.cc:499 I> log level 5 (INFO)
15:15:32.395 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.364 [1] main/104/interactive gc.c:131 I> wal/engine cleanup is paused
15:15:32.396 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.365 [1] main/104/interactive tuple.c:411 I> mapping 268435456 bytes for memtx tuple arena...
15:15:32.396 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.365 [1] main/104/interactive memtx_engine.cc:1778 I> Actual slab_alloc_factor calculated on the basis of desired slab_alloc_factor = 1.044274
15:15:32.396 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.366 [1] main/104/interactive tuple.c:411 I> mapping 134217728 bytes for vinyl tuple arena...
15:15:32.401 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.370 [1] main/104/interactive box.cc:2471 I> update replication_synchro_quorum = 1
15:15:32.401 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.370 [1] main/104/interactive box.cc:3556 I> The option replication_synchro_queue_max_size will actually take effect after the recovery is finished
15:15:32.401 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.370 [1] main/104/interactive box.cc:5747 I> instance uuid 9a492c41-5ff0-453d-8c13-658af696c68a
15:15:32.401 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.371 [1] main/104/interactive evio.c:284 I> tx_binary: bound to 0.0.0.0:3301
15:15:32.401 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.371 [1] main/104/interactive memtx_engine.cc:734 I> initializing an empty data directory
15:15:32.417 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.387 [1] main/104/interactive replication.cc:576 I> assigned id 1 to replica 9a492c41-5ff0-453d-8c13-658af696c68a
15:15:32.417 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.387 [1] main/104/interactive replication.cc:594 I> assigned name test-node to replica 9a492c41-5ff0-453d-8c13-658af696c68a
15:15:32.417 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.387 [1] main/104/interactive box.cc:2471 I> update replication_synchro_quorum = 1
15:15:32.417 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.387 [1] main/104/interactive alter.cc:4210 I> replicaset uuid b4acbd1b-6f18-4270-b0b1-ecbf0f92534e
15:15:32.417 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.387 [1] main/104/interactive alter.cc:4223 I> replicaset name: test-rs
15:15:32.418 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.388 [1] snapshot/101/main memtx_engine.cc:1079 I> saving snapshot `/data/00000000000000000000.snap.inprogress'
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] snapshot/101/main memtx_engine.cc:1181 I> done
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive box.cc:682 I> leaving waiting_for_own_rows mode
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive box.cc:6262 I> ready to accept requests
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/107/gc gc.c:319 I> wal/engine cleanup is resumed
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'custom_proc_title' configuration option to "tarantool - test-node"
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'instance_name' configuration option to "test-node"
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'log_nonblock' configuration option to false
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'listen' configuration option to [{"uri":"0.0.0.0:3301"}]
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'replication' configuration option to []
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'replicaset_name' configuration option to "test-rs"
15:15:32.419 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'instance_uuid' configuration option to "9a492c41-5ff0-453d-8c13-658af696c68a"
15:15:32.420 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive/box.load_cfg load_cfg.lua:966 I> set 'metrics' configuration option to {"labels":{"alias":"test-node"},"include":["all"],"exclude":[]}
15:15:32.420 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/104/interactive box.cc:444 I> box switched to rw
15:15:32.420 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.389 [1] main/108/checkpoint_daemon gc.c:650 I> scheduled next checkpoint for Wed Aug 27 13:35:30 2025
15:15:32.421 [docker-java-stream-1077198046] INFO  o.t.c.tarantool.Tarantool3Container - [test-node] STDERR: 2025-08-27 12:15:32.391 [1] main main.cc:1072 I> entering the event loop
15:15:32.451 [main] INFO  tc.tarantool/tarantool:3.6.0 - Container tarantool/tarantool:3.6.0 started in PT0.682201S
```
