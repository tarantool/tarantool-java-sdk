/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package org.testcontainers.containers.utils;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

public class CartridgeConfigParser {

  private final AtomicReference<Map<String, Map<String, Object>>> instances =
      new AtomicReference<>();

  public CartridgeConfigParser(String instanceFileName) {
    Yaml yaml = new Yaml();
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(instanceFileName);
    instances.set(Collections.unmodifiableMap(yaml.load(inputStream)));
  }

  public Integer[] getExposablePorts() {
    List<Integer> ports =
        instances.get().values().stream()
            .map(Instance::new)
            .map(Instance::getBinaryPort)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    ports.addAll(
        instances.get().values().stream()
            .map(Instance::new)
            .map(Instance::getHttpPort)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
    return ports.toArray(new Integer[] {});
  }

  static class Instance {
    private String workdir;
    private String advertiseUri;
    private Integer httpPort;
    private Integer binaryPort;

    public Instance(Map<String, Object> map) {
      this.workdir = (String) map.get("workdir");
      this.httpPort = (Integer) map.get("http_port");
      this.advertiseUri = (String) map.get("advertise_uri");
      this.binaryPort =
          this.advertiseUri != null
              ? Integer.parseInt(this.advertiseUri.substring(this.advertiseUri.indexOf(':') + 1))
              : null;
    }

    public String getWorkdir() {
      return workdir;
    }

    public void setWorkdir(String workdir) {
      this.workdir = workdir;
    }

    public String getAdvertiseUri() {
      return advertiseUri;
    }

    public void setAdvertiseUri(String advertiseUri) {
      this.advertiseUri = advertiseUri;
    }

    public void setBinaryPort(int binaryPort) {
      this.binaryPort = binaryPort;
    }

    public Integer getBinaryPort() {
      return binaryPort;
    }

    public Integer getHttpPort() {
      return httpPort;
    }

    public void setHttpPort(int httpPort) {
      this.httpPort = httpPort;
    }
  }
}
