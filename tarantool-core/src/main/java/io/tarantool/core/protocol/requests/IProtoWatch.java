/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.IPROTO_TYPE_WATCH;
import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_EVENT_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_EVENT_KEY;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_IPROTO_TYPE_WATCH;
import static io.tarantool.core.protocol.requests.IProtoConstant.RAW_MAP_HEADER_WITH_ONE_ITEM;

public class IProtoWatch extends IProtoBaseRequest {

  protected final String eventKey;

  public IProtoWatch(String eventKey) {
    this.eventKey = eventKey;
  }

  @Override
  public byte[] getPacket(MessageBufferPacker packer) throws IOException {
    preparePacker(packer);
    packer.addPayload(RAW_MAP_HEADER_WITH_ONE_ITEM);

    packer.addPayload(RAW_IPROTO_EVENT_KEY); // key
    packer.packString(eventKey); // value

    return getPacketFromBase(packer);
  }

  @Override
  public MapValue getBody() {
    Map<Value, Value> map = new HashMap<>();
    map.put(MP_IPROTO_EVENT_KEY, ValueFactory.newString(eventKey));
    return ValueFactory.newMap(map);
  }

  @Override
  public boolean hasSyncId() {
    return false;
  }

  @Override
  protected byte[] getRequestTypeRaw() {
    return RAW_IPROTO_TYPE_WATCH;
  }

  @Override
  public int getRequestType() {
    return IPROTO_TYPE_WATCH;
  }

  @Override
  public String toString() {
    if (this.stringBuilder == null) {
      this.stringBuilder = new StringBuilder();
      this.stringBuilder.append("IProtoWatch(key = ").append(eventKey).append(")");
    }
    return this.stringBuilder.toString();
  }
}
