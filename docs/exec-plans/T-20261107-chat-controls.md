# 聊天框统一模型与思考选择

2026-11-07；状态：待验收（完整离线及真机启动通过）；负责人：本聊天 Codex，单一写入/集成。

用户真机登录后报告“可以使用模型了”，并明确要求聊天框不显示账号邮箱、所有连接均在聊天框选择模型及思考强度，反馈回调停留127.0.0.1产生困惑。本任务按该明确产品要求继续实施（feature），不改登录客户端身份、数据库、备份格式或依赖；真实账号与 Key 不读取。起始开发分支 codex/chatgpt-subscription-login / 8806c36，保留前次真机安装证据的两份未提交文档；保护副本 `.engineering/backups/chat-controls-before/`。

验收：聊天框只显示连接来源/模型/思考选项，不显示邮箱；已配置的连接可在框内显式切换（不会自动切换计费）；ChatGPT 按账号目录显示模型，API 连接提供既有模型及手填模型入口；思考强度以目录/官方支持范围为准，不支持/未知显示模型默认及说明，选择确实写入对应协议；不同连接/模型偏好独立持久化，旧 Key 和授权保持，发送中禁止切换；回调页面明确本机授权交接和返回应用，不能提前宣称 token 校验完成。保留订阅来源和用量管理入口。

最小步骤：共享能力与持久化规则，聊天框统一选择面板及请求快照，Responses/Chat Completions/Claude 参数按官方协议映射，回调说明；补参数/切换/旧配置回归，最终隔离 baseline 和合成 AVD UI；生成0.5.1候选。回退只撤销本任务增量，不触及用户数据；真机新安装须先准备当前包/数据库/附件回退（不复制凭据），不自动清理或恢复。

来源：OpenAI 官方 [模型目录](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)、[预览限制](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations)、[reasoning](https://developers.openai.com/api/docs/guides/reasoning)、[公开目录元数据类型](https://github.com/openai/codex/blob/main/codex-rs/protocol/src/openai_models.rs)；[DeepSeek](https://api-docs.deepseek.com/guides/thinking_mode/)、[Claude effort](https://platform.claude.com/docs/en/build-with-claude/effort)、[Qwen兼容协议](https://help.aliyun.com/en/model-studio/qwen-api-via-openai-chat-completions)。目录元数据缺失时不能将未知档位显示为已支持。

首轮固定副本构建通过，network 中新可选目录元数据触发 JVM JSON 适配器缺失数组返回非 null 包装的差异（实际 Android 为 null）；修正 fixture 以匹配 Android 语义，未放宽业务断言。首轮验证期间另补 Qwen 精确支持范围、聊天内目录刷新和合成 UI 夹具，统一入口检测输入变化并标为 blocked，报告143541不作为交付证明。

第二轮固定输入 `.engineering/reports/20261107-143754-271192/summary.json` 全部通过；Android17 合成 UI 实际显示动态模型和目录强度，聊天框无邮箱。随后复核长模型名可能挤占发送区域，约束模型区域权重；设置只管理连接凭据，保存时恢复目标模型已有强度，避免覆盖聊天框选择。以上最后两项将重新固定输入验证；第二轮报告不覆盖该增补。

第三轮 `.engineering/reports/20261107-144043-004277/summary.json` 完整通过，随后收尾复核补充：发送中禁用选择入口并在保存处二次拦截；设置切换 ChatGPT 注册账号时按目标账号读取模型强度，防止原账号档位覆盖目标账号。补账号 override 正反断言。以下最终基线覆盖全部收尾，不再在验证期间编辑输入。合成 UI 脚本最初检查 FilterChip selected 而非 Android checked，实际界面选中正确，修正 host 脚本属性判断，不修改生产选中语义。

## 最终证据与交接

最终固定输入完整 baseline `.engineering/reports/20261107-144255-770347/summary.json` 全部通过，隔离目录 `bugu-baseline-a9236mrf`，JDK21/现有离线依赖，主APK与独立测试APK、Lint、自检及六项回归（含扩展网络/订阅）同源。74 tasks / 56秒；Lint 0 errors / 5 warnings，与v0.5.0最终输入相同（4项既有、1项保留官方图标长路径）。最终源码以 inputs.json 文件哈希为准。

Android15合成AVD新装/旧schema8悬空草稿升级/三次冷启动通过：`chat-controls-startup35-final/`；实际loopback/平台RS256/真实加密存储：`chat-controls-callback35-final/`。Android17合成AVD从前一候选覆盖最终包后，动态目录模型及强度切换、API/订阅显式切换、选择重启保持和各连接偏好隔离、邮箱隐藏通过：`chat-controls-ui17-final/`；未发送任何合成凭据网络请求。首次host UI断言误用selected，修正为平台FilterChip checked语义后通过，保留生产断言。回调HTML在离线真实socket测试中检查了返回说明且不包含code/state。

最终APK `.engineering/apk/Bugu-Notes-v0.5.1-chat-controls-debug.apk`，29,402,779字节，SHA256 `4f83e9983cc9dd2d556507ff1494967d9db38360dc5492c57cef04b7ee928e46`；应用身份、升级签名、schema9、业务备份格式不变，0.5.1/17，未新增依赖。旧注册记录无reasoning字段时兼容读取；新增元数据可由原读者忽略。

沿用用户已批准的本功能备份/安装/在线验收范围，先核对真机当前0.5.0包和签名，再私有备份数据库/附件并校验成功后覆盖0.5.1。备份 `.engineering/private/phone-before-chat-controls-v051-20261107-144608/`，不复制登录或API偏好、不读取密钥；不清数据、不恢复替换。安装后实际哈希一致、进程存活、MainActivity前台、无新增本应用崩溃：`phone-chat-controls/device-result.json`。用户既有登录/配置文件原位保留，未代发在线消息或读取真实笔记内容。

待用户验收手机上的模型与强度交互，以及各实际模型服务端响应；此前用户“可以使用模型了”只作为0.5.0真实登录可用反馈，不冒充代理执行全部厂商在线回归。未知/不支持可调强度的模型只显示模型默认，不发未经支持的参数。主分支仍v0.4.0，候选保留草稿PR，不发布正式Release。回退仅本开发分支增量；手机回退需按私有归档另核对，不能自动还原覆盖数据。

补充最终证据：Android17 同源回调/验签/加密存储通过 `chat-controls-callback17-final/`；最终UI长自定义模型ID仍保留完整可见发送按钮。quick 60份文档/431条链接/25项保护资产通过，diff空白与候选增量敏感内容检查通过；所有 android/tools 修改与最终固定副本逐文件哈希一致。待同步原草稿PR后结束写入，构建已结束，两台合成AVD将停止。

交接完成：实现提交 `a3f9d9a` 已同步 [草稿PR #1](https://github.com/maho37523/bugu-notes/pull/1)，仍为draft/open，main未合并。两台合成AVD均已停止，构建与回归已退出，手机保留0.5.1前台。此次文档同步后本聊天停止写入；下一动作是用户验收输入框底部选择面板，遇到具体模型错误时按安全错误文字定位，不读取密钥或回调URL。
