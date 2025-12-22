/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data31.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import static io.tarantool.balancer.BalancerMode.DEFAULT_BALANCER_MODE;
import static io.tarantool.client.TarantoolClient.DEFAULT_TAG;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_PASSWORD;
import static io.tarantool.client.crud.TarantoolCrudClient.DEFAULT_CRUD_USERNAME;
import static io.tarantool.core.protocol.requests.IProtoAuth.DEFAULT_AUTH_TYPE;
import static io.tarantool.pool.InstanceConnectionGroup.DEFAULT_HOST;
import static io.tarantool.spring.data.ProxyTarantoolQueryEngine.unwrapTuples;
import io.tarantool.balancer.BalancerMode;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;
import io.tarantool.core.protocol.requests.IProtoAuth;
import io.tarantool.mapping.Tuple;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties.PropertyFlushConsolidationHandler;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties.PropertyHeartbeatOpts;
import io.tarantool.spring.data.config.properties.BaseTarantoolProperties.PropertyInstanceConnectionGroup;
import io.tarantool.spring.data31.config.properties.TarantoolProperties;
import io.tarantool.spring.data31.utils.entity.ComplexPerson;
import io.tarantool.spring.data31.utils.entity.Person;

public class TarantoolTestSupport {

  private static final ThreadLocalRandom random = ThreadLocalRandom.current();
  private static final Map<String, Object> firstInternalMap = new HashMap<>();
  private static final Map<String, Object> secondInternalMap = new HashMap<>();
  private static final Map<String, Object> externalMap = new HashMap<>();
  private static final Representer representer;
  public static final String PERSON_SPACE = "person";
  public static final String COMPLEX_PERSON_SPACE = "complex_person";
  public static final int PERSONS_COUNT = 100;
  public static final Random RANDOMIZER = new Random();
  public static final Person UNKNOWN_PERSON = new Person(-1, true, "Alien");
  public static final ComplexPerson UNKNOWN_COMPLEX_PERSON =
      new ComplexPerson(-1, new UUID(0, 0), true, "Alien");

  public static final Path DEFAULT_TEST_PROPERTY_DIR = Paths.get("target", "test-classes");
  private static final Yaml yaml;

  static {
    representer = new Representer(new DumperOptions()) {
      @Override
      @Nullable
      protected NodeTuple representJavaBeanProperty(Object javaBean,
          Property property,
          @Nullable Object propertyValue,
          Tag customTag) {
        if (propertyValue == null) {
          return null;
        } else {
          return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }
      }
    };
    representer.addClassTag(TarantoolProperties.class, Tag.MAP);
    yaml = new Yaml(representer);
  }

  public static TarantoolProperties writeTestPropertiesYaml(@NonNull final String fileName) throws IOException {
    return writeTestPropertiesYaml(fileName, createRandomProperty());
  }

  public static TarantoolProperties writeTestPropertiesYaml(final String fileName, final TarantoolProperties properties)
      throws IOException {
    final File FILE = DEFAULT_TEST_PROPERTY_DIR.resolve(fileName).toFile().getAbsoluteFile();

    try (Writer writer = new FileWriter(FILE)) {
      firstInternalMap.put("tarantool", properties);
      secondInternalMap.put("data", firstInternalMap);
      externalMap.put("spring", secondInternalMap);

      yaml.dump(externalMap, writer);
      replaceNullToDefault(properties);
      return properties;
    }
  }

  public static void writeTestEmptyPropertiesYaml(@NonNull final String fileName) throws IOException {
    final File FILE = DEFAULT_TEST_PROPERTY_DIR.resolve(fileName).toFile().getAbsoluteFile();
    try (Writer writer = new FileWriter(FILE)) {
      yaml.dump(Collections.emptyMap(), writer);
    }
  }

  private static void replaceNullToDefault(final TarantoolProperties result) {

    if (result.getHost() == null) {
      result.setHost(DEFAULT_HOST);
    }

    if (result.getUserName() == null) {
      result.setUserName(DEFAULT_CRUD_USERNAME);
    }

    if (result.getBalancerMode() == null) {
      result.setBalancerMode(DEFAULT_BALANCER_MODE);
    }

    if (result.getPassword() == null) {
      result.setPassword(DEFAULT_CRUD_PASSWORD);
    }

    if (result.getConnectionGroups() != null) {
      for (PropertyInstanceConnectionGroup propertyInstanceConnectionGroup : result.getConnectionGroups()) {
        if (propertyInstanceConnectionGroup.getAuthType() == null) {
          propertyInstanceConnectionGroup.setAuthType(DEFAULT_AUTH_TYPE);
        }

        if (propertyInstanceConnectionGroup.getHost() == null) {
          propertyInstanceConnectionGroup.setHost(DEFAULT_HOST);
        }

        if (propertyInstanceConnectionGroup.getTag() == null) {
          propertyInstanceConnectionGroup.setTag(DEFAULT_TAG);
        }

        if (propertyInstanceConnectionGroup.getUserName() == null) {
          propertyInstanceConnectionGroup.setUserName(DEFAULT_CRUD_USERNAME);
        }

        if (propertyInstanceConnectionGroup.getUserName() == null) {
          propertyInstanceConnectionGroup.setPassword(DEFAULT_CRUD_PASSWORD);
        }
      }
    }
  }

  private static TarantoolProperties createRandomProperty() {
    final int intVar = 999;
    final long longVar = 9999L;
    final List<Boolean> booleanVar = Arrays.asList(true, false);

    PropertyHeartbeatOpts propertyHeartbeatOpts =
        heartbeat(choiceOrNull(longVar), choiceOrNull(intVar), choiceOrNull(intVar), choiceOrNull(intVar));

    PropertyFlushConsolidationHandler propertyFlushConsolidationHandler =
        choiceOrNull(flushConsolidation(choiceOrNull(intVar), choiceOrNull(booleanVar)));

    PropertyInstanceConnectionGroup propertyInstanceConnectionGroup =
        choiceOrNull(connectionGroup(choiceOrNull("host"),
            "password",
            choiceOrNull(intVar),
            choiceOrNull(intVar),
            choiceOrNull("tag"),
            "user",
            choiceOrNull(Arrays.asList(IProtoAuth.AuthType.values())),
            propertyFlushConsolidationHandler));
    List<PropertyInstanceConnectionGroup> propertyInstanceConnectionGroups = null;
    if (propertyInstanceConnectionGroup != null) {
      propertyInstanceConnectionGroups = new ArrayList<>();
      propertyInstanceConnectionGroups.add(propertyInstanceConnectionGroup);
    }

    return tarantoolProperties(choiceOrNull("host"),
        choiceOrNull("password"),
        choiceOrNull(intVar),
        choiceOrNull("user"),
        propertyInstanceConnectionGroups,
        choiceOrNull(intVar),
        choiceOrNull(booleanVar),
        choiceOrNull(propertyHeartbeatOpts),
        choiceOrNull(longVar),
        choiceOrNull(longVar),
        choiceOrNull(booleanVar),
        choiceOrNull(booleanVar),
        choiceOrNull(Arrays.asList(BalancerMode.values())));
  }

  @Nullable
  public static <T> T choiceOrNull(@Nullable T element) {
    int rnd = random.nextInt(2);
    if (rnd == 0 || element == null) {
      return null;
    }
    return element;
  }

  public static PropertyHeartbeatOpts heartbeat(@Nullable Long pingInterval,
      @Nullable Integer invalidationThreshold,
      @Nullable Integer windowSize,
      @Nullable Integer deathThreshold) {

    PropertyHeartbeatOpts propertyHeartbeatOpts = new PropertyHeartbeatOpts();
    if (invalidationThreshold != null) {
      propertyHeartbeatOpts.setInvalidationThreshold(invalidationThreshold);
    }
    if (pingInterval != null) {
      propertyHeartbeatOpts.setPingInterval(pingInterval);
    }
    if (deathThreshold != null) {
      propertyHeartbeatOpts.setDeathThreshold(deathThreshold);
    }
    if (windowSize != null) {
      propertyHeartbeatOpts.setWindowSize(windowSize);
    }

    return propertyHeartbeatOpts;
  }

  public static PropertyFlushConsolidationHandler flushConsolidation(
      @Nullable Integer explicitFlushAfterFlushes, @Nullable Boolean consolidateWhenNoReadInProgress) {

    PropertyFlushConsolidationHandler propertyFlushConsolidationHandler = new PropertyFlushConsolidationHandler();
    if (explicitFlushAfterFlushes != null) {
      propertyFlushConsolidationHandler.setExplicitFlushAfterFlushes(explicitFlushAfterFlushes);
    }

    if (consolidateWhenNoReadInProgress != null) {
      propertyFlushConsolidationHandler.setConsolidateWhenNoReadInProgress(consolidateWhenNoReadInProgress);
    }
    return propertyFlushConsolidationHandler;
  }

  @Nullable
  public static <T> T choiceOrNull(@Nullable List<T> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    int rnd = random.nextInt(list.size() + 1);

    if (rnd == list.size()) {
      return null;
    }
    return list.get(rnd);
  }

  @NonNull
  public static PropertyInstanceConnectionGroup connectionGroup(@Nullable String host,
      @Nullable String password,
      @Nullable Integer port,
      @Nullable Integer connectionNumber,
      @Nullable String tag,
      @Nullable String user,
      @Nullable IProtoAuth.AuthType type,
      @Nullable
      PropertyFlushConsolidationHandler propertyFlushConsolidationHandler) {

    PropertyInstanceConnectionGroup propertyInstanceConnectionGroup = new PropertyInstanceConnectionGroup();
    propertyInstanceConnectionGroup.setHost(host);
    if (connectionNumber != null) {
      propertyInstanceConnectionGroup.setConnectionGroupSize(connectionNumber);
    }
    propertyInstanceConnectionGroup.setAuthType(type);
    propertyInstanceConnectionGroup.setPassword(password);
    if (port != null) {
      propertyInstanceConnectionGroup.setPort(port);
    }
    propertyInstanceConnectionGroup.setTag(tag);
    propertyInstanceConnectionGroup.setUserName(user);

    propertyInstanceConnectionGroup.setFlushConsolidationHandler(propertyFlushConsolidationHandler);
    return propertyInstanceConnectionGroup;
  }

  @NonNull
  public static TarantoolProperties tarantoolProperties(@Nullable String host,
      @Nullable String password,
      @Nullable Integer port,
      @Nullable String user,
      @Nullable
      List<PropertyInstanceConnectionGroup> propertyInstanceConnectionGroups,
      @Nullable Integer eventLoopThreadsCount,
      @Nullable Boolean enableGracefulShutdown,
      @Nullable PropertyHeartbeatOpts propertyHeartbeatOptsOpts,
      @Nullable Long connectTimeout,
      @Nullable Long reconnectAfter,
      @Nullable Boolean fetchSchema,
      @Nullable Boolean ignoreOldSchemaVersion,
      @Nullable BalancerMode balancerClass) {

    TarantoolProperties prop = new TarantoolProperties();
    prop.setHost(host);
    prop.setPassword(password);
    if (port != null) {
      prop.setPort(port);
    }
    prop.setUserName(user);
    prop.setConnectionGroups(propertyInstanceConnectionGroups);
    if (eventLoopThreadsCount != null) {
      prop.setEventLoopThreadsCount(eventLoopThreadsCount);
    }

    if (enableGracefulShutdown != null) {
      prop.setGracefulShutdownEnabled(enableGracefulShutdown);
    }
    prop.setHeartbeat(propertyHeartbeatOptsOpts);
    if (connectTimeout != null) {
      prop.setConnectTimeout(connectTimeout);
    }
    if (reconnectAfter != null) {
      prop.setReconnectAfter(reconnectAfter);
    }
    if (fetchSchema != null) {
      prop.setFetchSchema(fetchSchema);
    }
    if (ignoreOldSchemaVersion != null) {
      prop.setIgnoreOldSchemaVersion(ignoreOldSchemaVersion);
    }
    prop.setBalancerMode(balancerClass);
    return prop;
  }

  public static List<Person> generatePersons(int count) {
    List<Person> persons = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      persons.add(new Person(i, i % 3 == 0 ? null : i % 2 == 0, "User-" + i));
    }
    return persons;
  }

  public static List<Person> generatePersons(int count, Consumer<Person> actionAtPerson) {
    List<Person> persons = generatePersons(count);
    persons.forEach(actionAtPerson);
    return persons;
  }

  public static List<ComplexPerson> generateComplexPersons(int count) {
    List<ComplexPerson> persons = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      persons.add(new ComplexPerson(i, UUID.randomUUID(), i % 3 == 0 ? null : i % 2 == 0, "User-" + i));
    }
    return persons;
  }

  public static List<ComplexPerson> generateComplexPersons(int count, Consumer<ComplexPerson> actionAtPerson) {
    List<ComplexPerson> persons = generateComplexPersons(count);
    persons.forEach(actionAtPerson);
    return persons;
  }

  public static List<Person> generateAndInsertPersons(int count, TarantoolCrudClient client) {
    final List<Person> persons = generatePersons(count);
    final TarantoolCrudSpace space = client.space(PERSON_SPACE);

    List<Tuple<Person>> insertedPersons = space.insertMany(persons, Person.class).join().getRows();
    insertedPersons.sort(Comparator.comparing((p) -> p.get().getId()));

    assertEquals(persons, unwrapTuples(insertedPersons));

    return persons;
  }

  public static List<Person> generateAndInsertPersons(int count, TarantoolCrudClient client, Consumer<Person> action) {
    final List<Person> persons = generatePersons(count, action);
    final TarantoolCrudSpace space = client.space(PERSON_SPACE);

    List<Tuple<Person>> insertedPersons = space.insertMany(persons, Person.class).join().getRows();
    insertedPersons.sort(Comparator.comparing((p) -> p.get().getId()));

    assertEquals(persons, unwrapTuples(insertedPersons));

    return persons;
  }

  public static List<ComplexPerson> generateAndInsertComplexPersons(int count, TarantoolCrudClient client) {
    final List<ComplexPerson> persons = generateComplexPersons(count);
    final TarantoolCrudSpace space = client.space(COMPLEX_PERSON_SPACE);

    List<Tuple<ComplexPerson>> insertedPersons = space.insertMany(persons, ComplexPerson.class).join().getRows();
    insertedPersons.sort(Comparator.comparing((p) -> p.get().getId()));

    assertEquals(persons, unwrapTuples(insertedPersons));

    return persons;
  }

  public static List<ComplexPerson> generateAndInsertComplexPersons(int count, TarantoolCrudClient client,
      Consumer<ComplexPerson> action) {

    final List<ComplexPerson> persons = generateComplexPersons(count, action);
    final TarantoolCrudSpace space = client.space(COMPLEX_PERSON_SPACE);

    List<Tuple<ComplexPerson>> insertedPersons = space.insertMany(persons, ComplexPerson.class).join().getRows();
    insertedPersons.sort(Comparator.comparing((p) -> p.get().getId()));

    assertEquals(persons, unwrapTuples(insertedPersons));

    return persons;
  }
}
