package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Global game-day tracking. dayIndex is anchored to the overworld day time so
 * players in the Nether/End share the same logical day (section 6.1), or to a
 * real-world date for the wall_clock_* sources.
 */
public final class DayService {
    private static long lastDayIndex = Long.MIN_VALUE;

    private DayService() {
    }

    public static void init(MinecraftServer server) {
        lastDayIndex = currentDayIndex(server);
    }

    public static long currentDayIndex(MinecraftServer server) {
        return switch (Config.DAY_SOURCE.get()) {
            case "wall_clock_utc" -> LocalDate.now(ZoneOffset.UTC).toEpochDay();
            case "wall_clock_offset" -> LocalDate.now(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(Config.WALL_CLOCK_OFFSET_HOURS.get()))).toEpochDay();
            default -> server.overworld().getDayTime() / 24000;
        };
    }

    /**
     * O(1) per tick. Only when the global day actually changes do we enqueue
     * every online player for a (budgeted) rollover.
     */
    public static void checkGlobalRollover(MinecraftServer server) {
        long current = currentDayIndex(server);
        if (lastDayIndex != Long.MIN_VALUE && current != lastDayIndex) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                RolloverQueue.enqueue(player.getUUID());
            }
        }
        lastDayIndex = current;
    }
}
