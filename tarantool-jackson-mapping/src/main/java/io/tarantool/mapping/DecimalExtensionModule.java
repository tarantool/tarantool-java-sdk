/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

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

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_EXT_DECIMAL;

public class DecimalExtensionModule {

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder;
  private static final int DECIMAL_MAX_DIGITS = 38;
  // See https://github.com/tarantool/decNumber/blob/master/decPacked.h
  private static final byte DECIMAL_MINUS = 0x0D;
  private static final byte DECIMAL_MINUS_ALT = 0x0B;
  private static final byte DECIMAL_PLUS = 0x0C;

  private static final String INVALID_DIGIT_AT_POSITION = "Invalid digit at position ";
  private static final String GREATER_DECIMAL_MAX_DIGITS_ERR =
      "Scales with absolute value greater than " + DECIMAL_MAX_DIGITS + " are not supported";
  public static final SimpleModule INSTANCE = new SimpleModule("msgpack-ext-decimal");

  static {
    threadLocalStringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    INSTANCE.addSerializer(BigDecimal.class, new BigDecimalSerializer(BigDecimal.class));
    INSTANCE.addDeserializer(BigDecimal.class, new BigDecimalDeserializer(BigDecimal.class));
  }

  private DecimalExtensionModule() {}

  public static class BigDecimalSerializer extends StdSerializer<BigDecimal> {

    public BigDecimalSerializer(Class<BigDecimal> t) {
      super(t);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      int scale = value.scale();
      if (scale > DECIMAL_MAX_DIGITS || scale < -DECIMAL_MAX_DIGITS) {
        throw new IOException(GREATER_DECIMAL_MAX_DIGITS_ERR);
      }

      String number = value.unscaledValue().toString();
      byte signum = DECIMAL_PLUS;
      int digitsNum = number.length();
      int pos = 0;
      if (number.charAt(0) == '-') {
        signum = DECIMAL_MINUS;
        digitsNum--;
        pos++;
      }

      int len = (digitsNum >> 1) + 1;
      byte[] bcd = new byte[len];
      bcd[len - 1] = signum;
      char[] digits = number.substring(pos).toCharArray();
      pos = digits.length - 1;
      for (int i = len - 1; i > 0; i--) {
        bcd[i] |= (byte) (Character.digit(digits[pos--], 10) << 4);
        bcd[i - 1] |= (byte) Character.digit(digits[pos--], 10);
      }
      if (pos == 0) {
        bcd[0] |= (byte) (Character.digit(digits[pos], 10) << 4);
      }

      MessagePackExtensionType extensionType;
      try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
        packer.packInt(scale);
        packer.writePayload(bcd);
        extensionType = new MessagePackExtensionType(IPROTO_EXT_DECIMAL, packer.toByteArray());
      }
      gen.writeObject(extensionType);
    }
  }

  public static class BigDecimalDeserializer extends StdDeserializer<BigDecimal>
      implements TarantoolDeserializer<BigDecimal> {

    public BigDecimalDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      MessagePackExtensionType ext = p.readValueAs(MessagePackExtensionType.class);
      if (ext.getType() != IPROTO_EXT_DECIMAL) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new JacksonMappingException(
            sb.delete(0, sb.length())
                .append("Unexpected extension type (0x")
                .append(Utils.byteToHex(ext.getType()))
                .append(") for BigDecimal object")
                .toString());
      }

      return deserialize(ext);
    }

    @Override
    public BigDecimal deserialize(MessagePackExtensionType ext) throws IOException {
      byte[] data = ext.getData();
      int scale;
      int scaleSize;
      try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
        scale = unpacker.unpackInt();
        scaleSize = (int) unpacker.getTotalReadBytes();
        if (!unpacker.hasNext()) {
          throw new IOException("Not enough bytes in the packed data");
        }
      }

      if (scale > DECIMAL_MAX_DIGITS || scale < -DECIMAL_MAX_DIGITS) {
        throw new IOException(GREATER_DECIMAL_MAX_DIGITS_ERR);
      }

      int len = data.length;
      // Extract sign from the last nibble
      int signum = (byte) (data[len - 1] & 0x0F);
      if (signum == DECIMAL_MINUS || signum == DECIMAL_MINUS_ALT) {
        signum = -1;
      } else if (signum <= 0x09) {
        throw new IOException("The sign nibble has wrong value");
      } else {
        signum = 1;
      }

      int i;
      // start at data byte after latest scale byte
      for (i = scaleSize; i < len && data[i] == 0; i++) {
        // skip zero bytes
      }
      if (len == i && (data[len - 1] & 0xF0) == 0) {
        return BigDecimal.ZERO;
      }

      int digitsNum = (len - i) << 1;
      char digit = (char) ((data[len - 1] & 0xF0) >>> 4);
      if (digit > 9) {
        StringBuilder sb = threadLocalStringBuilder.get();
        throw new IOException(
            sb.delete(0, sb.length())
                .append(INVALID_DIGIT_AT_POSITION)
                .append(digitsNum - 1)
                .toString());
      }
      char[] digits = new char[digitsNum];
      int pos = 2 * (len - i) - 1;
      digits[pos--] = Character.forDigit(digit, 10);
      // Starts at the end of the data and ends before the last byte scale
      for (int j = len - 2; j > scaleSize - 1; j--) {
        digit = (char) (data[j] & 0x0F);
        if (digit > 9) {
          StringBuilder sb = threadLocalStringBuilder.get();
          throw new IOException(
              sb.delete(0, sb.length()).append(INVALID_DIGIT_AT_POSITION).append(pos).toString());
        }
        digits[pos--] = Character.forDigit(digit, 10);
        digit = (char) ((data[j] & 0xF0) >>> 4);
        if (digit > 9) {
          StringBuilder sb = threadLocalStringBuilder.get();
          throw new IOException(
              sb.delete(0, sb.length())
                  .append(INVALID_DIGIT_AT_POSITION)
                  .append(pos - 1)
                  .toString());
        }
        digits[pos--] = Character.forDigit(digit, 10);
      }

      StringBuilder sb = new StringBuilder(len - i + 1);
      if (signum < 0) {
        sb.append('-');
      }

      pos = 0;
      while (digits[pos] == 0) {
        pos++;
      }
      for (; pos < digits.length; pos++) {
        sb.append(digits[pos]);
      }

      return new BigDecimal(new BigInteger(sb.toString()), scale);
    }
  }
}
