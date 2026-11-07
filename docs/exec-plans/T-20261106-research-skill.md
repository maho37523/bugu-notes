# T-20261106：research 项目 Skill

日期：2026-11-06。状态：完成（安装与离线验证）。负责人 / 集成人：本会话 Codex，串行写入后释放范围。

## 目标与授权

用户要求使用 skill-creator 增加 research：技术可行性调查、官方资料核实、方案比较、风险评估及可供后续会话读取的报告；优先官方和最新资料，区分事实、推测、待确认；发现不可行、授权限制或重大风险先说明，不修改业务代码或擅自开发。本次只创建该流程，不开展多厂商接入等具体产品研究。

验收：安装在 `.agents/skills/research/`，结构与元数据校验通过；与需求整理、实现、故障修复和代码审查边界清晰；报告路径与证据要求明确；通用版本与交接继续引用 AGENTS；新增文件进入版本和检查范围，其他私密路径限制不变。

允许：research 的 `SKILL.md`、`agents/openai.yaml`；`.gitignore`、`tools/check.py` 和自检中的精确名单追加；本任务、任务索引和 workflow 的 Skill 导航追加。排除：业务代码、依赖、架构、真实数据、其他四个 Skill 内容及已有需求文档。不自动提交、切换分支、推送或发布。

起始：main / `708a7f0`。已有未提交的四个项目 Skill、工具名单及自检、任务索引、workflow、项目 Skills 任务和多厂商接入需求；暂存差异为空。已逐项核对相关任务及 workflow 交接，均已停止写入；本会话仅追加本任务范围，保留已有差异。前副本与 SHA-256 清单位于 `.engineering/research-skill-before/`，不作为长期备份。

## 实施与验证计划

1. 创建简短 research 流程，明确官方来源、日期/版本适用性、结论置信边界、报告与停止条件。
2. 仅将 research 的两个文件加入原白名单，补充相应正例及私密路径负例；追加导航，不改变四个既有 Skill。
3. Skill Creator 结构校验、元数据/发现路径检查、自检、完整 baseline、最终文档及保护文件检查。

主要风险：把最新版本当作本工程现有能力；将未找到官方支持误判为明确禁止；研究结论冒充验证或授权；调查扩展为开发。通过报告证据与适用范围、三类结论、授权停止条件防止。无产品风险操作需要本次批准。

开工检查：项目根 `/Users/cris/Documents/bugu-notes` 执行 `python3 tools/check.py quick`，退出 0，42 份文档、325 个本地链接、25 项设计校验通过。

## 交付与可用性

- 已安装 [research/SKILL.md](../../.agents/skills/research/SKILL.md) 及 `agents/openai.yaml`，保留默认自动选择，也可显式使用 `$research`。不增加不必要的参考文件、脚本或报告模板副本。
- 研究报告保存在 `docs/exec-plans/T-YYYYMMDD-主题-research.md` 或已有专门研究任务，关联需求并登记索引；包含基线、研究问题、来源日期/版本、事实/推测/待确认、比较、建议、风险、阻塞及后续查证动作。
- 对资料冲突、无法访问及未找到官方支持，要求如实标待确认；最新能力须核对项目当前版本。不可行、授权限制或重大风险及时向用户说明并入报告，停止依赖该条件的操作；不自动实施或将建议写成已采纳决定。
- 本任务共涉及八个文件：两个新 Skill 文件、本任务、任务索引、workflow 导航、`.gitignore`、`tools/check.py`、`tools/test_engineering_checks.py`。工具名单仅追加 research；其余私密路径与符号链接限制保留。

静态路由走查（不是模型自动选择实测）：

| 请求 | 主流程与预期边界 |
| --- | --- |
| “调查某厂商是否允许第三方使用订阅额度，比较 API 路径” | research；核实官方来源，保存报告，不登录或开发 |
| “先整理多厂商接入需求和验收条件” | intake；不混为技术选型调查 |
| “检查现有客户端代码是否正确处理 SSE” | review；已有代码符合性与缺陷检查 |
| “按确认任务完成新厂商适配” / “修复现有流式错误” | feature / bugfix；仍须核对实施授权与阶段 |
| “找不到官方授权说明就先做登录原型” | research 记录授权待确认，说明风险并停止该实施，不以缺证据断言可行或被禁止 |

## 实际验证与限制

执行日期 2026-11-06，主目录 `/Users/cris/Documents/bugu-notes`，main / `708a7f0`；Python 3.14.7。基线含未提交工作文件，以报告输入清单为准。

- `/usr/bin/python3` 3.9.6 及已有 PyYAML 执行 Skill Creator 的 `scripts/generate_openai_yaml.py` 与 `scripts/quick_validate.py`，安装后结构校验退出 0；单独核对元数据长度、默认自动选择和 Git 忽略边界通过。未安装依赖或修改全局设置。
- `python3 -m unittest discover -s tools -p test_engineering_checks.py`：退出 0，11 项通过；research 两个指定文件允许，凭据、数据库、额外元数据、未知技能及其他受保护路径继续拒绝。
- `python3 tools/check.py baseline`：获准使用既有 Gradle 缓存锁后在全新隔离副本执行，退出 0；quick、自检、构建/Lint、migration、lifecycle、editor、network、zip、legacy_migration 全部通过，无跳过。报告 `.engineering/reports/20261106-234535-782381/summary.json` 与 `inputs.json`；已确认两个 research 文件纳入输入哈希。环境为既有 JDK 21、SDK 35 / Build Tools 35.0.1、Gradle 8.9 和离线缓存。
- 与开工前副本比较：四个既有 Skill 的八个文件、原项目 Skills 任务及多厂商需求记录内容完全不变。对 `android/`、`prototype/`、`design/` 的 103 个已跟踪文件核对 Git blob 哈希，均与 HEAD 一致。
- 完整基线后仅回填本任务及索引的结果；收尾 `python3 tools/check.py quick` 退出 0（44 份文档、329 个本地链接、25 项设计校验），`git diff --check`、`git diff --cached --check` 均退出 0，相对链接、状态和最终增量检查通过。不将完整 Git 差异中的前次修改归为本任务成果。

可用性指发现目录、文件和元数据有效；未新建会话实测菜单或模型自动选择，未执行具体厂商研究。不将安装校验当作研究结论，未验证设备、在线 API 或真实历史数据场景。

回退：只审查并撤销本任务新增内容和名单/导航增量，保留开工前与后续修改，不重置或清理目录。

交接：起止基线仍为 main / `708a7f0`，保留既有与本次未提交/未暂存修改，未进行版本提交、分支切换或推送。下一会话按 AGENTS 重新核对工作区；有技术调查需求时可使用 research，报告不替代需求确认或实施授权。用户目前无待决定事项。本会话检查进程已退出，未启动应用或服务，未开展具体产品研究或业务实施；收尾后停止写入并释放上述范围。
