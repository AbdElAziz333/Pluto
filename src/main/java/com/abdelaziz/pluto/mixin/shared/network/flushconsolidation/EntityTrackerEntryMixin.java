package com.abdelaziz.pluto.mixin.shared.network.flushconsolidation;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.abdelaziz.pluto.mod.shared.network.util.AutoFlushUtil.setAutoFlush;

@Mixin(ServerEntity.class)
public class EntityTrackerEntryMixin {

    @Inject(at = @At("HEAD"), method = "addPairing")
    public void startTracking$disableAutoFlush(ServerPlayer player, CallbackInfo ci) {
        setAutoFlush(player, false);
    }

    @Inject(at = @At("RETURN"), method = "addPairing")
    public void startTracking$reenableAutoFlush(ServerPlayer player, CallbackInfo ci) {
        setAutoFlush(player, true);
    }
}
