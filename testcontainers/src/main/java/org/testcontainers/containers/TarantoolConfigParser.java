/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

class TarantoolConfigParser {

  private final Config config;

  public TarantoolConfigParser(String instanceFileName) {
    Yaml yaml = new Yaml(new Constructor(Config.class, new LoaderOptions()));
    InputStream inputStream = this.getClass()
        .getClassLoader()
        .getResourceAsStream(instanceFileName);
    config = yaml.load(inputStream);
  }

  public Integer[] getExposablePorts() {
    List<Integer> ports = config.getGroups()
        .get("routers")
        .getReplicasets()
        .values()
        .stream()
        .map(replicaset -> replicaset.getInstances().values())
        .flatMap(Collection::stream)
        .map(instance -> instance.getIproto().values())
        .flatMap(Collection::stream)
        .flatMap(Collection::stream)
        .map(Map::values)
        .flatMap(Collection::stream)
        .map(Integer::parseInt)
        .collect(Collectors.toList());

    return ports.toArray(new Integer[]{});
  }
}
