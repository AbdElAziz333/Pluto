package com.abdelaziz.pluto.mixin.server.fastchunkentityaccess;

import com.abdelaziz.pluto.mod.shared.WorldEntityByChunkAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements WorldEntityByChunkAccess {

    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((WorldEntityByChunkAccess) this.entityManager.sectionStorage).getEntitiesInChunk(chunkX, chunkZ);
    }
}