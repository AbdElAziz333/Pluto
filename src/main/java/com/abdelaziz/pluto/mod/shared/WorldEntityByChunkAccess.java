package com.abdelaziz.pluto.mod.shared;

import net.minecraft.entity.Entity;

import java.util.Collection;

public interface WorldEntityByChunkAccess {
    Collection<Entity> getEntitiesInChunk(final int chunkX, final int chunkZ);
}
