package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.common.ActionType;
import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.common.Modifier;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import com.tanrunn.chineseoracle.server.registry.YiJiEntry;

/**
 * Precomputes the flattened modifier table from a snapshot (sections 5.2, 8.2).
 * Yi entries push positive, Ji entries flip their values to negative, and the
 * tier scale controls magnitude. Everything is clamped by the passed modifier cap.
 * Pure function: configuration is passed in explicitly.
 */
public final class ModifierTable {
    private ModifierTable() {
    }

    public static ActionType actionOf(String key) {
        return switch (key) {
            case "mining_speed", "ore_bonus" -> ActionType.MINING;
            case "outgoing_damage", "incoming_damage" -> ActionType.COMBAT;
            case "fishing_luck" -> ActionType.FISHING;
            case "trade_price" -> ActionType.TRADING;
            case "build_speed" -> ActionType.BUILDING;
            case "move_speed" -> ActionType.EXPLORE;
            case "enchant_level" -> ActionType.ENCHANTING;
            default -> null;
        };
    }

    public static Modifier build(FortuneSnapshot snapshot, FortuneRegistry registry, float modifierCap) {
        Acc acc = new Acc();
        float scale = Math.abs(registry.tierScale(snapshot.tierId()));
        for (String id : snapshot.yiIds()) {
            apply(acc, registry.yiJi(id), scale);
        }
        for (String id : snapshot.jiIds()) {
            apply(acc, registry.yiJi(id), -scale);
        }
        return acc.toModifier(modifierCap);
    }

    private static void apply(Acc acc, YiJiEntry entry, float signScale) {
        if (entry == null) return;
        entry.effects().forEach((key, value) -> {
            float v = value * signScale;
            switch (key) {
                case "mining_speed" -> acc.miningSpeedMul *= 1f + v;
                case "ore_bonus" -> acc.oreBonus += v;
                case "outgoing_damage" -> acc.outgoingDamageMul *= 1f + v;
                case "incoming_damage" -> acc.incomingDamageMul *= 1f + v;
                case "fishing_luck" -> acc.fishingLuck += v;
                case "trade_price" -> acc.tradePriceMul *= 1f + v;
            case "move_speed" -> acc.moveSpeedMul *= 1f + v;
            case "build_speed" -> acc.buildSpeedMul *= 1f + v;
            case "rest_heal" -> acc.restHeal += v;
            default -> {
            }
            }
        });
    }

    private static final class Acc {
        float miningSpeedMul = 1f;
        float oreBonus = 0f;
        float outgoingDamageMul = 1f;
        float incomingDamageMul = 1f;
        float fishingLuck = 0f;
        float tradePriceMul = 1f;
        float moveSpeedMul = 1f;
        float buildSpeedMul = 1f;
        float restHeal = 0f;

        Modifier toModifier(float cap) {
            return new Modifier(
                    clampMul(miningSpeedMul, cap),
                    clamp(oreBonus, 0f, 0.5f),
                    clampMul(outgoingDamageMul, cap),
                    clampMul(incomingDamageMul, cap),
                    clamp(fishingLuck, -2f, 2f),
                    clampMul(tradePriceMul, cap),
                    clampMul(moveSpeedMul, cap),
                    clampMul(buildSpeedMul, cap),
                    clamp(restHeal, 0f, 10f));
        }
    }

    private static float clampMul(float v, float cap) {
        return Math.max(1f - cap, Math.min(1f + cap, v));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
