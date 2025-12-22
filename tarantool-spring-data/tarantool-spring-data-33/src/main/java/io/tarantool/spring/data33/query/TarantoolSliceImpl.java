/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.query;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.lang.NonNull;

/**
 * Default implementation of {@link Slice} for Tarantool.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
class TarantoolSliceImpl<T> extends TarantoolChunk<T> {

  @Serial private static final long serialVersionUID = 867755909294344406L;

  private final boolean hasNext;

  private final Pageable pageable;

  /**
   * Creates a new {@link TarantoolSliceImpl} with the empty content. This will result in the
   * created {@link Slice} being identical to the entire {@link List}.
   */
  public TarantoolSliceImpl() {
    this(Collections.emptyList(), Pageable.unpaged(), false);
  }

  /**
   * Creates a new {@link TarantoolSliceImpl} with the given content. This will result in the
   * created {@link Slice} being identical to the entire {@link List}.
   *
   * @param content must not be {@literal null}.
   */
  public TarantoolSliceImpl(List<T> content) {
    this(content, Pageable.unpaged(), false);
  }

  /**
   * Creates a new {@link TarantoolSliceImpl} with the given content and {@link Pageable}.
   *
   * @param content the content of this {@link Slice}, must not be {@literal null}.
   * @param pageable the paging information, must not be {@literal null}.
   * @param hasNext whether there's another slice following the current one.
   */
  public TarantoolSliceImpl(List<T> content, Pageable pageable, boolean hasNext) {
    super(content, pageable);

    this.hasNext = hasContent() && hasNext;
    this.pageable = pageable;
  }

  /**
   * Returns true if there is a next data slice. No data content means that the next slice does not
   * exist and method will return false.
   *
   * @return Returns true if there is a next data slice. No data content means that the next slice
   *     does not exist and method will return false.
   */
  @Override
  public boolean hasNext() {
    return hasNext && hasContent();
  }

  @Override
  @NonNull
  public <U> Slice<U> map(@NonNull Function<? super T, ? extends U> converter) {
    return new TarantoolSliceImpl<>(getConvertedContent(converter), pageable, hasNext);
  }

  @Override
  public String toString() {

    String contentType = "UNKNOWN";
    List<T> content = getContent();

    if (content.size() > 0) {
      contentType = content.get(0).getClass().getName();
    }

    return String.format("Slice %d containing %s instances", getNumber(), contentType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TarantoolSliceImpl<?> that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    return hasNext == that.hasNext && pageable.equals(that.pageable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), hasNext, pageable);
  }
}
