/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.credentials.Credentials;
import io.tarantool.autogen.credentials.users.Users;
import io.tarantool.autogen.credentials.users.usersProperty.UsersProperty;

class ConfigurationUtilsTest {

  public static Stream<Arguments> dataForTestAddUsersMethod() {

    /*
    /**********************************************************
    /* Additional user properties
    /**********************************************************
    */
    final String userPwd = "user-pwd";
    final String userName = "user-name";
    final UsersProperty usersProperty = UsersProperty.builder().withPassword(userPwd).build();
    Map<String, UsersProperty> users =
        new HashMap<>() {
          {
            put(userName, usersProperty);
          }
        };

    /*
    /**********************************************************
    /* Without credentials section
    /**********************************************************
    */
    final Tarantool3Configuration oldConfigWithoutCredSection = new Tarantool3Configuration();
    final Tarantool3Configuration expectedConfigWithoutCredSection =
        Tarantool3Configuration.builder()
            .withCredentials(
                Credentials.builder()
                    .withUsers(
                        Users.builder().withAdditionalProperty(userName, usersProperty).build())
                    .build())
            .build();

    /*
    /**********************************************************
    /* With credentials section without users
    /**********************************************************
    */
    final Tarantool3Configuration oldConfigWithCredWithoutUsersSection =
        Tarantool3Configuration.builder().withCredentials(new Credentials()).build();
    final Tarantool3Configuration expectedConfigWithCredWithoutUsersSection =
        Tarantool3Configuration.builder()
            .withCredentials(
                Credentials.builder()
                    .withUsers(
                        Users.builder().withAdditionalProperty(userName, usersProperty).build())
                    .build())
            .build();

    /*
    /**********************************************************
    /* With credentials and users sections
    /**********************************************************
    */
    final Tarantool3Configuration oldConfigWithCredWithUsersSection =
        Tarantool3Configuration.builder()
            .withCredentials(Credentials.builder().withUsers(new Users()).build())
            .build();
    final Tarantool3Configuration expectedConfigWithCredWithUsersSection =
        Tarantool3Configuration.builder()
            .withCredentials(
                Credentials.builder()
                    .withUsers(
                        Users.builder().withAdditionalProperty(userName, usersProperty).build())
                    .build())
            .build();

    /*
    /**********************************************************
    /* Override users with same name
    /**********************************************************
    */
    final Tarantool3Configuration oldConfigWithUser =
        Tarantool3Configuration.builder()
            .withCredentials(
                Credentials.builder()
                    .withUsers(
                        Users.builder()
                            .withAdditionalProperty(userName, new UsersProperty())
                            .build())
                    .build())
            .build();
    final Tarantool3Configuration expectedConfigWithUser =
        Tarantool3Configuration.builder()
            .withCredentials(
                Credentials.builder()
                    .withUsers(
                        Users.builder().withAdditionalProperty(userName, usersProperty).build())
                    .build())
            .build();
    Assertions.assertNotEquals(oldConfigWithUser, expectedConfigWithUser);

    return Stream.of(
        // old config null
        Arguments.of(null, Collections.emptyMap(), null),

        // without credentials
        Arguments.of(oldConfigWithoutCredSection, users, expectedConfigWithoutCredSection),

        // with credentials and without users
        Arguments.of(
            oldConfigWithCredWithoutUsersSection, users, expectedConfigWithCredWithoutUsersSection),

        // with credentials and with users
        Arguments.of(
            oldConfigWithCredWithUsersSection, users, expectedConfigWithCredWithUsersSection),

        // override user with same name
        Arguments.of(oldConfigWithUser, users, expectedConfigWithUser));
  }

  @ParameterizedTest
  @MethodSource("dataForTestAddUsersMethod")
  void testAddUsersMethod(
      Tarantool3Configuration oldConfig,
      Map<String, UsersProperty> additionalUsers,
      Tarantool3Configuration expected) {
    Assertions.assertEquals(expected, ConfigurationUtils.addUsers(oldConfig, additionalUsers));
  }
}
