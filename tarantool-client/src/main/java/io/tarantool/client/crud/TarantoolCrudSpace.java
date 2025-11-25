/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.client.crud;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

import io.tarantool.client.BaseOptions;
import io.tarantool.client.Options;
import io.tarantool.client.TarantoolSpace;
import io.tarantool.client.crud.options.CountOptions;
import io.tarantool.client.crud.options.DeleteOptions;
import io.tarantool.client.crud.options.GetOptions;
import io.tarantool.client.crud.options.InsertManyOptions;
import io.tarantool.client.crud.options.InsertOptions;
import io.tarantool.client.crud.options.LenOptions;
import io.tarantool.client.crud.options.MinMaxOptions;
import io.tarantool.client.crud.options.SelectOptions;
import io.tarantool.client.crud.options.TruncateOptions;
import io.tarantool.client.crud.options.UpdateOptions;
import io.tarantool.client.crud.options.UpsertManyOptions;
import io.tarantool.client.operation.Operations;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.mapping.crud.CrudBatchResponse;
import io.tarantool.mapping.crud.CrudError;

/**
 * The interface provides a contract for operations that can be performed with the <a
 * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#spaces">space</a>.
 *
 * <p><i><b>Note</b></i>: Classes that implement this interface work with only <a
 * href="https://github.com/tarantool/crud">crud</a> module.
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
 *          public Integer id;
 *          public Boolean isMarried;
 *          public String name;
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
 *     //  An annotation that allows to convert class fields into
 *     //  an array, taking into account the order of the fields.
 *     &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
 *     {@code
 *     public class Person {
 *
 *         // First field in the data schema
 *         public Integer id;
 *
 *         // Second field in the data schema
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
public interface TarantoolCrudSpace extends TarantoolSpace {

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * // Use the tuple, options and TypeReference objects you created before
   * // Wait for the result of the insert operation
   * Person res = space.insert(options, typeReference, person, crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#insert">insert</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one inserted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple object you created before
   * // Wait for the result of the insert operation
   * List<?> res1 = space.insert(person1).join();
   * List<?> res2 = space.insert(person2).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> insert(Object tuple);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#insert(Object)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   */
  @Override
  CompletableFuture<Tuple<List<?>>> insertObject(Object tuple);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Use the tuple and options objects you created before
   * // Wait for the result of the insert operation
   * List<?> res1 = space.insert(person1, options) .join();
   * List<?> res2 = space.insert(person2, options) .join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param options {@link InsertOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> insert(Object tuple, InsertOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#insert(Object, InsertOptions)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   * @param options {@link InsertOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<Tuple<List<?>>> insertObject(Object tuple, InsertOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple and class objects you created before
   * // Wait for the result of the insert operation
   * Person res1 = space.insert(person1, Person.class).join();
   * Person res2 = space.insert(person2, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(Object tuple, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   *  // Use the tuple, options and Class objects you created before
   *  // Wait for the result of the insert operation
   *  Person res1 = space.insert(person1, options, Person.class).join();
   *  Person res2 = space.insert(person2, options, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param options {@link InsertOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one inserted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(Object tuple, InsertOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Vasya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple and TypeReference objects you created before
   * // Wait for the result of the insert operation
   * Person insertedPerson1 = space.insert(person1, typeReference).join();
   * List<?> insertedPerson2 = space.insert(person2, new TypeReference<List<?>>(){});
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one inserted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(Object tuple, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple, options and TypeReference objects you created before
   * // Wait for the result of the insert operation
   * Person res = space.insert(person, options, typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #insert(Object, TypeReference)}.
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param options {@link InsertOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one inserted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> insert(
      Object tuple, InsertOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * // Use the tuple, options and TypeReference objects you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<Person>> res = space.insertMany(options,
   *                                                  typeReference,
   *                                                  Arrays.asList(person, secondPerson),
   *                                                  crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#insert-many">insert-many</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> insertMany(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple object you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<?>> res1 = space.insertMany(Arrays.asList(person)).join();
   * CrudBatchResponse<List<?>> res2 = space.insertMany(Arrays.asList(secondPerson)).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for insertion. To create tuple object class follow example
   *     in {@link TarantoolCrudSpace} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertMany(List<?> tuples);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#insertMany(List)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuples POJOs, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Collection} of {@link java.util.Map}
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertObjectMany(List<?> tuples);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Use the tuple and options objects you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<?>> res1 = space.insertMany(Arrays.asList(person), options).join();
   * CrudBatchResponse<List<?>> res2 = space.insertMany(Arrays.asList(secondPerson), options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for insertion. To create tuple object class follow example
   *     in {@link TarantoolCrudSpace} interface.
   * @param options {@link InsertManyOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertMany(
      List<?> tuples, InsertManyOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#insertMany(List, InsertManyOptions)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuples POJOs, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Collection} of {@link java.util.Map}
   * @param options {@link InsertManyOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> insertObjectMany(
      List<?> tuples, InsertManyOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * List<?> person = Arrays.asList(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple and class objects you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<Person>> res = space.insertMany(Arrays.asList(person, secondPerson), Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for insertion. To create tuple object class follow example
   *     in {@link TarantoolCrudSpace} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> insertMany(
      List<?> tuples, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * List<?> person = Arrays.asList(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple, options and Class objects you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<Person>> res = space.insertMany(Arrays.asList(person, secondPerson),
   *                                                  options,
   *                                                  Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for insertion. To create tuple object class follow example
   *     in {@link TarantoolCrudSpace} interface.
   * @param options {@link InsertManyOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> insertMany(
      List<?> tuples, InsertManyOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Petya");
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // Use the tuple and TypeReference objects you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<Person>> insertedPerson = space.insertMany(Arrays.asList(person), typeReference).join();
   * CrudBatchResponse<List<?>> insertedPerson = space.insertMany(Arrays.asList(secondPerson), new TypeReference<List<?>>(){});
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for insertion. To create tuple object class follow example
   *     in {@link TarantoolCrudSpace} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> insertMany(
      List<?> tuples, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * // Use the tuple, options and TypeReference objects you created before
   * // Wait for the result of the insert operation
   * CrudBatchResponse<List<Person>> res = space.insertMany(Arrays.asList(person, secondPerson),
   *                                                  options,
   *                                                  typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #insertMany(List, TypeReference)}.
   *
   * @param tuples list of tuple objects for insertion. To create tuple object class follow example
   *     in {@link TarantoolCrudSpace} interface.
   * @param options {@link InsertManyOptions option} object implementing base {@link Options
   *     options} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> insertMany(
      List<?> tuples, InsertManyOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insertMany(Person.class, Arrays.asList(person, secondPerson), crudOptions).join();
   * List<?> newPersons = Arrays.asList(new Person(1, true, "Fedya"), new Person(2, true, "Petya"));
   *
   * CrudBatchResponse<List<Person>> res = space.replaceMany(options, typeReference, newPersons, crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#replace-many">replace-many</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> replaceMany(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person)).join();
   * space.insertMany(Arrays.asList(secondPerson)).join();
   *
   * List<?> newPersons = Arrays.asList(Arrays.asList(1, true, "Fedya"), Arrays.asList(2, true, "Petya"));
   * CrudBatchResponse<List<?>> res = space.replaceMany(newPersons).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for replace. To create tuple object class follow example in
   *     {@link TarantoolCrudSpace} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceMany(List<?> tuples);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#replaceMany(List)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuples POJOs, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Collection} of {@link java.util.Map}
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceObjectMany(List<?> tuples);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insertMany(Arrays.asList(person)).join();
   * space.insertMany(Arrays.asList(secondPerson)).join();
   *
   * List<?> newPersons = Arrays.asList(new Person(1, true, "Fedya"), new Person(2, true, "Petya"));
   *
   * CrudBatchResponse<List<?>> res = space.replaceMany(newPersons, options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for replace. To create tuple object class follow example in
   *     {@link TarantoolCrudSpace} interface.
   * @param options {@link InsertManyOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceMany(
      List<?> tuples, InsertManyOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#replaceMany(List, InsertManyOptions)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuples POJOs, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Collection} of {@link java.util.Map}
   * @param options {@link InsertManyOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<CrudBatchResponse<List<Tuple<List<?>>>>> replaceObjectMany(
      List<?> tuples, InsertManyOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person)).join();
   * space.insertMany(Arrays.asList(secondPerson)).join();
   *
   * List<?> newPersons = Arrays.asList(new Person(1, true, "Fedya"), new Person(2, true, "Petya"));
   *
   * CrudBatchResponse<List<Person>> res = space.replaceMany(newPersons, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for replace. To create tuple object class follow example in
   *     {@link TarantoolCrudSpace} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> replaceMany(
      List<?> tuples, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person)).join();
   * space.insertMany(Arrays.asList(secondPerson)).join();
   *
   * List<?> newPersons = Arrays.asList(new Person(1, true, "Fedya"), new Person(2, true, "Petya"));
   *
   * CrudBatchResponse<List<Person>> res = space.replaceMany(newPersons, options, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for replace. To create tuple object class follow example in
   *     {@link TarantoolCrudSpace} interface.
   * @param options {@link InsertManyOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<CrudBatchResponse<List<Tuple<T>>>> replaceMany(
      List<?> tuples, InsertManyOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> newPersons1 = Arrays.asList(new Person(1, true, "Fedya"), new Person(2, true, "Petya"));
   * List<?> newPersons2 = Arrays.asList(Arrays.asList(1, true, "Fedya"), Arrays.asList(2, true, "Petya"));
   *
   * CrudBatchResponse<List<Person>> res1 = space.replaceMany(newPersons1, typeReference).join();
   * CrudBatchResponse<List<?>> res2 = space.replaceMany(newPersons2, new TypeReference<List<?>>(){}).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuples list of tuple objects for replace. To create tuple object class follow example in
   *     {@link TarantoolCrudSpace} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> replaceMany(
      List<?> tuples, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> newPersons = Arrays.asList(new Person(1, true, "Fedya"), new Person(2, true, "Petya"));
   *
   * CrudBatchResponse<List<Person>> res = space.replaceMany(newPersons, options, typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #replaceMany(List, TypeReference)}.
   *
   * @param tuples list of tuple objects for replace. To create tuple object class follow example in
   *     {@link TarantoolCrudSpace} interface.
   * @param options {@link InsertManyOptions option} object implementing base {@link Options
   *     options} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     {@link CrudBatchResponse}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  <T> CompletableFuture<TarantoolResponse<CrudBatchResponse<T>>> replaceMany(
      List<?> tuples, InsertManyOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insertMany(Arrays.asList(person), options).join();
   * space.insertMany(Arrays.asList(secondPerson), options).join();
   *
   * List<List<String>> operation = Collections.singletonList(Arrays.asList("=", "name", "Peter 3"));
   * List<List<List<?>>> tuples1 = Arrays.asList(Arrays.asList(person.asList(), operation),
   *                                             Arrays.asList((new Person(3, false, "Nick")).asList(), operation));
   * List<List<List<?>>> tuples2 = Arrays.asList(Arrays.asList(Arrays.asList(1, true, "Petya"), operation),
   *                                             Arrays.asList(Arrays.asList(40, false, "Nick"), operation));
   *
   * List<CrudError> res1 = space.upsertMany(options, tuples1, crudOptions).join();
   * List<CrudError> res2 = space.upsertMany(options, tuples2, crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#upsert-many">upsert-many</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of {@link CrudError}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<List<CrudError>> upsertMany(Options options, Object... arguments);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsertMany(Options, Object...)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#upsert-many">upsert-many</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   */
  CompletableFuture<List<CrudError>> upsertObjectMany(Options options, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, true, "Kolya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person), options).join();
   * space.insertMany(Arrays.asList(secondPerson), options).join();
   *
   * List<List<String>> operation = Collections.singletonList(Arrays.asList("=", "name", "Peter 3"));
   * List<List<List<?>>> tuples1 = Arrays.asList(Arrays.asList(person.asList(), operation),
   *                                            Arrays.asList((new Person(3, false, "Nick")).asList(), operation));
   * List<List<List<?>>> tuples2 = Arrays.asList(Arrays.asList(Arrays.asList(1, true, "Petya"), operation),
   *                                            Arrays.asList(Arrays.asList(40, false, "Nick"), operation));
   *
   * List<CrudError> res1 = space.upsertMany(tuples1).join();
   * List<CrudError> res2 = space.upsertMany(tuples2).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuplesOperationData list of tuples with operations for upsert operation.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of {@link CrudError}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<List<CrudError>> upsertMany(List<?> tuplesOperationData);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsertMany(List)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuplesOperationData list of tuples with operations for upsert operation.
   */
  CompletableFuture<List<CrudError>> upsertObjectMany(List<?> tuplesOperationData);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * Person firstPerson = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   * space.insertMany(Arrays.asList(person, secondPerson), options).join();
   *
   * UpsertBatch batch = UpsertBatch.create()
   *     .add(firstPerson, Operations.create().set("name", "Peter"))
   *     .add(secondPerson, Operations.create().set("name", "Nick"));
   *
   * List<CrudError> res = space.upsertMany(batch).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param batch list of upsert pairs where each pair consists of tuple and list of update
   *     operations.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of {@link CrudError}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<List<CrudError>> upsertMany(UpsertBatch batch);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsertMany(UpsertBatch)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param batch list of upsert pairs where each pair consists of tuple and list of update
   *     operations.
   */
  CompletableFuture<List<CrudError>> upsertObjectMany(UpsertBatch batch);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * UpsertManyOptions options = UpsertManyOptions.builder()
   *                                              .withTimeout(3_000L)
   *                                              .build();
   *
   * space.insertMany(Arrays.asList(person, secondPerson), options).join();
   *
   * List<List<String>> operation = Collections.singletonList(Arrays.asList("=", "name", "Peter 3"));
   * List<List<List<?>>> tuples1 = Arrays.asList(Arrays.asList(person.asList(), operation),
   *                                            Arrays.asList((new Person(3, false, "Nick")).asList(), operation));
   * List<List<List<?>>> tuples2 = Arrays.asList(
   *                     Arrays.asList(Arrays.asList(1, true, "Petya"), operation),
   *                     Arrays.asList(Arrays.asList(40, false, "Nick"), operation)
   *
   * List<CrudError> res1 = space.upsertMany(tuples, options).join();
   * List<CrudError> res2 = space.upsertMany(tuples, options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuplesOperationData list of tuples with operations for upsert operation.
   * @param options {@link UpsertManyOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of {@link CrudError}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<List<CrudError>> upsertMany(
      List<?> tuplesOperationData, UpsertManyOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsertMany(List, UpsertManyOptions)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuplesOperationData list of tuples with operations for upsert operation.
   * @param options {@link UpsertManyOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<List<CrudError>> upsertObjectMany(
      List<?> tuplesOperationData, UpsertManyOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * Person firstPerson = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, true, "Kolya");
   *
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   * space.insertMany(Arrays.asList(person, secondPerson), options).join();
   *
   * UpsertManyOptions options = UpsertManyOptions.builder()
   *                                              .withTimeout(3_000L)
   *                                              .build();
   * UpsertBatch batch = UpsertBatch.create()
   *     .add(firstPerson.asList(), Operations.create().set("name", "Peter"))
   *     .add(secondPerson.asList(), Operations.create().set("name", "Nick"));
   *
   * List<CrudError> res = space.upsertMany(batch, options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param batch list of upsert pairs where each pair consists of tuple and list of update
   *     operations.
   * @param options {@link UpsertManyOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of {@link CrudError}, otherwise this future will be completed exceptionally by
   *     exception.
   */
  CompletableFuture<List<CrudError>> upsertMany(UpsertBatch batch, UpsertManyOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsertMany(UpsertBatch,
   * UpsertManyOptions)}. <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param batch list of upsert pairs where each pair consists of tuple and list of update
   *     operations.
   * @param options {@link UpsertManyOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<List<CrudError>> upsertObjectMany(UpsertBatch batch, UpsertManyOptions options);

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *      put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * Person res = space.replace(options,
   *                            typeReference,
   *                            new Person(1, true, "Vasya"),
   *                            crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#replace">replace</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person1).join();
   * space.insert(person2).join();
   *
   * List<?> res1 = space.replace(new Person(1, true, "Vasya")).join();
   * List<?> res2 = space.replace(Arrays.asList(1, true, "Vasya")).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> replace(Object tuple);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#replace(Object)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   */
  @Override
  CompletableFuture<Tuple<List<?>>> replaceObject(Object tuple);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person1).join();
   * space.insert(person2).join();
   *
   * List<?> res1 = space.replace(new Person(1, true, "Vasya"), options).join();
   * List<?> res2 = space.replace(Arrays.asList(1, true, "Vasya"), options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param options {@link InsertOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> replace(Object tuple, InsertOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#replace(Object, InsertOptions)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   * @param options {@link InsertOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<Tuple<List<?>>> replaceObject(Object tuple, InsertOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person1).join();
   * space.insert(person2).join();
   *
   * Person res1 = space.replace(new Person(1, true, "Vasya"), Person.class).join();
   * Person res2 = space.replace(Arrays.asList(1, true, "Vasya"), Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(Object tuple, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * // For example this timeout
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person1).join();
   * space.insert(person2).join();
   *
   * Person res1 = space.replace(new Person(1, true, "Vasya"), options, Person.class).join();
   * Person res2 = space.replace(Arrays.asList(1, true, "Vasya"), options, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param options {@link InsertOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one replaced tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(Object tuple, InsertOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Vasya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person1).join();
   * space.insert(person2).join();
   *
   * Person res = space.replace(new Person(1, true, "Vasya"), typeReference).join();
   * List<?> replacedPerson = space.replace(person2, new TypeReference<List<?>>(){});
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(Object tuple, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * InsertOptions options = InsertOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * Person res = space.replace(new Person(1, true, "Vasya"), options, typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #replace(Object, TypeReference)}.
   *
   * @param tuple tuple object for replace. To create tuple object class follow example in {@link
   *     TarantoolCrudSpace} interface.
   * @param options {@link InsertOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one replaced tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> replace(
      Object tuple, InsertOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * List<Person> res = space.select(options,
   *                                 typeReference,
   *                                 Collections.singletonList(Arrays.asList("=", "name", "Petya")),
   *                                 crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#select">select</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the construct specified in the {@link
   *     TypeReference} type, otherwise - returns {@link CompletableFuture} with exception.
   */
  <T> CompletableFuture<TarantoolResponse<T>> select(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   * List<List<?>> res = space.select(cond).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list with selected tuples represented as list of values, otherwise this future will be
   *     completed exceptionally by exception.
   */
  CompletableFuture<List<Tuple<List<?>>>> select(List<Condition> conditions);

  /**
   * Same as {@link #select(List)}.
   *
   * @param conditions list of {@link Condition} objects.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list with selected tuples represented as list of values, otherwise this future will be
   *     completed exceptionally by exception.
   */
  CompletableFuture<List<Tuple<List<?>>>> select(Condition... conditions);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * SelectOptions options = SelectOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * List<List<?>> res = space.select(cond, options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @param options {@link SelectOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list with selected tuples represented as list of values, otherwise this future will be
   *     completed exceptionally by exception.
   */
  CompletableFuture<List<Tuple<List<?>>>> select(List<Condition> conditions, SelectOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * List<Person> res = space.select(cond, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list with selected tuples represented as list of values, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<List<Tuple<T>>> select(List<Condition> conditions, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * SelectOptions options = SelectOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * List<Person> res = space.select(cond, options, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @param options {@link SelectOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list with selected tuples represented as list of values, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<List<Tuple<T>>> select(
      List<Condition> conditions, SelectOptions options, Class<T> entity);

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * List<Person> res = space.select(cond, typeReference).join();
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
   * List<?> person = Arrays.asList(1, true, "Petya");
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * List<List<?>> res = space.select(cond, new TypeReference<List<List<?>>>(){}).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @param entity {@link TypeReference} type of return object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the construct specified in the {@link
   *     TypeReference} type, otherwise - returns {@link CompletableFuture} with exception.
   */
  <T> CompletableFuture<TarantoolResponse<T>> select(
      List<Condition> conditions, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * SelectOptions options = SelectOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<List<Person>> typeReference = new TypeReference<List<Person>>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * List<Person> res = space.select(cond, options, typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #select(List, TypeReference)}.
   *
   * @param conditions list of {@link Condition} objects.
   * @param options {@link SelectOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} type of return object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the construct specified in the {@link
   *     TypeReference} type, otherwise - returns {@link CompletableFuture} with exception.
   */
  <T> CompletableFuture<TarantoolResponse<T>> select(
      List<Condition> conditions, SelectOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * Person res = space.get(options,
   *                        typeReference,
   *                        Collections.singletonList(1),
   *                        crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#get">get</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one got tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> get(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *      public Integer id;
   * };
   *
   * ...
   *
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.get(Collections.singletonList(1)).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key.
   * List<?> res = space.get(new PersonKey(1)).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of one element with got tuple represented as list of values, otherwise this future
   *     will be completed exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> get(Object key);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *      public Integer id;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * GetOptions options = GetOptions.builder()
   *                                .withTimeout(3_000L)
   *                                .build();
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.get(Collections.singletonList(1), options).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key.
   * List<?> res = space.get(new PersonKey(1), options).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param options {@link GetOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     list of one element with got tuple represented as list of values, otherwise this future
   *     will be completed exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> get(Object key, GetOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *      public Integer id;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.get(Collections.singletonList(1), Person.class).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key.
   * Person res = space.get(new PersonKey(1), Person.class).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one got tuple represented as custom class type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> get(Object key, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *      public Integer id;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * GetOptions options = GetOptions.builder()
   *                                .withTimeout(3_000L)
   *                                .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.get(Collections.singletonList(1), options, Person.class).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key.
   * Person res = space.get(new PersonKey(1), options, Person.class).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param options {@link GetOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one got tuple represented as custom class type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> get(Object key, GetOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *      public Integer id;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> key = Collections.singletonList(1);
   * Person res1 = space.get(key, typeReference).join();
   * List<?> res2 = space.get(key, new TypeReference<List<?>>(){}).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key.
   * PersonKey key = new PersonKey(1);
   * Person res1 = space.get(key, typeReference).join();
   * List<?> res2 = space.get(key, new TypeReference<List<?>>(){}).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one got tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> get(Object key, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *      public Integer id;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * GetOptions options = GetOptions.builder()
   *                                .withTimeout(3_000L)
   *                                .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.get(Collections.singletonList(1), options, typeReference).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key.
   * Person res = space.get(new PersonKey(1), options, typeReference).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * See more example {@link #get(Object, TypeReference)}.
   *
   * @param key key object (can be compound).
   * @param options {@link GetOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one got tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> get(Object key, GetOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * Person res = space.delete(options,
   *                           typeReference,
   *                           Collections.singletonList(1),
   *                           crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#delete">delete</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one deleted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.delete(Collections.singletonList(1)).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * List<?> res = space.delete(new PersonKey(1)).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  @Override
  CompletableFuture<Tuple<List<?>>> delete(Object key);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * DeleteOptions options = DeleteOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.delete(Collections.singletonList(1), options).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * List<?> res = space.delete(new PersonKey(1), options).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param options {@link DeleteOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> delete(Object key, DeleteOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.delete(Collections.singletonList(1), Person.class).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.delete(new PersonKey(1), Person.class).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(Object key, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * DeleteOptions options = DeleteOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.delete(Collections.singletonList(1), options, Person.class).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.delete(new PersonKey(1), options, Person.class).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param options {@link DeleteOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one deleted tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(Object key, DeleteOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.delete(Collections.singletonList(1), typeReference).join();
   * List<?> res = space.delete(Collections.singletonList(1), new TypeReference<List<?>>(){}).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.delete(new PersonKey(1), typeReference).join();
   * List<?> res = space.delete(new PersonKey(1), new TypeReference<List<?>>(){}).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one deleted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(Object key, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * DeleteOptions options = DeleteOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.delete(Collections.singletonList(1), options, typeReference).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.delete(new PersonKey(1), options, typeReference).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * See more example {@link #delete(Object, TypeReference)}.
   *
   * @param key key object (can be compound).
   * @param options {@link DeleteOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one deleted tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> delete(
      Object key, DeleteOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
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
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.min(options, typeReference, "pk", crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#min-and-max">min</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with minimum tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> min(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person secondPerson = new Person(2, false, "Kostya");
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> res = space.min("pk").join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     minimum tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> min(String indexName);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * MinMaxOptions options = MinMaxOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> res = space.min("pk", options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param options {@link MinMaxOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     minimum tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> min(String indexName, MinMaxOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.min("pk", Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     minimum tuple represented as custom class type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> min(String indexName, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // For example this timeout
   * // Create options object
   * MinMaxOptions options = MinMaxOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.min("pk", options, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param options {@link MinMaxOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     minimum tuple represented as custom class type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> min(String indexName, MinMaxOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * List<?> person2 = Arrays.asList(1, true, "Petya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person1)).join();
   * space.insertMany(Arrays.asList(person2)).join();
   *
   * Person res1 = space.min("pk", typeReference).join();
   * List<?> res2 = space.min("pk", new TypeReference<List<?>>(){}).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with minimum tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> min(String indexName, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // For example this timeout
   * // Create options object
   * MinMaxOptions options = MinMaxOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.min("pk", typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #min(String, TypeReference)}.
   *
   * @param indexName index name.
   * @param options {@link MinMaxOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with minimum tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> min(
      String indexName, MinMaxOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
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
   * TarantoolCrudSpace space  = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.max(options, typeReference, "pk", crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#min-and-max">max</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with maximum tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> max(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> res = space.max("pk").join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     maximum tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> max(String indexName);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * MinMaxOptions options = MinMaxOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> res = space.max("pk", options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param options {@link MinMaxOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     maximum tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> max(String indexName, MinMaxOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.max("pk", Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     maximum tuple represented as custom class type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> max(String indexName, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // For example this timeout
   * // Create options object
   * MinMaxOptions options = MinMaxOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.max("pk", options, Person.class).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param options {@link MinMaxOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     maximum tuple represented as custom class type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> max(String indexName, MinMaxOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.max("pk", typeReference).join();
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
   * List<?> person = Arrays.asList(1, true, "Petya");
   * List<?> secondPerson = Arrays.asList(2, false, "Kostya");
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * List<?> res = space.max("pk", new TypeReference<List<?>>(){}).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param indexName index name.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with maximum tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> max(String indexName, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   * Person secondPerson = new Person(2, false, "Kostya");
   *
   * // For example this timeout
   * // Create options object
   * MinMaxOptions options = MinMaxOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insertMany(Arrays.asList(person, secondPerson)).join();
   *
   * Person res = space.max("pk", typeReference).join();
   * }</pre>
   *
   * </blockquote>
   *
   * See more example {@link #max(String, TypeReference)}.
   *
   * @param indexName index name.
   * @param options {@link MinMaxOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with maximum tuple of the specified Java type, otherwise this future will be completed
   *     exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> max(
      String indexName, MinMaxOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
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
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * Person res = space.update(options,
   *                           typeReference,
   *                           Collections.singletonList(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#update">update</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Options options, TypeReference<T> entity, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.update(Collections.singletonList(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya"))).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * List<?> res = space.update(new PersonKey(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya"))).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  @Override
  CompletableFuture<Tuple<List<?>>> update(Object key, List<List<?>> operations);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.update(
   *     Collections.singletonList(1),
   *     Operations.create().set("name", "Kostya")
   * ).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * List<?> res = space.update(
   *     new PersonKey(1),
   *     Operations.create().set("name", "Kostya")
   * ).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> update(Object key, Operations operations);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.update(Collections.singletonList(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                            options).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * List<?> res = space.update(new PersonKey(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                            options).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> update(
      Object key, List<List<?>> operations, UpdateOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * List<?> res = space.update(
   *     Collections.singletonList(1),
   *     Operations.create().set("name", "Kostya"),
   *     options
   * ).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * List<?> res = space.update(
   *     new PersonKey(1),
   *     Operations.create().set("name", "Kostya"),
   *     options
   * ).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as list of values, otherwise this future will be completed
   *     exceptionally by exception.
   */
  CompletableFuture<Tuple<List<?>>> update(
      Object key, Operations operations, UpdateOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.update(Collections.singletonList(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           Person.class).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.update(new PersonKey(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           Person.class).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(Object key, List<List<?>> operations, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.update(
   *     Collections.singletonList(1),
   *     Operations.create().set("name", "Kostya"),
   *     Person.class
   * ).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.update(
   *     new PersonKey(1),
   *     Operations.create().set("name", "Kostya"),
   *     Person.class
   * ).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(Object key, Operations operations, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.update(Collections.singletonList(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           options,
   *                           Person.class).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.update(new PersonKey(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           options,
   *                           Person.class).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, UpdateOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.update(
   *     Collections.singletonList(1),
   *     Operations.create().set("name", "Kostya"),
   *     options,
   *     Person.class
   * ).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.update(
   *     new PersonKey(1),
   *     Operations.create().set("name", "Kostya"),
   *     options,
   *     Person.class
   * ).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   * @param entity {@link Class} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with
   *     a one updated tuple represented as custom class type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, UpdateOptions options, Class<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res1 = space.update(Collections.singletonList(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           typeReference).join();
   * List<?> res2 = space.update(Collections.singletonList(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                            new TypeReference<List<?>>(){}).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res1 = space.update(new PersonKey(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           typeReference).join();
   * List<?> res2 = space.update(new PersonKey(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                            new TypeReference<List<?>>(){}).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person1).join();
   *
   * // You can use composite key as list
   * Person res1 = space.update(Collections.singletonList(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                            typeReference).join();
   * List<?> res2 = space.update(Collections.singletonList(1),
   *                             Operations.create().set("name", "Kostya"),
   *                             new TypeReference<List<?>>(){}).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res1 = space.update(new PersonKey(1),
   *                            Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                            typeReference).join();
   * List<?> res2 = space.update(new PersonKey(1),
   *                             Operations.create().set("name", "Kostya"),
   *                             new TypeReference<List<?>>(){}).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * // Create options object
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.update(Collections.singletonList(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           options,
   *                           typeReference).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.update(new PersonKey(1),
   *                           Collections.singletonList(Arrays.asList("=", "name", "Kostya")),
   *                           options,
   *                           typeReference).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * See more example {@link #update(Object, List, TypeReference)}.
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param options {@link DeleteOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Object key, List<List<?>> operations, UpdateOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>
   * &#64;JsonFormat(shape = JsonFormat.Shape.ARRAY)
   * {@code
   * public class PersonKey {
   *     public Integer key;
   * };
   *
   * ...
   *
   * Person person = new Person(1, true, "Petya");
   *
   * // For example this timeout
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * TypeReference<Person> typeReference = new TypeReference<Person>(){};
   *
   * // Get specific space
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * // You can use composite key as list
   * Person res = space.update(Collections.singletonList(1),
   *                           Operations.create().set("name", "Kostya"),
   *                           options,
   *                           typeReference).join();
   *
   * ...
   *
   * // You can use a user class annotated as the PersonKey class for a composite key
   * Person res = space.update(new PersonKey(1),
   *                           Operations.create().set("name", "Kostya"),
   *                           options,
   *                           typeReference).join();
   * }
   * </pre>
   *
   * </blockquote>
   *
   * See more example {@link #update(Object, List, TypeReference)}.
   *
   * @param key key object (can be compound).
   * @param operations list of update operations.
   * @param options {@link DeleteOptions option} object implementing base {@link Options options}
   *     interface.
   * @param entity {@link TypeReference} class of tuple object.
   * @param <T> tuple object class type.
   * @return {@link CompletableFuture} object. If successful - the future is completed successfully
   *     with a one updated tuple of the specified Java type, otherwise this future will be
   *     completed exceptionally by exception.
   */
  <T> CompletableFuture<Tuple<T>> update(
      Object key, Operations operations, UpdateOptions options, TypeReference<T> entity);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * space.upsert(options,
   *              Arrays.asList(1, true, "Misha"),
   *              Collections.singletonList(Arrays.asList("=", "name", "Kolya"))).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#upsert">upsert</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Options options, Object... arguments);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsert(Options, Object...)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#upsert">upsert</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   */
  CompletableFuture<Void> upsertObject(Options options, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * space.upsert(Arrays.asList(1, true, "Petya"),
   *              Collections.singletonList(Arrays.asList("=", "name", "Kolya"))).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple if such a tuple exists, then it updates based on the passed operations, otherwise
   *     it inserts the passed tuple.
   * @param operations list of update operations.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsert(Object, List)}. <b>Note:</b>
   * serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   * @param operations list of update operations.
   */
  CompletableFuture<Void> upsertObject(Object tuple, List<List<?>> operations);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * space.upsert(
   *     Arrays.asList(1, true, "Petya"),
   *     Operations.create().set("name", "Kolya")
   * ).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple if such a tuple exists, then it updates based on the passed operations, otherwise
   *     it inserts the passed tuple.
   * @param operations list of update operations.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Object tuple, Operations operations);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsert(Object, Operations)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   * @param operations list of update operations.
   */
  CompletableFuture<Void> upsertObject(Object tuple, Operations operations);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person).join();
   *
   * space.upsert(Arrays.asList(1, true, "Petya"),
   *              Collections.singletonList(Arrays.asList("=", "name", "Kolya")),
   *              options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple if such a tuple exists, then it updates based on the passed operations, otherwise
   *     it inserts the passed tuple.
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Object tuple, List<List<?>> operations, UpdateOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsert(Object, List, UpdateOptions)}.
   * <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<Void> upsertObject(
      Object tuple, List<List<?>> operations, UpdateOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * UpdateOptions options = UpdateOptions.builder()
   *                                      .withTimeout(3_000L)
   *                                      .build();
   *
   * space.insert(person).join();
   *
   * space.upsert(
   *     Arrays.asList(1, true, "Petya"),
   *     Operations.create().set("name", "Kolya")
   *     options
   * ).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param tuple if such a tuple exists, then it updates based on the passed operations, otherwise
   *     it inserts the passed tuple.
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - {@link Void}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Void> upsert(Object tuple, Operations operations, UpdateOptions options);

  /**
   * The method works similarly to {@link TarantoolCrudSpace#upsert(Object, Operations,
   * UpdateOptions)}. <b>Note:</b> serializable object must be serializable as {@code MP_MAP}
   *
   * @param tuple POJO, which can be serialized by Jackson as {@code MP_MAP} or {@link
   *     java.util.Map}
   * @param operations list of update operations.
   * @param options {@link UpdateOptions} object implementing base {@link Options} interface.
   */
  CompletableFuture<Void> upsertObject(Object tuple, Operations operations, UpdateOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * boolean res = space.truncate(options, crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#truncate">truncate</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - {@link Boolean}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Boolean> truncate(Options options, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * boolean res = space.truncate().join();
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link CompletableFuture} object. If successful - {@link Boolean}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Boolean> truncate();

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * TruncateOptions options = TruncateOptions.builder()
   *                                          .withTimeout(3_000L)
   *                                          .build();
   *
   * space.insert(person).join();
   *
   * boolean res = space.truncate(options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param options {@link TruncateOptions} object.
   * @return {@link CompletableFuture} object. If successful - {@link Boolean}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Boolean> truncate(TruncateOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * int res = space.len(options, crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#len">len</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> len(Options options, Object... arguments);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * int res = space.len().join();
   * }</pre>
   *
   * </blockquote>
   *
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> len();

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * LenOptions options = LenOptions.builder()
   *                                .withTimeout(3_000L)
   *                                .build();
   *
   * space.insert(person).join();
   *
   * boolean res = space.len(options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param options {@link LenOptions} object.
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> len(LenOptions options);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create BaseOptions object
   * Options options = BaseOptions.builder()
   *                              .withTimeout(3_000L)
   *                              .build();
   *
   * Map<String, Object> crudOptions = new HashMap<String, Object>(){{
   *     put("timeout", 2_000L);
   * }};
   *
   * space.insert(person).join();
   *
   * int res = space.count(options,
   *                      Collections.singletonList(Arrays.asList("==", "name", "Petya")),
   *                      crudOptions).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param arguments arguments as in a function <a
   *     href="https://github.com/tarantool/crud#count">count</a> except space name.
   * @param options {@link BaseOptions option} object implementing base {@link Options options}
   *     interface.
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> count(Options options, Object... arguments);

  /**
   * Same as {@link #count(List)}.
   *
   * @param conditions list of {@link Condition} objects.
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> count(Condition... conditions);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get space by specified name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   * int res = space.count(cond).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> count(List<Condition> conditions);

  /**
   * Example of usage:
   *
   * <blockquote>
   *
   * <pre>{@code
   * // Example from interface description.
   * Person person = new Person(1, true, "Petya");
   *
   * // Get specific space with your space name
   * TarantoolCrudSpace space = crudClient.space(spaceName);
   *
   * // For example this timeout
   * // Create option object
   * CountOptions options = CountOptions.builder()
   *                                    .withTimeout(3_000L)
   *                                    .build();
   *
   * space.insert(person).join();
   *
   * List<Condition> cond = Collections.singletonList(Condition.builder()
   *                                                           .withOperator("==")
   *                                                           .withFieldIdentifier("name")
   *                                                           .withValue("Petya")
   *                                                           .build());
   *
   * int res = space.count(cond, options).join();
   * }</pre>
   *
   * </blockquote>
   *
   * @param conditions list of {@link Condition} objects.
   * @param options {@link CountOptions} object implementing base {@link Options} interface.
   * @return {@link CompletableFuture} object. If successful - {@link Integer}, otherwise - returns
   *     {@link CompletableFuture} with exception.
   */
  CompletableFuture<Integer> count(List<Condition> conditions, CountOptions options);
}
