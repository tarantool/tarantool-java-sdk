/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.integration.tqe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

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
            final List<User> users = generateUsers();

            // Subscribe before publishing so the consumer stream is active when messages are
            // produced — avoids relying on cursor="" replay, which is unreliable while the
            // TQE 3.x broker is still settling.
            final Set<User> result = new CopyOnWriteArraySet<>();
            Unreliables.retryUntilSuccess(
                RETRY_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
                () -> {
                  fx.consumerClient().subscribe(QUEUE_NAME, result::add);
                  return true;
                });

            Unreliables.retryUntilSuccess(
                RETRY_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
                () -> {
                  fx.publisherClient().publish(users, QUEUE_NAME);
                  return true;
                });

            Unreliables.retryUntilTrue(
                RETRY_TIMEOUT_SECONDS, TimeUnit.SECONDS, () -> users.size() == result.size());
            Assertions.assertEquals(new LinkedHashSet<>(users), result);
          }
        });
  }

  // Queue config sets deduplication_mode: keep_latest, so duplicate (age, name) payloads collapse
  // and the consumer can never receive USERS_COUNT messages. Generate strictly-unique users.
  private static List<User> generateUsers() {
    List<User> users = new ArrayList<>(USERS_COUNT);
    for (int i = 0; i < USERS_COUNT; i++) {
      users.add(new User(i, "user-" + i));
    }
    return users;
  }
}
