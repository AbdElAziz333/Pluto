package com.abdelaziz.pluto.mixin.network.flushconsolidation;

import com.abdelaziz.pluto.common.network.ConfigurableAutoFlush;
import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimizes ClientConnection by adding the ability to skip auto-flushing and using void promises where possible.
 */
@Mixin(Connection.class)
public abstract class ConnectionMixin implements ConfigurableAutoFlush {
    @Shadow
    private Channel channel;

    private AtomicBoolean autoFlush;

    @Shadow
    public abstract void setProtocol(ConnectionProtocol state);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAddedFields(CallbackInfo ci) {
        this.autoFlush = new AtomicBoolean(true);
    }

    /*
     *        @Inject(method = "sendImmediately", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;packetsSentCounter:I"))
     * 	private void checkPacket(Packet<?> packet, PacketCallbacks callback, CallbackInfo ci) {
     * 		if (this.packetListener instanceof PacketCallbackListener) {
     * 			((PacketCallbackListener) this.packetListener).sent(packet);
     *        }
     *    }
     * */

    /**
     * Refactored sendImmediately method. This is a better fit for {@code @Overwrite} but we have to write it this way
     * because the fabric-networking-api-v1 also mixes into this...
     *
     * @author Andrew Steinborn
     */
    @Inject(method = "sendPacket",
            at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;sentPackets:I",
                    opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void sendImmediately$rewrite(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo info, ConnectionProtocol packetState, ConnectionProtocol protocolState) {
        boolean newState = packetState != protocolState;

        if (this.channel.eventLoop().inEventLoop()) {
            if (newState) {
                this.setProtocol(packetState);
            }
            doSendPacket(packet, callback);
        } else {
            // Note: In newer versions of Netty, we could use AbstractEventExecutor.LazyRunnable to avoid a wakeup.
            // This has the advantage of requiring slightly less code.
            // However, in practice, (almost) every write will use a WriteTask which doesn't wake up the event loop.
            // The only exceptions are transitioning states (very rare) and when a listener is provided (but this is
            // only upon disconnect of a client). So we can sit back and enjoy the GC savings.
            if (!newState && callback == null) {
                ChannelPromise voidPromise = this.channel.voidPromise();
                if (this.autoFlush.get()) {
                    this.channel.writeAndFlush(packet, voidPromise);
                } else {
                    this.channel.write(packet, voidPromise);
                }
            } else {
                // Fallback.
                if (newState) {
                    this.channel.config().setAutoRead(false);
                }

                this.channel.eventLoop().execute(() -> {
                    if (newState) {
                        this.setProtocol(packetState);
                    }
                    doSendPacket(packet, callback);
                });
            }
        }

        info.cancel();
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(Connection clientConnection) {
        return null;
    }

    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        if (callback == null) {
            this.channel.write(packet, this.channel.voidPromise());
        } else {
            ChannelFuture channelFuture = this.channel.write(packet);
            channelFuture.addListener(callback);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        if (this.autoFlush.get()) {
            this.channel.flush();
        }

        if (this.autoFlush.get()) {
            this.channel.flush();
        }
    }

    @Override
    public void setShouldAutoFlush(boolean shouldAutoFlush) {
        boolean prev = this.autoFlush.getAndSet(shouldAutoFlush);
        if (!prev && shouldAutoFlush) {
            this.channel.flush();
        }
    }
}