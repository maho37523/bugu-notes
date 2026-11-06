# 当前工程状态与审计基线

日期：2026-11-06。本轮重新核对当前源码、构建与回归入口、资产校验和既有文档，证据见 [R00](exec-plans/R00-reboot-audit.md)。本文件区分本轮静态复核与历史运行记录，不依赖其他会话的聊天，也不表示本轮构建或运行通过。

## 已确认状态

| 项目 | 当前证据 |
| --- | --- |
| 版本 | [app/build.gradle.kts](../android/app/build.gradle.kts)：0.2.1 / versionCode 13，applicationId `com.dspro.android` |
| 工具链 | [根构建](../android/build.gradle.kts)：AGP 8.7.3、Kotlin/Compose 插件 2.0.21、KSP 2.0.21-1.0.28；[Wrapper](../android/gradle/wrapper/gradle-wrapper.properties)：Gradle 8.9 |
| SDK / JVM | minSdk 26、compileSdk/targetSdk 35、Build Tools 35.0.1；源码及字节码目标 17，既有构建文档使用 JDK 21 |
| 存储 | [NotesDatabase](../android/app/src/main/java/com/dspro/android/data/notes/NotesDatabase.kt)：Room 2.6.1、schema 8、13 个实体，迁移 1→8 |
| UI / 网络 | Compose、Material 3、Flow；HttpURLConnection 和手工 SSE；详见 [架构](../ARCHITECTURE.md) |
| 依赖来源 | [settings.gradle.kts](../android/settings.gradle.kts) 配置阿里云镜像及官方仓库；检查范围内未发现依赖锁定或校验元数据 |
| Web | [prototype](../prototype/README.md)：独立无依赖交互演示，localStorage 与模拟输出；未发现 Node 包管理清单 |
| Git | 本轮目录无 `.git`、无根 `.gitignore`，Git 查询无法识别仓库；分支、提交、远端、未提交状态均「未确认」，未初始化 |
| CI / 静态检查 | 未发现项目 CI、Detekt、ktlint 或独立类型检查配置；存在 Android Lint 与自建回归，见 [quality](quality.md) |

## 目录与文档口径

`android/` 是原生实现和历史 APK；`prototype/` 是演示；`tools/regression/` 与 `android/tools/regression/` 是回归资产；`docs/` 保存产品、历史版本及新工程文档。具体资源见 [assets](assets.md)。

[根 README](../README.md) 与 [Android README](../android/README.md) 的历史正文停留在 v0.2.0；[旧功能审计](05-feature-audit-and-roadmap.md) 明确描述 v0.1.1，不能据其认定当前没有项目或聊天持久化。[原架构文档](04-notes-architecture.md) 和 [原型技术方案](03-prototype-tech-plan.md) 按历史背景阅读，不覆盖当前代码事实。当前导航与状态以本套文档为准，发布事实仍引用各版本记录，不重写历史。

## 验证证据及其限制

- [v0.2.1 发布记录](15-bugu-notes-v0.2.1-release.md) 报告构建、Lint 与多项回归通过。这是历史记录，不是本阶段重新运行结果。
- 本轮重新只读核对 `android/Bugu-Notes-v0.2.1-debug.apk` SHA-256 为 `01e23b033018ca9b8bb9a5fbfb1a12b06cb3564128651318a6b54ba1ca14676f`，与发布记录相符；源码和安装包是否严格对应仍未确认。
- 现存 `android/app/build/reports/lint-results-debug.txt` 记录 0 errors / 12 warnings，但包含旧目录 `DS-Pro-4-Android` 路径，不能证明当前目录可重现构建。
- 既有 [M01](exec-plans/M01-design-assets.md) 记录隔离副本构建、Lint 和编辑器回归通过。本轮确认其临时目录及 Lint 文本仍存在，报告为 0 errors / 4 warnings；没有重新执行，也未据此确认全部源码、回归与历史 APK 对应。临时产物不是长期基线。
- 本轮 `design/SHA256SUMS` 的 25 项校验通过；源码、测试、配置及资产的变更保护与文档检查见 R00。没有构建、运行回归、启动应用或执行迁移。
- 发布记录明确未完成真实 API/搜索在线验证及设备触摸、IME 检查；本轮未补做这些验证。

## 工程体系落地程度

[AGENTS](../AGENTS.md)、[会话规则](workflow.md)、[任务索引](exec-plans/README.md) 和 [重启计划](exec-plans/000-project-reboot.md) 已形成文档工作流程，命令、验收门槛与交接格式可由后续会话读取执行。现有构建、Lint 和回归脚本已登记，但统一检查入口、CI、Git 历史保护及新增数据/设备测试尚未建立；不能称为自动质量保障已落地。当前只允许修改工程文档，后续实施须用户明确开启。

## 风险与未知事项

| 风险 | 证据 / 限制 |
| --- | --- |
| 启动清理数据 | [DsApplication](../android/app/src/main/java/com/dspro/android/DsApplication.kt) 清理超过 30 天的笔记回收站 |
| 恢复替换数据 | [BackupService](../android/app/src/main/java/com/dspro/android/data/notes/BackupService.kt) 替换业务表，提交后清理旧附件 |
| 凭据明文回退 | [ApiConfig](../android/app/src/main/java/com/dspro/android/data/ApiConfig.kt) 加密初始化失败使用普通偏好；[Manifest](../android/app/src/main/AndroidManifest.xml) 为 allowBackup=true，系统备份实际行为未确认 |
| 关系与删除 | [实体](../android/app/src/main/java/com/dspro/android/data/notes/NotesEntities.kt) 未声明外键；笔记永久删除多步操作未统一事务，附件清理一致性待验证 |
| 双链歧义 | [NotesRepository](../android/app/src/main/java/com/dspro/android/data/notes/NotesRepository.kt) refreshLinks 使用全局按标题查找，与项目内查找口径不同；同名、改名一致性待验证 |
| 测试复现 | 回归依赖本机缓存、JDK 及生成类；完整 1→8 升级、进程死亡、真实 IME、大文件与长文性能未确认 |
| TLS 警告 | 历史 Lint 中有依赖 BouncyCastle 的 TrustAllX509TrustManager 警告；运行时可达性未确认，不能据此认定应用关闭 TLS 校验 |

真实用户数据位置与规模、原 Git 来源、正式签名与发布渠道、字体以外视觉资产的授权来源、线上模型兼容性仍未确认。Codex 负责技术查证与隔离验证；用户只需补充无法从工程确认的来源信息、授权范围和产品取舍。未读取真实凭据或本机配置内容。
