# ChatGPT 订阅登录官方路径核查

日期：2026-11-07；状态：完成（文档研究，进入已授权后续开发）；负责人：本聊天 Codex，单一写入。

## 问题与授权

用户要求增加 ChatGPT 登录并使用订阅额度，声明项目开源，并要求上传 GitHub。随后明确选择 MIT，先上传修复后的 v0.4.0，再开发登录。基线 main / 708a7f0 及既有未提交 v0.3/v0.4 代码；保留所有前次修改。关联 [需求](T-20261107-openai-subscription-intake.md) 与 [GitHub 上传](T-20261107-github-publication.md)。本文件研究不运行 OAuth、不读取现有登录令牌或调用真实模型。

## 官方事实（当日实际读取）

| 来源 | 事实与边界 |
| --- | --- |
| [官方概览](https://developers.openai.com/siwc/token-sharing-open-source) | 存在开源、本地托管应用的可选套餐授权路径；付费或远程托管应用走另一意向入口。客户端授权和运行主机 ID 分开；stable host ID 在首次登录前持久化。不是只登录身份即可推理，也不授予 ChatGPT 历史对话访问。 |
| [注册与登录](https://developers.openai.com/siwc/token-sharing-open-source/sign-in) | 首次动态公共客户端注册，不需要客户端 secret/partner key；系统浏览器、PKCE S256、state/nonce、127.0.0.1 HTTP loopback；注册返回 issued client_id，后续复用，校验 ID token 签名/issuer/audience/expiry/nonce 和实际授权 scope。不能复用 Codex 或其他应用身份。 |
| [模型与推理](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference) | 用所选账号 access token 查询 /v1/models 的 models 数组，按 visibility/list 展示 display_name 并使用 slug；只向公共 /v1/responses 调用，store=false、stream=true，直到 response.completed 才算成功。 |
| [预览限制](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations) | 每次 HTTP 携带历史；显式 system message 不支持，使用 instructions/developer；多种采样/存储字段与工具受限，不使用 previous_response_id；不能盲复用当前 Chat Completions 请求体。 |
| [账号与会话](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions) | 注册/账号独立，刷新串行及原子保存，退出撤销 renewable session；token 不进 URL、日志、源码、系统/业务备份。Plus 多应用共享五小时总量，没有独立额外额度；Pro 不适用该五小时限制。 |
| [错误恢复](https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery) | scope 缺失则不推理；资格、地区/策略、限额、暂时不可用、失效令牌分别处理；不得静默切换计费来源。 |
| [UI 规范](https://developers.openai.com/siwc/ui-ux-guidelines) | Continue with ChatGPT 按钮、首次套餐授权确认、当前套餐来源标识、Manage usage 指向 ChatGPT 用量设置。 |

## 工程判断与未确认事项

已确认项目是本地 Android 应用，无服务端托管推理；用户选择公开 MIT 原创源码。这与官方 OSS/本地路径描述一致，可据指南实现公共客户端流程，不再将“没有官方订阅入口”作为阻塞。**开源意愿或公开仓库不保证每个账号/地区/workspace 的权限**，最终以授权 scopes、模型目录和服务返回判断。

Android 平台专项支持没有在本次读取页面得到保证；官方要求的 loopback、系统浏览器及公钥签名校验可由现有 Android/JVM 能力实现，这是工程推断，必须离线回调和生命周期测试，不改成私有 scheme 或 WebView 绕行。现有 EncryptedSharedPreferences 可延续 fail-closed 存储，不安装模型 SDK，不改 Room 或业务备份格式。

## 建议实施顺序与验收

GitHub 先发布 v0.4.0 修复源代码及 MIT。之后单独实施账号注册/PKCE/ID token 验证与安全存储、模型目录及 Responses SSE、设置和输入区来源标识、取消/错误/刷新/退出路径，补离线授权及流 fixture，完整隔离构建与回归，再运行 Android 15/17 合成检查。真实账号登录、在线调用或新版本设备安装须在具体可审查结果准备好后按项目边界获得授权；不借用本机 Codex 凭据。

必须保持已有 API Key、其他供应商、笔记/对话/项目/附件及 schema 9 兼容；订阅失败不自动回退 API 付费路线。套餐用量管理链接不冒充可查询精确余额。图像输入仅按模型能力开放，语音/视频/上传 API 不在首轮范围。依赖的账号真实资格、浏览器回调和在线模型能力尚未实际验证。

## 交接与回退

本研究仅文档核查，官方文档已实际读取；未做 OAuth 或在线推理。实施授权来自用户后续“先上传 v0.4.0，再开发登录”，不来自研究结论。研究回退只撤销本报告及其索引项，保留既有需求和其他修改。后续实现另建任务及开发分支，与已上传稳定源码分开。
