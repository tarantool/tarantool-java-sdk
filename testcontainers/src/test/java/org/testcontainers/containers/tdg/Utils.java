/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tdg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.testcontainers.containers.utils.pojo.User;
import org.testcontainers.containers.utils.HttpHost;

public abstract class Utils {

  private Utils() {}

  public static List<User> sendUsers(List<User> users, TDGContainer<?> container) throws IOException {
    final List<User> result = new ArrayList<>();
    final ObjectMapper objectMapper = new ObjectMapper();
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      for (final User user : users) {
        final String jsonUser = objectMapper.writeValueAsString(user);
        final String address = HttpHost.unsecure(container.httpMappedAddress()) + "/data/User";
        final HttpPost post = new HttpPost(address);
        final HttpEntity entity = new StringEntity(jsonUser, ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        result.add(httpClient.execute(post, response -> {
          if (response.getCode() != 200) {
            throw new RuntimeException(
                "Unexpected response code: " + response.getCode() + ", " + response.getReasonPhrase());
          }
          return objectMapper.readValue(EntityUtils.toString(response.getEntity()), User.class);
        }));
      }
    }
    return result;
  }

  public static List<User> getUsers(int count, TDGContainer<?> node) throws IOException {
    final ObjectMapper objectMapper = new ObjectMapper();
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      final String address = HttpHost.unsecure(node.httpMappedAddress()) + "/data/User?first=" + count;
      return httpClient.execute(new HttpGet(address), response -> {
        if (response.getCode() != 200) {
          throw new RuntimeException(
              "Unexpected response code: " + response.getCode() + ", " + response.getReasonPhrase());
        }
        return objectMapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<User>>() {});
      });
    }
  }

  public static String uuid() {
    return UUID.randomUUID().toString();
  }
}
