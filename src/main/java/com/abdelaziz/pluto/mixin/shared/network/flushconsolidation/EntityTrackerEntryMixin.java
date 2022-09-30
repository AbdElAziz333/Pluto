package com.abdelaziz.pluto.mixin.shared.network.flushconsolidation;

import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.abdelaziz.pluto.mod.shared.network.util.AutoFlushUtil.setAutoFlush;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {

    @Inject(at = @At("HEAD"), method = "startTracking")
    public void startTracking$disableAutoFlush(ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(player, false);
    }

    @Inject(at = @At("RETURN"), method = "startTracking")
    public void startTracking$reenableAutoFlush(ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(player, true);
    }
}
