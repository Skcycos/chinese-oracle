package com.tanrunn.chineseoracle.api;

import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * 公开打开入口的守卫行为测试。
 *
 * <p>真实展示链路（AUI 界面 / 聊天降级）依赖运行中的服务端与玩家网络会话，
 * 无法在单测中构造；这里覆盖 null 与未就绪（无服务端上下文）的拒绝路径。</p>
 */
class ChineseOracleApiTest {

    @Test
    void openAlmanacRejectsNullPlayer() {
        assertFalse(ChineseOracleApi.openAlmanac(null));
    }

    @Test
    void openAlmanacRejectsPlayerWithoutServerContext() {
        ServerPlayer player = mock(ServerPlayer.class);
        assertFalse(ChineseOracleApi.openAlmanac(player));
    }
}
