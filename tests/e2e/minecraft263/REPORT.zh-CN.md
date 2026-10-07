# Minecraft Java 26.3 适配与新增保护范围审批

> 后续变更：用户已批准四项坐垫权限及两侧坐垫分组，见 [坐垫实现与验证](../cushion263/README.md)。草床也已获批并接入现有 `BED` 使用权限；层架蘑菇修正仍待确认。本报告下文保留首次版本适配时的测试与提案记录。

核验日期：2026-10-07。用户所说的「2.6.3」按官方已发布的 **26.3** 处理。
本次只增加版本运行支持；没有增加 flag，也没有改变新增内容的保护策略。
基于主分支 `93f33ad6`，包含其已有的烟花、床/重生锚保护修复。

## 已实现

- 注册 `v26_3` 版本，实现顺序和旧版本事件继承保持一致。
- 识别正式版本字符串，以及 Paper 的 `26.3.build.159-beta` API 字符串。
- 新增独立的 26.3 NMS 模块，在官方 26.3 开发包上编译展示实体、数据包发送、
  原生对话框编码和回调桥接；没有仅修改版本白名单或退回旧 NMS 实现。
- 保持 Java 25 编译工具链和 Java 21 字节码输出，与现有跨版本打包方式一致。
- Full/Lite 打包和发布平台的支持版本范围扩展到 26.3。

## 实测结果

环境：Paper `26.3-159-main@a4b87cf`、Temurin Java 25、Linux、SQLite。
测试使用绑定 `127.0.0.1` 的离线服务端和真实 TCP 协议客户端（协议 777）。
Paper 成品下载端点返回 403，因此服务端由官方同版本开发包中的 Paperclip、
Mojang 原版服务端和开发包声明的运行时依赖构建，并核对启动版本。
外部认证发现服务不可达；不影响该离线测试环境。

| 验证 | 结果 |
| --- | --- |
| 全项目自动化测试，含 26.3 原生对话框/NBT 测试 | 135 项，0 失败 |
| Full、Lite 构建 | 均成功 |
| 版本/权限/常用事件/NMS/网络回调/真实方块破坏探针 | 387 项，0 失败 |
| 真实烟花伤害矩阵 | 22 场景、136 项，0 失败 |
| 床/重生锚/TNT 实际客户端交互矩阵 | 16 场景、234 项，0 失败 |
| 领地扩缩容矩阵 | 30 场景、127 项，0 失败 |
| 最终构建扩缩容后重启检查 | 60 项数据库/缓存检查，0 失败 |
| 最终构建烟花配置重启检查 | 6 项数据库/缓存检查，0 失败 |

Full 实测 JAR：`Dominion-feat-minecraft-26.3.1-full.jar`。
SHA-256：`64f98dcd8fc908043316c949f8bcd382e9525593b6fb68bcf11d600edb700811`。
扩缩容完整矩阵最初运行于同步主分支前的版本适配构建；其后主分支仅新增烟花和
床/重生锚保护修复。最终构建再次读取该数据库并通过全部 60 项重启检查。

387 项包括每个已启用访客/环境 flag 的允许与拒绝查询，以及常用监听器测试；
这不等于逐个穷举所有 flag 的全部游戏触发方式。展示实体通过网络发送生成、
元数据、传送和移除数据包；对话框回调通过网络执行一次，重复提交被拒绝。
客户端确实发送破坏方块请求，服务端分别验证禁止时保留、允许时移除方块。

未验证：图形客户端实际渲染、Folia/Spigot 实机、经济/WorldGuard/PlaceholderAPI
外部插件组合、多服 MySQL/PostgreSQL 部署，以及全部 GUI 操作和每一种事件路径。
因此不能把上述通过结果表述为「所有功能在所有平台上均已穷尽验证」。

## 26.3 新内容核对

对比 26.2 与 26.3 的注册表：新增 **90 个方块、121 个物品、3 个实体类型**，
没有删除这三类的既有注册项。完整名称保存在 [registry-delta.json](registry-delta.json)。
新增实体为 `cushion`、`poplar_boat`、`poplar_chest_boat`。
同时检查了 Paper 26.3 的 `Cushion`、`CushionItem`、`BlockAttachedEntity`、
`StrawBedBlock`、`ShelfMushroomBlock` 源码及原版方块标签，避免只按名称推测保护范围。

### 应继续使用现有 flag

| 内容/行为 | 现有 flag 与结论 |
| --- | --- |
| 杨树门、活板门、栅栏门、按钮、压力板 | `door`、`trapdoor`、`fence_gate`、`button`、`pressure`；原版标签自动覆盖，运行时逐项允许/拒绝测试通过，不需要新增代码或 flag。 |
| 杨树置物架 | `shelf`；现有监听器自动匹配，运行时允许/拒绝测试通过。 |
| 杨树告示牌、悬挂告示牌 | `edit_sign`；沿用告示牌事件，建议继续使用现有权限。 |
| 杨树船、运输船 | `vehicle_spawn`、`vehicle_destroy`、`riding`，以及适用的容器权限；沿用 Boat/Vehicle 分类，不建议新增木种专属 flag。尚未逐项做实体交互矩阵。 |
| 杨树树苗、叶子及其他普通新方块；羊毛/混凝土台阶、楼梯 | `place`、`break`；树苗属于 saplings 标签，另受 `plant_tree` 管理。骨粉行为继续归 `fertilizer`。 |
| 新地图物品 | 没有独立的领地破坏行为，不建议新增 flag；丢弃、拾取、容器和展示框行为沿用现有控制。 |
| 坐在坐垫上 | 现有 `riding` 监听器不限制坐骑类型，适合复用；不建议另增 `cushion_interact`。 |
| 草床使用 | **需要补映射到 `bed`，本次未改。** `straw_bed` 不属于 `minecraft:beds`；运行时 `bed=false` 仍未取消草床交互。建议扩充床识别，不新增 `straw_bed` flag。使用后自行销毁属于草床机制，需要把使用和附带消耗一起考虑。 |
| 坐垫被 TNT、苦力怕、火球/凋灵之首等破坏 | 建议复用相应现有实体爆炸伤害 flag；**需要新增事件映射，本次未改**。坐垫走 Paper `EntityBreakEvent` / `EntityBreakByEntityEvent`，不能因为现有普通实体伤害监听器存在就认定已经覆盖。 |
| 层架蘑菇 `shelf_mushroom` | 放置/破坏和施肥应归现有对应 flag；其弹跳效果本身不修改领地对象，不建议单独增设权限。现有 `SHELF` 用名称包含 `SHELF` 判断，会误把蘑菇认作置物架；建议改用精确类型或后缀识别，**本次未改**，无需新增蘑菇 flag。 |

这些项目中的「需要补映射」与「误匹配修正」均作为下一步审批内容，尚未实现。
标签自动覆盖属于原有代码在新版注册表上的自然行为。

### 建议新增、等待审批的 flag

坐垫是 `BlockAttachedEntity`，不是 Bukkit `Hanging`、`ArmorStand` 或 `Vehicle`。
既有的这几类放置/拆除保护不能自动覆盖坐垫。建议按现有细分权限风格增加：

| 建议键 | 控制行为 | 建议默认值 |
| --- | --- | --- |
| `place_cushion` | 玩家放置坐垫；监听 `EntityPlaceEvent` 并限定 Cushion | false |
| `cushion_direct_break` | 玩家直接拆除坐垫 | false |
| `cushion_projectile_break` | 玩家发射的弹射物拆除坐垫 | false |
| `cushion_mob_damage` | 非玩家生物直接/通过弹射物破坏坐垫 | false |
| `cushion_environment_break` | 火、支撑变化、挤压/活塞等环境原因移除坐垫；与爆炸分开 | false |

以上名称、粒度和默认值都是提案，**尚未创建任何一个 flag**。
爆炸建议复用现有 flag，不再为每种爆炸新增一套坐垫 flag。

雷击还有额外限制：当前 `Cushion.thunderHit` 直接执行 `kill/dropItem`，
没有经过普通 `EntityBreakEvent` 路径。不能承诺单靠以上监听器即可覆盖雷击；
需要进一步核对 Paper 可取消事件或向上游补事件，再确定是否复用现有雷击策略。
不把这条尚未具备明确拦截点的路径伪装成已经解决。

## 来源与复现

- [Mojang 官方版本清单](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
- [Paper 26.3 构建列表](https://fill.papermc.io/v3/projects/paper/versions/26.3/builds)
- [Paper 26.3 build 159 开发包](https://repo.papermc.io/repository/maven-public/io/papermc/paper/dev-bundle/26.3.build.159-beta/dev-bundle-26.3.build.159-beta.zip)
- [26.3 注册表数据](https://raw.githubusercontent.com/misode/mcmeta/26.3-summary/registries/data.json)
- [26.2 注册表数据](https://raw.githubusercontent.com/misode/mcmeta/26.2-summary/registries/data.json)
- [测试步骤与覆盖边界](README.md)

## 草床后续实现

用户批准后，新增 26.3 专用 StrawBed 交互监听，将右键使用 `STRAW_BED` 接入
现有 `BED` 权限，在使用及其附带消耗之前判定。没有新增 flag，
普通床继续由原来的床标签监听处理；草床放置、拆除仍沿用原有方块权限。

验证：26.3 模块测试通过（16 项），Full 构建通过。新增两项回归测试验证
点击位置、玩家、事件传入现有 BED 权限检查，并排除其他方块、左键和无点击方块。
本次未重新运行实机草床睡眠/消耗流程；上文历史草床审计结果来自修正前。

层架蘑菇计划沿用 `place`、`break`、`fertilizer`，不应匹配置物架 `shelf`；
该误匹配修正尚未实施。
