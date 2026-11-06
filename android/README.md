# 布谷笔记 · Android

原生 Android 应用，包含笔记、对话、项目范围、Markdown 编辑和本地数据管理。

技术栈：Kotlin、Jetpack Compose、Material 3、Room。`minSdk 26`、`targetSdk 35`。

## 构建

```bash
cd android
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/Users/cris/Library/Android/sdk
./gradlew --offline :app:assembleDebug :app:lintDebug
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## v0.1.9

- 默认首页是统一资料库，可筛选全部内容、笔记或对话；文件夹可混合收纳两种内容。
- 顶部提供项目范围、搜索和统一的新建菜单；新建菜单创建笔记、对话或文件夹。
- 对话在首次发送前保存在项目范围的临时草稿中，不会提前写入会话列表；首条消息通过配置和上下文校验后，再创建正式会话。
- 笔记和对话都能归档、恢复、移入回收站或确认后永久删除。侧栏提供资料库、类型筛选、归档、回收站、分类管理和全局设置。
- 搜索支持项目范围、类型、混合文件夹与笔记标签筛选；Markdown 导入、完整数据备份、项目记忆隔离和已有会话草稿继续保留。
- Room 数据升级会保留旧笔记和对话；备份仍包含内容、会话草稿和附件信息。新笔记只有在正文或主动填写标题后才保存。
- Markdown 预览保留单换行、空行、引用和列表分段；窄屏工具栏可收缩标题，交互按钮保持 44dp 点击区。
- 笔记、对话、记忆、附件和草稿存于本机。配置 API 后，请求内容会发送到 API 设置中显示的服务地址；API Key 不包含在数据备份内。

## 使用真实 API

打开「设置 → API 设置」，配置 API Key、Base URL 和模型，然后保存。应用使用 `/chat/completions` 的流式接口。未配置 API Key 时，发送会引导到 API 设置。

## 数据管理

完整备份会导出应用数据库记录和附件。导入会先显示备份记录摘要，再要求确认替换当前笔记、对话、记忆、附件和草稿。API Key 与模型连接设置不会从备份导入。

Markdown 导入会保存到当前工作区和笔记页当前选中的文件夹。编辑器仍支持单篇 Markdown 导出。

## APK

```text
Bugu-Notes-v0.2.0-debug.apk
```

v0.2.0 将笔记编辑器的隐藏 Markdown 标记改为仅透明绘制，保持字形尺寸和 offset 对齐；占位符与输入文字共用排版样式。应用品牌为布谷笔记，DeepSeek 模型和 API 设置仍按原服务显示。主题使用薄荷绿与深棕色，暗色侧栏标题置于浅色底板以保证可读性。
