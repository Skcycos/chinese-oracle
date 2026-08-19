package com.tanrunn.chineseoracle.api;

import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.common.network.FortuneDisplay;
import com.tanrunn.chineseoracle.server.fortune.FortuneService;
import com.tanrunn.chineseoracle.server.network.FortuneNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 黄历运势公开 API。
 *
 * <p>外部 Mod 可在服务端主线程调用本类方法。API 只依赖服务端/common 类型，
 * 不暴露客户端 AUI 类型或内部可变存储对象。</p>
 */
public final class ChineseOracleApi {
    private ChineseOracleApi() {
    }

    /**
     * 为玩家打开今日黄历（服务端权威）。
     *
     * <p>复用 {@link FortuneService#getSnapshot} 的黄历生成/读取逻辑与
     * {@link FortuneNetwork#showFortune} 展示链路：AUI 客户端显示黄历界面，
     * 无 AUI 时聊天降级。必须在服务端主线程调用。</p>
     *
     * @param player 目标玩家
     * @return true 表示打开请求已接受；player 为 null、不在服务端主线程或
     *         网络未就绪时返回 false。
     */
    public static boolean openAlmanac(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        MinecraftServer server = player.server;
        if (server == null || !server.isSameThread()) {
            return false;
        }
        if (player.connection == null || player.connection.getConnection() == null
                || !player.connection.getConnection().isConnected()) {
            return false;
        }
        FortuneNetwork.showFortune(player, FortuneService.getSnapshot(player), server);
        return true;
    }

    /**
     * 服务端只读的今日黄历摘要（展示就绪名称）。
     *
     * <p>复用 {@link FortuneService#getSnapshot} 与 {@link FortuneService#toDisplay}
     * 的抽签、日期、节气、宜忌、时辰与节日解析：不复制任何业务算法，返回的
     * tierName/yiNames/jiNames 均为注册表解析后的展示名称。可能触发黄历原有的
     * "确保今日数据存在"语义（getSnapshot 内部），不改签、不分享、不降级聊天。</p>
     *
     * @param player 目标玩家（必须在线）
     * @return 只读、展示就绪的摘要
     * @throws IllegalArgumentException player 为 null
     * @throws IllegalStateException 非服务端主线程调用
     */
    public static AlmanacSummary summary(ServerPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }
        MinecraftServer server = player.server;
        if (server == null || !server.isSameThread()) {
            throw new IllegalStateException("must be called on the server thread");
        }
        FortuneSnapshot snapshot = FortuneService.getSnapshot(player);
        FortuneDisplay display = FortuneService.toDisplay(player, snapshot);
        return new AlmanacSummary(
                display.dayIndex(),
                display.tierName(),
                display.tierRank(),
                display.yiNames(),
                display.jiNames(),
                display.wuxing(),
                display.solarTerm(),
                display.shichen(),
                display.shichenAuspicious(),
                display.festival());
    }
}
