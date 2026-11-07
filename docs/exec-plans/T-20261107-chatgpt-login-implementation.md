# v0.5.0：ChatGPT 订阅登录实施

日期：2026-11-07；状态：待验收（离线与隔离设备通过）；负责人：本聊天 Codex，单一写入/集成。

## 确认与授权

用户要求增加 ChatGPT 登录、使用订阅额度，确认 MIT 开源，并明确“先上传 v0.4.0，再开发登录”。v0.4.0 已公开并远端核对；此后创建 `codex/chatgpt-subscription-login`，起始 `71d1afe`，工作区无其他写入会话。沿用 [已确认目标与验收](T-20261107-openai-subscription-intake.md)，官方协议依据 [专项研究](T-20261107-openai-subscription-research.md)。当前授权产品实现和隔离/合成验证；不读取本机 Codex 登录凭据、不自动登录真实账号或在线推理、不安装新版本到用户手机。

## 实施与验收映射

- 系统浏览器公共客户端动态注册，持久 host ID、一次性 state/nonce/PKCE、127.0.0.1 回调；ID token 使用官方发现的 JWKS / RS256 校验签名、iss、aud、sub、时间和 nonce。取消、超时、重放和不同身份回调均不能替换原账号。
- 新的订阅连接与 API Key 独立；每个注册记录绑定 client ID/账号，刷新串行、原子保存、Keystore 失败闭锁、退出尝试撤销并清除本地 token；系统备份/设备迁移排除新凭据。保留多账号映射与稳定 host ID，不复用其他应用客户端。
- 模型来自当前账号公开 models 目录；Responses 固定官方 HTTPS 主机，store=false/stream=true，每次携带历史，将 system 指令映射 developer，引用上下文保留为不可信资料；completed/failed/incomplete/截断严格区分。不静默切换到 API Key、其他账号、付费或搜索路径。首轮文本/笔记/文本附件，未验证的图片和其他能力明确拒绝。
- 设置提供 Continue with ChatGPT、账号/模型选择、退出、重新授权/刷新模型、首次权限说明；输入区标识订阅来源并链接用量管理。不展示推测额度或写死订阅模型列表。
- 不改 Room schema、迁移、数据库名、业务备份格式、应用 ID 或签名；已有供应商与所有笔记/项目/草稿语义保持。版本采用 0.5.0 / 16，仅候选开发。

## 文件范围与验证

data 内新增 OAuth/loopback、账号状态/安全存储、Responses 协议和客户端；AppContainer 手工组装，既有网络客户端抽出小接口，不引入后端/新 SDK/新依赖或架构模式。设置和输入区局部接入；新系统备份排除规则。旧凭据和数据库文件不变。

在现有 network021 基础上增加离线 fixture：实际 RSA 签名正反例、授权 URL/PKCE/state/单次回调、账号隔离、串行刷新/失效刷新/退出/安全存储失败、官方主机和模型限制、请求字段、SSE 成功/失败/截断、取消、token 不进日志/备份。新增回归入口纳入既有完整 baseline 后，跑全部构建/Lint/原有六类回归及专用 Android 15/17 启动和离线回调场景。在线登录/资格和模型调用仍未确认，另记待验收。

## 保护、回退与交接

起始源码保存于已公开 main / 71d1afe，文件哈希 `.engineering/backups/chatgpt-login-before/inputs.json`。仅本分支增量；回退不触及 main、手机修复包或私有备份。每次同步记录实际输入/失败/结果，不将合成 OAuth 作为真实授权成功。

下一步：用户批准后备份并安装候选，由用户完成真实账号授权与一条固定短消息验收。未启动真实 OAuth、在线推理或真实设备安装，未发布 v0.5.0 正式版。

## 首轮实施与验证

生产组件、设置/输入区接入、模型/账号与固定计费路由已实现，无新增依赖或数据库迁移。首轮固定副本 `/tmp/bugu-chatgpt-compile-*` 编译/Lint 通过（52 秒、48 tasks、0 errors/4 既有 warnings）；单独框架测试包编译通过。实际源码输入以最终 baseline 为准，首轮不是最终交付证据。

JVM RSA/回调/注册/刷新/退出/存储及 Responses fixture 已定向通过，既有 API/network 回归保持；11 项工程自检通过。第一次测试拷贝命令错误使用隔离目录作源路径，未执行测试，修正绝对源路径后执行通过。Android 17 首次实际回调/签名/加密存储运行输出 PASS，但包装器误以为默认输出含 INSTRUMENTATION_CODE 而失败；加 -r 后正确通过，证据 `.engineering/reports/chatgpt-android17-raw/`，此前 stream-only 结果保留于 `chatgpt-android17-first/`。

安全复核补充：取消后仍将已收到的旋转令牌以不可取消的原子提交保存，避免刷新成功但丢失替换令牌；退出先持久化不可用状态并取消该账号请求，再尝试远端撤销，最终清除本地令牌；其他账号/API 配置不变。Android 省略可选 ID-token URL hint，仍严格绑定原身份。欢迎说明仅在安全存储可用时显示，登录完成事件按版本号处理，避免旧事件切换用户当前选择。之后固定输入执行完整 baseline，再测两套 AVD 最终产物。

首轮完整固定输入 baseline `.engineering/reports/20261107-140239-674876/summary.json` 全部通过（主 APK、框架测试 APK、Lint、全部现有/新增回归）；74 构建任务、50 秒。后续界面检查发现：订阅授权被服务器撤销但本地 access token 尚未到期时，用户需要直接重新登录入口；补充已授权账号的“重新登录此账号”按钮，避免必须先退出才能重授权。该按钮不强制重新同意，不自动触发登录。为最终交付重新固定输入跑 baseline；此前报告只覆盖按钮增补前源码。

包含重新登录入口的完整基线 `.engineering/reports/20261107-140434-934433/summary.json` 已全通过。品牌核对随后发现官方登录文档直接提供白色 ChatGPT SVG（21×21 单路径）；保留原 SVG 并按同一路径转换为 Android vector，补按钮图标和第三方来源说明，不改布谷原标志。首次写 vector 时目标 drawable 目录不存在，创建目录后生成；这属于文件准备失败，尚未运行该输入构建。加入最终资产后再固定输入验证，之前基线不作为该图标输入的最终构建证明。

## 最终候选证据与交接

2026-11-07 最终固定输入 baseline `.engineering/reports/20261107-140717-157168/summary.json` 全部通过：主 APK、独立测试 APK、Lint、11 项工程自检及 migration/lifecycle/editor/network（含 ChatGPT）/ZIP/legacy 回归。隔离目录 `bugu-baseline-wjmd03tt`，JDK 21、现有离线依赖。Lint 0 errors / 5 warnings：4 项既有警告，新增 1 项官方 SVG 转换路径过长的 VectorPath 警告；保留官方原路径，无全局抑制，静态小图标在模拟器实际显示正常。

最终同一 APK 在 Android 15 / API35 与 Android 17 / API37 的合成 AVD 均通过首次启动、旧版 schema8 悬空临时草稿文件夹迁移及三次冷启动；笔记、附件、草稿与既有数据兼容回归通过。证据 `chatgpt-v050-startup35/`、`chatgpt-v050-startup17/`。两套 Android 均通过实际 loopback 回调、平台 RSA 验签和真实 EncryptedSharedPreferences 隔离测试，证据 `chatgpt-v050-callback35/`、`chatgpt-v050-callback17/`。上述均为合成账号/数据，不代表真实 OpenAI 服务验收。

最终 Android17 UI 已核对官方按钮、未登录禁用“使用此订阅连接”、用量入口以及返回既有 API Key 设置，证据 `chatgpt-v050-ui17/`。首次 UI 脚本误检查 TextView 子节点 enabled 属性，父按钮实际正确禁用；修正为检查父按钮后通过，不涉及生产源码修改。

交付 APK：`.engineering/apk/Bugu-Notes-v0.5.0-chatgpt-candidate-debug.apk`，29,370,011 字节，SHA256 `3e7cddc62a35fac904070f99ec5d9383228b4e6c5e2c964637f36a08ed12a7d7`。应用身份、升级签名与已修复 v0.4.0 相同，版本 0.5.0 / 16、minSdk26 / target35，数据库仍 schema9。真实手机保留已修复 v0.4.0，未触及真实账号、API Key、笔记或备份。

剩余：真实浏览器 OAuth、账号/工作区资格、服务端模型目录与订阅推理，以及用户体验验收均未确认；开源并不保证所有账号均可授权。需先获得本候选安装/在线验收授权。仅候选开发分支，不合并 main 或发布正式 Release；本分支回退至 71d1afe 不影响 main 与手机。模拟器将在交付前停止，检查/构建进程均已结束；本聊天结束后释放写入。
