package com.abdelaziz.pluto.mod.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.PacketByteBuf;

public class MinecraftCompressEncoder extends MessageToByteEncoder<ByteBuf> {

  private int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressEncoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    PacketByteBuf wrappedBuf = new PacketByteBuf(out);
    int uncompressed = msg.readableBytes();
    if (uncompressed < threshold) {
      // Under the threshold, there is nothing to do.
      wrappedBuf.writeVarInt(0);
      out.writeBytes(msg);
    } else {
      wrappedBuf.writeVarInt(uncompressed);
      ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, msg);
      try {
        compressor.deflate(compatibleIn, out);
      } finally {
        compatibleIn.release();
      }
    }
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
      throws Exception {
    // We allocate bytes to be compressed plus 1 byte. This covers two cases:
    //
    // - Compression
    //    According to https://github.com/ebiggers/libdeflate/blob/master/libdeflate.h#L103,
    //    if the data compresses well (and we do not have some pathological case) then the maximum
    //    size the compressed size will ever be is the input size minus one.
    // - Uncompressed
    //    This is fairly obvious - we will then have one more than the uncompressed size.
    int initialBufferSize = msg.readableBytes() + 1;
    return MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
