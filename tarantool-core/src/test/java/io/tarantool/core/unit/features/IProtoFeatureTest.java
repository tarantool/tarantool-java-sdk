/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.unit.features;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import io.tarantool.core.IProtoFeature;

public class IProtoFeatureTest {

  @Test
  void testValueOf() {

    final IProtoFeature[] features = IProtoFeature.values();
    for (IProtoFeature feature : features) {
      assertEquals(feature, IProtoFeature.valueOf(feature.ordinal()));
    }

    assertThrows(
        IllegalArgumentException.class,
        () -> IProtoFeature.valueOf(ThreadLocalRandom.current().nextInt(20_000, 30_000)));
  }
}
