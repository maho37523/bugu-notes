package com.dspro.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import com.dspro.android.data.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    fun preview(text: String, raw: Boolean = false) =
        inlineMarkdown(text, Color.Black, Color.White, showSyntax = raw)
    val bold = preview("这是 **中文粗体** 和 __另一段__。")
    check(bold.text == "这是 中文粗体 和 另一段。")
    check(bold.spanStyles.count { it.item.fontWeight == FontWeight.Bold } == 2)
    val combined = preview("***粗斜体***")
    check(combined.text == "粗斜体")
    check(combined.spanStyles.single().item.fontWeight == FontWeight.Bold)
    check(combined.spanStyles.single().item.fontStyle == FontStyle.Italic)
    val mixed = "   **粗体**\r\n*斜体* [官网](https://example.org) [[双链]] `**代码**` \\*"
    val raw = preview(mixed, true)
    check(raw.text == mixed)
    check(raw.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    check(raw.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    check(preview(mixed).text == "   粗体\r\n斜体 官网 双链 **代码** *")
    check(preview("***** **未闭合").text == "***** **未闭合")
    println("PASS: preview and edit spans, Chinese bold, combined emphasis, links, code, escapes, incomplete input")
    val parsed = MarkdownParser.blocks("- bullet\n  + indented\n  3. numbered\n2) also numbered", true)
    val bulletBlocks = parsed.filterIsInstance<MdBlock.Bullet>()
    check(bulletBlocks.map { it.marker } == listOf("•", "•", "3.", "2)"))
    check(bulletBlocks.map { it.text } == listOf("bullet", "indented", "numbered", "also numbered"))
    println("PASS: Markdown preview parses indented unordered and ordered list markers without losing text")
    if (args.isNotEmpty()) runBlocking {
        for (level in ThinkingEffort.entries) {
            val config = ApiConfig(apiKey = "test-only", baseUrl = args.single(), effort = level)
            check(DeepSeekClient(config).stream(listOf(ChatTurn("user", "验证"))).toList() == listOf(StreamEvent.Done))
        }
        println("PASS: client sent four HTTP requests to local mock")
    }
}
