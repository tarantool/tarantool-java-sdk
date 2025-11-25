/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool;

import java.net.InetSocketAddress;

import io.netty.handler.flush.FlushConsolidationHandler;

import static io.tarantool.core.protocol.requests.IProtoAuth.DEFAULT_AUTH_TYPE;
import io.tarantool.core.protocol.requests.IProtoAuth.AuthType;

/**
 * Class that implements a group of connections to a host.
 *
 * @author <a href="https://github.com/bitgorbovsky">Ivan Bannikov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see Builder
 */
public final class InstanceConnectionGroup {

  /** Default hostname. */
  public static final String DEFAULT_HOST = "localhost";

  /** Default port. */
  public static final int DEFAULT_PORT = 3301;

  /** Default connection number in connection group. */
  public static final int DEFAULT_CONNECTION_NUMBER = 1;

  /**
   * Host name.
   *
   * <p><i><b>Default</b></i>: {@code "localhost"}.
   */
  private final String host;

  /**
   * Host port.
   *
   * <p><i><b>Default</b></i>: {@code 3301}.
   */
  private final int port;

  /**
   * Connection group size.
   *
   * <p><i><b>Default</b></i>: {@code 1}.
   */
  private final int size;

  /**
   * User name with which the connection is made.
   *
   * <p><i><b>Default</b></i>: {@code "guest"}.
   */
  private final String user;

  /**
   * Password for {@link #user}.
   *
   * <p><i><b>Default</b></i>: {@code no password (null)}.
   */
  private final String password;

  /**
   * Type of authentication in Tarantool.
   *
   * <p><i><b>Default</b></i>: {@link AuthType#CHAP_SHA1}.
   *
   * @see AuthType
   */
  private final AuthType authType;

  /**
   * IP socket address.
   *
   * @see InetSocketAddress
   */
  private final InetSocketAddress address;

  /** Default netty flush handler. */
  private final FlushConsolidationHandler flushConsolidationHandler;

  /**
   * Tag of group.
   *
   * <p><i><b>Default</b></i>: {@code "<user>:<host>:<port>"}.
   */
  private String tag;

  /**
   * Creates new {@link InstanceConnectionGroup} object with passed arguments.
   *
   * @param host {@link #host}
   * @param port {@link #port}
   * @param size {@link #size}
   * @param tag {@link #tag}
   * @param user {@link #user}
   * @param password {@link #password}
   * @param authType {@link #authType}
   * @param flushConsolidationHandler {@link #flushConsolidationHandler}
   * @throws IllegalArgumentException when {@code "guest"} password is not empty or password of
   *     other users is empty
   */
  private InstanceConnectionGroup(
      String host,
      int port,
      int size,
      String tag,
      String user,
      String password,
      AuthType authType,
      FlushConsolidationHandler flushConsolidationHandler)
      throws IllegalArgumentException {
    if (user != null && user.equals("guest")) {
      user = null;
    }
    if (user == null && password != null) {
      throw new IllegalArgumentException("password for guest should be empty");
    }
    this.port = port;
    this.user = user;
    this.authType = authType;
    this.size = size;
    this.host = host;
    this.password = password;
    this.tag = tag;
    this.address = new InetSocketAddress(host, port);
    this.flushConsolidationHandler = flushConsolidationHandler;
  }

  /**
   * Creates new {@link Builder} object.
   *
   * @return {@link Builder} object.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns value of host field.
   *
   * @return {@link #host} value.
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns value of port field.
   *
   * @return {@link #port} value.
   */
  public int getPort() {
    return port;
  }

  /**
   * Returns value of size field.
   *
   * @return {@link #size} value.
   */
  public int getSize() {
    return size;
  }

  /**
   * Returns value of tag field.
   *
   * @return {@link #tag} value.
   */
  public String getTag() {
    if (tag == null) {
      String newTag;

      if (user == null) {
        newTag = "guest:" + host + ":" + port;
        tag = newTag;
        return tag;
      }

      newTag = user + ":" + host + ":" + port;
      tag = newTag;
      return tag;
    }
    return tag;
  }

  /**
   * Returns value of user field.
   *
   * @return {@link #user} value.
   */
  public String getUser() {
    return user;
  }

  /**
   * Returns value of password field.
   *
   * @return {@link #password} value.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns value of authentication type field.
   *
   * @return {@link #authType} value.
   */
  public AuthType getAuthType() {
    return authType;
  }

  /**
   * Returns value of address field.
   *
   * @return {@link #address} value.
   */
  public InetSocketAddress getAddress() {
    return address;
  }

  /**
   * Gets flush consolidation handler.
   *
   * @return the flush consolidation handler
   */
  public FlushConsolidationHandler getFlushConsolidationHandler() {
    return flushConsolidationHandler;
  }

  /** Builder of {@link InstanceConnectionGroup} class. */
  public static class Builder {

    /**
     * @see InstanceConnectionGroup#host
     */
    private String host = DEFAULT_HOST;

    /**
     * @see InstanceConnectionGroup#port
     */
    private int port = DEFAULT_PORT;

    /**
     * @see InstanceConnectionGroup#size
     */
    private int size = DEFAULT_CONNECTION_NUMBER;

    /**
     * @see InstanceConnectionGroup#tag
     */
    private String tag;

    /**
     * @see InstanceConnectionGroup#user
     */
    private String user;

    /**
     * @see InstanceConnectionGroup#password
     */
    private String password;

    /**
     * @see InstanceConnectionGroup#authType
     */
    private AuthType authType = DEFAULT_AUTH_TYPE;

    /** Default netty flush handler. */
    private FlushConsolidationHandler flushConsolidationHandler;

    /** Constructor of {@link Builder}. */
    private Builder() {}

    /**
     * Sets value of {@link InstanceConnectionGroup#host}. Host should be not empty or null,
     * otherwise it will use default value.
     *
     * <p><i><b>Note</b></i>: Don't use this method if a default value is required:
     * <i>"localhost"</i>.
     *
     * @param host {@link InstanceConnectionGroup#host}.
     * @return {@link Builder} object.
     * @see InstanceConnectionGroup#host
     * @see InstanceConnectionGroup
     */
    public Builder withHost(String host) {
      if (host == null || host.trim().isEmpty()) {
        return this;
      }
      this.host = host;
      return this;
    }

    /**
     * Sets value of {@link InstanceConnectionGroup#port}. Port value should be from 0 to 65535.
     *
     * <p><i><b>Note</b></i>: Don't use this method if a default value is required: <i>3301</i>.
     *
     * @param port {@link InstanceConnectionGroup#port}.
     * @return {@link Builder} object.
     * @throws IllegalArgumentException when {@code port < 0 or port > 65535}.
     * @see InstanceConnectionGroup#port
     * @see InstanceConnectionGroup
     */
    public Builder withPort(int port) throws IllegalArgumentException {
      if (port < 0 || port > 65535) {
        throw new IllegalArgumentException("port value should be from 0 to 65535");
      }
      this.port = port;
      return this;
    }

    /**
     * Sets value of {@link InstanceConnectionGroup#size}. Size value should greater 0.
     *
     * <p><i><b>Note</b></i>: Don't use this method if a default value is required: <i>1</i>.
     *
     * @param size {@link InstanceConnectionGroup#size}.
     * @return {@link Builder} object.
     * @throws IllegalArgumentException when {@code size <= 0}.
     * @see InstanceConnectionGroup#size
     * @see InstanceConnectionGroup
     */
    public Builder withSize(int size) throws IllegalArgumentException {
      if (size <= 0) {
        throw new IllegalArgumentException("size should be greater 0");
      }
      this.size = size;
      return this;
    }

    /**
     * Sets value of {@link InstanceConnectionGroup#tag}. Tag value should be not null or empty,
     * otherwise will use default value.
     *
     * <p><i><b>Note</b></i>: Don't use this method if a default value is required: <i>{@code
     * <user>:<host>:<port>} </i>.
     *
     * @param tag {@link InstanceConnectionGroup#tag}.
     * @return {@link Builder} object.
     * @see InstanceConnectionGroup#tag
     * @see InstanceConnectionGroup
     */
    public Builder withTag(String tag) {
      if (tag == null || tag.trim().isEmpty()) {
        return this;
      }
      this.tag = tag;
      return this;
    }

    /**
     * Sets value of {@link InstanceConnectionGroup#user}. Username should be not null or empty,
     * otherwise will use default value.
     *
     * <p><i><b>Note</b></i>: Don't use this method if a default value is required: <i>"guest"</i>.
     *
     * @param user {@link InstanceConnectionGroup#user}.
     * @return {@link Builder} object.
     * @see InstanceConnectionGroup#user
     * @see InstanceConnectionGroup
     */
    public Builder withUser(String user) {
      if (user == null || user.trim().isEmpty()) {
        return this;
      }
      this.user = user;
      return this;
    }

    /**
     * Sets value of {@link InstanceConnectionGroup#password}.
     *
     * @param password {@link InstanceConnectionGroup#password}.
     * @return {@link Builder} object.
     * @see InstanceConnectionGroup#password
     * @see InstanceConnectionGroup
     */
    public Builder withPassword(String password) {
      if (password == null) {
        return this;
      }
      this.password = password;
      return this;
    }

    /**
     * Sets value of {@link InstanceConnectionGroup#authType}. Authentication type should be not
     * null, otherwise will use default value.
     *
     * <p><i><b>Note</b></i>: Don't use this method if a default value is required: {@link
     * AuthType#CHAP_SHA1}.
     *
     * @param authType {@link InstanceConnectionGroup#authType}.
     * @return {@link Builder} object.
     * @see InstanceConnectionGroup#authType
     * @see InstanceConnectionGroup
     */
    public Builder withAuthType(AuthType authType) {
      if (authType == null) {
        return this;
      }
      this.authType = authType;
      return this;
    }

    /**
     * Sets the {@link #flushConsolidationHandler} parameter when constructing an instance of a
     * builder class. The following example creates a {@link InstanceConnectionGroup} object with a
     * specified {@link #flushConsolidationHandler} parameter:
     *
     * <blockquote>
     *
     * <pre>{@code
     * InstanceConnectionGroup group = InstanceConnectionGroup
     *                                          .builder()
     *                                          .withFlushConsolidationHandler(
     *                                              new FlushConsolidationHandler(128, true)
     *                                          )
     *                                          .build();
     *
     * }</pre>
     *
     * </blockquote>
     *
     * @param flushConsolidationHandler see {@link FlushConsolidationHandler} option.
     * @return {@link Builder} object.
     */
    public Builder withFlushConsolidationHandler(
        FlushConsolidationHandler flushConsolidationHandler) {
      this.flushConsolidationHandler = flushConsolidationHandler;
      return this;
    }

    /**
     * Builds {@link InstanceConnectionGroup} object.
     *
     * @return {@link InstanceConnectionGroup} object.
     */
    public InstanceConnectionGroup build() {

      return new InstanceConnectionGroup(
          this.host,
          this.port,
          this.size,
          this.tag,
          this.user,
          this.password,
          this.authType,
          this.flushConsolidationHandler);
    }
  }
}
