/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data;

import static io.tarantool.spring.data.Helper.assertNotNull;
import io.tarantool.client.box.TarantoolBoxClient;

public class ProxyTarantoolBoxKeyValueAdapter {

  private final TarantoolBoxClient tarantoolBoxClient;

  public ProxyTarantoolBoxKeyValueAdapter(TarantoolBoxClient tarantoolBoxClient) {
    assertNotNull(tarantoolBoxClient, "tarantoolBoxClient must be not null");
    this.tarantoolBoxClient = tarantoolBoxClient;
  }

  public Object put(Object id, Object item, String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public boolean contains(Object id, String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Object get(Object id, String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Object delete(Object id, String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Iterable<?> getAllOf(String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public void deleteAllOf(String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public void clear() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public long count(String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public void destroy() throws Exception {
    tarantoolBoxClient.close();
  }
}
