package com.abdelaziz.pluto.mod.client;

import com.abdelaziz.pluto.mod.server.PlutoServerInitializer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PlutoClientInitializer {
    private static final Logger LOGGER = LogManager.getLogger(PlutoServerInitializer.class);

    public PlutoClientInitializer() {
        MinecraftForge.EVENT_BUS.addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Pluto is now accelerating your Minecraft client's networking stack \uD83D\uDE80");
        LOGGER.info("Note that Pluto is most effective on servers, not the client.");
    }
}
