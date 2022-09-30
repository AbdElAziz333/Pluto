package com.abdelaziz.pluto.mixin.client.fastchunkentityaccess;

import com.abdelaziz.pluto.mod.shared.WorldEntityByChunkAccess;
import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ClientWorld.class)
@OnlyIn(Dist.CLIENT)
public class ClientWorldMixin implements WorldEntityByChunkAccess {

    @Shadow @Final private ClientEntityManager<Entity> entityManager;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((WorldEntityByChunkAccess) this.entityManager.cache).getEntitiesInChunk(chunkX, chunkZ);
    }
}
