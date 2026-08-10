package com.tanrunn.chineseoracle.common;

public record Modifier(
        float miningSpeedMul,
        float oreBonusChance,
        float outgoingDamageMul,
        float incomingDamageMul,
        float fishingLuckBonus,
        float tradePriceMul,
        float moveSpeedMul,
        float buildSpeedMul,
        float restHeal) {

    public static Modifier neutral() {
        return new Modifier(1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 0f);
    }
}
