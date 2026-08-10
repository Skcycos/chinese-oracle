package com.tanrunn.chineseoracle.client.network;

import com.tanrunn.chineseoracle.common.network.AuiHandshakeC2S;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the ApricityUI presence handshake to the server on world join, so the
 * server can skip the chat display and push the fortune screen payload instead.
 */
@EventBusSubscriber(modid = "chinese_oracle", value = Dist.CLIENT)
public final class AuiHandshakeSender {
    private AuiHandshakeSender() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (event.getPlayer() == null || event.getConnection() == null) return;
        boolean hasAui = ModList.get().isLoaded("apricityui");
        PacketDistributor.sendToServer(new AuiHandshakeC2S(hasAui));
    }
}
