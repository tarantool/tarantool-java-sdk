/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.integration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EVENT_DATA;
import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.exceptions.BoxError;

@Timeout(value = 5)
@Testcontainers
public class IProtoClientWatchersTest extends BaseTest {

  @Container
  private static final TarantoolContainer tarantoolContainer = new TarantoolContainer()
      .withEnv(ENV_MAP)
      .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(IProtoClientWatchersTest.class)));

  private IProtoClient getClientAndConnect(TarantoolContainer tt) throws Exception {
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    IProtoClient client = new IProtoClientImpl(factory, factory.getTimerService());
    client.connect(address, 3_000).get(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    return client;
  }

  @Test
  public void testWatchAndUnwatch() throws Exception {
    testWatchAndUnwatchOnContainer(tarantoolContainer, System.getenv("TARANTOOL_VERSION"));
  }

  @Test
  @Timeout(value = 10)
  public void testWatcherRecoveryAfterReconnect() throws Exception {
    testWatcherRecoveryAfterReconnectOnContainer(tarantoolContainer, System.getenv("TARANTOOL_VERSION"));
  }

  private void testWatchAndUnwatchOnContainer(TarantoolContainer tt, String version) throws Exception {
    IProtoClient client = getClientAndConnect(tt);

    List<Value> eventsKey1 = new ArrayList<>();
    List<Value> eventsKey2 = new ArrayList<>();

    client.watch("key1", (v) -> eventsKey1.add(v.getBodyValue(IPROTO_EVENT_DATA)));
    client.watch("key2", (v) -> eventsKey2.add(v.getBodyValue(IPROTO_EVENT_DATA)));

    tt.executeCommand("box.broadcast('key1', 'myEvent');" +
        "box.broadcast('key2', {1, 2, 3});" +
        "box.broadcast('key3', 'wontbecaught');");
    Thread.sleep(100);

    client.unwatch("key1");
    client.unwatch("key2");
    tt.executeCommand("box.broadcast('key1', 'myEvent');" +
        "box.broadcast('key2', {1, 2, 3});" +
        "box.broadcast('key3', 'wontbecaught');");
    Thread.sleep(100);

    assertEquals(Arrays.asList(ValueFactory.newString("myEvent")), eventsKey1);
    assertEquals(
        Arrays.asList(
            ValueFactory.newArray(
                ValueFactory.newInteger(1),
                ValueFactory.newInteger(2),
                ValueFactory.newInteger(3)
            )
        ),
        eventsKey2
    );

    client.close();
    checkTTVersion(tt, version);
  }

  @Test
  public void testWatchOnce() throws Exception {
    testWatchOnceOnContainer(tarantoolContainer, System.getenv("TARANTOOL_VERSION"));
  }

  private void testWatchOnceOnContainer(TarantoolContainer tt, String version) throws Exception {
    IProtoClient client = getClientAndConnect(tt);
    Integer serverVersion = client.getServerProtocolVersion();
    if (serverVersion < 6) {
      CompletionException ex = assertThrows(CompletionException.class, () -> client.watchOnce("key1").join());
      Throwable cause = ex.getCause();
      assertTrue(cause instanceof BoxError);
      assertTrue(cause.getMessage().contains("Unknown request type 77"));
      return;
    }

    assertEquals(0, client.watchOnce("k1").join().getBodyValue(IPROTO_DATA).asArrayValue().size());
    assertEquals(0, client.watchOnce("k2").join().getBodyValue(IPROTO_DATA).asArrayValue().size());

    tt.executeCommand("box.broadcast('k1', 'myEvent');" +
        "box.broadcast('k2', {1, 2, 3});");
    Thread.sleep(100);

    assertEquals(
        ValueFactory.newArray(ValueFactory.newString("myEvent")),
        client.watchOnce("k1").join().getBodyValue(IPROTO_DATA).asArrayValue()
    );
    assertEquals(
        ValueFactory.newArray(
            ValueFactory.newArray(
                ValueFactory.newInteger(1),
                ValueFactory.newInteger(2),
                ValueFactory.newInteger(3)
            )
        ),
        client.watchOnce("k2").join().getBodyValue(IPROTO_DATA).asArrayValue()
    );

    client.close();
    checkTTVersion(tt, version);
  }

  private void testWatcherRecoveryAfterReconnectOnContainer(TarantoolContainer tt, String version) throws Exception {
    IProtoClient client = getClientAndConnect(tt);
    List<Value> eventsKey1 = new ArrayList<>();
    List<Value> eventsKey2 = new ArrayList<>();

    client.watch("keyA", (v) -> eventsKey1.add(v.getBodyValue(IPROTO_EVENT_DATA)));
    client.watch("keyB", (v) -> eventsKey2.add(v.getBodyValue(IPROTO_EVENT_DATA)));

    tt.executeCommand("box.broadcast('keyA', 'myEvent');" +
        "box.broadcast('keyB', {1, 2, 3});" +
        "box.broadcast('keyC', 'wontbecaught');");
    Thread.sleep(100);
    client.close();
    Thread.sleep(100);
    InetSocketAddress address = new InetSocketAddress(tt.getHost(), tt.getPort());
    client.connect(address, 3_000).get(); // todo https://github.com/tarantool/tarantool-java-ee/issues/412
    Thread.sleep(1000);

    tt.executeCommand("box.broadcast('keyA', 'myEvent2');" +
        "box.broadcast('keyB', {1, 2, 3, 4});" +
        "box.broadcast('keyC', 'wontbecaught2');");
    Thread.sleep(100);

    // when client side subscribes by watcher for some key and this key was
    // fired before then client side will receive response immediately thus
    // it can lead to duplication of events
    assertEquals(
        Arrays.asList(
            ValueFactory.newString("myEvent"),
            ValueFactory.newString("myEvent"),
            ValueFactory.newString("myEvent2")
        ),
        eventsKey1
    );
    assertEquals(
        Arrays.asList(
            ValueFactory.newArray(
                ValueFactory.newInteger(1),
                ValueFactory.newInteger(2),
                ValueFactory.newInteger(3)
            ),
            ValueFactory.newArray(
                ValueFactory.newInteger(1),
                ValueFactory.newInteger(2),
                ValueFactory.newInteger(3)
            ),
            ValueFactory.newArray(
                ValueFactory.newInteger(1),
                ValueFactory.newInteger(2),
                ValueFactory.newInteger(3),
                ValueFactory.newInteger(4)
            )
        ),
        eventsKey2
    );

    client.close();
    checkTTVersion(tt, version);
  }

  private void checkTTVersion(TarantoolContainer tt, String version) throws Exception {
    List<?> result = tt.executeCommandDecoded("return _TARANTOOL");
    String ttVersion = (String) result.get(0);
    assertTrue(ttVersion.startsWith(version.split("-")[0]));
  }
}
