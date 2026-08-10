# 黄历运势 · Chinese Oracle

> 一款把老黄历搬进 Minecraft 的每日运势 Mod —— 每天起床抽一签,宜忌吉凶,应验在一天的游戏行为里。

A Minecraft mod that brings the traditional Chinese almanac (黄历) into the game. Every day each player draws one fortune — luck tier, auspicious/avoided actions, and a fortune poem — that subtly shapes their gameplay for the day.

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| 加载器 Loader | NeoForge 21.1.x |
| Java | 21 |
| Mod ID | `chinese_oracle` |
| 依赖 Dependencies | ApricityUI (AUI, 仅客户端,可选) · LuckPerms (可选) |

---

## 这是什么 / What it does

每位玩家每天零点获得一份「今日签」,由服务端以**确定性的加权抽签**生成:

- **吉凶**(九等:上上大吉 → 下下大凶)+ **五行**
- **宜 / 忌**:数条当天建议或避免的行为,如「宜开矿」「忌安床」
- **签文 + 解签**:一段四句签诗,配一句白话解释
- **节气、时辰、节日**点缀其间,让每天的签都带着日子本身的质感

抽签结果**服务端权威**,玩家无法篡改;同一天内不同玩家签各不同,但同种子可复现,便于排查与测试。日切采用错峰队列逐玩家刷新,多人服午夜不卡 tick。

Every day at rollover each player draws a fortune, generated server-side with a **deterministic weighted draw**:

- **Luck tier** (nine grades, from 上上大吉 to 下下大凶) plus a **wuxing element** (五行)
- **Auspicious / avoided actions** (宜 / 忌) — e.g. 宜开矿 (good for mining), 忌安床 (bad for resting)
- A four-line **fortune poem** (签文) with a plain-language explanation (解签)
- Seasoned with the day's **solar term** (节气), **2-hour period** (时辰) and any **festival** (节日)

Everything is **server-authoritative**; the draw is seeded per player per day, reproducible for debugging, and day rollover is staggered so midnight never spikes the server tick.

## 玩法机制 / How it plays

### 宜忌影响 Gameplay hooks

签上的宜忌不是装饰,而是当天实实在在的修正(有上限,默认 ±15%,不会过分):

| 行为 | 影响 |
| --- | --- |
| 挖矿 / 战斗 / 交易 / 钓鱼 | 吉则加成,凶则衰减 |
| 睡觉 | 宜安床可回血;忌安床则醒来掉少量生命 |
| 节气 / 时辰 / 节日 | 当日节气与吉时小幅加持;春节、端午等节日另有加成 |

The fortune isn't decoration — it applies real, capped modifiers (±15% by default) to mining speed, combat, trading and fishing; sleeping can heal or lightly punish; solar terms, auspicious periods and festival days add small bonuses of their own.

### 大凶庇护 Bad-luck shelter

抽到「大凶」以上的签也不用绝望:回到庇护点(默认半径 48 格)内,惩罚会按比例减轻——黄历讲「趋吉避凶」,Mod 也讲。

Getting a 大凶 (great misfortune) sign isn't the end of the world: within a shelter point (default 48-block radius) penalties are scaled toward neutral — the almanac says seek good luck and dodge bad, and so does this mod.

### 改签 Rerolling

- 烧一炷**香**即可改签(每日限次、有冷却,均可配置)
- 管理员可随时 `/oracle reroll` 或 `/oracle set` 指定某人的吉凶

- Burn an **incense stick** to reroll your fortune (per-day limit and cooldown, both configurable)
- Ops can force a reroll or set a specific tier with `/oracle reroll` / `/oracle set`

## 命令 / Commands

| 命令 | 权限 | 说明 |
| --- | --- | --- |
| `/oracle` `/oracle me` | 所有人 | 查看今日签 |
| `/oracle share` | 所有人 | 把今日签分享到全服聊天(有冷却) |
| `/oracle player <玩家>` | op 2 | 查看他人今日签 |
| `/oracle set <玩家> <吉凶>` | op 2 | 强制设置某人吉凶 |
| `/oracle reroll <玩家>` | op 2 | 为某人重抽 |
| `/oracle reload` | op 2 | 热重载数据包词库 |
| `/oracle debug <玩家>` | op 2 | 输出抽签内部状态(种子等) |
| `/huangli` | — | `/oracle` 的中文别名 |

`/oracle reload` 可热重载,词库支持服务端安装、覆盖与扩展。

`/oracle reload` hot-reloads the registry — the word bank is a datapack, so servers can override or extend it.

## 数据包 / Datapack

词库全部以 JSON 数据包组织(服务端可装),结构:

```
data/chinese_oracle/
├── tiers/       # 吉凶九等(名称、权重、修正倍率)
├── yi_ji/       # 宜/忌条目(名称、影响项与数值)
├── signs/       # 签文(适用吉凶区间、签诗、解签)
└── festivals/   # 节日(名称、日期、持续天数)
```

内置词库:9 个传统节日(春节、元宵、清明、端午、七夕、中秋、重阳、冬至、除夕)、数十条宜忌与签诗。

Shipped content: 9 traditional festivals (春节, 元宵, 清明, 端午, 七夕, 中秋, 重阳, 冬至, 除夕), dozens of yi/ji entries and fortune poems.

## 安装 / Installation

1. 安装 [NeoForge 21.1.x](https://neoforged.net/)
2. 把 jar 放进 `mods/` 文件夹
3. 服务端直接用;客户端如需界面,另装 [ApricityUI (AUI)](https://github.com/Skcycos/AUI_skycos)

1. Install [NeoForge 21.1.x](https://neoforged.net/)
2. Drop the jar into `mods/`
3. Works on dedicated servers as-is; install [ApricityUI](https://github.com/Skcycos/AUI_skycos) on the client for the HTML fortune screen

> AUI 是客户端硬依赖,专用服务器不装也能跑 —— 服务端逻辑完全不依赖 UI。

> AUI is a client-side hard dependency; dedicated servers run fine without it — server logic has zero UI coupling.

## 开发 / Development

```bash
./gradlew build            # 编译 + 打包
./gradlew runClient        # 启动客户端
./gradlew runServer        # 启动服务端
./gradlew test             # 单元测试(抽签管线、修正表、节气/五行/节日)
```

```
src/main/java/com/tanrunn/chineseoracle/
├── server/fortune/        # 抽签管线、日切、五行/时辰/节日计算
├── server/registry/       # 数据包词库与热重载
├── server/hook/           # 游戏行为钩子(挖矿/战斗/交易/钓鱼/睡眠)
├── server/network/        # 服务端网络与 AUI 握手
├── client/                # 客户端入口与 AUI 集成
└── command/               # /oracle 命令树
```

设计文档《国风每日运势-Mod设计文档.md》与代码审计报告位于项目目录外侧,不随仓库发布。

The design document and audit report live outside the repo and are not distributed with it.

## 许可证 / License

**All Rights Reserved.** 保留所有权利,未经许可不得分发或修改。
