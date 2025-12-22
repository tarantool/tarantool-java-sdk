/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.client.tdg;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.tarantool.client.OnlyKeyValueOptions;
import io.tarantool.client.TarantoolSpace;

/**
 * <p>The interface provides a contract for operations that can be performed with the
 * <a href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#spaces">space</a>.</p>
 * <p><i><b>Note</b></i>: Classes that implement this interface work with only
 * <a href="https://www.tarantool.io/en/tdg/latest/">tdg</a> product.</p>
 * <p>Each method of this interface uses a tuple object. To create that you can follow two ways:</p>
 * <ul>
 *     <li>You can use POJO classes:
 *     <blockquote><pre>
 *
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
 *     space.put(person);
 *     }
 *     </pre></blockquote>
 *     </li>
 *     <li>You can use a Map as a tuple object, which elements are field values:
 *     <blockquote><pre>{@code
 *         // id, isMarried, name fields
 *         Map<?,?> person = Map.of(
 *                                "id", 1,
 *                                "is_married", true,
 *                                "name", "Artyom"
 *                                );
 *
 *         ...
 *
 *         // insert person tuple object
 *         space.put(person);
 *     }
 *     </pre></blockquote>
 *     </li>
 * </ul>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 * @author <a href="https://github.com/nickkkccc">Nikolay Belonogov</a>
 */
public interface TarantoolDataGridSpace extends TarantoolSpace {

  /**
   * <p>Inserts a tuple into the space.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * Map<?,?> person2 = Map.of("id", 1, "is_married", true, "name", "Petya");
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Use the tuple object you created before
   * // Wait for the result of the put operation
   * Map<?, ?> res1 = space.put(person1).join();
   * Map<?, ?> res2 = space.put(person2).join();
   * }
   * </pre></blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link TarantoolDataGridSpace}
   *              interface.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the inserted
   * tuple represented as map of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<Map<?, ?>> put(Object tuple);

  /**
   * <p>Inserts a tuple into the space with additional options.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * Map<?,?> person2 = Map.of("id", 1, "is_married", true, "name", "Petya");
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Use the tuple object you created before
   * // Wait for the result of the put operation
   * Map<?, ?> res1 = space.put(person1, options).join();
   * Map<?, ?> res2 = space.put(person2, options).join();
   * }
   * </pre></blockquote>
   *
   * @param tuple tuple object for insertion. To create tuple object class follow example in {@link TarantoolDataGridSpace}
   *              interface.
   * @param options additional options for the operation. Can be used to pass TDG-specific options such as
   *                {"skip_result": true} to skip returning the result
   * @param context additional context for the operation. Can be used to pass TDG-specific context such as
   *                authentication information or other metadata
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the inserted
   * tuple represented as map of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<Map<?, ?>> put(Object tuple, OnlyKeyValueOptions options, Map<String, Object> context);

  /**
   * <p>Retrieves a tuple from the space by its key.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * Map<?,?> person2 = Map.of("id", 1, "is_married", true, "name", "Petya");
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Use the tuple object you created before
   * // Wait for the result of the get operation
   * Map<?, ?> res1 = space.get(1).join();
   * Map<?, ?> res2 = space.get(person2).join();
   * }
   * </pre></blockquote>
   *
   * @param key key of the tuple to retrieve
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the retrieved
   * tuple represented as map of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<Map<?, ?>> get(Object key);

  /**
   * <p>Retrieves a tuple from the space by its key with additional options.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Example from interface description.
   * Person person1 = new Person(1, true, "Petya");
   * Map<?,?> person2 = Map.of("id", 1, "is_married", true, "name", "Petya");
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Use the tuple object you created before
   * // Wait for the result of the get operation
   * Map<?, ?> res1 = space.get(1, options).join();
   * Map<?, ?> res2 = space.get(person2, options).join();
   * }
   * </pre></blockquote>
   *
   * @param key key of the tuple to retrieve
   * @param options additional options for the operation
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the retrieved
   * tuple represented as map of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<Map<?, ?>> get(Object key, OnlyKeyValueOptions options);

  /**
   * <p>Updates tuples in the space matching the specified filters.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * // Update operation: set name field to "artyom"
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   * List<List<?>> updaters = Collections.singletonList(Arrays.asList("set", "name", "artyom"));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the update operation
   * List<Map<?, ?>> res = space.update(filters, updaters).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to update. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @param updaters list of update operations to apply. Each operation is a list with format:
   *                 [operation_type, field_name, value] where operation_type can be "set", "add", "sub", "mul", "div", etc.
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the updated
   * tuples represented as list of maps of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<List<Map<?, ?>>> update(List<List<?>> filters, List<List<?>> updaters);

  /**
   * <p>Updates tuples in the space matching the specified filters with additional options.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * // Update operation: set name field to "artyom"
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   * List<List<?>> updaters = Collections.singletonList(Arrays.asList("set", "name", "artyom"));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the update operation
   * List<Map<?, ?>> res = space.update(filters, updaters, options).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to update. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @param updaters list of update operations to apply. Each operation is a list with format:
   *                 [operation_type, field_name, value] where operation_type can be "set", "add", "sub", "mul", "div", etc.
   * @param options additional options for the operation
   * @param context additional context for the operation. Can be used to pass TDG-specific context such as
   *                authentication information or other metadata
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the updated
   * tuples represented as list of maps of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<List<Map<?, ?>>> update(List<List<?>> filters, List<List<?>> updaters, OnlyKeyValueOptions options, Map<String, Object> context);

  /**
   * <p>Deletes tuples from the space matching the specified filters.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the delete operation
   * List<Map<?, ?>> res = space.delete(filters).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to delete. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the deleted
   * tuples represented as list of maps of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<List<Map<?, ?>>> delete(List<List<?>> filters);

  /**
   * <p>Deletes tuples from the space matching the specified filters with additional options.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the delete operation
   * List<Map<?, ?>> res = space.delete(filters, options).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to delete. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @param options additional options for the operation
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the deleted
   * tuples represented as list of maps of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<List<Map<?, ?>>> delete(List<List<?>> filters, OnlyKeyValueOptions options);

  /**
   * <p>Finds tuples in the space matching the specified filters.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the find operation
   * List<Map<?, ?>> res = space.find(filters).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to find. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the found
   * tuples represented as list of maps of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<List<Map<?, ?>>> find(List<List<?>> filters);

  /**
   * <p>Finds tuples in the space matching the specified filters with additional options.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the find operation
   * List<Map<?, ?>> res = space.find(filters, options).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to find. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @param options additional options for the operation
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the found
   * tuples represented as list of maps of field names and values, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<List<Map<?, ?>>> find(List<List<?>> filters, OnlyKeyValueOptions options);

  /**
   * <p>Counts tuples in the space matching the specified filters.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the count operation
   * Integer res = space.count(filters).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to count. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the count
   * of matching tuples, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<Integer> count(List<List<?>> filters);

  /**
   * <p>Counts tuples in the space matching the specified filters with additional options.</p>
   * <p>Example of usage:</p>
   * <blockquote><pre>{@code
   * // Filter condition: id field equals 1
   * List<List<?>> filters = Collections.singletonList(Arrays.asList("id", "==", 1));
   *
   * // Get space by specified name
   * TarantoolDataGridSpace space = tdgClient.space(spaceName);
   *
   * // Wait for the result of the count operation
   * Integer res = space.count(filters, options).join();
   * }
   * </pre></blockquote>
   *
   * @param filters list of filter conditions to identify tuples to count. Each filter is a list with format:
   *                [field_name, operator, value] where operator can be "==", "!=", ">", "<", ">=", "<="
   * @param options additional options for the operation
   * @return {@link CompletableFuture} object. If successful - future is completed successfully with the count
   * of matching tuples, otherwise this future will be completed exceptionally by exception.
   */
  CompletableFuture<Integer> count(List<List<?>> filters, OnlyKeyValueOptions options);
}
