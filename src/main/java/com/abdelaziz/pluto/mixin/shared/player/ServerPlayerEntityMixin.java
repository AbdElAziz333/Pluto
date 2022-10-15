package com.abdelaziz.pluto.mixin.shared.player;

import com.abdelaziz.pluto.mod.shared.player.PlutoServerPlayerEntity;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = PlutoServerPlayerEntity.class, prefix = "krypton$", unique = true))
public class ServerPlayerEntityMixin implements PlutoServerPlayerEntity {
    @Unique
    private int playerViewDistance = -1;

    @Unique
    private boolean needsChunksReloaded = false;

    @Inject(method = "updateOptions", at = @At("HEAD"))
    public void setClientSettings(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        needsChunksReloaded = (playerViewDistance != packet.viewDistance());
        playerViewDistance = packet.viewDistance();
    }

    @Override
    public boolean getNeedsChunksReloaded() {
        return needsChunksReloaded;
    }

    @Override
    public void setNeedsChunksReloaded(boolean needsChunksReloaded) {
        this.needsChunksReloaded = needsChunksReloaded;
    }

    @Override
    public int getPlayerViewDistance() {
        return playerViewDistance;
    }
}
