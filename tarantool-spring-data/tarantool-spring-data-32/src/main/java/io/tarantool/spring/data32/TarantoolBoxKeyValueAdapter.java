/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data32;

import java.util.Map;

import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.NonNull;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.spring.data.ProxyTarantoolBoxKeyValueAdapter;

public class TarantoolBoxKeyValueAdapter extends AbstractKeyValueAdapter {

  private final ProxyTarantoolBoxKeyValueAdapter adapter;

  public TarantoolBoxKeyValueAdapter(@NonNull TarantoolBoxClient tarantoolBoxClient) {
    adapter = new ProxyTarantoolBoxKeyValueAdapter(tarantoolBoxClient);
  }

  @Override
  public Object put(Object id, Object item, String keyspace) {
    return adapter.put(id, item, keyspace);
  }

  @Override
  public boolean contains(Object id, String keyspace) {
    return adapter.contains(id, keyspace);
  }

  @Override
  public Object get(Object id, String keyspace) {
    return adapter.get(id, keyspace);
  }

  @Override
  public Object delete(Object id, String keyspace) {
    return adapter.delete(id, keyspace);
  }

  @Override
  public Iterable<?> getAllOf(String keyspace) {
    return adapter.getAllOf(keyspace);
  }

  @Override
  public void deleteAllOf(String keyspace) {
    adapter.deleteAllOf(keyspace);
  }

  @Override
  public void clear() {
    adapter.clear();
  }

  @Override
  public long count(String keyspace) {
    return adapter.count(keyspace);
  }

  @Override
  public void destroy() throws Exception {
    adapter.destroy();
  }

  @Override
  public CloseableIterator<Map.Entry<Object, Object>> entries(String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
