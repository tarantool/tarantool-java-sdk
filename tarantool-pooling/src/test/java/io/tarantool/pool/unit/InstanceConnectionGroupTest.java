/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.pool.unit;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import io.tarantool.core.protocol.requests.IProtoAuth;
import io.tarantool.pool.InstanceConnectionGroup;

class InstanceConnectionGroupTest {

  @Test
  void defaultParameterTest() {
    String defaultHost = "localhost";
    int defaultPort = 3301;

    final InstanceConnectionGroup group = InstanceConnectionGroup.builder().build();

    assertEquals(new InetSocketAddress(defaultHost, defaultPort), group.getAddress());
    assertEquals("guest:localhost:3301", group.getTag());
    assertEquals(defaultHost, group.getHost());
    assertEquals(defaultPort, group.getPort());
    assertEquals(1, group.getSize());
    assertEquals(IProtoAuth.AuthType.CHAP_SHA1, group.getAuthType());
    assertNull(group.getPassword());
    assertNull(group.getUser());
  }

  @Test
  void withoutPasswordTest() {
    String defaultHost = "localhost";
    int defaultPort = 3301;
    String tag = "h";
    String user = "guest";
    int size = 2;
    IProtoAuth.AuthType type = IProtoAuth.AuthType.PAP_SHA256;

    final InstanceConnectionGroup group = InstanceConnectionGroup.builder()
        .withTag(tag)
        .withUser(user)
        .withPort(defaultPort)
        .withSize(size)
        .withAuthType(type)
        .build();

    assertEquals(new InetSocketAddress(defaultHost, defaultPort), group.getAddress());
    assertEquals(tag, group.getTag());
    assertEquals(defaultHost, group.getHost());
    assertEquals(defaultPort, group.getPort());
    assertEquals(size, group.getSize());
    assertEquals(IProtoAuth.AuthType.PAP_SHA256, group.getAuthType());
    assertNull(group.getUser());
    assertNull(group.getPassword());
  }

  @Test
  void defaultParameterWithDefaultGuestUserTest() {
    String defaultHost = "localhost";
    int defaultPort = 3301;
    String defaultUser = "guest";

    final InstanceConnectionGroup group = InstanceConnectionGroup.builder().withUser(defaultUser).build();

    assertEquals(new InetSocketAddress(defaultHost, defaultPort), group.getAddress());
    assertEquals("guest:localhost:3301", group.getTag());
    assertEquals(defaultHost, group.getHost());
    assertEquals(defaultPort, group.getPort());
    assertEquals(1, group.getSize());
    assertEquals(IProtoAuth.AuthType.CHAP_SHA1, group.getAuthType());
    assertNull(group.getPassword());
    assertNull(group.getUser());
  }

  @Test
  void nullEmptyParametersTest() {
    String emptyHost = "";
    int defaultPort = 3301;
    String emptyUser = "";
    String emptyPassword = null;

    final InstanceConnectionGroup group = InstanceConnectionGroup.builder()
        .withUser(emptyUser)
        .withHost(emptyHost)
        .withPassword(emptyPassword)
        .build();

    assertEquals(new InetSocketAddress("localhost", defaultPort), group.getAddress());
    assertEquals("guest:localhost:3301", group.getTag());
    assertEquals("localhost", group.getHost());
    assertEquals(defaultPort, group.getPort());
    assertEquals(1, group.getSize());
    assertEquals(IProtoAuth.AuthType.CHAP_SHA1, group.getAuthType());
    assertNull(group.getPassword());
    assertNull(group.getUser());
  }
}