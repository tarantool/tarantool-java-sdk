/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_INTERVAL;

public class IntervalExtensionModule {

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder;
  public static final int FIELD_YEAR = 0;
  public static final int FIELD_MONTH = 1;
  public static final int FIELD_WEEK = 2;
  public static final int FIELD_DAY = 3;
  public static final int FIELD_HOUR = 4;
  public static final int FIELD_MIN = 5;
  public static final int FIELD_SEC = 6;
  public static final int FIELD_NSEC = 7;
  public static final int FIELD_ADJUST = 8;

  public static final int NONE_ADJUST = 0;
  public static final int EXCESS_ADJUST = 1;
  public static final int LAST_ADJUST = 2;

  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-interval");

  static {
    threadLocalStringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    INSTANCE.addSerializer(Interval.class, new IntervalSerializer(Interval.class));
    INSTANCE.addDeserializer(Interval.class, new IntervalDeserializer(Interval.class));
  }

  private IntervalExtensionModule() {
  }

  public static class IntervalSerializer extends StdSerializer<Interval> {

    public IntervalSerializer(Class<Interval> t) {
      super(t);
    }

    @Override
    public void serialize(Interval value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      int fieldsCount = 0;
      List<Long> fields = Arrays.asList(value.getYear(),
          value.getMonth(),
          value.getWeek(),
          value.getDay(),
          value.getHour(),
          value.getMin(),
          value.getSec(),
          value.getNsec());
      for (long fieldValue : fields) {
        if (fieldValue != 0) {
          fieldsCount++;
        }
      }
      int adjust = value.getAdjust().ordinal();
      if (adjust != 0) {
        fieldsCount++;
      }

      try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
        packer.packInt(fieldsCount);

        for (int i = 0; i < fields.size(); i++) {
          packField(packer, i, fields.get(i));
        }
        if (adjust != 0) {
          packer.packInt(FIELD_ADJUST);
          packer.packLong(adjust);
        }

        gen.writeObject(new MessagePackExtensionType(IPROTO_EXT_INTERVAL, packer.toByteArray()));
      }
    }

    private static void packField(MessageBufferPacker packer, int fieldId, long fieldValue) throws IOException {
      if (fieldValue != 0) {
        packer.packInt(fieldId);
        packer.packLong(fieldValue);
      }
    }
  }

  public static class IntervalDeserializer extends StdDeserializer<Interval> implements TarantoolDeserializer<Interval> {

    public IntervalDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public Interval deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_INTERVAL) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(sb.delete(0, sb.length())
            .append("Unexpected extension type (0x")
            .append(Utils.byteToHex(ext.getType()))
            .append(") for Interval object")
            .toString());
      }

      return deserialize(ext);
    }

    @Override
    public Interval deserialize(MessagePackExtensionType ext) throws IOException {
      byte[] data = ext.getData();

      Interval interval = new Interval();
      try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
        int fieldsCount = unpacker.unpackInt();
        for (int i = 0; i < fieldsCount; i++) {
          int type = unpacker.unpackInt();
          switch (type) {
            case FIELD_YEAR:
              interval.setYear(unpacker.unpackLong());
              break;
            case FIELD_MONTH:
              interval.setMonth(unpacker.unpackLong());
              break;
            case FIELD_WEEK:
              interval.setWeek(unpacker.unpackLong());
              break;
            case FIELD_DAY:
              interval.setDay(unpacker.unpackLong());
              break;
            case FIELD_HOUR:
              interval.setHour(unpacker.unpackLong());
              break;
            case FIELD_MIN:
              interval.setMin(unpacker.unpackLong());
              break;
            case FIELD_SEC:
              interval.setSec(unpacker.unpackLong());
              break;
            case FIELD_NSEC:
              interval.setNsec(unpacker.unpackLong());
              break;
            case FIELD_ADJUST:
              long adjust = unpacker.unpackLong();
              switch ((int) adjust) {
                case NONE_ADJUST:
                  interval.setAdjust(Adjust.NoneAdjust);
                  break;
                case EXCESS_ADJUST:
                  interval.setAdjust(Adjust.ExcessAdjust);
                  break;
                case LAST_ADJUST:
                  interval.setAdjust(Adjust.LastAdjust);
                  break;
              }
              break;
          }
        }
      }

      return interval;
    }
  }
}
