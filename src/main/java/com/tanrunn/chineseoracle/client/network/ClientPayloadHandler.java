package com.tanrunn.chineseoracle.client.network;

import com.tanrunn.chineseoracle.client.integration.ApricityIntegration;
import com.tanrunn.chineseoracle.common.network.FortuneDisplay;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(FortuneDisplay payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (ModList.get().isLoaded("apricityui")) {
                ApricityIntegration.openFortune(payload);
            }
        });
    }
}
