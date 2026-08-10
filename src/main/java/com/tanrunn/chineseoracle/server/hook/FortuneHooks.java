package com.tanrunn.chineseoracle.server.hook;

import com.tanrunn.chineseoracle.Config;
import com.tanrunn.chineseoracle.command.OracleCommand;
import com.tanrunn.chineseoracle.common.Modifier;
import com.tanrunn.chineseoracle.server.fortune.DayService;
import com.tanrunn.chineseoracle.server.fortune.Festival;
import com.tanrunn.chineseoracle.server.fortune.FortuneService;
import com.tanrunn.chineseoracle.server.fortune.RolloverQueue;
import com.tanrunn.chineseoracle.server.fortune.Shelter;
import com.tanrunn.chineseoracle.server.fortune.Shichen;
import com.tanrunn.chineseoracle.server.fortune.WuxingEffect;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration layer: lifecycle + action hooks (sections 4.2, 8.4, 9.3).
 * Every hot-path hook begins with the enabled/cache guards, and the modifier
 * read is a single map lookup.
 */
public class FortuneHooks {
    // Tracks per-player specialPriceDiff adjustments so they can be restored on close.
    private static final Map<UUID, List<MerchantOffer>> TRADED_OFFERS = new HashMap<>();
    private static final Map<UUID, List<Integer>> TRADED_DELTAS = new HashMap<>();

    // Rollover timing metrics (section 8.7).
    private static long drainAccumNs;
    private static int drainCount;
    private static long lastMetricsTick;

    private static void trackMetrics(ServerTickEvent.Post event, long elapsedNs) {
        if (!Config.METRICS.get()) return;
        drainAccumNs += elapsedNs;
        drainCount++;
        long now = event.getServer().getTickCount();
        if (now - lastMetricsTick >= 600) {
            if (drainCount > 0) {
                com.tanrunn.chineseoracle.ChineseOracleMod.LOGGER.info(
                        "oracle metrics: avg rollover drain {:.2f} us across {} drains",
                        drainAccumNs / 1000.0 / drainCount, drainCount);
            }
            drainAccumNs = 0;
            drainCount = 0;
            lastMetricsTick = now;
        }
    }
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        DayService.init(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FortuneService.ensureToday(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RolloverQueue.remove(player.getUUID());
            FortuneService.removeCache(player.getUUID());
            restoreTrades(player.getUUID());
            OracleCommand.onPlayerLogout(player);
            com.tanrunn.chineseoracle.server.network.FortuneNetwork.removeAui(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DayService.checkGlobalRollover(event.getServer());
        long start = System.nanoTime();
        RolloverQueue.drain(event.getServer());
        trackMetrics(event, System.nanoTime() - start);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_MINING.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Modifier modifier = FortuneService.getModifier(player);
        float mul = applyShelter(player, modifier.miningSpeedMul())
                * Shichen.miningBonusMul(player.server)
                * Festival.bonusMul(DayService.currentDayIndex(player.server))
                * WuxingEffect.bonusMul(FortuneService.getSnapshot(player).wuxing());
        if (mul == 1f) return;
        event.setNewSpeed(event.getNewSpeed() * mul);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_COMBAT.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Modifier modifier = FortuneService.getModifier(player);
        if (modifier.incomingDamageMul() == 1f) return;
        event.setAmount(event.getAmount() * applyShelter(player, modifier.incomingDamageMul()));
    }

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_COMBAT.get()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        Modifier modifier = FortuneService.getModifier(player);
        float mul = applyShelter(player, modifier.outgoingDamageMul())
                * WuxingEffect.bonusMul(FortuneService.getSnapshot(player).wuxing());
        if (mul == 1f) return;
        event.setNewDamage(event.getNewDamage() * mul);
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_FISHING.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Modifier modifier = FortuneService.getModifier(player);
        long dayIndex = DayService.currentDayIndex(player.server);
        float luck = modifier.fishingLuckBonus() + Shichen.fishingLuckBonus(player.server)
                + Festival.fishingLuckBonus(dayIndex)
                + WuxingEffect.fishingLuckBonus(FortuneService.getSnapshot(player).wuxing());
        if (luck == 0f) return;
        RandomSource random = player.getRandom();
        float chance = Math.min(0.8f, Math.abs(luck) * 0.30f);
        if (random.nextFloat() >= chance) return;
        if (luck > 0f) {
            event.getDrops().add(new ItemStack(Items.COD));
        } else {
            for (int i = event.getDrops().size() - 1; i >= 0; i--) {
                ItemStack stack = event.getDrops().get(i);
                if (stack.is(Items.COD) || stack.is(Items.SALMON)) {
                    event.getDrops().remove(i);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_REST.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Modifier modifier = FortuneService.getModifier(player);
        if (modifier.restHeal() > 0f) {
            player.heal(modifier.restHeal());
        } else if (modifier.restHeal() < 0f) {
            // 忌安床: 睡不安稳，醒来损失少量生命（轻惩罚，可被庇护等抵消）
            player.hurt(player.damageSources().generic(), -modifier.restHeal());
        }
    }

    @SubscribeEvent
    public static void onMerchantOpen(PlayerContainerEvent.Open event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_TRADING.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getContainer() instanceof MerchantMenu menu)) return;
        Modifier modifier = FortuneService.getModifier(player);
        if (modifier.tradePriceMul() == 1f) return;
        List<MerchantOffer> offers = new ArrayList<>(menu.getOffers());
        List<Integer> deltas = new ArrayList<>(offers.size());
        for (MerchantOffer offer : offers) {
            int current = offer.getCostA().getCount();
            int target = Math.max(1, Math.round(current * modifier.tradePriceMul()));
            int delta = target - current;
            if (delta != 0) {
                offer.addToSpecialPriceDiff(delta);
            }
            deltas.add(delta);
        }
        TRADED_OFFERS.put(player.getUUID(), offers);
        TRADED_DELTAS.put(player.getUUID(), deltas);
    }

    @SubscribeEvent
    public static void onMerchantClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreTrades(player.getUUID());
        }
    }

    private static void restoreTrades(UUID uuid) {
        List<MerchantOffer> offers = TRADED_OFFERS.remove(uuid);
        List<Integer> deltas = TRADED_DELTAS.remove(uuid);
        if (offers == null || deltas == null) return;
        int size = Math.min(offers.size(), deltas.size());
        for (int i = 0; i < size; i++) {
            int delta = deltas.get(i);
            if (delta != 0) {
                offers.get(i).addToSpecialPriceDiff(-delta);
            }
        }
    }

    /**
     * 大凶庇护（section 9.3): when the player's tier is bad enough and they are
     * inside the shelter radius, scale the penalty part toward neutral.
     */
    private static float applyShelter(ServerPlayer player, float mul) {
        if (mul == 1f) return mul;
        FortuneRegistry registry = FortuneRegistry.get();
        if (!registry.isWorseOrEqual(FortuneService.getSnapshot(player).tierId(), Config.SHELTER_BAD_TIER_MIN.get())) {
            return mul;
        }
        if (!Shelter.isSheltered(player)) return mul;
        float scale = Config.SHELTER_PENALTY_SCALE.get().floatValue();
        return mul < 1f ? 1f - (1f - mul) * scale : 1f + (mul - 1f) * scale;
    }
}
