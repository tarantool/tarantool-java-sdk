/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27;

import java.util.Collections;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.util.CloseableIterator;
import org.springframework.lang.NonNull;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.spring.data.ProxyTarantoolCrudKeyValueAdapter;
import io.tarantool.spring.data.mapping.model.CompositeKey;

public class TarantoolCrudKeyValueAdapter extends AbstractKeyValueAdapter {

  private final ProxyTarantoolCrudKeyValueAdapter adapter;

  public TarantoolCrudKeyValueAdapter(@NonNull TarantoolCrudClient client) {
    super(new TarantoolQueryEngine(client));
    this.adapter = new ProxyTarantoolCrudKeyValueAdapter(client);
  }

  @Override
  public Object put(Object id, Object item, String keyspace) {
    return adapter.put(convertId(id), item, keyspace);
  }

  @Override
  public boolean contains(Object id, String keyspace) {
    return adapter.contains(convertId(id), keyspace);
  }

  @Override
  public Object get(Object id, String keyspace) {
    return adapter.get(convertId(id), keyspace);
  }

  @Override
  public <T> T get(Object id, String keyspace, Class<T> type) {
    return adapter.get(convertId(id), keyspace, type);
  }

  @Override
  public Object delete(Object id, String keyspace) {
    return adapter.delete(convertId(id), keyspace);
  }

  @Override
  public <T> T delete(Object id, String keyspace, Class<T> type) {
    return adapter.delete(convertId(id), keyspace, type);
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
  public void destroy() throws Exception {
    adapter.destroy();
  }

  @Override
  public long count(String keyspace) {
    return adapter.count(keyspace);
  }

  @Override
  public CloseableIterator<Entry<Object, Object>> entries(String keyspace) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Convert the identifier to the form required by the tarantool-java-sdk driver.
   *
   * @param id identifier object
   * @return identifier in the required form
   */
  private Object convertId(Object id) {
    if (id instanceof CompositeKey || hasJsonFormatArrayAnnotation(id)) {
      return id;
    }
    return Collections.singletonList(id);
  }

  /**
   * Determine whether the identifier type is annotated with the {@link JsonFormat} annotation.
   *
   * @param id identifier object
   * @return return true if the annotation is present, false otherwise
   */
  private boolean hasJsonFormatArrayAnnotation(Object id) {
    final JsonFormat jsonFormatAnnotation =
        AnnotatedElementUtils.findMergedAnnotation(id.getClass(), JsonFormat.class);

    return jsonFormatAnnotation != null
        && JsonFormat.Shape.ARRAY.equals(jsonFormatAnnotation.shape());
  }
}
