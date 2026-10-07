# 26.3 坐垫权限验证

核验日期：2026-10-07。环境为 Paper 26.3 build 159、Java 25、SQLite。

| Flag | 范围 | 分组 |
| --- | --- | --- |
| `cushion_place` | 玩家放置 | 玩家权限：建筑、装饰、坐垫 |
| `cushion_break` | 玩家直接拆除及玩家弹射物 | 玩家权限：建筑、装饰、坐垫 |
| `cushion_mob_damage` | 生物直接破坏及生物弹射物 | 环境设置：实体保护、坐垫 |
| `cushion_environment_break` | 火、失去支撑、活塞、发射器及来源不明的弹射物 | 环境设置：实体保护、坐垫 |

四项默认关闭。已识别的爆炸继续使用各来源已有的爆炸权限；未知爆炸归环境移除。
英文显示名称均以 Cushion 开头，不再单设弹射物拆除权限。

`flags.yml` schema 5 → 6 采用一次性追加迁移，保留管理员自定义分组、成员顺序和图标；
不会重建管理员删除的原有分类。新建两侧坐垫分组，已有同名分组则追加成员。
升级后再自行删改分组不会在后续启动时被恢复。旧数据库新增的四项权限为 false；
已有其他权限及后续修改的坐垫权限不受重复初始化影响。

## 结果

- 全量单元测试：145 项，0 失败。新增覆盖来源分类、分组迁移及 SQLite 四表字段迁移。
- 实机运行：41 项，0 失败，见 `RESULTS.txt`。
- 重启持久化：18 项，0 失败，见 `RESULTS.txt`。
- Full 构建成功。

放置和玩家直接拆除通过 TCP 协议客户端发送真实交互包。
伤害测试调用真实 NMS 坐垫伤害入口，覆盖玩家箭、生物近战、生物箭、无主箭、
带发射器来源的箭、火和 TNT，并分别核验权限开启与关闭；不等同于实际弹射物飞行测试。
失去支撑通过移除地板后执行实体 tick 验证。活塞使用真实红石驱动伸出。
Paper 的活塞移动路径不触发坐垫 EntityBreakEvent，因此额外在活塞伸缩事件前检查保护区域。
伸出已实测；回缩、蜂蜜搬运几何及 Folia 多区域调度未实机覆盖。

限制：Paper 26.3 雷击移除坐垫没有对应可取消的 EntityBreakEvent，本次权限不覆盖雷击；
界面文案也注明此范围。未验证客户端渲染。本次没有修改草床或架子蘑菇的权限策略。

## 复现

仅使用一次性测试服：测试会创建领地、改动方块和权限。

```sh
./gradlew test
./gradlew shadowJar -PBuildFull=true
./gradlew -I tests/e2e/cushion263/init.gradle cushion263HarnessJar
```

安装 Full JAR 与 `build/cushion263/Cushion263.jar`，使用旧 schema 5 数据和配置启动。
为验证自定义项保留，在旧配置设置 `groups.privilege.custom-preserved.flags: [place]`，
以及 `groups.privilege.building.material: EMERALD`。
测试服绑定本机 25583，关闭在线验证和网络压缩；使用
`python tests/e2e/minecraft263/client.py` 登录 Probe263，控制台执行 `cushion263`。
等待 SUMMARY 后停服重启，控制台执行 `cushion263 verify`。
新运行应使用干净数据库，以免测试坐标与之前创建的领地重叠。

实机行为测试 JAR SHA-256：
`8ad342b02c77efbeb77f9e25de8d8f3508599cceaf8fd85f304d24f93fc0a55c`。
该构建后仅纠正英文显示名称为 Cushion Place / Cushion Break 和 JavaDoc，随后重新构建并运行单元测试。

最终 Full JAR SHA-256：
`40a958e7fa5b43100c6c5cf13dd186656d3d035d3327210786eee99fb75baf18`。
