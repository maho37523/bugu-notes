# 布谷笔记（Bugu Notes）

本地优先的 Android 笔记与 AI 对话应用，使用 Kotlin、Jetpack Compose 和 Room。支持 Markdown 笔记、项目与嵌套目录、对话草稿、引用上下文，以及业务备份与恢复。

当前开发分支：**v0.5.1 / versionCode 17 候选**，增加 ChatGPT 官方订阅授权连接。GitHub 的 `main` 仍是已发布源码的 v0.4.0 启动修复版；用户已反馈真实登录可用；新候选的模型/强度交互仍待用户验收。

ChatGPT 连接使用系统浏览器、PKCE 和官方动态公共客户端注册，账号凭据独立加密保存；模型来自所选账号目录，请求使用官方 Responses 路径和用户授予的订阅权限。它与既有 OpenAI API Key 分开，限额或授权失败不会静默转用付费 API。首版支持文本、引用笔记和文本附件；图片、语音和服务端工具不在当前验证范围。见 [实施记录](docs/exec-plans/T-20261107-chatgpt-login-implementation.md) 和 [官方路径研究](docs/exec-plans/T-20261107-openai-subscription-research.md)。

笔记、项目、目录、对话草稿和备份维持 schema 9；旧草稿失效目录的启动修复仍保留。见 [修复记录](docs/exec-plans/T-20261107-v040-startup-crash.md)。

## 构建与验证

Android 工程位于 `android/`。现有工具链为 JDK 21、Android SDK 35 / Build Tools 35.0.1、Gradle 8.9；应用 minSdk 26、targetSdk 35，Kotlin/Java 字节码目标 17。使用自己的 Android SDK 配置，不提交 `local.properties` 或签名私钥。

```sh
cd android
./gradlew :app:assembleDebug :app:lintDebug
```

本项目日常检查入口为 `python3 tools/check.py quick`；已有离线环境下的完整隔离检查为 `python3 tools/check.py baseline`。部分回归入口仍依赖既有 Mac 路径和离线缓存，可执行范围与限制见 [质量规则](docs/quality.md) 和 [回归说明](tools/regression/README.md)。源码仓库不包含用户笔记、API 凭据或本机回退备份。仓库中保留的旧 APK 是历史归档，不是本次修复交付包。

## 工程文档

[当前状态](docs/current-state.md) · [架构](ARCHITECTURE.md) · [产品范围](docs/product.md) · [资产](docs/assets.md) · [质量](docs/quality.md) · [任务索引](docs/exec-plans/README.md) · [协作规则](AGENTS.md)

## 许可证

原创源代码采用 [MIT License](LICENSE)。第三方依赖和资源仍遵循各自许可证，例如 [Montserrat 字体许可](prototype/assets/fonts/OFL.txt)。

## 历史调研与交付资料

以下保留早期版本背景，不作为当前实现、发布或验证结果。

> 调研基准日：2026-11-03　|　最新 Android 交付：布谷笔记 v0.2.0　|　范围：Android 手机优先

## 这次交付什么

| 文档 | 内容 | 适合谁看 |
| --- | --- | --- |
| [01 ChatGPT 移动端拆解](docs/01-chatgpt-mobile-benchmark.md) | ChatGPT 现在到底有哪些能力、哪些是移动端核心、哪些是生态功能 | 产品 / 设计 |
| [02 产品定义与交互逻辑](docs/02-deepseek-android-product-spec.md) | DeepSeek 版要做什么、不做什么、每个关键流程怎么走 | 产品 / 设计 / 研发 |
| [03 原型实现方案](docs/03-prototype-tech-plan.md) | 接哪些 API、用什么技术、分几步做、哪些先 Mock | 研发 |
| [产品地图](design/reference/product-map.png) | 一页看懂定位、信息架构、核心循环与功能分层 | 所有人 |
| [核心界面线框](design/reference/core-screens.png) | 首页、回答、语音通话三个关键画面的结构示意 | 产品 / 设计 |
| [Android 原生原型](android/README.md) | Kotlin + Compose 的可安装 APK，按参考图落成 | 所有人 |
| [笔记模块架构](docs/04-notes-architecture.md) | 笔记/对话双入口、Room 数据模型、Markdown 编辑器与 AI 互通 | 产品 / 研发 |
| [v0.1.2 实现记录](docs/06-v0.1.2.md) | 项目、混合编辑器、版本、垃圾桶、附件、引用等本版实现与缺口 | 产品 / 研发 |
| [v0.1.8 发布记录](docs/09-v0.1.8-release.md) | 本版范围、APK 校验信息和回归结果 | 所有人 |
| [布谷笔记 v0.2.0 发布记录](docs/13-bugu-notes-v0.2.0-release.md) | 编辑器排版、品牌和主题更新及验证结果 | 所有人 |
| [Web 交互原型](prototype/README.md) | 上一轮用于快速验证交互的 PWA | 产品 / 设计 |

> **布谷笔记 v0.2.0** 在统一资料库基础上修正 Markdown 编辑器光标排版，并更新品牌、图标和薄荷绿/深棕主题。下载 [v0.2.0 APK](android/Bugu-Notes-v0.2.0-debug.apk)，构建方式见 [Android README](android/README.md) 和 [发布记录](docs/13-bugu-notes-v0.2.0-release.md)。

## 先看结论

1. **ChatGPT 移动端已经不是“聊天框”**，而是“一个输入框 + 三种执行深度 + 一套上下文系统”。三种执行深度是 Chat（快问快答）、Work（长任务与交付物）、Scheduled（定时/触发）；上下文系统是记忆、项目、文件、连接应用。真正值得对齐的是这条“从一句话到可交付结果”的连续体验，而不是把功能清单逐项抄一遍。

2. **DeepSeek 不该复刻 ChatGPT 的“模式选择”**。ChatGPT 让用户在 Chat / Work 之间切换，是因为它的能力边界清晰；DeepSeek 9 月已经把快速、专家、识图合并成一个自动模式，这是更先进的默认体验。原型应该把它做成“**自动路由 + 结果可见 + 随时接管**”，而不是退回模型下拉框。

3. **首版原型建议做成“一个输入框，三条深化路径”**：
   - 直接回答（含思考过程、搜索引用）
   - 深度研究（需要多步查证时）
   - 定时任务（需要持续发生时）

   三者都沉淀在同一条对话里，不新增“工作台”概念，避免用户学习成本。

4. **最大的留存杠杆是记忆 + 项目，不是语音**。语音很吸睛，但使用频次低；记忆和项目决定用户第二天为什么还回来。首版必须把这两件事做对。

5. **最大的风险是范围失控**。ChatGPT 有 40+ 项能力，其中一半依赖生态、区域或付费。原型阶段建议明确“不做清单”，把资源集中在 8 个 P0 能力上。

## 建议的首版范围

### P0：没有就不像“完整产品”

- 对话流：流式输出、思考过程、停止/重试/重新生成、多版本、编辑消息
- 统一输入框：文字、图片、文件、听写、语音通话入口、工具面板
- 联网搜索：自动判断、过程可见、行内引用、来源下钻
- 图片理解：拍照/相册、多图、OCR、追问
- 文件问答：PDF/Office/图片、解析状态、页码级引用
- 语音通话：全屏、打断、字幕、转写回聊天（可先半双工）
- 记忆：自动提取、可见可改、按对话关闭
- 项目：资料、指令、项目内对话、项目级记忆
- 历史与分享：搜索、置顶、归档、长图/链接导出
- 基础设置、异常态、离线态

### P1：决定“比别人好用”

- 深度研究：澄清 → 计划 → 执行 → 报告 → 导出
- 定时任务：自然语言创建、推送、运行历史
- 画布：文档/代码/网页产物的编辑与预览
- 后台语音、锁屏继续
- Android 集成：分享目标、桌面小组件、快捷设置磁贴

### P2：先留入口，不投入实现

- 多账号切换、锁定对话
- 项目共享/协作
- 连接器（Google Drive、Notion、MCP）
- 主动式每日简报（Pulse 类）
- 学习模式（引导式辅导 / 测验）
- 图像生成（DeepSeek 暂无该能力）
- Work 类浏览器自动化

### 明确不做

- 图像生成 / 视频生成
- 浏览器自动化、云端电脑
- 插件市场
- 多人实时协作
- 健康、金融、购物等垂直业务
- CarPlay / 车机

## 需要你拍板的 5 件事

1. **原型目标**：是给投资/管理层看的可点击演示，还是要给研发做功能验证的可运行 MVP？前者可以大量 Mock，后者核心链路必须真。
2. **API Key**：现在已改成应用内「API 设置」直接配置，Key 由使用者自己填；如果要做多人共享、额度控制或文件解析，再考虑加最小后端。
3. **语音方案**：DeepSeek 公开 API 文档目前未见实时语音接口，原型是接第三方 ASR/TTS，还是先做“录音 → 转写 → 文字回答 → TTS 朗读”的半双工？
4. **目标市场**：国内还是海外？这决定登录方式、推送通道、地图/支付、是否依赖 GMS。
5. **导航形态**：4 个底部 Tab（对话/项目/任务/我的，易发现、偏国内习惯）还是 ChatGPT 式抽屉（更沉浸、更简洁）？我在文档里默认按 4 Tab 设计，但这一条可以改。

---

## 文档怎么读

- 只想快速判断方向：看本页 + [产品地图](design/reference/product-map.png)
- 想评审功能取舍：看 [01 拆解](docs/01-chatgpt-mobile-benchmark.md) 的功能全景表
- 想评审交互：看 [02 产品定义](docs/02-deepseek-android-product-spec.md) 第 7 章的关键流程
- 想评估能不能做：看 [03 实现方案](docs/03-prototype-tech-plan.md) 的 API 映射与里程碑
- 想直接体验：跑 [prototype](prototype/README.md)，或先看 [核心画面](prototype/screens/overview.png)
