/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;

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
import org.msgpack.value.ImmutableValue;

import io.tarantool.core.exceptions.BoxErrorValue;
import io.tarantool.core.protocol.requests.IProtoConstant;

public final class ErrorExtensionModule {

  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-error");

  static {
    INSTANCE.addDeserializer(BoxErrorValue.class, new ErrorDeserializer(BoxErrorValue.class));
    INSTANCE.addSerializer(BoxErrorValue.class, new ErrorSerializer(BoxErrorValue.class));
  }

  private ErrorExtensionModule() {}

  public static final class ErrorDeserializer extends StdDeserializer<BoxErrorValue>
      implements TarantoolDeserializer<BoxErrorValue> {

    public ErrorDeserializer(Class<?> cls) {
      super(cls);
    }

    /*
     * +============================+
     * |                            |
     * | 0x52: IPROTO_ERROR         |
     * | MP_MAP: extra information  |
     * |                            |
     * +============================+
     *       mp_ext 0x03
     * extra information contains only stack of error stack object
     */
    @Override
    public BoxErrorValue deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);

      if (ext == null) {
        return null;
      }

      if (ext.getType() != IProtoConstant.IPROTO_EXT_ERROR) {
        throw new JacksonMappingException(
            "Unexpected extension type (0x"
                + Utils.byteToHex(ext.getType())
                + ") for MP_ERROR object");
      }

      return deserialize(ext);
    }

    @Override
    public BoxErrorValue deserialize(MessagePackExtensionType ext) throws IOException {
      final byte[] data = ext.getData();
      try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
        final ImmutableValue val = unpacker.unpackValue();

        if (val == null || val.isNilValue()) {
          return null;
        }

        if (!val.isMapValue()) {
          throw new JacksonMappingException("MP_ERROR is not map in root");
        }
        return BoxErrorValue.from(val.asMapValue());
      }
    }
  }

  public static final class ErrorSerializer extends StdSerializer<BoxErrorValue> {

    public ErrorSerializer(Class<BoxErrorValue> t) {
      super(t);
    }

    @Override
    public void serialize(BoxErrorValue value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      if (value == null) {
        gen.writeObject(null);
        return;
      }

      final MessagePackExtensionType ext;
      try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
        value.serialize(packer);
        ext = new MessagePackExtensionType(IProtoConstant.IPROTO_EXT_ERROR, packer.toByteArray());
      }
      gen.writeObject(ext);
    }
  }
}
