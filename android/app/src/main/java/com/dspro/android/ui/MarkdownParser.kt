package com.dspro.android.ui

/** Shared block scan for preview and the source editor. Source text remains canonical. */
internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val marker: String, val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Gap(val count: Int = 1) : MdBlock
}

internal object MarkdownParser {
    private val ordered = Regex("^[ \\t]*(\\d+[.)])\\s+")
    private val unordered = Regex("^([ \\t]*)([-*+])\\s+")

    fun headingLevel(line: String): Int? {
        val match = Regex("^ {0,3}(#{1,6})\\s+").find(line) ?: return null
        return match.groupValues[1].length
    }
    fun orderedPrefix(line: String): Int? = ordered.find(line)?.range?.last?.plus(1)
    fun unorderedPrefix(line: String): Int? = unordered.find(line)?.range?.last?.plus(1)
    fun quotePrefix(line: String): Int? = line.takeWhile { it == ' ' || it == '\t' }.length
        .takeIf { it <= 3 && line.drop(it).startsWith("> ") }?.plus(2)
    fun isIndentedCode(line: String): Boolean = line.startsWith("    ") || line.startsWith("\t")

    fun blocks(source: String, preserveLineBreaks: Boolean = false): List<MdBlock> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').lines()
        val out = mutableListOf<MdBlock>()
        var i = 0
        fun gap() { if (out.isNotEmpty()) {
            val last = out.lastOrNull()
            if (last !is MdBlock.Gap) out.add(MdBlock.Gap(0))
        } }
        fun blankGap() { if (out.isNotEmpty()) {
            val last = out.lastOrNull()
            if (last is MdBlock.Gap) out[out.lastIndex] = last.copy(count = last.count + 1)
            else out.add(MdBlock.Gap(1))
        } }
        fun fence(line: String): Pair<Char, Int>? {
            if (!isFence(line)) return null
            val marker = line.dropWhile { it == ' ' || it == '\t' }
            val char = marker.firstOrNull()?.takeIf { it == '`' || it == '~' } ?: return null
            val count = marker.takeWhile { it == char }.length
            return if (count >= 3) char to count else null
        }
        while (i < lines.size) {
            val line = lines[i]
            val opening = fence(line)
            when {
                line.isBlank() -> { blankGap(); i++ }
                opening != null -> {
                    val code = StringBuilder(); var hasCodeLine = false; i++
                    while (i < lines.size) {
                        val closing = fence(lines[i])
                        if (closing?.first == opening.first && closing.second >= opening.second &&
                            lines[i].dropWhile { it == ' ' || it == '\t' }.drop(closing.second).isBlank()) { i++; break }
                        if (hasCodeLine) code.append('\n')
                        code.append(lines[i++]); hasCodeLine = true
                    }
                    gap(); out.add(MdBlock.Code(code.toString())); gap()
                }
                isIndentedCode(line) -> {
                    val code = StringBuilder(); var hasCodeLine = false
                    while (i < lines.size && (isIndentedCode(lines[i]) || lines[i].isBlank())) {
                        if (hasCodeLine) code.append('\n')
                        code.append(if (lines[i].startsWith("\t")) lines[i].drop(1) else lines[i].drop(4)); i++
                        hasCodeLine = true
                    }
                    gap(); out.add(MdBlock.Code(code.toString())); gap()
                }
                headingLevel(line) != null -> {
                    val prefix = Regex("^ {0,3}#{1,6}\\s+").find(line)!!
                    val level = headingLevel(line)!!
                    out.add(MdBlock.Heading(level, line.substring(prefix.range.last + 1).trimEnd().replace(Regex("\\s+#+\\s*$"), ""))); i++
                }
                quotePrefix(line) != null -> {
                    val quote = StringBuilder(); var hasQuoteLine = false
                    while (i < lines.size && quotePrefix(lines[i]) != null) {
                        if (hasQuoteLine) quote.append('\n')
                        quote.append(lines[i++].trimStart().removePrefix("> "))
                        hasQuoteLine = true
                    }
                    gap(); out.add(MdBlock.Quote(quote.toString())); gap()
                }
                unordered.containsMatchIn(line) -> {
                    val m = unordered.find(line)!!; out.add(MdBlock.Bullet("•", line.removeRange(m.range.first, m.range.last + 1))); i++
                }
                ordered.containsMatchIn(line) -> {
                    val m = ordered.find(line)!!; out.add(MdBlock.Bullet(m.groupValues[1], line.removeRange(m.range.first, m.range.last + 1))); i++
                }
                else -> {
                    val p = StringBuilder(line); i++
                    while (i < lines.size && lines[i].isNotBlank() && fence(lines[i]) == null &&
                        !isIndentedCode(lines[i]) && headingLevel(lines[i]) == null && quotePrefix(lines[i]) == null &&
                        !unordered.containsMatchIn(lines[i]) && !ordered.containsMatchIn(lines[i])) {
                        p.append(if (preserveLineBreaks) '\n' else ' ').append(lines[i].trim()); i++
                    }
                    out.add(MdBlock.Paragraph(p.toString()))
                }
            }
        }
        while (out.lastOrNull() is MdBlock.Gap) out.removeAt(out.lastIndex)
        return out
    }

    fun isFence(line: String): Boolean {
        val trimmed = line.trimStart()
        if (line.takeWhile { it == ' ' || it == '\t' }.length > 3) return false
        val c = trimmed.firstOrNull() ?: return false
        return (c == '`' || c == '~') && trimmed.takeWhile { it == c }.length >= 3
    }
}
