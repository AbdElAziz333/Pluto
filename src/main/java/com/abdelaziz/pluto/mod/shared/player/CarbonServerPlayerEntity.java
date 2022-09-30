package com.abdelaziz.pluto.mod.shared.player;


public interface CarbonServerPlayerEntity {
    void setNeedsChunksReloaded(boolean needsChunksReloaded);

    int getPlayerViewDistance();

    boolean getNeedsChunksReloaded();
}
