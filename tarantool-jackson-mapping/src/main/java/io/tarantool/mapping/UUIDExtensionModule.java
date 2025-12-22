/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_UUID;

public class UUIDExtensionModule {

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder;
  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-uuid");

  static {
    threadLocalStringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    INSTANCE.addSerializer(UUID.class, new UUIDSerializer(UUID.class));
    INSTANCE.addDeserializer(UUID.class, new UUIDDeserializer(UUID.class));
  }

  private UUIDExtensionModule() {}

  public static class UUIDSerializer extends StdSerializer<UUID> {

    public UUIDSerializer(Class<UUID> t) {
      super(t);
    }

    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);

      long mostSignificant = value.getMostSignificantBits();
      buffer
          .putInt((int) (mostSignificant >>> 32))
          .putShort((short) ((mostSignificant & 0x00000000FFFFFFFFL) >>> 16))
          .putShort((short) (mostSignificant & 0x000000000000FFFFL));

      long leastSignificant = value.getLeastSignificantBits();
      buffer
          .put((byte) (leastSignificant >>> 56))
          .put((byte) ((leastSignificant & 0x00FF000000000000L) >>> 48))
          .put((byte) ((leastSignificant & 0x0000FF0000000000L) >>> 40))
          .put((byte) ((leastSignificant & 0x000000FF00000000L) >>> 32))
          .put((byte) ((leastSignificant & 0x00000000FF000000L) >>> 24))
          .put((byte) ((leastSignificant & 0x0000000000FF0000L) >>> 16))
          .put((byte) ((leastSignificant & 0x000000000000FF00L) >>> 8))
          .put((byte) (leastSignificant & 0x00000000000000FFL));

      MessagePackExtensionType extensionType =
          new MessagePackExtensionType(IPROTO_EXT_UUID, buffer.array());
      gen.writeObject(extensionType);
    }
  }

  public static class UUIDDeserializer extends StdDeserializer<UUID>
      implements TarantoolDeserializer<UUID> {

    public UUIDDeserializer(Class<?> vc) {
      super(vc);
    }

    /*
       most significant
       0xFFFFFFFF00000000 time_low
       0x00000000FFFF0000 time_mid
       0x000000000000F000 version
       0x0000000000000FFF time_hi
       least significant
       0xC000000000000000 variant
       0x3FFF000000000000 clock_seq
       0x0000FFFFFFFFFFFF node
    */
    @Override
    public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_UUID) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append("Unexpected extension type (0x")
                .append(Utils.byteToHex(ext.getType()))
                .append(") for UUID object")
                .toString());
      }

      return deserialize(ext);
    }

    @Override
    public UUID deserialize(MessagePackExtensionType ext) {
      ByteBuffer buffer = ByteBuffer.wrap(ext.getData());
      long mostSignificant =
          (buffer.getInt() & 0xFFFFFFFFL) << 32
              | (buffer.getShort() & 0xFFFFL) << 16
              | (buffer.getShort() & 0xFFFFL);
      long leastSignificant =
          (buffer.get() & 0xFFL) << 56
              | (buffer.get() & 0xFFL) << 48
              | (buffer.get() & 0xFFL) << 40
              | (buffer.get() & 0xFFL) << 32
              | (buffer.get() & 0xFFL) << 24
              | (buffer.get() & 0xFFL) << 16
              | (buffer.get() & 0xFFL) << 8
              | (buffer.get() & 0xFFL);

      return new UUID(mostSignificant, leastSignificant);
    }
  }
}
