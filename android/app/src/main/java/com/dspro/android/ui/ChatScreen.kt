package com.dspro.android.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.dspro.android.data.ThinkingEffort
import com.dspro.android.data.ChatMessage
import com.dspro.android.data.MsgStatus
import com.dspro.android.data.Role

private const val MAX_QUOTE_LENGTH = 4000

enum class ChatAction { Copy, Regenerate, Share, Speak, Like, Dislike, SaveNote, Remember }

@Composable
fun ChatScaffold(
    sessionId: Long,
    title: String,
    messages: List<ChatMessage>,
    listState: LazyListState,
    input: String,
    onInputChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenSources: (ChatMessage) -> Unit,
    onOpenNote: (String) -> Unit,
    onMessageAction: (ChatMessage, ChatAction) -> Unit,
    onReasoningToggle: (Long) -> Unit,
    onAttach: () -> Unit,
    onMore: () -> Unit,
    model: String,
    provider: String,
    onModelSelect: (String) -> Unit,
    effort: ThinkingEffort,
    onEffortSelect: (ThinkingEffort) -> Unit,
    attachment: String?,
    onClearAttachment: () -> Unit,
    noteTitles: List<String>,
    onPickNote: () -> Unit,
    onClearNote: () -> Unit,
    onRemoveNote: (Int) -> Unit,
    quotedText: String?,
    onClearQuote: () -> Unit,
    onQuote: (String) -> Unit,
    apiConfigured: Boolean,
    onConfigureApi: () -> Unit,
    onBack: () -> Unit = {},
    busy: Boolean = false,
    modelDisplayName: String? = null,
    chatGptAccountLabel: String = "",
    onManageUsage: () -> Unit = {},
) {
    val c = Dsh.colors
    var quoteSource by remember(sessionId) { mutableStateOf<String?>(null) }
    quoteSource?.let { source ->
        QuoteSelectionSheet(
            source = source,
            onDismiss = { quoteSource = null },
            onConfirm = {
                quoteSource = null
                onQuote(it)
            },
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.main)
            .imePadding()
    ) {
        SimpleTopBar(title, onBack = onBack, onMenu = null, action = {
            DshIconButton(Icons.Outlined.MoreHoriz, "对话操作", onMore, size = 44.dp, tint = c.subtle)
        })
        if (!apiConfigured) {
            ApiBanner(onClick = onConfigureApi, message = if (provider == "chatgpt") "尚未连接可用的 ChatGPT 订阅账号" else "还没有配置 API Key")
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            MessageList(
                messages = messages,
                listState = listState,
                onOpenSources = onOpenSources,
                onOpenNote = onOpenNote,
                onMessageAction = onMessageAction,
                onSelectQuote = { quoteSource = it },
                onReasoningToggle = onReasoningToggle,
                modifier = Modifier.widthIn(max = 780.dp).fillMaxWidth(),
            )
        }

        Composer(
            input = input,
            onInputChange = onInputChange,
            isStreaming = isStreaming,
            onSend = onSend,
            onStop = onStop,
            onAttach = onAttach,
            model = model,
            provider = provider,
            modelDisplayName = modelDisplayName,
            chatGptAccountLabel = chatGptAccountLabel,
            onManageUsage = onManageUsage,
            onModelSelect = onModelSelect,
            effort = effort,
            onEffortSelect = onEffortSelect,
            attachment = attachment,
            onClearAttachment = onClearAttachment,
            noteTitles = noteTitles,
            onPickNote = onPickNote,
            onClearNote = onClearNote,
            onRemoveNote = onRemoveNote,
            quotedText = quotedText,
            onClearQuote = onClearQuote,
            onConfigureApi = onConfigureApi,
            busy = busy,
        )
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    onOpenSources: (ChatMessage) -> Unit,
    onOpenNote: (String) -> Unit,
    onMessageAction: (ChatMessage, ChatAction) -> Unit,
    onSelectQuote: (String) -> Unit,
    onReasoningToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageItem(
                msg = msg,
                onOpenSources = onOpenSources,
                onOpenNote = onOpenNote,
                onMessageAction = onMessageAction,
                onSelectQuote = onSelectQuote,
                onReasoningToggle = onReasoningToggle,
            )
        }
    }
}

@Composable
private fun MessageItem(
    msg: ChatMessage,
    onOpenSources: (ChatMessage) -> Unit,
    onOpenNote: (String) -> Unit,
    onMessageAction: (ChatMessage, ChatAction) -> Unit,
    onSelectQuote: (String) -> Unit,
    onReasoningToggle: (Long) -> Unit,
) {
    val c = Dsh.colors
    if (msg.role == Role.USER) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                msg.quoteText?.let { quote ->
                    QuoteCard(quote, Modifier.widthIn(max = 320.dp).padding(bottom = 6.dp))
                }
                Surface(color = c.userBubble, shape = RoundedCornerShape(16.dp)) {
                    SelectionContainer {
                        MarkdownText(
                            text = msg.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            baseSize = 14.sp,
                            onWikiLink = onOpenNote,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        )
                    }
                }
                msg.attachment?.let { name ->
                    Spacer(Modifier.height(6.dp))
                    DshPill(
                        text = name,
                        leading = { Icon(Icons.Outlined.Description, null, Modifier.size(12.dp), tint = c.muted) },
                    )
                }
                msg.noteRef?.let { name ->
                    Spacer(Modifier.height(6.dp))
                    DshPill(
                        text = "引用笔记 · $name",
                        leading = { Icon(Icons.Outlined.Description, null, Modifier.size(12.dp), tint = c.muted) },
                    )
                }
                msg.contextSnapshot?.let { raw ->
                    val snapshotInfo = runCatching {
                        val notes = org.json.JSONObject(raw).optJSONArray("notes") ?: return@runCatching null
                        val titles = buildList {
                            for (i in 0 until notes.length()) {
                                val item = notes.getJSONObject(i)
                                val source = when (item.optString("source", "note")) { "note" -> "笔记"; else -> item.optString("source", "笔记") }
                                add("$source · ${item.optString("title")}" + if (item.optBoolean("truncated")) "（部分内容已截断）" else "")
                            }
                        }
                        titles.takeIf { it.isNotEmpty() }
                    }.getOrNull()
                    snapshotInfo?.let { labels ->
                        Spacer(Modifier.height(5.dp))
                        Text("请求使用的笔记快照：${labels.joinToString("、")}", color = c.subtle, fontSize = 9.5.sp, maxLines = 3)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(msg.time, color = c.subtle, fontSize = 10.sp)
                    if (msg.text.isNotBlank()) {
                        ActionIcon(Icons.Outlined.FormatQuote, "选取引用") { onSelectQuote(msg.text) }
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        if (msg.reasoning != null) {
            DshPill(
                text = if (msg.reasoningSeconds > 0) "思考了 ${msg.reasoningSeconds} 秒" else "思考过程",
                leading = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(12.dp), tint = c.subtle) },
                trailing = {
                    Icon(
                        if (msg.reasoningExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null,
                        Modifier.size(13.dp),
                        tint = c.subtle,
                    )
                },
                onClick = { onReasoningToggle(msg.id) },
            )
            if (msg.reasoningExpanded) {
                Row(
                    Modifier
                        .padding(top = 8.dp, bottom = 2.dp)
                        .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                        .animateContentSize()
                ) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.hairlineStrong)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        msg.reasoning,
                        color = c.subtle,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        if (msg.sources.isNotEmpty()) {
            DshPill(
                text = "已联网搜索 · ${msg.sources.size} 个真实来源",
                leading = { Icon(Icons.Outlined.Search, null, Modifier.size(12.dp), tint = c.accent) },
                trailing = { Icon(Icons.Outlined.ExpandMore, null, Modifier.size(13.dp), tint = c.subtle) },
                onClick = { onOpenSources(msg) },
            )
            Spacer(Modifier.height(10.dp))
        }

        if (msg.searchStatus.isNotBlank() && !msg.searching) {
            Text(msg.searchStatus, color = c.muted, fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        if (msg.error != null) {
            if (msg.text.isNotEmpty()) {
                SelectionContainer {
                    MarkdownText(text = msg.text, color = MaterialTheme.colorScheme.onSurface, onWikiLink = onOpenNote)
                }
                Spacer(Modifier.height(6.dp))
            }
            ErrorCard(msg.error)
        } else if (msg.searching) {
            Text(msg.searchStatus.ifBlank { "正在联网搜索…" }, color = c.muted, fontSize = 11.sp)
        } else if (msg.status == MsgStatus.THINKING && msg.text.isEmpty()) {
            TypingRow()
        } else {
            Row(Modifier.fillMaxWidth()) {
                SelectionContainer(Modifier.weight(1f)) {
                    MarkdownText(
                        text = msg.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        onWikiLink = onOpenNote,
                    )
                }
                if (msg.status == MsgStatus.STREAMING) {
                    Text("▍", color = c.accent, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp, top = 2.dp))
                }
            }
            if (msg.status == MsgStatus.STOPPED) {
                Spacer(Modifier.height(6.dp))
                Text("已停止", color = c.subtle, fontSize = 10.sp)
            }
        }

        if (msg.sources.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DshHairline()
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("来源 ${msg.sources.size}", color = c.subtle, fontSize = 10.5.sp, modifier = Modifier.weight(1f))
                Text(
                    "查看全部",
                    color = c.accent,
                    fontSize = 10.5.sp,
                    modifier = Modifier.clickable { onOpenSources(msg) },
                )
            }
            Spacer(Modifier.height(3.dp))
            msg.sources.take(2).forEach { source ->
                SourceRow(source) { onOpenSources(msg) }
            }
        }

        if (msg.memory.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            DshPill(
                text = "使用了 ${msg.memory.size} 条记忆",
                leading = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(12.dp), tint = c.subtle) },
            )
        }

        if (msg.status == MsgStatus.DONE || msg.status == MsgStatus.STOPPED) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (msg.text.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onSelectQuote(msg.text) },
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Outlined.FormatQuote, null, Modifier.size(16.dp), tint = c.accent)
                        Spacer(Modifier.width(5.dp))
                        Text("选取引用", color = c.accent, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    ActionIcon(Icons.Outlined.ContentCopy, "复制") { onMessageAction(msg, ChatAction.Copy) }
                }
                if (msg.status == MsgStatus.DONE) {
                    ActionIcon(Icons.Outlined.Refresh, "重新生成") { onMessageAction(msg, ChatAction.Regenerate) }
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        ActionIcon(Icons.Outlined.MoreHoriz, "更多回复操作") { menuOpen = true }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            listOf(
                                Triple(ChatAction.SaveNote, Icons.Outlined.BookmarkAdd, "保存为笔记"),
                                Triple(ChatAction.Remember, Icons.Outlined.AutoAwesome, "记住这条"),
                                Triple(ChatAction.Like, Icons.Outlined.ThumbUp, if (msg.liked == true) "取消有帮助" else "有帮助"),
                                Triple(ChatAction.Dislike, Icons.Outlined.ThumbDown, if (msg.liked == false) "取消没帮助" else "没帮助"),
                            ).forEach { (action, icon, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                                    onClick = { menuOpen = false; onMessageAction(msg, action) },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (msg.versionCount > 1) {
                    Text("${msg.version}/${msg.versionCount}", color = c.subtle, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    val c = Dsh.colors
    Surface(
        color = Color(0xFFFFF0EC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x33D92D20)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(16.dp), tint = Color(0xFFD92D20))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("请求失败", color = Color(0xFFB42318), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(message, color = Color(0xFFB42318).copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun TypingRow() {
    val c = Dsh.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(c.subtle))
            }
        }
        Text("正在思考…", color = c.subtle, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteSelectionSheet(
    source: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val c = Dsh.colors
    var value by remember(source) { mutableStateOf(TextFieldValue(source)) }
    val selected = remember(value.selection, source) {
        source.substring(value.selection.min, value.selection.max)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.main,
    ) {
        Column(
            Modifier.align(Alignment.CenterHorizontally)
                .widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
        ) {
            Text("选取引用", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "长按消息原文并拖动选区手柄，选好后加入输入框。",
                color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            Surface(
                color = c.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, c.hairline),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    readOnly = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp, lineHeight = 23.sp,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 320.dp)
                        .padding(14.dp),
                )
            }
            if (selected.length > MAX_QUOTE_LENGTH) {
                Text("最多引用 $MAX_QUOTE_LENGTH 个字符，请缩小选区。", color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { value = value.copy(selection = TextRange(0, source.length)) },
                    enabled = source.length <= MAX_QUOTE_LENGTH,
                ) { Text("全选") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onConfirm(selected) },
                    enabled = selected.isNotBlank() && selected.length <= MAX_QUOTE_LENGTH,
                ) { Text("加入引用") }
            }
        }
    }
}

@Composable
private fun QuoteCard(text: String, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null) {
    val c = Dsh.colors
    var expanded by remember(text) { mutableStateOf(false) }
    Surface(
        color = c.accentSoft,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f, fill = onRemove != null)
                    .clickable(onClickLabel = if (expanded) "收起引用" else "展开引用") { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FormatQuote, null, Modifier.size(14.dp), tint = c.accent)
                    Spacer(Modifier.width(5.dp))
                    Text("引用片段", color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null, Modifier.size(13.dp), tint = c.accent)
                }
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (expanded) Modifier.heightIn(max = 144.dp).verticalScroll(rememberScrollState()) else Modifier,
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.Close, "移除引用", Modifier.size(17.dp), tint = c.muted)
                }
            }
        }
    }
}

@Composable
private fun Composer(
    input: String,
    onInputChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
    model: String,
    provider: String,
    onModelSelect: (String) -> Unit,
    effort: ThinkingEffort,
    onEffortSelect: (ThinkingEffort) -> Unit,
    attachment: String?,
    onClearAttachment: () -> Unit,
    noteTitles: List<String>,
    onPickNote: () -> Unit,
    onClearNote: () -> Unit,
    onRemoveNote: (Int) -> Unit,
    quotedText: String?,
    onClearQuote: () -> Unit,
    onConfigureApi: () -> Unit,
    busy: Boolean,
    modelDisplayName: String? = null,
    chatGptAccountLabel: String = "",
    onManageUsage: () -> Unit = {},
) {
    val c = Dsh.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val modelLabel = modelDisplayName?.takeIf { it.isNotBlank() } ?: when (model) {
        "deepseek-flash" -> "DeepSeek-V4.1-Flash"
        "deepseek-v4-pro" -> "DeepSeek-V4-Pro"
        else -> model
    }
    var modelMenu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 880.dp).fillMaxWidth(),
            color = c.main,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, c.hairlineStrong),
            shadowElevation = 3.dp,
        ) {
            Column(Modifier.padding(9.dp)) {
                if (provider == "chatgpt") Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("使用 ChatGPT 订阅", color = c.accent, fontSize = 11.sp)
                        if (chatGptAccountLabel.isNotBlank()) Text(chatGptAccountLabel, color = c.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = onManageUsage) { Text("管理用量", fontSize = 11.sp) }
                }
                if (attachment != null) {
                    Row(Modifier.padding(start = 3.dp, bottom = 6.dp)) {
                        DshPill(
                            text = attachment,
                            leading = { Icon(Icons.Outlined.Description, null, Modifier.size(12.dp), tint = c.muted) },
                            trailing = { Icon(Icons.Outlined.Close, null, Modifier.size(12.dp), tint = c.subtle) },
                            onClick = if (busy) null else onClearAttachment,
                        )
                    }
                }
                if (noteTitles.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 3.dp, bottom = 6.dp)) {
                        noteTitles.forEachIndexed { index, title ->
                            DshPill(
                                text = title,
                                leading = { Icon(Icons.Outlined.Description, null, Modifier.size(12.dp), tint = c.accent) },
                                trailing = { Icon(Icons.Outlined.Close, null, Modifier.size(12.dp), tint = c.subtle) },
                                onClick = if (busy) null else ({ onRemoveNote(index) }),
                            )
                        }
                    }
                }
                if (quotedText != null) {
                    QuoteCard(
                        text = quotedText,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onRemove = if (busy) null else onClearQuote,
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    readOnly = busy,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (!busy && !isStreaming && (input.isNotBlank() || attachment != null || noteTitles.isNotEmpty())) {
                            keyboard?.hide()
                            onSend()
                        }
                    }),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 120.dp),
                    decorationBox = { inner ->
                        Box(Modifier.padding(horizontal = 3.dp, vertical = 7.dp)) {
                            if (input.isEmpty()) {
                                Text(
                                    if (quotedText != null) "围绕引用继续提问…" else "发消息，@ 引用笔记",
                                    color = c.subtle,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAttach, enabled = !busy, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Add, "添加", Modifier.size(18.dp), tint = c.muted)
                    }
                    IconButton(onClick = onPickNote, enabled = !busy, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.AlternateEmail, "引用笔记", Modifier.size(17.dp), tint = if (noteTitles.isNotEmpty()) c.accent else c.muted)
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        Row(
                            Modifier.heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !busy) { modelMenu = true }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(if (provider == "deepseek") "DeepSeek · $modelLabel · ${effort.label}" else "${when (provider) { "openai" -> "OpenAI"; "chatgpt" -> "ChatGPT"; "anthropic" -> "Claude"; "aliyun" -> "阿里云百炼"; else -> "自定义" }} · $modelLabel", color = c.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(12.dp), tint = c.subtle)
                        }
                        DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                            Text("模型", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                            if (provider == "deepseek") listOf(
                                "deepseek-flash" to "DeepSeek-V4.1-Flash",
                                "deepseek-v4-pro" to "DeepSeek-V4-Pro",
                            ).forEach { (id, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (model == id) "✓  $label" else label) },
                                    onClick = { onModelSelect(id) },
                                )
                            }
                            if (provider == "deepseek") {
                                HorizontalDivider()
                                Text("思考强度", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                                ThinkingEffort.entries.forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text(if (effort == level) "✓  ${level.label}" else level.label) },
                                        onClick = { onEffortSelect(level); modelMenu = false },
                                    )
                                }
                            } else DropdownMenuItem(text = { Text("更改服务商或模型") }, onClick = { modelMenu = false; onConfigureApi() })
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    val enabled = isStreaming || (!busy && (input.isNotBlank() || attachment != null || noteTitles.isNotEmpty()))
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (enabled) c.accent else c.chipBg)
                            .clickable(enabled = enabled) {
                                if (isStreaming) onStop() else if (!busy) {
                                    keyboard?.hide(); onSend()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isStreaming) Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,
                            if (isStreaming) "停止" else "发送",
                            Modifier.size(17.dp),
                            tint = if (enabled) Color.White else c.subtle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiBanner(onClick: () -> Unit, message: String = "还没有配置 API Key") {
    val c = Dsh.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.accentSoft)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lock, null, Modifier.size(13.dp), tint = c.accent)
        Spacer(Modifier.width(7.dp))
        Text(message, color = c.accent, fontSize = 10.5.sp, modifier = Modifier.weight(1f))
        Text("打开设置", color = c.accent, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
    }
}

