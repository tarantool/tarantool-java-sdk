/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_DATETIME;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_DECIMAL;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_INTERVAL;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_UUID;

public class UniversalExtensionModule {

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder;
  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-any");

  static {
    INSTANCE.addDeserializer(Object.class, new ObjectDeserializer());
    threadLocalStringBuilder = ThreadLocal.withInitial(StringBuilder::new);
  }

  private UniversalExtensionModule() {
  }

  public static class ObjectDeserializer extends UntypedObjectDeserializer {

    private final HashMap<Byte, TarantoolDeserializer<?>> deserializers = new HashMap<>();

    public ObjectDeserializer() {
      super(null, null);
      deserializers.put(IPROTO_EXT_DECIMAL, new DecimalExtensionModule.BigDecimalDeserializer(BigDecimal.class));
      deserializers.put(IPROTO_EXT_UUID, new UUIDExtensionModule.UUIDDeserializer(UUID.class));
      deserializers.put(IPROTO_EXT_DATETIME,
          new DatetimeExtensionModule.ZonedDateTimeDeserializer(ZonedDateTime.class));
      deserializers.put(IPROTO_EXT_INTERVAL, new IntervalExtensionModule.IntervalDeserializer(Interval.class));
      deserializers.put(IPROTO_EXT_TUPLE, new TupleExtensionModule.TupleDeserializer());
    }

    private boolean isExtensionValue(Object object) {
      return object instanceof MessagePackExtensionType;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.currentTokenId() != JsonTokenId.ID_EMBEDDED_OBJECT || !isExtensionValue(p.getEmbeddedObject())) {
        return super.deserialize(p, ctxt);
      }
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      byte type = ext.getType();
      TarantoolDeserializer<?> res = deserializers.get(type);
      if (res == null) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new IllegalStateException(sb.delete(0, threadLocalStringBuilder.get().length())
            .append("Deserializer for type Object is not found for 0x")
            .append(Utils.byteToHex(type)).toString());
      }
      return res.deserialize(ext);
    }
  }
}
