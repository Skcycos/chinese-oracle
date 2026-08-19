package com.tanrunn.chineseoracle.api;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link ChineseOracleApi#summary} 与 {@link AlmanacSummary} 的测试。
 *
 * <p>真实抽签/缓存路径依赖运行中的服务端与玩家数据，无法在单测中构造；
 * 这里覆盖集合防御性复制与不可变、非 null 约束、null/线程守卫、方法签名
 * 反射守卫以及"不直接暴露 FortuneDisplay/网络 payload"边界。</p>
 */
class AlmanacSummaryTest {

    @Test
    void collectionsAreDefensivelyCopiedAndImmutable() {
        List<String> yi = new ArrayList<>(List.of("宜一", "宜二"));
        List<String> ji = new ArrayList<>(List.of("忌一"));
        AlmanacSummary summary = new AlmanacSummary(7, "吉", 1, yi, ji, "金", "白露", "午时", true, "中秋");
        // 外部继续修改源集合不影响 record。
        yi.add("外部修改");
        ji.clear();
        assertEquals(List.of("宜一", "宜二"), summary.yiNames());
        assertEquals(List.of("忌一"), summary.jiNames());
        // record 返回的 List 不可变。
        assertThrows(UnsupportedOperationException.class, () -> summary.yiNames().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> summary.jiNames().add("x"));
    }

    @Test
    void nonNullableStringsAreNeverNull() {
        AlmanacSummary summary = new AlmanacSummary(1, null, 1, List.of(), List.of(),
                null, null, null, false, null);
        assertEquals("", summary.tierName());
        assertEquals("", summary.shichen());
        assertNull(summary.wuxing());
        assertNull(summary.solarTerm());
        assertNull(summary.festival());
        assertTrue(!summary.shichenAuspicious());
        assertEquals(1, summary.dayIndex());
        assertEquals(1, summary.tierRank());
    }

    @Test
    void summaryRejectsNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> ChineseOracleApi.summary(null));
    }

    @Test
    void summaryRejectsPlayerWithoutServerThread() {
        // mock 玩家没有服务端上下文（server 为 null），走非服务端主线程守卫。
        ServerPlayer player = mock(ServerPlayer.class);
        assertThrows(IllegalStateException.class, () -> ChineseOracleApi.summary(player));
    }

    @Test
    void summaryIsNotAFortuneDisplayOrNetworkPayload() throws Exception {
        Method method = ChineseOracleApi.class.getMethod("summary", ServerPlayer.class);
        Class<?> returnType = method.getReturnType();
        assertTrue(returnType.isRecord(), "summary 必须返回 record");
        assertEquals("AlmanacSummary", returnType.getSimpleName());
        assertFalse("FortuneDisplay".equals(returnType.getSimpleName()), "不得直接暴露 FortuneDisplay");
        assertFalse(CustomPacketPayload.class.isAssignableFrom(returnType), "摘要不得作为网络 payload 暴露");
    }

    @Test
    void summarySignatureIsPublicStaticReturningPublicRecord() throws Exception {
        Method method = ChineseOracleApi.class.getMethod("summary", ServerPlayer.class);
        assertTrue(Modifier.isPublic(method.getModifiers()), "summary 必须 public");
        assertTrue(Modifier.isStatic(method.getModifiers()), "summary 必须 static");
        Class<?> returnType = method.getReturnType();
        assertTrue(returnType.isRecord(), "summary 必须返回 record");
        assertTrue(Modifier.isPublic(returnType.getModifiers()), "返回 record 必须 public");
    }
}
