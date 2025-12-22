/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package org.testcontainers.containers.tarantool.config.tarantool3;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.testcontainers.containers.tarantool.config.ConfigurationUtils;

import io.tarantool.autogen.Tarantool3Configuration;
import io.tarantool.autogen.groups.groupsProperty.GroupsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.ReplicasetsProperty;
import io.tarantool.autogen.groups.groupsProperty.replicasets.replicasetsProperty.instances.instancesProperty.InstancesProperty;

/**
 * Class that helps get parameter from {@link Tarantool3Configuration}. Use extractors with different scopes to get
 * parameter from that scopes:
 * <ol>
 *   <li>Use {@link #globalExtractor} to get parameter from global scope</li>
 *   <li>Use {@link #groupExtractor} to get parameter from groups scope</li>
 *   <li>Use {@link #replicasetExtractor} to get parameter from replicaset scope</li>
 *   <li>Use {@link #instanceExtractor} to get parameter from instance scope</li>
 * </ol>
 *
 * @param <R> extracting parameter type
 */
public class ParameterExtractor<R> {

  /**
   * Extractor to get parameters from global scope of configuration. <b>Use to extract parameters from global scope
   * only</b>. If parameter is not present in scope return {@link Optional#empty()} instance.
   */
  private final Function<Tarantool3Configuration, Optional<R>> globalExtractor;

  /**
   * Extractor to get parameters from groups scope of configuration. <b>Use to extract parameters from groups scope
   * only</b>. If parameter is not present in scope return {@link Optional#empty()} instance.
   */
  private final Function<GroupsProperty, Optional<R>> groupExtractor;

  /**
   * Extractor to get parameters from replicaset scope of configuration. <b>Use to extract parameters from replicaset
   * scope only</b>. If parameter is not present in scope return {@link Optional#empty()} instance.
   */
  private final Function<ReplicasetsProperty, Optional<R>> replicasetExtractor;

  /**
   * Extractor to get parameters from instance scope of configuration. <b>Use to extract parameters from instance scope
   * only</b>. If parameter is not present in scope return {@link Optional#empty()} instance.
   */
  private final Function<InstancesProperty, Optional<R>> instanceExtractor;

  /**
   * {@link ConfigCombiner} function to combine result returned from {@link #globalExtractor}, {@link #groupExtractor},
   * {@link #replicasetExtractor}, {@link #instanceExtractor}. Can use to set priority for returning parameters.
   */
  private final ConfigCombiner<Optional<R>, Optional<R>, Optional<R>, Optional<R>, Optional<R>> combineFunction;

  /**
   * See {@link ParameterExtractor}
   *
   * @param globalExtractor     see {@link #globalExtractor}
   * @param groupExtractor      see {@link #groupExtractor}
   * @param replicasetExtractor see {@link #replicasetExtractor}
   * @param instanceExtractor   see {@link #instanceExtractor}
   * @param combineFunction     see {@link #combineFunction}
   */
  public ParameterExtractor(
      Function<Tarantool3Configuration, Optional<R>> globalExtractor,
      Function<GroupsProperty, Optional<R>> groupExtractor,
      Function<ReplicasetsProperty, Optional<R>> replicasetExtractor,
      Function<InstancesProperty, Optional<R>> instanceExtractor,
      ConfigCombiner<Optional<R>, Optional<R>, Optional<R>, Optional<R>, Optional<R>> combineFunction) {
    this.globalExtractor = globalExtractor;
    this.groupExtractor = groupExtractor;
    this.replicasetExtractor = replicasetExtractor;
    this.instanceExtractor = instanceExtractor;
    this.combineFunction = combineFunction;
  }

  /**
   * Returns map with keys are Tarantool 3 instance names, values are extracted parameter. If resulting parameter is not
   * present or null, this parameter won't put into result map. Extracts parameter for all instance in configuration.
   */
  public Map<String, R> getParameter(Tarantool3Configuration config) {
    final List<String> instances = ConfigurationUtils.parseInstances(config);
    if (instances.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<String, R> result = new HashMap<>();

    for (String instance : instances) {
      final Optional<R> parameter = getParameter(instance, config);
      parameter.ifPresent(r -> result.put(instance, r));
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * Same as {@link #getParameter(Tarantool3Configuration)}. Returns parameter for only passed instance name.
   */
  public Optional<R> getParameter(String instanceName, Tarantool3Configuration config) {
    final Optional<R> globalValue = this.globalExtractor.apply(config);

    final Collection<GroupsProperty> groups = config.getGroups().orElseThrow(IllegalArgumentException::new)
        .getAdditionalProperties().values();

    for (GroupsProperty group : groups) {
      final Optional<R> groupValue = groupExtractor.apply(group);
      final Collection<ReplicasetsProperty> replicasets =
          group.getReplicasets().orElseThrow(IllegalArgumentException::new).getAdditionalProperties().values();

      for (ReplicasetsProperty replicaset : replicasets) {
        final Optional<R> replicasetValue = replicasetExtractor.apply(replicaset);

        final Map<String, InstancesProperty> additionalProperties = replicaset.getInstances()
            .orElseThrow(IllegalArgumentException::new).getAdditionalProperties();

        for (Entry<String, InstancesProperty> entry : additionalProperties.entrySet()) {
          if (Objects.equals(instanceName, entry.getKey())) {
            final Optional<R> instanceValue = instanceExtractor.apply(entry.getValue());
            return combineFunction.combine(globalValue, groupValue, replicasetValue, instanceValue);
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Use to combine result that return from {@link #globalExtractor}, {@link #groupExtractor},
   * {@link #replicasetExtractor}, {@link #instanceExtractor}.
   *
   * @param <GLOBAL>     type of parameter returned from {@link #globalExtractor}
   * @param <GROUP>      type of parameter returned from {@link #groupExtractor}
   * @param <REPLICASET> type of parameter returned from {@link #replicasetExtractor}
   * @param <INSTANCE>   type of parameter returned from {@link #instanceExtractor}
   * @param <RESULT>     type of result returned after combining
   */
  @FunctionalInterface
  public interface ConfigCombiner<GLOBAL, GROUP, REPLICASET, INSTANCE, RESULT> {

    RESULT combine(GLOBAL global, GROUP group, REPLICASET replicaset, INSTANCE instance);
  }
}
