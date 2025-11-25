/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data;

import static io.tarantool.spring.data.Helper.assertNotNull;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.mapping.Tuple;

public class ProxyTarantoolCrudKeyValueAdapter {

  public static final String POTENTIAL_PERFORMANCE_ISSUES_EXCEPTION_MESSAGE =
      "Not supported due to potential performance issues when working with large datasets. Please,"
          + " use the method implementation with pagination.";

  private final TarantoolCrudClient client;

  public ProxyTarantoolCrudKeyValueAdapter(TarantoolCrudClient client) {
    this.client = client;
  }

  public Object put(Object id, Object item, String keyspace) {
    assertNotNull(id, "Id must not be 'null' for adding.");
    assertNotNull(item, "Item must not be 'null' for adding.");

    return client.space(keyspace).replace(item).join();
  }

  public boolean contains(Object id, String keyspace) {
    // TODO: reimplement this method after
    // https://github.com/tarantool/crud/issues/352.  This ticket suggests
    // using index position and it allows to use crud.count instead of
    // crud.get operation.  Get operation is not appropriate due to
    // unpredictable size of record returned by crud.get
    return client.space(keyspace).get(id).join() != null;
  }

  public Object get(Object id, String keyspace) {
    return client.space(keyspace).get(id).join();
  }

  public <T> T get(Object id, String keyspace, Class<T> type) {
    return unwrapTuple(client.space(keyspace).get(id, type).join());
  }

  public static <T> T unwrapTuple(Tuple<T> tuple) {
    if (tuple != null) {
      return tuple.get();
    }
    return null;
  }

  public Object delete(Object id, String keyspace) {
    return client.space(keyspace).delete(id).join();
  }

  public <T> T delete(Object id, String keyspace, Class<T> type) {
    return unwrapTuple(client.space(keyspace).delete(id, type).join());
  }

  public Iterable<?> getAllOf(String keyspace) {
    throw new UnsupportedOperationException(POTENTIAL_PERFORMANCE_ISSUES_EXCEPTION_MESSAGE);
  }

  public void deleteAllOf(String keyspace) {
    client.space(keyspace).truncate().join();
  }

  public void clear() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public void destroy() throws Exception {
    client.close();
  }

  public long count(String keyspace) {
    return client.space(keyspace).count().join();
  }
}
