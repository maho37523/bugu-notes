# T-20261106：多厂商 AI 接入技术研究

日期：2026-11-06。状态：完成（文档研究；产品选择待决定）。负责人 / 集成人：本会话 Codex，串行文档写入后释放范围。

关联需求：[多厂商 AI 接入方式](T-20261106-multi-ai-access.md)。本次用户明确要求调查该需求的技术实现；不确认最终需求，不授权实施。

允许及占用：本报告与任务索引新增研究行。排除：原需求、业务源码、依赖、架构、ADR、凭据、真实数据、登录、付费调用及功能原型。最小步骤：现状静态核对 → 官方文档与授权核查 → 方案比较及后续验证设计 → 文档检查和交接。

起始基线：main / `708a7f0`，暂存差异为空。已有 `.gitignore`、索引、workflow、检查工具及自检、`.agents/` 与三个 T 任务的未提交差异；相关任务均记录停止写入，本次保留。索引前副本与 97 项文件哈希保存在 `/tmp/bugu-multi-ai-research-before/`，仅作本轮差异保护，不保证长期保留。不提交、不切分支、不推送。

开工检查：项目根执行 `python3 tools/check.py quick`，退出 0：44 份文档、329 个本地链接、25 项资产。文档范围无需业务构建与回归。风险：订阅授权条件变化、官方页面口径冲突及移动端适用性；结论按证据分层，不以静态或文档证明在线成功。

回退仅删除本报告及索引新增行，保留此前和后续差异。报告中的接口边界、类型名称及实施顺序都是建议，不是已采纳架构、已确认验收或开发授权。

## 结论与研究问题

**API 多厂商适配在协议层可行；订阅必须逐路径判断。** 建议先保留现有 Chat Completions 路径并补充 Claude 原生 Messages 适配。ChatGPT 已有官方第三方套餐使用预览，值得独立评估，但布谷笔记的分发资格、Android 授权回调及安全存储尚未验证。Claude 不能直接照搬账号 OAuth；部分套餐另有可领取的 API 抵扣额度，可经普通 API Key 使用。

本报告回答：现有代码能复用什么；OpenAI 与 Claude 的请求、流式及上下文如何映射；Plus/Pro 等套餐是否有官方接入方式；凭据与计费如何隔离；后续应先验证什么。以 OpenAI、Anthropic 为主要候选，DeepSeek、百炼与自定义兼容服务作为保留边界，不将额外厂商或搜索、语音纳入需求。

研究采用三种证据：**官方声明**只证明文档承诺；**静态代码**只证明当前实现；**实际实验**必须单列。本次只有文档及保护检查实测，没有模型请求、OAuth 登录或 Android 原型实测。

## 工程基线与静态事实

当前为 Android 0.2.1 / versionCode 13，minSdk 26、target/compileSdk 35，Kotlin 2.0.21、Coroutines 1.9.0、Room 2.6.1 / schema 8；单模块、HttpURLConnection、手工 JSON/SSE，无官方模型 SDK、无独立后端。依据：[构建](../../android/app/build.gradle.kts)、[架构](../../ARCHITECTURE.md)、[当前状态](../current-state.md)。本次未改版本或依赖。

| 静态证据 | 已确认事实及实现影响 |
| --- | --- |
| [ApiConfig.kt](../../android/app/src/main/java/com/dspro/android/data/ApiConfig.kt)：`endpoint`、`load/save/clear` | 固定追加 `/chat/completions`；每个 provider 一套 key/base/model；OpenAI 默认 `gpt-4o-mini`。这不是当前账号可用模型证明；仅按厂商的存储不足以表达同厂商的 API 与订阅两套认证。 |
| 同文件：`isConfigured`、`prefs` | 仅判断 API Key 非空；加密失败会回退普通偏好。不能把 access token 填入 apiKey 后宣称完成登录；新增长期令牌前须处理安全存储失败路径。 |
| [ApiProtocol.kt](../../android/app/src/main/java/com/dspro/android/data/ApiProtocol.kt)：`buildChatRequestBody/parseChatSseChunk` | `messages`、`stream:true`、图片 data URL；只有 DeepSeek 发送思考参数。解析 `choices[0].delta`，坏 JSON 返回空列表；不能直接解析 Messages 或 Responses。 |
| [DeepSeekClient.kt](../../android/app/src/main/java/com/dspro/android/data/DeepSeekClient.kt)：`stream` | Bearer 认证；读单行 `data:`、识别 `[DONE]`，退出读流后发送 Done；有取消断连。新协议须避免把流内失败、截断或未知事件吞掉后报告成功。 |
| [DshApp.kt](../../android/app/src/main/java/com/dspro/android/ui/DshApp.kt)：`launchApiStream`、`chatHistory`、`snapshotContextTurns` | 聊天请求固定创建 DeepSeekClient；请求启动时已复制配置；记忆、引用笔记和附件文本注入 system，历史引用可位于会话中段。上下文映射不能无声改变顺序或权限。 |
| [ApiSettingsScreen.kt](../../android/app/src/main/java/com/dspro/android/ui/ApiSettingsScreen.kt) | 设置页连接测试也直接调用 DeepSeekClient；必须与实际聊天共用后续路由，避免“测试成功、聊天走另一条协议”。 |
| [Manifest](../../android/app/src/main/AndroidManifest.xml)、[ADR-0001](../decisions/0001-backup-excludes-api-config.md) | allowBackup=true；自有业务备份排除连接配置，但系统备份排除与恢复后 Keystore 行为未确认。不能把前者当作所有令牌均不被备份的保证。 |
| [现有网络回归](../../tools/regression/network021/ProtocolTest.kt) | 覆盖配置迁移/隔离、Chat Completions 请求与 delta、搜索 fixture；没有 Claude/Responses/OAuth 或真实 HTTP 中断闭环。此处是阅读结论，本次未重跑业务回归。 |

## 官方证据与版本适用性

以下链接均于 **2026-11-06 实际打开或通过官方文档工具读取**，不是只依据搜索摘要。未标固定版本的网页按当日滚动版本理解；账号资格以厂商返回及实际协议为准。表中 O/A 编号供后文定位，不是 API 名称。

| 编号 / 一手来源 | 核查版本、适用范围与关键证据 |
| --- | --- |
| O1 [OpenAI Responses 迁移指南](https://developers.openai.com/api/docs/guides/migrate-to-responses)；[Chat Completions 创建](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create) | API v1 滚动文档；Chat Completions 继续支持，新项目推荐 Responses。当前项目只实现前者；推荐不构成全量迁移理由。 |
| O2 [ChatGPT plan usage 概览](https://developers.openai.com/siwc/token-sharing-open-source)；[商业接入入口](https://developers.openai.com/siwc/request-client-id) | 官方预览：开源、本地托管应用可申请用户授权使用套餐；付费或远程托管应用需走意向申请。普通身份登录不自动获得推理授权。本项目是否满足该路径资格未确认。 |
| O3 [注册与登录](https://developers.openai.com/siwc/token-sharing-open-source/sign-in) | 公开客户端动态注册、PKCE S256、state/nonce、固定 host ID；回调要求 `127.0.0.1` HTTP loopback。无 Android 专项落地证据，不能改用自定义 scheme 并假定支持。 |
| O4 [模型与推理](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference) | 套餐授权使用公开 `/v1/responses`，按账号查询模型；HTTP 请求 `store:false`、`stream:true`，必须等到 `response.completed`。不使用网页内部 backend-api。 |
| O5 [预览限制](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations) | 套餐路径不是普通 API Key 的全部能力：HTTP 每次携带历史；显式 system 消息项受限；多种参数和工具不支持。不能复用普通 Responses 的全部默认值。 |
| O6 [账号与会话](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions)；[UI 规范](https://developers.openai.com/siwc/ui-ux-guidelines) | 注册/账号分离、串行刷新、注销撤销及用量管理入口。Plus 的五小时限额跨应用共享；Pro 不适用该五小时限额，不等于无限用量。 |
| O7 [订阅者 API credits](https://developers.openai.com/api/docs/guides/chatgpt-api-credits) | 当日 Pro/Business 符合条件者可把订阅福利关联到 API organization，使用该组织 project key；Plus 不在资格内。抵扣额度与 ChatGPT/Codex 限额独立；余额耗尽可能继续消耗已购额度。 |
| O8 [API 认证](https://developers.openai.com/api/reference/overview#authentication)；[Codex App Server](https://learn.chatgpt.com/docs/app-server) | 普通 API Key 是秘密，官方要求避免暴露在客户端。App Server 是 Codex 的集成接口，不能作为当前 Android 已含运行时或普通聊天必需依赖的证据。 |
| A1 [Claude 创建 Message](https://platform.claude.com/docs/en/api/messages/create)；[认证](https://platform.claude.com/docs/en/manage-claude/authentication) | 直接 API：Messages、`max_tokens`、模型、上下文与可选 system；当前推荐 Bearer key，旧 x-api-key 仍可用；未限定工作区的 key 需 workspace header。App Attest 官方范围为 iOS/macOS，不能推到 Android。 |
| A2 [流式 Messages](https://platform.claude.com/docs/en/build-with-claude/streaming) | 按内容块 index/type 更新，message_stop 终结；HTTP 成功后仍可能有流内 error，另有 ping/未知事件。 |
| A3 [OpenAI SDK 兼容层](https://platform.claude.com/docs/en/cli-sdks-libraries/libraries/openai-sdk) | 官方提供兼容评估路径；stream 支持，system/developer 会合并提升到顶层；思考返回及若干能力受限。兼容层存在不证明本项目手写客户端已经通过。 |
| A4 [Claude Code 授权和凭据规则](https://code.claude.com/docs/en/legal-and-compliance)；[Agent SDK 概览](https://code.claude.com/docs/en/agent-sdk/overview) | 第三方自行提供 Claude.ai 登录/代用订阅凭据受限；原样运行 Claude Code 有另外的条件，不能等同于任意 Android 聊天 OAuth。SDK 文档也要求未获先行批准者走 API。 |
| A5 [Agent SDK 与订阅帮助页](https://support.claude.com/en/articles/15036540-use-the-claude-agent-sdk-with-your-claude-plan) | 页面含 2026-06-15 暂停调整、2026-10-07 更新，提到 SDK、`claude -p` 及第三方使用的订阅计量；与 A4 的产品授权范围需分开理解，不能用计量说明推翻授权限制。 |
| A6 [月度 API credits](https://support.claude.com/en/articles/17154008-monthly-api-credits-for-max-ultra-and-team-plans) | 当日 Max/Ultra/Team 可领取，Pro 不适用；关联 Console organization 后经 API Key 使用，与聊天订阅限额独立。按账期过期、先用福利再用购买额度；有余额/自动充值时会继续消耗。 |
| A7 [2026-10-21 迁移说明](https://platform.claude.com/docs/en/api/migration-2026-10-21)；[发布说明](https://platform.claude.com/docs/en/release-notes/overview) | 10 月 21 日发布新 API 版本，SDK Java 3.0.0 起默认使用；新版本移除 temperature/top_p/top_k 等参数。不能沿用记忆中的 `2023-06-01` 并称为最新。 |
| A8 [Anthropic Commercial Terms](https://www.anthropic.com/legal/commercial-terms) | 页面生效日期 2025-06-17；API 可用于面向用户的产品，但仍受使用政策、地区和服务专项规则约束。API 许可不替代订阅登录授权。 |
| P1 [DeepSeek Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)；[百炼 OpenAI compatible](https://www.alibabacloud.com/help/en/model-studio/compatibility-of-openai-with-dashscope) | DeepSeek 当日 API 文档；百炼页面更新 2026-10-13。现有协议有继续保留依据；百炼区域/工作区端点需按账号复核，不能据新示例自动改写已保存地址。 |
| P2 [OpenAI 支持地区](https://developers.openai.com/api/docs/supported-countries)；[Claude 支持地区](https://platform.claude.com/docs/en/api/supported-regions) | 发布前按实际用户与服务地区核对；当前未核对任何真实账号、资格或可达性，不作全球/中国大陆可用承诺。 |

### 已确认的官方事实：订阅存在三类不同路径

1. **ChatGPT 套餐直接授权**：O2—O6 是实际官方入口，不应再泛称“没有官方订阅接入”。但它是有范围和限制的预览；本项目还没有证明满足资格或 Android 上可完成回调。Plus 文档有共享限额说明，不意味着所有 Plus/Pro 账号均已获开放。
2. **订阅福利转换为 API credits**：O7、A6 给出可行的普通 API 路径。用户在厂商官方页面领取、绑定组织，再配置对应 API Key，应用无需获取订阅密码或 OAuth。它使用 API 抵扣预算，不直接消耗网页聊天额度；不能保证每次请求“只扣福利、不扣已购余额”，也不能仅从 key 推断额度来源。
3. **Claude 原生应用/Agent 订阅会话**：A4 明确限制第三方自行登录；A5 又说明部分 SDK 使用仍计入套餐。已确认有文档口径边界，尚未确认布谷笔记可用的例外授权。需要厂商书面澄清具体集成形态，不能选择最宽松一句就开发登录功能。

发现上述资格及授权限制后，本次仅继续资料核查；没有运行登录、提取本机 auth 文件、请求额度端点或试用内部接口。原样 Claude Code 的宿主方案与独立 Android 本地聊天是不同运行形态；即使另获授权，也会引入本需求排除的主机/桥接服务问题。

### 推测与建议：工程可行性

- 现有 Kotlin/HTTP 能分别承载三种公开协议，预计不必先换栈、拆模块或改 Room。依据是已有网络/Flow 链路及 O1/A1；没有原型实验证明工作量或设备稳定性。
- 在本地用户自带 key 的范围内，复用现有 HTTP 比同时加入两家 SDK 更容易控制变更；相应要自行维护 SSE、错误及取消。若改为公开发行且由开发者付费，内置共享 key 不成立，服务端方案又超出当前需求边界，应重新决定范围。
- OpenAI 套餐路径技术上可以独立于 Codex 运行时直接接 Responses；Android loopback、后台存活和令牌存储是主要未知，不能把桌面文档示例当作移动端通过。

### 待确认事项与查证办法

| 待确认 | 查证方式 / 恢复条件 |
| --- | --- |
| 布谷笔记是否开源、如何分发/收费，是否符合 O2 | 查清代码及品牌授权与分发计划；本轮文件检索未发现适用的项目软件 LICENSE，不能把本地 Git 仓库等同开源；必要时由用户决定后申请官方接入。 |
| Android 是否允许并可靠完成官方 loopback 流程 | 先向官方确认移动端适用范围；获准后在隔离测试应用用测试账号验证系统浏览器、127.0.0.1 回调、端口占用、进程重建；不自行换回调类型。 |
| Claude 的 SDK/原样二进制例外是否适用本产品 | 携带明确数据流、运行位置及凭据归属向 Anthropic 澄清 A4/A5，留存答复；此前不提供产品内 Claude 订阅登录。 |
| 首发厂商、模型、地区与套餐 | 用户决定范围；再按账号模型目录、版本和地区政策查证，不根据示例模型推定可用。 |
| BYOK 发布安全边界、系统备份、凭据加密失败 | 先做 Android 存储/备份方案审查；另获准后在合成测试环境验证 Keystore 故障和恢复，不读取真实偏好。 |
| API key 究竟扣福利还是付费余额 | 由用户在厂商 Console 核对组织预算/充值配置；应用只标 API 计费及可能额外费用，不声称可自行精确识别。 |

## 候选方案比较

| 方案 | 需求覆盖 | 兼容与维护成本 | 授权/计费、数据风险 | 研究建议 |
| --- | --- | --- | --- | --- |
| 沿用当前 Chat Completions | 保留现有厂商；OpenAI 基础聊天已有代码 | 最低；不覆盖原生 Claude 或套餐协议 | API 预算；BYOK 本机存储风险既有 | 保留作兼容基线，不能当作全部需求完成 |
| OpenAI API + Claude 原生 Messages，复用 HTTP | 覆盖两家 API、文本与经模型验证的图片 | 中等；两个请求/流事件映射，保留原配置迁移 | 无需消费订阅 OAuth；需按厂商保护 key、错误和数据发送边界 | 推荐 API 主路线 |
| Claude 官方 OpenAI 兼容层 | 基础聊天可能较快，思考等能力受限 | 接入较低，但上下文提升/静默忽略字段会增加验收成本 | 仍是 Claude API 计费，不能使用 Claude 网页订阅令牌 | 可做对照候选，不默认替代原生适配 |
| 两家官方 Java SDK | 协议类型、流与错误封装更丰富 | 新依赖及传递依赖、包体、R8、Android 兼容需另验；项目目前未用 | SDK 开源许可不授予服务额度或第三方订阅登录权 | 本轮不引入；复杂能力增加时另评估 |
| ChatGPT 官方套餐授权 + Responses | 条件满足时覆盖登录后套餐使用 | 较高；OAuth 生命周期、账号隔离、预览变化和移动回调 | 仅适用资格内应用/账号；刷新令牌和额度切换风险高 | 独立资格核查与隔离验证后再决定 |
| 套餐领取 API credits，再用 API Key | O7/A6 对应套餐可减少额外 API 支出 | 应用侧仍用 API 适配，领取/绑定发生在官网 | 非所有套餐；API 预算可能继续扣付费余额 | 推荐作为可明确说明的条件路径 |
| 自建代理 / 桌面 Codex 或 Claude Code 桥接 | 可能承载官方运行时 | 引入后台或主机、配对、额外数据及密钥边界 | 不消除官方授权条件，扩大维护与数据面 | 当前排除，不以此绕过资格限制 |

SDK 核查：官方 [openai-java 仓库](https://github.com/openai/openai-java) 当日 README 示例 4.101.0；[anthropic-sdk-java README](https://raw.githubusercontent.com/anthropics/anthropic-sdk-java/main/README.md) 示例 3.4.0。两者声明 Java 8+ 不等于 Android minSdk 26 已验证。当前 [OpenAI LICENSE](https://raw.githubusercontent.com/openai/openai-java/main/LICENSE) 为 Apache 2.0；[Anthropic LICENSE](https://raw.githubusercontent.com/anthropics/anthropic-sdk-java/main/LICENSE) 亦为 Apache 2.0，旧版本/既有部分贡献仍有 MIT 历史（README/A7）。未安装、锁定版本或检查全部传递许可；若采用须固定实际版本并保留所需许可/NOTICE，不能据 main 文档保证将来版本。

## 建议的最小实现边界（仅供后续开发）

### 1. 区分供应商、协议、认证与计费

在现有 data 包内增加小范围路由，不预设新的架构框架。连接记录至少区分 `provider`、`protocol`（Chat Completions / Responses / Messages）、认证类型、账号/注册标识、模型与能力、地址及可选 workspace。API Key 和 OAuth 凭据用不同记录；套餐的 API credits 仍归 API Key 路径，预算提示不能伪装成额度保证。

保留已有 `${provider}_api_key/base_url/model` 和 DeepSeek legacy 迁移；升级时映射为旧 API 连接，不能覆盖原 key。`reasoning_effort`、`web_search_enabled` 当前是全局偏好，后续按厂商能力决定显隐/映射，不能把 DeepSeek 的值盲传给所有模型。先设计不改变 Room/schema 8/业务备份的偏好扩展，仍须验证进程重启和回退读取；如果确需记录历史响应的模型/费用来源，单独评估迁移，不能顺手改备份。

统一在发送前固定 connection/credential/model 快照，并让设置页测试、发送、重试走相同工厂。切换连接不改写正在请求的凭据，不自动失败回退到别的 key/付费路径。重试使用原连接还是当前选择仍需产品决定并在界面显示。

### 2. 按协议构造请求和消费终止事件

| 协议 | 最小请求映射 | 流处理与兼容要点 |
| --- | --- | --- |
| 既有 Chat Completions | 原 messages/image_url；保留 DeepSeek 专有配置 | choices delta、结束/错误/取消；维持旧离线 fixture，不以新路径替换所有厂商 |
| OpenAI Responses API Key | input items、instructions、模型允许的图片/思考选项；建议无服务端会话依赖 | typed SSE；区分 completed、failed、incomplete、截断，保留本地聊天历史；与套餐路径使用不同能力限制 |
| OpenAI Responses 套餐 | O4/O5：账号模型目录、store:false、stream:true、每次携带历史；system 映射到允许的 instructions/developer 结构 | 正文 delta 不能当最终成功；额度不足可在流中失败；不能发送预览禁用参数、依赖 previous_response_id 或把 token 发到自定义 Base URL |
| Claude Messages | `/v1/messages`、固定 `anthropic-version:2026-10-21`、Bearer key、必要 workspace、max_tokens、模型及 content blocks | 依 index/type 汇总文本；处理 stop_reason、message_stop、流内 error；未知事件前向兼容但不能吞掉已知失败。图片需转换成该版本接受的 image/source 结构，不能沿用 image_url JSON |

表中原生 Claude 的上下文/图片与参数以 A1/A7 为准，流语义以 A2 为准；OpenAI 普通 API 以 O1 为准，套餐额外约束以 O4/O5 为准。先完成文本及现有附件等价映射，不附带增加工具调用、搜索、通用上传或后台任务。

现有 `StreamEvent.Content/Reasoning/Failure/Done` 可作为 UI 映射起点，但必须有**一次请求仅一个终态**的约定；已 Failure 后不能再 Done 清除错误。EOF/断连应保留部分正文并标未完成。输出上限、拒答、供应商过载、余额耗尽与普通限流分别呈现；取消保持当前“保存已到达内容”的生命周期。POST 已发送后不自动盲重试，避免重复回答/计费。

### 3. 先解决上下文语义差异

当前历史引用不是纯 user/assistant：DshApp 会在每条相关历史用户消息前插入 system 资料。Claude 兼容层会把它们提升到顶层（A3），OpenAI 套餐又限制显式 system item（O5），所以“只换地址或 JSON 字段名”不够。

建议在请求准备中区分项目指令/记忆、每轮引用资料、用户正文、历史回答及图片，保留顺序和资料边界，再逐协议映射。Claude 原生是否使用受模型支持的中途 system 形式，或把逐轮资料连同用户消息发送，应先按首发模型文档和合成 fixture 比对；不能把所有历史资料随意拼到最前面。此处涉及请求语义，需后续需求/方案确认；本研究未改变现有 prompt。

### 4. 套餐授权与凭据安全是独立工作包

OpenAI 仅在 O2 资格成立时评估 O3：安装实例保存稳定 host ID；每次授权新建 state/nonce/PKCE；系统浏览器完成官方授权；核对返回的注册 ID、ID token 签名/issuer/audience/过期/nonce 和实际 granted scopes 后才切换活动账号。官方要求的 loopback 不能擅自替换为 WebView、私有 scheme 或复用 Codex 客户端身份。

依据 O6，账号/注册的 client ID 与 tokens 成套隔离；刷新同会话串行并原子替换；注销停止请求、尝试撤销后清本地 tokens，未确认远端撤销则如实提示。Android 加密和备份应满足相同安全目标，不照抄桌面文件权限例子。不为保持可用性而让新令牌进入当前明文回退；应给出可恢复的失败状态。该安全策略须另行实施授权和回归，不能借研究修改现有 key 行为。

官方 API 路径也要遵守 O8：不打包开发者共享 key；用户自带 key 的本机使用仍有泄露边界，不等同服务端保密。限制凭据目的地与重定向，OAuth 固定官方 host；自定义地址只能使用用户专门选择的自定义凭据。日志/诊断不得含 key、token、授权 code、带 ID token hint 的 URL 或完整笔记；保持自有备份排除凭据并单独验证系统备份。

## 风险、阻塞与后续顺序

| 优先事项 | 后续动作及需要的证据 | 本次状态 |
| --- | --- | --- |
| P0：厂商授权和分发资格 | 确认开源/商业边界、目标地区；Claude 例外须厂商确认；不满足时只提供已授权 API | 官方资料已核查；产品和账号资格未确认 |
| P0：凭据存储与额外计费 | 审查加密失败/系统备份/目标 host；显示 API 或套餐来源，禁止静默跨预算回退 | 静态风险已记录；安全方案与设备验证未执行 |
| P1：协议/上下文差异 | 先确定首发模型及指令、引用映射，再做离线契约 fixture | 可执行方案已列；未写原型或测试代码 |
| P1：流内失败与取消 | 多行 SSE、拆帧、未知事件、终态、部分输出保存、取消断连、重试不串连接 | 当前回归覆盖不足，未来需扩充 |
| P1：模型与账号能力 | 分认证路径缓存模型/能力；模型无权访问、图片不支持、输出截断明确反馈 | 未进行在线账号验证 |
| P2：升级与备份兼容 | 旧 DeepSeek/百炼/custom 配置保存/重启/清除测试；无需迁移则维持 Room；有迁移单独评估 | 本次源码不变，未执行新的业务回归 |

建议后续按以下次序拆分，均须用户开启产品开发并确认范围：

1. 确定首发厂商/模型、使用地区、BYOK 分发方式、是否追求 ChatGPT 官方套餐预览；把 API credits 作为独立说明，确认是否满足用户意图。
2. 准备凭据存储与备份、连接标识、请求上下文的可审查设计；明确不支持的路径及理由。完成前不收集或迁移真实凭据。
3. 获准后先实现 API 文本与既有图片/引用的协议适配，扩充 network021 合成断言；按质量矩阵在全新隔离副本构建/Lint，重跑 network021 和 lifecycle019。
4. 套餐资格明确后另建 Android OAuth 最小验证任务，明确可使用的测试账号、环境、费用及数据，测试取消、拒绝授权、过期刷新、刷新竞争、撤销、浏览器返回及进程重建。
5. 在线验证单独授权：分别对厂商/认证路径做短文本、历史上下文、合成图片、停止/重试及错误场景；记录端点、API 版本、账号套餐类型、实际扣费侧与限制，不记录凭据。不得用离线通过替代在线或设备验收。

API 路径没有发现必须换栈的技术阻塞；ChatGPT 套餐路径仍有应用资格及 Android 验证前置；Claude 产品内订阅登录在未获明确适用授权前不进入开发。厂商条款及预览能力随时可能变化，实施开工应重新打开上述关键资料，不把本报告视为永久有效保证。

## 实际验证与交接

本次仅新增本报告及索引研究行，原需求保持原文，未更新产品状态、架构或 ADR。源码基线仍为 main / `708a7f0` 加开工前未提交差异；没有新提交、暂存或分支操作。

2026-11-06，在项目根 `/Users/cris/Documents/bugu-notes` 执行 `python3 tools/check.py quick`：退出 0，45 份文档、342 个本地链接、25 项资产检查通过；`git diff --check` 和 `git diff --cached --check` 均退出 0。额外核对本报告换行/行尾空白、相对链接、状态与版本；索引只比开工前增加本任务一行。开工前 97 项文件哈希中仅索引发生本任务允许的变化，其余 96 项（含原需求、既有未提交工作文件、项目 Skills、Android 源码、Web 和设计文件）未变。Android/原型/设计的 Git 差异为空；不将前次 Skill/检查工具修改记为本任务成果。

未执行 baseline、产品构建/业务回归、OAuth、在线推理或设备实验；原因是仅文档研究且无相应试验授权，不是已通过。未启动真实应用、数据库迁移、后台服务或模型运行进程。

下一动作：由用户决定首发厂商/模型、分发与地区范围，以及是否继续核查 ChatGPT 套餐预览；Codex 再按当时授权准备具体任务。原需求仍是待决定草案，本报告不替它确认需求。现有笔记、聊天、配置、数据库和附件行为没有修改。检查进程已退出，本会话交接后停止写入；其他会话进程状态未确认。
