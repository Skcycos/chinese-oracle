package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * 大凶庇护（design document section 9.3): inside the respawn/bed radius,
 * penalties are reduced. Distance is only computed on demand, never per tick.
 */
public final class Shelter {
    private Shelter() {
    }

    public static boolean isSheltered(ServerPlayer player) {
        if (!Config.SHELTER_ENABLED.get()) return false;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn == null) return false;
        if (!player.getRespawnDimension().equals(player.level().dimension())) return false;
        double radius = Config.SHELTER_RADIUS.get();
        double dx = player.getX() - (respawn.getX() + 0.5);
        double dy = player.getY() - respawn.getY();
        double dz = player.getZ() - (respawn.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
