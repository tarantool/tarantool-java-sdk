/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.box;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import io.tarantool.balancer.TarantoolBalancer;
import io.tarantool.client.BaseOptions;
import io.tarantool.client.Options;
import io.tarantool.client.TarantoolSpace;
import io.tarantool.client.box.options.DeleteOptions;
import io.tarantool.client.box.options.SelectOptions;
import io.tarantool.client.box.options.UpdateOptions;
import io.tarantool.client.operation.Operations;
import io.tarantool.mapping.SelectResponse;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.schema.TarantoolSchemaFetcher;

/**
 * The interface provides a contract for operations that can be performed with the <a
 * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#spaces">space</a>.
 *
 * <p>Note: Classes that implement this interface work directly with a specific Tarantool instance
 * that the {@link TarantoolBalancer balancer} selects, without referring to Tarantool instance
 * through router.
 *
 * <p>Each method of this interface uses a tuple object. To create that you can follow two ways:
 *
 * <ul>
 *   <li>You can use POJO classes annotated with special Jackson library annotations:
 *       <blockquote>
 *       <pre>
 *
 *     &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
 *     {@code
 *     public class Person {
 *         public Integer id;
 *         public Boolean isMarried;
 *         public String name;
 *     };
 *
 *     ...
 *
 *     Person person = new Person(1, true, "Ivan");
 *     space.insert(person);
 *     }
 *     </pre>
 *       </blockquote>
 *       <p>At the moment the mapping of fields in a tuple object occurs flatly. This means that
 *       mapping occurs not according to the names of the fields (the specifics of the space format
 *       are not taken into account), but taking into account their order in the list. More detailed
 *       explanation:
 *       <blockquote>
 *       <pre>
 *     // An annotation that allows to convert class fields into
 *     // an array, taking into account the order of the fields.
 *     &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
 *     {@code
 *     public class Person {
 *
 *         // First field in the data schema
 *         public Integer id;
 *
 *         //  Second field in the data schema
 *         public Boolean isMarried;
 *
 *         // Third field in the data schema
 *         public String name;
 *     };
 *     }
 *     </pre>
 *       </blockquote>
 *   <li>You can use a list as a tuple object, which elements are field values:
 *       <blockquote>
 *       <pre>{@code
 * // id, isMarried, name fields
 * List<?> person = Arrays.asList(1, true, "Artyom");
 *
 * ...
 *
 * // insert person tuple object
 * space.insert(person);
 * }</pre>
 *       </blockquote>
 * </ul>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolBoxSpace extends TarantoolSpace {

  /**
   * Exception message when trying to use space name and index name in {@code Tarantool version <
   * 3.0.0} with fetcher disabled.
   */
  String WITHOUT_ENABLED_FETCH_SCHEMA_OPTION_FOR_TARANTOOL_LESS_3_0_0 =
      "Can't use space or index names without enabled fetchSchema option for Tarantool version <"
          + " 3.0.0";

  /**
   * The method inserts a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">insert</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly. The implementation uses standard {@link BaseOptions options}.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * ...
   *
   * // Or this.
   * List<?> person = Arrays.asList(1, true, "Petya");
   *
   * // Get specific space with your name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple object you created earlier
   * // Wait for the result of the insert operation
   * List<?> insertedTuple = space.insert(person).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> insert(Object tuple);

  /**
   * The method inserts a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">insert</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly. {@link Options Options} parameter is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * ...
   *
   * // Or this.
   * List<?> person = Arrays.asList(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * // Use the tuple and options objects you created earlier
   * // Wait for the result of the insert operation
   * List<?> insertedTuple = space.insert(person, options).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> insert(Object tuple, Options options);

  /**
   * The method inserts a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">insert</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly . {@link Class} class of tuple object is passed as argument. The implementation uses
   * standard {@link BaseOptions options}.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple and class objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedTuple = space.insert(person, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(Object tuple, Class<T> entity);

  /**
   * The method inserts a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">insert</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly . {@link Class} class of tuple object and {@link Options Options} parameter are passed
   * as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple, options and Class objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedTuple = space.insert(person, options, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(Object tuple, Options options, Class<T> entity);

  /**
   * The method inserts a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">insert</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly . {@link TypeReference} object is passed as argument.
   *
   * <p>To use this method correctly, you can follow this examples:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple and TypeReference objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedPerson = space.insert(person, typeReference).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * <p>If you don't want to create a POJO tuple object and you know the structure of the data being
   * returned, you can return it directly:
   *
   * <blockquote>
   *
   * <pre>{@code
   * List<?> person = Arrays.asList(1, true, "Vasya");
   * List<?> insertedPerson = space.insert(person, new TypeReference<List<?>>(){});
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> insert(Object tuple, TypeReference<T> entity);

  /**
   * The method inserts a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">insert</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/insert/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly . {@link TypeReference} object and {@link Options Options} parameter are passed as
   * argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolBoxSpace space  = boxClient.space(spaceName);
   *
   * // Use the tuple, options and TypeReference objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedTuple = space.insert(person, options, typeReference).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #insert(Object, TypeReference)}.
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> insert(
      Object tuple, Options options, TypeReference<T> entity);

  /**
   * The method replaces a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">replace</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly . The implementation uses standard {@link BaseOptions options}.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * ...
   *
   * // Or this.
   * List<?> person = Arrays.asList(1, true, "Petya");
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple object you created earlier
   * // Wait for the result of the insert operation
   * List<?> insertedTuple = space.insert(person).join().get();
   *
   * // Change tuple object
   * person = new Person(1, false, "Tuzik");
   *
   * ...
   *
   * // Or this.
   * person = Arrays.asList(1, true, "Tuzik");
   *
   * // Use the tuple object you changed earlier (match by primary key)
   * // Wait for the result of the replace operation
   * List<?> replacedTuple = space.replace(person).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object to replace. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> replace(Object tuple);

  /**
   * The method replaces a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">replace</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly . {@link Options Options} parameter is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * ...
   *
   * // Or this.
   * List<?> person = Arrays.asList(1, true, "Petya");
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple and options objects you created earlier
   * // Wait for the result of the insert operation
   * List<?> insertedTuple = space.insert(person, options).join().get();
   *
   * person = new Person(1, false, "Tuzik");
   *
   * // Use the changed tuple and options object you created earlier
   * // Wait for the result of the replace operation
   * List<?> replacedTuple = space.replace(person, options).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> replace(Object tuple, Options options);

  /**
   * The method replaces a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">replace</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link Class} class of tuple object is passed as argument. The implementation uses
   * standard {@link BaseOptions options}.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple and class objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedTuple = space.insert(person, Person.class).join().get();
   *
   * // Change tuple object
   * person = new Person(1, false, "Tuzik");
   *
   * // Use the changed tuple and class object you created earlier
   * // Wait for the result of the replace operation
   * Person replacedTuple = space.replace(person, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(Object tuple, Class<T> entity);

  /**
   * The method replaces a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">replace</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link Class} class of tuple object and {@link Options Options} parameter are passed
   * as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use the tuple, options and Class objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedPerson = space.insert(person, options, Person.class).join().get();
   *
   * // Change tuple object
   * person = new Person(1, false, "Tuzik");
   *
   * // Use the changed tuple, options and Class object you created earlier
   * // Wait for the result of the replace operation
   * Person replacedTuple = space.replace(person, options, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(Object tuple, Options options, Class<T> entity);

  /**
   * The method replaces a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">replace</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link TypeReference} object is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space id
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Use the tuple and TypeReference objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedTuple = space.insert(person, typeReference).join().get().get();
   *
   * // Change tuple object
   * person = new Person(1, false, "Tuzik");
   *
   * // Use the changed tuple and TypeReference object you created earlier
   * // Wait for the result of the replace operation
   * Person replacedTuple = space.replace(person, typeReference).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> replace(Object tuple, TypeReference<T> entity);

  /**
   * The method replaces a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">replace</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/replace/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link TypeReference} object and {@link Options Options} parameter are passed as
   * argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Use the tuple, options and TypeReference objects you created earlier
   * // Wait for the result of the insert operation
   * Person insertedTuple = space.insert(person, options, typeReference).join().get().get();
   *
   * // Change tuple object
   * person = new Person(1, false, "Tuzik");
   *
   * // Use the changed tuple, options and TypeReference object you created earlier
   * // Wait for the result of the replace operation
   * Person replacedTuple = space.replace(person, options, typeReference).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolBoxSpace} interface.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> replace(
      Object tuple, Options options, TypeReference<T> entity);

  /**
   * The method selects a tuples object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">select</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .The implementation uses standard {@link SelectOptions options}.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key for select tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the select operation
   * SelectResponse<List<Tuple<List<?>>>> selectedTuples = space.select(key).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuples are selected.
   * @return {@link CompletableFuture} object of type {@link SelectResponse} that contains
   *     collection of tuples with data and format if successful, otherwise - returns {@link
   *     CompletableFuture} with exception.
   */
  CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> select(List<?> key);

  /**
   * The method is similar to {@link #select(List)}.
   *
   * @param key a set of parts of index key of an indefinite number by that tuples are selected.
   * @return {@link CompletableFuture} object of type {@link SelectResponse} that contains
   *     collection of tuples with data and format if successful, otherwise - returns {@link
   *     CompletableFuture} with exception.
   */
  CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> select(Object... key);

  /**
   * The method selects a tuples object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">select</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link SelectOptions Options} parameter is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Choose the options you want to use
   * SelectOptions options = SelectOptions.builder()
   *                                      .withLimit(10)
   *                                      .build();
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Key
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Use list of parts of index key for select tuples from Tarantool
   * // Wait for the result of the select operation
   * SelectResponse<List<Tuple<List<?>>>> selectedTuples = space.select(key, options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuples are selected.
   * @param options {@link SelectOptions} object.
   * @return {@link CompletableFuture} object of type {@link SelectResponse} that contains
   *     collection of tuples with data and format if successful, otherwise - returns {@link
   *     CompletableFuture} with exception.
   */
  CompletableFuture<SelectResponse<List<Tuple<List<?>>>>> select(
      List<?> key, SelectOptions options);

  /**
   * The method selects a tuples object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">select</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link Class} class of tuple object is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space id
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of keys for select tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the select operation
   * List<Tuple<Person>> = space.select(key, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuples are selected.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - list of selected tuple objects
   *     represented as custom class type, otherwise - returns {@link CompletableFuture} with
   *     exception.
   */
  <T> CompletableFuture<SelectResponse<List<Tuple<T>>>> select(List<?> key, Class<T> entity);

  /**
   * The method selects a tuples object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">select</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link Class} class and {@link SelectOptions options} parameter are passed as
   * argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Choose the options you want to use
   * SelectOptions options = SelectOptions.builder()
   *                                      .withLimit(10)
   *                                      .build();
   *
   * // Use list of parts of index key for select tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the select operation
   * List<Tuple<Person>> selectedTuples = space.select(key, options, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuples are selected.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @param options {@link SelectOptions} object.
   * @return {@link CompletableFuture} object. If successful - list of selected tuple objects
   *     represented as custom class type, otherwise - returns {@link CompletableFuture} with
   *     exception.
   */
  <T> CompletableFuture<SelectResponse<List<Tuple<T>>>> select(
      List<?> key, SelectOptions options, Class<T> entity);

  /**
   * The method selects a tuples object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">select</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link TypeReference} class is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   *
   * // Use list of parts of index key for select tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Create ref
   * TypeReference<Set<Tuple<Person>>> ref = new TypeReference<Set<Tuple<Person>>>() {};
   *
   * // Wait for the result of the select operation
   * Set<Tuple<Person>> selectedTuples = space.select(key, ref).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param key list of parts of index key by which tuple is selected.
   * @param entity {@link TypeReference} type of return object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a list of selected tuples of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<SelectResponse<T>> select(List<?> key, TypeReference<T> entity);

  /**
   * The method selects a tuples object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">select</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/select/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link TypeReference} class and {@link SelectOptions options} parameter are passed as
   * argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key for select tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Choose the options you want to use
   * SelectOptions options = SelectOptions.builder()
   *                                   .withLimit(10)
   *                                   .build();
   *
   * // Create ref
   * TypeReference<Set<Tuple<Person>>> ref = new TypeReference<Set<Tuple<Person>>>() {};
   *
   * // Wait for the result of the select operation
   * Set<Tuple<Person>> selectedTuples = space.select(key, options, ref).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param key list of parts of index key by which tuple is selected.
   * @param options {@link SelectOptions} object.
   * @param entity {@link TypeReference} type of return object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a list of selected tuples of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<SelectResponse<T>> select(
      List<?> key, SelectOptions options, TypeReference<T> entity);

  /**
   * The method deletes a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">delete</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .The implementation uses standard {@link DeleteOptions options}.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key for delete tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the delete operation
   * List<?> deleteTuple = space.delete(key).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is deleted.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> delete(List<?> key);

  /**
   * The method is similar to {@link #delete(List)}.
   *
   * @param key a set of parts of index key of an indefinite number by that tuple is deleted.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> delete(Object... key);

  /**
   * The method deletes a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">delete</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link DeleteOptions Options} parameter is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Choose the options you want to use
   * DeleteOptions options = DeleteOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   *
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key for delete tuple from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the delete operation
   * List<?> deletedTuple = space.delete(key, options).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is deleted.
   * @param options {@link DeleteOptions} object.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> delete(List<?> key, DeleteOptions options);

  /**
   * The method deletes a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">delete</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link Class} class of tuple object is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key for delete tuple from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the delete operation
   * Person deletedTuple = space.delete(key, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is deleted.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(List<?> key, Class<T> entity);

  /**
   * The method deletes a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">delete</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link Class} class and {@link DeleteOptions options} parameter are passed as
   * argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Choose the options you want to use
   * DeleteOptions options = DeleteOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   *
   * // Use list of parts of index key for delete tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the delete operation
   * Person deletedTuple = space.delete(key, options, Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuples are deleted.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @param options {@link DeleteOptions} object.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(List<?> key, DeleteOptions options, Class<T> entity);

  /**
   * The method deletes a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">delete</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link TypeReference} class is passed as argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Create ref
   * TypeReference<Person> ref = new TypeReference<Person>() {};
   *
   * // Use list of parts of index key for delete tuple from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the delete operation
   * Person deletedTuple = space.delete(key, ref).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param key list of parts of index key by which tuple is deleted.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one deleted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> delete(List<?> key, TypeReference<T> entity);

  /**
   * The method deletes a tuple object like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">delete</a>
   * does.
   *
   * <p>Note: the API presented via <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/delete/">lua</a>
   * may differ from the API presented here since the API presented here uses the binary protocol
   * directly .{@link TypeReference} class and {@link DeleteOptions options} parameter are passed as
   * argument.
   *
   * <p>To use this method correctly, you can follow this example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Creating TarantoolBoxClient object see in TarantoolBoxClientImpl class.
   * // Get specific space with your space name
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Choose the options you want to use
   * DeleteOptions options = DeleteOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   *
   * // Create ref
   * TypeReference<Person> ref = new TypeReference<Person>() {};
   *
   * // Use list of parts of index key for delete tuples from Tarantool
   * List<?> key = Arrays.asList("keyPart1", "keyPart2", ...);
   *
   * // Wait for the result of the delete operation
   * Person deletedTuple = space.delete(key, options, ref).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param key list of parts of index key by which tuple is deleted.
   * @param options {@link DeleteOptions} object.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one deleted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> delete(
      List<?> key, DeleteOptions options, TypeReference<T> entity);

  /**
   * The method updates a tuple like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>The implementation uses default instance of {@link UpdateOptions options}.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key and operations for update tuple in Tarantool
   * // Wait for the result of the update operation
   * List<?> updatedTuple = space.update(Arrays.asList(1),
   *                                     Collections.singletonList(Arrays.asList("=", 2, "Kolya"))).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is updated.
   * @param operations a list of operations indicating how to update the field.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> update(List<?> key, List<List<?>> operations);

  CompletableFuture<Tuple<List<?>>> update(List<?> key, Operations operations);

  /**
   * The method updates a tuple like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Choose the options you want to use
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   *
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key and operations for update tuple in Tarantool
   * // Wait for the result of the update operation
   * List<?> updatedTuple = space.update(Arrays.asList(1),
   *                                     Collections.singletonList(Arrays.asList("=", 2, "Kolya")),
   *                                     options).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is updated.
   * @param operations a list of operations indicating how to update the field.
   * @param options {@link UpdateOptions} object.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> update(
      List<?> key, List<List<?>> operations, UpdateOptions options);

  CompletableFuture<Tuple<List<?>>> update(
      List<?> key, Operations operations, UpdateOptions options);

  /**
   * The method updates a tuple like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>The implementation uses default instance of {@link UpdateOptions options}.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key and operations for update tuples in Tarantool
   * // Wait for the result of the update operation
   * Person updatedTuple = space.update(Arrays.asList(1),
   *                                    Collections.singletonList(Arrays.asList("=", 2, "Kolya")),
   *                                    Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is updated.
   * @param operations a list of operations indicating how to update the field.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(List<?> key, List<List<?>> operations, Class<T> entity);

  <T> CompletableFuture<Tuple<T>> update(List<?> key, Operations operations, Class<T> entity);

  /**
   * The method updates a tuple like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Choose the options you want to use
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   *
   * // Use list of parts of index key and operations for update tuples in Tarantool
   * // Wait for the result of the update operation
   * Person updatedTuple = space.update(Arrays.asList(1),
   *                                    Collections.singletonList(Arrays.asList("=", 2, "Kolya")),
   *                                    options,
   *                                    Person.class).join().get();
   * }</pre>
   *
   * </blockquote>
   *
   * @param key list of parts of index key by which tuple is updated.
   * @param operations a list of operations indicating how to update the field.
   * @param options {@link UpdateOptions} object.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      List<?> key, List<List<?>> operations, UpdateOptions options, Class<T> entity);

  <T> CompletableFuture<Tuple<T>> update(
      List<?> key, Operations operations, UpdateOptions options, Class<T> entity);

  /**
   * The method updates a tuple object the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>The implementation uses default instance of {@link UpdateOptions options}.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   * TypeReference<Person> typeRef = new TypeReference<Person>() {};
   *
   * // Use list of parts of index key for update tuples from Tarantool
   * // Wait for the result of the update operation
   * Person updatedTuple = space.update(Arrays.asList(1),
   *                                    Collections.singletonList(Arrays.asList("=", 2, "Kolya")),
   *                                    typeRef).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param key list of parts of index key by which tuple is updated.
   * @param operations a list of operations indicating how to update the field.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(
      List<?> key, List<List<?>> operations, TypeReference<T> entity);

  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(
      List<?> key, Operations operations, TypeReference<T> entity);

  /**
   * The method updates a tuple object the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Choose the options you want to use
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   * TypeReference<Person> typeRef = new TypeReference<Person>() {};
   *
   * // Use list of parts of index key for update tuples from Tarantool
   * // Wait for the result of the update operation
   * Person updatedTuple = space.update(Arrays.asList(1),
   *                                    Collections.singletonList(Arrays.asList("=", 2, "Kolya")),
   *                                    options,
   *                                    typeRef).join().get().get();
   * }</pre>
   *
   * </blockquote>
   *
   * See more examples: {@link #insert(Object, TypeReference)}.
   *
   * @param key list of parts of index key by which tuple is updated.
   * @param operations a list of operations indicating how to update the field.
   * @param options {@link UpdateOptions} object.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(
      List<?> key, List<List<?>> operations, UpdateOptions options, TypeReference<T> entity);

  <T> CompletableFuture<TarantoolResponse<Tuple<T>>> update(
      List<?> key, Operations operations, UpdateOptions options, TypeReference<T> entity);

  /**
   * The method upserts a tuple like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/upsert/">upsert</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>The implementation uses default instance of {@link UpdateOptions options}.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key and operations for upsert tuples in Tarantool
   * // Wait for the result of the upsert operation
   * space.upsert(Arrays.asList(1, true, "Misha"),
   *              Collections.singletonList(Arrays.asList("=", 2, "Kolya"))).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple if such a tuple exists, then it updates based on the passed operations, otherwise
   *     it inserts the passed tuple.
   * @param operations a list of operations indicating how to update the field.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations);

  CompletableFuture<Void> upsert(Object tuple, Operations operations);

  /**
   * The method upserts a tuple like the function <a
   * href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/upsert/">upsert</a>
   * does.
   *
   * <p>Note: the Lua API may differ from this API since Java driver uses the binary protocol
   * directly.
   *
   * <p>Example:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Choose the options you want to use
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(1_000L)
   *                                      .build();
   *
   * // To create instance of TarantoolBoxClient object see TarantoolBoxClientImpl class.
   * // Get specific space
   * TarantoolBoxSpace space = boxClient.space(spaceName);
   *
   * // Use list of parts of index key and operations for upsert tuples in Tarantool
   * // Wait for the result of the upsert operation
   * space.upsert(Arrays.asList(1, true, "Misha"),
   *              Collections.singletonList(Arrays.asList("=", 2, "Kolya")),
   *              options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple if such a tuple exists, then it updates based on the passed operations, otherwise
   *     it inserts the passed tuple.
   * @param operations a list of operations indicating how to update the field. See also: <a
   *     href="https://www.tarantool.io/en/doc/latest/reference/reference_lua/box_space/update/">update</a>.
   * @param options {@link UpdateOptions} object.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations, UpdateOptions options);

  CompletableFuture<Void> upsert(Object tuple, Operations operations, UpdateOptions options);

  /**
   * Special class that contains information about spaces.
   *
   * @return {@link TarantoolSchemaFetcher} object.
   */
  TarantoolSchemaFetcher getFetcher();
}
