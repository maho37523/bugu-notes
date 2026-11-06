package com.dspro.android.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.dspro.android.ui.inlineMarkdown

fun main() {
    val original = "# 标题\r\n\r\n**粗体**、_斜体_\r\n\r\n```kotlin\n**代码**\n```\n最后一段  "
    val doc = NoteDocument(original)
    check(doc.readMarkdown() == original)
    doc.edit(doc.value.copy(selection = TextRange(0, original.length)))
    check(!doc.canUndo && doc.revision == 0)
    doc.edit(TextFieldValue("跨段替换\n第二段"), typing = true)
    check(doc.readMarkdown() == "跨段替换\n第二段")
    doc.undo()
    check(doc.readMarkdown() == original && doc.canRedo)
    doc.redo()
    check(doc.readMarkdown() == "跨段替换\n第二段")
    val renderer = NoteLineRenderer(Color.Black, Color.White)
    val rendered = renderer.render(original, 0)
    check(rendered.text.length == original.length)
    check(rendered.text.contains('\u200B'))
    check(rendered.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    check(rendered.spanStyles.none { it.item.color == Color.Transparent })
    check(rendered.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
    check(renderer.render("**粗体**", 0).let { it.text == "**粗体**" && it.spanStyles.none { style -> style.item.color == Color.Transparent } })
    val inlineSource = "**粗** `code` [link](https://example.org) [[笔记]]"
    val inlineHidden = inlineMarkdown(inlineSource, Color.Black, Color.White, showSyntax = true, hideMarkers = true)
    check(inlineHidden.length == inlineSource.length && inlineHidden.text.contains("粗") && inlineHidden.text.contains("code") && inlineHidden.text.contains("link"))
    check(!inlineHidden.text.contains("**") && !inlineHidden.text.contains("https://example.org"))
    val listSource = "- 第一项\n  * 缩进项\n1. 第一序号\n2) 第二序号\n算式 a - b\n---\n```\n- 代码里的标记\n```"
    val listDoc = NoteDocument(listSource)
    val listRenderer = NoteLineRenderer(Color.Black, Color.White)
    val inactiveLists = listRenderer.render(listSource, listSource.length)
    val activeLists = listRenderer.render(listSource, 0)
    check(inactiveLists.text.startsWith("• 第一项\n  • 缩进项\n1. 第一序号\n2) 第二序号"))
    check(activeLists.text.startsWith("• 第一项\n  • 缩进项\n1. 第一序号\n2) 第二序号"))
    check(inactiveLists.text.contains("算式 a - b\n---\n```\n- 代码里的标记"))
    check(inactiveLists.text.contains("算式 a - b\n---\n```\n- 代码里的标记"))
    val quote = listRenderer.render("> 引用内容", -1)
    check(quote.length == "> 引用内容".length && quote.text.replace("\u200B", "").contains("引用内容"))
    check(inactiveLists.spanStyles.none { it.item.color == Color.Transparent && it.start <= 0 && it.end > 0 })
    listDoc.edit(listDoc.value.copy(selection = TextRange(2, 5)))
    check(listDoc.readMarkdown() == listSource && listDoc.value.selection == TextRange(2, 5))
    val unicode = "# 标题 😀\n**中文🙂**\n- 结束"
    val unicodeRendered = listRenderer.render(unicode, -1)
    check(unicodeRendered.text.length == unicode.length)
    check(unicodeRendered.text.replace("\u200B", "").contains("中文🙂"))
    check(NoteDocument(unicode).readMarkdown() == unicode)
    val unicodeDoc = NoteDocument(unicode)
    unicodeDoc.edit(unicodeDoc.value.copy(selection = TextRange(unicode.length, 2)))
    check(unicodeDoc.value.selection == TextRange(unicode.length, 2))
    val long = (1..4000).joinToString("\n\n") { "段落 $it，含 **粗体**  " }
    val renderedLong = renderer.render(long, 0)
    check(renderedLong.text.length == long.length && renderedLong.text.contains('\u200B'))
    for (offset in 0..long.length step 97) {
        check(androidx.compose.ui.text.input.OffsetMapping.Identity.originalToTransformed(offset) == offset)
        check(androidx.compose.ui.text.input.OffsetMapping.Identity.transformedToOriginal(offset) == offset)
    }
    check(NoteDocument(long).readMarkdown() == long)
    println("PASS: document-wide selection, edit/undo/redo, formatting, live bullets and ordered lists, code fences, exact source/selection and 4,000 paragraphs")
}
