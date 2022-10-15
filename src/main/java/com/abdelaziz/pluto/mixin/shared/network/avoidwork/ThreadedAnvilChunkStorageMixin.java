package com.abdelaziz.pluto.mixin.shared.network.avoidwork;

import com.abdelaziz.pluto.mod.shared.WorldEntityByChunkAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ChunkMap.class)
public class ThreadedAnvilChunkStorageMixin {
    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Inject(method = "playerLoadedChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/DebugPackets;sendPoiPacketsForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;)V", shift = At.Shift.AFTER, by = 1))
    public void sendChunkDataPackets$beSmart(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, LevelChunk chunk, CallbackInfo ci) {
        // Synopsis: when sending chunk data to the player, sendChunkDataPackets iterates over EVERY tracked entity in
        // the world, when it doesn't have to do so - we only need entities in the current chunk. A similar optimization
        // is present in Paper.
        final Collection<Entity> entitiesInChunk = ((WorldEntityByChunkAccess) chunk.getLevel()).getEntitiesInChunk(chunk.getPos().x, chunk.getPos().z);
        final List<Entity> attachmentsToSend = new ArrayList<>();
        final List<Entity> passengersToSend = new ArrayList<>();
        for (Entity entity : entitiesInChunk) {
            final ChunkMap.TrackedEntity entityTracker = this.entityMap.get(entity.getId());
            if (entityTracker != null) {
                entityTracker.updatePlayer(player);
                if (entity instanceof Mob && ((Mob) entity).getLeashHolder() != null) {
                    attachmentsToSend.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    passengersToSend.add(entity);
                }
            }
        }

        if (!attachmentsToSend.isEmpty()) {
            for (Entity entity : attachmentsToSend) {
                player.connection.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
            }
        }

        if (!passengersToSend.isEmpty()) {
            for (Entity entity : passengersToSend) {
                player.connection.send(new ClientboundSetPassengersPacket(entity));
            }
        }
    }

    @Redirect(method = "playerLoadedChunk", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkMap;entityMap:Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;", opcode = Opcodes.GETFIELD))
    public Int2ObjectMap<Entity> sendChunkDataPackets$nullifyRest(ChunkMap tacs) {
        return Int2ObjectMaps.emptyMap();
    }
}
