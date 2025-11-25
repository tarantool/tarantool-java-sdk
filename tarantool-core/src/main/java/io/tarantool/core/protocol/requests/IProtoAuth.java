/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.StringValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_AUTH;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_USER_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TUPLE;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_AUTH;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_USER_NAME;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_TWO_ITEMS;

public class IProtoAuth extends IProtoBaseRequest {

  public static final AuthType DEFAULT_AUTH_TYPE = AuthType.CHAP_SHA1;
  private final AuthType authType;
  private final String user;
  private final String password;
  private final byte[] salt;

  public IProtoAuth(String user, String password, byte[] salt, AuthType authType) {
    this.user = user;
    this.password = password;
    this.salt = salt;
    this.authType = authType;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException, NoSuchAlgorithmException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_TWO_ITEMS);

    packer.addPayload(RAW_IPROTO_USER_NAME); // key
    packer.packString(user); // value

    packer.addPayload(RAW_IPROTO_TUPLE);

    if (authType == AuthType.PAP_SHA256) {
      packer.packValue(
          ValueFactory.newArray(authType.getRawAuthTypeName(), ValueFactory.newString(password)));
    } else {
      byte[] scramble;
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      byte[] step1 = sha1.digest(password.getBytes());
      byte[] step2 = sha1.digest(step1);
      sha1.update(salt, 0, 20);
      sha1.update(step2);
      scramble = sha1.digest();
      for (int i = 0; i < 20; i++) {
        scramble[i] ^= step1[i];
      }

      packer.packValue(
          ValueFactory.newArray(authType.getRawAuthTypeName(), ValueFactory.newBinary(scramble)));
    }

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() throws Exception {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_USER_NAME, ValueFactory.newString(user));
    if (authType == AuthType.PAP_SHA256) {
      map.put(
          MP_IPROTO_TUPLE,
          ValueFactory.newArray(
              ValueFactory.newString(authType.toString()), ValueFactory.newString(password)));
    } else {
      byte[] scramble;
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      byte[] step1 = sha1.digest(password.getBytes());
      byte[] step2 = sha1.digest(step1);
      sha1.update(salt, 0, 20);
      sha1.update(step2);
      scramble = sha1.digest();
      for (int i = 0; i < 20; i++) {
        scramble[i] ^= step1[i];
      }

      map.put(
          MP_IPROTO_TUPLE,
          ValueFactory.newArray(
              ValueFactory.newString(authType.toString()), ValueFactory.newBinary(scramble)));
    }

    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_AUTH;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_AUTH;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoAuth(syncId = ")
          .append(getSyncId())
          .append(", user = ")
          .append(user)
          .append(", authType = ")
          .append(authType)
          .append(")");
    }
    return this.stringBuilder.toString();
  }

  public enum AuthType {
    CHAP_SHA1("chap-sha1"),
    PAP_SHA256("pap-sha256");

    private final String authTypeName;
    private final StringValue rawAuthTypeName;

    AuthType(String authTypeName) {
      this.authTypeName = authTypeName;
      this.rawAuthTypeName = ValueFactory.newString(authTypeName);
    }

    public String toString() {
      return this.authTypeName;
    }

    public StringValue getRawAuthTypeName() {
      return rawAuthTypeName;
    }
  }
}
