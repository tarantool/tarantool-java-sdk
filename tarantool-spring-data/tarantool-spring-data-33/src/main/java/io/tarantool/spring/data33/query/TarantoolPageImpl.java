/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data33.query;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

/**
 * Basic {@code Page} implementation for Tarantool.
 *
 * @param <T> domain class type.
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
class TarantoolPageImpl<T> extends TarantoolChunk<T> implements Page<T> {

  @Serial
  private static final long serialVersionUID = 867755909294344406L;

  private final long total;

  /**
   * Creates a new {@link TarantoolPageImpl} with empty content. This will result in the created {@link Page} being
   * identical to the entire {@link List}.
   */
  public TarantoolPageImpl() {
    this(Collections.emptyList(), Pageable.unpaged(), 0L);
  }

  /**
   * Creates a new {@link TarantoolPageImpl} with the given content. This will result in the created {@link Page} being
   * identical to the entire {@link List}.
   *
   * @param content must not be {@literal null}.
   */
  public TarantoolPageImpl(List<T> content) {
    this(content, Pageable.unpaged(), content == null ? 0L : content.size());
  }

  /**
   * Constructor of {@link TarantoolPageImpl}.
   *
   * @param content  the content of this page, must not be {@literal null}.
   * @param pageable the paging information, must not be {@literal null}.
   * @param total    the total amount of items available. The total might be adapted considering the length of the
   *                 content given, if it is going to be the content of the last page. This is in place to mitigate
   *                 inconsistencies.
   */
  public TarantoolPageImpl(List<T> content, Pageable pageable, long total) {

    super(content, pageable);

    this.total = pageable.toOptional()
        .filter(it -> !content.isEmpty())
        .filter(it -> it.getOffset() + it.getPageSize() > total)
        .map(it -> it.getOffset() + content.size())
        .orElse(total);
  }

  @Override
  public int getTotalPages() {
    return getSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getSize());
  }

  @Override
  public long getTotalElements() {
    return total;
  }

  @Override
  public boolean hasNext() {
    return getNumber() + 1 < getTotalPages() && hasContent();
  }

  @Override
  public boolean isLast() {
    return !hasNext();
  }

  @Override
  @NonNull
  public <U> Page<U> map(@NonNull Function<? super T, ? extends U> converter) {
    return new PageImpl<>(getConvertedContent(converter), getPageable(), total);
  }

  @Override
  public String toString() {

    List<T> content = getContent();
    boolean canGetContentType = !content.isEmpty() && content.get(0) != null;

    String contentType = canGetContentType ? content.get(0).getClass().getName() : "UNKNOWN";

    return String.format("Page %s of %d containing %s instances", getNumber() + 1, getTotalPages(), contentType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TarantoolPageImpl<?> that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    return total == that.total;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), total);
  }
}
