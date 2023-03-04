package com.abdelaziz.pluto.common.player;

public interface PlutoServerPlayer {
    void setNeedsChunksReloaded(boolean needsChunksReloaded);

    int getPlayerViewDistance();

    boolean getNeedsChunksReloaded();
}
