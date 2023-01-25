package com.abdelaziz.pluto.mixin.network.flushconsolidation;

import com.abdelaziz.pluto.common.network.util.AutoFlushUtil;
import com.abdelaziz.pluto.common.player.PlutoServerPlayerEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into various methods in {@code ChunkMap} to utilize flush consolidation for sending chunks all at once to the
 * client, along with loading chunks in a spiral order. Helpful for heavy server activity or flying very quickly.
 * <p>
 * Note for anyone attempting to modify this class in the future: for some reason, mojang includes both the chunk loading & chunk unloading
 * packets in the <i>same</i> method. This is why chunks must <i>always</i> be sent to the player when they leave an area.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    ServerLevel level;
    @Shadow
    int viewDistance;
    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
    @Shadow
    @Final
    private PlayerMap playerMap;
    @Shadow
    @Final
    private ChunkMap.DistanceManager distanceManager;

    @Shadow
    public static boolean isChunkInRange(int x1, int y1, int x2, int y2, int maxDistance) {
        // PAIL: isWithinEuclideanDistance(x1, y1, x2, y2, maxDistance)
        throw new AssertionError("pedantic");
    }

    /**
     * This is run on login. This method is overwritten to avoid sending duplicate chunks (which mc does by default)
     *
     * @reason optimize sending chunks
     * @author solonovamax
     */
    @Overwrite
    public void updatePlayerStatus(ServerPlayer player, boolean added) {
        boolean skipPlayer = this.skipPlayer(player);
        boolean isWatchingWorld = !this.playerMap.ignoredOrUnknown(player);

        int chunkPosX = SectionPos.blockToSectionCoord(player.getBlockX());
        int chunkPosZ = SectionPos.blockToSectionCoord(player.getBlockZ());

        AutoFlushUtil.setAutoFlush(player, false);

        try {
            if (added) {
                this.playerMap.addPlayer(ChunkPos.asLong(chunkPosX, chunkPosZ), player, skipPlayer);
                this.updatePlayerPos(player);

                if (!skipPlayer) {
                    this.distanceManager.addPlayer(SectionPos.of(player), player);
                }

                // Send spiral watch packets if added
                sendSpiralChunkWatchPackets(player);
            } else {
                SectionPos chunkSectionPos = player.getLastSectionPos();
                this.playerMap.removePlayer(chunkSectionPos.chunk().toLong(), player);

                if (isWatchingWorld) {
                    this.distanceManager.removePlayer(chunkSectionPos, player);
                }

                // Loop through & unload chunks if removed
                unloadChunks(player, chunkPosX, chunkPosZ, viewDistance);
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * @author Andrew Steinborn
     * @reason Add support for flush consolidation & optimize sending chunks
     */
    @Overwrite
    public void move(ServerPlayer player) {
        // TODO: optimize this further by only considering entities that the player is close to.
        //       use the FastChunkEntityAccess magic to do this.
        for (ChunkMap.TrackedEntity entityTracker : this.entityMap.values()) {
            if (entityTracker.entity == player) {
                entityTracker.updatePlayers(this.level.players());
            } else {
                entityTracker.updatePlayer(player);
            }
        }

        SectionPos oldPos = player.getLastSectionPos();
        SectionPos newPos = SectionPos.of(player);
        boolean isWatchingWorld = this.playerMap.ignored(player);
        boolean noChunkGen = this.skipPlayer(player);
        boolean movedSections = !oldPos.equals(newPos);

        if (movedSections || isWatchingWorld != noChunkGen) {
            this.updatePlayerPos(player);

            if (!isWatchingWorld) {
                this.distanceManager.removePlayer(oldPos, player);
            }

            if (!noChunkGen) {
                this.distanceManager.addPlayer(newPos, player);
            }

            if (!isWatchingWorld && noChunkGen) {
                this.playerMap.ignorePlayer(player);
            }

            if (isWatchingWorld && !noChunkGen) {
                this.playerMap.unIgnorePlayer(player);
            }

            long oldChunkPos = ChunkPos.asLong(oldPos.getX(), oldPos.getZ());
            long newChunkPos = ChunkPos.asLong(newPos.getX(), newPos.getZ());
            this.playerMap.updatePlayer(oldChunkPos, newChunkPos, player);
        }

        // The player *always* needs to be send chunks, as for some reason both chunk loading & unloading packets are handled
        // by the same method (why mojang)
        if (player.level == this.level)
            this.sendChunkWatchPackets(oldPos, player);
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    public void disableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayer player : level.players()) {
            AutoFlushUtil.setAutoFlush(player, false);
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    public void enableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayer player : level.players()) {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * @param player                The player
     * @param pos                   The position of the chunk to send
     * @param mutableObject         A new mutable object
     * @param oldWithinViewDistance If the chunk was previously within the player's view distance
     * @param newWithinViewDistance If the chunk is now within the player's view distance
     */
    @Shadow
    protected abstract void updateChunkTracking(ServerPlayer player, ChunkPos pos, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject,
                                                boolean oldWithinViewDistance, boolean newWithinViewDistance);

    @Shadow
    protected abstract boolean skipPlayer(ServerPlayer player);

    @Shadow
    protected abstract SectionPos updatePlayerPos(ServerPlayer serverPlayerEntity);

    private void sendChunkWatchPackets(SectionPos oldPos, ServerPlayer player) {
        AutoFlushUtil.setAutoFlush(player, false);
        try {
            int oldChunkX = oldPos.x();
            int oldChunkZ = oldPos.z();

            int newChunkX = SectionPos.blockToSectionCoord(player.getBlockX());
            int newChunkZ = SectionPos.blockToSectionCoord(player.getBlockZ());

            int playerViewDistance = getPlayerViewDistance(player); // +1 for buffer

            if (shouldReloadAllChunks(player)) { // Player updated view distance, unload chunks & resend (only unload chunks not visible)
                //noinspection InstanceofIncompatibleInterface
                if (player instanceof PlutoServerPlayerEntity plutoPlayer)
                    plutoPlayer.setNeedsChunksReloaded(false);

                for (int curX = newChunkX - viewDistance - 1; curX <= newChunkX + viewDistance + 1; ++curX) {
                    for (int curZ = newChunkZ - viewDistance - 1; curZ <= newChunkZ + viewDistance + 1; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inNew = isChunkInRange(curX, curZ, newChunkX, newChunkZ, playerViewDistance);

                        this.updateChunkTracking(player, chunkPos, new MutableObject<>(), true, inNew);
                    }
                }

                // Send new chunks
                sendSpiralChunkWatchPackets(player);
            } else if (Math.abs(oldChunkX - newChunkX) > playerViewDistance * 2 ||
                       Math.abs(oldChunkZ - newChunkZ) > playerViewDistance * 2) {
                // If the player is not near the old chunks, send all new chunks & unload old chunks

                // Unload previous chunks
                // Chunk unload packets are very light, so we can just do it like this
                unloadChunks(player, oldChunkX, oldChunkZ, viewDistance);

                // Send new chunks
                sendSpiralChunkWatchPackets(player);
            } else {
                int minSendChunkX = Math.min(newChunkX, oldChunkX) - playerViewDistance - 1;
                int minSendChunkZ = Math.min(newChunkZ, oldChunkZ) - playerViewDistance - 1;
                int maxSendChunkX = Math.max(newChunkX, oldChunkX) + playerViewDistance + 1;
                int maxSendChunkZ = Math.max(newChunkZ, oldChunkZ) + playerViewDistance + 1;

                // We're sending *all* chunks in the range of where the player was, to where the player currently is.
                // This is because the #updateChunkTracking method will also unload chunks.
                // For chunks outside of the view distance, it does nothing.
                for (int curX = minSendChunkX; curX <= maxSendChunkX; ++curX) {
                    for (int curZ = minSendChunkZ; curZ <= maxSendChunkZ; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inOld = isChunkInRange(curX, curZ, oldChunkX, oldChunkZ, playerViewDistance);
                        boolean inNew = isChunkInRange(curX, curZ, newChunkX, newChunkZ, playerViewDistance);
                        this.updateChunkTracking(player, chunkPos, new MutableObject<>(), inOld, inNew);
                    }
                }
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * Sends watch packets to the client in a spiral for a player, which has *no* chunks loaded in the area.
     */
    private void sendSpiralChunkWatchPackets(ServerPlayer player) {
        int chunkPosX = SectionPos.blockToSectionCoord(player.getBlockX());
        int chunkPosZ = SectionPos.blockToSectionCoord(player.getBlockZ());


        // + 1 because mc adds 1 when it sends chunks
        int playerViewDistance = getPlayerViewDistance(player) + 1;

        int x = 0, z = 0, dx = 0, dz = -1;
        int t = playerViewDistance * 2;
        int maxI = t * t * 2;
        for (int i = 0; i < maxI; i++) {
            if ((-playerViewDistance <= x) && (x <= playerViewDistance) && (-playerViewDistance <= z) && (z <= playerViewDistance)) {
                boolean inNew = isChunkInRange(chunkPosX, chunkPosZ, chunkPosX + x, chunkPosZ + z, playerViewDistance);

                this.updateChunkTracking(player,
                        new ChunkPos(chunkPosX + x, chunkPosZ + z),
                        new MutableObject<>(), false, inNew
                );
            }
            if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                t = dx;
                dx = -dz;
                dz = t;
            }
            x += dx;
            z += dz;
        }
    }

    private void unloadChunks(ServerPlayer player, int chunkPosX, int chunkPosZ, int distance) {
        for (int curX = chunkPosX - distance - 1; curX <= chunkPosX + distance + 1; ++curX) {
            for (int curZ = chunkPosZ - distance - 1; curZ <= chunkPosZ + distance + 1; ++curZ) {
                ChunkPos chunkPos = new ChunkPos(curX, curZ);

                this.updateChunkTracking(player, chunkPos, new MutableObject<>(), true, false);
            }
        }
    }

    private int getPlayerViewDistance(ServerPlayer playerEntity) {
        //noinspection InstanceofIncompatibleInterface
        return playerEntity instanceof PlutoServerPlayerEntity plutoPlayerEntity
                ? plutoPlayerEntity.getPlayerViewDistance() != -1
                // if -1, the view distance hasn't been set
                // We *actually* need to send view distance + 1, because mc doesn't render chunks adjacent to unloaded ones
                ? Math.min(this.viewDistance,
                plutoPlayerEntity.getPlayerViewDistance() +
                        1)
                : this.viewDistance : this.viewDistance;
    }

    private boolean shouldReloadAllChunks(ServerPlayer playerEntity) {
        //noinspection InstanceofIncompatibleInterface
        return playerEntity instanceof PlutoServerPlayerEntity plutoPlayerEntity && plutoPlayerEntity.getNeedsChunksReloaded();
    }
}
