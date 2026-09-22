/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
type TLSParams struct {
	Enabled      bool   `mapstructure:"enabled"`
	CertFile     string `mapstructure:"cert_file"`
	KeyFile      string `mapstructure:"key_file"`
	CaFile       string `mapstructure:"ca_file"`
	Password     string `mapstructure:"password"`
	PasswordFile string `mapstructure:"password_file"`
	Ciphers      string `mapstructure:"ciphers"`
}
 */
public class TLSParams {

  @JsonProperty("enabled")
  private final Boolean enabled;

  @JsonProperty("cert_file")
  private final String certFile;

  @JsonProperty("key_file")
  private final String keyFile;

  @JsonProperty("ca_file")
  private final String caFile;

  @JsonProperty("password")
  private final String password;

  @JsonProperty("password_file")
  private final String passwordFile;

  @JsonProperty("ciphers")
  private final String ciphers;

  @JsonCreator
  public TLSParams(
      @JsonProperty("enabled") Boolean enabled,
      @JsonProperty("cert_file") String certFile,
      @JsonProperty("key_file") String keyFile,
      @JsonProperty("ca_file") String caFile,
      @JsonProperty("password") String password,
      @JsonProperty("password_file") String passwordFile,
      @JsonProperty("ciphers") String ciphers) {
    this.enabled = enabled;
    this.certFile = certFile;
    this.keyFile = keyFile;
    this.caFile = caFile;
    this.password = password;
    this.passwordFile = passwordFile;
    this.ciphers = ciphers;
  }

  public Optional<Boolean> getEnabled() {
    return Optional.ofNullable(enabled);
  }

  public Optional<String> getCertFile() {
    return Optional.ofNullable(certFile);
  }

  public Optional<String> getKeyFile() {
    return Optional.ofNullable(keyFile);
  }

  public Optional<String> getCaFile() {
    return Optional.ofNullable(caFile);
  }

  public Optional<String> getPassword() {
    return Optional.ofNullable(password);
  }

  public Optional<String> getPasswordFile() {
    return Optional.ofNullable(passwordFile);
  }

  public Optional<String> getCiphers() {
    return Optional.ofNullable(ciphers);
  }
}
