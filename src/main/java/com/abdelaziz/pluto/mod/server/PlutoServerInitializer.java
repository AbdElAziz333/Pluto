package com.abdelaziz.pluto.mod.server;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlutoServerInitializer {
    private static final Logger LOGGER = LogManager.getLogger(PlutoServerInitializer.class);

    public PlutoServerInitializer() {
        MinecraftForge.EVENT_BUS.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Pluto is now accelerating your Minecraft server's networking stack \uD83D\uDE80");
    }
}