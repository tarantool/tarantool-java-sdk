/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VshardClusterContainerTest {

  private static final String dockerRegistry =
      System.getenv().getOrDefault("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX", "");

  protected static Stream<Arguments> dataForTestEqualsAndHashCode() {
    VshardClusterContainer firstEqualContainer =
        new VshardClusterContainer(
            "vshard_cluster/Dockerfile",
            dockerRegistry + "vshard-cluster-java",
            "vshard_cluster/instances.yaml",
            "vshard_cluster/config.yaml",
            "tarantool/tarantool");

    VshardClusterContainer secondNotEqualContainer =
        new VshardClusterContainer(
            "vshard_cluster/Dockerfile",
            dockerRegistry + "vshard-cluster-java",
            "vshard_cluster/instances.yaml",
            "vshard_cluster/config.yaml",
            "tarantool/tarantool");

    VshardClusterContainer notEqualContainer =
        new VshardClusterContainer(
                "vshard_cluster/Dockerfile",
                dockerRegistry + "vshard-cluster-java",
                "vshard_cluster/instances.yaml",
                "vshard_cluster/config.yaml",
                "tarantool/tarantool")
            .withRouterPort(3302);

    return Stream.of(
        Arguments.of(
            Arrays.asList(firstEqualContainer, firstEqualContainer), notEqualContainer, 1_000),
        Arguments.of(
            Arrays.asList(firstEqualContainer, firstEqualContainer),
            secondNotEqualContainer,
            1_000));
  }

  @ParameterizedTest
  @MethodSource("dataForTestEqualsAndHashCode")
  void testEqualsAndHashCode(
      List<VshardClusterContainer> equalContainers,
      VshardClusterContainer notEqualContainer,
      int iterationCount) {
    for (int i = 0; i < iterationCount; i++) {
      for (VshardClusterContainer container : equalContainers) {
        for (VshardClusterContainer otherContainer : equalContainers) {
          assertEquals(container, otherContainer);
          assertEquals(container.hashCode(), otherContainer.hashCode());
        }
        assertNotEquals(container, notEqualContainer);
        if (notEqualContainer != null) {
          assertNotEquals(container.hashCode(), notEqualContainer.hashCode());
        }
      }
    }
  }
}
