# T-20261107：项目文件夹树与工作区入口技术研究

日期 / 状态 / 负责人：2026-11-07 / 完成（文档研究；产品选择待决定） / 本聊天 Codex（内容管理 调查实现）。

关联需求：[项目内嵌套文件夹与工作区入口草案](T-20261107-folder-project-intake.md)。本报告是技术建议，不构成需求确认、ADR 或开发授权。

可行性结论：基于官方机制与当前代码，项目独立嵌套目录、树导航及主题适配具有可行实现路径，推荐沿用现有栈；尚无编译或实验结果。旧共享目录映射与操作规则是开发前置，不能直接只改 UI。

## 任务、授权与工程基线

- 目标：查明项目独立目录、嵌套树、上下文新增、项目居中及暗色品牌适配的实现路径，给出数据兼容与验证门槛。
- 授权依据：用户要求“根据新的需求草案调查技术实现”。允许静态分析、官方资料核查、报告及索引；不改业务代码、依赖或架构，不安装依赖、不做原型、登录、付费调用、真实数据操作或品牌改图。
- 验收：建议可追溯到源码或官方证据；区分事实、推测、待确认；比较沿用方案；记录兼容风险、后续验证及产品决定。平台暂按 Android 研究，Web 仅说明边界，不自行确认草案。
- 起始：`main / 708a7f0`；已有未暂存 `.gitignore`、索引、workflow、检查工具及自检，另有未跟踪 Skills、需求、研究、版本规划与实施记录；暂存为空，全部保留。当前原生版本 0.2.1 / 13，Room 2.6.1 / schema 8、Compose BOM 2024.12.01、Kotlin 2.0.21、minSdk 26 / targetSdk 35。
- 同目录版本聊天最初活跃，本聊天先只读；随后桌面查询其空闲，且实施任务声明停止写入，才开始文档写入。观察不能保证未来没有其他写入。占用仅本报告及索引本任务行；本聊天负责集成，不触及 AI 实施任务。
- 最小步骤：核对草案和链路 → 官方文档/许可 → 比较与建议 → 文档检查及保护哈希。索引前副本及既有文档、源码、设计、工具的哈希在 `/tmp/bugu-folder-research-start/`，临时目录不承诺长期保存。
- 回退：只删除本报告和索引对应行，先核对后续差异；不重置工作区、不提交或切分支。质量要求见 [quality](../quality.md)，资产保护见 [assets](../assets.md)。

## 已确认事实：静态代码证据

下列为源码结论，没有运行应用或用真实数据实测。路径均为当前工作文件。

| 证据 | 当前行为及研究意义 |
| --- | --- |
| [NotesEntities.kt](../../android/app/src/main/java/com/dspro/android/data/notes/NotesEntities.kt) 的 `FolderEntity` | 仅 `id/name/sort/archivedAt`；无项目、父节点。笔记、对话及临时草稿已有项目/文件夹字段，不能从文件夹本身还原唯一项目归属。 |
| [NotesDao.kt](../../android/app/src/main/java/com/dspro/android/data/notes/NotesDao.kt)、[NotesRepository.kt](../../android/app/src/main/java/com/dspro/android/data/notes/NotesRepository.kt) | 全局观察文件夹；创建不传项目；移动笔记到项目与移动到文件夹分别写字段，当前逻辑不能自动维护拟议的同项目不变量。删除文件夹会在事务内清除笔记、对话、临时草稿的目录引用并删除文件夹；没有子树语义。 |
| [ProjectsRepository.kt](../../android/app/src/main/java/com/dspro/android/data/notes/ProjectsRepository.kt) 的 `delete` | 将笔记和对话移到全局、删除项目记忆，再删项目；引入目录归属后必须重新规定文件夹如何处理。不能直接假设项目删除等于级联删除全部内容。 |
| [LibraryScreen.kt](../../android/app/src/main/java/com/dspro/android/ui/LibraryScreen.kt)、[DshApp.kt](../../android/app/src/main/java/com/dspro/android/ui/DshApp.kt) | 文件夹平铺，仅一级返回；主资料库按项目过滤内容但传入全局文件夹。新笔记和新对话已有当前目录入口链，新文件夹没有项目/父目录参数。顶栏项目按钮用 `weight(1f)`，两侧控件数量不等，不能保证相对整栏几何居中。 |
| [BackupService.kt](../../android/app/src/main/java/com/dspro/android/data/notes/BackupService.kt) | 业务格式 `dspro-user-backup` version 1，当前读取 databaseVersion 6..8；恢复按数据库现存列过滤 JSON 字段，再插入，已有部分关系校验。加字段不能替代旧备份的语义转换；Room 升级不会自动转换 ZIP 内 JSON。旧应用拒绝更高 databaseVersion，不能承诺新备份能在旧应用恢复。 |
| [NotesDatabase.kt](../../android/app/src/main/java/com/dspro/android/data/notes/NotesDatabase.kt) | schema 8，迁移 1→8，`exportSchema=false`；不能直接假定具备自动迁移所需历史 schema。 |
| [Components.kt](../../android/app/src/main/java/com/dspro/android/ui/Components.kt) 的 `DshWordmark`、[Theme.kt](../../android/app/src/main/java/com/dspro/android/ui/Theme.kt) | Logo 容器写死浅绿，图片固定 `bugu_title`；应用已有 `Dsh.dark` 和明暗调色板。改容器即可消除一部分割裂，但图片本身是否需要新版仍须视觉确认。 |

## 已确认事实：官方资料核查

核查日期均为 **2026-11-07**。以下 Android/SQLite 文档公开可读，无账号调用要求；适用本地 Android 数据和 UI，不代表在线服务、设备效果或本项目编译已验证。滚动文档未固定发布版，不能直接照搬最新示例依赖。

| 来源与版本范围 | 官方证据及当前项目适用性 |
| --- | --- |
| [Room 迁移指南](https://developer.android.com/training/data-storage/room/migrating-db-versions)；滚动版 | 支持手动迁移；自动迁移依赖前后 schema 导出；破坏性回退存在数据删除风险。本项目已有手动链，建议另加显式迁移和业务映射，不能启用破坏性重建。页面中的新依赖坐标不作为 2.6.1 配置。 |
| [Room 发布记录](https://developer.android.com/jetpack/androidx/releases/room)；当前稳定栏 2.8.5，历史 2.6.1 发布于 2023-11-29 | 项目仍为 2.6.1；新版维护与能力不等于当前已接入。目录建模不以升级 Room 为前提；本次不升级。 |
| [Compose 列表](https://developer.android.com/develop/ui/compose/lists)；滚动版 | LazyColumn 按需组合列表项，支持稳定 key。将可见树节点展平成列表是本报告推导的方案，不是官方提供的现成目录树组件。避免直接照用新版动画 API。 |
| [Navigation drawer](https://developer.android.com/develop/ui/compose/components/drawer)、[App bars](https://developer.android.com/develop/ui/compose/components/app-bars)；滚动版 | 提供抽屉及居中标题栏组件，可作为树容器与项目切换器容器。手机与宽屏布局仍需产品取舍及本项目版本编译核对。 |
| [Material 3 发布记录](https://developer.android.com/jetpack/androidx/releases/compose-material3)、[BOM 映射入口](https://developer.android.com/develop/ui/compose/bom/bom-mapping) | 当前稳定栏为 1.4.0，项目离线 BOM POM 对应 Material 3 为 1.3.1（本地元数据证据，非本次依赖解析结果）。基础布局足够作为候选，最新组件/签名不可推定兼容；不引入 Expressive 或 adaptive 新依赖。 |
| [暗色主题](https://developer.android.com/develop/ui/views/theming/darktheme)、[资源配置](https://developer.android.com/guide/topics/resources/providing-resources)；含 API 8 起的 night 限定符 | Android 支持随配置选择夜间资源；但应用自己的明暗状态若未改变系统 Configuration，单靠 `drawable-night` 不一定跟随。当前 Compose 可按 `Dsh.dark` 显式选择资源，需后续测试覆盖手动切换和系统主题。 |
| [SQLite WITH](https://www.sqlite.org/lang_with.html)；滚动版 | 递归 CTE 可遍历树/图。是候选查询机制，不能由文档推定所有目标设备 SQLite 版本、Room 2.6.1 查询校验和性能均通过；首选内存迭代建树可减少此验证负担。 |
| [AndroidX AppBar 官方源码](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt)、[Room3 官方源码](https://raw.githubusercontent.com/androidx/androidx/androidx-main/room3/room3-runtime/src/commonMain/kotlin/androidx/room3/RoomDatabase.kt)、[AOSP 许可说明](https://source.android.com/docs/setup/about/licenses) | 已实际打开仓库文件，文件头标注 Apache-2.0。Room3 仅作当前仓库许可证据，不作 Room 2.6.1 API 证据；旧分支 LICENSE 链接访问失败，不据此否认许可。本次另只读核对本机 Gradle 缓存的 Room runtime 2.6.1、Material3 Android 1.3.1 POM，两者均声明 Apache-2.0；未安装或解析新依赖。发布前仍须复核归档与 NOTICE。 |
| [SQLite 版权说明](https://www.sqlite.org/copyright.html) | 核心代码为公有领域；可选付费支持不代表目录功能需要服务订阅。本方案不新增云服务、账号或按量调用；不据此推定现有品牌图和其他资产具有同样授权。 |

官方事实仅支持可选机制，不能替代旧数据映射、编译、迁移或真机证据。以上每来源摘要控制在有限范围，未复制官方示例代码。

## 方案比较与建议（推测，尚未采纳）

| 候选 | 满足度 | 接入、兼容和维护成本 | 许可、数据及验证缺口 |
| --- | --- | --- | --- |
| A：沿用全局平铺，仅 UI 按关联内容过滤 | 不能满足独立空目录和嵌套；可短期保留现状 | 成本最低，但同一目录跨项目歧义仍在 | 无新增依赖；隐藏 UI 不能保证存储边界，不建议当目标实现 |
| B：现表加项目与父节点（推荐） | 能表达每项目一棵或多棵树、上下文创建 | 保留目录 ID 和内容表，新增归属校验、手动迁移、备份转换；子树查询和循环检测由应用处理 | 沿用本地栈，无新增服务费用；核心风险是历史共享目录映射，待合成升级验证 |
| C：路径字符串或闭包表 | 可支持子树检索 | 路径方案改名/移动需批量重写；闭包表需维护祖先关系及额外备份表，当前需求下成本偏高 | 不需云账号，但一致性和升级面扩大；无性能证据证明值得采用 |
| D：映射成真实文件系统/嵌入桌面式文件浏览器 | 可更接近桌面工具；超出草案 | 引入文件权限、同步、路径和数据库双源一致性问题，现有对话和备份需重新设计 | 新库许可、平台适配和维护均需另查；不为借鉴外观而复制 VS Code/Obsidian 代码、图标或商标 |

建议 B，继续单模块、手工容器、Room/Flow 与 Compose。以下为候选契约，不是已确认产品标准：

1. `FolderEntity` 增加可空 `projectId`、`parentId`，以稳定 ID 关联；`parentId=null` 表示根。是否允许全局树仍由用户决定。`projectId=null` 不同时偷偷表示“待迁移”和“全局”两种状态；如保留迁移待处理区须明确状态与入口。增加项目/父节点查询索引，排序维持同父节点范围。
2. 每个目录的父节点必须存在、同项目，且不得为自己或后代；内容的项目须等于目录项目。所有创建、导入、移动、恢复及草稿提交入口在 Repository 事务中校验，不只在 UI 过滤。普通外键存在性约束本身不能证明无环或同项目；不顺手为全部表引入级联删除。
3. 整棵项目目录按 scope 读取后建 `parentId → children` 映射；迭代遍历加 visited 检测，展开集决定可见列表，再交给 LazyColumn，key 用目录 ID。一次重建约 O(n)，显示约 O(v)，这是算法估计，未做设备性能实验。禁止无限递归、每行查数据库或纵向无限约束嵌套列表；目录数/深度和展开集恢复预算须测后确定。
4. 目录选择、展开集、返回路径按项目隔离；切项目时校验选择有效性，进入目录展示面包屑并逐级返回。失效或已归档目录不能残留为新内容的默认目标。新增动作捕获并再次校验当前项目和目录，防止弹窗期间切换导致串用。

## 旧数据与备份：开发前必须关闭的风险

**重大兼容风险**：旧文件夹可能被多个项目以及全局内容共同引用，空目录也没有归属证据。直接将全部旧目录设为某个项目会改变可见性；简单加两个空字段又可能使项目内容引用全局目录。本次不执行任何映射。

| 旧数据处理候选 | 收益与代价 | 建议 |
| --- | --- | --- |
| 保留旧共享区，用户逐步整理 | 不猜测归属；但需要长期保留共享例外，或有明确待整理入口 | 如果优先保持原结构可选；不能在 UI 中让这批数据失去入口 |
| 按实际内容 scope 拆目录 | 一个旧目录对应若干项目目录，保持内容各自 projectId，重映射 folderId；空目录另归待整理/全局 | 若目标必须立即项目隔离，优先评估此方案；增加目录数量，需保留旧 ID→新 ID 对照并先获产品确认 |
| 用户统一指定归入项目 | 规则直观 | 对多项目内容会产生冲突，除非同时明确内容迁移规则；不作默认 |

映射必须包含活跃、归档、回收站笔记与对话、临时草稿及所有持有 folderId 的对象，不能只统计首页可见内容。拆分不能顺便移动原内容的项目、改变记忆或改写已发送上下文快照；引用展示是否受目录变化影响另测。

实施时新 schema 版本按届时磁盘基线分配，不预占“9”。在现有 1→8 链之后添加显式迁移；保存映射与行数断言，避免随机不可重现映射。先导出/保护准确的旧 schema 并制定回退，不能用降版本打开已升级数据库当作回退。

备份必须单独走兼容链：校验旧 manifest → 在隔离内存/暂存中规范化版本和目录映射 → 校验父节点、同项目、无环及全部引用 → 数据库事务替换 → 提交后清理附件。对于新必填字段须填值，不能依赖当前“忽略未知列”。新库读旧备份、旧应用读新备份是不同承诺；建议旧应用明确拒绝新 schema 备份，保留升级前归档用于回退。业务 ZIP 格式版本是否升级另做兼容方案，不能仅为了让旧应用接受而伪报 databaseVersion。

删除和移动规则未确认：删除文件夹可“内容移到父级、子目录提升”或“整个子树移出/入回收站”；两种都需处理全部后代和隐藏内容。当前文件夹菜单“回收站”实际调用删除并解除关联，不能从文案推定已有目录回收站。跨项目移动必须决定是否包含整子树、是否允许单篇移出后清空 folderId，以及项目删除如何容纳原目录；所有多对象写入应事务化。建议先确认规则再开发。

## 入口、项目居中与品牌方案

| 问题 | 推荐候选 | 替代与边界 |
| --- | --- | --- |
| 查看树 | 窄屏抽屉内树、宽屏现有侧栏内树；内容区保留目录与面包屑 | 始终固定侧栏会挤占手机宽度；可改独立结构面板，取决于草案确认，不需先换导航架构 |
| 新增入口 | 工作区级新增＋选中目录行内菜单，共用同一套项目/父目录参数 | 右下 FAB 易触达但会占编辑/键盘空间；顶栏保留也可，实际位置仍待产品选择，不把研究推荐当视觉稿 |
| 项目居中 | 若指工作区顶栏，以整段内容栏为基准，左右预留等宽动作区；长名省略 | 可评估 CenterAlignedTopAppBar；需要核对当前 1.3.1 签名、侧栏打开后的基准与文字放大，不承诺长名仍完整且绝对居中 |
| 暗色 Logo | 先让容器使用主题色；若品牌图需变化，新增独立暗色资源并以 `Dsh.dark` 选择 | `drawable-night` 仅在主题配置一致时使用。全图反色/统一 tint 可能破坏品牌色；不覆盖 `bugu_title` 或设计原图。透明或专用暗色图须审查授权与视觉后另行制作 |

这些 UI 变化同样接触 DshApp，而 v0.3.0 AI 工作也接触该文件。后续实施必须串行集成或获准隔离副本，不能因主题和网络看似不同就共享目录并行写入。

## 待确认事项及后续可执行验证

产品决定仍见关联草案，研究不阻塞于这些缺口：Android 是否唯一平台；是否保留全局树及旧共享区；采用何种旧目录拆分方式；项目居中的对象和基准；新增入口位置；子树移动、删除、归档恢复规则；Logo 暗色方向。原生方案可行不等于以上选择已确认。

获准进入实现前，Codex 可按以下顺序形成可审查方案与隔离验证；本次均未运行：

| 阶段 | 合成验证与通过条件 |
| --- | --- |
| 数据契约 | 列出所有目录引用写入点；覆盖同名、空目录、跨项目共享、全局与项目混用、无效父节点、自环/多节点环、父节点归档。无效操作拒绝且不产生半写入。 |
| 历史升级 | 现存受支持历史链到新版本及新建 schema 对齐；对 schema 6/7/8 目录引用分别构造合成数据。核对 ID 对照、内容行数/项目/正文/附件校验、归档及回收站可访问；中途失败回滚，不能只证明新库可建。 |
| 备份恢复 | 6/7/8 旧备份导入、升级后往返、坏目录引用/环/缺附件/中途 SQL 失败；恢复失败保留原库和附件，暂存清理。新备份被旧版本拒绝的行为明确；不使用真实用户备份。 |
| 目录与草稿 | 深层展开折叠、逐级返回、切项目、目录删除/移动时弹窗和新笔记/临时聊天未提交，重启/进程恢复后无串用；跨项目引用和已有上下文快照保持约定。 |
| 构建与离线回归 | 按质量说明在新隔离副本用现有离线依赖运行 assembleDebug、lintDebug、migration019、lifecycle019、ZIP Codec、历史 v6 脚本；检查体系改变或交付基线时 baseline，不能借旧生成类验收。 |
| 设备体验 | 授权隔离设备和合成数据后验证 320dp 窄屏/宽屏、RTL/大字/长项目名、明暗手动与系统切换、TalkBack 展开状态和焦点、目录量/深度预算；构建不能代替视觉验收。 |

未验证：任何候选代码的编译、最新 API 对当前库的精确适配、真实历史库结构/规模、真实设备 SQLite 和性能、触摸/IME、进程恢复、暗色 Logo 实际效果、发布包完整许可清单。未做功能实验，不存在可报告的原型成功率或迁移成功结果。

## 验证与交接

- 开工于项目根运行 `python3 tools/check.py quick`，退出 0：48 文档、357 本地链接、25 资产；该结果仅针对开工输入。
- 本次只新增本报告并增加索引行，不更新产品状态、架构、ADR、资产哈希或原需求确认状态。收尾在 `/Users/cris/Documents/bugu-notes` 执行 `python3 tools/check.py quick` 退出 0（49 文档、371 本地链接、25 资产），`git diff --check` 和 `git diff --cached --check` 均退出 0。对开工保护清单 169 个既有文件核对，除索引本任务增量外其余 168 个哈希一致；索引与前副本比对仅增加研究行。报告独立空白检查通过。未运行 baseline，因为没有修改检查体系或产品代码；未运行构建和业务回归。结束仍为 main / 708a7f0，未提交。
- 下一步先由用户确认上述产品选择，再准备独立实施任务和可回退迁移方案；涉及备份兼容的变更须按 AGENTS 完成可审查方案与回退准备后核对批准。研究本身不授权开始业务开发。
- 本聊天没有启动应用、构建、服务或产品回归进程，也未读取真实数据/密钥。本次交接完成，已停止写入并释放报告与索引范围；接手时重查工作区与并行会话。
