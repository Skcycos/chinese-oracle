package com.tanrunn.chineseoracle.server.network;

import com.tanrunn.chineseoracle.common.network.AuiHandshakeC2S;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    public static void handle(AuiHandshakeC2S payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player) {
                FortuneNetwork.setAui(player.getUUID(), payload.hasAui());
            }
        });
    }
}
