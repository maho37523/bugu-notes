# 布谷笔记 v0.2.1 发布记录

## 版本与安装包

- Version name：`0.2.1`
- Version code：`13`
- APK：[Bugu-Notes-v0.2.1-debug.apk](../android/Bugu-Notes-v0.2.1-debug.apk)
- SHA-256：`01e23b033018ca9b8bb9a5fbfb1a12b06cb3564128651318a6b54ba1ca14676f`
- applicationId 保持 `com.dspro.android`；旧版 APK 未改写。
- Android SDK 元数据：package `com.dspro.android`、version code `13`、version name `0.2.1`。
- `apksigner verify --verbose --print-certs`：v2 签名通过，单个 Android Debug 签名者。证书 SHA-256：`b55864e4aaf8cca0f1e27b259c43715d16fbf9b172e500fc540c61dbfad53917`。

## 更新

- 笔记编辑和阅读共用一个 `BasicTextField` 排版。活动焦点行显示 Markdown 标记；焦点离开或软键盘收起后，标题、引用与强调标记以等 UTF-16 长度零宽字符显示，选区映射保持 identity；列表继续显示项目符号、编号和间距。收键盘后按返回会回到保存/退出流程。工具栏提供双链列表入口，选择后可打开对应笔记。
- 对话列表消息间距从 22dp 收到 12dp。
- 归档区的笔记和会话行提供明确的恢复图标按钮，行点击也可恢复；成功会提示“已取消归档”。取消笔记归档时使用非空 `updatedAt`，保留内容及项目/文件夹归属。
- API 设置支持 DeepSeek、OpenAI、阿里云百炼与 OpenAI 兼容自定义服务。供应商各自保存 Key、Base URL、模型；旧通用 DeepSeek 配置首次加载时迁移到 DeepSeek 专属字段。通用供应商请求不会携带 DeepSeek 专有 thinking 参数。清除 Key 仅清理当前供应商。
- DeepSeek 可选择联网搜索。仅在启用、供应商为 DeepSeek、Base URL 主机为官方 `api.deepseek.com` 时请求官方 Search API；只使用本轮用户文本，截取最多 128 个 Unicode 字符。显示真实搜索进度、无结果或失败，搜索来源显示实际标题/网址并可打开；结果作为不可信资料传给模型，不生成虚构来源或阅读计数。DeepSeek 官方说明：[Search & Fetch](https://api-docs.deepseek.com/guides/search/)；[Responses API](https://api-docs.deepseek.com/guides/responses_api/)。本版本采用 Search API，不采用 Responses API 的服务端 web_search。
- Room schema 从 7 升到 8，保存聊天来源及查询 JSON。旧 v0.2.0 备份缺少新字段时使用数据库默认值继续恢复。
- SSE 解析同时保留 reasoning 与正文增量，也保留仅含空格的正文；取消请求会断开 HTTP 连接并向上传递取消状态。

## 验证

- `:app:assembleDebug :app:lintDebug`：通过；Lint 0 个错误、12 个警告（包含依赖和既有提示）。
- `tools/regression/editor018/run.sh`：通过；覆盖原文长度/往返、撤销重做、活动/非活动列表、编号/项目符号、标题、引用、强调、链接、代码、中文、emoji、反向选区、identity offset 和 4,000 段落。
- `tools/regression/lifecycle019/run.sh`：通过；覆盖实际 DAO SQL 的 archive→unarchive，更新时间、正文/归属保留，对话生命周期、搜索、v0.2.1 备份往返、v0.2.0 v7 备份和 v0.1.8 v6 备份兼容。
- `python3 tools/regression/migration019.py`：通过；真实 Room 6→8 迁移链与新 schema 对照。
- `tools/regression/network021/run.sh`：通过离线 fixtures；覆盖旧配置迁移、供应商切换/重启/清除隔离，通用与 DeepSeek 请求体参数，SSE 混合和空白增量，128 emoji 查询限制，搜索成功/空结果/失败与来源 URL 校验。
- APK 在无 API 凭据条件下未做在线请求验证；因此未声称真实搜索服务已连通。只读启动了 DshPhone30 模拟器，但 ADB 未连接，尝试连接失败；没有安装 APK，也未验证设备视觉、软/硬键盘和真实 IME 切换。

## 编辑器映射限制

零宽替代符与原语法字符一一对应 UTF-16 单元，因此文本长度及 identity 映射保持一致；隐藏标记占据同一水平坐标，触摸定位到多个相邻隐藏标记时无法区分其具体原始边界。编辑焦点行会显示原始语法，减少该情况；实际设备上的触摸/IME 边界仍需后续验证。非活动代码块保留原样以保障编辑安全。
