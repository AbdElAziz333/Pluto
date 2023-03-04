package com.abdelaziz.pluto.mixin.fast_entity_access;

import com.abdelaziz.pluto.common.entity.WorldEntityByChunkAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ClientLevel.class)
@OnlyIn(Dist.CLIENT)
public class ClientLevelMixin implements WorldEntityByChunkAccess {
    @Shadow
    @Final
    private TransientEntitySectionManager<Entity> entityStorage;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((WorldEntityByChunkAccess) this.entityStorage.sectionStorage).getEntitiesInChunk(chunkX, chunkZ);
    }
}