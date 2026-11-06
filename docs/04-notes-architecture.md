# 笔记模块架构与 v0.1.1 实现

> v0.1.2 已删除笔记内 AI，并改为单栏混合编辑器、项目与版本；最新实现见 [06-v0.1.2.md](06-v0.1.2.md)。

> 版本：v0.1.1　|　状态：已实现　|　范围：Android 原生

## 1. 产品结构

笔记和对话是两个一级入口，底部 Tab 等权：

```
笔记 Tab                         对话 Tab
├─ 笔记列表                      ├─ 会话
├─ 文件夹 / 标签筛选              ├─ @引用笔记
├─ Markdown 编辑器                ├─ 回答保存为笔记
└─ AI 建议 → 确认                 └─ 引用 chip
        └──────── 共享 Room + 上下文 ────────┘
```

用户已确认的五个决定：

1. 编辑器：源码 + 预览，不做 WYSIWYG
2. 一级导航：底部「笔记 / 对话」
3. 组织：文件夹 + 标签 + `[[双链]]`
4. AI 写入：所有修改走「建议 → 确认」
5. 存储：应用私有目录 + 导出 `.md`

## 2. 分层

```
UI (Compose)
 ├─ NotesScreen          # 列表 / 筛选 / 搜索
 ├─ NoteEditorScreen     # 源码 / 预览 / 工具栏 / AI 建议
 └─ ChatScreen           # 对话 + 引用笔记
ViewModel
 ├─ NotesViewModel       # 列表、筛选、CRUD
 └─ Chat state (DshApp)  # 现有对话状态
Data
 ├─ NotesDatabase (Room)
 ├─ NotesRepository
 ├─ NotesDao
 └─ ApiConfigStore       # 现有，加密保存 API Key
AI
 └─ DeepSeekClient       # 现有 SSE 客户端，编辑器复用
```

当前仍在单 module 内按 feature 分包；接口稳定后再拆 Gradle module。

## 3. 数据模型

| 表 | 作用 |
| --- | --- |
| `notes` | Markdown 正文真源、标题、文件夹、置顶、软删除 |
| `folders` | 文件夹 |
| `tags` / `note_tags` | 标签与多对多关联 |
| `note_links` | `[[标题]]` 解析结果、反向链接 |
| `note_revisions` | AI 修改前的版本快照 |

关键约定：

- 正文只存 Markdown，预览 AST 不落库。
- 笔记 ID 用 UUID。
- `note_links.toNoteId` 允许为空：目标笔记尚不存在时仍保留链接；目标创建后自动回填。
- 删除采用软删除；当前版本没有恢复入口。
- 数据库文件在应用私有目录，卸载即删除。
- 导出使用系统 `CreateDocument`，只导出当前 `.md`。

## 4. 编辑器

- `BasicTextField` + `TextFieldValue`，Monospace。
- 工具栏：粗体、斜体、标题、列表、引用、代码、链接、双链、AI。
- 撤销 / 重做：`TextFieldValue` 快照栈，最多 100 步。
- 自动保存：停止输入 500ms 写入 Room；返回时再保存一次。
- 预览：`MarkdownText` 的 Compose 渲染；手机切换编辑/预览，宽度 ≥ 720dp 左右分屏。
- 双链：预览中的 `[[标题]]` 可点击；预览底部列出「链接的笔记」和「反向链接」。
- AI：选中文本 → 选择润色/扩写/缩短/总结/翻译 → 流式建议 → 替换/插入/取消；应用前写入 revision，不直接覆盖。

## 5. 对话 ↔ 笔记

- 对话 Composer 的 `@` 按钮打开笔记选择器，选中后显示引用 chip。
- 发送时把笔记正文作为 `system` 上下文，最多 6000 字；用户消息上显示引用来源。
- 助手回答的操作栏增加「保存为笔记」，标题取回答第一行，正文原样保存。
- 笔记侧没有主动向对话注入全文；所有引用都在发送前明确显示。

## 6. 仍然模拟的部分

- 图片/视觉理解、联网搜索、任务执行、语音 ASR/TTS
- 笔记版本历史 UI（表已存在）
- 文件夹/标签重命名、删除
- 云同步、协作、向量检索

## 7. 后续扩展点

- 把 `MarkdownText` 换成 CommonMark AST 渲染，补表格/任务列表/代码高亮
- 笔记内 AI 侧边面板，保留多轮上下文
- Room FTS4 替换当前 LIKE 搜索
- 版本历史与恢复入口
- 导出整个文件夹为 Markdown 包
