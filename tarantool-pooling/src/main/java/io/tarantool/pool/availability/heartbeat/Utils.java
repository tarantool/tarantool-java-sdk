/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.pool.availability.heartbeat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.util.Timer;

/**
 * The internal context of the policy: the timer and the metrics shared between all the connections.
 */
final class Utils {

  private final Timer timer;
  private final MeterRegistry metrics;

  /** Count of successful heartbeat ping requests. */
  private final Counter heartbeatSuccess;

  /** Count of failed heartbeat ping requests. */
  private final Counter heartbeatErrors;

  /** Time of ping request. */
  private final LongTaskTimer heartbeatTime;

  private Utils(
      Timer timer,
      MeterRegistry metrics,
      Counter heartbeatSuccess,
      Counter heartbeatErrors,
      LongTaskTimer heartbeatTime) {
    this.timer = timer;
    this.metrics = metrics;
    this.heartbeatSuccess = heartbeatSuccess;
    this.heartbeatErrors = heartbeatErrors;
    this.heartbeatTime = heartbeatTime;
  }

  static Utils configure(Timer timer, MeterRegistry metrics) {
    Counter heartbeatSuccess = null;
    Counter heartbeatErrors = null;
    LongTaskTimer heartbeatTime = null;
    if (metrics != null) {
      heartbeatSuccess =
          Counter.builder(HeartbeatConstants.POOL_HEARTBEAT_SUCCESS)
              .description("Count of successful heartbeat ping requests")
              .register(metrics);

      heartbeatErrors =
          Counter.builder(HeartbeatConstants.POOL_HEARTBEAT_ERRORS)
              .description("Count of failed heartbeat ping requests")
              .register(metrics);

      heartbeatTime =
          LongTaskTimer.builder(HeartbeatConstants.POOL_HEARTBEAT_TIME)
              .description("Time of ping request")
              .register(metrics);
    }

    return new Utils(timer, metrics, heartbeatSuccess, heartbeatErrors, heartbeatTime);
  }

  /**
   * Creates timer span for time measurement.
   *
   * @return instance of {@code LongTaskTimer.Sample}
   */
  LongTaskTimer.Sample beginHeartbeatTimeMetric() {
    if (this.metrics == null) {
      return null;
    }

    return this.heartbeatTime.start();
  }

  /**
   * If timer span is not null, stops time measurement.
   *
   * @param timer an instance of {@code LongTaskTimer.Sample}
   */
  void finishHeartbeatTimeMetric(LongTaskTimer.Sample timer) {
    if (timer == null) {
      return;
    }

    timer.stop();
  }

  /**
   * Increments heartbeat related metrics.
   *
   * @param fail if ping is failed then this value is 1, otherwise this value is 0.
   */
  void incHeartbeatCounters(int fail) {
    if (this.metrics == null) {
      return;
    }

    if (fail != 0) {
      heartbeatErrors.increment();
    } else {
      heartbeatSuccess.increment();
    }
  }

  Timer timer() {
    return this.timer;
  }
}
