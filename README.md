# 黄历运势 (Chinese Oracle)

一款基于中国民俗黄历文化的 Minecraft **每日运势** Mod（NeoForge / MC 1.21.1）。

每位玩家每天获得一份「今日签」——吉凶、宜忌、签文，并轻度影响挖矿、战斗、交易、钓鱼等行为。逻辑**服务端权威**，事件驱动，为多人服务器日切并发负载设计。

## 技术栈

- Minecraft 1.21.1 / NeoForge 21.1.x / Java 21
- Mod ID：`chinese_oracle`
- 包名：`com.tanrunn.chineseoracle`

## 目录结构

```
src/main/java/com/tanrunn/chineseoracle/
├── ChineseOracleMod.java          # 主类
├── ChineseOracleModClient.java    # 客户端入口（配置界面）
├── Config.java                    # 服务端配置（ModConfigSpec）
└── command/
    └── OracleCommand.java         # /oracle 命令骨架
```

## 开发

```bash
./gradlew build            # 编译 + 打包
./gradlew runClient        # 启动客户端
./gradlew runServer        # 启动服务端
./gradlew --refresh-dependencies   # 刷新依赖缓存
```

Mapping 名称默认使用 Mojang 官方映射（NeoForm），见
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## 路线图

见设计文档 `国风每日运势-Mod设计文档.md`（Phase 0 工程骨架 → Phase 1 服务器 MVP）。

## 许可证

All Rights Reserved。
