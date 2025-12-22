/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.connection.codecs;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import io.tarantool.core.protocol.IProtoRequest;

/**
 * Converts Tarantool requests from Java objects to MessagePack frames
 *
 * @author Ivan Bannikov
 * @author Artyom Dubinin
 */
public class IProtoFrameEncoder extends MessageToByteEncoder<IProtoRequest> {

  private final MessageBufferPacker packer;

  public IProtoFrameEncoder() {
    packer = MessagePack.newDefaultBufferPacker();
  }

  @Override
  protected void encode(ChannelHandlerContext ctx,
      IProtoRequest message,
      ByteBuf buf)
      throws Exception {
    packer.clear();
    byte[] packet = message.getPacket(packer);
    buf.capacity(packet.length);
    buf.writeBytes(packet);
  }
}
