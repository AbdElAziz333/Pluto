package com.abdelaziz.pluto.mixin.server.fastchunkentityaccess;

import com.abdelaziz.pluto.mod.shared.WorldEntityByChunkAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements WorldEntityByChunkAccess {

    @Shadow
    @Final
    private ServerEntityManager<Entity> entityManager;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((WorldEntityByChunkAccess) this.entityManager.cache).getEntitiesInChunk(chunkX, chunkZ);
    }
}