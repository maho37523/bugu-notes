# 项目资产登记

日期：2026-11-06。下表登记实际存在文件；路径相对项目根。只登记位置、用途和保留方式，不表示完成内容授权或运行验证。初始登记阶段未修改资产；M01 已仅移动两张历史设计参考图，原生与 Web 运行时资源原位保留。详见 [M01 记录](exec-plans/M01-design-assets.md)。

设计分类、PNG 元数据和消费方见 [设计入口](../design/README.md)，完整性基线见 [SHA256SUMS](../design/SHA256SUMS)。不另复制原图或重复维护配色实现。

## 视觉、文档与交付原件

| 类型 | 实际路径 | 用途与状态 | 保留方式 |
| --- | --- | --- | --- |
| 原生品牌原图 | [android/app/src/main/assets/brand/bugu_logo_original.png](../android/app/src/main/assets/brand/bugu_logo_original.png) | 原图；当前品牌输入 | 原样保留，不覆盖；衍生图另存，来源授权待确认 |
| 原生品牌原图 | [android/app/src/main/assets/brand/bugu_title_original.png](../android/app/src/main/assets/brand/bugu_title_original.png) | 原图；当前品牌输入 | 原样保留，不覆盖；衍生图另存，来源授权待确认 |
| 原生展示资源 | [android/app/src/main/res/drawable-nodpi/bugu_title.png](../android/app/src/main/res/drawable-nodpi/bugu_title.png) | 标题展示；现存资源 | 保留路径与原图关系，迁移前验证资源引用 |
| 原生启动图标 | [android/app/src/main/res/mipmap-hdpi/ic_launcher.png](../android/app/src/main/res/mipmap-hdpi/ic_launcher.png) | 各密度启动图标；现存资源 | 成套保留，禁止只迁移单一密度 |
| 原生启动图标 | [android/app/src/main/res/mipmap-mdpi/ic_launcher.png](../android/app/src/main/res/mipmap-mdpi/ic_launcher.png) | 各密度启动图标；现存资源 | 成套保留，禁止只迁移单一密度 |
| 原生启动图标 | [android/app/src/main/res/mipmap-xhdpi/ic_launcher.png](../android/app/src/main/res/mipmap-xhdpi/ic_launcher.png) | 各密度启动图标；现存资源 | 成套保留，禁止只迁移单一密度 |
| 原生启动图标 | [android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png](../android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png) | 各密度启动图标；现存资源 | 成套保留，禁止只迁移单一密度 |
| 原生启动图标 | [android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png](../android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png) | 各密度启动图标；现存资源 | 成套保留，禁止只迁移单一密度 |
| Web 图片及图标 | [prototype/assets/apple-touch-icon.png](../prototype/assets/apple-touch-icon.png) | 独立演示素材；不等于原生资源 | 保留演示与品牌历史，不自动替换原生资产 |
| Web 图片及图标 | [prototype/assets/demo-chart.png](../prototype/assets/demo-chart.png) | 独立演示素材；不等于原生资源 | 保留演示与品牌历史，不自动替换原生资产 |
| Web 图片及图标 | [prototype/assets/icon-192.png](../prototype/assets/icon-192.png) | 独立演示素材；不等于原生资源 | 保留演示与品牌历史，不自动替换原生资产 |
| Web 图片及图标 | [prototype/assets/icon-512.png](../prototype/assets/icon-512.png) | 独立演示素材；不等于原生资源 | 保留演示与品牌历史，不自动替换原生资产 |
| Web 图片及图标 | [prototype/assets/icon.svg](../prototype/assets/icon.svg) | 独立演示素材；不等于原生资源 | 保留演示与品牌历史，不自动替换原生资产 |
| 字体与授权 | [prototype/assets/fonts/OFL.txt](../prototype/assets/fonts/OFL.txt) | Montserrat Web 字体及 OFL 授权 | 字体和授权同行保存；不推定已用于 Android |
| 字体与授权 | [prototype/assets/fonts/montserrat-light.woff2](../prototype/assets/fonts/montserrat-light.woff2) | Montserrat Web 字体及 OFL 授权 | 字体和授权同行保存；不推定已用于 Android |
| 字体与授权 | [prototype/assets/fonts/montserrat-medium.woff2](../prototype/assets/fonts/montserrat-medium.woff2) | Montserrat Web 字体及 OFL 授权 | 字体和授权同行保存；不推定已用于 Android |
| 字体与授权 | [prototype/assets/fonts/montserrat-regular.woff2](../prototype/assets/fonts/montserrat-regular.woff2) | Montserrat Web 字体及 OFL 授权 | 字体和授权同行保存；不推定已用于 Android |
| 设计参考 | [design/reference/core-screens.png](../design/reference/core-screens.png) | 产品地图、核心线框；历史设计 | 原样保留，标明历史参考 |
| 设计参考 | [design/reference/product-map.png](../design/reference/product-map.png) | 产品地图、核心线框；历史设计 | 原样保留，标明历史参考 |
| 原型截图 | [prototype/screens/chat.png](../prototype/screens/chat.png) | Web 演示画面；非设备验证证据 | 保留版本语境，不作当前原生截图 |
| 原型截图 | [prototype/screens/home.png](../prototype/screens/home.png) | Web 演示画面；非设备验证证据 | 保留版本语境，不作当前原生截图 |
| 原型截图 | [prototype/screens/overview.png](../prototype/screens/overview.png) | Web 演示画面；非设备验证证据 | 保留版本语境，不作当前原生截图 |
| 原型截图 | [prototype/screens/voice.png](../prototype/screens/voice.png) | Web 演示画面；非设备验证证据 | 保留版本语境，不作当前原生截图 |
| 历史安装包 | [android/Bugu-Notes-v0.2.0-debug.apk](../android/Bugu-Notes-v0.2.0-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/Bugu-Notes-v0.2.1-debug.apk](../android/Bugu-Notes-v0.2.1-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.1-debug.apk](../android/DeepSeek-Mobile-v0.1.1-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.2-debug.apk](../android/DeepSeek-Mobile-v0.1.2-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.3-debug.apk](../android/DeepSeek-Mobile-v0.1.3-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.4-debug.apk](../android/DeepSeek-Mobile-v0.1.4-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.5-debug.apk](../android/DeepSeek-Mobile-v0.1.5-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.6-debug.apk](../android/DeepSeek-Mobile-v0.1.6-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.7-debug.apk](../android/DeepSeek-Mobile-v0.1.7-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.8-debug.apk](../android/DeepSeek-Mobile-v0.1.8-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史安装包 | [android/DeepSeek-Mobile-v0.1.9-debug.apk](../android/DeepSeek-Mobile-v0.1.9-debug.apk) | debug 交付；含旧 DeepSeek 品牌和布谷 0.2.0/0.2.1 | 保留原件与发布记录；后续归档前计算校验并确认签名 |
| 历史产品/版本文档 | [docs/01-chatgpt-mobile-benchmark.md](../docs/01-chatgpt-mobile-benchmark.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/02-deepseek-android-product-spec.md](../docs/02-deepseek-android-product-spec.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/03-prototype-tech-plan.md](../docs/03-prototype-tech-plan.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/04-notes-architecture.md](../docs/04-notes-architecture.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/05-feature-audit-and-roadmap.md](../docs/05-feature-audit-and-roadmap.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/06-v0.1.2.md](../docs/06-v0.1.2.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/08-v0.1.8-plan.md](../docs/08-v0.1.8-plan.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/09-v0.1.8-release.md](../docs/09-v0.1.8-release.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/10-v0.1.9-plan.md](../docs/10-v0.1.9-plan.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/11-v0.1.9-release.md](../docs/11-v0.1.9-release.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/12-bugu-notes-v0.2.0-plan.md](../docs/12-bugu-notes-v0.2.0-plan.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/13-bugu-notes-v0.2.0-release.md](../docs/13-bugu-notes-v0.2.0-release.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/14-bugu-notes-v0.2.1-plan.md](../docs/14-bugu-notes-v0.2.1-plan.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |
| 历史产品/版本文档 | [docs/15-bugu-notes-v0.2.1-release.md](../docs/15-bugu-notes-v0.2.1-release.md) | 调研、架构、旧审计、计划与发布记录 | 保留日期/版本，不重写成当前事实；由新入口引用 |

## 工程、数据与保护边界

| 实际路径 | 用途与状态 | 保留方式 |
| --- | --- | --- |
| [android/app/src/main/java/com/dspro/android/ui/Theme.kt](../android/app/src/main/java/com/dspro/android/ui/Theme.kt) | 当前原生浅/深色配色 | 代码内设计 token；本阶段不改，不用旧 Web 配色覆盖 |
| [prototype/styles.css](../prototype/styles.css) | Web token、响应式与动效 | 演示样式原样保留 |
| [android/app/src/main/java/com/dspro/android/data/notes](../android/app/src/main/java/com/dspro/android/data/notes) | 实体、DAO、Repository、内嵌 1→8 迁移、备份与导入 | 整套保护；详细边界见架构，不单独搬动迁移或备份 |
| [android/app/src/main/java/com/dspro/android/data/MockData.kt](../android/app/src/main/java/com/dspro/android/data/MockData.kt) | Android 任务/插件等模拟数据 | 保留且明确演示身份，不当真实用户数据 |
| [prototype/app.js](../prototype/app.js) | Web 状态、种子数据及模拟交互 | 保留独立原型边界 |
| [tools/regression](../tools/regression) | 编辑器、生命周期、网络及历史回归，含测试替身与数据 | 保留当前和历史套件；不因版本目录旧而删除 |
| [android/tools/regression](../android/tools/regression) | ZIP 与迁移补充回归 | 保留；迁移脚本兼容风险见质量说明 |
| [android/build.gradle.kts](../android/build.gradle.kts) | 插件版本 | 不自动升级 |
| [android/settings.gradle.kts](../android/settings.gradle.kts) | 单 app 模块及依赖仓库 | 保持现状，迁移前验证解析 |
| [android/app/build.gradle.kts](../android/app/build.gradle.kts) | 版本、SDK、依赖及打包 | 保护 applicationId 和版本语义；本阶段不改 |
| [android/gradle](../android/gradle) | Wrapper jar 与分发配置 | 成套保留 |
| [android/gradlew](../android/gradlew) | Unix 构建入口 | 保留执行属性 |
| [android/gradlew.bat](../android/gradlew.bat) | Windows 构建入口 | 原样保留 |
| [android/gradle.properties](../android/gradle.properties) | Gradle 工程参数 | 本阶段不改 |
| [android/app/proguard-rules.pro](../android/app/proguard-rules.pro) | 现有混淆规则文件 | 原样保留；不能据文件存在声称 release 已启用混淆 |
| [android/app/src/main/AndroidManifest.xml](../android/app/src/main/AndroidManifest.xml) | 应用入口、权限、allowBackup | 保护并评估备份边界；本阶段不改 |
| [android/local.properties](../android/local.properties) | 本机 SDK 配置；仅识别位置，未读取内容 | 保留本机使用，不复制为通用配置或输出内容 |
| [android/app/build](../android/app/build) | 既有编译产物、生成 schema、Lint 报告，审计约 215 MB | 可再生候选；现有测试依赖其产物，重现基线前不清理 |
| [android/.gradle](../android/.gradle) | 本地构建缓存，审计约 10 MB | 可再生候选；离线构建确认前保留 |
| [android/.kotlin](../android/.kotlin) | Kotlin 会话与错误记录 | 可整理候选；保留有用故障证据，不批量输出日志 |
| [.DS_Store](../.DS_Store) | 本机目录元数据 | 可整理候选，本阶段未处理 |
| [README.md](../README.md) | 历史产品导航 | 保留历史正文，添加新工程入口 |
| [android/README.md](../android/README.md) | 原生使用、构建及数据说明，正文停留 v0.2.0 | 有效说明引用复用；当前版本以 current-state 为准 |
| [prototype/README.md](../prototype/README.md) | 演示能力与限制 | 保留并引用，不重复其完整清单 |

## 仓库外与未确认资产

- `dsh_notes.db` 和应用私有 `attachments/` 是运行时数据位置约定，不是当前仓库已发现文件。数据库、附件、草稿和上下文路径须整体保护；实际设备位置、规模与备份副本未确认。
- `ds_api_secure` / `ds_api_fallback` 是代码中的偏好存储名称，不是已读取的凭据文件。密钥、签名私钥不得登记值或复制进文档。
- 检查范围内未发现真实数据库文件、正式签名文件、独立设计源文件或后端部署配置；它们是否存在于其他位置未确认。
- v0.2.1 APK 校验及签名历史记录引用 [发布记录](15-bugu-notes-v0.2.1-release.md)，当前核对程度见 [current-state](current-state.md)。
- 工程新文档入口见 [AGENTS](../AGENTS.md)、[架构](../ARCHITECTURE.md)、[产品](product.md)、[质量](quality.md)、[决策](decisions/README.md)、[执行计划](exec-plans/000-project-reboot.md)。
