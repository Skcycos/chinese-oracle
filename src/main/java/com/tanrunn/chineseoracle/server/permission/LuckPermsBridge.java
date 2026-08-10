package com.tanrunn.chineseoracle.server.permission;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reflection bridge to the LuckPerms API (soft dependency). If LuckPerms is not
 * installed, every call degrades to false and the caller falls back to OP levels.
 *
 * <p>NOTE: this path is best-effort and only exercised when LuckPerms is present;
 * it is not covered by the smoke tests in this project.
 */
final class LuckPermsBridge {
    private LuckPermsBridge() {
    }

    static boolean hasPermission(ServerPlayer player, String node) {
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object provider = providerClass.getMethod("get").invoke(null);
            Object userManager = provider.getClass().getMethod("getUserManager").invoke(provider);
            Object userFuture = userManager.getClass().getMethod("loadUser", UUID.class).invoke(userManager, player.getUUID());
            Object user = userFuture instanceof CompletableFuture<?> future ? future.get(3, TimeUnit.SECONDS) : userFuture;
            if (user == null) return false;
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object queryResult = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, node);
            Object result = queryResult.getClass().getMethod("result").invoke(queryResult);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }
}
