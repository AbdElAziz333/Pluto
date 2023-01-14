package com.abdelaziz.pluto.mod.shared.network.util;

import com.abdelaziz.pluto.mod.shared.network.ConfigurableAutoFlush;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

public class AutoFlushUtil {
    public static void setAutoFlush(ServerPlayer player, boolean val) {
        if (player.getClass() == ServerPlayer.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) player.connection.getConnection());
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    public static void setAutoFlush(Connection conn, boolean val) {
        if (conn.getClass() == Connection.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) conn);
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    private AutoFlushUtil() {
    }
}
