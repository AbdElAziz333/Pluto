package com.abdelaziz.pluto.common;

import net.minecraft.world.entity.Entity;

import java.util.Collection;

public interface WorldEntityByChunkAccess {
    Collection<Entity> getEntitiesInChunk(final int chunkX, final int chunkZ);
}
