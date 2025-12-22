/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aggregates all configuration for grpc nodes of TQE with version < 3.x
 */

//TODO: избавиться от класса после того, как будет завершена задача<a href="https://jira.vk.team/browse/TNTP-5506">
// jira</a>
/*
type AppConfig struct {
	AppName     string       `mapstructure:"app_name"`
	AppVersion  string       `mapstructure:"app_version"`
	CoreHost    string       `mapstructure:"core_host"`
	CorePort    uint32       `mapstructure:"core_port"`
	GrpcOptions GrpcOptions  `mapstructure:"grpc_options"`
	GrpcListen  []GrpcListen `mapstructure:"grpc_listen"`
	GrpcHost    string       `mapstructure:"grpc_host"`
	GrpcPort    uint32       `mapstructure:"grpc_port"`
	ConfigPath  string       `mapstructure:"config_path"`
	EtcdAddr    string       `mapstructure:"etcd_addr"`
	Daemon      bool         `mapstructure:"daemon"`

	Publisher  PublisherConfig  `mapstructure:"publisher"`
	Consumer   ConsumerConfig   `mapstructure:"consumer"`
	MockServer MockServerConfig `mapstructure:"mock_server"`

	Tracing TracingConfig `mapstructure:"tracing"`
	Log     LoggerConfig  `mapstructure:"log"`
}

 */
public class GrpcConfiguration {

  @JsonProperty("app_name")
  private final String appName;

  @JsonProperty("app_version")
  private final String appVersion;

  @JsonProperty("core_host")
  private final String coreHost;

  @JsonProperty("core_port")
  private final Long corePort;

  @JsonProperty("grpc_options")
  private final GrpcOptions grpcOptions;

  @JsonProperty("grpc_listen")
  private final Set<GrpcListen> grpcListen;

  @JsonProperty("grpc_host")
  private final String grpcHost;

  @JsonProperty("grpc_port")
  private final Long grpcPort;

  @JsonProperty("config_path")
  private final String configPath;

  @JsonProperty("etcd_addr")
  private final String etcdAddr;

  @JsonProperty("daemon")
  private final Boolean daemon;

  @JsonProperty("publisher")
  private final PublisherConfig publisher;

  @JsonProperty("consumer")
  private final ConsumerConfig consumer;

  @JsonProperty("mock_server")
  private final MockServerConfig mockServer;

  @JsonProperty("tracing")
  private final TracingConfig tracing;

  @JsonProperty("log")
  private final LoggerConfig log;

  @JsonCreator
  public GrpcConfiguration(
      @JsonProperty("app_name") String appName,
      @JsonProperty("app_version") String appVersion,
      @JsonProperty("core_host") String coreHost,
      @JsonProperty("core_port") Long corePort,
      @JsonProperty("grpc_options") GrpcOptions grpcOptions,
      @JsonProperty("grpc_listen") Set<GrpcListen> grpcListen,
      @JsonProperty("grpc_host") String grpcHost,
      @JsonProperty("grpc_port") Long grpcPort,
      @JsonProperty("config_path") String configPath,
      @JsonProperty("etcd_addr") String etcdAddr,
      @JsonProperty("daemon") Boolean daemon,
      @JsonProperty("publisher") PublisherConfig publisher,
      @JsonProperty("consumer") ConsumerConfig consumer,
      @JsonProperty("mock_server") MockServerConfig mockServer,
      @JsonProperty("tracing") TracingConfig tracing,
      @JsonProperty("log") LoggerConfig log) {
    this.appName = appName;
    this.appVersion = appVersion;
    this.coreHost = coreHost;
    this.corePort = corePort;
    this.grpcOptions = grpcOptions;
    this.grpcListen = grpcListen;
    this.grpcHost = grpcHost;
    this.grpcPort = grpcPort;
    this.configPath = configPath;
    this.etcdAddr = etcdAddr;
    this.daemon = daemon;
    this.publisher = publisher;
    this.consumer = consumer;
    this.mockServer = mockServer;
    this.tracing = tracing;
    this.log = log;
  }

  public Optional<String> getAppName() {
    return Optional.ofNullable(this.appName);
  }

  public Optional<String> getAppVersion() {
    return Optional.ofNullable(this.appVersion);
  }

  public Optional<String> getCoreHost() {
    return Optional.ofNullable(this.coreHost);
  }

  public Optional<Long> getCorePort() {
    return Optional.ofNullable(this.corePort);
  }

  public Optional<GrpcOptions> getGrpcOptions() {
    return Optional.ofNullable(this.grpcOptions);
  }

  public Optional<Set<GrpcListen>> getGrpcListen() {
    return Optional.ofNullable(this.grpcListen);
  }

  public Optional<String> getGrpcHost() {
    return Optional.ofNullable(this.grpcHost);
  }

  public Optional<Long> getGrpcPort() {
    return Optional.ofNullable(this.grpcPort);
  }

  public Optional<String> getConfigPath() {
    return Optional.ofNullable(this.configPath);
  }

  public Optional<String> getEtcdAddr() {
    return Optional.ofNullable(this.etcdAddr);
  }

  public Optional<Boolean> getDaemon() {
    return Optional.ofNullable(this.daemon);
  }

  public Optional<PublisherConfig> getPublisher() {
    return Optional.ofNullable(this.publisher);
  }

  public Optional<ConsumerConfig> getConsumer() {
    return Optional.ofNullable(this.consumer);
  }

  public Optional<MockServerConfig> getMockServer() {
    return Optional.ofNullable(this.mockServer);
  }

  public Optional<TracingConfig> getTracing() {
    return Optional.ofNullable(this.tracing);
  }

  public Optional<LoggerConfig> getLog() {
    return Optional.ofNullable(this.log);
  }
}
