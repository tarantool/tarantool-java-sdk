/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_DATETIME;

public class DatetimeExtensionModule {

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder;
  private static final String UNEXPECTED_EXTENSION_TYPE = "Unexpected extension type (0x";
  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-datetime");
  public static final int HOURS_PER_DAY = 24;
  public static final int MINUTES_PER_HOUR = 60;
  public static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
  public static final int SECONDS_PER_MINUTE = 60;
  public static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
  public static final long SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
  public static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;
  public static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000_000L;
  public static final long NANOS_PER_MILLI = 1000_000L;
  public static final long NANOS_PER_SECOND = 1000_000_000L;
  public static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;
  public static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;
  public static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;
  public static final short DEFAULT_TIMEZONE_INDEX = 0;
  public static final short DEFAULT_TIMEZONE_OFFSET = 0;
  public static final int DEFAULT_NANOS = 0;
  public static final int MP_DATETIME_SIZE_8_BYTES = 8;
  public static final int MP_DATETIME_SIZE_16_BYTES = 16;

  static {
    threadLocalStringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    INSTANCE.addSerializer(Instant.class, new InstantSerializer(Instant.class));
    INSTANCE.addDeserializer(Instant.class, new InstantDeserializer(Instant.class));
    INSTANCE.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(ZonedDateTime.class));
    INSTANCE.addDeserializer(
        ZonedDateTime.class, new ZonedDateTimeDeserializer(ZonedDateTime.class));
    INSTANCE.addSerializer(
        OffsetDateTime.class, new OffsetDateTimeSerializer(OffsetDateTime.class));
    INSTANCE.addDeserializer(
        OffsetDateTime.class, new OffsetDateTimeDeserializer(OffsetDateTime.class));
    INSTANCE.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LocalDateTime.class));
    INSTANCE.addDeserializer(
        LocalDateTime.class, new LocalDateTimeDeserializer(LocalDateTime.class));
    INSTANCE.addSerializer(LocalDate.class, new LocalDateSerializer(LocalDate.class));
    INSTANCE.addDeserializer(LocalDate.class, new LocalDateDeserializer(LocalDate.class));
    INSTANCE.addSerializer(LocalTime.class, new LocalTimeSerializer(LocalTime.class));
    INSTANCE.addDeserializer(LocalTime.class, new LocalTimeDeserializer(LocalTime.class));
  }

  private DatetimeExtensionModule() {}

  private static MessagePackExtensionType marshallDatetime(
      long seconds, int nano, short tzOffset, short tzIndex) {
    boolean hasOptionalFields =
        (nano != DEFAULT_NANOS)
            || (tzOffset != DEFAULT_TIMEZONE_OFFSET)
            || (tzIndex != DEFAULT_TIMEZONE_INDEX);
    int size = (hasOptionalFields) ? MP_DATETIME_SIZE_16_BYTES : MP_DATETIME_SIZE_8_BYTES;

    ByteBuffer buffer = ByteBuffer.wrap(new byte[size]).order(ByteOrder.LITTLE_ENDIAN);

    buffer.putLong(seconds);
    if (hasOptionalFields) {
      buffer.putInt(nano);
      buffer.putShort(tzOffset);
      buffer.putShort(tzIndex);
    }
    return new MessagePackExtensionType(IPROTO_EXT_DATETIME, buffer.array());
  }

  private static String getZone(short tzIndex) {
    return (tzIndex == DEFAULT_TIMEZONE_INDEX)
        ? null
        : TarantoolTimezones.indexToTimezone.get(tzIndex);
  }

  public static class InstantSerializer extends StdSerializer<Instant> {

    public InstantSerializer(Class<Instant> t) {
      super(t);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeObject(
          marshallDatetime(
              value.getEpochSecond(),
              value.getNano(),
              DEFAULT_TIMEZONE_OFFSET,
              DEFAULT_TIMEZONE_INDEX));
    }
  }

  public static class InstantDeserializer extends StdDeserializer<Instant> {

    public InstantDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DATETIME) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append(UNEXPECTED_EXTENSION_TYPE)
                .append(Utils.byteToHex(ext.getType()))
                .append(") for Instant object")
                .toString());
      }

      byte[] data = ext.getData();
      int size = data.length;
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      long seconds = buffer.getLong();
      int nsec = DEFAULT_NANOS;
      if (size == MP_DATETIME_SIZE_16_BYTES) {
        nsec = buffer.getInt();
      }

      return Instant.ofEpochSecond(seconds, nsec);
    }
  }

  public static class ZonedDateTimeSerializer extends StdSerializer<ZonedDateTime> {

    public ZonedDateTimeSerializer(Class<ZonedDateTime> t) {
      super(t);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      long seconds = value.toEpochSecond();
      int nano = value.getNano();
      short tzOffset = (short) (value.getOffset().getTotalSeconds() / SECONDS_PER_MINUTE);
      ZoneId zoneId = value.getZone();
      Short tzIndexObj = TarantoolTimezones.timezoneToIndex.get(zoneId.getId());
      final short tzIndex = (tzIndexObj == null) ? DEFAULT_TIMEZONE_INDEX : tzIndexObj;

      gen.writeObject(marshallDatetime(seconds, nano, tzOffset, tzIndex));
    }
  }

  public static class ZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime>
      implements TarantoolDeserializer<ZonedDateTime> {

    public ZonedDateTimeDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DATETIME) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append(UNEXPECTED_EXTENSION_TYPE)
                .append(Utils.byteToHex(ext.getType()))
                .append(") for ZonedDateTime object")
                .toString());
      }

      return deserialize(ext);
    }

    @Override
    public ZonedDateTime deserialize(MessagePackExtensionType ext) throws IOException {
      byte[] data = ext.getData();
      int size = data.length;
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      long seconds = buffer.getLong();
      int nsec = DEFAULT_NANOS;
      short tzOffset = DEFAULT_TIMEZONE_OFFSET;
      short tzIndex = DEFAULT_TIMEZONE_INDEX;
      if (size == MP_DATETIME_SIZE_16_BYTES) {
        nsec = buffer.getInt();
        tzOffset = buffer.getShort();
        tzIndex = buffer.getShort();
      }

      Instant instant = Instant.ofEpochSecond(seconds, nsec);
      String zone = getZone(tzIndex);
      if (zone != null) {
        return ZonedDateTime.ofInstant(instant, ZoneId.of(zone));
      }
      return ZonedDateTime.ofInstant(
          instant, ZoneOffset.ofTotalSeconds(tzOffset * SECONDS_PER_MINUTE));
    }
  }

  public static class OffsetDateTimeSerializer extends StdSerializer<OffsetDateTime> {

    public OffsetDateTimeSerializer(Class<OffsetDateTime> t) {
      super(t);
    }

    @Override
    public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      long seconds = value.toEpochSecond();
      int nano = value.getNano();
      short tzOffset = (short) (value.getOffset().getTotalSeconds() / SECONDS_PER_MINUTE);
      gen.writeObject(marshallDatetime(seconds, nano, tzOffset, DEFAULT_TIMEZONE_INDEX));
    }
  }

  public static class OffsetDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {

    public OffsetDateTimeDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DATETIME) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append(UNEXPECTED_EXTENSION_TYPE)
                .append(Utils.byteToHex(ext.getType()))
                .append(") for OffsetDateTime object")
                .toString());
      }

      byte[] data = ext.getData();
      int size = data.length;
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      long seconds = buffer.getLong();
      int nsec = DEFAULT_NANOS;
      short tzOffset = DEFAULT_TIMEZONE_OFFSET;
      short tzIndex = DEFAULT_TIMEZONE_INDEX;
      if (size == MP_DATETIME_SIZE_16_BYTES) {
        nsec = buffer.getInt();
        tzOffset = buffer.getShort();
        tzIndex = buffer.getShort();
      }

      Instant instant = Instant.ofEpochSecond(seconds, nsec);
      String zone = getZone(tzIndex);
      if (zone != null) {
        return OffsetDateTime.ofInstant(instant, ZoneId.of(zone));
      }
      return OffsetDateTime.ofInstant(
          instant, ZoneOffset.ofTotalSeconds(tzOffset * SECONDS_PER_MINUTE));
    }
  }

  public static class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

    public LocalDateTimeSerializer(Class<LocalDateTime> t) {
      super(t);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeObject(
          marshallDatetime(
              value.toEpochSecond(ZoneOffset.UTC),
              value.getNano(),
              DEFAULT_TIMEZONE_OFFSET,
              DEFAULT_TIMEZONE_INDEX));
    }
  }

  public static class LocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public LocalDateTimeDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DATETIME) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append(UNEXPECTED_EXTENSION_TYPE)
                .append(Utils.byteToHex(ext.getType()))
                .append(") for LocalDateTime object")
                .toString());
      }

      byte[] data = ext.getData();
      int size = data.length;
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      long seconds = buffer.getLong();
      int nsec = DEFAULT_NANOS;
      short tzOffset = DEFAULT_TIMEZONE_OFFSET;
      short tzIndex = DEFAULT_TIMEZONE_INDEX;
      if (size == MP_DATETIME_SIZE_16_BYTES) {
        nsec = buffer.getInt();
        tzOffset = buffer.getShort();
        tzIndex = buffer.getShort();
      }

      Instant instant = Instant.ofEpochSecond(seconds, nsec);
      String zone = getZone(tzIndex);
      if (zone != null) {
        return LocalDateTime.ofInstant(instant, ZoneId.of(zone));
      }
      return LocalDateTime.ofInstant(
          instant, ZoneOffset.ofTotalSeconds(tzOffset * SECONDS_PER_MINUTE));
    }
  }

  public static class LocalDateSerializer extends StdSerializer<LocalDate> {

    public LocalDateSerializer(Class<LocalDate> t) {
      super(t);
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeObject(
          marshallDatetime(
              value.toEpochDay() * SECONDS_PER_DAY,
              DEFAULT_NANOS,
              DEFAULT_TIMEZONE_OFFSET,
              DEFAULT_TIMEZONE_INDEX));
    }
  }

  public static class LocalDateDeserializer extends StdDeserializer<LocalDate> {

    public LocalDateDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DATETIME) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append(UNEXPECTED_EXTENSION_TYPE)
                .append(Utils.byteToHex(ext.getType()))
                .append(") for LocalDate object")
                .toString());
      }

      byte[] data = ext.getData();
      int size = data.length;
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      long seconds = buffer.getLong();
      short tzOffset = DEFAULT_TIMEZONE_OFFSET;
      if (size == MP_DATETIME_SIZE_16_BYTES) {
        tzOffset = buffer.getShort();
      }

      long localSecond = seconds + ((long) tzOffset) * SECONDS_PER_MINUTE;
      long epochDay = Math.floorDiv(localSecond, SECONDS_PER_DAY);
      return LocalDate.ofEpochDay(epochDay);
    }
  }

  public static class LocalTimeSerializer extends StdSerializer<LocalTime> {

    public LocalTimeSerializer(Class<LocalTime> t) {
      super(t);
    }

    @Override
    public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeObject(
          marshallDatetime(
              value.toSecondOfDay(),
              value.getNano(),
              DEFAULT_TIMEZONE_OFFSET,
              DEFAULT_TIMEZONE_INDEX));
    }
  }

  public static class LocalTimeDeserializer extends StdDeserializer<LocalTime> {

    public LocalTimeDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DATETIME) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append(UNEXPECTED_EXTENSION_TYPE)
                .append(Utils.byteToHex(ext.getType()))
                .append(") for LocalTime object")
                .toString());
      }

      byte[] data = ext.getData();
      int size = data.length;
      ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      long seconds = buffer.getLong();
      int nsec = DEFAULT_NANOS;
      short tzOffset = DEFAULT_TIMEZONE_OFFSET;
      if (size == MP_DATETIME_SIZE_16_BYTES) {
        nsec = buffer.getInt();
        tzOffset = buffer.getShort();
      }

      long localSecond = seconds + ((long) tzOffset) * SECONDS_PER_MINUTE;
      long secsOfDay = Math.floorMod(localSecond, SECONDS_PER_DAY);
      return LocalTime.ofNanoOfDay(secsOfDay * NANOS_PER_SECOND + nsec);
    }
  }
}
