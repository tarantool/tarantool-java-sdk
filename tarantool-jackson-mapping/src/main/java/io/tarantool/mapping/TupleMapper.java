/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;

import static io.tarantool.mapping.BaseTarantoolJacksonMapping.objectMapper;

/**
 * Utility class for mapping Tarantool tuples to POJOs using field format metadata.
 *
 * <p>This class provides convenient methods to convert flat tuple arrays (List) into structured
 * POJO objects using the field format information from Tarantool space schema or CRUD response
 * metadata.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Using with CRUD response
 * CrudResponse<List<List<?>>> response = client.call(
 *     "crud.select", args, new TypeReference<CrudResponse<List<List<?>>>>() {}
 * ).join().get();
 *
 * List<Person> persons = TupleMapper.mapToPojoList(
 *     response.getRows(),
 *     response.getMetadata(),
 *     Person.class
 * );
 *
 * // Using with Box API and Tuple response
 * List<Tuple<List<?>>> tuples = space.select(Arrays.asList(1)).join().get();
 * List<Person> persons = TupleMapper.mapToPojoList(tuples, Person.class);
 * }</pre>
 *
 * @author <a href="https://github.com/ArtDu">Artyom Dubinin</a>
 */
public final class TupleMapper {

  private TupleMapper() {
    // utility class
  }

  /**
   * Converts a flat tuple list to a map using field format metadata.
   *
   * <p>The method creates a map where keys are field names from the format and values are
   * corresponding elements from the tuple list.
   *
   * @param tuple the flat tuple as a list of values
   * @param format the field format metadata with field names
   * @return a map with field names as keys and tuple values as values
   * @throws IllegalArgumentException if tuple and format sizes don't match
   */
  public static Map<String, Object> toMap(List<?> tuple, List<Field> format) {
    if (tuple == null || format == null) {
      return new HashMap<>();
    }

    if (tuple.size() != format.size()) {
      throw new IllegalArgumentException(
          String.format(
              "Tuple size (%d) doesn't match format size (%d)", tuple.size(), format.size()));
    }

    return IntStream.range(0, tuple.size())
        .boxed()
        .collect(
            Collectors.toMap(i -> format.get(i).getName(), tuple::get, (a, b) -> a, HashMap::new));
  }

  /**
   * Converts a map to a POJO using Jackson object mapper.
   *
   * @param map the map with field names and values
   * @param entityClass the target POJO class
   * @param <T> the type of the target POJO
   * @return the mapped POJO object
   */
  public static <T> T mapToPojo(Map<String, Object> map, Class<T> entityClass) {
    return objectMapper.convertValue(map, entityClass);
  }

  /**
   * Converts a map to a POJO using Jackson object mapper with TypeReference.
   *
   * @param map the map with field names and values
   * @param typeReference the target type reference
   * @param <T> the type of the target POJO
   * @return the mapped POJO object
   */
  public static <T> T mapToPojo(Map<String, Object> map, TypeReference<T> typeReference) {
    return objectMapper.convertValue(map, typeReference);
  }

  /**
   * Converts a flat tuple list directly to a POJO using format metadata.
   *
   * <p>This is a convenience method that combines {@link #toMap(List, List)} and {@link
   * #mapToPojo(Map, Class)} into a single operation.
   *
   * @param tuple the flat tuple as a list of values
   * @param format the field format metadata with field names
   * @param entityClass the target POJO class
   * @param <T> the type of the target POJO
   * @return the mapped POJO object
   */
  public static <T> T mapToPojo(List<?> tuple, List<Field> format, Class<T> entityClass) {
    Map<String, Object> map = toMap(tuple, format);
    return mapToPojo(map, entityClass);
  }

  /**
   * Converts a flat tuple list directly to a POJO using format metadata.
   *
   * <p>This is a convenience method that combines {@link #toMap(List, List)} and {@link
   * #mapToPojo(Map, TypeReference)} into a single operation.
   *
   * @param tuple the flat tuple as a list of values
   * @param format the field format metadata with field names
   * @param typeReference the target type reference
   * @param <T> the type of the target POJO
   * @return the mapped POJO object
   */
  public static <T> T mapToPojo(List<?> tuple, List<Field> format, TypeReference<T> typeReference) {
    Map<String, Object> map = toMap(tuple, format);
    return mapToPojo(map, typeReference);
  }

  /**
   * Converts a Tuple with flat list data to a POJO.
   *
   * <p>This method extracts format from the Tuple object (if available) and uses it for mapping to
   * the POJO.
   *
   * @param tuple the Tuple object containing flat list data and format
   * @param entityClass the target POJO class
   * @param <T> the type of the target POJO
   * @return the mapped POJO object, or null if tuple is null
   * @throws IllegalStateException if tuple doesn't have format information
   */
  public static <T> T mapToPojo(Tuple<List<?>> tuple, Class<T> entityClass) {
    if (tuple == null) {
      return null;
    }

    List<Field> format = tuple.getFormat();
    if (format == null) {
      throw new IllegalStateException(
          "Tuple doesn't contain format information. "
              + "Use mapToPojo(List, List, Class) method with explicit format parameter.");
    }

    return mapToPojo(tuple.get(), format, entityClass);
  }

  /**
   * Converts a Tuple with flat list data to a POJO.
   *
   * <p>This method extracts format from the Tuple object (if available) and uses it for mapping to
   * the POJO.
   *
   * @param tuple the Tuple object containing flat list data and format
   * @param typeReference the target type reference
   * @param <T> the type of the target POJO
   * @return the mapped POJO object, or null if tuple is null
   * @throws IllegalStateException if tuple doesn't have format information
   */
  public static <T> T mapToPojo(Tuple<List<?>> tuple, TypeReference<T> typeReference) {
    if (tuple == null) {
      return null;
    }

    List<Field> format = tuple.getFormat();
    if (format == null) {
      throw new IllegalStateException(
          "Tuple doesn't contain format information. "
              + "Use mapToPojo(List, List, TypeReference) method with explicit format parameter.");
    }

    return mapToPojo(tuple.get(), format, typeReference);
  }

  /**
   * Converts a list of flat tuples to a list of POJOs using format metadata.
   *
   * @param tuples the list of flat tuples
   * @param format the field format metadata with field names
   * @param entityClass the target POJO class
   * @param <T> the type of the target POJO
   * @return the list of mapped POJO objects
   */
  public static <T> List<T> mapToPojoList(
      List<List<?>> tuples, List<Field> format, Class<T> entityClass) {
    if (tuples == null || tuples.isEmpty()) {
      return new ArrayList<>();
    }

    return tuples.stream()
        .map(tuple -> mapToPojo(tuple, format, entityClass))
        .collect(Collectors.toList());
  }

  /**
   * Converts a list of flat tuples to a list of POJOs using format metadata.
   *
   * @param tuples the list of flat tuples
   * @param format the field format metadata with field names
   * @param typeReference the target type reference
   * @param <T> the type of the target POJO
   * @return the list of mapped POJO objects
   */
  public static <T> List<T> mapToPojoList(
      List<List<?>> tuples, List<Field> format, TypeReference<T> typeReference) {
    if (tuples == null || tuples.isEmpty()) {
      return new ArrayList<>();
    }

    return tuples.stream()
        .map(tuple -> mapToPojo(tuple, format, typeReference))
        .collect(Collectors.toList());
  }

  /**
   * Converts a list of Tuple objects to a list of POJOs.
   *
   * <p>This method extracts format from each Tuple object (if available) and uses it for mapping to
   * POJOs.
   *
   * @param tuples the list of Tuple objects
   * @param entityClass the target POJO class
   * @param <T> the type of the target POJO
   * @return the list of mapped POJO objects
   */
  public static <T> List<T> mapToPojoListFromTuples(
      List<Tuple<List<?>>> tuples, Class<T> entityClass) {
    if (tuples == null || tuples.isEmpty()) {
      return new ArrayList<>();
    }

    return tuples.stream().map(tuple -> mapToPojo(tuple, entityClass)).collect(Collectors.toList());
  }

  /**
   * Converts a list of Tuple objects to a list of POJOs.
   *
   * <p>This method extracts format from each Tuple object (if available) and uses it for mapping to
   * POJOs.
   *
   * @param tuples the list of Tuple objects
   * @param typeReference the target type reference
   * @param <T> the type of the target POJO
   * @return the list of mapped POJO objects
   */
  public static <T> List<T> mapToPojoListFromTuples(
      List<Tuple<List<?>>> tuples, TypeReference<T> typeReference) {
    if (tuples == null || tuples.isEmpty()) {
      return new ArrayList<>();
    }

    return tuples.stream()
        .map(tuple -> mapToPojo(tuple, typeReference))
        .collect(Collectors.toList());
  }
}
