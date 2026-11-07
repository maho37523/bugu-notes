# v0.4.0 点击图标立即闪退修复

日期：2026-11-07；状态：待验收（故障已修复，OPPO 真机启动验证通过）；负责人：本聊天 Codex，单一写入。

## 目标与授权

用户反馈安装 v0.4.0 APK 后点击图标立即闪退，要求解决。授权最小故障修复、隔离合成数据验证和候选 APK；不操作真实手机数据、凭据或备份，不改变应用身份、签名、架构或产品能力。

## 起始与保护

main / `708a7f0`，已有大量 v0.3.0/v0.4.0 未提交修改，全部保留。桌面聊天列表确认相关其他聊天 idle，前任务已交接。源码及文档前副本和输入哈希存 `.engineering/backups/v040-startup-before/`。未提交或切换分支。

占用：本任务、任务索引；定位后仅修改确证故障涉及的源码及回归文件。先取证，不以猜测改迁移。旧 APK 哈希见 [构建记录](T-20261107-v040-apk.md)。

## 步骤与验证

开工 quick 已通过：54 文档、403 链接、25 资产。ADB 初次沙箱启动失败；获工具权限后查询无设备连接，不存在手机日志证据。用已有 Android 35 系统镜像和全新临时 AVD（不复制既有 AVD 用户数据）复现旧 APK 启动；依证据修复，再执行隔离构建/Lint、相关回归及设备启动检查。数据库兼容修复若涉及高风险边界，先提供方案及回退准备。

回退：仅撤销本任务相对前副本增量，保留既有修改。临时 AVD 可丢弃，真实数据不接触。

## 交接

尚在诊断；根因未确认，设备型号和 Android 版本待补充。下一步在全新隔离 AVD 取得异常栈。

## 本轮诊断结果与限制

用户补充：OPPO Find X9 Pro、Android 17、覆盖旧版安装；明确暂不调试。本会话遵循该选择，未连接或操作真机。

核对原候选 APK SHA-256 为 `9838cc0b60925c3a47727fa9c4bf6c99758f1c7cb5e91b4dccd8c227d29f6804`，匹配原构建记录。静态沿 `DsApplication → AppContainer → DshAppContent` 核对初始化、配置读取、数据库 schema 8→9 与临时草稿 scope；未发现足以确认本次真机闪退根因的证据。没有依据推定为 OPPO、Android 17 或迁移问题。

实际隔离尝试：仅复制既有 AVD 的配置文件，创建 `/tmp/bugu-startup-avd/` 下全新 Android 35/30/34 AVD；不复制原 AVD 数据。既有 emulator 37.1.6 beta 在 Android 35/34 无法建立可用 ADB，35 系统 zygote 反复退出；Android 30 在应用安装前出现 `VFS: Unable to mount root fs` 内核失败。显式 sysdir/qemu2 及串口尝试未解决。此为验证环境失败，不能作为应用闪退复现证据。日志 `/tmp/bugu-startup-emulator-kernel.log`、`/tmp/bugu-startup-emulator30.log`、`/tmp/bugu-startup-emulator34-verbose.log` 仅为临时环境证据。

未安装或启动 APK，未读取真实数据、密钥或手机日志；未修改产品源码、迁移或现有 APK。未执行构建或产品回归，因为没有已确证的修复；旧离线通过记录不证明当前真机正常。根因与修复状态均为未确认，任务未完成。

恢复条件：获得该次故障的异常类型、异常栈及覆盖前版本（可在用户以后愿意调试时由 Codex 读取受限崩溃日志），或具备可运行的匹配隔离系统后复现。必要时可另行提供无需 USB 的诊断包方案，先说明日志内容、隐私边界和保留数据方式，不擅自修改真实设备。无需卸载、清空应用或替换真实数据库来排查。

本轮最终仅新增本任务并更新索引；停止源码及文档写入，本次创建的模拟器已全部停止，无构建/应用服务遗留。ADB 查询服务由本轮启动，收尾单独停止，不执行卸载或数据清理。下一会话先复核工作区及其他聊天状态。回退仅删除本任务及其索引行，前副本保留。

## 续作授权

用户要求本地正常运行后打包交付；继续故障修复及隔离设备启动/覆盖升级验证，不以编译通过代替运行通过。允许使用现有离线模拟器和合成数据，保留真实设备不调试的选择。本聊天为单一写入，其他相关聊天 idle。继续使用前副本保护。

用户已明确允许下载 Google 官方稳定版测试工具到临时目录。本轮选择官方归档 Emulator 36.6.11（构建 15507667），不替换 SDK 原有工具、不变更项目依赖。官方包预期 SHA-256：`aebcd4dde29a4921d47e5e79b8c1ffece69a70f5b280d8dc7033ebaffa737072`，来源 [官方归档](https://developer.android.com/studio/emulator_archive)。

## 本地实际运行进展

稳定版 36.6.11 + 全新 Android 35 AVD 已正常启动，确认前次失败为预览版模拟器环境问题。原 v0.4.0 APK 新安装后进入资料库且进程存活，crash buffer 无应用异常。随后在同一专用 AVD 重置合成安装，安装 v0.3.0（schema 8），加入 A/B/全局共享目录、三篇合成笔记、会话及孤立临时草稿，再覆盖原 v0.4.0：Android Room 实际升级至 9，三篇正文保留，目录拆分为三个 scope，UI 可打开目录及笔记。此场景尚未复现用户故障，不能据此称根因已修复。

继续下载官方稳定 Android 17 API 37.0 / 16 KB 镜像和其要求的稳定 Emulator 37.2.13，均仅放临时目录。镜像清单来源 `https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml`，revision 8，SHA-1 `6ce19849a4b3cf29b356073cee35a5b7c4bde999`；37.2.13 来源官方 SDK 清单，构建 16740729，包 SHA-1 `6c5e61b873e11c7d1d08683bea089f762baee070`。APK arm64 native path 库的 LOAD 对齐为 16 KB；这只是静态信息，不替代 Android 17 运行。

新增隔离 `tools/regression/android_startup.py` 防止再将仅编译通过当作启动证据，相关范围与命令见 [回归说明](../../tools/regression/README.md)。产品源码目前没有改动；本轮变更回归体系，完成后执行完整 baseline。

第一轮完整 baseline 已通过，报告 `.engineering/reports/20261107-122857-151157/summary.json`；隔离重建 APK 与原包 SHA-256 完全相同。Android 17 / API 37 / 16 KB 专用 AVD 的原包新安装及第二次冷启动通过。首次升级脚本在旧版页面检查处失败：v0.3.0 本身没有 v0.4.0 的“资料库”标题，进程与首页过滤栏正常；属于新夹具的旧版 UI 假设错误，不是应用异常。调整为各版均验证“笔记/对话”过滤栏，v0.4.0 仍额外验证资料库标题；没有删去新版本渲染或进程/异常断言。随后重新运行两套设备检查，并对最终脚本重跑 baseline。

## 最终本地验证及交付

2026-11-07 最终固定输入 baseline 退出 0，报告 `.engineering/reports/20261107-123141-568382/summary.json`：工具自检、assembleDebug / lintDebug、migration、lifecycle、editor、network、ZIP、legacy_migration 全部通过。构建 64 秒，48 tasks；输入哈希见同目录 `inputs.json`。最终启动脚本哈希与该基线输入一致。构建/Lint/回归与设备测试属于同一 APK 内容哈希。

| 实际运行 | 结果与证据 |
| --- | --- |
| Android 15 / API 35 / 4 KB，全新 BuguSmoke35 AVD | 新安装、重复冷启动、v0.3.0 实际安装后合成 schema 8 数据覆盖升级、schema 9 / 正文 / 同 scope 检查、升级后三次冷启动全部通过；`.engineering/reports/startup35-final/summary.json` |
| Android 17 / API 37 / 16 KB，全新 BuguSmoke17 AVD | 相同全部场景通过；`.engineering/reports/startup17-final/summary.json`。另实际点击升级后的目录与笔记，正文及“已保存”状态可见，证据 `interaction-summary.json`、`opened-note.png` |
| 本机签名及包信息 | 指定现有 JAVA_HOME 后 apksigner verify 通过，v2 签名，证书 SHA-256 `b55864e4aaf8cca0f1e27b259c43715d16fbf9b172e500fc540c61dbfad53917`，与现存 v0.3.0 候选一致；applicationId `com.dspro.android`、0.4.0 / 15、min 26 / target 35。首次缺 JAVA_HOME 的签名检查环境失败，指定 JDK 21 后通过 |

交付 `.engineering/apk/Bugu-Notes-v0.4.0-local-verified-debug.apk`，29,269,227 bytes；SHA-256 `9838cc0b60925c3a47727fa9c4bf6c99758f1c7cb5e91b4dccd8c227d29f6804`。该包从最终隔离构建复制，**内容与原 v0.4.0 APK 相同**，不能称为修复包或声称重新安装即可消除手机故障。没有证据支持将故障归咎于 Android 版本，也没有证据证实具体程序缺陷；未在手机环境复现，根因仍未确认。

实际变更仅新增启动回归脚本、说明和任务证据；产品源码、数据库迁移、应用身份、版本和现有候选 APK 没有修改。已有功能无代码行为变化。用户“先在本地运行后打包”的交付条件已在上述范围完成，原始真机故障修复仍未完成。覆盖前确切版本、真实旧库状态、OPPO 运行栈及厂商环境仍未知；在线、IME、真实历史数据库没有执行。无需用户决定产品规则；若真机继续闪退，需要能区分原因的实际崩溃证据，不以清空数据或猜测改迁移代替诊断。

收尾：本次两个专用 AVD 停止，构建与回归进程已结束；停止写入并释放本任务范围。未连接真机、读真实凭据/数据、安装业务依赖、提交或发布。回退仅移除新增启动脚本及本次文档增量，保留前次修改、旧 APK 和保护副本；工具下载均在临时目录，不修改原 SDK。

## USB 真机诊断与修复续作

用户表示仍闪退，已连接 USB 并开启调试，要求查看。授权只读系统/应用版本和布谷自身崩溃记录；本次尚未启动、安装或更改手机应用。系统确认 OPPO PLG110 / Android 17 / API 37，已安装 0.4.0 / 15。其他相关聊天 idle，继续单一写入，新增前副本 `.engineering/backups/phone-startup-before/`。

已取得多次一致崩溃（最近 12:54）：`IllegalArgumentException: Invalid temporary_chat_drafts.folderId reference`，调用链 `splitLegacy:64 → migrateLegacyDatabase:141 → MIGRATION_8_9 → Room onUpgrade → cleanupTrash`。仅保存本应用 AndroidRuntime 行于 `.engineering/reports/phone-startup/app-crash.txt`，未输出业务正文或凭据，未拷贝真实数据库。根因明确为 schema 8 临时草稿引用不存在目录，新迁移严格拒绝，未处理异常导致进程退出。先前合成升级测试只有“项目不存在但目录存在”，遗漏“目录不存在”。旧数据产生悬空引用的具体历史操作仍未知，不推断用户误操作。

最小方案：仅在旧库 SQL 8→9 迁移读取临时草稿时，将不存在的 folderId 规范为 null（回到草稿所在 scope 根目录），保留 projectScope/projectId、正文、引用、选中笔记、附件 JSON 和时间。有效目录仍按原映射拆分；笔记/会话非法目录仍报错并事务回滚；ZIP 导入和 v9 严格验证规则不变，不重建数据库或删除草稿。先补齐合成失败证据、SQL/Room 与保留/回滚断言，修复后完整 baseline 和 Android 15/17 测试。

真实设备覆盖安装/首次启动将触发升级，且现有启动逻辑会清理超 30 天回收站。因此先完成可审查 APK 与回退方案，再按 AGENTS 的真实数据边界请求一次批准：获准后由 Codex 在不输出内容的前提下保存原 APK、数据库/WAL/SHM 和附件及偏好文件的私有本地回退归档，校验备份，然后同签名覆盖安装并验证。没有批准前不启动、不安装或迁移手机数据。回退不能直接用旧版打开新 schema；如需回退需原 APK 与完整迁移前归档配套恢复，实际恢复仍须单独明确批准。

### 失败证据与候选实现

新增 lifecycle 合成断言在修复前退出 1，与手机完全相同的 `Invalid temporary_chat_drafts.folderId reference`，日志 `.engineering/reports/phone-startup/lifecycle-before.log`。Android 17 原 APK 在同类合成旧库覆盖升级后进程退出，crash stack 同样指向 splitLegacy/migrateLegacyDatabase，报告 `.engineering/reports/startup17-dangling-before/`。此前“本地通过”的证据没有覆盖这类输入，不再作为该故障已修复依据。

生产修复仅 `FolderScopeMigration.kt` 的 SQL 旧草稿读取：基于实际旧目录 ID 集合，将悬空 draft folderId 映射为 null，再复用原事务内映射写入。不修改通用 splitLegacy / validateScoped / ZIP，不更改任何 schema 号、升级签名、附件或业务正文。lifecycle 新增全局/现存项目/孤立项目三类悬空目录草稿，逐值比较正文、引用、选中笔记、附件 JSON 和时间戳；有效目录保留及通用校验严格拒绝悬空输入，异常事务回滚仍保留旧草稿原引用。

定向隔离 lifecycle 修复后已通过，日志 `.engineering/reports/phone-startup/lifecycle-after-isolated.log`。另有一次命令误在主源码目录执行同一合成 runner（仅生成临时测试数据库，无真实数据），其结果不用于验收；随后改在已有构建的隔离副本执行并通过。启动脚本增加 `--dangling-draft-folder` 与附件文件保留断言。进入最终完整 baseline 后固定输入不再编辑，完成后运行设备回归。

### 手机操作已获明确批准

本轮最终 baseline `.engineering/reports/20261107-125956-652818/summary.json` 全部通过；修复包 SHA-256 `37875afd48d3a0524ab39224aad2d84e74bf9557d8f47bb6b5f00385c1343e6c`，0.4.0 / 15，签名与原包一致。Android 15/17 的故障输入、草稿/附件保留、新安装及三次升级后冷启动全部通过：`.engineering/reports/startup35-dangling-fixed/` 与 `startup17-dangling-fixed/`。同一份原先崩溃的合成库直接覆盖候选也通过：`.engineering/reports/phone-startup/direct17/summary.json`。

在明确说明数据库升级、超 30 天回收站清理和本地私有备份范围后，用户回复“允许备份后安装验证”。由 Codex 先校验现装 APK 和签名，停应用保证备份一致性，保存 APK、databases（含存在的 WAL/SHM）和 files/attachments；不复制 API 凭据或输出真实内容。验证归档完整性并记录哈希，再同签名覆盖安装。任何备份失败在安装前停止；不自动恢复或清空数据。操作结果在后续追加。

### 真机修复验证结果与最终交接

已按明确批准执行：安装前现装 APK 内容与原候选哈希一致，原包及候选签名均通过且证书相同；停应用后备份原 APK、完整 databases（含存在的 WAL/SHM），附件目录仅在存在时归档；本设备未发现 files/attachments 目录，实际数据归档为 1 个数据库文件 / 151,552 bytes。私有回退目录 `.engineering/private/phone-before-startup-fix-20261107-130454/`，目录 0700、文件 0600，归档全量读取完整性检查通过；仅记录数量和哈希，不输出文件名/正文。该归档是本机回退点，不是异地备份；不含 API 偏好或密钥。

备份校验成功后使用 `adb install -r` 覆盖安装，启动等待 10 秒：布谷进程存活、MainActivity 前台、与安装前日志相比无新增应用崩溃；从设备已安装 base.apk 流式计算 SHA-256，确认等于修复包 `37875afd48d3a0524ab39224aad2d84e74bf9557d8f47bb6b5f00385c1343e6c`。安全摘要 `.engineering/reports/phone-startup/device-result.json`，私有备份 manifest 记录归档哈希；没有截取或输出真实笔记界面，没有发起 API 请求。没有执行卸载、清空数据或恢复。实际启动发生在用户已明确同意数据库升级及既有回收站清理之后；未逐条读取真实数据作内容比对，保留状态以用户验收及回退备份为依据。

本轮交付 `.engineering/apk/Bugu-Notes-v0.4.0-startup-fix-debug.apk`（29,269,227 bytes），0.4.0 / 15、应用 ID 和签名不变；SHA-256 见上。它与此前“local-verified”原包不同，只有本文件的 startup-fix 包包含修复。回退不能直接用 schema 8 旧版读取新库；如需回退，保留本机私有目录完整原 APK 和数据库/附件归档，另获用户明确批准后配套恢复，绝不自动执行恢复或删除归档。

修改范围：生产仅 `FolderScopeMigration.kt` 的旧草稿读取归一化；回归为 lifecycle019/Test.kt 和 android_startup.py；其余为任务/索引、当前状态、质量和回归说明。最终三份生产/测试文件哈希与 `.engineering/reports/20261107-125956-652818/inputs.json` 完全一致，用户前次开发改动全部保留。未提交、切换分支、推送或公开发布，未引入依赖/版本/架构变更。

技术修复和此手机的启动故障验证完成，等待用户确认实际笔记、草稿及日常体验。剩余边界：真实数据逐条内容一致性、中文 IME、在线模型、所有历史升级链和手机长时间稳定性未验证。只承诺上述实际证据，不扩展为全部功能通过。

收尾将停止两台本次测试模拟器；本地构建与回归均已结束，停止写入并释放范围；已修复的手机应用留在前台供用户验收，保留 USB 调试连接，不停止用户的手机应用。下一动作由用户体验验收或明确的新故障触发。其他相关聊天最近核对为空闲；后续会话重新核对磁盘和写入状态。

最终收尾检查：quick 通过（55 文档、409 链接、25 资产），`git diff --check` 与暂存差异检查通过；两台专用 AVD 已停止，手机应用和 USB 连接保留。真实数据库只作为获准私有归档复制，没有输出或逐条检查内容。
