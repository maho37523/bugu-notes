# M01：设计资产整理

日期：2026-11-06。用户明确开启本项渐进迁移；范围仅设计资产组织、引用与验证，不授权下一项迁移。依据：[根协作约定](../../AGENTS.md)、[架构](../../ARCHITECTURE.md)、[总计划](000-project-reboot.md)、[原资产清单](../assets.md)。

## 当前行为、依赖与保留接口

- Android `Components.kt` 的 DshWordmark 使用 `R.drawable.bugu_title`；Manifest 使用 `@mipmap/ic_launcher`。原图保存在打包 assets/brand；未发现需要把原图移动出 APK 的既定决策，因此保留原位。
- Web 的 HTML、CSS、Manifest、app.js 和 Service Worker 消费 assets 下的图标、字体和示例图。原型 README 使用 screens/overview.png。所有这些 URL 和字体许可位置保持不变。
- Theme.kt 和 styles.css 分别是原生和原型的样式维护位置，不提取或合并 token，不改变颜色、字体或布局。
- 两张 docs/assets 参考图只供文档消费，本次检索未发现运行时或测试引用；现有编辑器回归只覆盖文本格式等逻辑，没有专用资产路径测试。
- 应用身份、资源 ID、API、数据库/迁移/备份格式、公开函数与产品行为均不改。若后续发现需要修改其他模块接口，应停止并报告。

## 步骤与文件对应

第一步只移动两张 PNG，并立即比较迁移前 SHA-256；第二步更新文档引用与资产元数据。没有混入代码重构，没有重编码、压缩、裁剪或复制视觉原件。

| 迁移前 | 迁移后 | 原因 |
| --- | --- | --- |
| `docs/assets/product-map.png` | [design/reference/product-map.png](../../design/reference/product-map.png) | 将历史设计参考集中到明确设计目录 |
| `docs/assets/core-screens.png` | [design/reference/core-screens.png](../../design/reference/core-screens.png) | 同上；保留文件名与内容 |
| Android / Web 运行时图片、字体、截图、样式 | 原路径 → 原路径 | 保持构建资源和浏览器 URL 兼容，不扩展接口改动 |

新增：[design/README.md](../../design/README.md)（分类、消费关系、字节数和 PNG 尺寸）、[design/SHA256SUMS](../../design/SHA256SUMS)（23 个视觉/许可文件及 2 个代码内样式源的校验）、本记录。

修改：[根 README](../../README.md)、[历史产品定义](../02-deepseek-android-product-spec.md)（仅更新两图链接）、[assets](../assets.md)（路径与设计入口）、[总计划](000-project-reboot.md)（M01 授权与状态）。旧产品正文未重写；其他计划步骤未勾选为完成。空的原 docs/assets 目录未专门删除。

## 验证证据

验证临时工作目录：`/private/tmp/bugu-m01-3zldret2`。迁移前保存 122 个源码、测试、原型与配置文件的内容哈希，以及 23 个视觉/许可文件哈希；对修改文档保存临时前副本。未读取 local.properties 或真实凭据。临时目录是本次检查证据，不是长期数据备份。

| 实际命令 / 检查 | 结果与范围 |
| --- | --- |
| `shasum -a 256 -c design/SHA256SUMS`，项目根 | 25 项通过；两张移图及其余登记内容字节一致 |
| `bash tools/regression/editor018/run.sh` | 两次通过：第一次在项目根使用旧编译类；第二次将未修改的 editor018 套件复制到隔离副本相同相对位置，在 `/private/tmp/bugu-m01-3zldret2` 使用本次新编译类。覆盖原文/选区、撤销重做、列表、代码、中文强调、查找替换及 4,000 段落；不能替代设备视觉验证 |
| `python3 /private/tmp/bugu-m01-3zldret2/validate_m01.py` | 通过：122 个保护文件不变、23 个视觉/许可文件字节一致、237 个 Markdown 相对链接、20 个 Web 资源引用有效，7 个变更文本空白/no-index 检查无诊断。该脚本是本次临时检查工具，不是新增工程测试入口 |
| `git diff --check`，项目根 | 无 Git 元数据，退出 129，无法完成真实 Git 差异检查；补做 no-index 与空白检查，不初始化 Git |
| `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/Users/cris/Library/Android/sdk ./gradlew --offline --no-daemon :app:assembleDebug :app:lintDebug` | 在临时 Android 源码副本执行；首次沙箱执行因用户 Gradle 缓存锁不可写而退出 1；获工具权限后重试成功，46 秒、48 个任务，Lint 0 errors / 4 warnings |
| Python 标准库 zipfile 检查新 APK | 通过：新包 `assets/brand/` 的两张原图与源码逐字节一致 |

隔离副本不复制 local.properties、旧 build/.gradle/.kotlin 或历史 APK，不启动应用、不操作用户数据、不安装依赖。数据库/网络回归与本次路径整理无直接关联，未执行；设备视觉、服务在线验证未执行，不声称端到端通过。

本次 Lint 报告位于 `/private/tmp/bugu-m01-3zldret2/android/app/build/reports/lint-results-debug.txt`，新 APK 位于同副本 `app/build/outputs/apk/debug/app-debug.apk`。4 个警告分别为既有 Sidebar 参数顺序及依赖 BouncyCastle 的 3 项 TLS 信任管理器提示；运行时可达性未确认，不在 M01 修复。与历史 12 个警告的数量不同，不表示本次修复了依赖警告。编译另有既有弃用/预览 API 提示，过程中的 Kotlin daemon 首次启动提示未导致最终失败。

## 风险、遗留与回退

- 仓库外直接引用旧 docs/assets 图片路径的链接可能失效，仓库内引用统一更新；没有证据能覆盖仓库外消费者。
- 品牌/图片授权来源、原图到图标的生成方法仍未确认；保留 OFL 文件不意味着所有图片均有授权。
- 质量基线、原 Git 来源及源码/APK 对应仍未全面确认；本次完成不意味着总重启计划完成。
- 回退仅需将两张 PNG 移回旧位置，恢复上述四个已修改文档的 M01 改动，移除三个本次新增文件；不回滚业务数据，不触碰运行时资源或历史 APK。先确认无后续修改，不能直接覆盖他人的新内容。

## 最终结果

M01 最小设计资产整理完成，验证结果如上。仅两张文档参考图发生路径迁移，未改变公开函数、应用资源 ID、运行时 URL、视觉内容或业务行为；仓库内图片链接改为新位置。没有业务代码重构、依赖/构建配置/现有测试修改、提交、推送、部署或下一项迁移。
