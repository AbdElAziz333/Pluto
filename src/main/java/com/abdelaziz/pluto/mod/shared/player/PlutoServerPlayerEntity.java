package com.abdelaziz.pluto.mod.shared.player;

public interface PlutoServerPlayerEntity {
    void setNeedsChunksReloaded(boolean needsChunksReloaded);

    int getPlayerViewDistance();

    boolean getNeedsChunksReloaded();
}
