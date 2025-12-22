/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.connection.codecs;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.MapValue;

import io.tarantool.core.protocol.IProtoRawResponse;

/**
 * Converts Tarantool server responses from MessagePack frames to Java objects
 *
 * @author Ivan Bannikov
 * @author Artyom Dubinin
 */
public class IProtoFrameDecoder extends ReplayingDecoder<IProtoFrameDecoder.DecoderState> {

  protected enum DecoderState {
    LENGTH,
    BODY
  }

  private static final int MINIMAL_HEADER_SIZE = 5; // MP_UINT32
  private int size;

  public IProtoFrameDecoder() {
    super(DecoderState.LENGTH);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list)
      throws Exception {

    switch (state()) {
      case LENGTH:
        readSize(byteBuf);
        checkpoint(DecoderState.BODY);
      case BODY:
        readPacket(byteBuf, list);
        checkpoint(DecoderState.LENGTH);
        break;
      default:
        throw new Error("Shouldn't reach here.");
    }
  }

  private void readSize(ByteBuf buf) throws IOException {
    // we don't need to check readable bytes because we use replayingBuffer
    // and readBytes is non-blocking method
    byte mpSize[] = new byte[MINIMAL_HEADER_SIZE];
    buf.readBytes(mpSize, 0, MINIMAL_HEADER_SIZE);
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(mpSize)) {
      size = unpacker.unpackInt();
    }
  }

  private void readPacket(ByteBuf buf, List<Object> list) throws IOException {
    if (size <= 0) {
      return;
    }

    // we don't need to check readable bytes because we use replayingBuffer
    // and readBytes is non-blocking method
    byte packet[] = new byte[size];
    buf.readBytes(packet, 0, size);
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(packet)) {
      MapValue header = unpacker.unpackValue().asMapValue();
      int read = (int) unpacker.getTotalReadBytes();
      list.add(new IProtoRawResponse(header, packet, read));
      size = 0;
    }
  }
}
