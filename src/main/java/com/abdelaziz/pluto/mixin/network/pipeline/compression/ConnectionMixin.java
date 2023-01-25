package com.abdelaziz.pluto.mixin.network.pipeline.compression;

import com.abdelaziz.pluto.common.network.compression.MinecraftCompressDecoder;
import com.abdelaziz.pluto.common.network.compression.MinecraftCompressEncoder;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Mixin(Connection.class)
public class ConnectionMixin {
    private static Constructor<?> pluto_viaEventConstructor;

    static {
        pluto_findViaEvent();
    }

    @Shadow
    private Channel channel;

    private static void pluto_findViaEvent() {
        // ViaFabric compatibility
        try {
            pluto_viaEventConstructor =
                    Class.forName("com.viaversion.fabric.common.handler.PipelineReorderEvent").getConstructor();
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }
    }

    @Inject(method = "setupCompression", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int compressionThreshold, boolean validate, CallbackInfo ci) {
        if (compressionThreshold == -1) {
            this.channel.pipeline().remove("decompress");
            this.channel.pipeline().remove("compress");
        } else {
            MinecraftCompressDecoder decoder = (MinecraftCompressDecoder) channel.pipeline()
                    .get("decompress");
            MinecraftCompressEncoder encoder = (MinecraftCompressEncoder) channel.pipeline()
                    .get("compress");
            if (decoder != null && encoder != null) {
                decoder.setThreshold(compressionThreshold);
                encoder.setThreshold(compressionThreshold);
            } else {
                VelocityCompressor compressor = Natives.compress.get().create(4);

                encoder = new MinecraftCompressEncoder(compressionThreshold, compressor);
                decoder = new MinecraftCompressDecoder(compressionThreshold, validate, compressor);

                channel.pipeline().addBefore("decoder", "decompress", decoder);
                channel.pipeline().addBefore("encoder", "compress", encoder);
            }
        }

        this.handleViaCompression();

        ci.cancel();
    }

    private void handleViaCompression() {
        if (pluto_viaEventConstructor == null) return;
        try {
            this.channel.pipeline().fireUserEventTriggered(pluto_viaEventConstructor.newInstance());
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
