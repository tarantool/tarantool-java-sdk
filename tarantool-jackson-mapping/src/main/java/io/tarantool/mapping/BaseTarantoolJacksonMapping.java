/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_DATA;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EVENT_DATA;
import io.tarantool.core.protocol.ByteBodyValueWrapper;
import io.tarantool.core.protocol.IProtoResponse;

public class BaseTarantoolJacksonMapping {

  public static final MessagePackFactory mpFactory = new MessagePackFactory();

  public static final ObjectMapper objectMapper =
      new ObjectMapper(mpFactory)
          .registerModule(DatetimeExtensionModule.INSTANCE)
          .registerModule(DecimalExtensionModule.INSTANCE)
          .registerModule(IntervalExtensionModule.INSTANCE)
          .registerModule(TupleExtensionModule.INSTANCE)
          .registerModule(UUIDExtensionModule.INSTANCE)
          .registerModule(UniversalExtensionModule.INSTANCE);
  public static final TypeFactory typeFactory = objectMapper.getTypeFactory();
  public static final CollectionType LIST_TUPLE =
      typeFactory.constructCollectionType(List.class, Tuple.class);
  public static final CollectionType LIST_TUPLE_LIST =
      typeFactory.constructCollectionType(
          List.class, typeFactory.constructParametricType(Tuple.class, List.class));
  public static final ObjectMapper objectMapperWithMessagePackKeySerializer =
      new ObjectMapper(mpFactory)
          .registerModule(DatetimeExtensionModule.INSTANCE)
          .registerModule(DecimalExtensionModule.INSTANCE)
          .registerModule(IntervalExtensionModule.INSTANCE)
          .registerModule(TupleExtensionModule.INSTANCE)
          .registerModule(UUIDExtensionModule.INSTANCE)
          .registerModule(UniversalExtensionModule.INSTANCE)
          .registerModule(FormatsModule.INSTANCE);
  public static final MessagePackFactory innerMpFactory =
      new MessagePackFactory().setReuseResourceInParser(false).setReuseResourceInGenerator(false);

  public static final ObjectMapper innerObjectMapper =
      new ObjectMapper(innerMpFactory)
          .registerModule(DecimalExtensionModule.INSTANCE)
          .registerModule(UUIDExtensionModule.INSTANCE)
          .registerModule(DatetimeExtensionModule.INSTANCE)
          .registerModule(IntervalExtensionModule.INSTANCE)
          .registerModule(TupleExtensionModule.INSTANCE)
          .registerModule(UniversalExtensionModule.INSTANCE);

  public static Object readData(IProtoResponse response) {
    return readValue(response.getByteBodyValue(IPROTO_DATA));
  }

  public static Object readEventData(IProtoResponse response) {
    return readValue(response.getByteBodyValue(IPROTO_EVENT_DATA));
  }

  public static Object readValue(ByteBodyValueWrapper byteBodyValueWrapper) {
    try {
      return objectMapper.readValue(
          byteBodyValueWrapper.getPacket(),
          byteBodyValueWrapper.getOffset(),
          byteBodyValueWrapper.getValueLength(),
          Object.class);
    } catch (IOException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static <T> T readData(IProtoResponse response, Class<T> entity) {
    return readValue(response.getByteBodyValue(IPROTO_DATA), entity);
  }

  public static <T> T readEventData(IProtoResponse response, Class<T> entity) {
    return readValue(response.getByteBodyValue(IPROTO_EVENT_DATA), entity);
  }

  public static <T> T readValue(ByteBodyValueWrapper byteBodyValueWrapper, Class<T> entity) {
    try {
      return objectMapper.readValue(
          byteBodyValueWrapper.getPacket(),
          byteBodyValueWrapper.getOffset(),
          byteBodyValueWrapper.getValueLength(),
          entity);
    } catch (IOException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static <T> T readData(IProtoResponse response, TypeReference<T> entity) {
    return readValue(response.getByteBodyValue(IPROTO_DATA), entity);
  }

  public static <T> T readEventData(IProtoResponse response, TypeReference<T> entity) {
    return readValue(response.getByteBodyValue(IPROTO_EVENT_DATA), entity);
  }

  public static <T> T readValue(
      ByteBodyValueWrapper byteBodyValueWrapper, TypeReference<T> entity) {
    try {
      return objectMapper.readValue(
          byteBodyValueWrapper.getPacket(),
          byteBodyValueWrapper.getOffset(),
          byteBodyValueWrapper.getValueLength(),
          entity);
    } catch (IOException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static <T> T readData(IProtoResponse response, JavaType entity) {
    return readValue(response.getByteBodyValue(IPROTO_DATA), entity);
  }

  public static <T> T readValue(ByteBodyValueWrapper byteBodyValueWrapper, JavaType entity) {
    try {
      return objectMapper.readValue(
          byteBodyValueWrapper.getPacket(),
          byteBodyValueWrapper.getOffset(),
          byteBodyValueWrapper.getValueLength(),
          entity);
    } catch (IOException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static <T> T readValueAux(ByteBodyValueWrapper byteBodyValueWrapper, JavaType entity) {
    try {
      return innerObjectMapper.readValue(
          byteBodyValueWrapper.getPacket(),
          byteBodyValueWrapper.getOffset(),
          byteBodyValueWrapper.getValueLength(),
          entity);
    } catch (IOException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static byte[] toValue(Object object) {
    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static byte[] toValueWithKeySerializer(Object object) {
    try {
      return objectMapperWithMessagePackKeySerializer.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new JacksonMappingException(e);
    }
  }

  public static byte[] toValueAux(Object object) {
    try {
      return innerObjectMapper.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new JacksonMappingException(e);
    }
  }

  /**
   * The method allows to create a single parameterized type from those specified in the input
   * arguments.
   *
   * @param externalType a type that will be parameterized by the {@code internalType} type
   * @param internalType type with which the type {@code externalType} will be parameterized
   * @param <T> type of {@code internalType}
   * @param <E> type of {@code externalType}
   * @return {@link JavaType} parameterized type
   */
  public static <T, E> JavaType wrapIntoType(Class<E> externalType, TypeReference<T> internalType) {
    return wrapIntoType(externalType, objectMapper.constructType(internalType));
  }

  /**
   * Same as {@link #wrapIntoType(Class, TypeReference)}.
   *
   * @param externalType a type that will be parameterized by the {@code internalType} type
   * @param internalType type with which the type {@code externalType} will be parameterized
   * @param <T> type of {@code internalType}
   * @param <E> type of {@code externalType}
   * @return {@link JavaType} parameterized type
   */
  public static <T, E> JavaType wrapIntoType(Class<E> externalType, Class<T> internalType) {
    return wrapIntoType(externalType, typeFactory.constructType(internalType));
  }

  /**
   * The method allows to create a single parameterized type from those specified in the input
   * arguments.
   *
   * @param externalType a type that will be parameterized by the {@code internalType} type
   * @param internalType type with which the type {@code externalType} will be parameterized
   * @param <E> type of {@code externalType}
   * @return {@link JavaType} parameterized type
   */
  public static <E> JavaType wrapIntoType(Class<E> externalType, JavaType internalType) {
    return typeFactory.constructParametricType(externalType, internalType);
  }

  public static <T> JavaType wrapIntoList(TypeReference<T> internalType) {
    return wrapIntoType(List.class, internalType);
  }

  public static <T> JavaType wrapIntoList(JavaType internalType) {
    return wrapIntoType(List.class, internalType);
  }

  public static <T> JavaType wrapIntoList(Class<T> internalType) {
    return wrapIntoType(List.class, internalType);
  }

  public static <T> JavaType wrapIntoTuple(TypeReference<T> internalType) {
    return wrapIntoType(Tuple.class, internalType);
  }

  public static <T> JavaType wrapIntoTuple(JavaType internalType) {
    return wrapIntoType(Tuple.class, internalType);
  }

  public static <T> JavaType wrapIntoTuple(Class<T> internalType) {
    return wrapIntoType(Tuple.class, internalType);
  }

  /**
   * The method extracts tuple from the returned list with single tuple. If the list is empty or
   * null, then null will be returned.
   *
   * @param multiReturnResultList the result of operations as a list of tuples
   * @return a tuple as list of Java objects or null
   * @throws JacksonMappingException if for some reason a list with many tuples is returned
   */
  public static Tuple<List<?>> getFirstOrNullForReturnAsList(
      List<Tuple<List<?>>> multiReturnResultList) throws JacksonMappingException {
    if (multiReturnResultList == null || multiReturnResultList.isEmpty()) {
      return null;
    }

    if (multiReturnResultList.size() == 1) {
      return multiReturnResultList.get(0);
    }

    throw new JacksonMappingException(
        "This method should return one tuple or null, but it returned "
            + multiReturnResultList.size());
  }

  /**
   * The method extracts tuple from the returned list with single tuple. If the list is empty or
   * null, then null will be returned.
   *
   * @param multiReturnResultList the result of operations as a list of tuples
   * @param <T> data type of the tuple representation (custom type)
   * @return a tuple as custom type or null
   * @throws JacksonMappingException if for some reason a list with many tuples is returned
   */
  public static <T> T getFirstOrNullForReturnAsClass(List<T> multiReturnResultList)
      throws JacksonMappingException {
    if (multiReturnResultList == null || multiReturnResultList.isEmpty()) {
      return null;
    }

    if (multiReturnResultList.size() == 1) {
      return multiReturnResultList.get(0);
    }

    throw new JacksonMappingException(
        "This method should return one tuple or null, but it returned "
            + multiReturnResultList.size());
  }
}
