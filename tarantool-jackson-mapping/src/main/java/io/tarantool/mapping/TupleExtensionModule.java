/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_INTERVAL;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_TUPLE;
import io.tarantool.core.protocol.ByteBodyValueWrapper;

public class TupleExtensionModule {

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder;
  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-tuple");
  public static final JavaType JAVA_TYPE_OBJECT =
      new ObjectMapper().getTypeFactory().constructFromCanonical(Object.class.getName());

  static {
    threadLocalStringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    INSTANCE.addSerializer(Tuple.class, new TupleSerializer(Tuple.class));
    INSTANCE.addDeserializer(Tuple.class, new TupleDeserializer());
  }

  public TupleExtensionModule() {
  }

  public static class TupleSerializer extends StdSerializer<Tuple> {

    public TupleSerializer(Class<Tuple> t) {
      super(t);
    }

    @Override
    public void serialize(Tuple tuple, JsonGenerator gen, SerializerProvider provider) throws IOException {
      try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
        packer.packLong(tuple.getFormatId());

        packer.addPayload(
            TarantoolJacksonMapping.toValueAux(
                tuple.get()
            )
        );

        gen.writeObject(new MessagePackExtensionType(IPROTO_EXT_TUPLE, packer.toByteArray()));
      }
    }
  }

  public static class TupleDeserializer extends JsonDeserializer<Tuple<?>> implements TarantoolDeserializer<Tuple<?>>,
      ContextualDeserializer {

    private JavaType valueType;

    public TupleDeserializer(JavaType valueType) {
      this.valueType = valueType;
    }

    public TupleDeserializer() {
      this.valueType = JAVA_TYPE_OBJECT;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
      List<JavaType> parameters = ctxt.getContextualType().getBindings().getTypeParameters();
      JavaType type = JAVA_TYPE_OBJECT;
      if (!parameters.isEmpty()) {
        type = parameters.get(0);
      }
      return new TupleDeserializer(type);
    }

    @Override
    public Tuple<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.currentTokenId() != JsonTokenId.ID_EMBEDDED_OBJECT) {
        return new Tuple<>(ctxt.readValue(p, valueType), null, Collections.emptyList());
      }
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_TUPLE) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(sb.delete(0, sb.length())
            .append("Unexpected extension type (0x")
            .append(Utils.byteToHex(ext.getType()))
            .append(") for TUPLE object")
            .toString());
      }

      return deserialize(ext);
    }

    @Override
    public Tuple<?> deserialize(MessagePackExtensionType ext) throws IOException {
      byte[] data = ext.getData();

      try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
        int formatId = unpacker.unpackInt();
        int read = (int) unpacker.getTotalReadBytes();
        return new Tuple(
            TarantoolJacksonMapping.readValueAux(
                new ByteBodyValueWrapper(data, read, data.length - read),
                valueType
            ),
            formatId,
            Collections.emptyList()
        );
      }
    }
  }
}
