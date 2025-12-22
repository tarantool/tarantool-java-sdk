/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.factory;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

import io.tarantool.core.exceptions.ClientException;

/**
 * Class implements factory to create Tarantool clients.
 *
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @author <a href="https://github.com/artdu">Artyom Dubinin</a>
 */
public class TarantoolFactory {

  /**
   * Micrometer registry that hold set of collections of metrics.
   *
   * <p>See for details: <a
   * href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
   */
  private static MeterRegistry metricsRegistry =
      new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);

  /** Shortcut for @{code metricsRegistry.get("request.counter").counter()} */
  private static Counter requestCounter;

  /** Shortcut for @{code metricsRegistry.get("response.success").counter()} */
  private static Counter responseSuccess;

  /** Shortcut for @{code metricsRegistry.get("response.errors").counter()} */
  private static Counter responseErrors;

  /** Shortcut for @{code metricsRegistry.get("response.ignored").counter()} */
  private static Counter ignoredResponsesCounter;

  /** Flag used to check whether metrics have been registered or not. */
  private static boolean metricsRegistered;

  /** Creates {@link TarantoolFactory} object. */
  private TarantoolFactory() {}

  /**
   * Set global metrics registrator. Can be used only once before first factory usage.
   *
   * <p>See for details: <a
   * href="https://micrometer.io/docs/concepts#_registry">micrometer.io/docs/concepts#_registry</a>
   *
   * @param meterRegistry micrometer metrics registrator
   */
  public static void setMeterRegistry(MeterRegistry meterRegistry) {
    if (metricsRegistered) {
      throw new ClientException(
          "Metrics has already been initialized, set meterRegistry before first factory usage");
    }
    TarantoolFactory.metricsRegistry = meterRegistry;
  }

  /** Register metrics if needed. */
  private static void initMetricsIfNeeded() {
    if (metricsRegistry != null && !metricsRegistered) {
      LongTaskTimer.builder("request.timer")
          .description("Latency of requests to Tarantool")
          .register(metricsRegistry);
      requestCounter =
          Counter.builder("request.counter")
              .description("Number of requests to Tarantool")
              .register(metricsRegistry);
      responseSuccess =
          Counter.builder("response.success")
              .description("Number of successful responses")
              .register(metricsRegistry);
      responseErrors =
          Counter.builder("response.errors")
              .description("Number of error responses")
              .register(metricsRegistry);
      ignoredResponsesCounter =
          Counter.builder("response.ignored")
              .description("Number of ignored IProto packets")
              .register(metricsRegistry);
      metricsRegistered = true;
    }
  }

  /**
   * Creates builder for {@link TarantoolBoxClientImpl} class.
   *
   * @return {@link TarantoolBoxClientBuilder} object.
   */
  public static TarantoolBoxClientBuilder box() {
    initMetricsIfNeeded();
    return TarantoolBoxClientImpl.builder().withMeterRegistry(metricsRegistry);
  }

  /**
   * Creates builder for {@link TarantoolCrudClientImpl} class.
   *
   * @return {@link TarantoolCrudClientBuilder} object.
   */
  public static TarantoolCrudClientBuilder crud() {
    initMetricsIfNeeded();
    return TarantoolCrudClientImpl.builder().withMeterRegistry(metricsRegistry);
  }

  /**
   * Creates builder for {@link TarantoolDataGridClientImpl} class.
   *
   * <p><b>WARNING: This is a beta version of the client. Some features may not work correctly or
   * may change in future releases.</b>
   *
   * @return {@link TarantoolDataGridClientBuilder} object.
   */
  public static TarantoolDataGridClientBuilder tdg() {
    initMetricsIfNeeded();
    return TarantoolDataGridClientImpl.builder().withMeterRegistry(metricsRegistry);
  }

  /**
   * Gets total request amount between all clients.
   *
   * @return the request amount
   */
  public static double getRequestAmount() {
    initMetricsIfNeeded();
    if (metricsRegistry == null) {
      throw new ClientException("No metrics exist");
    }
    return requestCounter.count();
  }

  /**
   * Gets response success amount between all clients.
   *
   * @return the response success amount
   */
  public static double getResponseSuccessAmount() {
    initMetricsIfNeeded();
    if (metricsRegistry == null) {
      throw new ClientException("No metrics exist");
    }
    return responseSuccess.count();
  }

  /**
   * Gets response error amount between all clients.
   *
   * @return the response error amount
   */
  public static double getResponseErrorAmount() {
    initMetricsIfNeeded();
    if (metricsRegistry == null) {
      throw new ClientException("No metrics exist");
    }
    return responseErrors.count();
  }

  /**
   * Gets ignored responses amount among all clients.
   *
   * @return an ignored responses amount
   */
  public static double getIgnoredResponsesAmount() {
    initMetricsIfNeeded();
    if (metricsRegistry == null) {
      throw new ClientException("No metrics exist");
    }
    return ignoredResponsesCounter.count();
  }
}
