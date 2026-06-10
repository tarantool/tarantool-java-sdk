/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import org.instancio.Instancio;
import org.instancio.Select;
import org.instancio.generators.Generators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.utils.pojo.User;

class TQEClusterIntegrationTest {

  private static final String QUEUE_NAME = "test";
  private static final int USERS_COUNT = 100;
  private static final int RETRY_TIMEOUT_SECONDS = 60;

  @ParameterizedTest
  @EnumSource(TQEVersion.class)
  @DisplayName("publish → subscribe round-trip across TQE versions")
  void testPublishAndConsumeData(TQEVersion version) {
    Assertions.assertDoesNotThrow(
        () -> {
          try (TQEClusterFixture fx = new TQEClusterFixture(version)) {
            ManagedChannel publisherChannel = fx.createPublisherChannel();
            ManagedChannel consumerChannel = fx.createConsumerChannel();
            try {
              final List<User> users = generateUsers();

              Unreliables.retryUntilSuccess(
                  RETRY_TIMEOUT_SECONDS,
                  TimeUnit.SECONDS,
                  () -> {
                    version.client().publish(publisherChannel, users, QUEUE_NAME);
                    return true;
                  });

              final Set<User> result = new CopyOnWriteArraySet<>();
              Unreliables.retryUntilSuccess(
                  RETRY_TIMEOUT_SECONDS,
                  TimeUnit.SECONDS,
                  () -> {
                    version.client().subscribe(consumerChannel, QUEUE_NAME, result::add);
                    return true;
                  });

              Unreliables.retryUntilTrue(
                  RETRY_TIMEOUT_SECONDS, TimeUnit.SECONDS, () -> users.size() == result.size());
              Assertions.assertEquals(new LinkedHashSet<>(users), result);
            } finally {
              consumerChannel.shutdownNow();
              publisherChannel.shutdownNow();
            }
          }
        });
  }

  private static List<User> generateUsers() {
    return Instancio.ofList(User.class)
        .size(USERS_COUNT)
        .generate(
            Select.field(User::getName), g -> g.string().alphaNumeric().allowEmpty().nullable())
        .generate(Select.field(User::getAge), Generators::ints)
        .create();
  }
}
