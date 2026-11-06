package com.dspro.android.ui.notes

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.DshHairline
import com.dspro.android.ui.MarkdownText
import com.dspro.android.ui.MarkdownParser
import com.dspro.android.ui.inlineMarkdown

@Stable
internal class NoteDocument(markdown: String) {
    var value by mutableStateOf(TextFieldValue(markdown))
        private set
    var revision by mutableIntStateOf(0)
        private set
    private val undoHistory = mutableStateListOf<TextFieldValue>()
    private val redoHistory = mutableStateListOf<TextFieldValue>()
    val canUndo by derivedStateOf { undoHistory.isNotEmpty() }
    val canRedo by derivedStateOf { redoHistory.isNotEmpty() }
    private var lastTypingAt: Long? = null

    fun edit(next: TextFieldValue, typing: Boolean = false) {
        val previous = value
        value = next
        if (previous.text == next.text) {
            if (previous.selection != next.selection && next.composition == null) lastTypingAt = null
            return
        }
        val now = SystemClock.uptimeMillis()
        if (!typing || lastTypingAt?.let { now - it < 700 } != true || undoHistory.isEmpty()) {
            undoHistory.add(previous)
            if (undoHistory.size > 60) undoHistory.removeAt(0)
        }
        redoHistory.clear()
        lastTypingAt = if (typing) now else null
        revision++
    }

    fun undo() = restore(undoHistory, redoHistory)
    fun redo() = restore(redoHistory, undoHistory)

    private fun restore(from: MutableList<TextFieldValue>, to: MutableList<TextFieldValue>) {
        if (from.isEmpty()) return
        to.add(value)
        value = from.removeAt(from.lastIndex).copy(composition = null)
        lastTypingAt = null
        revision++
    }

    fun readMarkdown(): String = value.text

    fun matches(query: String, caseSensitive: Boolean = false): List<Int> {
        if (query.isEmpty()) return emptyList()
        val result = mutableListOf<Int>()
        var from = 0
        while (from <= value.text.length - query.length) {
            val found = value.text.indexOf(query, from, ignoreCase = !caseSensitive)
            if (found < 0) break
            result.add(found)
            from = found + query.length
        }
        return result
    }

    fun selectMatch(start: Int, length: Int) {
        value = value.copy(selection = TextRange(start, start + length), composition = null)
        lastTypingAt = null
    }

    fun replaceMatch(start: Int, query: String, replacement: String, all: Boolean = false, caseSensitive: Boolean = false) {
        if (query.isEmpty()) return
        val source = value.text
        val positions = if (all) matches(query, caseSensitive) else listOf(start)
        val result = buildString {
            var cursor = 0
            for (position in positions) {
                if (position < cursor || position < 0 || position + query.length > source.length) continue
                if (!source.regionMatches(position, query, 0, query.length, ignoreCase = !caseSensitive)) continue
                append(source, cursor, position); append(replacement); cursor = position + query.length
            }
            append(source, cursor, source.length)
        }
        if (result != source) edit(TextFieldValue(result, TextRange(value.selection.start.coerceIn(0, result.length))))
    }
}

@Composable
internal fun ContinuousNoteEditor(
    document: NoteDocument,
    onWikiLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Dsh.colors
    var showFind by remember(document) { mutableStateOf(false) }
    var showWikiLinks by remember(document) { mutableStateOf(false) }
    var query by remember(document) { mutableStateOf("") }
    var replacement by remember(document) { mutableStateOf("") }
    var activeMatch by remember(document) { mutableIntStateOf(0) }
    val matches = remember(document.value.text, query) { document.matches(query) }
    var editorFocused by remember(document) { mutableStateOf(false) }
    val wikiLinks = remember(document.value.text) {
        Regex("\\[\\[([^\\]]+)\\]\\]").findAll(document.value.text).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.distinct().toList()
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val latestImeVisible by rememberUpdatedState(imeVisible)
    val latestEditorFocused by rememberUpdatedState(editorFocused)
    val modalOpen by rememberUpdatedState(showFind || showWikiLinks)
    var imeWasVisible by remember(document) { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible) {
            delay(150)
            if (!latestImeVisible && latestEditorFocused && !modalOpen) focusManager.clearFocus()
            imeWasVisible = false
        }
    }
    BackHandler(enabled = imeVisible) {
        keyboard?.hide()
        focusManager.clearFocus()
    }

    fun format(prefix: String, suffix: String? = null) {
        val value = document.value
        document.edit(if (suffix == null) formatLine(value, prefix) else formatInline(value, prefix, suffix))
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth()) {
            run {
                val value = document.value
                val cursor = value.selection.end
                val activeLineStart = if (editorFocused) value.text.lastIndexOf('\n', cursor - 1) + 1 else -1
                val renderer = remember(c.accent, c.codeBg) { NoteLineRenderer(c.accent, c.codeBg) }
                val transformation = remember(renderer, activeLineStart) {
                    VisualTransformation { source ->
                        TransformedText(renderer.render(source.text, activeLineStart), OffsetMapping.Identity)
                    }
                }
                val editorTextStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                )
                BasicTextField(
                    value = value,
                    onValueChange = { document.edit(pairedDelete(document.value, it), typing = true) },
                    visualTransformation = transformation,
                    textStyle = editorTextStyle,
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxSize()
                        .onFocusChanged { editorFocused = it.isFocused }
                        .semantics { contentDescription = "笔记正文" }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Box {
                            if (value.text.isEmpty()) Text("写下你的想法…", style = editorTextStyle.copy(color = c.muted))
                            inner()
                        }
                    },
                )
            }
        }
        DshHairline()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            run {
                IconButton(onClick = { document.undo() }, enabled = document.canUndo) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, "撤销", Modifier.size(20.dp))
                }
                IconButton(onClick = { document.redo() }, enabled = document.canRedo) {
                    Icon(Icons.AutoMirrored.Outlined.Redo, "重做", Modifier.size(20.dp))
                }
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    EditorTool("B", "粗体") { format("**", "**") }
                    EditorTool("I", "斜体") { format("*", "*") }
                    EditorTool("H", "标题") { format("## ") }
                    EditorTool("•", "列表") { format("- ") }
                    EditorTool("❝", "引用") { format("> ") }
                    EditorTool("</>", "行内代码") { format("`", "`") }
                    EditorTool("链接", "插入链接") { format("[", "](url)") }
                    EditorTool("[[ ]]", "插入笔记双链") { format("[[", "]]") }
                    EditorTool("打开双链", "打开笔记双链") { showWikiLinks = true }
                    IconButton(onClick = { showFind = true }) { Icon(Icons.Outlined.Search, "在笔记中查找和替换", Modifier.size(20.dp)) }
                }
            }
        }
    }
    if (showFind) AlertDialog(
        onDismissRequest = { showFind = false },
        title = { Text("查找和替换") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it; activeMatch = 0 }, label = { Text("查找") }, singleLine = true)
                OutlinedTextField(replacement, { replacement = it }, label = { Text("替换为") }, singleLine = true)
                Text(if (query.isBlank()) "输入内容开始查找" else "${matches.size} 个匹配", color = Dsh.colors.muted)
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick = { if (matches.isNotEmpty()) { activeMatch = (activeMatch - 1 + matches.size) % matches.size; document.selectMatch(matches[activeMatch], query.length) } }, enabled = matches.isNotEmpty()) { Text("上一个") }
                        TextButton(onClick = { if (matches.isNotEmpty()) { activeMatch = (activeMatch + 1) % matches.size; document.selectMatch(matches[activeMatch], query.length) } }, enabled = matches.isNotEmpty()) { Text("下一个") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick = { if (matches.isNotEmpty()) document.replaceMatch(matches[activeMatch.coerceIn(0, matches.lastIndex)], query, replacement) }, enabled = matches.isNotEmpty()) { Text("替换") }
                        TextButton(onClick = { document.replaceMatch(0, query, replacement, all = true); activeMatch = 0 }, enabled = matches.isNotEmpty()) { Text("全部替换") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showFind = false }) { Text("完成") } },
    )
    if (showWikiLinks) AlertDialog(
        onDismissRequest = { showWikiLinks = false },
        title = { Text("笔记双链") },
        text = {
            if (wikiLinks.isEmpty()) Text("这篇笔记还没有 [[双链]]", color = Dsh.colors.muted)
            else Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                wikiLinks.forEach { target ->
                    TextButton(onClick = { showWikiLinks = false; onWikiLink(target) }) { Text("打开《$target》") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showWikiLinks = false }) { Text("完成") } },
    )
}

@Composable
private fun EditorTool(text: String, description: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 44.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) { Text(text, color = Dsh.colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

internal class NoteLineRenderer(private val accent: Color, private val codeBg: Color) {
    private val lines = android.util.LruCache<Pair<String, Boolean>, AnnotatedString>(4096)

    fun render(source: String, activeLineStart: Int): AnnotatedString = buildAnnotatedString {
        var offset = 0
        var fence: String? = null
        source.splitToSequence('\n').forEachIndexed { index, line ->
            if (index > 0) append('\n')
            val fenceMarker = line.trimStart().takeWhile { it == '`' || it == '~' }
                .takeIf { MarkdownParser.isFence(line) }
            if (fence != null || fenceMarker != null || MarkdownParser.isIndentedCode(line)) {
                val start = length
                append(line)
                if (line.isNotEmpty()) addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg), start, length)
                val opened = fence
                if (opened == null) fence = fenceMarker
                else if (fenceMarker?.startsWith(opened) == true && line.trimStart().drop(fenceMarker.length).isBlank()) fence = null
            } else {
                val key = line to (offset == activeLineStart)
                append(lines.get(key) ?: styledLine(line, key.second).also { lines.put(key, it) })
            }
            offset += line.length + 1
        }
    }

    private fun styledLine(line: String, active: Boolean): AnnotatedString = buildAnnotatedString {
        val heading = MarkdownParser.headingLevel(line)
        val indent = line.takeWhile { it == ' ' || it == '\t' }.length
        val unorderedEnd = MarkdownParser.unorderedPrefix(line)
        val orderedEnd = MarkdownParser.orderedPrefix(line)
        val unordered = unorderedEnd != null
        val listPrefix = when {
            unordered -> indent + 2
            orderedEnd != null -> orderedEnd
            else -> 0
        }
        val prefix = when {
            heading != null -> line.indexOfFirst { it == '#' } + heading + 1
            MarkdownParser.quotePrefix(line) != null -> MarkdownParser.quotePrefix(line)!!
            listPrefix > 0 -> listPrefix
            else -> 0
        }
        if (unordered) {
            append(line.take(indent))
            append('•')
            append(line.substring(indent + 1, prefix))
        } else if (orderedEnd != null) {
            append(line.take(prefix))
        } else if (heading != null || MarkdownParser.quotePrefix(line) != null) {
            append(if (active) line.take(prefix) else "\u200B".repeat(prefix))
        } else append(line.take(prefix))
        append(inlineMarkdown(line.drop(prefix), accent, codeBg, showSyntax = true, hideMarkers = !active))
        if (heading != null) {
            val size = when (heading) { 1 -> 22.sp; 2 -> 19.sp; else -> 17.sp }
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), prefix, length)
        }
        if (MarkdownParser.quotePrefix(line) != null && length > prefix) addStyle(SpanStyle(color = accent), prefix, length)
    }
}

internal fun formatInline(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val source = value.text
    var start = value.selection.min
    var end = value.selection.max
    val emphasis = prefix == "*" || prefix == "**"
    if (emphasis) {
        while (start < end && source[start].isWhitespace()) start++
        while (end > start && source[end - 1].isWhitespace()) end--
    }

    val selected = source.substring(start, end)
    if (emphasis && ('\n' in selected || selected.hasEmphasis(prefix.length))) {
        val formatted = selected.split('\n').joinToString("\n") { line ->
            val core = line.trim()
            val styled = when {
                core.isEmpty() -> core
                core.hasEmphasis(prefix.length) -> core.drop(prefix.length).dropLast(suffix.length)
                else -> "$prefix$core$suffix"
            }
            if (core.isEmpty()) line else line.replaceFirst(core, styled)
        }
        return TextFieldValue(source.replaceRange(start, end, formatted), TextRange(start, start + formatted.length))
    }

    val before = start - prefix.length
    val wrapped = before >= 0 && source.startsWith(prefix, before) && source.startsWith(suffix, end)
    val hasOuterEmphasis = !emphasis || start == end ||
        matchingEmphasis(
            source.take(start).takeLastWhile { it == '*' }.length,
            source.drop(end).takeWhile { it == '*' }.length,
            prefix.length,
        )
    if (wrapped && hasOuterEmphasis) {
        return TextFieldValue(source.replaceRange(before, end + suffix.length, selected), TextRange(before, before + selected.length))
    }
    return TextFieldValue(
        source.replaceRange(start, end, prefix + selected + suffix),
        TextRange(start + prefix.length, end + prefix.length),
    )
}

private fun String.hasEmphasis(length: Int): Boolean {
    val left = takeWhile { it == '*' }.length
    val right = takeLastWhile { it == '*' }.length
    return this.length > left + right && matchingEmphasis(left, right, length)
}

private fun matchingEmphasis(left: Int, right: Int, length: Int): Boolean =
    left == right && left in 1..3 && (left and length) != 0

private fun formatLine(value: TextFieldValue, prefix: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val lineStart = value.text.lastIndexOf('\n', start - 1) + 1
    val removing = value.text.startsWith(prefix, lineStart)
    val newText = if (removing) value.text.removeRange(lineStart, lineStart + prefix.length)
        else value.text.replaceRange(lineStart, lineStart, prefix)
    val shift = if (removing) -prefix.length else prefix.length
    return TextFieldValue(newText, TextRange((start + shift).coerceAtLeast(lineStart), (end + shift).coerceAtLeast(lineStart)))
}

private val pairMap = mapOf('[' to ']', '(' to ')', '{' to '}', '<' to '>', '“' to '”', '‘' to '’')

private fun pairedDelete(old: TextFieldValue, next: TextFieldValue): TextFieldValue {
    if (old.composition != null || next.composition != null || !old.selection.collapsed) return next
    val cursor = old.selection.start
    if (cursor <= 0 || next.text.length != old.text.length - 1 ||
        !next.selection.collapsed || next.selection.start != cursor - 1
    ) return next

    val right = old.text.getOrNull(cursor) ?: return next
    if (pairMap[old.text[cursor - 1]] == right) {
        return TextFieldValue(old.text.removeRange(cursor - 1, cursor + 1), TextRange(cursor - 1))
    }
    if (cursor >= 2 && old.text.substring(cursor - 2, cursor) == "[]" && right == '(') {
        val close = old.text.indexOf(')', cursor + 1)
        if (close > cursor) {
            return TextFieldValue(old.text.removeRange(cursor - 2, close + 1), TextRange(cursor - 2))
        }
    }
    return next
}
