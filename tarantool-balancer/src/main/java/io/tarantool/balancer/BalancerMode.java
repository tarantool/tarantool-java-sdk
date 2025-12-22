/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.balancer;


import static io.tarantool.balancer.TarantoolBalancer.DEFAULT_BALANCER_CLASS;

/**
 * Enumeration of balancer classes types that are used in the driver.
 */
public enum BalancerMode {

  /**
   * Represents a TarantoolDistributingRoundRobinBalancer class.
   */
  DISTRIBUTING_ROUND_ROBIN(TarantoolDistributingRoundRobinBalancer.class),

  /**
   * Represents a TarantoolRoundRobinBalancer class.
   */
  ROUND_ROBIN(TarantoolRoundRobinBalancer.class);

  private final Class<? extends TarantoolBalancer> balancerClass;
  public static final BalancerMode DEFAULT_BALANCER_MODE = BalancerMode.valueOf(DEFAULT_BALANCER_CLASS);

  BalancerMode(Class<? extends TarantoolBalancer> balancerClass) {
    this.balancerClass = balancerClass;
  }

  public static BalancerMode valueOf(Class<? extends TarantoolBalancer> balancerClass) {
    for (BalancerMode type : BalancerMode.values()) {
      if (balancerClass.equals(type.getBalancerClass())) {
        return type;
      }
    }
    throw new IllegalArgumentException(
        "The BalancerClass constant was not found for the specified class balancer.");
  }

  public Class<? extends TarantoolBalancer> getBalancerClass() {
    return balancerClass;
  }
}
