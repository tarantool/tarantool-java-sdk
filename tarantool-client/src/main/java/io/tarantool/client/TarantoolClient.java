/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.channel.ChannelOption;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.pool.IProtoClientPool;

/**
 * <p>Implements a base contract for a clients.</p>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/iDneprov">Ivan Dneprov</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
// TODO: call/eval with multi return https://github.com/tarantool/tarantool-java-ee/issues/248
// TODO: add sql functions https://github.com/tarantool/tarantool-java-ee/issues/221
public interface TarantoolClient extends AutoCloseable {

  /**
   * <p>Default netty network channel options.</p>
   */
  Map<ChannelOption<?>, Object> DEFAULT_NETTY_CHANNEL_OPTIONS =
      Collections.unmodifiableMap(new HashMap<ChannelOption<?>, Object>() {{
        put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000);
        put(ChannelOption.TCP_NODELAY, true);
        put(ChannelOption.SO_KEEPALIVE, true);
        put(ChannelOption.SO_REUSEADDR, true);
      }});

  /**
   * <p>Default instance group tag.</p>
   */
  String DEFAULT_TAG = "default";

  /**
   * <p>Default number of netty threads.</p>
   */
  int DEFAULT_CONNECTION_THREADS_NUMBER = 0;

  /**
   * <p>Default connection timeout.</p>
   */
  long DEFAULT_CONNECTION_TIMEOUT = 3_000L;

  /**
   * <p>Default time after which reconnect occurs.</p>
   */
  long DEFAULT_RECONNECT_AFTER = 1_000L;

  /**
   * <p>Default graceful shutdown value.</p>
   */
  boolean DEFAULT_GRACEFUL_SHUTDOWN = true;

  /**
   * <p>Function returns  {@link TarantoolSpace space} with the name specified as the input argument.</p>
   *
   * @param name name of the {@link TarantoolSpace space} that was requested.
   * @return {@link TarantoolSpace} object.
   */
  TarantoolSpace space(String name);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String)} represents a remote
   * stored procedure call. {@link #call(String)} returns what it returns function. <i><b>Limitation</b></i>: a called
   * function cannot return a function, for example if <i>func2</i> is defined as <i>function func2 () return func
   * end</i>, then <i>{@link #call(String)}</i> will return the error “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  ***
   *
   *  // lua function
   *  function func() return 1 end
   *
   *  ***
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<?> output = client.call("func").join();
   *  assertEquals(Collections.singletonList(1), output);
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of stored procedure
   * result, otherwise this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> call(String function);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, Class)} represents a
   * remote stored procedure call. {@link #call(String, Class)} returns what it returns function.
   * <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is defined as
   * <i>function func2 () return func end</i>, then <i> {@link #call(String, Class)}</i> will return the error
   * “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   * ***
   *
   * function get_default_person()
   *   return {1, false, "default_name"}
   * end
   *
   * ***
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<Person> output = client.call("get_default_person", Person.class).join();
   *  assertEquals(Collections.singletonList(new Person(1, false, "default_name)), output);
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param <T>      return type
   * @param entity   {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of stored procedure
   * result, otherwise this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> call(String function, Class<T> entity);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, TypeReference)}
   * represents a remote stored procedure call. {@link #call(String, TypeReference)} returns what it returns function.
   * <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is defined as
   * <i>function func2 () return func end</i>, then <i>{@link #call(String, TypeReference)}</i> will return the error
   * “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   * ***
   *
   * function get_default_person()
   *   return { 1, false, "default_name" }
   * end
   *
   * ***
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<Person> output = client.call("get_default_person", new TypeReference<List<Person>>(){}).join();
   *  assertEquals(Collections.singletonList(new Person(1, false, "default_name")), output);
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param <T>      return type
   * @param entity   {@link TypeReference} object
   * @return {@link CompletableFuture} object. If successful - future is completed with procedure return type, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<T>> call(String function, TypeReference<T> entity);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, List)} represents a
   * remote stored procedure call. {@link #call(String, List)} returns what it returns function.
   * <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is defined as
   * <i>function func2 () return func end</i>, then <i>{@link #call(String, List)}</i> will return the error “error:
   * unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<List<?>> tuples = Arrays.asList(Arrays.asList(2, false, "IvanD"),
   *                                       Arrays.asList(3, true, "IvanB"));
   *  List<?> output = client.call("echo", tuples).join();
   *  assertEquals(tuples, output);
   *
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param args     list of arguments
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of stored procedure
   * result, otherwise this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> call(String function, List<?> args);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, List, Class)}
   * represents a remote stored procedure call. {@link #call(String, List, Class)} returns what it returns function.
   * <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is defined as
   * <i>function func2 () return func end</i>, then <i>{@link #call(String, List, Class)}</i> will return the error
   * “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<List<?>> tuples = Arrays.asList(Arrays.asList(2, false, "IvanD"),
   *                                       Arrays.asList(3, true, "IvanB"));
   *  List<Person> output = client.call("echo", tuples, Person.class).join();
   *  assertEquals(Arrays.asList(new Person(2, false, "IvanD"),
   *                             new Person(3, true, "IvanB")), output);
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param args     list of arguments
   * @param <T>      return type
   * @param entity   {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of stored procedure
   * result, otherwise this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> call(
      String function,
      List<?> args,
      Class<T> entity);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, List, TypeReference)}
   * represents a remote stored procedure call. {@link #call(String, List, TypeReference)} returns what it returns
   * function. <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is
   * defined as <i>function func2 () return func end</i>, then <i>{@link #call(String, List, TypeReference)}</i> will
   * return the error “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<List<?>> tuples = Arrays.asList(Arrays.asList(2, false, "IvanD"),
   *                                       Arrays.asList(3, true, "IvanB"));
   *  List<Person> output = client.call("echo", tuples, new TypeReference<List<Person>>(){}).join();
   *  assertEquals(Arrays.asList(new Person(2, false, "IvanD"),
   *                             new Person(3, true, "IvanB")), output);
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param args     list of arguments
   * @param <T>      return type
   * @param entity   {@link TypeReference} object
   * @return {@link CompletableFuture} object. If successful - future is completed with procedure return type, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<T>> call(
      String function,
      List<?> args,
      TypeReference<T> entity);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, List, Options)}
   * represents a remote stored procedure call. {@link #call(String, List, Options)} returns what it returns function.
   * <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is defined as
   * <i>function func2 () return func end</i>, then <i>{@link #call(String, List, Options)}</i> will return the error
   * “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(2_000L)
   *                               .build();
   *
   *  List<List<?>> tuples = Arrays.asList(Arrays.asList(2, false, "IvanD"),
   *                                       Arrays.asList(3, true, "IvanB"));
   *  List<?> output = client.call("echo", tuples, options).join();
   *  assertEquals(tuples, output);
   *
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param args     list of arguments
   * @param opts     {@link Options} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of stored procedure
   * result, otherwise this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> call(
      String function,
      List<?> args,
      Options opts);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, List, Options, Class)}
   * represents a remote stored procedure call. {@link #call(String, List, Options, Class)} returns what it returns
   * function. <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is
   * defined as <i>function func2 () return func end</i>, then <i>{@link #call(String, List, Options, Class)}</i> will
   * return the error “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(2_000L)
   *                               .build();
   *
   *  List<List<?>> tuples = Arrays.asList(Arrays.asList(2, false, "IvanD"),
   *                                       Arrays.asList(3, true, "IvanB"));
   *  List<Person> output = client.call("echo", tuples, options, Person.class).join();
   *  assertEquals(Arrays.asList(new Person(2, false, "IvanD"),
   *                             new Person(3, true, "IvanB")), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param function name of stored procedure
   * @param args     list of arguments
   * @param opts     {@link Options} object
   * @param <T>      return type
   * @param entity   {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with procedure return type, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> call(
      String function,
      List<?> args,
      Options opts,
      Class<T> entity);

  /**
   * <p>The method calls stored procedure like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.call">call</a>
   * does. The method makes a remote call similar to func('1', '2', '3'). So {@link #call(String, List, Options, Class)}
   * represents a remote stored procedure call. {@link #call(String, List, Options, Class)} returns what it returns
   * function. <i><b>Limitation</b></i>: a called function cannot return a function, for example if <i>func2</i> is
   * defined as <i>function func2 () return func end</i>, then <i>{@link #call(String, List, Options, Class)}</i> will
   * return the error “error: unsupported Lua type “function”.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p><i><b>Note</b></i>: the return value of the function called through this method must be serializable in
   * msgpack.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(2_000L)
   *                               .build();
   *
   *  List<List<?>> tuples = Arrays.asList(Arrays.asList(2, false, "IvanD"),
   *                                       Arrays.asList(3, true, "IvanB"));
   *  List<Person> output = client.call("echo", tuples, options, new TypeReference<List<Person>>(){})
   *                              .join();
   *
   *  assertEquals(Arrays.asList(new Person(2, false, "IvanD"),
   *                             new Person(3, true, "IvanB")), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param <T>      return type
   * @param function name of stored procedure
   * @param args     list of arguments
   * @param formats  formats if tuples are in argument list
   * @param opts     {@link Options} object
   * @param entity   {@link TypeReference} object
   * @return {@link CompletableFuture} object. If successful - future is completed with procedure return type, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<T>> call(
      String function,
      List<?> args,
      Object formats,
      Options opts,
      TypeReference<T> entity);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<?> output = client.eval("return 2008").join();
   *  assertEquals(Collections.singletonList(2008), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> eval(String expression);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<Integer> output = client.eval("return 2008", Integer.class).join();
   *  assertEquals(Collections.singletonList(2008), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param <T>        return type
   * @param entity     {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> eval(String expression, Class<T> entity);


  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  // Returns integer
   *  List<Integer> output = client.eval("return 2008", new TypeReference<List<Integer>>() {}).join();
   *
   *  // Returns long
   *  List<Long> secondOutput = client.eval("return 2008", new TypeReference<List<Long>>() {}).join();
   *
   *  assertEquals(Collections.singletonList(2008), output);
   *  assertEquals(Collections.singletonList(2008), secondOutput);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param <T>        return type
   * @param entity     {@link TypeReference} object
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with return type,
   * otherwise this future will be completed exceptionally by exception
   */
  <T> CompletableFuture<TarantoolResponse<T>> eval(String expression, TypeReference<T> entity);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<?> input = Arrays.asList(1, true, "IvanB");
   *
   *  // we use table packing because it's more popular than multi return value
   *  // ... - replaced to -> arg1, arg2, arg3, ...
   *  // client.eval("return 1, 2, 3") <- equals to -> client.eval("return ...", Arrays.asList(1,2,3))
   *  List<?> output = client.eval("return ...", input).join();
   *  assertEquals(input, output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param args       list of arguments
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> eval(String expression, List<?> args);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<?> input = Arrays.asList(1, true, "IvanB");
   *
   *  // Person class see in TarantoolCrudSpace interface description
   *  // ... - replaced to -> arg1, arg2, arg3, ...
   *  // client.eval("return 1, 2, 3") <- equals to -> client.eval("return ...", Arrays.asList(1,2,3))
   *  List<Person> output = client.eval("return {...}", input, Person.class).join();
   *  assertEquals(Collections.singletonList(new Person(1, true, "IvanB")), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param args       list of arguments
   * @param <T>        return type
   * @param entity     {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> eval(
      String expression,
      List<?> args,
      Class<T> entity);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<?> input = Arrays.asList(1, true, "IvanB");
   *
   *  // Person class see in TarantoolCrudSpace interface description
   *  // ... - replaced to -> arg1, arg2, arg3, ...
   *  // client.eval("return 1, 2, 3") <- equals to -> client.eval("return ...", Arrays.asList(1,2,3))
   *  List<?> output = client.eval("return ...", input, new TypeReference<List<?>>(){}).join();
   *  assertEquals(input, output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param args       list of arguments
   * @param <T>        return type
   * @param entity     {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<T>> eval(
      String expression,
      List<?> args,
      TypeReference<T> entity);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(4_000L)
   *                               .build();
   *
   *  List<?> input = Arrays.asList(1, true, "IvanB");
   *
   *  // we use table packing because it's more popular than multi return value
   *  // ... - replaced to -> arg1, arg2, arg3, ...
   *  // client.eval("return 1, 2, 3") <- equals to -> client.eval("return ...", Arrays.asList(1,2,3))
   *  List<?> output = client.eval("return ...", input, options).join();
   *  assertEquals(input, output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param args       list of arguments
   * @param opts       {@link Options} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> eval(
      String expression,
      List<?> args,
      Options opts);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p><i><b>Note</b></i>: This method always returns a list of results since Lua functions have multi-return
   * .</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(4_000L)
   *                               .build();
   *
   *  List<?> input = Arrays.asList(1, true, "IvanB");
   *
   *  // Person class see in TarantoolCrudSpace interface description
   *  // ... - replaced to -> arg1, arg2, arg3, ...
   *  // client.eval("return 1, 2, 3") <- equals to -> client.eval("return ...", Arrays.asList(1,2,3))
   *  List<Person> output = client.eval("return {...}", input, options, Person.class).join();
   *  assertEquals(Collections.singletonList(new Person(1, true, "IvanB")), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param expression expression in Lua.
   * @param args       list of arguments
   * @param opts       {@link Options} object
   * @param <T>        return type
   * @param entity     {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> eval(
      String expression,
      List<?> args,
      Options opts,
      Class<T> entity);

  /**
   * <p>The method evaluates Lua expression like the function
   * <a href="https://www.tarantool.io/ru/doc/latest/reference/reference_lua/net_box/#lua-function.conn.eval">eval</a>
   * does. Evaluates and executes an expression in a Lua string, which can be any expression or several expressions.
   * Execute permissions are required.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sent as Lua string
   * will be executed on the router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<?> input = Arrays.asList(1, true, "IvanB");
   *
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(4_000L)
   *                               .build();
   *
   *  // Person class see in TarantoolCrudSpace interface description
   *  // ... - replaced to -> arg1, arg2, arg3, ...
   *  // client.eval("return 1, 2, 3") <- equals to -> client.eval("return ...", Arrays.asList(1,2,3))
   *  Person output = client.eval("return ...", input, options, new TypeReference<Person>(){}).join();
   *  assertEquals(new Person(1, true, "IvanB"), output);
   *
   * }
   * </pre></blockquote>
   *
   * @param <T>        return type
   * @param expression expression in Lua.
   * @param args       list of arguments
   * @param formats    formats if tuples are in argument list
   * @param opts       {@link Options} object
   * @param entity     {@link Class} object
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of eval result, otherwise
   * this future will be completed exceptionally
   */
  <T> CompletableFuture<TarantoolResponse<T>> eval(
      String expression,
      List<?> args,
      Object formats,
      Options opts,
      TypeReference<T> entity);

  /**
   * <p>Method sends ping request to Tarantool.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  boolean result = client.ping().join();
   *
   * }
   * </pre></blockquote>
   *
   * @return {@link CompletableFuture} object. If successful - future is completed with ping request result, otherwise
   * this future will be completed exceptionally
   */
  CompletableFuture<Boolean> ping();

  /**
   * <p>Method sends ping request with {@link Options options} to Tarantool.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  Options options = BaseOptions.builder()
   *                               .withTimeout(4_000L)
   *                               .build();
   *
   *  boolean result = client.ping(options).join();
   *
   * }
   * </pre></blockquote>
   *
   * @param opts {@link Options} object
   * @return {@link CompletableFuture} object. If successful - future is completed with ping request result, otherwise
   * this future will be completed exceptionally
   */
  CompletableFuture<Boolean> ping(Options opts);


  /**
   * <p>Method allows to subscribe to events broadcast by Tarantool.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<Object> eventsKey1 = new ArrayList<>();
   *  List<Object> eventsKey2 = new ArrayList<>();
   *
   *  client.watch("key1", eventsKey1::add);
   *  client.watch("key2", eventsKey2::add);
   *
   *  client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
   *  Thread.sleep(100);
   *
   *  client.unwatch("key1");
   *  client.unwatch("key2");
   *  client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
   *  Thread.sleep(100);
   *
   *  assertEquals(Collections.singletonList("myEvent"), eventsKey1);
   *  assertEquals(Collections.singletonList(Arrays.asList(1, 2, 3)), eventsKey2);
   *
   * }
   * </pre></blockquote>
   *
   * @param key      key of event
   * @param callback handler function
   */
  void watch(String key, Consumer<TarantoolResponse<?>> callback);

  /**
   * <p>Method allows to subscribe to events broadcast by Tarantool.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<String> eventsKey1 = new ArrayList<>();
   *  List<Person> eventsKey2 = new ArrayList<>();
   *
   *  client.watch("string_event", eventsKey1::add, String.class);
   *  client.watch("secret_person", eventsKey2::add, Person.class);
   *
   *  client.eval("box.broadcast('string_event', 'myEvent'); box.broadcast('secret_person', {1, true, 'JohnWick'})");
   *  Thread.sleep(100);
   *
   *  assertEquals(Collections.singletonList("myEvent"), eventsKey1);
   *  assertEquals(Collections.singletonList(new Person(1, true, "JohnWick")), eventsKey2);
   *
   * }
   * </pre></blockquote>
   *
   * @param key      key of event
   * @param <T>      return type
   * @param entity   {@link Class} object
   * @param callback handler function
   */
  <T> void watch(
      String key,
      Consumer<TarantoolResponse<T>> callback,
      Class<T> entity);

  /**
   * <p>Method allows to subscribe to events broadcast by Tarantool.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<Map<String, List<Person>>> eventsKey = new ArrayList<>();
   *
   *  client.watch("mega_event", eventsKey::add, new TypeReference<Map<String, List<Person>>>() {});
   *
   *  client.eval("box.broadcast('mega_event', { agents = {{1, true, 'Wick'}, {007, false, 'Bond'}} })");
   *  Thread.sleep(100);
   *
   *  HashMap<String, List<Person>> map = new HashMap<>();
   *  map.put("agents", Arrays.asList(new Person(1, true, "Wick"), new Person(007, false, "Bond")));
   *
   *  List<Map<String, List<Person>>> expected = Collections.singletonList(map);
   *  assertEquals(expected, eventsKey);
   *
   * }
   * </pre></blockquote>
   *
   * @param key      key of event
   * @param <T>      return type
   * @param entity   {@link TypeReference} object
   * @param callback handler function
   */
  <T> void watch(
      String key,
      Consumer<TarantoolResponse<T>> callback,
      TypeReference<T> entity);

  /**
   * <p>Method allows fetching the value currently associated with a
   * specified notification key without subscribing to changes. </p>
   *
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
   *  assertEquals(
   *     Collections.singletonList("myEvent"),
   *     client.watchOnce("key1").join()
   *  );
   *  assertEquals(
   *     Collections.singletonList(Arrays.asList(1, 2, 3)),
   *     client.watchOnce("key2").join()
   *  );
   *
   *  client.eval("box.broadcast('key1', 1)");
   *  assertEquals(
   *      Collections.singletonList(1),
   *      client.watchOnce("key1").join()
   *  );
   *  assertEquals(
   *      Collections.singletonList(Arrays.asList(1, 2, 3)),
   *      client.watchOnce("key2").join()
   *  );
   * }
   * </pre></blockquote>
   *
   * @param key key of event
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of broadcast result,
   * otherwise this future will be completed exceptionally
   */
  CompletableFuture<TarantoolResponse<List<?>>> watchOnce(String key);

  /**
   * <p>Method allows fetching the value currently associated with a
   * specified notification key without subscribing to changes. </p>
   *
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
   *  assertArrayEquals(
   *      "myEvent".toCharArray(),
   *      client.watchOnce("key1", char[].class).join().get(0)
   *  );
   *  assertEquals(
   *      Collections.singletonList(new HashSet<>(Arrays.asList(1, 2, 3))),
   *      client.watchOnce("key2", Set.class).join()
   *  );
   *
   *  client.eval("box.broadcast('key1', 1)");
   *  assertArrayEquals(
   *      1L,
   *      client.watchOnce("key1", Long.class).join().get(0)
   *  );
   *  assertEquals(
   *      Collections.singletonList(new HashSet<>(Arrays.asList(1, 2, 3))),
   *      client.watchOnce("key2", Set.class).join()
   *  );
   * }
   * </pre></blockquote>
   *
   * @param key    key of event
   * @param entity {@link Class} object
   * @param <T>    return type
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of broadcast result,
   * otherwise this future will be completed exceptionally
   * @see TarantoolClient#watchOnce(String)
   */
  <T> CompletableFuture<TarantoolResponse<List<T>>> watchOnce(String key, Class<T> entity);

  /**
   * <p>Method allows fetching the value currently associated with a
   * specified notification key without subscribing to changes. </p>
   *
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  client.eval("box.broadcast('key2', {1, 2, 3})");
   *  assertEquals(
   *     Collections.singletonList(Arrays.asList(1L, 2L, 3L)),
   *     client.watchOnce("key2", new TypeReference<List<List<Long>>>() {}).join()
   *  );
   * }
   * </pre></blockquote>
   *
   * @param key    key of event
   * @param entity {@link TypeReference} object
   * @param <T>    return type
   * @return {@link CompletableFuture} object. If successful - future is completed with a list of broadcast result,
   * otherwise this future will be completed exceptionally
   * @see TarantoolClient#watchOnce(String)
   * @see TarantoolClient#watchOnce(String, Class)
   */
  <T> CompletableFuture<TarantoolResponse<T>> watchOnce(String key, TypeReference<T> entity);

  /**
   * <p>Method allows to unsubscribe to events broadcast by Tarantool.</p>
   * <p><i><b>Note</b></i>: when calling this method via {@link TarantoolCrudClient} requests sends to router.</p>
   * <p>To use this method correctly, you can follow this example:</p>
   * <blockquote><pre>{@code
   *  // Creates client with default parameters
   *  TarantoolClient client = TarantoolFactory.box()
   *                                           .build();
   *
   *  List<Object> eventsKey1 = new ArrayList<>();
   *  List<Object> eventsKey2 = new ArrayList<>();
   *
   *  client.watch("key1", eventsKey1::add);
   *  client.watch("key2", eventsKey2::add);
   *
   *  client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
   *  Thread.sleep(100);
   *
   *  client.unwatch("key1");
   *  client.unwatch("key2");
   *  client.eval("box.broadcast('key1', 'myEvent'); box.broadcast('key2', {1, 2, 3})");
   *  Thread.sleep(100);
   *
   *  assertEquals(Collections.singletonList("myEvent"), eventsKey1);
   *  assertEquals(Collections.singletonList(Arrays.asList(1, 2, 3)), eventsKey2);
   *
   * }
   * </pre></blockquote>
   *
   * @param key key of event
   */
  void unwatch(String key);

  /**
   * <p>Method closes all connection from client to Tarantool.</p>
   *
   * @throws Exception exception
   */
  @Override
  void close() throws Exception;

  /**
   * Gets instance of {@link TarantoolBalancer}.
   *
   * @return the balancer
   */
  TarantoolBalancer getBalancer();

  /**
   * Gets pool of {@link io.tarantool.core.IProtoClient}.
   *
   * @return the pool
   */
  IProtoClientPool getPool();

  ClientType getType();

  /**
   * Returns true if the client is closed and all resources are freed.
   *
   * @return {@link Boolean} object
   */
  boolean isClosed();
}
