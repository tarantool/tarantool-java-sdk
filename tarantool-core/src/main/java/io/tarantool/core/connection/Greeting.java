/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import io.tarantool.core.connection.exceptions.BadGreetingException;

public class Greeting {

  private static final int LINE_LENGTH = 64;
  private static final int GREETING_LENGTH = 128;
  private static final String BINARY_PROTOCOL = "binary";

  private final String version;
  private final String protocolType;
  private final UUID instanceUUID;
  private final byte[] salt;

  protected Greeting(String version,
      String protocolType,
      UUID instanceUUID,
      byte[] salt) {
    this.version = version;
    this.protocolType = protocolType;
    this.instanceUUID = instanceUUID;
    this.salt = salt;
  }

  public String getVersion() {
    return this.version;
  }

  public String getProtocolType() {
    return this.protocolType;
  }

  public UUID getInstanceUUID() {
    return this.instanceUUID;
  }

  public byte[] getSalt() {
    return this.salt;
  }

  public static Greeting parse(byte[] greeting) {
    String firstLine;
    byte[] salt;
    try {
      firstLine = new String(greeting, 0, LINE_LENGTH, StandardCharsets.UTF_8);
      salt = Arrays.copyOfRange(greeting, LINE_LENGTH, GREETING_LENGTH);
    } catch (IndexOutOfBoundsException e) {
      throw new BadGreetingException("bad greeting!");
    }

    String[] chunks = firstLine.toLowerCase().trim().split(" ");
    if (!chunks[0].equals("tarantool")) {
      throw new BadGreetingException("bad greeting start: " + chunks[0]);
    }
    if (!chunks[2].equals("(binary)")) {
      throw new BadGreetingException("bad protocol type: " + chunks[2]);
    }
    UUID instanceUUID;
    try {
      instanceUUID = UUID.fromString(chunks[3]);
    } catch (IllegalArgumentException e) {
      throw new BadGreetingException("bad instance uuid!");
    }
    return new Greeting(
        chunks[1],
        BINARY_PROTOCOL,
        instanceUUID,
        Base64.getDecoder().decode(new String(salt).trim())
    );
  }
}
