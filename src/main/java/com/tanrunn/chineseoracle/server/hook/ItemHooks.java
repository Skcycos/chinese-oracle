package com.tanrunn.chineseoracle.server.hook;

import com.tanrunn.chineseoracle.ChineseOracleMod;
import com.tanrunn.chineseoracle.Config;
import com.tanrunn.chineseoracle.api.ChineseOracleApi;
import com.tanrunn.chineseoracle.server.fortune.DayService;
import com.tanrunn.chineseoracle.server.fortune.FortuneData;
import com.tanrunn.chineseoracle.server.fortune.FortuneService;
import com.tanrunn.chineseoracle.server.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Item interactions (design document section 3.6): 竹签筒/黄历书/求签台 view the
 * day's fortune; 香 (incense) consumes one item to reroll.
 */
public class ItemHooks {
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.INCENSE.get())) {
            useIncense(player, stack);
        } else if (stack.is(ModItems.BAMBOO_SIGN_TUBE.get()) || stack.is(ModItems.ALMANAC.get())) {
            showFortune(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel().getBlockState(event.getPos()).is(ModItems.FORTUNE_TABLE.get())) {
            showFortune(player);
        }
    }

    private static void showFortune(ServerPlayer player) {
        ChineseOracleApi.openAlmanac(player);
    }

    private static void useIncense(ServerPlayer player, ItemStack stack) {
        FortuneData data = player.getData(ChineseOracleMod.FORTUNE_DATA.get());
        long dayIndex = DayService.currentDayIndex(player.server);
        if (data.dayIndex != dayIndex) {
            FortuneService.ensureToday(player);
            return;
        }
        if (!Config.REROLL_REQUIRE_ITEM.get()) {
            FortuneService.reroll(player);
            return;
        }
        if (data.rerollsUsed >= Config.REROLL_MAX_PER_DAY.get()) {
            player.sendSystemMessage(Component.literal("今日改签次数已用完。"));
            return;
        }
        if (FortuneService.reroll(player)) {
            stack.shrink(1);
            player.sendSystemMessage(Component.literal("烧了一炷香，签已重排。"));
        }
    }
}
