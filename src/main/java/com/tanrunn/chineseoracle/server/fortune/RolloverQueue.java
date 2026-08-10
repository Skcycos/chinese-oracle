package com.tanrunn.chineseoracle.server.fortune;

import com.mojang.logging.LogUtils;
import com.tanrunn.chineseoracle.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Budgeted day-rollover queue (design document section 6.4). Spreads the
 * day-change spike across ticks instead of drawing for everyone at once.
 */
public final class RolloverQueue {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Deque<UUID> QUEUE = new ArrayDeque<>();

    private RolloverQueue() {
    }

    public static void enqueue(UUID uuid) {
        if (!QUEUE.contains(uuid)) {
            QUEUE.add(uuid);
        }
    }

    public static void remove(UUID uuid) {
        QUEUE.remove(uuid);
    }

    public static void drain(MinecraftServer server) {
        int budget = Config.ROLLOVER_PLAYERS_PER_TICK.get();
        int processed = 0;
        while (!QUEUE.isEmpty() && processed < budget) {
            UUID uuid = QUEUE.poll();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) continue;
            try {
                FortuneService.ensureToday(player);
                processed++;
            } catch (Exception e) {
                LOGGER.error("Failed to roll over fortune for player {}", uuid, e);
            }
        }
    }
}
