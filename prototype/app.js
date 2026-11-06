/* ============================================================
   DeepSeek Android prototype — vanilla JS, mock data
   ============================================================ */

const STORAGE_KEY = "dsproto.v1";
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const esc = (s = "") =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const nowTime = () =>
  new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
const uid = (p = "id") => `${p}_${Math.random().toString(36).slice(2, 9)}`;
const icon = (name, cls = "") =>
  `<svg class="icon ${cls}" aria-hidden="true"><use href="#i-${name}"/></svg>`;

/* ------------------------------ seed ------------------------------ */

const AI_SOURCES = [
  {
    id: 1,
    title: "多模态模型进入“长上下文”竞争",
    site: "36氪",
    domain: "36kr.com",
    date: "2026-11-03",
    snippet:
      "新一代多模态模型的卖点从参数规模转向长上下文与工具调用，一次可处理的资料量明显提升。",
    url: "https://36kr.com/",
  },
  {
    id: 2,
    title: "推理集群扩建，API 价格战延续",
    site: "晚点财经",
    domain: "latepost.com",
    date: "2026-11-03",
    snippet:
      "两家云厂商宣布扩建推理集群。标准档价格继续下探，低延迟与稳定供给成为新的溢价点。",
    url: "https://www.latepost.com/",
  },
  {
    id: 3,
    title: "开源 Agent 框架为何开始强调可观测性",
    site: "机器之心",
    domain: "jiqizhixin.com",
    date: "2026-11-02",
    snippet:
      "新登上趋势榜的 Agent 框架把“看清每一步”作为核心卖点，强调本地部署与调用链追踪。",
    url: "https://www.jiqizhixin.com/",
  },
];

const AI_REASONING = `用户问的是“今天 AI 圈有什么大事”，需要最新信息，先联网。
检索到模型发布、算力投资、开源框架三条高相关线索。
按“对普通用户的影响”排序，而不是按新闻热度排序。`;

const AI_NEWS = `先说结论：今天最值得关注的是**模型、算力、开源**三条线。

**1. 模型：多模态成为默认项**
某厂商发布新一代多模态模型，重点不在参数，而在长上下文与工具调用。对普通用户的影响是：一次能传更多资料，回答更少“断片”。[1]

**2. 算力：推理集群继续扩张**
两家云厂商宣布扩建推理集群，价格战可能延续到明年。便宜的是标准档，贵的是“稳定 + 低延迟”。[2]

**3. 开源：Agent 框架开始拼可观测性**
一个开源 Agent 框架登上趋势榜，卖点不是“更聪明”，而是能看清每一步做了什么。[3]

> 如果你只关心一件事：API 价格短期不会涨，但“便宜”和“稳定”会进一步分化。

需要我把其中一条展开，或者整理成一份可分享的早报吗？`;

const EARPHONE = `先说结论：**通勤选 A，飞行选 B，预算优先选 C**。

| | A | B | C |
|---|---|---|---|
| 降噪 | 强 | 最强 | 中 |
| 佩戴 | 轻 | 略重 | 最轻 |
| 价格 | ¥999 | ¥1,499 | ¥599 |

**A** 的优势是地铁低频降噪干净，缺点是风噪一般；**B** 的飞机舱压感最轻，但耳罩偏大；**C** 适合偶尔用，别期待图书馆级安静。

如果你告诉我每天通勤多久、是否戴眼镜，我可以把范围缩到一款。`;

const FILE_ANSWER = `这份 18 页的报告，核心结论集中在第 3 页和第 12 页。

**一句话结论**：作者认为 2027 年之前，推理成本会继续下降，但“稳定供给”会成为新的分水岭。[1]

**三个关键判断**
1. 价格战不会停，但低价档的限流会更常见。（第 3 页）
2. 企业采购从“按 token”转向“按任务成功率”。（第 8 页）
3. 开源模型的主要战场从“跑分”转向“工具调用”。（第 12 页）

**作者没明说、但值得警惕的**：报告的数据主要来自三家云厂商，样本有偏向。`;

const EMAIL_ANSWER = `可以，下面这版更得体，也保留了你的立场。

**主题：关于项目排期的调整说明**

> 各位好，
>
> 由于上游接口联调比预期多花了两天，原定本周五的提测需要顺延到下周二。为减少影响，我们已经把不依赖接口的页面提前完成，并准备了降级方案。
>
> 如果这个时间点会影响各位的后续安排，请在明天中午前告诉我，我们再一起调整。
>
> 谢谢理解。

需要更硬一点（对内部）或更客气一点（对客户），我可以再改一版。`;

const MCP_ANSWER = `MCP（Model Context Protocol）可以理解成 **AI 应用的“USB-C 接口”**。

它解决的是：模型如何以统一方式连接外部工具和数据，而不是每个工具都写一套私有对接。

**三个角色**
- Host：你用的 AI 应用（比如这个 App）
- Client：应用里的连接器
- Server：对外提供能力的工具（文件、数据库、搜索）

**为什么重要**：接一次，多个模型 / 应用都能用；权限、调用、结果格式有统一约定。

**一个例子**：你让 AI 查项目文档，它通过 MCP Server 读 Notion，而不是把整份文档复制进 prompt。`;

const IMAGE_ANSWER = `这是一张柱状图，横轴是 2024–2026 年，纵轴是推理请求占比，三组颜色分别对应文本、图片和语音。

**我看到的三个信号**
1. 文本请求仍占大头，但增速在放缓。
2. 图片请求的占比从 2025 年开始明显抬升。
3. 语音的绝对值小，波动大，样本可能不足。

**结论**：如果这张图来自你的业务数据，下一步应该先确认图片请求的增长来自哪个场景，而不是直接扩容量。`;

const DEMO_IMAGE = "assets/demo-chart.png";

const GENERIC_ANSWER = `我把这个问题拆成三步来看。

**先给结论**：这件事的关键不在于“能不能做”，而在于“用什么代价做、做完能不能复用”。

**拆解**
1. 目标：先明确你要的结果是“一个答案”还是“一个可交付物”。
2. 约束：时间、资料、预算，哪个是硬约束。
3. 路径：先做最小验证，再决定要不要投入。

如果你愿意补充一点背景（给谁看、什么时候要），我可以直接给你一版可执行的方案。`;

const MW_ANSWER = `结合项目里的资料，我的判断是：先别急着扩样本，先把“可观测性”这一个变量测干净。

**从资料里能确认的**
- 现有实验的评测集只有 120 条，跨领域泛化不足。（第 4 页）
- 三位标注者的一致性只有 0.61，结论的置信区间偏宽。（第 9 页）
- 作者自己也承认，长上下文带来的收益在短任务上不明显。（第 12 页）

**建议的下一步**：补 200 条跨领域样本，先复现主实验，再决定要不要换模型。需要我帮你列一个补实验的清单吗？`;

const WEEKLY_ANSWER = `可以，改得更像人话，同时保留信息量：

> 本周完成了接口联调的前 two 个阶段，登录与列表页已经可以走通。剩余的问题集中在异常态：弱网下的重试和空数据的兜底还没收口，预计下周二前完成。风险是上游接口的字段可能还会变，已和对方约定周四冻结。

需要我再压缩成三句话版本吗？`;

function seedState() {
  return {
    theme: "dark",
    chats: [
      {
        id: "c1",
        title: "AI 行业早报",
        projectId: null,
        meta: "今天 21:12 · 8 个来源",
        messages: [
          { id: "c1m1", role: "user", text: "今天 AI 圈有什么大事？", time: "21:12" },
          {
            id: "c1m2",
            role: "assistant",
            status: "done",
            time: "21:12",
            versions: [
              {
                text: AI_NEWS,
                reasoning: AI_REASONING,
                search: {
                  queries: ["2026年11月3日 AI 行业 新闻", "大模型 多模态 长上下文", "推理集群 API 价格"],
                  sources: AI_SOURCES,
                },
                memory: ["用户偏好先给结论，再给理由", "用户关注 AI 行业动态"],
                followups: ["展开第 2 条", "整理成可分享的早报"],
              },
            ],
            vIndex: 0,
          },
        ],
      },
      {
        id: "c2",
        title: "毕业论文",
        projectId: "p1",
        meta: "昨天 · 项目「毕业论文」",
        messages: [
          { id: "c2m1", role: "user", text: "帮我把这段文献综述改得更学术一点：现在很多人都在研究多模态 Agent，但是怎么评估它好不好用还没有统一说法。", time: "昨天" },
          {
            id: "c2m2",
            role: "assistant",
            status: "done",
            time: "昨天",
            versions: [
              {
                text: `可以。更学术的写法是先把“问题”立住，再引出“缺口”：

> 近年来，多模态 Agent 的能力边界持续扩展，但对其可用性的评估仍缺乏统一框架。现有研究多聚焦于任务完成率，较少讨论工具调用链条的可观测性与失败归因，这构成了本文的研究缺口。

如果你要投的是 CS 方向的会议，我建议把“可观测性”换成更具体的 operational transparency，并在下一句补一篇近两年的代表工作。`,
                reasoning: "",
                search: null,
                memory: ["用户正在写多模态 Agent 方向的毕业论文"],
                followups: ["再给两个版本", "帮我找相关文献"],
              },
            ],
            vIndex: 0,
          },
        ],
      },
      {
        id: "c3",
        title: "降噪耳机对比",
        projectId: null,
        meta: "昨天 · 3 款产品",
        messages: [
          { id: "c3m1", role: "user", text: "帮我对比一下这三款降噪耳机的降噪和佩戴，通勤用。", time: "昨天" },
          {
            id: "c3m2",
            role: "assistant",
            status: "done",
            time: "昨天",
            versions: [
              {
                text: EARPHONE,
                reasoning: "用户给了明确场景（通勤），需要可执行的结论，而不是参数堆砌。先给选型，再解释取舍。",
                search: {
                  queries: ["降噪耳机 通勤 对比 2026", "入耳式 降噪 佩戴 舒适度"],
                  sources: [
                    { id: 1, title: "三款主流降噪耳机横评：通勤场景谁更安静", site: "少数派", domain: "sspai.com", date: "2026-10-28", snippet: "地铁低频降噪差距明显，风噪与佩戴压力是第二梯队的分水岭。", url: "https://sspai.com/" },
                    { id: 2, title: "入耳式降噪耳机长期佩戴舒适度调查", site: "消费者报告", domain: "consumerreports.org", date: "2026-10-20", snippet: "耳压感与耳塞材质的相关性高于重量，戴眼镜用户更敏感。", url: "https://www.consumerreports.org/" },
                  ],
                },
                memory: ["用户每天地铁通勤", "用户戴眼镜"],
                followups: ["我每天通勤 1 小时", "有没有更便宜的"],
              },
            ],
            vIndex: 0,
          },
        ],
      },
      {
        id: "c4",
        title: "周报润色",
        projectId: null,
        meta: "10 月 30 日",
        messages: [
          { id: "c4m1", role: "user", text: "把这段周报改得更像人话：本周完成了接口联调的前两个阶段，登录与列表页已经可以走通，剩余问题集中在异常态。", time: "10 月 30 日" },
          {
            id: "c4m2",
            role: "assistant",
            status: "done",
            time: "10 月 30 日",
            versions: [{ text: WEEKLY_ANSWER, reasoning: "", search: null, memory: [], followups: ["再短一点", "加一个下周计划"] }],
            vIndex: 0,
          },
        ],
      },
    ],
    projects: [
      {
        id: "p1",
        name: "毕业论文",
        icon: "book",
        desc: "多模态 Agent 的可观测性，文献、提纲与答辩材料。",
        instructions: "你是我的论文导师。先指出逻辑漏洞，再给修改建议；引用要具体到页码，不要泛泛而谈。",
        sources: [
          { name: "related_work.pdf", meta: "PDF · 2.4 MB" },
          { name: "experiment_log.xlsx", meta: "XLSX · 180 KB" },
          { name: "答辩提纲.md", meta: "Markdown · 12 KB" },
        ],
        chatIds: ["c2"],
        updated: "今天",
      },
      {
        id: "p2",
        name: "AI 硬件选型",
        icon: "layers",
        desc: "芯片、麦克风阵列与整机方案的对比。",
        instructions: "优先给可量产的方案，标注价格区间与交期；不确定的地方明确说不确定。",
        sources: [
          { name: "芯片报价单.xlsx", meta: "XLSX · 96 KB" },
          { name: "结构方案_v3.pdf", meta: "PDF · 8.1 MB" },
        ],
        chatIds: [],
        updated: "昨天",
      },
      {
        id: "p3",
        name: "个人知识库",
        icon: "sparkle",
        desc: "长期积累的读书笔记与灵感碎片。",
        instructions: "回答时优先引用我自己的笔记，并标出来源。",
        sources: [{ name: "读书笔记.md", meta: "Markdown · 320 KB" }],
        chatIds: [],
        updated: "10 月 26 日",
      },
    ],
    tasks: [
      {
        id: "t1",
        title: "AI 行业早报",
        prompt: "汇总过去 24 小时 AI 行业重要新闻，按重要性排序，附来源。",
        schedule: "每天 08:00",
        timezone: "Asia/Shanghai",
        status: "active",
        next: "明天 08:00",
        last: "今天 08:00 · 12 条",
        icon: "sparkle",
        runs: [
          { at: "今天 08:00", status: "done", result: "12 条更新，3 条值得跟进" },
          { at: "昨天 08:00", status: "done", result: "9 条更新，价格战相关 2 条" },
        ],
      },
      {
        id: "t2",
        title: "竞品价格监控",
        prompt: "每周一检查三家竞品的官网定价与套餐变化，输出差异表。",
        schedule: "每周一 09:30",
        timezone: "Asia/Shanghai",
        status: "paused",
        next: "已暂停",
        last: "10 月 27 日 · 无变化",
        icon: "globe",
        runs: [],
      },
      {
        id: "t3",
        title: "周五周报提醒",
        prompt: "每周五 17:00 提醒我整理本周进展，并给出一个三段式模板。",
        schedule: "每周五 17:00",
        timezone: "Asia/Shanghai",
        status: "active",
        next: "周五 17:00",
        last: "10 月 30 日 · 已提醒",
        icon: "clock",
        runs: [],
      },
    ],
    memories: [
      { id: "mem1", type: "preference", label: "偏好", text: "先给结论，再给理由；不喜欢过长的列表。", source: "来自 3 次对话" },
      { id: "mem2", type: "identity", label: "身份", text: "产品经理，正在做一个 AI 硬件项目。", source: "来自 2026-10-28 的对话" },
      { id: "mem3", type: "project", label: "项目", text: "毕业论文关注“多模态 Agent 的可观测性”。", source: "来自项目「毕业论文」" },
      { id: "mem4", type: "relationship", label: "关系", text: "养了一只猫，叫豆包。", source: "来自 2026-09-30 的对话" },
    ],
    activeChatId: "c1",
    activeProjectId: "p1",
    mode: { thinking: "auto", web: "auto", style: "standard" },
    settings: { memory: true, autoWeb: true, voiceAutoStart: false, fontScale: 1, haptics: true },
    research: { step: "input", topic: "", progress: 0 },
  };
}

/* ------------------------------ state ------------------------------ */

function loadState() {
  const base = seedState();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return base;
    const saved = JSON.parse(raw);
    return {
      ...base,
      ...saved,
      mode: { ...base.mode, ...(saved.mode || {}) },
      settings: { ...base.settings, ...(saved.settings || {}) },
      research: { ...base.research, ...(saved.research || {}) },
    };
  } catch (e) {
    return base;
  }
}

let state = loadState();
state.route = "home";
state.attachments = [];
state.homeDraft = "";
state.stream = null;
state.voice = null;
state.voiceTimers = [];
state.researchTimers = [];
state.taskDraft = null;
state.lastShareMsg = null;

function saveState() {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        theme: state.theme,
        chats: state.chats,
        projects: state.projects,
        tasks: state.tasks,
        memories: state.memories,
        activeChatId: state.activeChatId,
        activeProjectId: state.activeProjectId,
        mode: state.mode,
        settings: state.settings,
        research: state.research,
      })
    );
  } catch (e) {
    /* storage may be full or unavailable */
  }
}

const findChat = (id) => state.chats.find((c) => c.id === id);
const findProject = (id) => state.projects.find((p) => p.id === id);
const findTask = (id) => state.tasks.find((t) => t.id === id);
const activeChat = () => findChat(state.activeChatId) || state.chats[0];
const activeProject = () => findProject(state.activeProjectId) || state.projects[0];
const currentVersion = (msg) => msg.versions?.[msg.vIndex ?? 0] || msg.versions?.[0] || {};

/* ------------------------------ theme / toast ------------------------------ */

function applyTheme() {
  document.documentElement.dataset.theme = state.theme;
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.content = state.theme === "dark" ? "#151517" : "#f5f6f7";
  document.documentElement.style.setProperty("--font-scale", state.settings.fontScale || 1);
}

let toastTimer = null;
function toast(message) {
  const el = $("#toast");
  if (!el) return;
  el.textContent = message;
  el.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("show"), 1900);
}

function haptic() {
  if (state.settings.haptics && navigator.vibrate) navigator.vibrate(8);
}

/* ------------------------------ navigation / render ------------------------------ */

function navigate(route, opts = {}) {
  state.route = route;
  if (opts.chatId) state.activeChatId = opts.chatId;
  if (opts.projectId) state.activeProjectId = opts.projectId;
  state.chatScroll = null;
  closeOverlay(true);
  render({ bottom: route === "chat" });
}

function closeOverlay(immediate = false) {
  const ov = $("#overlay");
  if (!ov) return;
  ov.classList.remove("open");
  $("#toast")?.classList.remove("on-voice");
  clearVoiceTimers();
  state.voice = null;
  const clear = () => {
    if (!ov.classList.contains("open")) ov.innerHTML = "";
  };
  if (immediate) clear();
  else setTimeout(clear, 280);
}

function openOverlay(html) {
  const ov = $("#overlay");
  ov.innerHTML = html;
  // force animation frame so the open class transitions
  requestAnimationFrame(() => ov.classList.add("open"));
}

function statusbar() {
  return `<div class="statusbar"><span>${nowTime()}</span><span class="sb-right"><span class="sb-5g">5G</span><span class="sb-sig"><i></i><i></i><i></i><i></i></span><span class="sb-batt"></span></span></div>`;
}

function updateRail() {
  $$("#rail-nav button").forEach((b) => {
    const jump = b.dataset.jump;
    b.classList.toggle("active", jump === state.route || (jump === "projects" && state.route === "project"));
  });
  const label = $("[data-theme-label]");
  const themeIcon = $("[data-theme-icon] use");
  if (label) label.textContent = state.theme === "dark" ? "深色" : "浅色";
  if (themeIcon) themeIcon.setAttribute("href", state.theme === "dark" ? "#i-moon" : "#i-sun");
}

function render(opts = {}) {
  const sc = $("#chat-scroll");
  if (sc) state.chatScroll = sc.scrollTop;
  const app = $("#app");
  app.innerHTML = statusbar() + screenHTML();
  updateRail();
  applyTheme();
  afterRender(opts);
}

function screenHTML() {
  switch (state.route) {
    case "chat":
      return chatScreen();
    case "projects":
      return projectsScreen();
    case "project":
      return projectScreen();
    case "tasks":
      return tasksScreen();
    case "task":
      return taskScreen();
    case "memory":
      return memoryScreen();
    case "me":
      return meScreen();
    case "settings":
      return settingsScreen();
    case "research":
      return researchScreen();
    default:
      return homeScreen();
  }
}

function afterRender(opts = {}) {
  const sc = $("#chat-scroll");
  if (sc) {
    if (opts.bottom || state.forceBottom) {
      sc.scrollTop = sc.scrollHeight;
      state.forceBottom = false;
    } else if (state.chatScroll != null) {
      sc.scrollTop = state.chatScroll;
    }
    sc.addEventListener("scroll", onChatScroll, { passive: true });
    onChatScroll();
  }
  const ta = $("#composer-input");
  if (ta) {
    autoGrow(ta);
    if (opts.focus) requestAnimationFrame(() => ta.focus());
  }
  if (state.route === "research" && state.research?.step === "report") {
    $$(".report-body h2").forEach((h, i) => { h.id = "r" + (i + 1); });
  }
}

function onChatScroll() {
  const sc = $("#chat-scroll");
  if (!sc) return;
  state.chatScroll = sc.scrollTop;
  const btn = $("#scroll-bottom");
  if (!btn) return;
  const away = sc.scrollHeight - sc.scrollTop - sc.clientHeight > 80;
  btn.style.display = away ? "grid" : "none";
}

/* ------------------------------ topbars ------------------------------ */

function topbarHome() {
  return `<header class="topbar">
    <button class="avatar" data-action="open-drawer" aria-label="打开历史与菜单">C</button>
    <div class="brand">DeepSeek<span class="brand-dot">.</span></div>
    <div class="tb-actions">
      <button class="icon-btn" data-action="new-chat" aria-label="新对话">${icon("edit", "icon-sm")}</button>
    </div>
  </header>`;
}

function topbarPage(title, action = "") {
  return `<header class="topbar">
    <div class="tb-title">${esc(title)}</div>
    <div class="tb-actions">${action}</div>
  </header>`;
}

function topbarBack(title, action = "", subtitle = "") {
  return `<header class="topbar">
    <button class="icon-btn" data-action="back" aria-label="返回">${icon("chevron-left")}</button>
    <div class="tb-title">${esc(title)}${subtitle ? `<span class="tb-sub">${esc(subtitle)}</span>` : ""}</div>
    <div class="tb-actions">${action}</div>
  </header>`;
}

function topbarChat() {
  const chat = activeChat();
  return `<header class="topbar">
    <button class="icon-btn" data-action="back" aria-label="返回">${icon("chevron-left")}</button>
    <div class="tb-title">${esc(chat?.title || "新对话")}</div>
    <div class="tb-actions">
      <button class="icon-btn" data-action="open-chat-menu" aria-label="更多">${icon("more")}</button>
    </div>
  </header>`;
}

/* ------------------------------ home ------------------------------ */

function greeting() {
  const h = new Date().getHours();
  if (h < 6) return "夜深了";
  if (h < 11) return "早上好";
  if (h < 14) return "中午好";
  if (h < 18) return "下午好";
  return "晚上好";
}

function homeScreen() {
  const suggestions = [
    { icon: "file", title: "读一份报告", sub: "上传 PDF，提炼结论", text: "帮我读一份报告，先给结论，再列三个关键判断。" },
    { icon: "globe", title: "对比几款产品", sub: "联网查资料，给出选型", text: "帮我对比一下这三款降噪耳机，通勤用，重点看降噪和佩戴。" },
    { icon: "edit", title: "写一封邮件", sub: "把草稿改得更得体", text: "帮我把这段周报改得更像人话，保留信息量。" },
    { icon: "sparkle", title: "解释一个概念", sub: "从原理到例子讲清楚", text: "解释一下 MCP 是什么，用一个例子说明它为什么重要。" },
  ];
  const recents = state.chats.filter((c) => c.messages.length).slice(0, 3);
  return `<div class="screen">
    ${topbarHome()}
    <main class="scroll home-scroll">
      <section class="home-head">
        <h1 class="greeting">${greeting()}，<span class="accent">Cris</span></h1>
        <p class="home-sub">今天想聊点什么？</p>
        <button class="auto-line" data-action="open-mode">${icon("sparkle")}<span><b>自动模式</b> · 按需思考与联网</span></button>
      </section>
      <section class="suggestions">
        ${suggestions
          .map(
            (s) => `<button class="suggestion" data-action="use-suggestion" data-text="${esc(s.text)}">
              <span class="s-icon">${icon(s.icon)}</span>
              <span><span class="s-title">${s.title}</span><span class="s-sub">${s.sub}</span></span>
            </button>`
          )
          .join("")}
      </section>
      <div class="section-label"><span>最近对话</span><button data-action="open-drawer">查看全部</button></div>
      <div class="recent-list">
        ${recents
          .map(
            (c, i) => `<button class="recent-item" data-action="open-chat" data-id="${c.id}">
              <span class="recent-icon ${i === 0 ? "accent" : ""}">${i === 0 ? icon("sparkle", "icon-sm") : esc(c.title.slice(0, 2))}</span>
              <span class="recent-main"><span class="recent-title">${esc(c.title)}</span><span class="recent-meta">${esc(c.meta || "最近更新")}</span></span>
              <span class="recent-time">${esc((c.messages[c.messages.length - 1]?.time) || "")}</span>
            </button>`
          )
          .join("")}
      </div>
    </main>
    ${composer("home")}
    ${tabbar("home")}
  </div>`;
}

function tabbar(active) {
  const items = [
    { key: "home", icon: "chat", label: "对话" },
    { key: "projects", icon: "folder", label: "项目" },
    { key: "tasks", icon: "clock", label: "任务" },
    { key: "me", icon: "user", label: "我的" },
  ];
  return `<nav class="tabbar">${items
    .map(
      (i) => `<button class="tab ${active === i.key ? "active" : ""}" data-action="tab" data-tab="${i.key}">
        ${icon(i.icon)}<span>${i.label}</span></button>`
    )
    .join("")}</nav>`;
}

/* ------------------------------ composer ------------------------------ */

function modeLabel() {
  const { thinking, web } = state.mode;
  if (thinking === "auto" && web === "auto") return "自动";
  const t = thinking === "deep" ? "深入" : thinking === "fast" ? "快速" : "自动";
  const w = web === "on" ? "联网" : web === "off" ? "不联网" : "";
  return [t, w].filter(Boolean).join(" · ");
}

function modeHint() {
  const manual = state.mode.thinking !== "auto" || state.mode.web !== "auto";
  return manual ? "仅作用于当前对话" : "自动选择思考与联网";
}

function composer(context = "home") {
  const chat = context === "chat" ? activeChat() : null;
  const project = context === "project" ? activeProject() : null;
  const draft = context === "home" ? state.homeDraft : context === "project" ? project?.draft || "" : chat?.draft || "";
  const streaming = context === "chat" && state.stream && state.stream.chatId === chat?.id;
  const atts = state.attachments || [];
  const placeholder = context === "project" ? "在项目中提问…" : streaming ? "正在回答…" : "输入消息…";
  const chips = [];
  if (project) chips.push(`<span class="chip project">${icon("folder")}${esc(project.name)}</span>`);
  if (chat?.projectId && context === "chat") {
    const p = findProject(chat.projectId);
    if (p) chips.push(`<span class="chip project">${icon("folder")}${esc(p.name)}</span>`);
  }
  atts.forEach((a, i) => {
    chips.push(`<span class="chip">${a.kind === "image" ? `<img src="${a.dataUrl}" alt="">` : icon("file")}<span class="chip-name">${esc(a.name)}</span><button class="chip-x" data-action="remove-attachment" data-idx="${i}" aria-label="移除">${icon("close", "icon-xs")}</button></span>`);
  });
  return `<div class="composer-wrap">
    <div class="mode-row">
      <button class="mode-pill" data-action="open-mode">${icon("sparkle")}<span>${modeLabel()}</span>${icon("chevron-down", "icon-xs")}</button>
      <span class="mode-hint">${modeHint()}</span>
    </div>
    ${chips.length ? `<div class="chips">${chips.join("")}</div>` : ""}
    <div class="composer">
      <button class="icon-btn" data-action="open-tools" aria-label="添加">${icon("plus")}</button>
      <textarea id="composer-input" rows="1" data-context="${context}" placeholder="${placeholder}" enterkeyhint="send">${esc(draft)}</textarea>
      <button class="icon-btn" data-action="dictate" aria-label="听写">${icon("mic")}</button>
      <button class="icon-btn" data-action="voice" aria-label="语音通话">${icon("wave")}</button>
      <button class="send-btn ${streaming ? "stop" : ""}" data-action="send" aria-label="${streaming ? "停止" : "发送"}" ${streaming || draft.trim() || atts.length ? "" : "disabled"}>
        ${streaming ? icon("stop") : icon("arrow-up")}
      </button>
    </div>
  </div>`;
}

function autoGrow(el) {
  el.style.height = "auto";
  el.style.height = Math.min(120, Math.max(33, el.scrollHeight)) + "px";
}

function onComposerInput(e) {
  const ta = e.target;
  const context = ta.dataset.context;
  if (context === "home") state.homeDraft = ta.value;
  else if (context === "project") {
    const p = activeProject();
    if (p) p.draft = ta.value;
  } else {
    const c = activeChat();
    if (c) c.draft = ta.value;
  }
  const btn = $('[data-action="send"]');
  if (btn) btn.disabled = !ta.value.trim() && !(state.attachments || []).length;
  autoGrow(ta);
}

/* ------------------------------ chat ------------------------------ */

function chatScreen() {
  const chat = activeChat();
  if (!chat) return `<div class="screen">${topbarChat()}<div class="empty">没有对话</div></div>`;
  const empty = chat.messages.length === 0;
  return `<div class="screen">
    ${topbarChat()}
    <main class="scroll chat-scroll" id="chat-scroll">
      ${empty ? chatEmpty() : chat.messages.map((m) => renderMessage(m, chat)).join("")}
    </main>
    ${composer("chat")}
  </div>`;
}

function chatEmpty() {
  const prompts = [
    "今天 AI 圈有什么大事？",
    "帮我对比三款降噪耳机",
    "解释一下 MCP 是什么",
  ];
  return `<div class="chat-empty">
    <div class="ce-mark">${icon("sparkle", "icon-lg")}</div>
    <h2>开始新的对话</h2>
    <p>所有输入都从这一个框进入。<br>需要查证时自动联网，需要深入时自动思考。</p>
    <div class="ce-prompts">${prompts.map((p) => `<button class="followup" data-action="use-suggestion" data-text="${esc(p)}">${esc(p)}</button>`).join("")}</div>
  </div>`;
}

function renderMessage(msg, chat) {
  if (msg.role === "user") {
    const list = msg.attachments || [];
    const imgs = list.filter((a) => a.kind === "image");
    const files = list.filter((a) => a.kind !== "image");
    return `<div class="msg-user">${esc(msg.text)}${imgs.length ? `<div class="msg-images">${imgs.map((a) => `<img src="${a.dataUrl}" alt="${esc(a.name)}">`).join("")}</div>` : ""}${files.length ? `<div class="msg-atts">${files.map((a) => `<span class="msg-att">${icon("file", "icon-xs")}${esc(a.name)}</span>`).join("")}</div>` : ""}</div>`;
  }
  const v = currentVersion(msg);
  const streaming = msg.status === "streaming";
  const thinking = msg.status === "thinking";
  const versions = msg.versions || [];
  const hasVersionNav = versions.length > 1;
  const body =
    msg.status === "failed"
      ? `<div class="error-card">${icon("alert", "icon-sm")}<div><b>生成失败</b><span>网络或服务暂时不可用，请重试。</span></div><button class="btn ghost" data-action="regenerate" data-id="${msg.id}">重试</button></div>`
      : msg.status === "moderated"
      ? `<div class="moderated-card">${icon("lock", "icon-sm")} 这个请求触发了安全策略，换一种问法试试。</div>`
      : thinking || (streaming && !v.text)
      ? `<div class="thinking-row"><span class="typing"><i></i><i></i><i></i></span><span>正在思考…</span></div>`
      : mdToHtml(v.text || "") + (streaming ? `<span class="cursor"></span>` : "");

  return `<div class="msg-assistant" data-msg="${msg.id}">
    <div class="msg-head">
      <span class="msg-avatar">DS</span>
      <span>DeepSeek</span>
      <span class="msg-time">${esc(msg.time || "")}</span>
      ${hasVersionNav ? `<div class="version-nav"><button data-action="version-prev" data-id="${msg.id}" aria-label="上一版">${icon("chevron-left")}</button><span>${msg.vIndex + 1}/${versions.length}</span><button data-action="version-next" data-id="${msg.id}" aria-label="下一版">${icon("chevron-right")}</button></div>` : ""}
    </div>
    ${renderRouter(msg, v)}
    ${v.search ? `<button class="activity" data-action="open-process" data-id="${msg.id}"><span class="a-dot"></span><span class="a-text">${v.search.kind === "file" ? "已读取 1 个文件 · 3 处引用" : `已搜索 ${v.search.queries?.length || 1} 个关键词 · 阅读 ${v.search.readCount || 8} 个来源`}</span>${icon("chevron-right", "icon-xs chev")}</button>` : ""}
    ${v.reasoning ? `<button class="reason-chip" data-action="toggle-reason" data-id="${msg.id}">${icon("sparkle")}思考了 ${v.reasoningSeconds || 12} 秒${icon("chevron-down", "icon-xs")}</button><div class="reason-box" data-reason="${msg.id}">${esc(v.reasoning)}</div>` : ""}
    <div class="msg-body" data-body="${msg.id}">${body}</div>
    ${msg.status === "stopped" ? `<div class="stopped-row">已停止<button class="link-btn" data-action="continue-msg" data-id="${msg.id}">继续生成</button></div>` : ""}
    ${v.search?.sources?.length ? renderSources(msg, v.search.sources) : ""}
    ${v.memory?.length ? `<button class="memory-chip" data-action="open-memory-used" data-id="${msg.id}">${icon("sparkle")}使用了 ${v.memory.length} 条记忆${icon("chevron-right", "icon-xs")}</button>` : ""}
    ${msg.status === "done" ? renderActions(msg, v) : ""}
    ${msg.status === "done" && v.followups?.length ? `<div class="followups">${v.followups.map((f) => `<button class="followup" data-action="followup" data-text="${esc(f)}">${esc(f)}</button>`).join("")}</div>` : ""}
  </div>`;
}

function renderRouter(msg, v) {
  if (!msg || !v || msg.kind === "voice") return "";
  if (msg.status === "thinking") {
    return `<div class="router-line"><span class="rl-dot"></span><span>自动 · 正在判断</span></div>`;
  }
  const t = v.reasoning ? "深入思考" : "快速回答";
  const w = v.search ? "已联网" : "未联网";
  return `<div class="router-line"><span class="rl-dot"></span><span>自动 · ${t} · ${w}</span><button class="link-btn" data-action="why-route">为什么</button></div>`;
}

function renderSources(msg, sources) {
  const shown = sources.slice(0, 2);
  return `<div class="sources">
    <div class="sources-head"><span>来源 <b>${sources.length}</b></span><button class="link-btn" data-action="open-sources" data-id="${msg.id}">查看全部</button></div>
    <div class="source-list">
      ${shown
        .map(
          (s, i) => `<button class="source" data-action="open-source" data-id="${msg.id}" data-n="${i + 1}">
            <span class="source-icon">${esc((s.site || s.domain || "?").slice(0, 2))}</span>
            <span class="source-main"><span class="source-title">${esc(s.title)}</span><span class="source-meta">${esc(s.site || s.domain)} · ${esc(s.date || "本地文件")}</span></span>
          </button>`
        )
        .join("")}
    </div>
  </div>`;
}

function renderActions(msg, v) {
  return `<div class="actions">
    <button class="action" data-action="copy-msg" data-id="${msg.id}" title="复制">${icon("copy")}</button>
    <button class="action" data-action="regenerate" data-id="${msg.id}" title="重新生成">${icon("refresh")}</button>
    <button class="action" data-action="share-msg" data-id="${msg.id}" title="分享">${icon("share")}</button>
    <button class="action" data-action="read-msg" data-id="${msg.id}" title="朗读">${icon("volume")}</button>
    <button class="action ${msg.liked ? "active" : ""}" data-action="like" data-id="${msg.id}" title="有帮助">${icon("thumb-up")}</button>
    <button class="action ${msg.disliked ? "active" : ""}" data-action="dislike" data-id="${msg.id}" title="没帮助">${icon("thumb-down")}</button>
  </div>`;
}

/* ------------------------------ markdown ------------------------------ */

function inlineMd(text) {
  let h = esc(text);
  h = h.replace(/`([^`]+)`/g, "<code>$1</code>");
  h = h.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  h = h.replace(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
  h = h.replace(/\[(\d+)\]/g, '<button class="cite" data-action="cite" data-n="$1">$1</button>');
  return h;
}

function mdToHtml(md) {
  if (!md) return "";
  const codes = [];
  let src = String(md).replace(/```([a-zA-Z0-9+#.-]*)\n([\s\S]*?)```/g, (_, lang, code) => {
    const i = codes.length;
    codes.push(`<div class="code-block"><div class="code-head"><span>${esc(lang || "code")}</span><button class="link-btn" data-action="copy-code" data-code="${encodeURIComponent(code)}">复制</button></div><pre><code>${esc(code.replace(/\n$/, ""))}</code></pre></div>`);
    return `\n\n@@CODE${i}@@\n\n`;
  });

  const lines = src.split("\n");
  const blocks = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (/^\s*$/.test(line)) { i++; continue; }

    // table
    if (line.includes("|") && lines[i + 1] && /^\s*\|?[\s:|-]+\|?\s*$/.test(lines[i + 1]) && lines[i + 1].includes("-")) {
      const split = (l) => l.replace(/^\s*\|/, "").replace(/\|\s*$/, "").split("|").map((c) => c.trim());
      const head = split(line);
      i += 2;
      const rows = [];
      while (i < lines.length && lines[i].includes("|") && !/^\s*$/.test(lines[i])) {
        rows.push(split(lines[i]));
        i++;
      }
      blocks.push(`<div class="table-wrap"><table><thead><tr>${head.map((c) => `<th>${inlineMd(c)}</th>`).join("")}</tr></thead><tbody>${rows.map((r) => `<tr>${r.map((c) => `<td>${inlineMd(c)}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`);
      continue;
    }

    // heading
    const hm = line.match(/^(#{2,4})\s+(.+)$/);
    if (hm) {
      const level = Math.min(3, Math.max(2, hm[1].length));
      blocks.push(`<h${level}>${inlineMd(hm[2])}</h${level}>`);
      i++;
      continue;
    }

    // blockquote
    if (/^>\s?/.test(line)) {
      const q = [];
      while (i < lines.length && /^>\s?/.test(lines[i])) {
        q.push(lines[i].replace(/^>\s?/, ""));
        i++;
      }
      blocks.push(`<blockquote>${q.map(inlineMd).join("<br>")}</blockquote>`);
      continue;
    }

    // list
    if (/^\s*([-*]|\d+\.)\s+/.test(line)) {
      const ordered = /^\s*\d+\.\s+/.test(line);
      const items = [];
      while (i < lines.length && /^\s*([-*]|\d+\.)\s+/.test(lines[i])) {
        items.push(lines[i].replace(/^\s*([-*]|\d+\.)\s+/, ""));
        i++;
      }
      const tag = ordered ? "ol" : "ul";
      blocks.push(`<${tag}>${items.map((it) => `<li>${inlineMd(it)}</li>`).join("")}</${tag}>`);
      continue;
    }

    // code placeholder
    if (/^@@CODE\d+@@$/.test(line.trim())) {
      const idx = Number(line.trim().match(/@@CODE(\d+)@@/)[1]);
      blocks.push(codes[idx] || "");
      i++;
      continue;
    }

    // paragraph (single line; consecutive non-empty lines join with <br>)
    const p = [line];
    i++;
    while (i < lines.length && !/^\s*$/.test(lines[i]) && !/^(#{2,4})\s+/.test(lines[i]) && !/^>\s?/.test(lines[i]) && !/^\s*([-*]|\d+\.)\s+/.test(lines[i]) && !/^@@CODE\d+@@$/.test(lines[i].trim())) {
      p.push(lines[i]);
      i++;
    }
    blocks.push(`<p>${p.map(inlineMd).join("<br>")}</p>`);
  }
  return blocks.join("");
}

function plainText(md) {
  return String(md || "")
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/[#>*_`]/g, "")
    .replace(/\[(\d+)\]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

/* ------------------------------ send / stream ------------------------------ */

function makeTitle(text, attachments = []) {
  const t = (text || "").trim().replace(/\s+/g, " ");
  if (attachments.length) return attachments[0].kind === "image" ? "图片理解" : "报告解读";
  if (/耳机|降噪/.test(t)) return "降噪耳机对比";
  if (/邮件/.test(t)) return "邮件润色";
  if (/周报/.test(t)) return "周报润色";
  if (/MCP/i.test(t)) return "MCP 是什么";
  if (/PDF|pdf|报告|文件|论文/.test(t)) return "报告解读";
  if (/AI|新闻|行业/.test(t)) return "AI 行业早报";
  if (t) return t.length > 16 ? t.slice(0, 16) + "…" : t;
  if (attachments.length) return attachments[0].name || "新对话";
  return "新对话";
}

function answerFor(text) {
  if (/图片|这张图/.test(text)) return { text: IMAGE_ANSWER };
  if (/耳机|降噪/.test(text)) return { text: EARPHONE };
  if (/文献|综述|学术/.test(text)) return { text: MW_ANSWER };
  if (/论文|PDF|pdf|报告|文件|资料/.test(text)) return { text: FILE_ANSWER };
  if (/邮件|周报|润色|更像人话/.test(text)) return { text: /邮件/.test(text) ? EMAIL_ANSWER : WEEKLY_ANSWER };
  if (/MCP|概念|解释/.test(text)) return { text: MCP_ANSWER };
  if (/AI|新闻|行业|大事|发布/.test(text)) return { text: AI_NEWS };
  return { text: GENERIC_ANSWER };
}

function reasoningFor(text) {
  if (state.mode.thinking === "fast") return "";
  if (state.mode.thinking === "deep") {
    return `这个问题值得拆开看。先把目标、约束和可选路径分别列出来，再判断哪条路径的代价最低。
用户没有给太多背景，所以先给一个可执行的默认方案，并标出需要补充的信息。`;
  }
  if (/AI|新闻|行业|大事|耳机|降噪|PDF|报告|文件|对比|调研|分析/.test(text)) {
    if (/耳机|降噪/.test(text)) return "用户给了明确场景（通勤），需要可执行的结论，而不是参数堆砌。先给选型，再解释取舍。";
    if (/PDF|报告|文件/.test(text)) return "先定位文档里的结论页和关键数据，再用“一句话结论 + 三个判断”的方式组织，避免复述全文。";
    return AI_REASONING;
  }
  return "";
}

function searchFor(text) {
  if (state.mode.web === "off") return null;
  if (state.mode.web === "auto" && !/最新|今天|新闻|价格|对比|调研|行业|AI|发布|耳机|降噪|PDF|pdf|报告|文件|论文|资料/.test(text)) return null;
  if (/耳机|降噪/.test(text)) {
    return {
      queries: ["降噪耳机 通勤 对比 2026", "入耳式 降噪 佩戴 舒适度"],
      sources: [
        { id: 1, title: "三款主流降噪耳机横评：通勤场景谁更安静", site: "少数派", domain: "sspai.com", date: "2026-10-28", snippet: "地铁低频降噪差距明显，风噪与佩戴压力是第二梯队的分水岭。", url: "https://sspai.com/" },
        { id: 2, title: "入耳式降噪耳机长期佩戴舒适度调查", site: "消费者报告", domain: "consumerreports.org", date: "2026-10-20", snippet: "耳压感与耳塞材质的相关性高于重量，戴眼镜用户更敏感。", url: "https://www.consumerreports.org/" },
      ],
      readCount: 6,
    };
  }
  if (/PDF|报告|文件|论文|资料/.test(text)) {
    return {
      kind: "file",
      queries: ["本地文件：AI_infra_report_2026.pdf"],
      sources: [
        { id: 1, title: "AI_infra_report_2026.pdf", site: "本地文件", domain: "AI_infra_report_2026.pdf", date: "18 页", snippet: "第 3 页：推理成本与稳定供给；第 8 页：按任务成功率采购；第 12 页：开源模型的工具调用。", url: "#", local: true },
      ],
      readCount: 1,
    };
  }
  return {
    queries: ["2026年11月3日 AI 行业 新闻", "大模型 多模态 长上下文", "推理集群 API 价格"],
    sources: AI_SOURCES,
    readCount: 8,
  };
}

function memoryFor(text) {
  if (!state.settings.memory) return [];
  if (/不要记|别记/.test(text)) return [];
  if (/耳机|降噪/.test(text)) return ["用户每天地铁通勤", "用户戴眼镜"];
  if (/论文|文献|综述/.test(text)) return ["用户正在写多模态 Agent 方向的毕业论文"];
  if (/AI|新闻|行业/.test(text)) return ["用户偏好先给结论，再给理由", "用户关注 AI 行业动态"];
  return [];
}

function followupsFor(text) {
  if (/耳机|降噪/.test(text)) return ["我每天通勤 1 小时", "有没有更便宜的"];
  if (/PDF|报告|文件/.test(text)) return ["展开第 3 页", "把结论整理成一页纸"];
  if (/邮件|周报/.test(text)) return ["再短一点", "加一个下周计划"];
  if (/AI|新闻|行业/.test(text)) return ["展开第 2 条", "整理成可分享的早报"];
  if (/MCP|概念/.test(text)) return ["举个更具体的例子", "它和插件有什么区别"];
  return ["换个角度说说", "给我一个可执行的下一步"];
}

function sendMessage(text, context = "chat") {
  const atts = (state.attachments || []).slice();
  let clean = String(text || "").trim();
  if (!clean && atts.length) clean = atts[0].kind === "image" ? "请看看这张图片，告诉我里面有什么。" : "请看看这份 PDF 报告，提炼核心结论。";
  if (!clean) return;
  if (state.stream) stopStream();

  let chat;
  if (context === "home") {
    chat = { id: uid("chat"), title: makeTitle(clean, atts), projectId: null, meta: "刚刚", messages: [], draft: "" };
    state.chats.unshift(chat);
    state.activeChatId = chat.id;
  } else if (context === "project") {
    const p = activeProject();
    chat = { id: uid("chat"), title: makeTitle(clean, atts), projectId: p.id, meta: "刚刚", messages: [], draft: "" };
    state.chats.unshift(chat);
    p.chatIds.unshift(chat.id);
    state.activeChatId = chat.id;
  } else {
    chat = activeChat();
  }
  if (!chat) return;
  if (!chat.messages.length) chat.title = makeTitle(clean, atts);

  chat.messages.push({ id: uid("m"), role: "user", text: clean || "请看看这些附件", attachments: atts, time: nowTime() });
  chat.meta = "刚刚";
  if (context === "home") state.homeDraft = "";
  else if (context === "project") {
    const p = activeProject();
    if (p) p.draft = "";
  } else chat.draft = "";
  state.attachments = [];

  const msg = {
    id: uid("m"),
    role: "assistant",
    status: "thinking",
    time: nowTime(),
    versions: [{ text: "", reasoning: "", search: null, memory: [], followups: [] }],
    vIndex: 0,
  };
  chat.messages.push(msg);
  state.route = "chat";
  state.activeChatId = chat.id;
  state.forceBottom = true;
  saveState();
  render({ bottom: true });

  clearTimeout(state.sendTimer);
  state.sendTimer = setTimeout(() => {
    const v = currentVersion(msg);
    v.reasoning = reasoningFor(clean);
    v.reasoningSeconds = 8 + Math.floor(Math.random() * 7);
    v.search = searchFor(clean);
    v.memory = memoryFor(clean);
    v.followups = followupsFor(clean);
    msg.status = "streaming";
    saveState();
    render({ bottom: true });
    startStream(chat, msg, answerFor(clean).text);
  }, 780);
}

function startStream(chat, msg, fullText, startAt = 0) {
  clearInterval(state.stream?.timer);
  const v = currentVersion(msg);
  v.text = fullText.slice(0, startAt);
  v.fullText = fullText;
  msg.status = "streaming";
  state.stream = { chatId: chat.id, msgId: msg.id, fullText, i: startAt, timer: null };
  updateStreamingDOM(msg, v);
  state.stream.timer = setInterval(() => {
    if (!state.stream || state.stream.msgId !== msg.id) return;
    const step = 3 + Math.floor(Math.random() * 5);
    state.stream.i = Math.min(fullText.length, state.stream.i + step);
    v.text = fullText.slice(0, state.stream.i);
    updateStreamingDOM(msg, v);
    if (state.stream.i >= fullText.length) {
      clearInterval(state.stream.timer);
      state.stream = null;
      msg.status = "done";
      v.text = fullText;
      saveState();
      state.forceBottom = true;
      render({ bottom: true });
    }
  }, 18);
}

function updateStreamingDOM(msg, v) {
  const el = document.querySelector(`[data-body="${msg.id}"]`);
  if (!el) return;
  el.innerHTML = mdToHtml(v.text || "") + '<span class="cursor"></span>';
  const sc = $("#chat-scroll");
  if (sc && sc.scrollHeight - sc.scrollTop - sc.clientHeight < 120) sc.scrollTop = sc.scrollHeight;
}

function stopStream() {
  if (!state.stream) return;
  const { chatId, msgId } = state.stream;
  clearInterval(state.stream.timer);
  state.stream = null;
  const chat = findChat(chatId);
  const msg = chat?.messages.find((m) => m.id === msgId);
  if (msg) {
    msg.status = "stopped";
    const v = currentVersion(msg);
    v.fullText = v.fullText || v.text;
    saveState();
  }
  render({ bottom: true });
}

function continueMsg(id) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === id);
  if (!msg) return;
  const v = currentVersion(msg);
  const full = v.fullText || v.text;
  if (!full) return;
  startStream(chat, msg, full, (v.text || "").length);
}

function regenerate(id) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === id);
  if (!msg) return;
  const idx = chat.messages.indexOf(msg);
  const userText = chat.messages[idx - 1]?.text || "";
  const v = {
    text: "",
    reasoning: reasoningFor(userText),
    reasoningSeconds: 7 + Math.floor(Math.random() * 8),
    search: searchFor(userText),
    memory: memoryFor(userText),
    followups: followupsFor(userText),
  };
  msg.versions.push(v);
  msg.vIndex = msg.versions.length - 1;
  msg.status = "streaming";
  state.forceBottom = true;
  saveState();
  render({ bottom: true });
  startStream(chat, msg, answerFor(userText).text);
}

/* ------------------------------ sheets ------------------------------ */

function sheetShell(title, sub, body, extra = "") {
  return `<div class="backdrop" data-action="close-sheet"></div>
    <div class="sheet" role="dialog" aria-modal="true" aria-label="${esc(title)}">
      <div class="sheet-grabber"></div>
      <div class="sheet-head">
        <div><div class="sheet-title">${esc(title)}</div>${sub ? `<div class="sheet-sub">${esc(sub)}</div>` : ""}</div>
        <button class="sheet-close" data-action="close-sheet" aria-label="关闭">${icon("close")}</button>
      </div>
      ${body}
      ${extra}
    </div>`;
}

function openModeSheet() {
  const m = state.mode;
  const seg = (key, label, options) => `<div class="opt-group">
    <div class="opt-label"><span>${label}</span><span>${key === "thinking" ? "自动按问题难度选择" : key === "web" ? "自动判断是否需要最新信息" : "影响回答的长度与结构"}</span></div>
    <div class="segmented">${options.map((o) => `<button class="${m[key] === o.v ? "active" : ""}" data-action="set-mode" data-key="${key}" data-value="${o.v}">${o.l}</button>`).join("")}</div>
  </div>`;
  const body = `
    ${seg("thinking", "思考深度", [{ v: "auto", l: "自动" }, { v: "fast", l: "快速" }, { v: "deep", l: "深入" }])}
    ${seg("web", "联网搜索", [{ v: "auto", l: "自动" }, { v: "on", l: "开启" }, { v: "off", l: "关闭" }])}
    ${seg("style", "回答风格", [{ v: "concise", l: "简洁" }, { v: "standard", l: "标准" }, { v: "detailed", l: "详细" }])}
    <div class="sheet-note">手动设置只作用于当前对话。新对话会回到自动模式；你随时可以在这里改回来。</div>
    <div class="sheet-actions"><button class="btn ghost block" data-action="reset-mode">${icon("refresh")}重置为自动</button></div>`;
  openOverlay(sheetShell("对话能力", "自动可解释，手动可接管", body));
}

function openToolsSheet() {
  const body = `<div class="tool-grid">
    <button class="tool" data-action="attach-image"><span class="t-icon">${icon("camera")}</span><span class="t-name">拍照</span><span class="t-sub">识图 / 扫描</span></button>
    <button class="tool" data-action="attach-image"><span class="t-icon">${icon("image")}</span><span class="t-name">相册</span><span class="t-sub">最多 9 张</span></button>
    <button class="tool" data-action="attach-file"><span class="t-icon">${icon("file")}</span><span class="t-name">文件</span><span class="t-sub">PDF / Office</span></button>
    <button class="tool" data-action="start-research"><span class="t-icon">${icon("layers")}</span><span class="t-name">深度研究</span><span class="t-sub">计划 → 报告</span></button>
    <button class="tool" data-action="canvas"><span class="t-icon">${icon("edit")}</span><span class="t-name">画布</span><span class="t-sub">文档 / 网页</span></button>
    <button class="tool" data-action="project-sources"><span class="t-icon">${icon("folder")}</span><span class="t-name">项目资料</span><span class="t-sub">引用上下文</span></button>
  </div>
  <div class="sheet-note">原型演示：文件与图片会在本地读取，不会上传。深度研究使用模拟进度。</div>
  <div class="sheet-note" style="margin-top:6px">没有准备文件？<button class="link-btn" data-action="demo-attachment" data-kind="file">添加示例 PDF</button> · <button class="link-btn" data-action="demo-attachment" data-kind="image">示例图片</button></div>`;
  openOverlay(sheetShell("添加内容", "所有输入都从一个框进入", body));
}

function openWhyRoute() {
  const msg = activeChat()?.messages.find((m) => m.role === "assistant" && m.status !== "thinking");
  const v = msg ? currentVersion(msg) : {};
  const body = `<div class="why-list">
    <div class="why-item"><span class="why-k">问题类型</span><span>${v.search ? "需要最新信息，自动联网" : "不依赖实时信息，直接回答"}</span></div>
    <div class="why-item"><span class="why-k">思考深度</span><span>${v.reasoning ? "问题需要多步推理，自动深度思考" : "问题较直接，使用快速回答"}</span></div>
    <div class="why-item"><span class="why-k">输出风格</span><span>${state.mode.style === "concise" ? "简洁" : state.mode.style === "detailed" ? "详细" : "标准"}</span></div>
  </div>
  <div class="sheet-note">这套判断只影响本次回答。你可以随时在“对话能力”里手动接管。</div>
  <div class="sheet-actions"><button class="btn block" data-action="open-mode">${icon("settings")}打开对话能力</button></div>`;
  openOverlay(sheetShell("为什么这样回答", "自动路由不是黑盒", body));
}

function openProcess(msgId) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === msgId);
  const s = msg ? currentVersion(msg).search : null;
  if (!s) return;
  const body = `<div class="process-block">
    <div class="process-label">搜索词</div>
    <div class="process-queries">${s.queries.map((q) => `<span class="process-query">${esc(q)}</span>`).join("")}</div>
    <div class="process-label">阅读来源</div>
    <div class="source-list">${s.sources.map((src, i) => `<button class="source" data-action="open-source" data-id="${msgId}" data-n="${i + 1}">
      <span class="source-icon">${esc((src.site || "?").slice(0, 2))}</span>
      <span class="source-main"><span class="source-title">${esc(src.title)}</span><span class="source-meta">${esc(src.site)} · ${esc(src.date)}</span></span>
    </button>`).join("")}</div>
  </div>
  <div class="sheet-note">过程只展示“搜了什么、看了什么”，不展示模型的完整内部推理。</div>`;
  openOverlay(sheetShell("搜索过程", `${s.queries?.length || 1} 个关键词 · ${s.readCount || 8} 个来源`, body));
}

function openSources(msgId) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === msgId);
  const s = msg ? currentVersion(msg).search : null;
  if (!s) return;
  const body = `<div class="source-list">${s.sources.map((src, i) => `<button class="source" data-action="open-source" data-id="${msgId}" data-n="${i + 1}">
    <span class="source-icon">${esc((src.site || "?").slice(0, 2))}</span>
    <span class="source-main"><span class="source-title">${esc(src.title)}</span><span class="source-meta">${esc(src.site)} · ${esc(src.date)}</span></span>
    <span class="source-num">${i + 1}</span>
  </button>`).join("")}</div>`;
  openOverlay(sheetShell(`来源 · ${s.sources.length}`, "按相关度与权威性排序", body));
}

function openSource(msgId, n) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === msgId);
  const s = msg ? currentVersion(msg).search : null;
  const src = s?.sources?.[Number(n) - 1];
  if (!src) return;
  const body = `<div class="source-detail">
    <div class="source-detail-domain"><span class="source-icon">${esc((src.site || "?").slice(0, 2))}</span><span>${esc(src.site)} · ${esc(src.date)}</span></div>
    <h3>${esc(src.title)}</h3>
    <div class="source-detail-meta">${esc(src.domain || "")}${src.local ? " · 本地文件" : ""}</div>
    <p>${esc(src.snippet || "该来源的摘要暂不可用。")}</p>
    <div class="sheet-actions">
      <button class="btn primary block" data-action="open-url" data-url="${esc(src.url || "#")}">${icon("external")}${src.local ? "打开文件" : "打开原网页"}</button>
    </div>
  </div>`;
  openOverlay(sheetShell("来源详情", `第 ${n} 条`, body));
}

function openMemoryUsed(msgId) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === msgId);
  const mem = msg ? currentVersion(msg).memory : [];
  if (!mem?.length) return;
  const body = `<div class="memory-used-list">${mem.map((m) => `<div class="memory-used"><span class="mu-icon">${icon("sparkle", "icon-sm")}</span><span>${esc(m)}</span></div>`).join("")}</div>
    <div class="sheet-note">这些内容来自你的记忆。你可以在「我的 → 记忆」里查看、纠正或删除。</div>
    <div class="sheet-actions"><button class="btn ghost block" data-action="open-memory">${icon("settings")}管理记忆</button></div>`;
  openOverlay(sheetShell("本次使用的记忆", `${mem.length} 条`, body));
}

function openShare(msgId) {
  const chat = activeChat();
  const msg = chat?.messages.find((m) => m.id === msgId);
  if (!msg) return;
  const v = currentVersion(msg);
  const idx = chat.messages.indexOf(msg);
  const q = chat.messages[idx - 1]?.text || chat.title;
  const body = `<div class="share-preview">
    <div class="sp-brand"><span class="msg-avatar">DS</span> DeepSeek · 对话分享</div>
    <div class="sp-q">${esc(q)}</div>
    <div class="sp-a">${esc(plainText(v.text).slice(0, 120))}…</div>
    <div class="sp-foot"><span>deepseek.com/share/8f2c</span><span>2026-11-03</span></div>
  </div>
  <div class="share-options">
    <button class="btn block" data-action="copy-link">${icon("copy")}复制链接</button>
    <button class="btn block" data-action="share-image">${icon("image")}生成长图</button>
    <button class="btn block" data-action="share-pdf">${icon("file")}导出 PDF</button>
  </div>
  <div class="sheet-note">分享内容不包含记忆来源与附件本地路径。锁定对话不可分享。</div>`;
  openOverlay(sheetShell("分享对话", "链接获得者可查看此对话", body));
}

function openChatMenu() {
  const chat = activeChat();
  if (!chat) return;
  const body = `<div class="menu-group" style="margin-top:0">
    <button class="menu-row" data-action="rename-chat">${icon("edit", "icon-sm")}<span class="m-main"><span class="m-title">重命名对话</span></span>${icon("chevron-right", "m-icon chev")}</button>
    <button class="menu-row" data-action="share-last">${icon("share", "icon-sm")}<span class="m-main"><span class="m-title">分享对话</span></span>${icon("chevron-right", "m-icon chev")}</button>
    <button class="menu-row" data-action="archive-chat">${icon("folder", "icon-sm")}<span class="m-main"><span class="m-title">归档对话</span></span>${icon("chevron-right", "m-icon chev")}</button>
    <button class="menu-row danger" data-action="delete-chat">${icon("trash", "icon-sm")}<span class="m-main"><span class="m-title">删除对话</span></span></button>
  </div>`;
  openOverlay(sheetShell("对话设置", "", body));
}

function openDrawer() {
  const items = state.chats
    .filter((c) => c.messages.length)
    .map((c) => `<button class="drawer-chat" data-action="open-chat" data-id="${c.id}">${esc(c.title)}</button>`)
    .join("");
  const html = `<div class="drawer open">
    <div class="backdrop" data-action="close-drawer"></div>
    <div class="drawer-panel">
      <div class="drawer-head">
        <div class="drawer-brand"><span class="rail-mark">DS</span> DEEPSEEK</div>
        <button class="icon-btn" data-action="close-drawer" aria-label="关闭">${icon("close")}</button>
      </div>
      <div class="drawer-search">${icon("search")}<input id="drawer-search" placeholder="搜索对话…" autocomplete="off"></div>
      <button class="drawer-new" data-action="new-chat">${icon("plus")}新对话</button>
      <div class="drawer-label">全部对话</div>
      <div class="drawer-list" id="drawer-list">${items || `<div class="empty" style="padding:24px 8px"><div class="e-sub">还没有对话</div></div>`}</div>
      <div class="drawer-links">
        <button class="drawer-link" data-action="tab" data-tab="projects">${icon("folder")}项目</button>
        <button class="drawer-link" data-action="tab" data-tab="tasks">${icon("clock")}任务</button>
        <button class="drawer-link" data-action="open-memory">${icon("sparkle")}记忆</button>
        <button class="drawer-link" data-action="open-settings">${icon("settings")}设置</button>
      </div>
    </div>
  </div>`;
  openOverlay(html);
  const input = $("#drawer-search");
  if (input) {
    input.addEventListener("input", () => {
      const q = input.value.trim().toLowerCase();
      $$("#drawer-list .drawer-chat").forEach((b) => {
        b.style.display = b.textContent.toLowerCase().includes(q) ? "block" : "none";
      });
    });
  }
}

/* ------------------------------ projects ------------------------------ */

function projectsScreen() {
  return `<div class="screen">
    ${topbarPage("项目", `<button class="icon-btn" data-action="new-project" aria-label="新建项目">${icon("plus")}</button>`)}
    <main class="scroll page-scroll">
      <div class="page-head">
        <div><h1 class="page-title">你的项目</h1><p class="page-sub">把资料、指令和对话放在同一个上下文里。</p></div>
      </div>
      <div class="project-grid">
        ${state.projects
          .map(
            (p) => `<button class="project-card" data-action="open-project" data-id="${p.id}">
              <span class="project-icon">${icon(p.icon || "folder")}</span>
              <span class="project-main">
                <span class="project-name">${esc(p.name)}</span>
                <span class="project-desc">${esc(p.desc)}</span>
                <span class="project-meta"><span>${icon("file")}${p.sources.length} 份资料</span><span>${icon("sparkle")}${p.chatIds.length} 个对话</span><span>${esc(p.updated)}</span></span>
              </span>
            </button>`
          )
          .join("")}
      </div>
    </main>
    ${tabbar("projects")}
  </div>`;
}

function projectScreen() {
  const p = activeProject();
  if (!p) return projectsScreen();
  const chats = state.chats.filter((c) => c.projectId === p.id);
  return `<div class="screen">
    ${topbarBack("项目", `<button class="icon-btn" data-action="project-menu" aria-label="更多">${icon("more")}</button>`)}
    <main class="scroll">
      <div class="project-hero">
        <div class="p-kicker">PROJECT</div>
        <h1>${esc(p.name)}</h1>
        <p class="p-desc">${esc(p.desc)}</p>
      </div>
      <div class="instruction-card">
        <div class="ic-head">${icon("sparkle", "icon-xs")}项目指令</div>
        <div class="ic-body">${esc(p.instructions)}</div>
      </div>
      <div class="section-label" style="margin:0 16px 7px"><span>资料 · ${p.sources.length}</span><button data-action="add-source">添加</button></div>
      <div class="source-strip">
        ${p.sources
          .map(
            (s) => `<button class="source-tile" data-action="open-project-source" data-name="${esc(s.name)}">
              <span class="st-icon">${icon(s.name.endsWith(".xlsx") ? "layers" : s.name.endsWith(".pdf") ? "file" : "edit")}</span>
              <span class="st-name">${esc(s.name)}</span>
              <span class="st-meta">${esc(s.meta)}</span>
            </button>`
          )
          .join("")}
        <button class="source-tile add" data-action="add-source">${icon("plus")}<span>添加资料</span></button>
      </div>
      <div class="section-label" style="margin:18px 16px 4px"><span>对话 · ${chats.length}</span></div>
      <div class="project-chat-list">
        ${chats.length
          ? chats
              .map(
                (c) => `<button class="pc-row" data-action="open-chat" data-id="${c.id}">
                  <span class="pc-icon">${icon("sparkle")}</span>
                  <span class="pc-main"><span class="pc-title">${esc(c.title)}</span><span class="pc-meta">${esc(c.meta || "项目上下文")}</span></span>
                  ${icon("chevron-right", "pc-chev icon-xs")}
                </button>`
              )
              .join("")
          : `<div class="empty" style="padding:24px 8px"><div class="e-sub">这个项目还没有对话。从下面的输入框开始。</div></div>`}
      </div>
    </main>
    ${composer("project")}
  </div>`;
}

function openNewProject() {
  const body = `<label class="field"><span>项目名称</span><input id="project-name" placeholder="例如：毕业论文" autocomplete="off"></label>
    <label class="field"><span>项目指令（可选）</span><textarea id="project-instr" placeholder="例如：先指出逻辑漏洞，再给修改建议。" rows="3"></textarea></label>
    <div class="sheet-note">项目里的对话会默认带上这份资料与指令；你也可以在创建后修改。</div>
    <div class="sheet-actions"><button class="btn primary block" data-action="create-project">${icon("plus")}创建项目</button></div>`;
  openOverlay(sheetShell("新建项目", "一个上下文容器，不是文件夹", body));
}

function createProject() {
  const name = $("#project-name")?.value.trim();
  if (!name) return toast("先给项目起个名字");
  const instr = $("#project-instr")?.value.trim() || "回答时优先引用项目资料，并标出来源。";
  state.projects.unshift({ id: uid("p"), name, icon: "folder", desc: "新建的项目", instructions: instr, sources: [], chatIds: [], updated: "刚刚" });
  saveState();
  closeOverlay();
  render();
  toast("项目已创建");
}

function openAddSource() {
  const body = `<div class="tool-grid" style="grid-template-columns:1fr 1fr">
    <button class="tool" data-action="add-source-mock">${icon("file")}<span class="t-name">上传文件</span><span class="t-sub">PDF / Office</span></button>
    <button class="tool" data-action="add-source-mock">${icon("globe")}<span class="t-name">粘贴链接</span><span class="t-sub">网页 / 文档</span></button>
  </div><div class="sheet-note">原型演示：添加后会出现在项目资料区，并在项目对话中被引用。</div>`;
  openOverlay(sheetShell("添加资料", "让项目拥有长期上下文", body));
}

/* ------------------------------ tasks ------------------------------ */

function tasksScreen() {
  const active = state.tasks.filter((t) => t.status === "active");
  const paused = state.tasks.filter((t) => t.status === "paused");
  const done = state.tasks.filter((t) => t.status === "done");
  return `<div class="screen">
    ${topbarPage("任务", `<button class="icon-btn" data-action="new-task" aria-label="新建任务">${icon("plus")}</button>`)}
    <main class="scroll page-scroll">
      <div class="page-head"><div><h1 class="page-title">定时任务</h1><p class="page-sub">用一句话创建，到点自动执行。</p></div></div>
      ${active.length ? `<div class="section-label" style="margin-top:0"><span>进行中 · ${active.length}</span></div><div class="task-list">${active.map(taskCard).join("")}</div>` : ""}
      ${paused.length ? `<div class="section-label"><span>已暂停</span></div><div class="task-list">${paused.map(taskCard).join("")}</div>` : ""}
      ${done.length ? `<div class="section-label"><span>已完成</span></div><div class="task-list">${done.map(taskCard).join("")}</div>` : ""}
      ${!state.tasks.length ? `<div class="empty"><div class="e-icon">${icon("clock")}</div><div class="e-title">还没有任务</div><div class="e-sub">试试说“每天早上 8 点给我一份 AI 行业简报”。</div></div>` : ""}
    </main>
    ${tabbar("tasks")}
  </div>`;
}

function taskCard(t) {
  return `<div class="task-card ${t.status === "done" ? "task-done" : ""}" data-action="open-task" data-id="${t.id}" tabindex="0" role="button">
    <div class="task-top">
      <span class="task-icon">${icon(t.icon || "clock")}</span>
      <span class="task-main"><span class="task-name">${esc(t.title)}</span><span class="task-prompt">${esc(t.prompt)}</span></span>
      <label class="switch" aria-label="暂停或启用任务"><input type="checkbox" ${t.status === "active" ? "checked" : ""} data-task-toggle="${t.id}"><span></span></label>
    </div>
    <div class="task-meta">${icon("clock")}${esc(t.schedule)}<span class="sep">·</span>下次 ${esc(t.next)}<span class="sep">·</span>${esc(t.last)}</div>
  </div>`;
}

function taskScreen() {
  const t = findTask(state.activeTaskId);
  if (!t) return tasksScreen();
  return `<div class="screen">
    ${topbarBack("任务", `<button class="icon-btn" data-action="task-more" aria-label="更多">${icon("more")}</button>`)}
    <main class="scroll page-scroll">
      <div class="task-detail-head">
        <span class="task-icon" style="width:42px;height:42px;border-radius:12px">${icon(t.icon || "clock")}</span>
        <div><h1 class="page-title" style="font-size:22px">${esc(t.title)}</h1><p class="page-sub">${t.status === "active" ? "进行中" : t.status === "paused" ? "已暂停" : "已完成"} · ${esc(t.timezone)}</p></div>
      </div>
      <div class="detail-block"><div class="db-label">执行内容</div><div class="db-body">${esc(t.prompt)}</div></div>
      <div class="detail-block"><div class="db-label">计划</div><div class="db-body">${esc(t.schedule)}<br><span style="color:var(--text-3);font-size:11px">下次：${esc(t.next)}</span></div></div>
      <div class="detail-block"><div class="db-label">运行历史</div>
        ${t.runs?.length ? t.runs.map((r) => `<div class="run-row"><span class="run-dot done">${icon("check", "icon-xs")}</span><span><b>${esc(r.at)}</b><span>${esc(r.result)}</span></span></div>`).join("") : `<div class="db-body" style="color:var(--text-4)">还没有运行记录</div>`}
      </div>
      <div class="detail-actions">
        <button class="btn primary block" data-action="run-task" data-id="${t.id}">${icon("play")}立即运行</button>
        <button class="btn block" data-action="task-toggle-btn" data-id="${t.id}">${t.status === "active" ? "暂停任务" : "启用任务"}</button>
      </div>
    </main>
  </div>`;
}

function openNewTask() {
  const body = `<label class="field"><span>你想让它什么时候做什么？</span><textarea id="task-input" rows="3" placeholder="例如：每天早上 8 点给我一份 AI 行业简报，重点看模型发布和融资。"></textarea></label>
    <div class="research-chips">
      <button class="research-chip" data-action="task-example" data-text="每天早上 8 点给我一份 AI 行业简报，重点看模型发布和融资">每天 8 点 · AI 简报</button>
      <button class="research-chip" data-action="task-example" data-text="每周一检查三家竞品的官网定价与套餐变化，输出差异表">每周一 · 竞品价格</button>
    </div>
    <div class="sheet-note">自然语言创建；下一步会生成一张确认卡，确认后才会开始计时。</div>
    <div class="sheet-actions"><button class="btn primary block" data-action="parse-task">${icon("sparkle")}解析任务</button></div>`;
  openOverlay(sheetShell("新建任务", "一句话，变成可执行的计划", body));
}

function parseTask() {
  const text = $("#task-input")?.value.trim();
  if (!text) return toast("先描述一下你想让它做什么");
  const daily = /每天|每日/.test(text);
  const weekly = /每周|周一|周报/.test(text);
  const time = text.match(/(上午|下午|晚上|中午)?\s*(\d{1,2})\s*[:：点]\s*(\d{0,2})/);
  let hour = time ? Number(time[2]) : 8;
  const period = time?.[1] || "";
  if ((period === "下午" || period === "晚上") && hour < 12) hour += 12;
  if (!period && hour < 7) hour += 12;
  const hh = String(hour).padStart(2, "0");
  const mm = time && time[3] ? String(time[3]).padStart(2, "0") : "00";
  let title = "新任务";
  if (/早报|简报|日报/.test(text)) title = "AI 行业早报";
  else if (/会议纪要|纪要|整理/.test(text)) title = "会议纪要整理";
  else if (/监控|竞品|价格/.test(text)) title = "竞品价格监控";
  else if (/周报/.test(text)) title = "周报提醒";
  else title = text.slice(0, 14);
  state.taskDraft = {
    title,
    prompt: text,
    schedule: daily ? `每天 ${hh}:${mm}` : weekly ? `每周一 ${hh}:${mm}` : `每天 ${hh}:${mm}`,
    next: daily ? `明天 ${hh}:${mm}` : `周一 ${hh}:${mm}`,
  };
  openTaskConfirm();
}

function openTaskConfirm() {
  const d = state.taskDraft;
  if (!d) return;
  const body = `<div class="confirm-card">
    <div class="confirm-row"><span>名称</span><b>${esc(d.title)}</b></div>
    <div class="confirm-row"><span>时间</span><b>${esc(d.schedule)}（Asia/Shanghai）</b></div>
    <div class="confirm-row"><span>内容</span><b>${esc(d.prompt)}</b></div>
    <div class="confirm-row"><span>通知</span><b>推送 + 写入对话</b></div>
    <div class="confirm-row"><span>下次</span><b>${esc(d.next)}</b></div>
  </div>
  <div class="sheet-note">创建后可以在「任务」里暂停、编辑或删除。连续失败 3 次会自动暂停。</div>
  <div class="sheet-actions"><button class="btn ghost" data-action="new-task">返回修改</button><button class="btn primary" data-action="create-task">创建任务</button></div>`;
  openOverlay(sheetShell("确认任务", "创建前最后看一眼", body));
}

function createTask() {
  const d = state.taskDraft;
  if (!d) return;
  state.tasks.unshift({ id: uid("t"), ...d, timezone: "Asia/Shanghai", status: "active", last: "尚未运行", icon: "clock", runs: [] });
  state.taskDraft = null;
  saveState();
  closeOverlay();
  render();
  toast("任务已创建");
}

function runTask(id) {
  const t = findTask(id);
  if (!t) return;
  const body = `<div class="run-progress">
    <div class="run-spinner">${icon("sparkle")}</div>
    <div class="run-title">正在执行「${esc(t.title)}」</div>
    <div class="run-sub">搜索 → 阅读 → 整理结果</div>
    <div class="research-progress-bar"><i style="width:40%"></i></div>
  </div>`;
  openOverlay(sheetShell("运行任务", "可以离开，完成后会通知你", body));
  setTimeout(() => {
    const result = "已生成 12 条更新，其中 3 条值得跟进。";
    t.last = "刚刚 · " + result;
    t.runs = [{ at: "刚刚", status: "done", result }, ...(t.runs || [])];
    saveState();
    const sheet = $("#overlay .sheet");
    if (sheet) {
      sheet.innerHTML = `<div class="sheet-grabber"></div><div class="sheet-head"><div><div class="sheet-title">运行完成</div><div class="sheet-sub">结果已写入任务对话</div></div><button class="sheet-close" data-action="close-sheet">${icon("close")}</button></div>
        <div class="confirm-card"><div class="confirm-row"><span>状态</span><b style="color:var(--success)">成功</b></div><div class="confirm-row"><span>结果</span><b>${esc(result)}</b></div></div>
        <div class="sheet-actions"><button class="btn primary block" data-action="close-sheet">查看任务</button></div>`;
    }
    render();
  }, 1800);
}

/* ------------------------------ memory ------------------------------ */

function memoryScreen() {
  const list = state.memories;
  return `<div class="screen">
    ${topbarBack("记忆", `<button class="icon-btn" data-action="add-memory" aria-label="添加记忆">${icon("plus")}</button>`)}
    <main class="scroll page-scroll">
      <div class="page-head"><div><h1 class="page-title">关于你</h1><p class="page-sub">它记得什么、从哪来、怎么忘，都由你决定。</p></div></div>
      <div class="settings-group">
        <div class="menu-group">
          <div class="menu-row">
            <span class="m-icon">${icon("sparkle")}</span>
            <span class="m-main"><span class="m-title">允许使用记忆</span><span class="m-sub">关闭后不读取，也不写入新记忆</span></span>
            <label class="switch"><input type="checkbox" ${state.settings.memory ? "checked" : ""} data-setting="memory"><span></span></label>
          </div>
        </div>
      </div>
      ${state.settings.memory
        ? `<div class="memory-list">${list
            .map(
              (m) => `<div class="memory-card">
                <div class="memory-top"><span class="memory-type">${esc(m.label)}</span><span class="memory-actions">
                  <button class="action" data-action="edit-memory" data-id="${m.id}" title="编辑">${icon("edit")}</button>
                  <button class="action" data-action="delete-memory" data-id="${m.id}" title="删除">${icon("trash")}</button>
                </span></div>
                <div class="memory-text">${esc(m.text)}</div>
                <div class="memory-src">${icon("history")}${esc(m.source)}</div>
              </div>`
            )
            .join("")}</div>`
        : `<div class="empty"><div class="e-icon">${icon("lock")}</div><div class="e-title">记忆已关闭</div><div class="e-sub">当前对话不会读取或写入记忆。你随时可以在上面重新打开。</div></div>`}
    </main>
  </div>`;
}

function openMemorySheet(id) {
  const m = id ? state.memories.find((x) => x.id === id) : null;
  const body = `<label class="field"><span>内容</span><textarea id="memory-text" rows="3" placeholder="例如：先给结论，再给理由。">${m ? esc(m.text) : ""}</textarea></label>
    <label class="field"><span>类型</span><select id="memory-type"><option value="preference" ${m?.type === "preference" ? "selected" : ""}>偏好</option><option value="identity" ${m?.type === "identity" ? "selected" : ""}>身份</option><option value="project" ${m?.type === "project" ? "selected" : ""}>项目</option><option value="relationship" ${m?.type === "relationship" ? "selected" : ""}>关系</option></select></label>
    <div class="sheet-note">不要在这里记录密码、证件、银行卡或他人的隐私。</div>
    <div class="sheet-actions"><button class="btn primary block" data-action="save-memory" data-id="${id || ""}">${m ? "保存修改" : "添加记忆"}</button></div>`;
  openOverlay(sheetShell(m ? "编辑记忆" : "添加记忆", "记忆是可查看、可纠正的", body));
}

/* ------------------------------ me / settings ------------------------------ */

function meScreen() {
  return `<div class="screen">
    ${topbarPage("我的", `<button class="icon-btn" data-action="open-settings" aria-label="设置">${icon("settings")}</button>`)}
    <main class="scroll page-scroll">
      <div class="profile-card">
        <div class="profile-top">
          <div class="profile-avatar">C</div>
          <div><div class="profile-name">Cris</div><div class="profile-meta">Free · 已使用 12 / 50 次对话</div></div>
        </div>
        <div class="usage-card">
          <div class="usage-top"><span>今日用量</span><span>12 / 50</span></div>
          <div class="usage-bar"><i></i></div>
        </div>
      </div>
      <div class="menu-group">
        <button class="menu-row" data-action="open-memory"><span class="m-icon">${icon("sparkle")}</span><span class="m-main"><span class="m-title">记忆</span><span class="m-sub">它记得什么，由你决定</span></span><span class="m-value">${state.memories.length} 条</span>${icon("chevron-right", "chev")}</button>
        <button class="menu-row" data-action="tab" data-tab="projects"><span class="m-icon">${icon("folder")}</span><span class="m-main"><span class="m-title">项目</span><span class="m-sub">资料、指令与对话</span></span><span class="m-value">${state.projects.length} 个</span>${icon("chevron-right", "chev")}</button>
        <button class="menu-row" data-action="tab" data-tab="tasks"><span class="m-icon">${icon("clock")}</span><span class="m-main"><span class="m-title">任务</span><span class="m-sub">到点自动执行</span></span><span class="m-value">${state.tasks.filter((t) => t.status === "active").length} 个进行中</span>${icon("chevron-right", "chev")}</button>
      </div>
      <div class="menu-group">
        <button class="menu-row" data-action="open-settings"><span class="m-icon">${icon("settings")}</span><span class="m-main"><span class="m-title">设置</span><span class="m-sub">主题、语音、通知与数据</span></span>${icon("chevron-right", "chev")}</button>
        <button class="menu-row" data-action="about"><span class="m-icon">${icon("book")}</span><span class="m-main"><span class="m-title">帮助与关于</span><span class="m-sub">版本 0.1 · 原型演示</span></span>${icon("chevron-right", "chev")}</button>
      </div>
      <div class="rail-version" style="margin-top:22px;text-align:center">DeepSeek Android 原型 · v0.1</div>
    </main>
    ${tabbar("me")}
  </div>`;
}

function settingsScreen() {
  const seg = (action, key, options, current) => `<div class="seg-mini">${options.map((o) => `<button class="${current === o.v ? "active" : ""}" data-action="${action}" data-value="${o.v}">${o.l}</button>`).join("")}</div>`;
  return `<div class="screen">
    ${topbarBack("设置")}
    <main class="scroll page-scroll">
      <div class="settings-group">
        <div class="settings-label">通用</div>
        <div class="menu-group">
          <div class="menu-row"><span class="m-icon">${icon("moon")}</span><span class="m-main"><span class="m-title">主题</span></span>${seg("set-theme", "theme", [{ v: "dark", l: "深色" }, { v: "light", l: "浅色" }], state.theme)}</div>
          <div class="menu-row"><span class="m-icon">${icon("book")}</span><span class="m-main"><span class="m-title">字号</span></span>${seg("set-font", "font", [{ v: "0.92", l: "小" }, { v: "1", l: "标准" }, { v: "1.08", l: "大" }], String(state.settings.fontScale))}</div>
          <div class="menu-row"><span class="m-icon">${icon("wave")}</span><span class="m-main"><span class="m-title">触感反馈</span><span class="m-sub">发送、完成时轻震</span></span><label class="switch"><input type="checkbox" ${state.settings.haptics ? "checked" : ""} data-setting="haptics"><span></span></label></div>
        </div>
      </div>
      <div class="settings-group">
        <div class="settings-label">对话</div>
        <div class="menu-group">
          <div class="menu-row"><span class="m-icon">${icon("globe")}</span><span class="m-main"><span class="m-title">自动联网</span><span class="m-sub">时间敏感的问题自动搜索</span></span><label class="switch"><input type="checkbox" ${state.settings.autoWeb ? "checked" : ""} data-setting="autoWeb"><span></span></label></div>
          <div class="menu-row"><span class="m-icon">${icon("sparkle")}</span><span class="m-main"><span class="m-title">记忆</span><span class="m-sub">跨对话记住偏好与项目</span></span><label class="switch"><input type="checkbox" ${state.settings.memory ? "checked" : ""} data-setting="memory"><span></span></label></div>
          <button class="menu-row" data-action="open-mode"><span class="m-icon">${icon("settings")}</span><span class="m-main"><span class="m-title">默认思考深度</span></span><span class="m-value">${state.mode.thinking === "auto" ? "自动" : state.mode.thinking === "deep" ? "深入" : "快速"}</span>${icon("chevron-right", "chev")}</button>
          <button class="menu-row" data-action="custom-instructions"><span class="m-icon">${icon("edit")}</span><span class="m-main"><span class="m-title">自定义指令</span><span class="m-sub">全局的回答风格</span></span>${icon("chevron-right", "chev")}</button>
        </div>
      </div>
      <div class="settings-group">
        <div class="settings-label">语音</div>
        <div class="menu-group">
          <button class="menu-row" data-action="voice-settings"><span class="m-icon">${icon("mic")}</span><span class="m-main"><span class="m-title">音色</span></span><span class="m-value">白浪</span>${icon("chevron-right", "chev")}</button>
          <div class="menu-row"><span class="m-icon">${icon("phone")}</span><span class="m-main"><span class="m-title">后台通话</span><span class="m-sub">锁屏后继续</span></span><label class="switch"><input type="checkbox" ${state.settings.voiceAutoStart ? "checked" : ""} data-setting="voiceAutoStart"><span></span></label></div>
        </div>
      </div>
      <div class="settings-group">
        <div class="settings-label">数据</div>
        <div class="menu-group">
          <button class="menu-row" data-action="clear-cache"><span class="m-icon">${icon("trash")}</span><span class="m-main"><span class="m-title">清除缓存</span><span class="m-sub">不会删除对话</span></span></button>
          <button class="menu-row danger" data-action="reset-demo"><span class="m-icon">${icon("refresh")}</span><span class="m-main"><span class="m-title">重置演示数据</span><span class="m-sub">恢复初始对话与设置</span></span></button>
        </div>
      </div>
      <div class="settings-group">
        <div class="settings-label">关于</div>
        <div class="menu-group">
          <div class="menu-row"><span class="m-icon">${icon("book")}</span><span class="m-main"><span class="m-title">版本</span></span><span class="m-value">0.1.0 · prototype</span></div>
          <button class="menu-row" data-action="privacy"><span class="m-icon">${icon("lock")}</span><span class="m-main"><span class="m-title">隐私政策</span></span>${icon("chevron-right", "chev")}</button>
        </div>
      </div>
      <div class="rail-version" style="text-align:center;margin-top:4px">DeepSeek Android 原型 · 模拟数据</div>
    </main>
  </div>`;
}

/* ------------------------------ research ------------------------------ */

const REPORT_MD = `## 摘要

2026 年的 AI 基础设施竞争，已经从“谁更便宜”转向“谁能在便宜的同时保证稳定供给”。对开发者来说，选型的关键指标正在从 token 单价，变成任务成功率、限流策略与工具调用能力。

## 1. 市场格局

头部云厂商继续扩建推理集群，标准档价格继续下探；中小厂商则转向垂直场景与私有化部署。[1]

## 2. 价格结构

- 输入价格下降快于输出价格，缓存命中成为新的成本杠杆。
- 低价档的限流更常见，稳定低延迟开始溢价。[2]

## 3. 能力对比

长上下文、工具调用与多模态成为默认项。真正拉开差距的是“多步任务的成功率”，而不是单轮跑分。[3]

## 4. 选型建议

1. 先用真实任务做一周 A/B，不要只看 benchmark。
2. 把“失败重试成本”算进总成本。
3. 对关键链路保留第二供应商。

## 局限性

公开数据主要来自厂商自述与第三方评测，样本对中小开发者不完全代表性。`;

function researchScreen() {
  const r = state.research || { step: "input", topic: "", progress: 0 };
  if (r.step === "plan") return researchPlanScreen(r);
  if (r.step === "running") return researchRunningScreen(r);
  if (r.step === "report") return researchReportScreen(r);
  return `<div class="screen">
    ${topbarBack("深度研究", `<button class="icon-btn" data-action="back" aria-label="关闭">${icon("close")}</button>`)}
    <main class="scroll research-scroll">
      <div class="research-hero">
        <div class="research-kicker">${icon("layers", "icon-xs")}DEEP RESEARCH</div>
        <h1>把一个问题<br>查成一份报告。</h1>
        <p>先给计划，再开始执行。研究过程可见、可打断，完成后给你一份带来源的报告。</p>
      </div>
      <textarea class="research-input" id="research-topic" placeholder="例如：2026 年国产大模型 API 的价格与能力对比，重点看长上下文、工具调用和稳定性。">${esc(r.topic || "")}</textarea>
      <div class="research-chips">
        <button class="research-chip" data-action="research-example" data-text="2026 年国产大模型 API 的价格与能力对比，重点看长上下文、工具调用和稳定性">国产大模型 API 对比</button>
        <button class="research-chip" data-action="research-example" data-text="多模态 Agent 的可观测性：现有评估方法与缺口">多模态 Agent 综述</button>
        <button class="research-chip" data-action="research-example" data-text="AI 硬件的麦克风阵列选型：成本、功耗与降噪效果">麦克风阵列选型</button>
      </div>
      <div class="sheet-note" style="margin-top:12px">预计 8-15 分钟 · 最多 30 个来源 · 同一时间只跑 1 个研究</div>
      <div class="sheet-actions"><button class="btn primary block" data-action="research-plan">${icon("sparkle")}生成研究计划</button></div>
    </main>
  </div>`;
}

function researchPlanScreen(r) {
  return `<div class="screen">
    ${topbarBack("研究计划", `<button class="icon-btn" data-action="research-adjust" aria-label="调整">${icon("edit")}</button>`)}
    <main class="scroll research-scroll">
      <div class="research-kicker">${icon("layers", "icon-xs")}PLAN</div>
      <div class="plan-card">
        <div class="plan-head"><div class="pl-label">研究主题</div><div class="pl-title">${esc(r.topic)}</div></div>
        <div class="plan-body">
          <div class="plan-step"><span class="ps-n">01</span><span>市场格局与主要玩家</span></div>
          <div class="plan-step"><span class="ps-n">02</span><span>价格结构：输入 / 输出 / 缓存</span></div>
          <div class="plan-step"><span class="ps-n">03</span><span>能力对比：上下文、工具、多模态</span></div>
          <div class="plan-step"><span class="ps-n">04</span><span>适用场景与选型建议</span></div>
          <div class="plan-meta"><span>${icon("globe", "icon-xs")}公开网络</span><span>${icon("clock", "icon-xs")}8-12 分钟</span><span>${icon("file", "icon-xs")}约 25 个来源</span></div>
        </div>
        <div class="plan-actions"><button class="btn ghost" data-action="research-adjust">调整计划</button><button class="btn primary" data-action="research-begin">${icon("play")}开始研究</button></div>
      </div>
      <div class="sheet-note" style="margin-top:12px">开始后可以离开页面。完成后会推送通知，结果写回当前对话。</div>
    </main>
  </div>`;
}

function researchRunningScreen(r) {
  const steps = ["拆解问题", "搜索 24 个来源", "阅读与交叉验证", "撰写报告"];
  const progress = r.progress || 0;
  return `<div class="screen">
    ${topbarBack("深度研究", `<span class="rail-version">${progress}/4</span>`)}
    <main class="scroll research-scroll">
      <div class="research-kicker">${icon("sparkle", "icon-xs")}RUNNING</div>
      <h1 class="page-title" style="font-size:22px;margin-top:8px">${esc(r.topic)}</h1>
      <p class="page-sub">研究进行中，可以离开页面。</p>
      <div class="research-progress-bar"><i style="width:${Math.min(100, progress * 25)}%"></i></div>
      <div class="progress-list">
        ${steps.map((s, i) => `<div class="progress-step ${i < progress ? "done" : i === progress ? "active" : ""}"><span class="ps-dot">${i < progress ? icon("check", "icon-xs") : ""}</span><span>${s}</span></div>`).join("")}
      </div>
      <div class="sheet-note" style="margin-top:16px">已完成 ${progress}/4 个阶段。预计还需 ${Math.max(1, 4 - progress)} 分钟。</div>
      <div class="sheet-actions"><button class="btn ghost block" data-action="research-cancel">取消研究</button></div>
    </main>
  </div>`;
}

function researchReportScreen(r) {
  return `<div class="screen">
    ${topbarBack("研究报告", `<button class="icon-btn" data-action="research-export" aria-label="导出">${icon("share")}</button>`)}
    <main class="scroll research-scroll">
      <div class="report-head">
        <div class="r-kicker">${icon("layers", "icon-xs")}REPORT · 4 节 · 3 个来源</div>
        <h1>${esc(r.topic || "2026 年国产大模型 API 价格与能力对比")}</h1>
        <div class="report-meta"><span>2026-11-03</span><span>8 分 42 秒</span><span>25 个来源</span></div>
      </div>
      <div class="toc"><div class="toc-title">目录</div><a href="#r1">摘要</a><a href="#r2">1. 市场格局</a><a href="#r3">2. 价格结构</a><a href="#r4">3. 能力对比</a><a href="#r5">4. 选型建议</a><a href="#r6">局限性</a></div>
      <div class="report-body">${mdToHtml(REPORT_MD)}</div>
      <div class="sheet-actions" style="margin-top:18px"><button class="btn primary block" data-action="research-back-chat">${icon("sparkle")}回到对话并保存</button><button class="btn block" data-action="research-export">${icon("file")}导出 PDF</button></div>
    </main>
  </div>`;
}

function clearResearchTimers() {
  (state.researchTimers || []).forEach((t) => clearTimeout(t));
  state.researchTimers = [];
}

function startResearch(topic = "") {
  clearResearchTimers();
  state.research = { step: topic ? "plan" : "input", topic, progress: 0 };
  navigate("research");
}

function makeResearchPlan() {
  const ta = $("#research-topic");
  const topic = ta?.value.trim();
  if (!topic) return toast("先写下你想研究的问题");
  state.research = { step: "plan", topic, progress: 0 };
  render();
}

function beginResearch() {
  clearResearchTimers();
  state.research.step = "running";
  state.research.progress = 0;
  render();
  [1, 2, 3, 4].forEach((n, i) => {
    state.researchTimers.push(
      setTimeout(() => {
        state.research.progress = n;
        if (n === 4) {
          state.researchTimers.push(
            setTimeout(() => {
              state.research.step = "report";
              render();
              toast("报告已生成");
            }, 700)
          );
        } else render();
      }, 1100 * (i + 1))
    );
  });
}

/* ------------------------------ voice ------------------------------ */

function voiceHTML() {
  return `<div class="voice" id="voice-screen" data-state="connecting">
    <div class="voice-head">
      <div><span class="v-title">AI 通话</span><span class="v-timer" id="voice-timer">00:00</span></div>
      <button class="icon-btn" data-action="voice-end" aria-label="结束通话">${icon("close")}</button>
    </div>
    <div class="voice-main">
      <div class="orb-wrap" data-action="voice-orb" role="button" tabindex="0" aria-label="打断或继续">
        <span class="orb-ring r1"></span><span class="orb-ring r2"></span><span class="orb-ring r3"></span>
        <span class="orb"></span>
      </div>
      <div class="voice-status" id="voice-status">正在连接…</div>
      <div class="voice-hint" id="voice-hint">点按光球可以打断</div>
      <div class="voice-wave"><i></i><i></i><i></i><i></i><i></i><i></i></div>
      <div class="voice-transcript" id="voice-transcript"></div>
    </div>
    <div class="voice-controls">
      <div class="v-ctrl"><button class="v-btn" data-action="voice-mute" id="voice-mute">${icon("mic")}</button><span class="v-label">静音</span></div>
      <div class="v-ctrl"><button class="v-btn" data-action="voice-speaker" id="voice-speaker">${icon("speaker")}</button><span class="v-label">扬声器</span></div>
      <div class="v-ctrl"><button class="v-btn end" data-action="voice-end" id="voice-end">${icon("phone")}</button><span class="v-label">挂断</span></div>
      <div class="v-ctrl"><button class="v-btn" data-action="voice-camera">${icon("camera")}</button><span class="v-label">相机</span></div>
    </div>
  </div>`;
}

function startVoice() {
  clearVoiceTimers();
  state.voice = { state: "connecting", seconds: 0, lines: [], muted: false, speaking: false, typeTimer: null };
  openOverlay(voiceHTML());
  $("#toast")?.classList.add("on-voice");
  const screen = $("#voice-screen");
  if (screen) screen.dataset.state = "connecting";
  state.voice.clock = setInterval(() => {
    if (!state.voice) return;
    state.voice.seconds++;
    const t = $("#voice-timer");
    if (t) t.textContent = fmtDuration(state.voice.seconds);
  }, 1000);

  addVoiceTimer(() => setVoiceState("listening"), 700);
  addVoiceTimer(() => addVoiceLine("user", "帮我对比一下这三款耳机的降噪"), 2300);
  addVoiceTimer(() => setVoiceState("thinking"), 3300);
  addVoiceTimer(() => {
    setVoiceState("speaking");
    typeVoiceLine("ai", "好的，先说结论：通勤选 A，飞行选 B。");
  }, 4600);
}

function fmtDuration(sec) {
  return `${String(Math.floor(sec / 60)).padStart(2, "0")}:${String(sec % 60).padStart(2, "0")}`;
}

function addVoiceTimer(fn, ms) {
  state.voiceTimers.push(setTimeout(fn, ms));
}

function clearVoiceTimers() {
  (state.voiceTimers || []).forEach((t) => clearTimeout(t));
  state.voiceTimers = [];
  if (state.voice?.clock) clearInterval(state.voice.clock);
  if (state.voice?.typeTimer) clearInterval(state.voice.typeTimer);
}

function setVoiceState(s) {
  if (!state.voice) return;
  state.voice.state = s;
  const screen = $("#voice-screen");
  const status = $("#voice-status");
  const hint = $("#voice-hint");
  if (screen) screen.dataset.state = s;
  const labels = { connecting: "正在连接…", listening: "我在听…", thinking: "让我想想…", speaking: "正在回答…" };
  if (status) status.textContent = labels[s] || "";
  if (hint) hint.textContent = s === "speaking" ? "点按光球可以打断" : s === "listening" ? "直接说话，或点按光球" : "可以随时挂断";
}

function addVoiceLine(who, text) {
  if (!state.voice) return;
  state.voice.lines.push({ who, text });
  const box = $("#voice-transcript");
  if (!box) return;
  const div = document.createElement("div");
  div.className = "v-line " + (who === "ai" ? "ai" : "");
  div.innerHTML = `<span class="v-who">${who === "ai" ? "DeepSeek" : "你"}</span>${esc(text)}`;
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
}

function typeVoiceLine(who, text) {
  if (!state.voice) return;
  const box = $("#voice-transcript");
  if (!box) return;
  const div = document.createElement("div");
  div.className = "v-line ai";
  div.innerHTML = `<span class="v-who">DeepSeek</span><span class="vt"></span>`;
  box.appendChild(div);
  const target = div.querySelector(".vt");
  let i = 0;
  state.voice.speaking = true;
  state.voice.typeTimer = setInterval(() => {
    i += 2;
    target.textContent = text.slice(0, i);
    box.scrollTop = box.scrollHeight;
    if (i >= text.length) {
      clearInterval(state.voice.typeTimer);
      state.voice.typeTimer = null;
      state.voice.speaking = false;
      state.voice.lines.push({ who: "ai", text });
      state.voiceTimers.push(setTimeout(() => setVoiceState("listening"), 2200));
    }
  }, 42);
}

function interruptVoice() {
  if (!state.voice) return;
  if (state.voice.typeTimer) {
    clearInterval(state.voice.typeTimer);
    state.voice.typeTimer = null;
  }
  if (state.voice.state === "speaking") {
    setVoiceState("listening");
    addVoiceLine("user", "等等，先说服帖度。");
    state.voiceTimers.push(
      setTimeout(() => {
        setVoiceState("thinking");
        state.voiceTimers.push(
          setTimeout(() => {
            setVoiceState("speaking");
            typeVoiceLine("ai", "明白。通勤一小时以上，佩戴压力比降噪深度更值得优先看。");
          }, 900)
        );
      }, 700)
    );
  } else if (state.voice.state === "listening") {
    toast("我在听，直接说就好");
  } else {
    toast("正在处理，稍等一下");
  }
}

function endVoice() {
  if (!state.voice) return;
  const seconds = state.voice.seconds;
  const lines = state.voice.lines.slice();
  clearVoiceTimers();
  state.voice = null;
  closeOverlay(true);
  const chat = activeChat();
  if (chat) {
    const transcript = lines.map((l) => `${l.who === "ai" ? "我" : "你"}：${l.text}`).join("\n");
    chat.messages.push({
      id: uid("m"),
      role: "assistant",
      kind: "voice",
      status: "done",
      time: nowTime(),
      versions: [
        {
          text: `**语音通话 · ${fmtDuration(seconds)}**\n\n${transcript || "（本次通话没有转写内容）"}\n\n> 通话已结束，转写已保存到这条对话。`,
          reasoning: "",
          search: null,
          memory: [],
          followups: ["继续用文字聊", "把结论整理成清单"],
        },
      ],
      vIndex: 0,
    });
    saveState();
  }
  navigate("chat", { chatId: chat?.id });
  toast("通话已结束，转写已保存");
}

/* ------------------------------ canvas ------------------------------ */

function openCanvas() {
  const body = `<div class="canvas-note">${icon("sparkle", "icon-sm")} 画布让回答变成可编辑的草稿</div>
    <textarea class="canvas-editor" id="canvas-editor"># 项目方案：AI 行业早报

## 目标
用一句话说清楚：让关心 AI 的人，在 3 分钟内知道昨天发生了什么。

## 本周动作
1. 确定 5 个固定信息源
2. 设计早报的三段式结构
3. 跑 3 天人工版，再决定是否自动化</textarea>
    <div class="sheet-actions"><button class="btn" data-action="canvas-ai">${icon("sparkle")}让 AI 润色</button><button class="btn primary" data-action="canvas-export">${icon("share")}导出</button></div>
    <div class="sheet-note">原型演示：编辑不会保存；导出为模拟操作。</div>`;
  openOverlay(sheetShell("画布", "文档 / 代码 / 网页产物", body));
}

function openProjectSources() {
  const p = activeProject();
  if (!p || !p.sources?.length) return toast("当前项目还没有资料");
  const body = `<div class="source-list">${p.sources.map((src) => `<button class="source" data-action="open-project-source" data-name="${esc(src.name)}"><span class="source-icon">${icon("file", "icon-xs")}</span><span class="source-main"><span class="source-title">${esc(src.name)}</span><span class="source-meta">${esc(src.meta)}</span></span></button>`).join("")}</div><div class="sheet-note">项目对话会自动引用这些资料。</div>`;
  openOverlay(sheetShell("项目资料", p.name, body));
}

/* ------------------------------ actions ------------------------------ */

function back() {
  if (state.route === "chat") {
    const chat = activeChat();
    if (chat?.projectId) navigate("project", { projectId: chat.projectId });
    else navigate("home");
  } else if (state.route === "project") navigate("projects");
  else if (state.route === "task") navigate("tasks");
  else if (state.route === "settings" || state.route === "memory") navigate("me");
  else if (state.route === "research") navigate("home");
  else navigate("home");
}

function handleAction(action, el) {
  const id = el.dataset.id;
  switch (action) {
    case "tab":
      navigate(el.dataset.tab === "me" ? "me" : el.dataset.tab);
      break;
    case "back":
      back();
      break;
    case "open-drawer":
      openDrawer();
      break;
    case "close-drawer":
      closeOverlay();
      break;
    case "close-sheet":
      closeOverlay();
      break;
    case "new-chat": {
      const chat = { id: uid("chat"), title: "新对话", projectId: null, meta: "刚刚", messages: [], draft: "" };
      state.chats.unshift(chat);
      state.activeChatId = chat.id;
      saveState();
      navigate("chat", { chatId: chat.id });
      setTimeout(() => $("#composer-input")?.focus(), 60);
      break;
    }
    case "open-chat":
      closeOverlay(true);
      state.activeChatId = id;
      state.chatScroll = null;
      navigate("chat", { chatId: id });
      break;
    case "use-suggestion":
      closeOverlay(true);
      sendMessage(el.dataset.text || "", state.route === "chat" ? "chat" : "home");
      break;
    case "followup":
      sendMessage(el.dataset.text || "", "chat");
      break;
    case "send": {
      if (state.stream) return stopStream();
      const ta = $("#composer-input");
      const text = ta?.value || "";
      const context = ta?.dataset.context || "home";
      sendMessage(text, context);
      break;
    }
    case "open-mode":
      openModeSheet();
      break;
    case "set-mode":
      state.mode[el.dataset.key] = el.dataset.value;
      saveState();
      openModeSheet();
      break;
    case "reset-mode":
      state.mode = { thinking: "auto", web: "auto", style: "standard" };
      saveState();
      openModeSheet();
      toast("已恢复自动");
      break;
    case "open-tools":
      openToolsSheet();
      break;
    case "canvas":
      closeOverlay(true);
      openCanvas();
      break;
    case "canvas-ai":
      toast("已让 AI 润色（演示）");
      break;
    case "canvas-export":
      toast("已导出画布（演示）");
      break;
    case "project-sources":
      closeOverlay(true);
      openProjectSources();
      break;
    case "dictate":
      dictate();
      break;
    case "voice":
      startVoice();
      break;
    case "attach-image":
      closeOverlay(true);
      $("#file-image")?.click();
      break;
    case "attach-file":
      closeOverlay(true);
      $("#file-doc")?.click();
      break;
    case "remove-attachment":
      state.attachments.splice(Number(el.dataset.idx), 1);
      render();
      break;
    case "demo-attachment":
      closeOverlay(true);
      if (el.dataset.kind === "image") {
        state.attachments.push({ kind: "image", name: "inference_share.svg", size: 184000, dataUrl: DEMO_IMAGE });
      } else {
        state.attachments.push({ kind: "file", name: "AI_infra_report_2026.pdf", size: 2410000 });
      }
      render();
      toast("已添加示例附件");
      break;
    case "scroll-bottom":
      $("#chat-scroll")?.scrollTo({ top: 1e9, behavior: "smooth" });
      break;
    case "toggle-reason": {
      const box = document.querySelector(`[data-reason="${id}"]`);
      if (box) box.classList.toggle("open");
      break;
    }
    case "why-route":
      openWhyRoute();
      break;
    case "cite": {
      const msg = el.closest("[data-msg]")?.dataset.msg;
      openSource(msg, el.dataset.n);
      break;
    }
    case "open-source":
      openSource(id, el.dataset.n);
      break;
    case "open-sources":
      openSources(id);
      break;
    case "open-process":
      openProcess(id);
      break;
    case "open-memory-used":
      openMemoryUsed(id);
      break;
    case "copy-msg": {
      const msg = activeChat()?.messages.find((m) => m.id === id);
      if (msg) copyText(plainText(currentVersion(msg).text));
      break;
    }
    case "copy-code":
      copyText(decodeURIComponent(el.dataset.code || ""));
      break;
    case "regenerate":
      regenerate(id);
      break;
    case "share-msg":
      openShare(id);
      break;
    case "read-msg": {
      const msg = activeChat()?.messages.find((m) => m.id === id);
      if (msg) speak(plainText(currentVersion(msg).text));
      break;
    }
    case "like":
    case "dislike": {
      const msg = activeChat()?.messages.find((m) => m.id === id);
      if (!msg) break;
      if (action === "like") { msg.liked = !msg.liked; msg.disliked = false; }
      else { msg.disliked = !msg.disliked; msg.liked = false; }
      saveState();
      render();
      toast(action === "like" ? "已记录：有帮助" : "已记录：需要改进");
      break;
    }
    case "version-prev":
    case "version-next": {
      const msg = activeChat()?.messages.find((m) => m.id === id);
      if (!msg) break;
      const n = msg.versions.length;
      msg.vIndex = (msg.vIndex + (action === "version-prev" ? -1 : 1) + n) % n;
      state.forceBottom = true;
      render({ bottom: true });
      break;
    }
    case "continue-msg":
      continueMsg(id);
      break;
    case "open-chat-menu":
      openChatMenu();
      break;
    case "share-last": {
      const chat = activeChat();
      const last = [...chat.messages].reverse().find((m) => m.role === "assistant" && m.status === "done");
      if (last) openShare(last.id);
      break;
    }
    case "rename-chat": {
      const chat = activeChat();
      if (!chat) return;
      const body = `<label class="field"><span>对话名称</span><input id="rename-input" value="${esc(chat.title)}" autocomplete="off"></label><div class="sheet-actions"><button class="btn primary block" data-action="do-rename">保存</button></div>`;
      openOverlay(sheetShell("重命名对话", "", body));
      break;
    }
    case "do-rename": {
      const chat = activeChat();
      const v = $("#rename-input")?.value.trim();
      if (chat && v) { chat.title = v; saveState(); }
      closeOverlay();
      render();
      break;
    }
    case "archive-chat":
      closeOverlay();
      toast("已归档（演示）");
      break;
    case "delete-chat": {
      const chat = activeChat();
      if (!chat) return;
      const body = `<div class="confirm-card"><div class="confirm-row"><span>对话</span><b>${esc(chat.title)}</b></div></div><div class="sheet-note">删除后无法恢复。原型里只是从列表移除。</div><div class="sheet-actions"><button class="btn ghost" data-action="close-sheet">取消</button><button class="btn" style="background:var(--danger);border-color:var(--danger);color:#fff" data-action="do-delete-chat">删除</button></div>`;
      openOverlay(sheetShell("删除对话", "二次确认", body));
      break;
    }
    case "do-delete-chat": {
      const chat = activeChat();
      state.chats = state.chats.filter((c) => c.id !== chat?.id);
      state.activeChatId = state.chats[0]?.id || null;
      saveState();
      closeOverlay(true);
      navigate("home");
      toast("对话已删除");
      break;
    }
    case "open-project":
      state.activeProjectId = id;
      navigate("project", { projectId: id });
      break;
    case "new-project":
      openNewProject();
      break;
    case "create-project":
      createProject();
      break;
    case "project-menu":
      toast("项目设置：编辑指令 / 删除项目（演示）");
      break;
    case "add-source":
      openAddSource();
      break;
    case "add-source-mock": {
      const p = activeProject();
      if (p) {
        p.sources.push({ name: "新增资料_" + (p.sources.length + 1) + ".pdf", meta: "PDF · 1.2 MB" });
        saveState();
      }
      closeOverlay();
      render();
      toast("资料已添加（演示）");
      break;
    }
    case "open-project-source":
      toast("预览：" + (el.dataset.name || "项目资料"));
      break;
    case "open-task":
      state.activeTaskId = id;
      navigate("task");
      break;
    case "new-task":
      openNewTask();
      break;
    case "task-example": {
      const ta = $("#task-input");
      if (ta) ta.value = el.dataset.text || "";
      break;
    }
    case "parse-task":
      parseTask();
      break;
    case "create-task":
      createTask();
      break;
    case "task-toggle-btn": {
      const t = findTask(id);
      if (t) { t.status = t.status === "active" ? "paused" : "active"; t.next = t.status === "active" ? "明天 08:00" : "已暂停"; saveState(); render(); }
      break;
    }
    case "run-task":
      runTask(id);
      break;
    case "task-more": {
      const t = findTask(state.activeTaskId);
      const body = `<div class="menu-group" style="margin-top:0">
        <button class="menu-row" data-action="task-toggle-btn" data-id="${t?.id}">${icon("pause", "icon-sm")}<span class="m-main"><span class="m-title">${t?.status === "active" ? "暂停任务" : "启用任务"}</span></span></button>
        <button class="menu-row danger" data-action="delete-task" data-id="${t?.id}">${icon("trash", "icon-sm")}<span class="m-main"><span class="m-title">删除任务</span></span></button>
      </div>`;
      openOverlay(sheetShell("任务设置", "", body));
      break;
    }
    case "delete-task": {
      state.tasks = state.tasks.filter((t) => t.id !== id);
      saveState();
      closeOverlay(true);
      navigate("tasks");
      toast("任务已删除");
      break;
    }
    case "open-memory":
      closeOverlay(true);
      navigate("memory");
      break;
    case "add-memory":
      openMemorySheet();
      break;
    case "edit-memory":
      openMemorySheet(id);
      break;
    case "save-memory": {
      const text = $("#memory-text")?.value.trim();
      const type = $("#memory-type")?.value || "preference";
      if (!text) return toast("先写点内容");
      const labels = { preference: "偏好", identity: "身份", project: "项目", relationship: "关系" };
      if (id) {
        const m = state.memories.find((x) => x.id === id);
        if (m) { m.text = text; m.type = type; m.label = labels[type]; }
      } else {
        state.memories.unshift({ id: uid("mem"), type, label: labels[type], text, source: "手动添加" });
      }
      saveState();
      closeOverlay(true);
      render();
      toast("记忆已保存");
      break;
    }
    case "delete-memory": {
      state.memories = state.memories.filter((m) => m.id !== id);
      saveState();
      render();
      toast("记忆已删除");
      break;
    }
    case "open-settings":
      closeOverlay(true);
      navigate("settings");
      break;
    case "set-theme":
      state.theme = el.dataset.value;
      applyTheme();
      saveState();
      render();
      break;
    case "set-font":
      state.settings.fontScale = Number(el.dataset.value);
      applyTheme();
      saveState();
      render();
      toast("字号已调整");
      break;
    case "clear-cache":
      toast("缓存已清除（演示）");
      break;
    case "reset-demo": {
      const body = `<div class="sheet-note">这会清除本地改动，恢复初始对话、项目、任务与记忆。</div><div class="sheet-actions"><button class="btn ghost" data-action="close-sheet">取消</button><button class="btn" style="background:var(--danger);border-color:var(--danger);color:#fff" data-action="do-reset">重置</button></div>`;
      openOverlay(sheetShell("重置演示数据", "不可撤销", body));
      break;
    }
    case "do-reset":
      localStorage.removeItem(STORAGE_KEY);
      if (state.stream) clearInterval(state.stream.timer);
      clearResearchTimers();
      state = seedState();
      state.route = "home";
      state.attachments = [];
      state.homeDraft = "";
      state.voice = null;
      state.voiceTimers = [];
      state.researchTimers = [];
      state.stream = null;
      state.taskDraft = null;
      closeOverlay(true);
      render();
      toast("已恢复初始数据");
      break;
    case "about":
    case "privacy":
      toast(action === "about" ? "DeepSeek Android 原型 · v0.1" : "隐私政策：演示占位");
      break;
    case "custom-instructions":
      toast("自定义指令：原型里先用固定风格");
      break;
    case "voice-settings":
      toast("音色：白浪 / 贝壳 / 海星 / 暗潮（演示）");
      break;
    case "start-research":
      closeOverlay(true);
      startResearch(state.homeDraft || activeChat()?.draft || "");
      break;
    case "research-plan":
      makeResearchPlan();
      break;
    case "research-adjust":
      state.research.step = "input";
      render();
      break;
    case "research-begin":
      beginResearch();
      break;
    case "research-export":
      toast("已导出 PDF（演示）");
      break;
    case "research-cancel":
      clearResearchTimers();
      state.research.step = "input";
      state.research.progress = 0;
      render();
      toast("已取消研究");
      break;
    case "research-back-chat": {
      const chat = activeChat();
      if (chat) {
        chat.messages.push({
          id: uid("m"), role: "assistant", status: "done", time: nowTime(),
          versions: [{ text: `**深度研究完成 · ${state.research.topic}**\n\n${REPORT_MD.split("\n").slice(0, 8).join("\n")}\n\n> 完整报告已保存，可在研究页重新打开。`, reasoning: "", search: { queries: ["深度研究"], sources: AI_SOURCES, readCount: 25 }, memory: [], followups: ["展开报告", "导出 PDF"] }],
          vIndex: 0,
        });
        saveState();
      }
      navigate("chat", { chatId: chat?.id });
      toast("报告已保存到对话");
      break;
    }
    case "research-example": {
      const ta = $("#research-topic");
      if (ta) ta.value = el.dataset.text || "";
      break;
    }
    case "copy-link":
      copyText("https://deepseek.com/share/8f2c");
      break;
    case "share-image":
      toast("长图已生成（演示）");
      break;
    case "share-pdf":
      toast("PDF 已导出（演示）");
      break;
    case "open-url": {
      const url = el.dataset.url;
      if (url && url !== "#") window.open(url, "_blank", "noopener");
      else toast("这是本地文件，原型里不打开");
      break;
    }
    case "voice-end":
      endVoice();
      break;
    case "voice-orb":
      interruptVoice();
      break;
    case "voice-mute":
      if (state.voice) {
        state.voice.muted = !state.voice.muted;
        const b = $("#voice-mute");
        if (b) b.innerHTML = state.voice.muted ? icon("mute") : icon("mic");
        toast(state.voice.muted ? "已静音" : "已取消静音");
      }
      break;
    case "voice-speaker":
      toast("已切换到扬声器（演示）");
      break;
    case "voice-camera":
      toast("相机预览将在下一版接入");
      break;
    default:
      console.warn("unhandled action", action);
  }
}

/* ------------------------------ helpers ------------------------------ */

function copyText(text) {
  if (!text) return;
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(() => toast("已复制")).catch(() => fallbackCopy(text));
  } else fallbackCopy(text);
}

function fallbackCopy(text) {
  const ta = document.createElement("textarea");
  ta.value = text;
  ta.style.position = "fixed";
  ta.style.opacity = "0";
  document.body.appendChild(ta);
  ta.select();
  try { document.execCommand("copy"); toast("已复制"); } catch (e) { toast("复制失败"); }
  ta.remove();
}

function speak(text) {
  if (!("speechSynthesis" in window)) return toast("当前浏览器不支持朗读");
  if (speechSynthesis.speaking) {
    speechSynthesis.cancel();
    toast("已停止朗读");
    return;
  }
  const u = new SpeechSynthesisUtterance(String(text).slice(0, 800));
  u.lang = "zh-CN";
  u.rate = 1.03;
  speechSynthesis.speak(u);
  toast("正在朗读…");
}

function dictate() {
  const sample = "帮我总结一下这份报告的核心结论，先给结论，再列三个关键判断。";
  if (state.route === "chat") {
    const c = activeChat();
    if (c) c.draft = sample;
  } else if (state.route === "project") {
    const p = activeProject();
    if (p) p.draft = sample;
  } else {
    state.homeDraft = sample;
  }
  render({ focus: true });
  toast("听写完成（演示）");
}

function handleFiles(files, kind) {
  const list = Array.from(files || []);
  if (!list.length) return;
  if ((state.attachments?.length || 0) + list.length > 5) return toast("最多添加 5 个附件");
  list.forEach((file) => {
    if (kind === "image" && file.type.startsWith("image/")) {
      const reader = new FileReader();
      reader.onload = () => {
        state.attachments.push({ kind: "image", name: file.name, size: file.size, dataUrl: reader.result });
        render();
      };
      reader.readAsDataURL(file);
    } else {
      state.attachments.push({ kind: "file", name: file.name, size: file.size });
      render();
    }
  });
  setTimeout(() => toast("已添加附件"), 120);
}

/* ------------------------------ events ------------------------------ */

document.addEventListener("click", (e) => {
  const jump = e.target.closest("[data-jump]");
  if (jump) {
    const target = jump.dataset.jump;
    if (target === "chat") navigate("chat", { chatId: "c1" });
    else if (target === "research") startResearch();
    else navigate(target);
    return;
  }
  if (e.target.closest(".switch")) return;
  const el = e.target.closest("[data-action]");
  if (!el) return;
  if (el.tagName === "INPUT") return;
  handleAction(el.dataset.action, el);
});

document.addEventListener("input", (e) => {
  if (e.target.id === "composer-input") onComposerInput(e);
  if (e.target.id === "drawer-search") {
    const q = e.target.value.trim().toLowerCase();
    $$("#drawer-list .drawer-chat").forEach((b) => {
      b.style.display = b.textContent.toLowerCase().includes(q) ? "block" : "none";
    });
  }
});

document.addEventListener("change", (e) => {
  const setting = e.target.dataset.setting;
  if (setting) {
    state.settings[setting] = e.target.checked;
    saveState();
    if (setting === "memory") render();
    toast(e.target.checked ? "已开启" : "已关闭");
    return;
  }
  const taskId = e.target.dataset.taskToggle;
  if (taskId) {
    const t = findTask(taskId);
    if (t) {
      t.status = e.target.checked ? "active" : "paused";
      t.next = e.target.checked ? "明天 08:00" : "已暂停";
      saveState();
      toast(t.status === "active" ? "任务已启用" : "任务已暂停");
      render();
    }
  }
});

document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    if (state.voice) return endVoice();
    const ov = $("#overlay");
    if (ov?.classList.contains("open")) return closeOverlay();
  }
  if (e.target.id === "composer-input" && e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    const ta = e.target;
    sendMessage(ta.value, ta.dataset.context || "home");
  }
  if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
    e.preventDefault();
    openDrawer();
  }
});

$("#file-image")?.addEventListener("change", (e) => {
  handleFiles(e.target.files, "image");
  e.target.value = "";
});
$("#file-doc")?.addEventListener("change", (e) => {
  handleFiles(e.target.files, "file");
  e.target.value = "";
});

/* ------------------------------ init ------------------------------ */

applyTheme();
render();
if ("serviceWorker" in navigator && location.protocol.startsWith("http")) {
  const local = /^(127\.0\.0\.1|localhost)$/.test(location.hostname);
  if (local) navigator.serviceWorker.getRegistrations().then((rs) => rs.forEach((r) => r.unregister())).catch(() => {});
  else navigator.serviceWorker.register("sw.js").catch(() => {});
}
setTimeout(() => {
  if (!localStorage.getItem("dsproto.seen")) {
    toast("原型演示 · 所有内容为模拟数据");
    try { localStorage.setItem("dsproto.seen", "1"); } catch (e) {}
  }
}, 700);
