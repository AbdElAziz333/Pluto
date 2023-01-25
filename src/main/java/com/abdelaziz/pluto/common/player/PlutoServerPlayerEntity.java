package com.abdelaziz.pluto.common.player;

public interface PlutoServerPlayerEntity {
    void setNeedsChunksReloaded(boolean needsChunksReloaded);

    int getPlayerViewDistance();

    boolean getNeedsChunksReloaded();
}
