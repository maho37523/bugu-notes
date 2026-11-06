# 布谷笔记 v0.2.1 实施计划

## 目标与交互

1. 笔记使用同一套连续排版：去掉显式编辑/预览切换，点击正文定位并调出键盘；收起键盘进入阅读状态，返回键先收键盘，再返回保存。活动行显示必要的 Markdown 语法，其余行按所见即所得呈现；阅读状态所有行以相同排版显示。标题、列表、引用、强调、链接及代码需保持原始 Markdown，可编辑、选择、撤销、查找替换，支持中文输入法。避免隐藏字符保留宽度造成缩进或切换两套布局造成错位。实现真实原文到显示文本的双向 offset 映射，确保边界、选区、组合输入、换行和 Unicode/emoji 安全；若某语法无法稳定映射，优先保持内容可编辑与不丢失。
2. 对话消息的块间距、气泡内边距、时间和操作区按内容自然排列；避免段落末尾叠加额外空白，压缩消息之间的间距，保留足够点击区域及多段、代码、长消息、流式输出和引用布局。
3. API 设置提供 DeepSeek、OpenAI 兼容自定义及可用的服务商预设（例如 OpenAI、阿里云百炼），允许填写模型与各自 Base URL、Key；每个供应商独立保存凭据，旧设置无损迁移，切换服务时绝不能将旧服务 Key 发往新地址。兼容接口只发送通用参数，DeepSeek 专有参数只发给 DeepSeek；发送、重试、测试连接使用同一配置。修复流式空白增量/同一增量含推理与正文时的数据丢失。网络请求必须以真实供应商响应为准，未配置或不支持时明确说明。
4. DeepSeek 添加用户可控联网开关。官方 Search API 可用同一 DeepSeek Key（POST https://api.deepseek.com/v1/tools/search），适用于当前两款模型；仅在启用且当前供应商为 DeepSeek 时发起搜索，使用最多 128 Unicode 字符的明确查询，展示真实搜索进度、失败/无结果和可打开的实际来源，将有界搜索结果作为不可信资料提供给模型，禁止伪造来源或显示虚构的阅读计数。搜索请求可能额外计费，应在开关附近提示；其他供应商不可静默假装联网。参考 https://api-docs.deepseek.com/guides/search/ 和 https://api-docs.deepseek.com/guides/responses_api/ 。如采用 Responses 服务端 web_search，须限制 deepseek-v4-pro，Flash 会静默忽略该工具；必须处理真实 SSE 状态和来源。
5. 修复笔记取消归档：NotesDao.setArchived 不能用 nullable archivedAt 写非空 updatedAt；archive/unarchive 均使用有效更新时间。检查对话与笔记恢复流程、筛选与项目范围，归档列表提供明确恢复入口并反馈结果，保留原内容、项目和文件夹。异常需反馈，不得假报成功。

## 验证与交付

- 更新到 v0.2.1 / versionCode 13，保留旧 APK 和 applicationId；记录计划、实现、局限、来源、测试和签名。
- 为映射及编辑添加覆盖活动/非活动、段落/标题/有序无序列表、引用/强调、空行、中文、emoji、反向选区、边界/输入法组合、原文往返与撤销的实际回归；评估编辑器点击与键盘交互。
- 以实际 DAO SQL + repository 验证笔记 archive→unarchive（非空 updatedAt、正文与归属保留），覆盖对话、筛选及垃圾桶交互。
- 为配置迁移和供应商隔离、请求体/URL、SSE 空白/推理正文增量、搜索成功/空结果/失败与来源提供离线 fixtures；无真实凭据不得声称在线成功。
- 运行 assembleDebug、lintDebug 及相关现有回归，验证 APK 元数据、签名，尝试设备或模拟器检查并如实记录。生成 android/Bugu-Notes-v0.2.1-debug.apk。
