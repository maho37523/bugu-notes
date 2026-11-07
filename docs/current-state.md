# 当前工程状态与审计基线

日期：2026-11-06。静态审计见 [R00](exec-plans/R00-reboot-audit.md)，本次获准实施的资产/版本保护见 [R01](exec-plans/R01-version-protection.md)，实际隔离验证见 [R02](exec-plans/R02-quality-baseline.md)。本文件区分审计、历史记录与本次执行证据，不依赖其他会话的聊天。

## 已确认状态

| 项目 | 当前证据 |
| --- | --- |
| 版本 | [app/build.gradle.kts](../android/app/build.gradle.kts)：0.4.0 / versionCode 15（开发中，未完成本轮离线/设备验收），applicationId `com.dspro.android`；v0.3.0 / 14 有已完成的离线候选记录，设备及在线仍待验收 |
| 工具链 | [根构建](../android/build.gradle.kts)：AGP 8.7.3、Kotlin/Compose 插件 2.0.21、KSP 2.0.21-1.0.28；[Wrapper](../android/gradle/wrapper/gradle-wrapper.properties)：Gradle 8.9 |
| SDK / JVM | minSdk 26、compileSdk/targetSdk 35、Build Tools 35.0.1；源码及字节码目标 17，既有构建文档使用 JDK 21 |
| 存储 | [NotesDatabase](../android/app/src/main/java/com/dspro/android/data/notes/NotesDatabase.kt)：Room 2.6.1、schema 9、13 个实体，迁移 1→9；v0.4.0 schema 8→9 与 v6/7/8 旧备份转换已通过本机离线合成回归，设备/真实数据尚未验证 |
| UI / 网络 | Compose、Material 3、Flow；HttpURLConnection 和手工 SSE；v0.3.0 的 Claude 协议与安全配置为离线候选，设备及在线验收待完成；v0.4.0 目录树与内容归属代码已实现并通过离线构建/Lint，视觉设备验收待完成，详见 [实施任务](exec-plans/T-20261107-v040-implementation.md) |
| 依赖来源 | [settings.gradle.kts](../android/settings.gradle.kts) 配置阿里云镜像及官方仓库；检查范围内未发现依赖锁定或校验元数据 |
| Web | [prototype](../prototype/README.md)：独立无依赖交互演示，localStorage 与模拟输出；未发现 Node 包管理清单 |
| Git | 本次已新建本地仓库 main，初始保护提交 8ea1938；根 .gitignore 排除本机/敏感/生成文件；2026-11-07 获用户明确授权后已公开至 GitHub `maho37523/bugu-notes`，origin 已建立、main 已推送、MIT 已识别；原历史来源仍未确认，见 R01 及 GitHub 上传任务 |
| CI / 静态检查 | 已有 Android Lint、六项回归与新增 tools/check.py 本地入口；未引入 CI、Detekt 或 ktlint，见 [quality](quality.md) |

## 目录与文档口径

`android/` 是原生实现和历史 APK；`prototype/` 是演示；`tools/regression/` 与 `android/tools/regression/` 是回归资产；`docs/` 保存产品、历史版本及新工程文档。具体资源见 [assets](assets.md)。

[根 README](../README.md) 与 [Android README](../android/README.md) 的历史正文停留在 v0.2.0；[旧功能审计](05-feature-audit-and-roadmap.md) 明确描述 v0.1.1，不能据其认定当前没有项目或聊天持久化。[原架构文档](04-notes-architecture.md) 和 [原型技术方案](03-prototype-tech-plan.md) 按历史背景阅读，不覆盖当前代码事实。当前导航与状态以本套文档为准，发布事实仍引用各版本记录，不重写历史。

## 验证证据及其限制

- [v0.2.1 发布记录](15-bugu-notes-v0.2.1-release.md) 报告构建、Lint 与多项回归通过。这是历史记录，不是本阶段重新运行结果。
- R00 只读核对 `android/Bugu-Notes-v0.2.1-debug.apk` SHA-256 为 `01e23b033018ca9b8bb9a5fbfb1a12b06cb3564128651318a6b54ba1ca14676f`，与发布记录相符；源码和历史安装包是否严格对应仍未确认。
- 现存 `android/app/build/reports/lint-results-debug.txt` 记录 0 errors / 12 warnings，但包含旧目录 `DS-Pro-4-Android` 路径，不能证明当前目录可重现构建。
- 既有 [M01](exec-plans/M01-design-assets.md) 通过记录仍是历史证据，临时产物不是长期基线。
- R01 归档并隔离恢复核对 169 项内容和权限，建立本地 Git；原件保留，未读取真实凭据、本机配置内容或私钥。保护归档只在本机，不是异地或用户数据备份。
- R02 在全新隔离副本实际构建/Lint通过；原始六回归中五项通过，历史 v6 测试夹具复现 schema 8 重复列错误，最小修复后通过。最终统一入口另建全新副本：构建 44.88 秒、48 任务、0 errors / 4 warnings，六项回归及工具 10 项自检全部通过，精确证据见 R02/R04。
- v0.4.0 在新隔离副本通过 schema 8→9 Room 迁移、schema 8/7/6 备份转换、scope/目录树 Repository 回归及原有五类回归；最新完整报告见 [实施任务](exec-plans/T-20261107-v040-implementation.md)。这只覆盖合成 JDBC/ZIP 输入和离线构建，不是 Android 设备或真实历史数据验收。
- 25 项设计哈希通过。新增 [R03 离线恢复失败回归](exec-plans/R03-data-safety.md) 已定向通过，数据库/旧附件保持、暂存文件清理有合成测试证据；不代表真实设备、真实历史库或完整迁移验证。
- 工程基础阶段未安装或启动应用。2026-11-07 [启动故障任务](exec-plans/T-20261107-v040-startup-crash.md) 后续通过 USB 日志定位旧临时草稿悬空 folderId 导致迁移闪退；SQL 升级读取仅将该失效引用归 null，保留草稿与附件，ZIP/v9 严格验证不变。原包合成复现失败、修复后 Android 15/17 同类升级及完整 baseline 通过；获用户明确批准、私有备份并校验后，在 OPPO Android 17 覆盖安装修复包并确认启动存活、首页前台、无新增崩溃。真实内容逐条一致性由用户验收；未做在线 API、中文 IME 或全部历史链验证。

## 工程体系落地程度

[AGENTS](../AGENTS.md)、[会话规则](workflow.md)、[任务索引](exec-plans/README.md) 和 [重启计划](exec-plans/000-project-reboot.md) 定义开工/验收/交接；本地版本保护与 [检查入口](../tools/check.py) 已建立且集成验证通过，见 [R04](exec-plans/R04-check-automation.md)。工程基础阶段已完成；用户已另行开启 v0.3.0 业务开发，实际进度见实施任务。CI、异地备份、完整历史升级和设备验收尚未建立，不声称完整长期可靠性已验证。

## 风险与未知事项

| 风险 | 证据 / 限制 |
| --- | --- |
| 启动清理数据 | [DsApplication](../android/app/src/main/java/com/dspro/android/DsApplication.kt) 清理超过 30 天的笔记回收站 |
| 恢复替换数据 | [BackupService](../android/app/src/main/java/com/dspro/android/data/notes/BackupService.kt) 替换业务表，提交后清理旧附件 |
| 凭据与系统备份 | v0.3.0 代码中加密失败停止凭据读取及请求，并为凭据偏好配置系统备份排除；Keystore 故障及备份/设备迁移的真机行为仍未确认，既有明文回退文件不会自动删除 |
| 关系与删除 | [实体](../android/app/src/main/java/com/dspro/android/data/notes/NotesEntities.kt) 未声明外键；笔记永久删除多步操作未统一事务，附件清理一致性待验证 |
| 双链歧义 | [NotesRepository](../android/app/src/main/java/com/dspro/android/data/notes/NotesRepository.kt) refreshLinks 使用全局按标题查找，与项目内查找口径不同；同名、改名一致性待验证 |
| 测试复现 | 本机隔离构建已复现，入口依赖 JDK/SDK/离线缓存；完整 1→8、真实历史库、进程死亡、IME、磁盘耗尽及设备性能未确认 |
| TLS 警告 | 历史 Lint 中有依赖 BouncyCastle 的 TrustAllX509TrustManager 警告；运行时可达性未确认，不能据此认定应用关闭 TLS 校验 |

真实用户数据位置与规模、原 Git 来源、正式签名与发布渠道、字体以外视觉资产的授权来源、线上模型兼容性仍未确认。Codex 负责技术查证与隔离验证；用户只需补充无法从工程确认的来源信息、授权范围和产品取舍。未读取真实凭据或本机配置内容。
