package com.dspro.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.Source

@Composable
fun DshHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Dsh.colors.hairline)
    )
}

@Composable
fun DshWordmark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE3F0E5))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Image(
            painter = painterResource(com.dspro.android.R.drawable.bugu_title),
            contentDescription = "布谷笔记",
            modifier = Modifier.height(28.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun DshIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 19.dp,
    tint: Color = Dsh.colors.muted,
) {
    IconButton(onClick = onClick, modifier = modifier.size(size)) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize), tint = tint)
    }
}

@Composable
fun DshPill(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    background: Color = Dsh.colors.chipBg,
    contentColor: Color = Dsh.colors.muted,
    borderColor: Color = Dsh.colors.hairline,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = background,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            leading?.invoke()
            Text(text, color = contentColor, fontSize = 10.5.sp, lineHeight = 14.sp)
            trailing?.invoke()
        }
    }
}

@Composable
fun Avatar(name: String, size: Dp = 30.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF9BD5B2), Color(0xFF557A60)))),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(1), color = Color.White, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SourceRow(source: Source, onClick: () -> Unit) {
    val c = Dsh.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(c.chipBg)
                .border(1.dp, c.hairline, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(source.site.take(2), color = c.subtle, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text(
                source.title,
                color = c.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text("${source.site} · ${source.date}", color = c.subtle, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(13.dp), tint = c.subtle)
    }
}

@Composable
fun ActionIcon(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val c = Dsh.colors
    IconButton(onClick = onClick, modifier = Modifier.size(29.dp)) {
        Icon(
            icon,
            contentDescription,
            Modifier.size(15.5.dp),
            tint = if (active) c.accent else c.subtle,
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val c = Dsh.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.chipBg)
                .border(1.dp, c.hairline, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = c.subtle)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = c.subtle, fontSize = 11.sp, lineHeight = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/* ----------------------------- markdown ----------------------------- */

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    baseSize: TextUnit = 15.sp,
    onWikiLink: ((String) -> Unit)? = null,
    preserveLineBreaks: Boolean = false,
    compact: Boolean = false,
    lazy: Boolean = false,
) {
    val c = Dsh.colors
    val blocks = remember(text, preserveLineBreaks) { MarkdownParser.blocks(text, preserveLineBreaks) }
    val renderBlock: @Composable (Int, MdBlock) -> Unit = { index, block ->
        when (block) {
            is MdBlock.Heading -> {
                val size = when (block.level) {
                    1 -> if (compact) 23.sp else 26.sp
                    2 -> if (compact) 20.sp else 22.sp
                    else -> 17.sp
                }
                Text(
                    rememberInlineMarkdown(block.text, c.accent, c.codeBg, onWikiLink),
                    color = color,
                    fontSize = size,
                    lineHeight = (size.value * 1.35f).sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else if (compact) 9.dp else 18.dp, bottom = if (compact) 4.dp else 8.dp),
                )
            }
            is MdBlock.Paragraph -> {
                InlineMarkdown(
                    text = block.text,
                    accent = c.accent,
                    codeBg = c.codeBg,
                    color = color,
                    fontSize = baseSize,
                    lineHeight = (baseSize.value * if (compact) 1.45f else 1.72f).sp,
                    onWikiLink = onWikiLink,
                    modifier = Modifier.padding(bottom = if (compact) 5.dp else 11.dp),
                )
            }
            is MdBlock.Bullet -> {
                val listLineHeight = (baseSize.value * if (compact) 1.40f else 1.62f).sp
                Row(Modifier.padding(bottom = 5.dp)) {
                    Text(
                        block.marker,
                        color = color,
                        fontSize = baseSize,
                        lineHeight = listLineHeight,
                        modifier = Modifier.alignByBaseline().padding(end = 8.dp),
                    )
                    InlineMarkdown(
                        text = block.text,
                        accent = c.accent,
                        codeBg = c.codeBg,
                        color = color,
                        fontSize = baseSize,
                        lineHeight = listLineHeight,
                        onWikiLink = onWikiLink,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            is MdBlock.Quote -> {
                Row(Modifier.padding(vertical = if (compact) 2.dp else 4.dp).fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.hairlineStrong)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        rememberInlineMarkdown(block.text, c.accent, c.codeBg, onWikiLink),
                        color = c.muted,
                        fontSize = (baseSize.value - 2).sp,
                        lineHeight = (baseSize.value * 1.6f).sp,
                    )
                }
            }
            is MdBlock.Code -> {
                Surface(
                    color = c.codeBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, c.hairline),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(
                        block.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            is MdBlock.Gap -> Spacer(Modifier.height((block.count * 7).coerceAtMost(42).dp))
        }
    }
    if (lazy) {
        LazyColumn(modifier) { itemsIndexed(blocks) { index, block -> renderBlock(index, block) } }
    } else {
        Column(modifier) { blocks.forEachIndexed { index, block -> renderBlock(index, block) } }
    }
}

@Composable
private fun InlineMarkdown(
    text: String,
    accent: Color,
    codeBg: Color,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    onWikiLink: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Text(
        rememberInlineMarkdown(text, accent, codeBg, onWikiLink),
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        modifier = modifier,
    )
}

@Composable
private fun rememberInlineMarkdown(
    text: String,
    accent: Color,
    codeBg: Color,
    onWikiLink: ((String) -> Unit)?,
): AnnotatedString {
    val currentHandler = rememberUpdatedState(onWikiLink)
    return remember(text, accent, codeBg, onWikiLink != null) {
        inlineMarkdown(text, accent, codeBg, if (onWikiLink == null) null else { target ->
            currentHandler.value?.invoke(target)
        })
    }
}

private val inlineTokens = Regex(
    """\\([\\`*_\[\]])|(`+)([^`\n]+)\2(?!`)|\[\[([^\]\n]+)\]\]|\[([^\]\n]+)\]\(([^\s()]+)\)|""" +
        """(?<!\*)(\*{1,3})(?!\*)(?=\S)([^\n]+?)(?<=\S)\7(?!\*)|""" +
        """(?<![\p{L}\p{N}_])(_{1,3})(?!_)(?=\S)([^\n]+?)(?<=\S)\9(?![\p{L}\p{N}_])|\[(\d+)\]"""
)

internal fun inlineMarkdown(
    source: String,
    accent: Color,
    codeBg: Color,
    onWikiLink: ((String) -> Unit)? = null,
    showSyntax: Boolean = false,
    hideMarkers: Boolean = false,
): AnnotatedString = buildAnnotatedString {
    val linkStyle = SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
    val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)

    fun appendMarker(text: String) {
        // Keep one UTF-16 code unit per source unit so TextField offsets remain reversible.
        if (hideMarkers) append("\u200B".repeat(text.length))
        else append(text)
    }

    fun render(text: String, depth: Int) {
        var offset = 0
        if (depth >= 4) {
            append(text)
            return
        }
        inlineTokens.findAll(text).forEach { token ->
            append(text.substring(offset, token.range.first))
            val escaped = token.groups[1]
            val code = token.groups[3]
            val wiki = token.groups[4]
            val link = token.groups[5]
            val marker = token.groups[7] ?: token.groups[9]
            val citation = token.groups[11]
            when {
                escaped != null -> {
                    if (showSyntax && hideMarkers) { appendMarker("\\"); append(escaped.value) }
                    else append(if (showSyntax) token.value else escaped.value)
                }
                code != null -> {
                    if (showSyntax && hideMarkers) {
                        appendMarker(token.value.takeWhile { it == '`' })
                        withStyle(codeStyle) { append(code.value) }
                        appendMarker(token.value.takeLastWhile { it == '`' })
                    } else withStyle(codeStyle) { append(if (showSyntax) token.value else code.value) }
                }
                wiki != null -> {
                    val target = wiki.value.trim()
                    val start = length
                    withStyle(linkStyle) {
                        if (showSyntax && hideMarkers) {
                            appendMarker("[[")
                            append(wiki.value)
                            appendMarker("]]" )
                        } else append(if (showSyntax) token.value else target)
                    }
                    if (!showSyntax && onWikiLink != null && target.isNotEmpty()) {
                        addLink(LinkAnnotation.Clickable(target) { onWikiLink(target) }, start, length)
                    }
                }
                link != null -> {
                    val url = token.groupValues[6]
                    val start = length
                    withStyle(linkStyle) {
                        if (showSyntax && hideMarkers) {
                            appendMarker("[")
                            render(link.value, depth + 1)
                            appendMarker("]($url)")
                        } else if (showSyntax) append(token.value) else render(link.value, depth + 1)
                    }
                    if (!showSyntax && (url.startsWith("https://", true) || url.startsWith("http://", true))) {
                        addLink(LinkAnnotation.Url(url), start, length)
                    }
                }
                marker != null -> {
                    val size = marker.value.length
                    val content = (token.groups[8] ?: token.groups[10])!!.value
                    withStyle(SpanStyle(
                        fontWeight = if (size >= 2) FontWeight.Bold else null,
                        fontStyle = if (size % 2 != 0) FontStyle.Italic else null,
                        fontSynthesis = FontSynthesis.All,
                    )) {
                        if (showSyntax) appendMarker(marker.value)
                        render(content, depth + 1)
                        if (showSyntax) appendMarker(marker.value)
                    }
                }
                citation != null -> withStyle(SpanStyle(color = accent)) {
                    append(if (showSyntax) token.value else citation.value)
                }
            }
            offset = token.range.last + 1
        }
        append(text.substring(offset))
    }
    render(source, 0)
}
