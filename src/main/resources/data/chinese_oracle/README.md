# 黄历运势 · 数据包格式说明

命名空间：`chinese_oracle`。所有词库 JSON 均支持通过数据包覆盖/扩展，`/oracle reload` 热重载。

## fortune_tier/（可选，覆盖吉凶权重）

默认 9 等吉凶内置于代码；如需调整权重或关闭某一档，放同名 JSON：

```json
{
  "weight": 1,
  "enabled": true
}
```

- 可用的 id：`ss_da_ji, da_ji, zhong_ji, xiao_ji, ping, xiao_xiong, zhong_xiong, da_xiong, xx_da_xiong`
- 例：关闭「下下大凶」：`fortune_tier/xx_da_xiong.json` → `{ "enabled": false }`

## yi_ji/（宜忌词条）

```json
{
  "category": "yi",
  "name": "开矿",
  "weight": 1,
  "effects": {
    "mining_speed": 0.10,
    "ore_bonus": 0.05
  }
}
```

- `category`：`yi`（宜，数值取正）/ `ji`（忌，数值取负）
- `weight`：可选，抽取权重（默认 1），调大 = 更常出现（运营平衡用）
- `effects` 可用键（值 = 相对幅度，会再乘以吉凶档位缩放，最终受 `modifierCap` 钳制）：
  - `mining_speed`、`ore_bonus`（挖矿）
  - `outgoing_damage`、`incoming_damage`（战斗）
  - `fishing_luck`（钓鱼，加算）
  - `trade_price`（交易）
  - `move_speed`（探索）、`build_speed`（营造）
  - `rest_heal`（休息：>0 醒来回血，<0 醒来扣少量生命）

## signs/（签文）

```json
{
  "min_tier": "xiao_ji",
  "max_tier": "ss_da_ji",
  "poem": ["第一句", "第二句"],
  "explain": "白话解签"
}
```

- `min_tier` / `max_tier` 限定该签适用的吉凶区间（按档位 rank）
- `poem` 建议 2～4 句；`explain` 为解签文案
- `prefer_yi` / `prefer_ji`：可选，声明偏好宜忌条目 id；当日抽到匹配宜忌时，该签抽取权重 ×`signPreferWeight`（config，默认 3）

## festivals/（节日，可全量自定义）

```json
{
  "name": "春节",
  "day_of_year": 0,
  "days": 3
}
```

- `day_of_year`：起始日（0 = 立春，一游戏年 365 天）；`days`：持续天数
- 节日生效时，运势标题行显示节日名，并应用 `festivalBonus`（默认 +5%）小加成
- 内置 9 个（春节/元宵/清明/端午/七夕/中秋/重阳/冬至/除夕）；运营可用同名 JSON 覆盖或新增
