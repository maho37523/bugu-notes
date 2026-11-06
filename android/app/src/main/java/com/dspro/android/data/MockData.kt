package com.dspro.android.data

object MockData {

    const val ARTICLE = """风格与实现

视觉是从 DeepSeek Harness 的 token 里取的：`#151517 / #1B1B1C` 的中性偏蓝灰阶、Montserrat 标题、细边框、16px 圆角、DeepSeek 蓝 `#5686FE`；桌面端保留 DSH 那种“左侧评审栏 + 设备预览”的工具感。

实现是原生 Android + Jetpack Compose，没有套用第三方 UI 模板。页面结构、状态机和 `docs/03` 里的 Compose 映射是一一对应的，后面接真实模型时不用重做产品逻辑。

边界说明

这一版先把交互与视觉落下来，模型输出、文件解析、任务执行仍然是模拟数据：

- AI 输出、文件解析、任务执行、分享/导出：全部是本地模拟
- 语音：状态与转写模拟，没有接入真实麦克风 / ASR / TTS
- 模式设置目前是全局的，正式版应按对话保存

文件

- 产品定义与交互逻辑
- MainActivity.kt · ChatScreen.kt · Theme.kt
- 03 实现方案（Compose / API 映射）"""

    const val AI_NEWS = """先说结论：今天最值得关注的是**模型、算力、开源**三条线。

**1. 模型：多模态成为默认项**
某厂商发布新一代多模态模型，重点不在参数，而在长上下文与工具调用。对普通用户的影响是：一次能传更多资料，回答更少“断片”。[1]

**2. 算力：推理集群继续扩张**
两家云厂商宣布扩建推理集群，价格战可能延续到明年。便宜的是标准档，贵的是“稳定 + 低延迟”。[2]

**3. 开源：Agent 框架开始拼可观测性**
一个开源 Agent 框架登上趋势榜，卖点不是“更聪明”，而是能看清每一步做了什么。[3]

> 如果你只关心一件事：API 价格短期不会涨，但“便宜”和“稳定”会进一步分化。

需要我把其中一条展开，或者整理成一份可分享的早报吗？"""

    const val EARPHONE = """先说结论：**通勤选 A，飞行选 B，预算优先选 C**。

**A：地铁低频降噪干净**
佩戴轻，适合每天一小时以内的通勤；缺点是风噪一般。

**B：飞机舱压感最轻**
降噪最强，但耳罩偏大，夏天戴会闷。

**C：适合偶尔用**
价格最低，别期待图书馆级安静。

如果你告诉我每天通勤多久、是否戴眼镜，我可以把范围缩到一款。"""

    const val FILE_ANSWER = """这份 18 页的报告，核心结论集中在第 3 页和第 12 页。

**一句话结论**：作者认为 2027 年之前，推理成本会继续下降，但“稳定供给”会成为新的分水岭。[1]

**三个关键判断**
1. 价格战不会停，但低价档的限流会更常见。（第 3 页）
2. 企业采购从“按 token”转向“按任务成功率”。（第 8 页）
3. 开源模型的主要战场从“跑分”转向“工具调用”。（第 12 页）

**作者没明说、但值得警惕的**：报告的数据主要来自三家云厂商，样本有偏向。"""

    const val IMAGE_ANSWER = """这是一张柱状图，横轴是 2024–2026 年，纵轴是推理请求占比，三组颜色分别对应文本、图片和语音。

**我看到的三个信号**
1. 文本请求仍占大头，但增速在放缓。
2. 图片请求的占比从 2025 年开始明显抬升。
3. 语音的绝对值小，波动大，样本可能不足。

**结论**：如果这张图来自你的业务数据，下一步应该先确认图片请求的增长来自哪个场景，而不是直接扩容量。"""

    const val GENERIC = """我把这个问题拆成三步来看。

**先给结论**：这件事的关键不在于“能不能做”，而在于“用什么代价做、做完能不能复用”。

**拆解**
1. 目标：先明确你要的结果是“一个答案”还是“一个可交付物”。
2. 约束：时间、资料、预算，哪个是硬约束。
3. 路径：先做最小验证，再决定要不要投入。

如果你愿意补充一点背景（给谁看、什么时候要），我可以直接给你一版可执行的方案。"""

    private const val REASONING = """用户的问题需要先对齐产品目标，再落到界面结构。
先给判断，再补实现细节；避免一上来就堆控件。"""

    val aiSources = listOf(
        Source(
            title = "多模态模型进入“长上下文”竞争",
            site = "36氪",
            date = "2026-11-03",
            snippet = "新一代多模态模型的卖点从参数规模转向长上下文与工具调用，一次可处理的资料量明显提升。",
        ),
        Source(
            title = "推理集群扩建，API 价格战延续",
            site = "晚点财经",
            date = "2026-11-03",
            snippet = "两家云厂商宣布扩建推理集群。标准档价格继续下探，低延迟与稳定供给成为新的溢价点。",
        ),
        Source(
            title = "开源 Agent 框架为何开始强调可观测性",
            site = "机器之心",
            date = "2026-11-02",
            snippet = "新登上趋势榜的 Agent 框架把“看清每一步”作为核心卖点，强调本地部署与调用链追踪。",
        ),
    )

    fun initialSessions(): List<Session> = listOf(
        Session(
            id = 1L,
            title = "调研 ChatGPT 移动端功能与交互",
            updated = "22 分钟前",
            messages = listOf(
                ChatMessage(
                    id = 101L,
                    role = Role.USER,
                    text = "按这个方向继续，把界面做出来。",
                    time = "22:12",
                ),
                ChatMessage(
                    id = 102L,
                    role = Role.ASSISTANT,
                    text = ARTICLE,
                    time = "22:13",
                ),
            ),
        ),
        Session(
            id = 2L,
            title = "AI 行业早报",
            updated = "今天 08:00",
            messages = listOf(
                ChatMessage(
                    id = 201L,
                    role = Role.USER,
                    text = "今天 AI 圈有什么大事？",
                    time = "08:00",
                ),
                ChatMessage(
                    id = 202L,
                    role = Role.ASSISTANT,
                    text = AI_NEWS,
                    time = "08:00",
                    reasoning = REASONING,
                    queries = listOf("2026年11月3日 AI 行业 新闻", "大模型 多模态 长上下文", "推理集群 API 价格"),
                    sources = aiSources,
                    memory = listOf("用户偏好先给结论，再给理由", "用户关注 AI 行业动态"),
                ),
            ),
        ),
        Session(
            id = 3L,
            title = "降噪耳机对比",
            updated = "昨天",
            messages = listOf(
                ChatMessage(
                    id = 301L,
                    role = Role.USER,
                    text = "帮我对比一下这三款降噪耳机的降噪和佩戴，通勤用。",
                    time = "昨天",
                ),
                ChatMessage(
                    id = 302L,
                    role = Role.ASSISTANT,
                    text = EARPHONE,
                    time = "昨天",
                    reasoning = "用户给了明确场景（通勤），需要可执行的结论，而不是参数堆砌。",
                    sources = aiSources.take(2),
                    memory = listOf("用户每天地铁通勤", "用户戴眼镜"),
                ),
            ),
        ),
        Session(
            id = 4L,
            title = "毕业论文",
            updated = "昨天",
            messages = listOf(
                ChatMessage(
                    id = 401L,
                    role = Role.USER,
                    text = "帮我把这段文献综述改得更学术一点。",
                    time = "昨天",
                ),
                ChatMessage(
                    id = 402L,
                    role = Role.ASSISTANT,
                    text = "可以。更学术的写法是先把“问题”立住，再引出“缺口”：\n\n> 近年来，多模态 Agent 的能力边界持续扩展，但对其可用性的评估仍缺乏统一框架。现有研究多聚焦于任务完成率，较少讨论工具调用链条的可观测性与失败归因，这构成了本文的研究缺口。\n\n如果你要投的是 CS 方向的会议，我建议把“可观测性”换成更具体的 operational transparency，并在下一句补一篇近两年的代表工作。",
                    time = "昨天",
                    memory = listOf("用户正在写多模态 Agent 方向的毕业论文"),
                ),
            ),
        ),
        Session(
            id = 5L,
            title = "未命名会话",
            updated = "1 个月前",
            messages = listOf(
                ChatMessage(
                    id = 501L,
                    role = Role.USER,
                    text = "先放一放，晚点继续。",
                    time = "1 个月前",
                ),
            ),
        ),
    )

    fun answerFor(text: String, attachment: String? = null): String = when {
        attachment != null && attachment.endsWith(".png", true) -> IMAGE_ANSWER
        attachment != null -> FILE_ANSWER
        text.contains("耳机") || text.contains("降噪") -> EARPHONE
        text.contains("AI") || text.contains("新闻") || text.contains("行业") -> AI_NEWS
        text.contains("界面") || text.contains("Compose") || text.contains("Harness") -> ARTICLE
        else -> GENERIC
    }

    fun sourcesFor(text: String, attachment: String? = null): List<Source> = when {
        attachment != null && attachment.endsWith(".png", true) -> emptyList()
        attachment != null -> listOf(
            Source("AI_infra_report_2026.pdf", "本地文件", "18 页", "第 3 页：推理成本与稳定供给；第 8 页：按任务成功率采购。")
        )
        text.contains("AI") || text.contains("新闻") || text.contains("行业") -> aiSources
        text.contains("耳机") || text.contains("降噪") -> aiSources.take(2)
        else -> emptyList()
    }

    fun reasoningFor(text: String, attachment: String? = null): String? = when {
        attachment != null -> "先定位附件里的结论页和关键数据，再用“一句话结论 + 三个判断”的方式组织。"
        text.contains("AI") || text.contains("新闻") -> REASONING
        text.contains("耳机") -> "用户给了明确场景（通勤），需要可执行的结论，而不是参数堆砌。"
        else -> null
    }

    fun initialTasks(): List<AutomationTask> = listOf(
        AutomationTask(1L, "AI 行业早报", "每天 08:00", "汇总过去 24 小时 AI 行业重要新闻，按重要性排序，附来源。", true, "今天 08:00 · 12 条"),
        AutomationTask(2L, "竞品价格监控", "每周一 09:30", "检查三家竞品的官网定价与套餐变化，输出差异表。", false, "10 月 27 日 · 无变化"),
        AutomationTask(3L, "周五周报提醒", "每周五 17:00", "提醒我整理本周进展，并给出一个三段式模板。", true, "10 月 30 日 · 已提醒"),
    )

    fun initialPlugins(): List<PluginInfo> = listOf(
        PluginInfo("files", "本地文件", "读取工作区里的 PDF、Markdown 与表格。", true, "file"),
        PluginInfo("web", "联网搜索", "在回答前检索最新网页，并给出来源。", true, "globe"),
        PluginInfo("calendar", "日历", "读取日程，辅助安排任务与提醒。", false, "calendar"),
        PluginInfo("github", "GitHub", "查看仓库、Issue 与 Pull Request。", false, "code"),
    )
}
