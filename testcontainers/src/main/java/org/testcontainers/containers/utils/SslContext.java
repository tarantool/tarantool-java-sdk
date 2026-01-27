/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import lombok.Getter;

@Getter
public class SslContext {
  private String keyFile;
  private String certFile;

  private SslContext() {}

  private SslContext(String keyFile, String certFile) {
    this.keyFile = keyFile;
    this.certFile = certFile;
  }

  public static SslContext getSslContext() {
    return new SslContext();
  }

  public static SslContext getSslContext(String keyFile, String certFile) {
    return new SslContext(keyFile, certFile);
  }
}
