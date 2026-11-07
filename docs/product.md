# 产品范围与功能状态

布谷笔记当前围绕本地笔记、对话、项目范围、Markdown 编辑和数据管理组织。依据是 [Android 实现说明](../android/README.md)、[当前状态](current-state.md) 及下列代码；早期 AI 移动端全景需求保留为历史规划。

「已实现」仅表示可找到代码链路；「未实现」表示在当前检查范围内未找到真实闭环或明确为模拟；运行成功、设备体验和服务兼容性另列「待确认」。

## 已实现（代码证据）

以下源码路径均相对 `android/app/src/main/java/com/dspro/android/`，目录入口见 [源码](../android/app/src/main/java/com/dspro/android)。

| 能力 | 代码位置与边界 |
| --- | --- |
| 统一资料库、筛选与内容操作 | `ui/LibraryScreen.kt`、`LibraryModels.kt`、`DshApp.kt` |
| 笔记保存、空白新笔记清理、文件夹与标签 | `data/notes/NotesRepository.kt`、`NotesDao.kt`、`ui/notes/NotesViewModel.kt` |
| 连续 Markdown、撤销重做、查找替换、成对符号 | `ui/notes/ContinuousNoteEditor.kt`、`NoteEditorScreen.kt`；零宽替代与 identity offset 的设备边界待确认 |
| 手动版本、自动检查点与恢复 | `NoteEditorScreen.kt`、`NotesRepository.saveVersion()` |
| 双链与反向链接 | `NotesRepository.kt`、`NotesDao.kt`；同名及跨项目解析风险见 current-state |
| 项目、指令及手动记忆 | `ProjectsRepository.kt`、`MemoryRepository.kt`、对应 UI 与 `DshApp.kt` |
| 聊天、草稿、首条消息事务提交 | `ChatRepository.kt`、`ChatDao.kt`、`DshApp.kt` |
| 归档、回收站、恢复、搜索 | Notes/Chat Repository、`WorkspaceSearchService.kt`、`ui/notes/TrashScreen.kt` |
| API 配置及流式调用 | `data/ApiConfig.kt`、`ApiProtocol.kt`、`DeepSeekClient.kt`；按供应商保存配置，v0.3.0 的 Claude 适配正在验证，在线结果未确认 |
| DeepSeek 可选搜索及真实 URL 来源展示 | `DeepSeekSearchClient.kt`、`DshApp.kt`；在线成功待确认 |
| 引用上下文快照 | `ContextSnapshotService.kt`；最多 8 篇，总笔记正文预算 12,000 字符 |
| Markdown 导入、单篇导出、完整业务备份恢复 | `MarkdownImportService.kt`、`NoteEditorScreen.kt`、`BackupService.kt`、`BackupZipCodec.java` |

v0.4.0 项目目录 scope、嵌套目录树、父级操作和内容上下文新增已完成源码实现，并通过本机离线构建/Lint及合成回归；触屏、中文输入法和实际视觉仍未设备验收。详细交接见 [v0.4.0 实施任务](exec-plans/T-20261107-v040-implementation.md)。

## 部分实现

- 附件：已实现本地选择、文件复制、文本/PDF 提取和单图请求编码；Office、OCR、通用上传、解析重试及页码引用未形成完整链路。拍照入口明确未接入。
- 记忆：已有手动维护、回答保存为记忆及请求注入；未发现自动提取闭环。
- 编辑器：已有连续排版与原文保存；隐藏语法触摸边界、真实中文 IME、键盘切换未完成设备验证。限制见 [发布记录](15-bugu-notes-v0.2.1-release.md)。

## 未实现真实闭环 / 仅演示

- Android `ui/VoiceScreen.kt` 使用固定文本与延时，未发现真实录音/ASR/TTS。
- Android 任务和插件使用 `data/MockData.kt` 与内存开关；未发现后台调度或连接器执行。分享、朗读操作显示演示提示。
- [Web 原型](../prototype/README.md) 的 AI、研究、语音、任务和分享导出属于模拟交互，不代表原生能力。
- [早期产品定义](02-deepseek-android-product-spec.md)、[旧审计及路线图](05-feature-audit-and-roadmap.md) 中的真实深度研究、推送任务、电脑 DSH 配对/远程执行、云同步、协作、连接器、后台语音、小组件等，当前未发现完整原生实现证据。历史 P2 或明确不做项不自动纳入重启范围。

## 待确认与范围决定

真实供应商/模型及搜索服务兼容性、图片理解成功、进程死亡后的草稿恢复、长文/附件性能、数据恢复可靠性和无障碍体验均需后续验证。目标市场、正式发布标准、远程执行是否进入范围及功能优先级需人工确定；本文件不新增产品承诺。

## v0.5.0 ChatGPT 订阅连接候选

开发分支已接入独立的 ChatGPT 官方公共客户端登录、账号/模型选择、授权更新与退出，以及只使用该账号订阅授权的 Responses 请求。界面显示订阅来源并提供用量管理入口；不会在失败时自动切换 API Key 或其他计费来源。首轮为文本、笔记和文本附件，图片及未验证能力有明确拒绝。代码和合成验证不代表账号实际有资格或在线模型调用成功；真实浏览器授权、账号权限和在线体验待验收。现有 API 接入、本地内容及 schema 9 保持兼容。详细证据见 [实施记录](exec-plans/T-20261107-chatgpt-login-implementation.md)。
