package com.tanrunn.chineseoracle.server.network;

import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.server.fortune.FortuneService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fortune display routing (design document 7.2): clients that announced
 * ApricityUI via the handshake get the screen payload; everyone else keeps the
 * chat display. Without the client mod the payload is simply ignored.
 */
public final class FortuneNetwork {
    private static final Set<UUID> AUI_CLIENTS = new HashSet<>();

    private FortuneNetwork() {
    }

    public static void setAui(UUID uuid, boolean hasAui) {
        if (hasAui) {
            AUI_CLIENTS.add(uuid);
        } else {
            AUI_CLIENTS.remove(uuid);
        }
    }

    public static void removeAui(UUID uuid) {
        AUI_CLIENTS.remove(uuid);
    }

    public static void showFortune(ServerPlayer viewer, FortuneSnapshot snapshot, MinecraftServer server) {
        if (AUI_CLIENTS.contains(viewer.getUUID())) {
            PacketDistributor.sendToPlayer(viewer, FortuneService.toDisplay(viewer, snapshot));
        } else {
            viewer.sendSystemMessage(Component.literal(FortuneService.formatSnapshot(snapshot, server)));
        }
    }
}
