/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TupleMapperTest {

  @Test
  public void testToMap() {
    List<?> tuple = Arrays.asList(1, "John", true);
    List<Field> format =
        Arrays.asList(
            new Field().setName("id"), new Field().setName("name"), new Field().setName("active"));

    Map<String, Object> result = TupleMapper.toMap(tuple, format);

    assertEquals(3, result.size());
    assertEquals(1, result.get("id"));
    assertEquals("John", result.get("name"));
    assertEquals(true, result.get("active"));
  }

  @Test
  public void testToMapWithNullInputs() {
    Map<String, Object> result1 = TupleMapper.toMap(null, Collections.emptyList());
    assertTrue(result1.isEmpty());

    Map<String, Object> result2 = TupleMapper.toMap(Collections.emptyList(), null);
    assertTrue(result2.isEmpty());
  }

  @Test
  public void testToMapWithMismatchedSizes() {
    List<?> tuple = Arrays.asList(1, "John");
    List<Field> format =
        Arrays.asList(
            new Field().setName("id"), new Field().setName("name"), new Field().setName("active"));

    assertThrows(IllegalArgumentException.class, () -> TupleMapper.toMap(tuple, format));
  }

  @Test
  public void testMapToPojoFromTupleAndFormat() {
    List<?> tuple = Arrays.asList(1, "John", true);
    List<Field> format =
        Arrays.asList(
            new Field().setName("id"), new Field().setName("name"), new Field().setName("active"));

    TestPerson result = TupleMapper.mapToPojo(tuple, format, TestPerson.class);

    assertNotNull(result);
    assertEquals(1, result.id);
    assertEquals("John", result.name);
    assertEquals(true, result.active);
  }

  @Test
  public void testMapToPojoFromTupleWithFormat() {
    List<?> data = Arrays.asList(1, "John", true);
    List<Field> format =
        Arrays.asList(
            new Field().setName("id"), new Field().setName("name"), new Field().setName("active"));
    Tuple<List<?>> tuple = new Tuple<>(data, 0, format);

    TestPerson result = TupleMapper.mapToPojo(tuple, TestPerson.class);

    assertNotNull(result);
    assertEquals(1, result.id);
    assertEquals("John", result.name);
    assertEquals(true, result.active);
  }

  @Test
  public void testMapToPojoFromTupleWithoutFormat() {
    List<?> data = Arrays.asList(1, "John", true);
    Tuple<List<?>> tuple = new Tuple<>(data, 0); // no format

    assertThrows(IllegalStateException.class, () -> TupleMapper.mapToPojo(tuple, TestPerson.class));
  }

  @Test
  public void testMapToPojoFromNullTuple() {
    TestPerson result = TupleMapper.mapToPojo((Tuple<List<?>>) null, TestPerson.class);
    assertNull(result);
  }

  @Test
  public void testMapToPojoList() {
    List<List<?>> tuples =
        Arrays.asList(Arrays.asList(1, "John", true), Arrays.asList(2, "Jane", false));
    List<Field> format =
        Arrays.asList(
            new Field().setName("id"), new Field().setName("name"), new Field().setName("active"));

    List<TestPerson> result = TupleMapper.mapToPojoList(tuples, format, TestPerson.class);

    assertEquals(2, result.size());
    assertEquals(1, result.get(0).id);
    assertEquals("John", result.get(0).name);
    assertEquals(2, result.get(1).id);
    assertEquals("Jane", result.get(1).name);
  }

  @Test
  public void testMapToPojoListFromTuples() {
    List<Field> format =
        Arrays.asList(
            new Field().setName("id"), new Field().setName("name"), new Field().setName("active"));
    List<Tuple<List<?>>> tuples =
        Arrays.asList(
            new Tuple<>(Arrays.asList(1, "John", true), 0, format),
            new Tuple<>(Arrays.asList(2, "Jane", false), 0, format));

    List<TestPerson> result = TupleMapper.mapToPojoListFromTuples(tuples, TestPerson.class);

    assertEquals(2, result.size());
    assertEquals(1, result.get(0).id);
    assertEquals("John", result.get(0).name);
    assertEquals(2, result.get(1).id);
    assertEquals("Jane", result.get(1).name);
  }

  @Test
  public void testMapToPojoListWithEmptyInput() {
    List<TestPerson> result1 =
        TupleMapper.mapToPojoList(null, Collections.emptyList(), TestPerson.class);
    assertTrue(result1.isEmpty());

    List<TestPerson> result2 =
        TupleMapper.mapToPojoList(
            Collections.emptyList(), Collections.emptyList(), TestPerson.class);
    assertTrue(result2.isEmpty());
  }

  // Test POJO class
  public static class TestPerson {
    public Integer id;
    public String name;
    public Boolean active;

    public TestPerson() {}
  }
}
