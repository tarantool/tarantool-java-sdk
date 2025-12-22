/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.integration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.ValueFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.bouncycastle.util.Strings;

import io.tarantool.balancer.TarantoolDistributingRoundRobinBalancer;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.ManagedResource;
import io.tarantool.core.connection.ConnectionFactory;
import io.tarantool.mapping.Field;
import io.tarantool.pool.IProtoClientPoolImpl;
import io.tarantool.pool.InstanceConnectionGroup;
import io.tarantool.schema.Index;
import io.tarantool.schema.Space;
import io.tarantool.schema.TarantoolSchemaFetcher;

@Timeout(value = 5)
@Testcontainers
public class TarantoolSchemaFetcherTest {

  private static final String API_USER = "api_user";

  private static final Map<String, String> CREDS =
      new HashMap<String, String>() {
        {
          put(API_USER, "secret");
        }
      };

  private static final Map<String, String> CREDS_MAP =
      new HashMap<String, String>() {
        {
          put("TARANTOOL_USER_NAME", API_USER);
          put("TARANTOOL_USER_PASSWORD", CREDS.get(API_USER));
        }
      };

  private static final Bootstrap bootstrap =
      new Bootstrap()
          .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
          .channel(NioSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
  private static final Timer timerService = new HashedWheelTimer();
  private static final ConnectionFactory factory = new ConnectionFactory(bootstrap, timerService);

  @Container
  private static final TarantoolContainer tt = new TarantoolContainer().withEnv(CREDS_MAP);

  private Long spacePersonId;
  private static IProtoClient client;

  @BeforeEach
  public void truncateSpaces() throws Exception {
    List<?> result = tt.executeCommandDecoded("return box.space.person.id");
    this.spacePersonId = Long.valueOf((Integer) result.get(0));

    tt.executeCommand("return box.space.person:truncate()");
  }

  @BeforeAll
  public static void setUp() {
    client = new IProtoClientImpl(factory, timerService);
    client
        .connect(new InetSocketAddress(tt.getHost(), tt.getPort()), 3_000)
        .join(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    client.authorize(API_USER, CREDS.get(API_USER)).join();
  }

  @Test
  public void testSchemaVersionBehaviour() {
    TarantoolSchemaFetcher fetcher = getTarantoolSchemaFetcher();

    Long initialSchemaVersion = client.ping().join().getSchemaVersion();

    assertEquals(spacePersonId, fetcher.getSpace("person").getId());
    assertEquals(initialSchemaVersion, fetcher.getSchemaVersion());

    fetcher.processRequest(client.ping()).join();

    assertEquals(initialSchemaVersion, fetcher.getSchemaVersion());
    client
        .eval("box.schema.space.create('space_from_java_code')", ValueFactory.emptyArray())
        .join();
    assertEquals(initialSchemaVersion, fetcher.getSchemaVersion());

    fetcher.processRequest(client.ping()).join();
    assertEquals(initialSchemaVersion + 1, fetcher.getSchemaVersion());
  }

  @Test
  public void testSpacesIndexes() throws Exception {
    Map<?, ?> realIndexFromEval =
        (Map<?, ?>) ((List<?>) tt.executeCommandDecoded("return box.space.person.index")).get(0);
    assertEquals(new HashSet<>(Arrays.asList(0, "pk")), realIndexFromEval.keySet());
    Map<?, ?> pk = (Map<?, ?>) realIndexFromEval.get("pk");

    TarantoolSchemaFetcher fetcher = getTarantoolSchemaFetcher();
    Map<String, Index> indexes = fetcher.getSpace("person").getIndexes();
    assertEquals(1, indexes.size());
    Index pkFromFetcher = indexes.get("pk");

    assertEquals(pk.get("id"), pkFromFetcher.getIndexId());
    assertEquals(pk.get("name"), pkFromFetcher.getName());
    assertEquals("pk", pkFromFetcher.getName());
    assertEquals(Strings.toLowerCase((String) pk.get("type")), pkFromFetcher.getType());
  }

  @Test
  void testGetSpaceById() {
    final TarantoolSchemaFetcher fetcher = getTarantoolSchemaFetcher();
    final Space spaceByName = fetcher.getSpace("person");
    final int spaceId = spaceByName.getId();
    final Space spaceById = fetcher.getSpace(spaceId);
    assertEquals(spaceByName, spaceById);
  }

  private static TarantoolSchemaFetcher getTarantoolSchemaFetcher() {
    ManagedResource<Timer> timerResource = ManagedResource.external(timerService);
    IProtoClientPoolImpl pool = new IProtoClientPoolImpl(factory, timerResource);
    pool.setGroups(
        Collections.singletonList(
            InstanceConnectionGroup.builder()
                .withHost(tt.getHost())
                .withPort(tt.getPort())
                .withTag("a")
                .build()));

    TarantoolDistributingRoundRobinBalancer balancer =
        new TarantoolDistributingRoundRobinBalancer(pool);
    balancer.getNext().join().authorize(API_USER, CREDS.get(API_USER)).join();
    return new TarantoolSchemaFetcher(balancer, false);
  }

  @Test
  public void testSpacesChange() {
    TarantoolSchemaFetcher fetcher = getTarantoolSchemaFetcher();
    fetcher.processRequest(client.ping()).join();

    List<Field> initialFormat =
        Arrays.asList(
            new Field().setName("id").setType("number"),
            new Field().setName("is_married").setType("boolean").setNullable(true),
            new Field().setName("name").setType("string"));
    assertEquals(initialFormat, fetcher.getSpace("person").getFormat());

    client
        .eval(
            "box.space.person:format({"
                + "{ 'id', type = 'number' }, "
                + "{ 'is_married', type = 'boolean', is_nullable = true}, "
                + "{ 'name', type = 'string' }, "
                + "{ 'new_field', type = 'string', is_nullable = true }"
                + "})",
            ValueFactory.emptyArray())
        .join();
    assertEquals(initialFormat, fetcher.getSpace("person").getFormat());

    fetcher.processRequest(client.ping()).join();
    List<Field> changedFormat = new ArrayList<>(initialFormat);
    changedFormat.add(new Field().setName("new_field").setType("string").setNullable(true));
    assertEquals(changedFormat, fetcher.getSpace("person").getFormat());
  }

  @Test
  public void testIndexesChange() throws Exception {
    TarantoolSchemaFetcher fetcher = getTarantoolSchemaFetcher();
    fetcher.processRequest(client.ping()).join();

    Map<?, ?> realIndexFromEval =
        (Map<?, ?>) ((List<?>) tt.executeCommandDecoded("return box.space.person.index")).get(0);
    assertEquals(new HashSet<>(Arrays.asList(0, "pk")), realIndexFromEval.keySet());

    assertEquals(1, fetcher.getSpace("person").getIndexes().size());

    client
        .eval(
            "box.space.person:create_index('name_index', { parts = { 'name' } })",
            ValueFactory.emptyArray())
        .join();

    realIndexFromEval =
        (Map<?, ?>) ((List<?>) tt.executeCommandDecoded("return box.space.person.index")).get(0);
    assertEquals(
        new HashSet<>(Arrays.asList(0, 1, "pk", "name_index")), realIndexFromEval.keySet());

    assertEquals(1, fetcher.getSpace("person").getIndexes().size());

    fetcher.processRequest(client.ping()).join();

    Map<String, Index> indexes = fetcher.getSpace("person").getIndexes();
    assertEquals(2, indexes.size());

    Map<?, ?> nameIndex = (Map<?, ?>) realIndexFromEval.get("name_index");
    Index nameIndexFromFetcher = indexes.get("name_index");

    assertEquals(nameIndex.get("id"), nameIndexFromFetcher.getIndexId());
    assertEquals(nameIndex.get("name"), nameIndexFromFetcher.getName());
    assertEquals("name_index", nameIndexFromFetcher.getName());
    assertEquals(
        Strings.toLowerCase((String) nameIndex.get("type")), nameIndexFromFetcher.getType());

    client.eval("box.space.person.index['name_index']:drop()", ValueFactory.emptyArray()).join();
  }
}
