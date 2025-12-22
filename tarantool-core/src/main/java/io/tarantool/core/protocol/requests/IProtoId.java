/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_FEATURES;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_VERSION;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_FEATURES;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_ID;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_VERSION;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_TWO_ITEMS;

public class IProtoId extends IProtoBaseRequest {

  private final int protocolVersion;
  private final List<Integer> features;

  public IProtoId(int protocolVersion, List<Integer> features) {
    this.protocolVersion = protocolVersion;
    this.features = features;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_TWO_ITEMS);

    packer.addPayload(RAW_IPROTO_VERSION);  // key
    packer.packInt(protocolVersion);       // value

    packer.addPayload(RAW_IPROTO_FEATURES);
    packer.packValue(ValueFactory.newArray(
        features
            .stream()
            .map(ValueFactory::newInteger)
            .collect(Collectors.toList())
    ));

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_VERSION, ValueFactory.newInteger(protocolVersion));
    map.put(MP_IPROTO_FEATURES, ValueFactory.newArray(
        features
            .stream()
            .map(ValueFactory::newInteger)
            .collect(Collectors.toList())
    ));
    return ValueFactory.newMap(map);
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_ID;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_ID;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder
          .append("IProtoId(syncId = ")
          .append(getSyncId())
          .append(", version = ")
          .append(protocolVersion)
          .append(", features = ")
          .append(features)
          .append(")");
    }
    return this.stringBuilder.toString();
  }
}
