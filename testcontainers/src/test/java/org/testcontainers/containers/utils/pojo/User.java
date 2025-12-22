/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils.pojo;


import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// ignore need to parse tdg http response with extra field
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

  private int age;

  private String name;

  @JsonCreator
  public User(@JsonProperty("age") int age, @JsonProperty("name") String name) {
    this.age = age;
    this.name = name;
  }

  public int getAge() {
    return this.age;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return age == user.age && Objects.equals(name, user.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(age, name);
  }
}
