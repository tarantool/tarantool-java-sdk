/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.net.InetSocketAddress;
import java.util.Objects;

public class HttpHost {

  private static final String HTTP_SCHEME = "http";

  private static final String HTTPS_SCHEME = HTTP_SCHEME + "s";

  private final Credentials credentials;

  private final InetSocketAddress address;

  private final String scheme;

  HttpHost(String hostname, int port, String scheme, Credentials credentials) {
    this.address = new InetSocketAddress(hostname, port);
    this.scheme = scheme;
    this.credentials = credentials;
  }

  public static HttpHost secure(String hostname, int port) {
    return secure(hostname, port, null);
  }

  public static HttpHost secure(String hostname, int port, Credentials credentials) {
    return new HttpHost(hostname, port, HTTPS_SCHEME, credentials);
  }

  public static HttpHost unsecure(String hostname, int port) {
    return unsecure(hostname, port, null);
  }

  public static HttpHost unsecure(String hostname, int port, Credentials credentials) {
    return new HttpHost(hostname, port, HTTP_SCHEME, credentials);
  }

  public static HttpHost secure(InetSocketAddress address) {
    return secure(address, null);
  }

  public static HttpHost secure(InetSocketAddress address, Credentials credentials) {
    return new HttpHost(address.getHostString(), address.getPort(), HTTPS_SCHEME, credentials);
  }

  public static HttpHost unsecure(InetSocketAddress address) {
    return unsecure(address, null);
  }

  public static HttpHost unsecure(InetSocketAddress address, Credentials credentials) {
    return new HttpHost(address.getHostString(), address.getPort(), HTTP_SCHEME, credentials);
  }

  public CharSequence string() {
    if (this.credentials == null) {
      return this.scheme + "://" + this.address.getHostString() + ":" + this.address.getPort();
    }
    return this.scheme
        + "://"
        + this.credentials.string()
        + "@"
        + this.address.getHostString()
        + ":"
        + this.address.getPort();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpHost httpHost = (HttpHost) o;
    return Objects.equals(credentials, httpHost.credentials)
        && Objects.equals(address, httpHost.address)
        && Objects.equals(scheme, httpHost.scheme);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentials, address, scheme);
  }

  @Override
  public String toString() {
    if (this.credentials == null) {
      return this.scheme + "://" + this.address.getHostString() + ":" + this.address.getPort();
    }
    return this.scheme
        + "://"
        + this.credentials
        + "@"
        + this.address.getHostString()
        + ":"
        + this.address.getPort();
  }

  public static class Credentials {

    private final CharSequence login;
    private final CharSequence password;

    Credentials(CharSequence login, CharSequence password) {
      this.login = login;
      this.password = password;
    }

    public static Credentials of(CharSequence login, CharSequence password) {
      return new Credentials(login, password);
    }

    public static Credentials login(CharSequence username) {
      return new Credentials(username, null);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Credentials that = (Credentials) o;
      return Objects.equals(login, that.login) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
      return Objects.hash(login, password);
    }

    public CharSequence string() {
      if (this.password == null) {
        return this.login;
      }
      return this.login + ":" + this.password;
    }

    @Override
    public String toString() {
      if (this.password == null) {
        return "******";
      }
      return "*****:*****";
    }
  }
}
