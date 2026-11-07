# T-20261106：项目级 Skills

日期：2026-11-06。状态：完成（安装与离线验证）。负责人 / 集成人：本会话 Codex，串行写入后释放范围。

## 目标、授权与范围

用户要求使用 skill-creator，按 intake、feature、bugfix、review 顺序建立简短、可复用、触发互斥的项目级 Skills，验证并报告可用性；本阶段不修改产品功能。统一版本保存与交接要求继续由 AGENTS.md 及其引用规范管理。

验收：四个 Skill 位于项目发现路径，结构校验通过；路由、确认与实施授权分开，未开启产品开发时不因调用 Skill 自动放行；需求与检查报告可保存在现有任务体系；不重复制定版本和交接规则；已有业务、依赖、架构及资产不变。

允许：`.agents/skills/{intake,feature,bugfix,review}/SKILL.md` 与各自 `agents/openai.yaml`、本任务及任务索引；为使上述文件可被保护和检查，最小修改 `.gitignore`、`tools/check.py` 及其合成自检。不新增依赖，不自动提交、切换分支或推送。排除全部产品代码、数据及资源。

起始基线：main / `708a7f0`；`git status --short` 为空，暂存和未暂存差异均为空。既有任务记录已释放相关写入范围，无本次重叠任务登记；任务记录不作为锁。拟改已有文件前副本位于 `.engineering/skill-setup-before/`。系统进程列表查询被沙箱阻止，未将其视作无其他会话的证据；只认领上述工程流程路径，不接管业务任务。

## 实施计划与风险

1. 复用现有任务格式和质量矩阵，依次创建与校验四个 Skill。
2. 项目扫描路径采用 `.agents/skills`；只放行这四个目录中的指定说明/元数据，不放行其他 `.agents` 内容。增加边界负例并执行完整 baseline。
3. 检查触发示例、相对链接、最终差异和保护文件，记录验证与交接。

风险：将需求确认误当实施授权；将 review 变成自动修复；将 feature 的相关测试误路由至 bugfix；放宽私密路径检查。分别通过技能边界、修复范围约束、精确文件白名单和负例验证处理。当前 AGENTS 的工程基础阶段继续有效。本次不需要产品风险操作批准。

依据：已读取 `/Users/cris/.codex/skills/.system/skill-creator/SKILL.md`；另使用 OpenAI Docs 核对项目安装位置与默认自动发现，依据 [OpenAI 官方 Skills 文档](https://developers.openai.com/codex/skills/)，本次已检索并读取。未修改用户级 Skills 或全局设置。

## 验证与交接

开工：项目根 `/Users/cris/Documents/bugu-notes`，Python 3.14.7，`python3 tools/check.py quick` 退出 0；36 份文档、305 个本地链接、25 项设计校验通过。

### 实际交付与逐项可用性

四个 Skill 的说明、元数据生成、结构校验和安装均按下表顺序完成。每个目录只含 `SKILL.md` 和 `agents/openai.yaml`；不新增脚本、模板或重复的通用规范。统一规则通过相对链接引用根 AGENTS。

| Skill | 项目入口 | 触发与交付 | 可用性 |
| --- | --- | --- | --- |
| intake | [SKILL.md](../../.agents/skills/intake/SKILL.md) | 自然语言新需求整理 → 待确认任务；不实施 | 已安装，结构与元数据校验通过 |
| feature | [SKILL.md](../../.agents/skills/feature/SKILL.md) | 实施已确认的新能力 → 功能与相关测试 | 已安装，校验通过；当前业务开发仍未开启 |
| bugfix | [SKILL.md](../../.agents/skills/bugfix/SKILL.md) | 恢复既有约定 → 复现、根因、最小修复与回归 | 已安装，校验通过；业务修复仍受当前阶段约束 |
| review | [SKILL.md](../../.agents/skills/review/SKILL.md) | 独立检查或只诊断不修复 → 缺陷、测试与风险报告 | 已安装，结构与元数据校验通过 |

保留默认自动选择，可显式使用 `$intake`、`$feature`、`$bugfix`、`$review`。本次未新建 Codex 会话实测菜单或模型自动选用；“可用”指位于官方发现目录且文件校验通过，不声称真实开发任务的端到端验证。

静态路由走查（人工核对描述和流程，不冒充模型实测）：

| 示例请求 | 唯一主流程及边界 |
| --- | --- |
| “想做标签合并，先整理需求” | intake；不能自行确认或开发 |
| “按已确认任务实现标签合并并测试” | feature；先核对实施授权，当前阶段不自动放行 |
| “修复项目切换后草稿串用” | bugfix；恢复既有隔离约定，先核对修复授权 |
| “只定位草稿串用根因并报告，不改代码” | review；不自动修复 |
| “审查这个新功能是否符合验收条件” | review；独立查证，不沿用作者通过结论 |
| “继续实现已确认功能并修正本功能测试失败” | feature；相关测试不另触发 bugfix |
| “先整理新能力需求，再等我确认” | intake；保存确认缺口，到此结束 |
| “用 feature 开发尚未确认的新想法” | feature 入口检查不满足；仅按 intake 整理缺口，无业务编辑 |

实际变更共 13 个文件：八个 Skill 文件；`.gitignore` 精确放行，保留其他及嵌套 `.agents` 忽略；`tools/check.py` 仅允许四个目录中的两个指定文件名进入检查；`tools/test_engineering_checks.py` 补充允许文件、私密/未知路径和符号链接负例；本任务与索引。AGENTS、产品规范、架构及通用交接规则未改。

### 实际验证证据

- Skill Creator 的 `scripts/generate_openai_yaml.py` 与 `scripts/quick_validate.py`：四个 Skill 依次执行成功，安装后再次逐个校验，退出 0。默认 Python 缺 PyYAML；使用已有 `/usr/bin/python3` 3.9.6 及其已有 PyYAML，无安装依赖。单独核对元数据字符串长度、默认自动选择、八个文件未被 Git 忽略、其他 `.agents` 文件仍忽略。
- `python3 -m unittest discover -s tools -p test_engineering_checks.py`：退出 0，11 项通过，包括白名单、私密路径及符号链接边界。
- 第一次 `python3 tools/check.py baseline`：因沙箱禁止 Gradle 缓存锁写入而失败，六项后续回归未执行，报告 `.engineering/reports/20261106-223307-772601/summary.json`，不能算通过。
- 获准写已有缓存锁后第二次 baseline：自检、构建/Lint、六项回归通过，但检查期间 `.gitignore` 补齐嵌套忽略规则，入口正确拒绝输入变化，退出 1。报告 `.engineering/reports/20261106-223330-330956/summary.json`，整轮不能算通过。
- 冻结实现文件后第三次 `python3 tools/check.py baseline`：退出 0，整轮通过。报告 `.engineering/reports/20261106-223438-501076/summary.json`；同目录 `inputs.json` 包含八个 Skill 文件及执行文件 SHA-256。起始提交 `708a7f0eb4c95fa4dcf2582bc9e234a71fca0faa`，未提交内容以该清单为准。JDK 21、SDK 35 / Build Tools 35.0.1、Gradle 8.9 及已有离线缓存；全新隔离副本构建/Lint、migration、lifecycle、editor、network、zip、legacy_migration 全部实际通过，无跳过。
- 用 Git blob 哈希核对 `android/`、`prototype/`、`design/` 的 103 个已跟踪文件：全部与 HEAD 相同，包括生产源码、配置、品牌资产和历史 APK。版本范围与最终差异已检查，无业务改动。
- 报告摘要回填后重新运行 quick 和 `git diff --check`，核对文档链接、状态与空白；最终结果见下方收尾记录。完整基线后仅修改本任务和索引的结果说明，不改实现输入。

覆盖限制：未启动或安装真实应用，未操作真实数据、迁移真实数据库或调用在线 API；未做设备体验、全历史迁移或新会话 Skill 行为实测。不把文件有效性或本机离线回归扩大为这些结果。

### 收尾与接手

收尾记录：2026-11-06，项目根执行 `python3 tools/check.py quick` 退出 0，41 份文档、314 个本地链接、25 项设计校验通过；`git diff --check`、`git diff --cached --check` 均退出 0。技能文件已纳入 quick 的文档、空白、路径与链接检查。

起止基线均为 main / `708a7f0`，本次文件保持未提交、未暂存；没有自动提交、分支切换或推送。版本与交接仍由 AGENTS 和 workflow 管理。用户无待决定事项；今后实施新功能/业务修复时依本次实际授权与阶段检查，不以本任务安装记录代替授权。

回退：仅逐项撤销本任务文件差异（上述 13 个文件），保护后续修改；不重置或清理目录。前副本位于 `.engineering/skill-setup-before/`；临时报告、副本可重建，关键结果已在本任务保留。

下一动作：后续会话按 AGENTS 开工，从四个 Skill 选择符合实际意图的入口；无产品开发自动待执行任务。检查结束后本会话停止写入并释放上述范围，本会话未启动应用或服务；本会话构建/检查进程均已退出。全系统其他会话进程状态未确认。
