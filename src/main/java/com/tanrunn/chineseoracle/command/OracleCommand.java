package com.tanrunn.chineseoracle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tanrunn.chineseoracle.ChineseOracleMod;
import com.tanrunn.chineseoracle.Config;
import com.tanrunn.chineseoracle.api.ChineseOracleApi;
import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.server.fortune.DayService;
import com.tanrunn.chineseoracle.server.fortune.FortuneService;
import com.tanrunn.chineseoracle.server.permission.PermissionManager;
import com.tanrunn.chineseoracle.server.permission.PermissionNodes;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import com.tanrunn.chineseoracle.server.registry.SignEntry;
import com.tanrunn.chineseoracle.server.registry.TierData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// /oracle command tree (design document section 3.7).
public final class OracleCommand {
    private static final Map<UUID, Long> LAST_SHARE = new HashMap<>();

    private OracleCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var oracle = dispatcher.register(Commands.literal("oracle")
                .executes(ctx -> showSelf(ctx))
                .then(Commands.literal("me")
                        .executes(ctx -> showSelf(ctx)))
                .then(Commands.literal("share")
                        .executes(ctx -> share(ctx)))
                .then(Commands.literal("player")
                        .requires(src -> PermissionManager.hasPermission(src, PermissionNodes.CMD_OTHERS, 2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> show(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("set")
                        .requires(src -> PermissionManager.hasPermission(src, PermissionNodes.CMD_SET, 2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("tier", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FortuneRegistry.get().tierIds(), builder))
                                        .executes(ctx -> setFortune(ctx)))))
                .then(Commands.literal("reroll")
                        .requires(src -> PermissionManager.hasPermission(src, PermissionNodes.CMD_REROLL, 2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reroll(ctx))))
                .then(Commands.literal("reload")
                        .requires(src -> PermissionManager.hasPermission(src, PermissionNodes.CMD_RELOAD, 2))
                        .executes(ctx -> reload(ctx)))
                .then(Commands.literal("debug")
                        .requires(src -> PermissionManager.hasPermission(src, PermissionNodes.CMD_OTHERS, 2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> debug(ctx)))));

        // 中文别名 /huangli（design document section 3.7）.
        dispatcher.register(Commands.literal("huangli").redirect(oracle));
    }

    public static void onPlayerLogout(ServerPlayer player) {
        LAST_SHARE.remove(player.getUUID());
    }

    /** /oracle 与 /oracle me：打开自己的今日黄历（汇聚到公开 API）。 */
    private static int showSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return ChineseOracleApi.openAlmanac(player) ? 1 : 0;
    }

    private static int share(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        long now = System.currentTimeMillis();
        Long last = LAST_SHARE.get(player.getUUID());
        int cooldownMs = Config.SHARE_COOLDOWN_SECONDS.get() * 1000;
        if (last != null && now - last < cooldownMs) {
            ctx.getSource().sendFailure(Component.literal("分享太频繁，请稍后再试。"));
            return 0;
        }
        LAST_SHARE.put(player.getUUID(), now);
        FortuneSnapshot snapshot = FortuneService.getSnapshot(player);
        FortuneRegistry registry = FortuneRegistry.get();
        TierData tier = registry.tier(snapshot.tierId());
        SignEntry sign = registry.sign(snapshot.signId());
        StringBuilder sb = new StringBuilder("§e「").append(player.getName().getString()).append("」今日运势§r：");
        sb.append(tier != null ? tier.displayName() : snapshot.tierId());
        if (snapshot.wuxing() != null) {
            sb.append("  §6五行§r：").append(snapshot.wuxing());
        }
        if (sign != null && !sign.poem().isEmpty()) {
            sb.append("  §7").append(sign.poem().get(0));
        }
        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(Component.literal(sb.toString()), false);
        return 1;
    }

    private static int show(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        FortuneSnapshot snapshot = FortuneService.getSnapshot(target);
        if (ctx.getSource().getPlayer() != null) {
            com.tanrunn.chineseoracle.server.network.FortuneNetwork.showFortune(ctx.getSource().getPlayer(), snapshot, ctx.getSource().getServer());
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(FortuneService.formatSnapshot(snapshot, ctx.getSource().getServer())), false);
        }
        return 1;
    }

    private static int setFortune(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String tier = StringArgumentType.getString(ctx, "tier");
        String oldTier = FortuneService.getSnapshot(target).tierId();
        boolean ok = FortuneService.forceSet(target, tier);
        ChineseOracleMod.LOGGER.info("oracle set: player={} op={} oldTier={} newTier={} day={}",
                target.getName().getString(), ctx.getSource().getTextName(), oldTier, tier,
                DayService.currentDayIndex(ctx.getSource().getServer()));
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal("未知吉凶等级：" + tier));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("已强制设置 " + target.getName().getString() + " 的吉凶为 " + tier), true);
        return 1;
    }

    private static int reroll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String oldTier = FortuneService.getSnapshot(target).tierId();
        boolean ok = FortuneService.reroll(target);
        ChineseOracleMod.LOGGER.info("oracle reroll: player={} op={} oldTier={} day={} ok={}",
                target.getName().getString(), ctx.getSource().getTextName(), oldTier,
                DayService.currentDayIndex(ctx.getSource().getServer()), ok);
        ctx.getSource().sendSuccess(() -> Component.literal("已为 " + target.getName().getString() + " 重抽今日签"), true);
        return ok ? 1 : 0;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        long start = System.nanoTime();
        FortuneService.reload(ctx.getSource().getServer());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        FortuneRegistry registry = FortuneRegistry.get();
        ctx.getSource().sendSuccess(() -> Component.literal("词库已重载：吉凶 " + registry.tierCount()
                + " / 宜忌 " + registry.yiJiCount() + " / 签文 " + registry.signCount()
                + " / 节日 " + registry.festivalCount()
                + "，耗时 " + elapsedMs + "ms"), true);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        FortuneSnapshot s = FortuneService.getSnapshot(target);
        ctx.getSource().sendSuccess(() -> Component.literal("day=" + s.dayIndex()
                + " tier=" + s.tierId() + " seed=" + s.seed() + " rerolls=" + s.rerollsUsed()
                + " yi=" + s.yiIds() + " ji=" + s.jiIds()
                + " sign=" + s.signId() + " wuxing=" + s.wuxing()), false);
        return 1;
    }
}
