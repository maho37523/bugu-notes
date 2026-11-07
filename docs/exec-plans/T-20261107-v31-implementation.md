# v0.3.0 实施：开工与协议前置

日期：2026-11-07；状态：待验收（离线实现及候选构建通过；设备及在线未验收）；负责人 / 集成人：本会话 Codex。

## 授权、范围和基线

用户要求“根据版本开发文档开始新版本开发”，本会话据此采用 [v0.3.0 规划](T-20261107-v0.3.0-plan.md) 的推荐范围及兼容取舍，并开启其中的产品开发：OpenAI 与 Claude BYOK API、保留旧厂商，暂缓订阅登录，含规划内安全收口。用户另确认目标为两家各自支持的所有地区；各厂商的地区清单不一定相同，不代表所有地区已经实测。设备安装、真实凭据、付费在线测试、发布或真实数据操作未因此获准。

起始 `main / 708a7f0`；未暂存差异包括 `.gitignore`、任务索引、workflow、检查工具及自检，另有 `.agents/` 和既有需求、研究、规划文档未跟踪；暂存为空。上述差异保留。既有任务记录已声明停止写入，Codex 桌面最近聊天中同目录其他聊天显示空闲；此信息不能证明其他工具或未来会话不会写入。索引和规划的前副本位于 `/tmp/bugu-v31-start/`，临时副本不保证长期保存。

本次占用任务文件和索引；后续业务实施按计划串行认领配置、网络、设置及聊天链，保留数据库 schema、迁移、业务 ZIP、应用身份和品牌原件。回退只审查撤销本任务增量，不覆盖已有或后续修改。不会提交、切换分支、推送或启动真实应用。

## V31-01：候选与未关闭事项

2026-11-07 核对官方文档；下表为技术候选，尚非账号可用或设备/在线通过的证明。

| 厂商 | 候选、能力和端点 | 待验证 |
| --- | --- | --- |
| OpenAI | `gpt-5.4-mini-2026-03-17`：官方列文本及图片输入、文本输出、Chat Completions 和流式；`https://api.openai.com/v1/chat/completions`。见 [模型说明](https://developers.openai.com/api/docs/models/gpt-5.4-mini) | 官方最大输出 128,000；Chat Completions 使用 `max_completion_tokens`（含推理 token）；本产品预算、终止契约、账号权限和实际单图待定。此前 `gpt-4.1-mini` 已被[标记弃用](https://developers.openai.com/api/docs/models/gpt-4.1-mini)，不选为长期基线。 |
| Anthropic | `claude-sonnet-5-5`：官方列文本及视觉、128K 最大输出；`https://api.anthropic.com/v1/messages`，版本 `2026-10-21`；API Key 用 Bearer，跨 workspace 的凭据需指定 workspace。见[模型](https://platform.claude.com/docs/en/models/sonnet-5-5/overview)、[认证](https://platform.claude.com/docs/en/manage-claude/authentication)、[版本](https://platform.claude.com/docs/en/api/versioning?version=2026-10-21) | 固定本产品的 `max_tokens`、实际单图、流事件和账号权限；版本移除 temperature、top_p、top_k，不能沿用旧链参数。 |

地区依据：[OpenAI API 支持地区](https://help.openai.com/en/articles/5347006-openai-api-supported-countries-and-territories)、[Claude API 支持地区入口](https://support.claude.com/en/articles/8116515-where-is-the-claude-api-supported)。实际在线地点未指定，须在测试前按当时清单确认；不得推断设备时区即为测试地点或厂商支持证明。

Claude [流文档](https://platform.claude.com/docs/en/build-with-claude/streaming)列出按 index 分块、message_delta、message_stop、ping 及流中 error；须将已知失败与真正成功区分。规划已选逐轮资料块和当前连接重试语义；实现前仍须核对 OpenAI 精确模型参数、各旧厂商 SSE 正常终止差异、预算和完整合成验证矩阵。不能把候选写为已验证能力白名单。

DeepSeek 官方 [Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/)明确流以 `[DONE]` 结尾，`length` 可能截断；百炼和 custom 仍需分别核对，不能凭此概括所有兼容服务。OpenAI 参数依据为 [Chat Completions 参考](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create)。

安全前置源码核对：`ApiConfigStore` 在加密初始化失败时使用 `ds_api_fallback`；Manifest 为 `allowBackup=true`。需要隔离合成测试证明安全收口及备份规则，不能据此推定真实设备上已有凭据的状态。

## 验证与交接

- 2026-11-07 在项目根运行 `python3 tools/check.py quick`，退出 0：47 份文档、355 个本地链接、25 项资产通过；这是开工状态，不是业务实现验收。
- 本轮仅开展静态核对与协议候选研究，尚未改业务代码、构建或运行产品回归，未启动应用/设备，未读取真实 Key 或用户数据，未发模型请求。
- 下一步完成 V31-01 的模型快照与参数、旧厂商终态和合成场景矩阵；关闭该门槛后按规划 V31-02 开始安全存储实施。在线测试之前另行落实支持地区、隔离设备、测试账号与费用预算；这些当前均未验证。

- 收尾执行 `python3 tools/check.py quick` 退出 0（48 份文档、357 个链接、25 项资产），`git diff --check` 与 `git diff --cached --check` 均退出 0。本会话停止写入；未启动构建或后台服务。

## 2026-11-07 独立审查

审查对象：本任务开工记录及任务索引中关联状态，基线 `main / 708a7f0` 和当时未提交工作区；未审查其他任务的全部内容或实现。用户要求只审查改动，本次不修改业务代码或修复发现。前副本 `/tmp/bugu-v31-review-before.md` 仅作临时保护。

1. **P2：开发授权状态在入口文档间冲突。** 本任务第 7 行及索引实施行记录已按版本规划开启开发，但 `AGENTS.md` 第 7 行与 `docs/workflow.md` 第 3 行仍以当前状态禁止新功能；规划文件第 3、160 行仍写范围待确认，索引第 15、17 行也写未授权实施。接手会话按要求先读这些文件时会得到互相矛盾的当前指示，可能再次停在规划或误判授权。建议保留历史记录，明确补充本次版本的有效授权、范围和剩余限制，并让索引指向新的状态依据；不将旧阶段叙述改写为历史上已经授权。
2. **P2：文件夹需求任务位于索引表之外。** `docs/exec-plans/README.md` 最后一行在任务格式代码块之后，而不是任务表内；渲染时不属于任务清单。接手者扫描任务表时可能漏掉已存在的待决定需求，重复建任务或忽略其数据兼容约束。建议把该条目移入现有任务表并保留状态及链接；该问题属于当前工作区既有未提交差异，并非本任务开工记录所创建。

验证：2026-11-07 项目根 `python3 tools/check.py quick` 退出 0（49 份文档、371 个本地链接、25 项资产）；`git diff --check`、`git diff --cached --check` 退出 0。快速检查无法判断授权语义或任务行是否在表内。未运行 Android 构建、回归、设备或在线测试；本次未验证全部未提交文件，未读取真实数据和凭据。审查记录追加后停止写入，未启动后台进程；回退仅移除本节并保留其他修改。

## 2026-11-07 审查问题修复

用户要求读取审查报告、修复、运行测试并更新 PR。开工 `main / 708a7f0`，工作区保留先前未提交差异；另一会话完成并停止写入后才修改共享目录，相关文档前副本在 `/tmp/bugu-review-fix-before/`。修复范围仅为授权状态与索引：在 AGENTS、workflow 和规划入口区分历史工程阶段与本次 v0.3.0 开发授权，索引的旧需求/研究行指向本实施任务；设备、在线、发布及真实数据限制不变。文件夹任务条目在修复前已由其他会话移入索引表，本次核实且保留。

验证：项目根 `python3 tools/check.py quick` 退出 0（50 份文档、391 个本地链接、25 项资产）；定向 Python 断言验证文件夹任务在表内且仅出现一次、授权入口指向本实施任务、研究索引不再误称未授权；`git diff --check` 和 `git diff --cached --check` 均退出 0。仅修改文档，未运行 Android 构建、设备或在线测试，未启动应用或服务；本会话结束后停止写入。回退仅撤销本节及本次四份文档的状态修正，保留其他会话修改。

PR 更新未执行：`git remote -v` 无输出，`gh pr status` 报告 `no git remotes found`，当前无法确定目标 PR；未提交、推送或配置远端。需提供现有 PR 链接或仓库信息以继续核对。

## 2026-11-07 未提交改动审查与集成核对

用户要求核查未提交代码并审查合并。起始 `main / 708a7f0`；仅有本地 main，`git remote -v` 无输出，无可识别的来源分支或 PR，暂存为空。工作区包含检查脚本及其测试、项目 Skill 和多份文档，未发现 Android 业务源码差异；保留全部现有改动。本次审查聚焦检查脚本白名单差异、配套自检及工作区/任务状态，不声称逐项审查所有未跟踪规划内容；未发现所查代码中需阻止集成的问题。

2026-11-07 执行 `python3 tools/check.py quick` 通过；`python3 -m unittest discover -s tools -p test_engineering_checks.py` 11 项通过；`git diff --check` 和 `git diff --cached --check` 通过。完整 `python3 tools/check.py baseline` 首次因沙箱不能写既有 Gradle 缓存锁失败，后获准使用该锁，在新隔离副本离线重跑退出 0：工具自检、构建/Lint、migration、lifecycle、editor、network、ZIP、legacy migration 全部通过，证据 `.engineering/reports/20261107-003607-476863/summary.json` 和 `inputs.json`。这些检查使用合成数据，不证明设备、在线或真实历史数据通过。

未执行合并或提交：没有来源分支、远端或 PR；工作区内容已经在 main 的未提交工作树中，不能称为已合并。下一步由用户提供待合并分支/PR，或明确是否需要将现有改动整理为本地提交；提交前重新核对完整范围及并行写入。审查记录前副本 `/tmp/bugu-uncommitted-review-before.md`；本会话结束停止写入，构建进程已退出，未启动应用。回退仅撤销本节，保留其他差异。

## 2026-11-07 正式业务开发

用户再次明确要求“开始业务代码正式开发”。已确认同目录另一会话结束；起始 `main / 708a7f0`，保留所有既有未提交差异。先完成 V31-01 协议前置并推进 V31-02 配置存储，允许修改 API 配置与协议、设置调用点和相关合成回归；不修改数据库、迁移、业务备份格式、身份或品牌。源文件和本任务前副本在 `/tmp/bugu-v31-business-before/`；仅作临时保护。按任务顺序先实施、再以隔离副本进行构建、Lint 与网络/生命周期回归，未完成安全收口前不声称支持真实 Key。

V31-01 初步契约：固定 OpenAI 候选 `gpt-5.4-mini-2026-03-17`、Claude 候选 `claude-sonnet-5-5`，各设测试输出预算 4096 token（含模型内部输出；正式体验须验收），Claude API 版本 `2026-10-21`，workspace 在凭据未绑定单一 workspace 时指定。两者均为官方文档支持图文输入的候选，账号权限及实际图片仍须在线验收。旧 OpenAI、DeepSeek 和百炼以有允许的 finish_reason 及 `[DONE]` 结束作为所选契约；[DeepSeek](https://api-docs.deepseek.com/api/create-chat-completion/) 和 [百炼](https://docs.modelstudio.console.alibabacloud.com/en/model-studio/stream) 文档分别明确流结束标记。custom 必须满足该契约，否则报不兼容。合成矩阵至少覆盖旧配置迁移/厂商隔离/Key 清除及不复活、安全存储初始化与解密故障、目的地/重定向、上下文顺序、图片限制、流分帧及取消/唯一终态；设备、账号及费用仍未落实。

本次业务代码起步：`ApiConfig.kt` 增加 Anthropic 独立 endpoint、默认模型和每厂商 workspace 字段；`ApiProtocol.kt` 配置保存规则保存该字段；`network021/ProtocolTest.kt` 用合成 Key 检查与 OpenAI 的隔离、清除 Anthropic Key 后另一厂商及 workspace 保留。设置页和网络路由未开放 Anthropic，现有 API 安全回退未修复；V31-02 仅部分完成，不能使用真实 Key 验收新版本或声称 Claude 可用。模型候选和测试预算是本地离线契约，在线权限、设备与地区仍未验证。

验证：2026-11-07 在全新隔离副本运行 `python3 tools/check.py baseline`，退出 0；构建/Lint、工具自检及 migration、lifecycle、editor、network、ZIP、legacy_migration 均通过；报告 `.engineering/reports/20261107-005018-704260/summary.json`，确切输入哈希见同目录 `inputs.json`。未启动应用、未进行设备或在线测试。后续继续 V31-02 的安全存储失败关闭、目的地和系统备份排除与合成回归，再按 V31-03—08 实施；不得把本段视为版本完成。回退仅撤销本次三处源码/测试增量与本节，保留所有已有和后续改动；本会话结束后停止写入，构建进程已退出。

## 2026-11-07 连续实施

用户要求连续完成已授权的离线开发和测试 APK，在线及真机条件未落实时继续其他可独立完成部分。保留既有所有未提交差异；检查同目录其他会话结束后，串行修改本版本文件，不提交或推送。

已实施的候选行为：独立 Anthropic 配置与 workspace；安全存储失败关闭并在设置、聊天和连接测试提示；按目的地确认自定义地址及禁止自动重定向；系统备份排除两个凭据偏好；OpenAI Chat Completions 与 Claude Messages 请求/流解码，使用官方候选模型的 4096 输出预算；引用按轮映射，保留项目指令和启用的项目记忆；发送/重试使用连接快照，显示发送目的地和费用提示；错误、截断、取消不视为成功，阻断未经确认的图片组合。设置页连接测试只发送合成短文本并等待完整终态。版本配置提升至 0.3.0 / 14，包名与 Room schema、业务 ZIP 格式未改。

新增合成验证包括实际配置存储类的 Keystore 初始化/读取/提交故障替身、厂商隔离与清除，以及模拟 HTTP 的请求映射、302 拒绝、流终态、部分输出、取消断连；系统备份规则静态断言。离线代码不能证明厂商账号权限、图片理解、地区可用、设备 Keystore/系统迁移或用户体验。阶段完成与最终构建证据以本节后续收尾记录为准，在线及真机未验收时不标记版本完成。

### 待决定：失败原因跨重启保存

代码核对发现 `ChatMessage.error` 仅为内存字段，schema 8 的 `chat_messages` 没有对应列，现有 `ChatRepository` 保存和读取均不含错误；原规划“沿用已有错误字段”不足以实现具体失败原因在进程重建后保留。当前候选对失败保存既有 `STOPPED` 状态和部分正文，并在本次进程内显示脱敏错误；重启后仍标示已停止，但具体错误原因丢失。该选择不改数据库或业务备份，不能声称完整满足具体错误持久化。

若要求跨重启保留具体原因，需要另获批准后设计候选：schema 8→9 为 `chat_messages` 新增可空脱敏错误列，旧记录默认为 null，Repository 往返映射；业务备份仍使用既有 ZIP 封装但数据库版本变为 9，须审查版本 6—8 备份导入、新版备份恢复和旧版的拒绝行为、附件及事务回滚，先用合成数据与隔离副本验证。旧版应用不能直接打开升级后的数据库，也可能无法恢复新版备份；回退需升级前业务备份和签名一致的旧安装包，并在隔离副本演练，真实备份恢复会替换业务数据，不在本次授权内。此方案只是可审查草案，尚未实现、测试或获准；不得对真实数据迁移、覆盖安装或恢复。

用户明确选择“接受限制，保持兼容”：失败的部分正文和 STOPPED 状态保留，具体错误原因仅在当前进程展示，重启后不保留。该项作为经用户接受的 v0.3.0 验收例外；不实施 schema 9 或改变业务备份格式。已添加实际 ChatRepository 的合成往返断言，确认部分正文及 STOPPED 保留，错误字段不会被误称已持久化。

### 离线候选交接与验收

- 最终代码在新隔离副本运行 `python3 tools/check.py baseline`，退出 0；构建/Lint、11 项工具自检、migration、lifecycle、editor、network、ZIP 和 legacy_migration 全部通过，无跳过。报告 `.engineering/reports/20261107-110433-244605/summary.json`，准确未提交输入哈希见同目录 `inputs.json`；核对其中 102 项 Android 构建及回归输入与工作区一致。之前一次新增存储测试因夹具的 Kotlin 模块可见性失败，修正为编译同源协议规则后，定向与最终基线通过，未删断言。
- Lint 为 0 errors / 4 warnings：既有 Sidebar Modifier 参数顺序 1 项、BouncyCastle 依赖信任管理器提示 3 项，与 M01 历史同类；依赖提示的运行时可达性仍未确认，不据此认定生产代码禁用 TLS 校验。
- 候选 APK：`.engineering/apk/Bugu-Notes-v0.3.0-debug.apk`，SHA-256 `cfd09e04a5287756238f9f605f1d79b6cefd750d2c44b8a5c25f00b5a00933d5`；`aapt` 核验 `com.dspro.android`、0.3.0 / versionCode 14、minSdk 26。`apksigner verify --print-certs` 通过；证书 SHA-256 `b55864e4aaf8cca0f1e27b259c43715d16fbf9b172e500fc540c61dbfad53917`，与仓库历史 v0.2.1 debug APK 一致，不代表已核验用户设备上的签名。
- 用户要求在线/真机不可执行时继续独立工作并生成测试 APK，因此本项交付是离线测试候选，并非规划中通过 V31-07 的正式版本验收。未安装或启动应用，未操作真实数据或凭据，未发真实模型请求；OpenAI/Claude 模型权限、地区、实际图片和语义、DeepSeek/百炼线上回归、设备 Keystore 与系统备份迁移、输入法和用户体验均未验证。V31-07/08 不标记完成；在安全存储与系统备份设备验证及相应授权前不使用真实 Key 联调。
- 未更改 Room schema、迁移、业务 ZIP 格式、applicationId 或品牌资产。系统备份规则只排除 API 凭据偏好，真实系统备份和设备迁移尚未实测；现有旧明文回退不会被自动删除。失败回答的具体错误跨重启不保留是用户明确接受的兼容例外，部分正文及 STOPPED 保留并有合成往返测试。
- 保留所有既有未提交内容；未提交、切分支、推送或发布。下一步：用户先在隔离设备验收并确认线上测试地区、账号和费用条件；设备及在线结果按 V31-07 逐项记录。若涉及真实安装覆盖、恢复或数据替换，先依据项目规则准备保护和回退。结束后本会话停止写入，构建进程已退出；临时隔离目录和 `.engineering` 不是长期备份。回退须逐项审查本任务源码/资源/测试增量及版本号，保留其他会话差异，不使用 reset/clean。
