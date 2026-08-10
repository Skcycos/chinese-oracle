package com.tanrunn.chineseoracle.server.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central permission check. Uses LuckPerms when available (fabric-permissions-api
 * semantics on NeoForge via LuckPerms), otherwise falls back to OP levels.
 */
public final class PermissionManager {
    private static boolean luckPermsChecked;
    private static boolean luckPermsPresent;

    private PermissionManager() {
    }

    public static boolean hasPermission(CommandSourceStack source, String node, int opLevel) {
        if (source.getEntity() == null) return true; // console / command block
        if (source.getEntity() instanceof ServerPlayer player) {
            return hasPermission(player, node, opLevel);
        }
        return source.hasPermission(opLevel);
    }

    public static boolean hasPermission(ServerPlayer player, String node, int opLevel) {
        if (node != null && !node.isBlank() && isLuckPermsPresent() && LuckPermsBridge.hasPermission(player, node)) {
            return true;
        }
        return player.hasPermissions(opLevel);
    }

    private static boolean isLuckPermsPresent() {
        if (!luckPermsChecked) {
            try {
                Class.forName("net.luckperms.api.LuckPermsProvider");
                luckPermsPresent = true;
            } catch (ClassNotFoundException e) {
                luckPermsPresent = false;
            }
            luckPermsChecked = true;
        }
        return luckPermsPresent;
    }
}
