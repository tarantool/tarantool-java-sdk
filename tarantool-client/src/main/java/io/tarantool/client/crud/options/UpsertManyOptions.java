/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.crud.options;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tarantool.mapping.crud.CrudError;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.client.crud.TarantoolCrudSpace;

/**
 * <p> The class implements options for the CRUD operation
 * <a href="https://github.com/tarantool/crud#upsert-many"> upsert-many</a>.</p>
 * <p>The following options are available:</p>
 * <p>Common options:</p>
 * <ul>
 *     <li>{@link #timeout}.</li>
 *     <li>{@link #streamId}.</li>
 * </ul>
 * <p>Crud options:</p>
 * <ul>
 *     <li>{@value TIMEOUT}.</li>
 *     <li>{@value FIELDS}.</li>
 *     <li>{@value STOP_ON_ERROR}.</li>
 *     <li>{@value ROLLBACK_ON_ERROR}.</li>
 *     <li>{@value VSHARD_ROUTER}.</li>
 *     <li>{@value NO_RETURN}.</li>
 *     <li>{@value FETCH_LATEST_METADATA}.</li>
 * </ul>
 * <p>Examples:</p>
 * <blockquote><pre>{@code
 * <<Example 1>>
 *
 *  TarantoolCrudSpace space = crudClient.space("spaceName");
 *  long timeout = 3_000L;
 *  Map<String, Object> options = new HashMap<String, Object>(){{
 *      put("timeout", 2_000L);
 *      // Determine which fields will be returned
 *      put("fields", Arrays.asList("id", "name"));
 *  }};
 *
 *  UpsertManyOptions option = new UpsertManyOptions(timeout, null, options);
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  List<List<?>> operation = Collections.singletonList(Arrays.asList("=", "name", "Kostya"));
 *
 *  List<List<List<?>>> tuplesOperationData = Arrays.asList(
 *      Arrays.asList(Arrays.asList(1, true, "Vanya"), operation),
 *      Arrays.asList(Arrays.asList(2, false, "Victor"), operation)
 *  );
 *  // Update tuple (id = 1, isMarried = true, name = "Vanya") to tuple (id = 1, isMarried = true, name = "Kostya")
 *  // Insert tuple (id = 2, isMarried = false, name = "Victor")
 *  List<CrudError> res = space.upsert(tuplesOperationData, option);
 *
 * }</pre></blockquote>
 * <blockquote><pre>{@code
 * <<Example 2>>
 *
 * TarantoolCrudSpace space = crudClient.space("spaceName");
 *
 * UpsertManyOptions option = UpsertManyOptions.builder()
 *                                              .withTimeout(3_000L)
 *                                              .withCrudTimeout(2_000L)
 *                                              .withFields("id", "name")
 *                                              .build();
 *
 *  // Inserts tuple (id = 1, isMarried = true, name = "Vanya")
 *  space.insert(Arrays.asList(1, true, "Vanya")).get();
 *
 *  List<List<?>> operation = Collections.singletonList(Arrays.asList("=", "name", "Kostya"));
 *
 *  List<List<List<?>>> tuplesOperationData = Arrays.asList(
 *      Arrays.asList(Arrays.asList(1, true, "Vanya"), operation),
 *      Arrays.asList(Arrays.asList(2, false, "Victor"), operation)
 *  );
 *  // Update tuple (id = 1, isMarried = true, name = "Vanya") to tuple (id = 1, isMarried = true, name = "Kostya")
 *  // Insert tuple (id = 2, isMarried = false, name = "Victor")
 *  List<CrudError> res = space.upsert(tuplesOperationData, option);
 * }</pre></blockquote>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 * @see TarantoolCrudClient
 * @see TarantoolCrudSpace
 * @see CrudError
 */
public class UpsertManyOptions implements CrudOptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpsertManyOptions.class);

  /**
   * <p>{@link TarantoolCrudClient#call(String) vshard.call} timeout and vshard master
   * discovery timeout (in milliseconds).</p>
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a message from the
   * router to Tarantool instance and the time when the answer will come from Tarantool instance to router.</p>
   */
  private static final String TIMEOUT = "timeout";

  /**
   * <p>Field names for getting only a subset of fields.</p>
   */
  private static final String FIELDS = "fields";

  /**
   * <p>Stop on a first error and report error regarding the failed operation and error about what tuples were not
   * performed, default is <tt>false</tt>.</p>
   */
  private static final String STOP_ON_ERROR = "stop_on_error";

  /**
   * <p>Any failed operation will lead to rollback on a storage, where the operation is failed, report error about
   * what tuples were rollback, default is <tt>false</tt>.</p>
   */
  private static final String ROLLBACK_ON_ERROR = "rollback_on_error";

  /**
   * <p>Cartridge vshard group name. Set this parameter if your space is not a part of
   * the default vshard cluster.</p>
   */
  private static final String VSHARD_ROUTER = "vshard_router";

  /**
   * <p>Suppress successfully processed tuple (first return value is nil (null)). <tt>False</tt> by default.</p>
   */
  private static final String NO_RETURN = "noreturn";

  /**
   * <p>Guarantees the up-to-date metadata (space format) in first return value, otherwise it may not take into
   * account the latest migration of the data format. Performance overhead is up to 15%. <tt>False</tt> by default
   * .</p>
   */
  private static final String FETCH_LATEST_METADATA = "fetch_latest_metadata";

  /**
   * <p>Default value for {@link #timeout}.</p>
   */
  public static final long DEFAULT_TIMEOUT = 5_000;

  /**
   * <p> The time after which the request is considered invalid (in milliseconds).</p>
   * <p>Default value: {@value #DEFAULT_TIMEOUT} milliseconds.</p>
   * <p><i><b>Note</b></i>: The time indicated by this parameter is the time between sending a message from the
   * connector to Tarantool and the time when the answer will come from Tarantool to connector.</p>
   */
  private final Long timeout;

  /**
   * <p> Stream id for count operation.</p>
   * <p> Default value: {@code null}.</p>
   *
   * @see <a href="https://www.tarantool.io/ru/doc/latest/dev_guide/internals/iproto/streams/">Tarantool
   * documentation</a>
   */
  private final Long streamId;

  /**
   * <p>A map containing the correspondence between option names and their meanings.</p>
   */
  private final Map<String, Object> crudOptions;

  /**
   * <p>Creates a {@link UpsertManyOptions} object with the given parameters.</p>
   *
   * @param timeout  {@link #timeout}
   * @param streamId {@link #streamId}
   * @param options  {@link #crudOptions}
   * @see UpsertManyOptions
   */
  public UpsertManyOptions(Long timeout, Long streamId, Map<String, Object> options) {
    this.crudOptions = options;

    this.timeout = timeout;
    this.streamId = streamId;
  }

  /**
   * <p>Creates new builder instance of this class.</p>
   *
   * @return {@link UpsertManyOptions.Builder} object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * <p>Returns value of timeout option.</p>
   *
   * @return {@link #timeout} value.
   */
  @Override
  public long getTimeout() {
    return timeout;
  }

  /**
   * <p>Returns value of stream id option.</p>
   *
   * @return {@link #streamId} value.
   */
  @Override
  public Long getStreamId() {
    return streamId;
  }

  /**
   * <p>Returns an immutable option map for the count operation.</p>
   *
   * @return {@link Map} object.
   */
  @Override
  public Map<String, Object> getOptions() {
    return Collections.unmodifiableMap(crudOptions);
  }

  /**
   * <p>Builder class for {@link SelectOptions}.</p>
   *
   * @see SelectOptions
   */
  public static class Builder {

    /**
     * <p>See also: {@link UpsertManyOptions#crudOptions}.</p>
     */
    private final Map<String, Object> options = new HashMap<>();

    /**
     * <p>See also: {@link UpsertManyOptions#timeout}.</p>
     */
    private long timeout = DEFAULT_TIMEOUT;

    /**
     * <p>See also: {@link UpsertManyOptions#streamId}.</p>
     */
    private Long streamId;

    /**
     * <p>Sets value of {@link #timeout} option. Timeout parameter should be greater than 0.</p>
     *
     * @param timeout value of timeout option.
     * @return {@link UpsertManyOptions.Builder} object.
     * @throws IllegalArgumentException when timeout &#8804; 0.
     * @see UpsertManyOptions#timeout
     * @see UpsertManyOptions
     */
    public Builder withTimeout(long timeout) {
      if (timeout <= 0) {
        throw new IllegalArgumentException("timeout should be greater than 0");
      }
      this.timeout = timeout;
      return this;
    }

    /**
     * <p>Sets value of {@link #streamId} option. StreamId parameter should be greater or equal 0.</p>
     *
     * @param streamId value of stream id option
     * @return {@link UpsertManyOptions.Builder} object
     * @throws IllegalArgumentException when streamId &#60; 0
     * @see UpsertManyOptions#streamId
     * @see UpsertManyOptions
     */
    public Builder withStreamId(long streamId) {
      if (streamId < 0) {
        throw new IllegalArgumentException("streamId should be greater or equal 0");
      }
      this.streamId = streamId;
      return this;
    }

    /**
     * <p>Sets value of {@link #TIMEOUT} option. Timeout parameter should be greater or equal 0.</p>
     *
     * @param timeout value of {@link #TIMEOUT} option
     * @return {@link UpsertManyOptions.Builder} object
     * @see #TIMEOUT
     * @see UpsertManyOptions
     */
    public Builder withCrudTimeout(long timeout) {
      addOption(TIMEOUT, timeout);
      return this;
    }

    /**
     * <p>Adds options by name into {@link #options} map. Name parameter should not be equal {@code null}.</p>
     *
     * @param name  name of option
     * @param value value of option
     * @see UpsertManyOptions
     * @see UpsertManyOptions.Builder
     */
    private void addOption(String name, Object value) {
      if (name == null) {
        LOGGER.warn("Option isn't used since name of option is null, value = {}", value);
        return;
      }
      this.options.put(name, value);
    }

    /**
     * <p>Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.</p>
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link UpsertManyOptions.Builder} object
     * @see #FIELDS
     * @see UpsertManyOptions
     */
    public Builder withFields(String... fields) {
      addOption(FIELDS, Arrays.asList(fields));
      return this;
    }

    /**
     * <p>Sets value of {@link #FIELDS} option. Fields parameter should not be equal null.</p>
     *
     * @param fields value of {@link #FIELDS} option
     * @return {@link UpsertManyOptions.Builder} object
     * @see #FIELDS
     * @see UpsertManyOptions
     */
    public Builder withFields(List<String> fields) {
      addOption(FIELDS, fields);
      return this;
    }

    /**
     * <p>Sets value of {@link #VSHARD_ROUTER} option.</p>
     *
     * @param vshardRouter value of {@link  #VSHARD_ROUTER} option
     * @return {@link UpsertManyOptions.Builder}
     * @see #VSHARD_ROUTER
     * @see UpsertManyOptions
     */
    public Builder withVshardRouter(String vshardRouter) {
      addOption(VSHARD_ROUTER, vshardRouter);
      return this;
    }

    /**
     * <p>Sets value of {@link #NO_RETURN} to <tt>true</tt>.</p>
     *
     * @return {@link UpsertManyOptions.Builder} object
     * @see #NO_RETURN
     * @see UpsertManyOptions
     */
    public Builder withNoReturn() {
      addOption(NO_RETURN, true);
      return this;
    }

    /**
     * <p>Sets value of {@link #FETCH_LATEST_METADATA} to <tt>true</tt>.</p>
     *
     * @return {@link UpsertManyOptions.Builder} object
     * @see #FETCH_LATEST_METADATA
     * @see UpsertManyOptions
     */
    public Builder fetchLatestMetadata() {
      addOption(FETCH_LATEST_METADATA, true);
      return this;
    }

    /**
     * <p>Sets value of {@link #STOP_ON_ERROR} to <tt>true</tt>.</p>
     *
     * @return {@link UpsertManyOptions.Builder} object
     * @see #STOP_ON_ERROR
     * @see UpsertManyOptions
     */
    public Builder stopOnError() {
      addOption(STOP_ON_ERROR, true);
      return this;
    }

    /**
     * <p>Sets value of {@link #ROLLBACK_ON_ERROR} to <tt>true</tt>.</p>
     *
     * @return {@link UpsertManyOptions.Builder} object
     * @see #ROLLBACK_ON_ERROR
     * @see UpsertManyOptions
     */
    public Builder rollbackOnError() {
      addOption(ROLLBACK_ON_ERROR, true);
      return this;
    }

    /**
     * <p>Sets options by name and value. OptionName parameter should not be equal {@code null}.</p>
     *
     * @param optionName  name of option
     * @param optionValue value of option
     * @return {@link UpsertManyOptions.Builder}
     * @see UpsertManyOptions
     */
    public Builder withOption(String optionName, Object optionValue) {
      addOption(optionName, optionValue);
      return this;
    }

    /**
     * <p>Builds object of {@link UpsertManyOptions} class.</p>
     *
     * @return {@link UpsertManyOptions} object
     * @see UpsertManyOptions
     */
    public UpsertManyOptions build() {
      return new UpsertManyOptions(timeout, streamId, options);
    }
  }
}
