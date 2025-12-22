/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.tarantool.client.box.options.DeleteOptions;
import io.tarantool.client.box.options.SelectOptions;
import io.tarantool.client.box.options.UpdateOptions;
import io.tarantool.core.protocol.BoxIterator;

/**
 * @author Artyom Dubinin
 */
public class OptionsTest {

  @Test
  public void testDefaultIterator() {
    assertEquals(BoxIterator.EQ, SelectOptions.builder().build().getIterator());
  }

  @Test
  public void testDefaultLimit() {
    assertEquals(100, SelectOptions.builder().build().getLimit());
  }

  @Test
  public void testDefaultOffset() {
    assertEquals(0, SelectOptions.builder().build().getOffset());
  }

  @Test
  public void testDefaultIndexId() {
    assertEquals(0, SelectOptions.builder().build().getIndexId());
  }

  public static Stream<Arguments> dataForDefaultTimeout() {
    return Stream.of(
        Arguments.of(BaseOptions.builder().build().getTimeout()),
        Arguments.of(SelectOptions.builder().build().getTimeout()),
        Arguments.of(DeleteOptions.builder().build().getTimeout()),
        Arguments.of(UpdateOptions.builder().build().getTimeout())
    );
  }

  @ParameterizedTest
  @MethodSource("dataForDefaultTimeout")
  public void testDefaultTimeout(Long timeout) {
    assertEquals(5000, timeout);
  }

  public static Stream<Arguments> dataForDefaultStreamId() {
    return Stream.of(
        Arguments.of(BaseOptions.builder().build().getStreamId()),
        Arguments.of(SelectOptions.builder().build().getStreamId()),
        Arguments.of(DeleteOptions.builder().build().getStreamId()),
        Arguments.of(UpdateOptions.builder().build().getStreamId())
    );
  }

  @ParameterizedTest
  @MethodSource("dataForDefaultStreamId")
  public void testDefaultStreamId(Long streamId) {
    assertNull(streamId);
  }


  @Test
  void testDefaultCrudLimit() {
    assertEquals(io.tarantool.client.crud.options.SelectOptions.DEFAULT_LIMIT,
        io.tarantool.client.crud.options.SelectOptions.builder().build().getOptions().get("first"));
  }

  @Test
  void testAfterWithBefore() {

    assertDoesNotThrow(() -> io.tarantool.client.crud.options.SelectOptions.builder()
        .withFirst(-1)
        .build());

    assertDoesNotThrow(() -> io.tarantool.client.crud.options.SelectOptions.builder()
        .withFirst(1)
        .build());
  }
}

