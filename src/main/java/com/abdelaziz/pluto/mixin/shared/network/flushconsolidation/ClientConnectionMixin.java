package com.abdelaziz.pluto.mixin.shared.network.flushconsolidation;

import com.abdelaziz.pluto.mod.shared.network.ConfigurableAutoFlush;
import io.netty.channel.*;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
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
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements ConfigurableAutoFlush {
    @Shadow private Channel channel;
    private AtomicBoolean autoFlush;

    @Shadow public abstract void setState(NetworkState state);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAddedFields(CallbackInfo ci) {
        this.autoFlush = new AtomicBoolean(true);
    }

    /**
     * Refactored sendImmediately method. This is a better fit for {@code @Overwrite} but we have to write it this way
     * because the fabric-networking-api-v1 also mixes into this...
     *
     * @author Andrew Steinborn
     */
    @Inject(locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true,
            method = "sendImmediately",
            at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;packetsSentCounter:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void sendImmediately$rewrite(Packet<?> packet, @Nullable PacketCallbacks callback, CallbackInfo info, NetworkState packetState, NetworkState protocolState) {
        boolean newState = packetState != protocolState;

        if (this.channel.eventLoop().inEventLoop()) {
            if (newState) {
                this.setState(packetState);
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
                        this.setState(packetState);
                    }
                    doSendPacket(packet, callback);
                });
            }
        }

        info.cancel();
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(ClientConnection clientConnection) {
        return null;
    }

    private void doSendPacket(Packet<?> packet, @Nullable PacketCallbacks callback) {
        if (callback == null) {
            this.channel.write(packet, this.channel.voidPromise());
        } else {
            ChannelFuture channelFuture = this.channel.write(packet);
            channelFuture.addListener(listener -> {
                if (listener.isSuccess()) {
                    callback.onSuccess(); // onSuccess moj
                } else {
                    Packet<?> failedPacket = callback.getFailurePacket(); // onFailure moj
                    if (failedPacket != null) {
                        ChannelFuture failedChannelFuture = this.channel.writeAndFlush(failedPacket);
                        failedChannelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                }
            });
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
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
