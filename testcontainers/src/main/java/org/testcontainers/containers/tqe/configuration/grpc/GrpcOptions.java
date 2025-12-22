/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tqe.configuration.grpc;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * type GrpcOptions struct {
 * 	InitialConnWindowSize int32   `mapstructure:"initial_conn_window_size"`
 * 	InitialWindowSize     int32   `mapstructure:"initial_window_size"`
 * 	HeaderTableSize       *uint32 `mapstructure:"header_table_size"`
 * 	MaxHeaderListSize     *uint32 `mapstructure:"max_header_list_size"`
 * 	MaxConcurrentStreams  uint32  `mapstructure:"max_concurrent_streams"`
 * 	NumStreamWorkers      uint32  `mapstructure:"num_stream_workers"`
 * 	MaxRecvMsgSize        int     `mapstructure:"max_recv_msg_size"`
 * 	MaxSendMsgSize        int     `mapstructure:"max_send_msg_size"`
 * 	ReadBufferSize        int     `mapstructure:"read_buffer_size"`
 * 	WriteBufferSize       int     `mapstructure:"write_buffer_size"`
 * 	SharedWriteBuffer     bool    `mapstructure:"shared_write_buffer"`
 * 	ReflectionEnabled     bool    `mapstructure:"reflection_enabled"`
 * }
 */
public class GrpcOptions {

  @JsonProperty("initial_conn_window_size")
  private final Integer initialConnWindowSize;

  @JsonProperty("initial_window_size")
  private final Integer initialWindowSize;

  @JsonProperty("header_table_size")
  private final Long headerTableSize;

  @JsonProperty("max_header_list_size")
  private final Long maxHeaderListSize;

  @JsonProperty("max_concurrent_streams")
  private final Long maxConcurrentStreams;

  @JsonProperty("num_stream_workers")
  private final Long numStreamWorkers;

  @JsonProperty("max_recv_msg_size")
  private final Integer maxRecvMsgSize;

  @JsonProperty("max_send_msg_size")
  private final Integer maxSendMsgSize;

  @JsonProperty("read_buffer_size")
  private final Integer readBufferSize;

  @JsonProperty("write_buffer_size")
  private final Integer writeBufferSize;

  @JsonProperty("shared_write_buffer")
  private final Boolean sharedWriteBuffer;

  @JsonProperty("reflection_enabled")
  private final Boolean reflectionEnabled;

  @JsonCreator
  public GrpcOptions(
      @JsonProperty("initial_conn_window_size") Integer initialConnWindowSize,
      @JsonProperty("initial_window_size") Integer initialWindowSize,
      @JsonProperty("header_table_size") Long headerTableSize,
      @JsonProperty("max_header_list_size") Long maxHeaderListSize,
      @JsonProperty("max_concurrent_streams") Long maxConcurrentStreams,
      @JsonProperty("num_stream_workers") Long numStreamWorkers,
      @JsonProperty("max_recv_msg_size") Integer maxRecvMsgSize,
      @JsonProperty("max_send_msg_size") Integer maxSendMsgSize,
      @JsonProperty("read_buffer_size") Integer readBufferSize,
      @JsonProperty("write_buffer_size") Integer writeBufferSize,
      @JsonProperty("shared_write_buffer") Boolean sharedWriteBuffer,
      @JsonProperty("reflection_enabled") Boolean reflectionEnabled) {
    this.initialConnWindowSize = initialConnWindowSize;
    this.initialWindowSize = initialWindowSize;
    this.headerTableSize = headerTableSize;
    this.maxHeaderListSize = maxHeaderListSize;
    this.maxConcurrentStreams = maxConcurrentStreams;
    this.numStreamWorkers = numStreamWorkers;
    this.maxRecvMsgSize = maxRecvMsgSize;
    this.maxSendMsgSize = maxSendMsgSize;
    this.readBufferSize = readBufferSize;
    this.writeBufferSize = writeBufferSize;
    this.sharedWriteBuffer = sharedWriteBuffer;
    this.reflectionEnabled = reflectionEnabled;
  }

  public Optional<Integer> getInitialConnWindowSize() {
    return Optional.ofNullable(this.initialConnWindowSize);
  }

  public Optional<Integer> getInitialWindowSize() {
    return Optional.ofNullable(this.initialWindowSize);
  }

  public Optional<Long> getHeaderTableSize() {
    return Optional.ofNullable(this.headerTableSize);
  }

  public Optional<Long> getMaxHeaderListSize() {
    return Optional.ofNullable(this.maxHeaderListSize);
  }

  public Optional<Long> getMaxConcurrentStreams() {
    return Optional.ofNullable(this.maxConcurrentStreams);
  }

  public Optional<Long> getNumStreamWorkers() {
    return Optional.ofNullable(this.numStreamWorkers);
  }

  public Optional<Integer> getMaxRecvMsgSize() {
    return Optional.ofNullable(this.maxRecvMsgSize);
  }

  public Optional<Integer> getMaxSendMsgSize() {
    return Optional.ofNullable(this.maxSendMsgSize);
  }

  public Optional<Integer> getReadBufferSize() {
    return Optional.ofNullable(this.readBufferSize);
  }

  public Optional<Integer> getWriteBufferSize() {
    return Optional.ofNullable(this.writeBufferSize);
  }

  public Optional<Boolean> getSharedWriteBuffer() {
    return Optional.ofNullable(this.sharedWriteBuffer);
  }

  public Optional<Boolean> getReflectionEnabled() {
    return Optional.ofNullable(this.reflectionEnabled);
  }
}
