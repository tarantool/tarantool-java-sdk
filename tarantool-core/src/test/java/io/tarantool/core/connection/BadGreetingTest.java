/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import io.tarantool.core.connection.exceptions.BadGreetingException;


/**
 * @author Ivan Dneprov
 */

public class BadGreetingTest {

  @Test
  public void test_parse_shouldThrowException_withEmptyGreeting() {
    // given
    byte[] greeting = new byte[0];
    // when
    Exception ex = assertThrows(BadGreetingException.class, () -> Greeting.parse(greeting));
    // then
    assertEquals("bad greeting!", ex.getMessage());
  }

  @Test
  public void test_parse_shouldThrowException_withRandomBytesGreeting() {
    // given
    byte[] greeting = new byte[5];
    // when
    Exception ex = assertThrows(BadGreetingException.class, () -> Greeting.parse(greeting));
    // then
    assertEquals("bad greeting!", ex.getMessage());
  }

  @Test
  public void test_parse_shouldThrowException_withBadGreetingStart() {
    // given
    byte[] greeting = "bad_greeting_start 1 (type) XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
        .getBytes(StandardCharsets.UTF_8);
    // when
    Exception ex = assertThrows(BadGreetingException.class, () -> Greeting.parse(greeting));
    // then
    assertEquals("bad greeting start: bad_greeting_start", ex.getMessage());
  }

  @Test
  public void test_parse_shouldThrowException_withBadGreetingType() {
    // given
    byte[] greeting = "tarantool 123456 (bad_type) XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
        .getBytes(StandardCharsets.UTF_8);
    // when
    Exception ex = assertThrows(BadGreetingException.class, () -> Greeting.parse(greeting));
    // then
    assertEquals("bad protocol type: (bad_type)", ex.getMessage());
  }

  @Test
  public void test_parse_shouldThrowException_withBadInstanceUUID() {
    // given
    byte[] greeting = "tarantool version1 (binary) XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
        .getBytes(StandardCharsets.UTF_8);
    // when
    Exception ex = assertThrows(BadGreetingException.class, () -> Greeting.parse(greeting));
    // then
    assertEquals("bad instance uuid!", ex.getMessage());
  }

  @Test
  public void test_parse_shouldReturnGreeting_withCorrectGreeting() {
    // given
    byte[] greeting = "tarantool version1 (binary) 11111111-1111-1111-1111-111111111111"
        .getBytes(StandardCharsets.UTF_8);
    // when
    Greeting parsedGreeting = Greeting.parse(greeting);
    // then
    assertEquals(parsedGreeting.getVersion(), "version1");
    assertEquals(parsedGreeting.getProtocolType(), "binary");
    assertEquals(parsedGreeting.getInstanceUUID(), UUID.fromString("11111111-1111-1111-1111-111111111111"));
    assertEquals(parsedGreeting.getSalt().length, 0);
  }
}
