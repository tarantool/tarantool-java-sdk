/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config.tarantool3;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostPort {

  /*
  /**********************************************************
  /* Constants
  /**********************************************************
  */
  private static final Logger LOGGER = LoggerFactory.getLogger(HostPort.class);

  private static final String SCHEMA_GROUP = "schema";

  private static final String HOST_GROUP = "host";

  private static final String PORT_GROUP = "port";

  private static final Pattern PATTERN =
      Pattern.compile(
          String.format(
              "(?:(?<%s>[\\w.-]+)://)?(?<%s>[\\w.-]+):(?<%s>\\d+)$",
              SCHEMA_GROUP, HOST_GROUP, PORT_GROUP));

  /*
  /**********************************************************
  /* Fields
  /**********************************************************
  */
  private final String raw;

  private final String host;

  private final String schema;

  private final int port;

  public HostPort(String host, int port, String raw, String schema) {
    validate(raw);
    this.raw = raw;
    this.host = host;
    this.port = port;
    this.schema = schema;
    LOGGER.trace("Created HostPort: {}", this);
  }

  public HostPort(String host, int port) {
    this(host, port, host + ":" + port, null);
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getSchema() {
    return schema;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HostPort hostPort = (HostPort) o;
    return port == hostPort.port
        && Objects.equals(raw, hostPort.raw)
        && Objects.equals(host, hostPort.host)
        && Objects.equals(schema, hostPort.schema);
  }

  @Override
  public int hashCode() {
    return Objects.hash(raw, host, schema, port);
  }

  @Override
  public String toString() {
    return "HostPort{" + this.raw + "}";
  }

  private static Matcher validate(String address) {
    final Matcher matcher = PATTERN.matcher(address);
    if (!matcher.matches()) {
      LOGGER.error("Invalid 'host:port' address: '{}'. Pattern: '{}'", address, PATTERN);
      throw new IllegalArgumentException("Invalid 'host:port' address. See logs for details.");
    }
    return matcher;
  }

  public static HostPort parse(String address) {
    final Matcher matcher = validate(address);
    final String group = matcher.group(HOST_GROUP);
    final int port = Integer.parseUnsignedInt(matcher.group(PORT_GROUP));
    final String schema = matcher.group(SCHEMA_GROUP);
    return new HostPort(group, port, address, schema);
  }
}
