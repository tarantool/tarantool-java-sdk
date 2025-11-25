/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data27.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * A chunk of data restricted by the configured {@link Pageable} for Tarantool.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
abstract class TarantoolChunk<T> implements Slice<T>, Serializable {

  private static final long serialVersionUID = 867755909294344406L;

  private final List<T> content = new ArrayList<>();

  private final Pageable pageable;

  /**
   * Creates a new {@link TarantoolChunk} with the given content and the given governing {@link
   * TarantoolPageable}.
   *
   * @param content must not be {@literal null}.
   * @param pageable must not be {@literal null}.
   */
  public TarantoolChunk(List<T> content, Pageable pageable) {

    Assert.notNull(content, "Content must not be null");
    Assert.notNull(pageable, "Pageable must not be null");

    if (pageable.isPaged() && !(pageable instanceof TarantoolPageable)) {
      throw new IllegalArgumentException("Pageable must be TarantoolPageable<T> or Unpaged type");
    }

    this.content.addAll(content);
    this.pageable = pageable;
  }

  @Override
  public boolean isFirst() {
    return !hasPrevious();
  }

  @Override
  public boolean isLast() {
    return !hasNext();
  }

  @Override
  public boolean hasPrevious() {
    return getNumber() > 0 && hasContent();
  }

  @Override
  public boolean hasContent() {
    return !content.isEmpty();
  }

  /**
   * Returns the {@link Pageable} to request the next {@link Slice}. Can be {@link
   * Pageable#unpaged()} in case the current {@link Slice} is already the last one or when data
   * content is empty. Clients should check {@link #hasNext()} and {@link #hasContent()} before
   * calling this method.
   *
   * @see #nextOrLastPageable()
   */
  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public Pageable nextPageable() {
    if (hasNext() && pageable.isPaged()) {
      return ((TarantoolPageable<T>) pageable).next(content.get(content.size() - 1));
    }
    return Pageable.unpaged();
  }

  /**
   * Returns the {@link Pageable} to request the previous {@link Slice}. Can be {@link
   * Pageable#unpaged()} in case the current {@link Slice} is already the first one or data content
   * is empty. Clients should check {@link #hasPrevious()} and {@link #hasContent()} before calling
   * this method.
   *
   * @see #previousOrFirstPageable()
   */
  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public Pageable previousPageable() {
    if (hasPrevious() && pageable.isPaged()) {
      return ((TarantoolPageable<T>) pageable).previousOrFirst(content.get(0));
    }
    return Pageable.unpaged();
  }

  @Override
  public Iterator<T> iterator() {
    return content.iterator();
  }

  @NonNull
  @Override
  public List<T> getContent() {
    return Collections.unmodifiableList(content);
  }

  @NonNull
  @Override
  public Pageable getPageable() {
    return pageable;
  }

  @NonNull
  @Override
  public Sort getSort() {
    return pageable.getSort();
  }

  @Override
  public int getNumber() {
    if (pageable.isPaged()) {
      return pageable.getPageNumber();
    }
    return 0;
  }

  @Override
  public int getSize() {
    if (pageable.isPaged()) {
      return pageable.getPageSize();
    }
    return content.size();
  }

  @Override
  public int getNumberOfElements() {
    return content.size();
  }

  /**
   * Applies the given {@link Function} to the content of the {@link TarantoolChunk}.
   *
   * @param converter must not be {@literal null}.
   */
  protected <U> List<U> getConvertedContent(Function<? super T, ? extends U> converter) {

    Assert.notNull(converter, "Function must not be null");

    return this.stream().map(converter).collect(Collectors.toList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TarantoolChunk)) {
      return false;
    }
    TarantoolChunk<?> that = (TarantoolChunk<?>) o;
    return content.equals(that.content) && pageable.equals(that.pageable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, pageable);
  }
}
