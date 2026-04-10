/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data40;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.util.CloseableIterator;

import io.tarantool.client.box.TarantoolBoxClient;
import io.tarantool.spring.data.ProxyTarantoolBoxKeyValueAdapter;

public class TarantoolBoxKeyValueAdapter extends AbstractKeyValueAdapter {

  private final ProxyTarantoolBoxKeyValueAdapter adapter;

  public TarantoolBoxKeyValueAdapter(@NonNull TarantoolBoxClient tarantoolBoxClient) {
    adapter = new ProxyTarantoolBoxKeyValueAdapter(tarantoolBoxClient);
  }

  @Override
  public Object put(@NonNull Object id, @NonNull Object item, @NonNull String keyspace) {
    return adapter.put(id, item, keyspace);
  }

  @Override
  public boolean contains(@NonNull Object id, @NonNull String keyspace) {
    return adapter.contains(id, keyspace);
  }

  @Override
  public Object get(@NonNull Object id, @NonNull String keyspace) {
    return adapter.get(id, keyspace);
  }

  @Override
  public Object delete(@NonNull Object id, @NonNull String keyspace) {
    return adapter.delete(id, keyspace);
  }

  @Override
  @NonNull
  public Iterable<Object> getAllOf(@NonNull String keyspace) {
    return adapter.getAllOf(keyspace);
  }

  @Override
  public void deleteAllOf(@NonNull String keyspace) {
    adapter.deleteAllOf(keyspace);
  }

  @Override
  public void clear() {
    adapter.clear();
  }

  @Override
  public long count(@NonNull String keyspace) {
    return adapter.count(keyspace);
  }

  @Override
  public void destroy() throws Exception {
    adapter.destroy();
  }

  @Override
  @NonNull
  public CloseableIterator<Map.Entry<Object, Object>> entries(@NonNull String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
