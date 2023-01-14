package com.abdelaziz.pluto.mixin.shared.network.microopt;

import com.abdelaziz.pluto.mod.shared.network.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Multiple micro-optimizations for packet writing.
 */
@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin extends ByteBuf {

    @Shadow
    @Final
    private ByteBuf source;

    @Shadow
    public abstract int writeCharSequence(CharSequence charSequence, Charset charset);

    /**
     * @author Andrew
     * @reason Use optimized VarInt byte size lookup table
     */
    @Overwrite
    public static int getVarIntSize(int value) {
        return VarIntUtil.getVarIntLength(value);
    }

    /**
     * @author Andrew
     * @reason Use {@link ByteBuf#writeCharSequence(CharSequence, Charset)} instead for improved performance along with
     * computing the byte size ahead of time with {@link ByteBufUtil#utf8Bytes(CharSequence)}
     */
    @Overwrite
    public FriendlyByteBuf writeUtf(String string, int i) {
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        if (utf8Bytes > i) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + i + ")");
        } else {
            this.writeVarInt(utf8Bytes);
            this.writeCharSequence(string, StandardCharsets.UTF_8);
            return new FriendlyByteBuf(source);
        }
    }

    /**
     * @author Andrew
     * @reason optimized VarInt writing
     */
    @Overwrite
    public FriendlyByteBuf writeVarInt(int value) {
        // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
        // that the proxy will write, to improve inlining.
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            source.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            source.writeShort(w);
        } else {
            writeVarIntFull(source, value);
        }
        return new FriendlyByteBuf(source);
    }

    private static void writeVarIntFull(ByteBuf buf, int value) {
        // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buf.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buf.writeShort(w);
        } else if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buf.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
            buf.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
            buf.writeInt(w);
            buf.writeByte(value >>> 28);
        }
    }
}
