# v0.4.0 修复版本公开到 GitHub

日期：2026-11-07；状态：完成（公开源码）；负责人：本聊天 Codex，单一集成人。

用户明确要求上传 GitHub，并选择“先上传 v0.4.0，再开发登录”与 MIT。允许公开源码、本地提交、建立项目远端并推送；不公开真实数据、凭据、回退归档或替换正式签名；不创建 APK Release 或宣传 ChatGPT 登录已完成。

起始 main / 708a7f0，无远端；保留已有 v0.3/v0.4 及工程文档未提交修改。前副本 `.engineering/backups/github-publication-before/`。现有 GitHub CLI 在允许网络后已确认账号 maho37523 可用；沙箱内初次 auth status 的 token 无效提示不作为真实登录结论。拟公开 `maho37523/bugu-notes`，查询未发现同名仓库，不覆盖其他仓库。

出版前审查：项目候选 209 文件、历史 440 个对象的已知服务令牌/私钥特征扫描未发现匹配；历史 APK 内 dex/xml/json/txt 同类扫描未发现匹配。该扫描有规则覆盖限制，不宣称穷尽。所有单文件小于 100 MB。`.engineering/` 已被 Git 排除，包含手机数据库、日志、私有备份和新候选 APK 均不暂存或推送；本机配置、Keystore、数据库和凭据路径受既有排除规则保护。

变更：MIT 原创源码许可、当前版本 README（历史正文保留）、研究/需求更新和发布任务证据。当前修复源码的完整验证见 [故障任务](T-20261107-v040-startup-crash.md)；最终检查执行 quick、diff/check、已暂存路径与内容扫描。只推送明确枚举且审查的项目文件，保留原本地两次提交，不伪造原历史。

尚未推送。下一步：检查最终输入、创建本地提交、建立空公共仓库，添加 origin 并推送 main，再核对远端 HEAD/可见性/许可证。Git 身份仅使用现有身份或单命令显式身份，不修改全局配置。失败时保留本地提交和原工作内容，不 force push 或删除仓库。推送完成后记录 URL、提交和结果。

## 本地提交与上传进展

本地提交 `5f6846d09cdfabaeb1f4e45cebf6c9e192c14f15` 已建立，MIT、当前源码和原有修改均保留；前两次保护提交不改写。新提交使用账号的 GitHub noreply 邮箱，不修改全局身份，也不公开全局配置中的个人邮箱。公开仓库已创建为 `https://github.com/maho37523/bugu-notes`，origin 为其 HTTPS 地址。

首次推送被本机失效代理 127.0.0.1:10808 阻断；临时禁用代理后可连接，但 HTTP/2 大包传输高 CPU。停止本次未完成传输，重试仅命令级 HTTP/1.1 与 postBuffer，未改全局配置、未 force push、未删原历史。首次打包约 55.30 MiB，包含保留的历史 APK；目前等待远端确认。上传完成后更新本节，不提前宣称成功。

## 上传结果

公开仓库 [maho37523/bugu-notes](https://github.com/maho37523/bugu-notes) 已创建并推送成功；远端 main 与本地 `5f6846d09cdfabaeb1f4e45cebf6c9e192c14f15` 一致，默认 main、public=true、license=MIT 已由 GitHub API 核对。首包传输较慢但最终成功，不再存在上传阻塞。未创建 APK Release，当前公开版本不包含 ChatGPT 订阅登录，后续在独立分支开发。

quick 通过（57 文档、416 链接、25 资产）；工作和暂存 diff/check 通过。原历史提交作者 Codex/local.invalid 保留，新提交采用账号 noreply 地址；凭据特征扫描未发现匹配，真实手机备份不在任一公开提交中。停止本任务写入，无推送进程遗留；仅后续交接文档小提交同步 main。回退不自动删除公共仓库或重写历史，需用户另行决定。
