package com.tanrunn.chineseoracle.network;

import com.tanrunn.chineseoracle.client.network.ClientPayloadHandler;
import com.tanrunn.chineseoracle.common.network.AuiHandshakeC2S;
import com.tanrunn.chineseoracle.common.network.FortuneDisplay;
import com.tanrunn.chineseoracle.server.network.ServerPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(FortuneDisplay.TYPE, FortuneDisplay.STREAM_CODEC, ClientPayloadHandler::handle);
        registrar.playToServer(AuiHandshakeC2S.TYPE, AuiHandshakeC2S.STREAM_CODEC, ServerPayloadHandler::handle);
    }
}
