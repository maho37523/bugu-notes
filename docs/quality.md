# 质量基线与验证

状态：工程基础实施已完成；截至 2026-11-07，v0.4.0 授权离线代码工作包和基线已完成，报告见下方及 [实施任务](exec-plans/T-20261107-v040-implementation.md)。长期设备/在线验收仍未完成。[R00](exec-plans/R00-reboot-audit.md)、[v0.2.1](15-bugu-notes-v0.2.1-release.md) 与 [M01](exec-plans/M01-design-assets.md) 保留各自时间/范围，不替代本次执行。检查由 Codex 执行，用户不需要手动操作终端。

## 日常入口

在项目根，由 Codex 执行（Git + Python 3.10+，只用标准库）：

```sh
python3 tools/check.py quick
python3 -m unittest discover -s tools -p test_engineering_checks.py
python3 tools/check.py baseline
```

quick 检查文档链接/锚点、变更文档空白、Git 工作差异和暂存差异、版本范围私密路径及 25 项设计校验。自检用合成资料验证失败确实会被拦住。baseline 先 quick，再检查已有离线环境、复制当前工作文件到新隔离副本，运行自检、构建/Lint 和下面六项回归；不复制 APK、真实数据、本机配置或旧 build，不安装依赖或启动应用。

报告写 `.engineering/reports/<时间>/summary.json`、inputs.json 和逐项日志，包含起始提交、工作文件哈希、隔离路径、命令、退出码和时间。文件已修改但未提交时，以 inputs.json 为准确执行输入，不能只引用 HEAD。机器状态 passed/failed/not_run/blocked 分别对应通过/失败/未执行/环境受阻，非全部通过返回非零。环境前置受阻同样返回非零；quick 失败时直接报告，不创建完整基线报告。

baseline 需要已有 JDK 21、SDK 35 / Build Tools 35.0.1、Gradle 8.9 离线分发及 ~/.gradle 缓存。当前默认使用既有 Mac 路径，可用 JAVA_HOME/ANDROID_HOME 指定已有工具；历史 runner 不支持其他 Gradle 缓存位置，入口会明确受阻。运行时可能需要工具权限写缓存锁；这不是允许安装新依赖。

## 文档验证

检查新文档的本地链接、文件路径、版本、功能状态、当前/目标架构及命令来源；检查 Markdown 空白。要求执行 `git diff --check`，但当前缺失 Git 元数据，不能将失败或空结果报告为通过；可补充独立的行尾空白检查。未初始化 Git 前，无法提供真实提交差异。

无 Git 时先保留拟改文档的前副本，再比较本轮差异；核对所有本地链接存在、标题锚点有效、变更文档无行尾空白且以换行结束。对保护范围内源码和资产比较前后 SHA-256。临时副本用于检查本轮修改，不能替代长期版本历史或用户数据备份。

## 按变更选择检查

下表为实施阶段的最低选择规则，不是本轮已执行结果。共同前提是保护现有修改、记录源码基线、使用隔离副本与测试数据。Android 编译包含 Kotlin/Java 类型检查，先复用编译和 Lint，不为了“有工具”而另装检查器。

| 变更 | 必须执行 | 额外验收边界 |
| --- | --- | --- |
| 仅工程文档 | 链接、状态/版本、空白、最终差异和保护文件校验 | 无需为文档启动应用或运行业务回归 |
| Android 源码或构建配置 | 同源 `assembleDebug` + `lintDebug`，再选下列相关回归 | 新增错误阻止集成；警告区分既有/新增并评估，不通过全局忽略来“变绿” |
| 编辑器、Markdown、保存与版本 | editor018；保存/版本涉及持久化时加 lifecycle019 | 触摸、中文 IME、emoji、撤销、收键盘与长文需要设备验证 |
| 聊天、草稿、项目、归档或搜索 | lifecycle019；涉及请求/流式再加 network021 | 项目切换、停止/重试和进程恢复使用测试会话；离线结果不证明真实 API |
| Entity / DAO / 迁移 / 备份 / 附件 | migration019、lifecycle019（含 schema 8→9 目录转换）、ZIP Codec；先复核历史 v6 脚本 | 改动迁移前补齐受影响历史版本、失败回滚和附件完整性；schema 9 的新建库不能代替升级测试 |
| 网络与连接配置 | network021；必要时 lifecycle019 验证消息持久化 | 线上验证另获授权，用测试 Key，不输出凭据，记录供应商/模型与结果 |
| 品牌、主题或资源路径 | 设计校验、资源引用、构建/Lint及设备视觉 | 获准改图后才能更新校验值，保留原图；不能以哈希更新代替视觉审查 |
| Web 演示代码或资源 | 静态引用与相关浏览器交互 | 保持演示身份，不能据此确认 Android 功能；本轮不运行 Web |

首次基线及交付发布跑全部当前有效回归；日常小改动按影响选择，集成后验证最终源码。必要检查失败、未执行或环境受阻时，相应任务不能写成“全部验证通过”。若本阶段只交付部分结果，明确限制并保留后续验收项。

## 证据与失败处理

每次在任务中保存日期、源码提交或文件校验、工作目录、JDK/SDK/缓存前提、实际命令与退出码、结果摘要及报告位置。结果统一为「通过 / 失败 / 未执行 / 环境受阻」。测试数据应可重建，不包含真实笔记、附件或 Key；日志先检查敏感内容再引用。

先判断是环境缺失、旧生成产物、脚本兼容问题还是产品缺陷。需要调整脚本时先说明其实际覆盖，修复后运行对应检查；不得删断言或降低标准以隐藏失败。受阻时记录可由 Codex 执行的恢复动作，用户只决定授权、预算或产品行为。没有新变化或新疑点时不重复运行已通过检查。

## 已存在的构建入口（后续实施阶段使用）

工作目录为 `android/`，来源：[Android README](../android/README.md)。既有文档使用本机 JDK 21 和 Android SDK；先确认本地环境，不读取或输出配置中的敏感值。Kotlin/Java 编译目标为 17，与运行 Gradle 的 JDK 版本应区分。

```sh
./gradlew --offline :app:assembleDebug :app:lintDebug
```

离线缓存缺失时会失败，不代表可自动安装依赖。输出路径 `android/app/build/outputs/apk/debug/app-debug.apk`。不存在已确认的正式发布/签名流水线。未发现独立类型检查、ktlint、Detekt、常规 src/test 或 src/androidTest 测试源；不得编造对应命令。

## 既有回归入口

以下命令均在项目根目录运行，来自现有脚本及发布记录。执行前应在隔离副本用同一份源码重新构建，避免混用旧生成类。JVM runner 依赖 JDK 21、本机 Gradle 缓存；脚本会写临时目录，不属于只读检查。

| 命令 | 覆盖与限制 |
| --- | --- |
| `python3 tools/regression/migration019.py` | 历史回归：对比 6→8 SQL 迁移链与生成 Room schema，检查列、索引和历史记录；不等于全量 1→9 验证 |
| `bash tools/regression/lifecycle019/run.sh` | 生产 Repository、DAO SQL、实体、上下文、备份服务配合真实 SQLite JDBC 与适配器；生命周期、草稿、归档恢复、搜索和旧备份兼容 |
| `bash tools/regression/editor018/run.sh` | 使用当前已编译编辑器/渲染类；原文往返、选区、撤销、Markdown、Unicode 和长文逻辑；不覆盖真实触摸/IME |
| `bash tools/regression/network021/run.sh` | 离线配置迁移、供应商隔离、请求体、SSE 和搜索 fixtures；不证明线上连通 |
| `python3 android/tools/regression/test_backup_zip_codec.py` | 实际 Java ZIP Codec 的往返、路径、重复条目及大小限制 |
| `python3 android/tools/regression/test_room_migration_v6.py` | 从当前 DDL 重建合成 v5，执行历史 5→8 SQL 并检查草稿删除、旧记录和 sources/queries 默认值；schema 8→9 在 lifecycle019 单独验证；不是真实历史库或完整 1→9 |

脚本位置见 [根回归说明](../tools/regression/README.md)、[Android 回归目录](../android/tools/regression)。目录名中的历史版本不等于当前覆盖；`backup018/`、`migration018.py` 保留为历史回归，不自动当作当前 schema 验收。

R02 已实际复现 `test_room_migration_v6.py` 的重复列失败，修复重建合成 v5 时未去除 schema 8 新列的问题，补充迁移默认值/旧记录断言，未改生产迁移或删除断言。lifecycle019 新增恢复中途 SQL 失败的数据/附件保留断言，已定向通过；限制见 [R03](exec-plans/R03-data-safety.md)。

## 自动化与版本保护的实施边界

当前已有本地 Git 基线、保护归档恢复验证、统一检查入口和既有回归；没有远端 CI、依赖锁定/校验或独立历史 schema 导出。实施范围见 [R01—R04](exec-plans/000-project-reboot.md)。没有引入新依赖、格式化框架或工具链升级；远端和 CI 待平台明确后复用本地入口。

## 必须补齐的验收

2026-11-07 v0.4.0 离线实施基线已在隔离副本通过：构建/Lint、migration、lifecycle（含 Room schema 8→9、schema 8 ZIP 共享目录转换与恢复失败附件保护）、editor、network、ZIP Codec、历史迁移均通过；证据为 `.engineering/reports/20261107-115907-056011/summary.json`。该结果不覆盖下列设备、真实数据、在线或发布验收。

- 真机/模拟器：连续编辑触摸、中文 IME、emoji、软硬键盘、长文、进程死亡、项目切换和草稿隔离。
- 数据：全部受支持历史升级链、恢复失败回滚、附件完整性、归档/删除、同名双链及改名。
- 网络：使用用户授权的测试配置验证供应商及模型兼容、搜索成功/空结果/失败、停止/重试；不在报告中记录 Key。
- 发布：确认应用身份和升级签名连续性，记录版本、APK 哈希和签名验证；不声称已有 release 流水线。

启动会清理超期回收站，完整恢复会替换业务数据，删除项目会删除项目记忆。上述验收只能针对受保护的副本/测试数据。历史 Lint 0 errors/12 warnings 的报告含旧目录路径；依赖 TLS 警告需评估可达性，不能只因总错误为零而忽略。

## 2026-11-07 隔离启动检查补充

`tools/regression/android_startup.py` 在专用可丢弃 AVD 验证 APK 新安装、Compose 首页、异步 Room 打开、合成 schema 8→9 覆盖升级、正文/目录归属和多次冷启动；身份防护、命令与报告内容见 [回归说明](../tools/regression/README.md#隔离-android-启动与覆盖升级)。该检查不在 baseline 中自动执行，不能将 JDBC 或 assembleDebug 通过等同于运行通过。交付要求本地可运行的 APK 时，应在隔离 AVD 运行最终同哈希产物，并记录实际系统版本。

本轮 Android 15 / 4 KB 与 Android 17 / 16 KB 实际运行均通过；基线 `.engineering/reports/20261107-123141-568382/summary.json`、设备报告 `.engineering/reports/startup35-final/summary.json` 和 `.engineering/reports/startup17-final/summary.json`。原包和重建包哈希相同，用户真机闪退未复现，不能据此称已修复；OPPO、真实旧库、IME、在线与全部历史升级链仍未确认。

后续 USB 真机日志已将启动故障定位到旧临时草稿悬空 folderId。修复仅限 SQL 8→9 读取，新增原包失败/修复通过的 Android 17 及 JDBC 负例，完整基线 `.engineering/reports/20261107-125956-652818/summary.json` 通过；Android 15/17 `startup35-dangling-fixed` / `startup17-dangling-fixed` 通过。用户明确批准后，备份并校验现有 APK/数据库/附件，再在 OPPO Android 17 覆盖修复包，进程、前台 Activity 和新增异常检查通过。该后续证据取代前段“真机根因未确认”的历史状态，不取代用户实际内容和完整体验验收。详见 [启动修复任务](exec-plans/T-20261107-v040-startup-crash.md)。

## v0.5.0 订阅连接验证补充

baseline 构建增加 `:app:assembleDebugAndroidTest`，编译自定义 Android 框架 Instrumentation 测试包，无新增 JUnit 或其他依赖。该测试包只用于专用合成 AVD，不随用户主 APK 打包，也不安装到真实手机；框架运行器和外部脚本同时限制虚拟设备/专用 AVD 名称，并拒绝已有账号凭据。

network021 现在调用 `tools/regression/chatgpt/run.sh`：实际临时 RSA 签名、授权回调/PKCE/state/nonce、账号隔离/模型目录、令牌刷新并发与取消、退出/存储失败、官方主机和 Responses 终态等均用合成 fixture，不发真实模型请求。Android 运行检查另用 `tools/regression/chatgpt/android_callback.py`：生产 loopback、Android JCA 和真实 EncryptedSharedPreferences 隔离，测试后清除合成凭据。完整命令和限制见 [回归说明](../tools/regression/README.md)。真实浏览器登录、套餐资格及在线推理不能用这些结果替代。
