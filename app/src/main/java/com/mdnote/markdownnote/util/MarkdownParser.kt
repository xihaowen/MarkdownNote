package com.mdnote.markdownnote.util

/**
 * 大纲项 - 用于目录显示
 */
data class OutlineItem(
    val level: Int,
    val text: String,
    val lineIndex: Int
)

/**
 * 表格对齐方式
 */
enum class TableAlignment {
    LEFT, CENTER, RIGHT
}

/**
 * 应用支持的 Markdown 元素类型。
 */
sealed class MarkdownElement {
    // YAML Front Matter
    data class YamlFrontMatter(val content: String) : MarkdownElement()

    // 标题 (H1-H6) - 支持行内格式
    data class Heading(val level: Int, val text: String, val spans: List<Span> = emptyList()) : MarkdownElement()

    // 段落
    data class Paragraph(val spans: List<Span>) : MarkdownElement()

    // 无序列表
    data class UnorderedList(val items: List<List<Span>>) : MarkdownElement()

    // 有序列表
    data class OrderedList(val items: List<List<Span>>, val startIndex: Int = 1) : MarkdownElement()

    // 任务列表
    data class TaskList(val items: List<TaskItem>) : MarkdownElement()

    data class TaskItem(val checked: Boolean, val spans: List<Span>)

    // 代码块
    data class CodeBlock(val code: String, val language: String = "") : MarkdownElement()

    // Mermaid 图表
    data class MermaidBlock(val code: String) : MarkdownElement()

    // 引用块
    data class BlockQuote(val elements: List<MarkdownElement>) : MarkdownElement()

    // 分割线
    data object HorizontalRule : MarkdownElement()

    // 图片
    data class Image(val altText: String, val url: String, val title: String = "") : MarkdownElement()

    // 链接
    data class Link(val text: String, val url: String) : MarkdownElement()

    // 表格
    data class Table(
        val headers: List<String>,
        val alignments: List<TableAlignment>,
        val rows: List<List<String>>
    ) : MarkdownElement()

    // 块级数学公式
    data class MathBlock(val latex: String) : MarkdownElement()

}

/**
 * 行内样式元素（支持嵌套）
 */
sealed class Span {
    data class PlainText(val text: String) : Span()
    data class Bold(val spans: List<Span>) : Span()
    data class Italic(val spans: List<Span>) : Span()
    data class BoldItalic(val spans: List<Span>) : Span()
    data class Code(val text: String) : Span()
    data class Link(val text: String, val url: String) : Span()
    data class Math(val latex: String) : Span()
    data class Highlight(val spans: List<Span>) : Span()       // ==高亮==
    data class Strikethrough(val spans: List<Span>) : Span()   // ~~删除线~~
    data class Subscript(val text: String) : Span()       // H~2~O
    data class Superscript(val text: String) : Span()     // X^2^
    data class Image(val altText: String, val url: String, val title: String = "") : Span()
    data class FootnoteRef(val label: String) : Span()
    data class Colored(val color: String, val spans: List<Span>) : Span()  // <font color="red">text</font>
    data class Underline(val spans: List<Span>) : Span()  // <u>text</u>
    data class Emoji(val emoji: String) : Span()  // :smile: -> 😄
}

/**
 * 应用内置 Markdown 解析器。
 */
object MarkdownParser {

    // Emoji shortcode 映射表（常用 Emoji）
    private val emojiMap = mapOf(
        // 表情
        "smile" to "😄", "smiley" to "😃", "grinning" to "😀", "blush" to "😊", "wink" to "😉",
        "heart_eyes" to "😍", "kissing_heart" to "😘", "stuck_out_tongue" to "😛", "stuck_out_tongue_winking_eye" to "😜",
        "grin" to "😁", "joy" to "😂", "rofl" to "🤣", "smirk" to "😏", "unamused" to "😒",
        "sweat_smile" to "😅", "laughing" to "😆", "yum" to "😋", "sunglasses" to "😎", "hugging_face" to "🤗",

        // 爱心和符号
        "heart" to "❤️", "red_heart" to "❤️", "blue_heart" to "💙", "green_heart" to "💚",
        "yellow_heart" to "💛", "purple_heart" to "💜", "orange_heart" to "🧡", "broken_heart" to "💔",
        "heartpulse" to "💗", "sparkling_heart" to "💖", "two_hearts" to "💕", "love_letter" to "💌",
        "star" to "⭐", "star2" to "🌟", "stars" to "🌠", "sparkles" to "✨", "zap" to "⚡",
        "fire" to "🔥", "boom" to "💥", "exclamation" to "❗", "question" to "❓", "grey_question" to "❔",
        "check" to "✅", "white_check_mark" to "✅", "x" to "❌", "negative_squared_cross_mark" to "❎",
        "plus" to "➕", "minus" to "➖", "infinity" to "♾️", "copyright" to "©️", "registered" to "®️",
        "tm" to "™️", "hash" to "#️⃣", "asterisk" to "*️⃣",

        // 手势
        "thumbsup" to "👍", "thumbsdown" to "👎", "ok_hand" to "👌", "v" to "✌️", "peace" to "✌️",
        "clap" to "👏", "raised_hands" to "🙌", "fist" to "✊", "punch" to "👊", "wave" to "👋",
        "hand" to "✋", "muscle" to "💪", "point_up" to "☝️", "point_down" to "👇", "point_left" to "👈", "point_right" to "👉",

        // 动物
        "dog" to "🐕", "cat" to "🐱", "mouse" to "🐭", "hamster" to "🐹", "rabbit" to "🐰",
        "fox" to "🦊", "bear" to "🐻", "panda" to "🐼", "koala" to "🐨", "tiger" to "🐯",
        "lion" to "🦁", "cow" to "🐮", "pig" to "🐷", "frog" to "🐸", "monkey" to "🐒",
        "chicken" to "🐔", "penguin" to "🐧", "bird" to "🐦", "duck" to "🦆", "eagle" to "🦅",
        "butterfly" to "🦋", "bug" to "🐛", "bee" to "🐝", "ant" to "🐜", "spider" to "🕷️",

        // 自然
        "sunny" to "☀️", "sun" to "☀️", "moon" to "🌙", "full_moon" to "🌕", "crescent_moon" to "🌙",
        "cloud" to "☁️", "rainbow" to "🌈", "umbrella" to "☔", "snowflake" to "❄️", "snowman" to "⛄",
        "tulip" to "🌷", "rose" to "🌹", "sunflower" to "🌻", "blossom" to "🌸", "cherry_blossom" to "🌸",
        "tree" to "🌳", "evergreen_tree" to "🌲", "cactus" to "🌵", "palm_tree" to "🌴", "maple_leaf" to "🍁",
        "leaves" to "🍃", "four_leaf_clover" to "🍀", "seedling" to "🌱",

        // 食物
        "apple" to "🍎", "banana" to "🍌", "watermelon" to "🍉", "grapes" to "🍇", "strawberry" to "🍓",
        "peach" to "🍑", "cherries" to "🍒", "pineapple" to "🍍", "lemon" to "🍋", "orange" to "🍊",
        "pizza" to "🍕", "hamburger" to "🍔", "fries" to "🍟", "hotdog" to "🌭", "sandwich" to "🥪",
        "taco" to "🌮", "burrito" to "🌯", "rice" to "🍚", "ramen" to "🍜", "spaghetti" to "🍝",
        "bread" to "🍞", "cheese" to "🧀", "egg" to "🥚", "bacon" to "🥓", "icecream" to "🍦",
        "cake" to "🍰", "cookie" to "🍪", "chocolate" to "🍫", "candy" to "🍬", "lollipop" to "🍭",
        "coffee" to "☕", "tea" to "🍵", "beer" to "🍺", "beers" to "🍻", "wine" to "🍷", "cocktail" to "🍸",

        // 物品
        "book" to "📖", "books" to "📚", "notebook" to "📓", "pencil" to "✏️", "pen" to "🖊️",
        "memo" to "📝", "bulb" to "💡", "flashlight" to "🔦", "gift" to "🎁", "balloon" to "🎈",
        "tada" to "🎉", "confetti_ball" to "🎊", "bell" to "🔔", "key" to "🔑", "lock" to "🔒",
        "gem" to "💎", "ring" to "💍", "crown" to "👑", "trophy" to "🏆", "medal" to "🏅",
        "computer" to "💻", "desktop" to "🖥️", "laptop" to "💻", "phone" to "📱", "iphone" to "📱",
        "keyboard" to "⌨️", "mouse" to "🖱️", "camera" to "📷", "video_camera" to "📹", "tv" to "📺",
        "radio" to "📻", "headphones" to "🎧", "microphone" to "🎤", "music" to "🎵", "notes" to "🎶",
        "guitar" to "🎸", "piano" to "🎹", "drum" to "🥁", "violin" to "🎻", "saxophone" to "🎷",

        // 交通
        "car" to "🚗", "taxi" to "🚕", "bus" to "🚌", "train" to "🚆", "metro" to "🚇",
        "airplane" to "✈️", "helicopter" to "🚁", "rocket" to "🚀", "ship" to "🚢", "boat" to "⛵",
        "bicycle" to "🚲", "motorcycle" to "🏍️", "fuel_pump" to "⛽",

        // 建筑
        "house" to "🏠", "home" to "🏠", "office" to "🏢", "school" to "🏫", "hospital" to "🏥",
        "bank" to "🏦", "hotel" to "🏨", "church" to "⛪", "mosque" to "🕌", "synagogue" to "🕍",
        "statue_of_liberty" to "🗽", "tokyo_tower" to "🗼", "eiffel_tower" to "🗼", "castle" to "🏰",

        // 国旗
        "cn" to "🇨🇳", "china" to "🇨🇳", "us" to "🇺🇸", "usa" to "🇺🇸", "uk" to "🇬🇧",
        "jp" to "🇯🇵", "japan" to "🇯🇵", "kr" to "🇰🇷", "korea" to "🇰🇷", "de" to "🇩🇪",
        "fr" to "🇫🇷", "france" to "🇫🇷", "it" to "🇮🇹", "italy" to "🇮🇹", "es" to "🇪🇸",
        "ru" to "🇷🇺", "russia" to "🇷🇺", "au" to "🇦🇺", "australia" to "🇦🇺", "ca" to "🇨🇦",
        "canada" to "🇨🇦", "br" to "🇧🇷", "brazil" to "🇧🇷", "in" to "🇮🇳", "india" to "🇮🇳",

        // 其他常用
        "ok" to "👌", "yes" to "✅", "no" to "❌", "cool" to "😎", "good" to "👍", "bad" to "👎",
        "idea" to "💡", "warning" to "⚠️", "info" to "ℹ️", "help" to "❓", "new" to "🆕",
        "hot" to "🔥", "cold" to "❄️", "fast" to "⚡", "slow" to "🐢", "up" to "⬆️", "down" to "⬇️",
        "left" to "⬅️", "right" to "➡️", "back" to "🔙", "end" to "🔚", "on" to "🔛", "top" to "🔝",
        "soon" to "🔜", "free" to "🆓", "vip" to "👑", "money" to "💰", "dollar" to "💵", "yen" to "💴",
        "euro" to "💶", "pound" to "💷", "chart" to "📊", "calendar" to "📅", "clock" to "🕐",
        "hourglass" to "⏳", "watch" to "⌚", "alarm_clock" to "⏰", "timer" to "⏲️"
    )
    // 块级结构检测（用于 parse 主循环与段落解析）
    private val TASK_LIST_DETECT_PATTERN = Regex("^[-*+]\\s+\\[[ xX]\\]")
    private val UNORDERED_LIST_DETECT_PATTERN = Regex("^[-*+]\\s")
    private val ORDERED_LIST_DETECT_PATTERN = Regex("^\\d+\\.\\s")
    private val HEADING_PATTERN = Regex("^(#{1,6})\\s+(.+)$")
    private val STANDALONE_IMAGE_PATTERN = Regex("""^!\[([^\]]*)\]\(([^)]+)(?:\s+"([^"]+)")?\)\s*$""")
    private val UNORDERED_LIST_ITEM_PATTERN = Regex("^[-*+]\\s+(.*)$")
    private val ORDERED_LIST_ITEM_PATTERN = Regex("^(\\d+)\\.\\s+(.*)$")
    private val TASK_LIST_ITEM_PATTERN = Regex("^[-*+]\\s+\\[([ xX])\\]\\s*(.*)$")
    private val FONT_TAG_PATTERN = Regex("""<font\s+color\s*=\s*["']?([^"'\s>]+)["']?\s*>""", RegexOption.IGNORE_CASE)
    private val INLINE_IMAGE_PATTERN = Regex("""!\[([^\]]*)\]\(([^)]+)(?:\s+"([^"]+)")?\)""")

    // stripMarkdownFormatting 使用的替换模式
    private val STRIP_BOLD_ITALIC_PATTERN = Regex("\\*\\*\\*(.+?)\\*\\*\\*")
    private val STRIP_BOLD_ASTERISK_PATTERN = Regex("\\*\\*(.+?)\\*\\*")
    private val STRIP_BOLD_UNDERSCORE_PATTERN = Regex("__(.+?)__")
    private val STRIP_ITALIC_ASTERISK_PATTERN = Regex("\\*(.+?)\\*")
    private val STRIP_ITALIC_UNDERSCORE_PATTERN = Regex("_(.+?)_")
    private val STRIP_HIGHLIGHT_PATTERN = Regex("==(.+?)==")
    private val STRIP_STRIKETHROUGH_PATTERN = Regex("~~(.+?)~~")
    private val STRIP_INLINE_CODE_PATTERN = Regex("`(.+?)`")
    private val STRIP_INLINE_MATH_PATTERN = Regex("\\$(.+?)\\$")
    private val STRIP_FONT_TAG_PATTERN = Regex("<font[^>]*>(.+?)</font>")
    private val STRIP_B_TAG_PATTERN = Regex("<b>(.+?)</b>", RegexOption.IGNORE_CASE)
    private val STRIP_I_TAG_PATTERN = Regex("<i>(.+?)</i>", RegexOption.IGNORE_CASE)
    private val STRIP_U_TAG_PATTERN = Regex("<u>(.+?)</u>", RegexOption.IGNORE_CASE)
    private val STRIP_STRONG_TAG_PATTERN = Regex("<strong>(.+?)</strong>", RegexOption.IGNORE_CASE)
    private val STRIP_EM_TAG_PATTERN = Regex("<em>(.+?)</em>", RegexOption.IGNORE_CASE)
    private val STRIP_MARK_TAG_PATTERN = Regex("<mark>(.+?)</mark>", RegexOption.IGNORE_CASE)
    private val STRIP_DEL_TAG_PATTERN = Regex("<del>(.+?)</del>", RegexOption.IGNORE_CASE)
    private val STRIP_S_TAG_PATTERN = Regex("<s>(.+?)</s>", RegexOption.IGNORE_CASE)
    private val STRIP_CODE_TAG_PATTERN = Regex("<code>(.+?)</code>", RegexOption.IGNORE_CASE)
    private val STRIP_SUB_TAG_PATTERN = Regex("<sub>(.+?)</sub>", RegexOption.IGNORE_CASE)
    private val STRIP_SUP_TAG_PATTERN = Regex("<sup>(.+?)</sup>", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<MarkdownElement> {
        val lines = text.lines()
        val elements = mutableListOf<MarkdownElement>()
        var i = 0

        // 解析 YAML Front Matter
        if (lines.isNotEmpty() && lines[0].trim() == "---") {
            var endIndex = -1
            for (idx in 1 until lines.size) {
                if (lines[idx].trim() == "---") {
                    endIndex = idx
                    break
                }
            }
            if (endIndex > 0) {
                val yamlContent = lines.subList(1, endIndex).joinToString("\n")
                elements.add(MarkdownElement.YamlFrontMatter(yamlContent))
                i = endIndex + 1
            }
        }

        while (i < lines.size) {
            val line = lines[i]

            when {
                line.isBlank() -> { i++ }

                line.startsWith("#") -> {
                    parseHeading(line)?.let { elements.add(it) }
                    i++
                }

                isHorizontalRule(line) -> {
                    elements.add(MarkdownElement.HorizontalRule)
                    i++
                }

                line.trim().startsWith("$$") -> {
                    val (block, end) = parseMathBlock(lines, i)
                    elements.add(block)
                    i = end + 1
                }

                line.startsWith("```") || line.startsWith("~~~") -> {
                    val (block, end) = parseCodeBlock(lines, i)
                    // 如果是 Mermaid 图表，添加 MermaidBlock
                    if (block.language.lowercase() == "mermaid") {
                        elements.add(MarkdownElement.MermaidBlock(block.code))
                    } else {
                        elements.add(block)
                    }
                    i = end + 1
                }

                line.startsWith(">") -> {
                    val (block, end) = parseBlockQuote(lines, i)
                    elements.add(block)
                    i = end + 1
                }

                line.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1]) -> {
                    val (table, end) = parseTable(lines, i)
                    elements.add(table)
                    i = end + 1
                }
                TASK_LIST_DETECT_PATTERN.containsMatchIn(line.trimStart()) -> {
                    val (items, end) = parseTaskList(lines, i)
                    elements.add(MarkdownElement.TaskList(items))
                    i = end + 1
                }

                UNORDERED_LIST_DETECT_PATTERN.containsMatchIn(line.trimStart()) -> {
                    val (items, end) = parseUnorderedList(lines, i)
                    elements.add(MarkdownElement.UnorderedList(items))
                    i = end + 1
                }

                ORDERED_LIST_DETECT_PATTERN.containsMatchIn(line.trimStart()) -> {
                    val (items, end) = parseOrderedList(lines, i)
                    elements.add(MarkdownElement.OrderedList(items))
                    i = end + 1
                }

                else -> {
                    // 检测独立图片行
                    if (isStandaloneImage(line)) {
                        parseStandaloneImage(line)?.let { elements.add(it) }
                        i++
                    } else {
                        val (paraLines, end) = parseParagraphLines(lines, i)
                        elements.add(MarkdownElement.Paragraph(parseInlineSpans(paraLines.joinToString("\n"))))
                        i = end + 1
                    }
                }
            }
        }

        return elements
    }

    private fun parseHeading(line: String): MarkdownElement.Heading? {
        val match = HEADING_PATTERN.find(line) ?: return null
        val level = match.groupValues[1].length
        val text = match.groupValues[2].trim()
        // 解析标题中的行内格式
        val spans = parseInlineSpans(text)
        return MarkdownElement.Heading(level, text, spans)
    }

    private fun isHorizontalRule(line: String): Boolean {
        val t = line.trim()
        return t.isNotEmpty() && t.length >= 3 && (t.all { it == '-' } || t.all { it == '*' } || t.all { it == '_' })
    }

    /**
     * 检测一行是否为独立的图片行
     * 格式: ![alt](url) 或 ![alt](url "title")
     */
    private fun isStandaloneImage(line: String): Boolean {
        val trimmed = line.trim()
        // 匹配独立的图片语法，整行只有图片没有其他内容
        return STANDALONE_IMAGE_PATTERN.matches(trimmed)
    }

    /**
     * 解析独立图片行
     */
    private fun parseStandaloneImage(line: String): MarkdownElement.Image? {
        val trimmed = line.trim()
        val match = STANDALONE_IMAGE_PATTERN.find(trimmed) ?: return null
        return MarkdownElement.Image(
            altText = match.groupValues[1],
            url = match.groupValues[2].trim(),
            title = match.groupValues.getOrElse(3) { "" }
        )
    }

    private fun isTableSeparator(line: String): Boolean {
        val t = line.trim()
        return t.isNotEmpty() && t.all { it == '|' || it == '-' || it == ' ' || it == ':' } && t.contains("|")
    }

    private fun parseCodeBlock(lines: List<String>, start: Int): Pair<MarkdownElement.CodeBlock, Int> {
        val delim = if (lines[start].startsWith("```")) "```" else "~~~"
        val lang = lines[start].removePrefix(delim).trim()
        val code = mutableListOf<String>()
        var i = start + 1
        while (i < lines.size && !lines[i].startsWith(delim)) {
            // 将 Tab 字符替换为 4 个空格，避免显示为方框
            code.add(lines[i].replace("\t", "    "))
            i++
        }
        return MarkdownElement.CodeBlock(code.joinToString("\n"), lang) to i
    }

    private fun parseMathBlock(lines: List<String>, start: Int): Pair<MarkdownElement.MathBlock, Int> {
        val first = lines[start].trim()
        if (first.startsWith("$$") && first.endsWith("$$") && first.length > 4) {
            return MarkdownElement.MathBlock(first.removePrefix("$$").removeSuffix("$$").trim()) to start
        }
        val latex = mutableListOf<String>()
        var i = start + 1
        while (i < lines.size && !lines[i].trim().endsWith("$$")) { latex.add(lines[i]); i++ }
        return MarkdownElement.MathBlock(latex.joinToString("\n")) to i
    }

    private fun parseBlockQuote(lines: List<String>, start: Int): Pair<MarkdownElement.BlockQuote, Int> {
        val quoteLines = mutableListOf<String>()
        var i = start
        while (i < lines.size && lines[i].trimStart().startsWith(">")) {
            quoteLines.add(lines[i].removePrefix(">").removePrefix(" ").trim()); i++
        }
        return MarkdownElement.BlockQuote(parse(quoteLines.joinToString("\n"))) to i - 1
    }

    private fun parseUnorderedList(lines: List<String>, start: Int): Pair<List<List<Span>>, Int> {
        val items = mutableListOf<List<Span>>()
        var i = start
        while (i < lines.size) {
            val match = UNORDERED_LIST_ITEM_PATTERN.find(lines[i].trimStart())
            if (match != null) { items.add(parseInlineSpans(match.groupValues[1].trim())); i++ }
            else if (lines[i].isBlank()) break else break
        }
        return items to i - 1
    }

    private fun parseOrderedList(lines: List<String>, start: Int): Pair<List<List<Span>>, Int> {
        val items = mutableListOf<List<Span>>()
        var i = start
        while (i < lines.size) {
            val match = ORDERED_LIST_ITEM_PATTERN.find(lines[i].trimStart())
            if (match != null) { items.add(parseInlineSpans(match.groupValues[2].trim())); i++ }
            else if (lines[i].isBlank()) break else break
        }
        return items to i - 1
    }

    private fun parseTaskList(lines: List<String>, start: Int): Pair<List<MarkdownElement.TaskItem>, Int> {
        val items = mutableListOf<MarkdownElement.TaskItem>()
        var i = start
        while (i < lines.size) {
            val match = TASK_LIST_ITEM_PATTERN.find(lines[i].trimStart())
            if (match != null) {
                items.add(MarkdownElement.TaskItem(
                    match.groupValues[1].lowercase() == "x",
                    parseInlineSpans(match.groupValues[2].trim())
                )); i++
            } else if (lines[i].isBlank()) break else break
        }
        return items to i - 1
    }

    private fun parseParagraphLines(lines: List<String>, start: Int): Pair<List<String>, Int> {
        val para = mutableListOf<String>()
        var i = start
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.startsWith("#") || line.startsWith("```") || line.startsWith("~~~") ||
                line.startsWith(">") || isHorizontalRule(line) ||
                UNORDERED_LIST_DETECT_PATTERN.containsMatchIn(line.trimStart()) ||
                ORDERED_LIST_DETECT_PATTERN.containsMatchIn(line.trimStart())) break
            para.add(line); i++
        }
        return para to i - 1
    }

    private fun parseTable(lines: List<String>, start: Int): Pair<MarkdownElement.Table, Int> {
        val headers = lines[start].trim().split("|").filter { it.isNotEmpty() }.map { it.trim() }
        val aligns = lines[start + 1].trim().split("|").filter { it.isNotEmpty() }.map { cell ->
            val t = cell.trim()
            when { t.startsWith(":") && t.endsWith(":") -> TableAlignment.CENTER
                t.endsWith(":") -> TableAlignment.RIGHT
                else -> TableAlignment.LEFT }
        }
        val rows = mutableListOf<List<String>>()
        var i = start + 2
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || !line.contains("|")) break
            val cells = line.split("|").filter { it.isNotEmpty() || it == "" }.map { it.trim() }
            if (cells.size == headers.size) rows.add(cells)
            i++
        }
        return MarkdownElement.Table(headers, aligns, rows) to i - 1
    }

    fun parseInlineSpans(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        var i = 0
        val len = text.length

        while (i < len) {
            // 检测 HTML 标签：<font color="xxx">, <u>, </font>, </u>
            if (text[i] == '<') {
                // 检测 <font color="xxx"> 或 <font color='xxx'> 或 <font color=xxx>（支持无引号）
                val fontMatch = FONT_TAG_PATTERN.find(text.substring(i))
                if (fontMatch != null && fontMatch.range.first == 0) {
                    val color = fontMatch.groupValues[1]
                    val tagLen = fontMatch.value.length
                    val endTag = text.indexOf("</font>", i + tagLen, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + tagLen, endTag)
                        spans.add(Span.Colored(color, parseInlineSpans(innerText)))
                        i = endTag + 7  // </font> 长度为 7
                        continue
                    }
                }

                // 检测 <u> 下划线
                if (text.substring(i).lowercase().startsWith("<u>")) {
                    val endTag = text.indexOf("</u>", i + 3, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 3, endTag)
                        spans.add(Span.Underline(parseInlineSpans(innerText)))
                        i = endTag + 4  // </u> 长度为 4
                        continue
                    }
                }

                // 检测扩展语法 <mark> 高亮。
                if (text.substring(i).lowercase().startsWith("<mark>")) {
                    val endTag = text.indexOf("</mark>", i + 6, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 6, endTag)
                        spans.add(Span.Highlight(parseInlineSpans(innerText)))
                        i = endTag + 7  // </mark> 长度为 7
                        continue
                    }
                }

                // 检测 <b> 粗体
                if (text.substring(i).lowercase().startsWith("<b>")) {
                    val endTag = text.indexOf("</b>", i + 3, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 3, endTag)
                        spans.add(Span.Bold(parseInlineSpans(innerText)))
                        i = endTag + 4
                        continue
                    }
                }

                // 检测 <i> 或 <em> 斜体
                if (text.substring(i).lowercase().startsWith("<i>")) {
                    val endTag = text.indexOf("</i>", i + 3, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 3, endTag)
                        spans.add(Span.Italic(parseInlineSpans(innerText)))
                        i = endTag + 4
                        continue
                    }
                }
                if (text.substring(i).lowercase().startsWith("<em>")) {
                    val endTag = text.indexOf("</em>", i + 4, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 4, endTag)
                        spans.add(Span.Italic(parseInlineSpans(innerText)))
                        i = endTag + 5
                        continue
                    }
                }

                // 检测 <strong> 粗体
                if (text.substring(i).lowercase().startsWith("<strong>")) {
                    val endTag = text.indexOf("</strong>", i + 8, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 8, endTag)
                        spans.add(Span.Bold(parseInlineSpans(innerText)))
                        i = endTag + 9
                        continue
                    }
                }

                // 检测 <del> 或 <s> 删除线
                if (text.substring(i).lowercase().startsWith("<del>")) {
                    val endTag = text.indexOf("</del>", i + 5, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 5, endTag)
                        spans.add(Span.Strikethrough(parseInlineSpans(innerText)))
                        i = endTag + 6
                        continue
                    }
                }
                if (text.substring(i).lowercase().startsWith("<s>")) {
                    val endTag = text.indexOf("</s>", i + 3, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 3, endTag)
                        spans.add(Span.Strikethrough(parseInlineSpans(innerText)))
                        i = endTag + 4
                        continue
                    }
                }

                // 检测 <code> 行内代码
                if (text.substring(i).lowercase().startsWith("<code>")) {
                    val endTag = text.indexOf("</code>", i + 6, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 6, endTag)
                        spans.add(Span.Code(innerText))
                        i = endTag + 7
                        continue
                    }
                }

                // 检测 <sub> 下标
                if (text.substring(i).lowercase().startsWith("<sub>")) {
                    val endTag = text.indexOf("</sub>", i + 5, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 5, endTag)
                        spans.add(Span.Subscript(innerText))
                        i = endTag + 6
                        continue
                    }
                }

                // 检测 <sup> 上标
                if (text.substring(i).lowercase().startsWith("<sup>")) {
                    val endTag = text.indexOf("</sup>", i + 5, ignoreCase = true)
                    if (endTag > 0) {
                        val innerText = text.substring(i + 5, endTag)
                        spans.add(Span.Superscript(innerText))
                        i = endTag + 6
                        continue
                    }
                }

                // 检测自动链接 <url> 或 <email>
                if (text[i] == '<') {
                    val endAngle = text.indexOf('>', i + 1)
                    if (endAngle > 0) {
                        val content = text.substring(i + 1, endAngle)
                        // 检测 URL
                        if (content.startsWith("http://") || content.startsWith("https://") ||
                            content.startsWith("ftp://") || content.startsWith("www.")) {
                            val url = if (content.startsWith("www.")) "https://$content" else content
                            spans.add(Span.Link(content, url))
                            i = endAngle + 1
                            continue
                        }
                        // 检测邮箱
                        if (content.contains("@") && content.contains(".")) {
                            spans.add(Span.Link(content, "mailto:$content"))
                            i = endAngle + 1
                            continue
                        }
                    }
                }
            }

            // 检测 Emoji shortcode :smile: :heart: 等
            if (text[i] == ':') {
                val endColon = text.indexOf(':', i + 1)
                if (endColon > 0 && endColon > i + 1) {
                    val emojiCode = text.substring(i + 1, endColon)
                    val emoji = emojiMap[emojiCode.lowercase()]
                    if (emoji != null) {
                        spans.add(Span.Emoji(emoji))
                        i = endColon + 1
                        continue
                    }
                }
            }

            // 检测图片
            if (i + 1 < len && text[i] == '!' && text[i + 1] == '[') {
                val imgEnd = text.indexOf(')', i + 2)
                if (imgEnd > 0) {
                    val match = INLINE_IMAGE_PATTERN.find(text.substring(i))
                    if (match != null && match.range.first == 0) {
                        spans.add(Span.Image(match.groupValues[1], match.groupValues[2], match.groupValues.getOrElse(3) { "" }))
                        i += match.value.length
                        continue
                    }
                }
            }

            // 检测粗斜体 ***text*** 或 ___text___（支持嵌套）
            if (i + 2 < len && ((text[i] == '*' && text[i + 1] == '*' && text[i + 2] == '*') ||
                    (text[i] == '_' && text[i + 1] == '_' && text[i + 2] == '_'))) {
                val delimiter = text[i].toString().repeat(3)
                val endIdx = findMatchingDelimiter(text, delimiter, i + 3)
                if (endIdx > 0) {
                    val innerText = text.substring(i + 3, endIdx)
                    spans.add(Span.BoldItalic(parseInlineSpans(innerText)))
                    i = endIdx + 3
                    continue
                }
            }

            // 检测粗体 **text** 或 __text__（支持嵌套）
            if (i + 1 < len && ((text[i] == '*' && text[i + 1] == '*') ||
                    (text[i] == '_' && text[i + 1] == '_'))) {
                val delimiter = text[i].toString().repeat(2)
                val endIdx = findMatchingDelimiter(text, delimiter, i + 2)
                if (endIdx > 0) {
                    val innerText = text.substring(i + 2, endIdx)
                    spans.add(Span.Bold(parseInlineSpans(innerText)))
                    i = endIdx + 2
                    continue
                }
            }

            // 检测高亮 ==text==（支持嵌套）
            if (i + 1 < len && text[i] == '=' && text[i + 1] == '=') {
                val endIdx = findMatchingDelimiter(text, "==", i + 2)
                if (endIdx > 0) {
                    val innerText = text.substring(i + 2, endIdx)
                    spans.add(Span.Highlight(parseInlineSpans(innerText)))
                    i = endIdx + 2
                    continue
                }
            }

            // 检测删除线 ~~text~~（支持嵌套）
            if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
                val endIdx = findMatchingDelimiter(text, "~~", i + 2)
                if (endIdx > 0) {
                    val innerText = text.substring(i + 2, endIdx)
                    spans.add(Span.Strikethrough(parseInlineSpans(innerText)))
                    i = endIdx + 2
                    continue
                }
            }

            // 检测行内代码 `code`（不支持嵌套，保持原样）
            if (text[i] == '`') {
                // 先检查双反引号
                if (i + 1 < len && text[i + 1] == '`') {
                    val endIdx = text.indexOf("``", i + 2)
                    if (endIdx > 0) {
                        spans.add(Span.Code(text.substring(i + 2, endIdx)))
                        i = endIdx + 2
                        continue
                    }
                }
                // 单反引号
                val endIdx = text.indexOf('`', i + 1)
                if (endIdx > 0) {
                    spans.add(Span.Code(text.substring(i + 1, endIdx)))
                    i = endIdx + 1
                    continue
                }
            }

            // 检测数学公式 $latex$（不支持嵌套，保持原样）
            if (text[i] == '$') {
                // 先检查双美元符号
                if (i + 1 < len && text[i + 1] == '$') {
                    val endIdx = text.indexOf("$$", i + 2)
                    if (endIdx > 0) {
                        spans.add(Span.Math(text.substring(i + 2, endIdx)))
                        i = endIdx + 2
                        continue
                    }
                }
                // 单美元符号
                val endIdx = text.indexOf('$', i + 1)
                if (endIdx > 0) {
                    spans.add(Span.Math(text.substring(i + 1, endIdx)))
                    i = endIdx + 1
                    continue
                }
            }

            // 检测斜体 *text* 或 _text_（支持嵌套）
            // 要求：起始标记后必须是字母或数字，结束标记前必须是字母或数字
            if ((text[i] == '*' || text[i] == '_') && (i + 1 >= len || text[i + 1] != text[i])) {
                val delimiter = text[i]
                // 检查起始标记后是否是字母或数字（避免把 `_id` 或 `*` 开头的变量名解析为斜体）
                if (i + 1 < len && text[i + 1].isLetterOrDigit()) {
                    val endIdx = findMatchingDelimiter(text, delimiter.toString(), i + 1)
                    // 检查结束标记前是否是字母或数字
                    if (endIdx > 0 && endIdx > i + 1 && text[endIdx - 1].isLetterOrDigit()) {
                        val innerText = text.substring(i + 1, endIdx)
                        spans.add(Span.Italic(parseInlineSpans(innerText)))
                        i = endIdx + 1
                        continue
                    }
                }
            }

            // 检测链接 [text](url)（链接文本支持嵌套解析）
            if (text[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket > 0 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen > 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val linkUrl = text.substring(closeBracket + 2, closeParen)
                        // 链接文本可能包含嵌套格式，递归解析
                        spans.add(Span.Link(linkText, linkUrl))
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // 检测脚注引用 [^label]
            if (i + 1 < len && text[i] == '[' && text[i + 1] == '^') {
                val closeBracket = text.indexOf(']', i + 2)
                if (closeBracket > 0) {
                    spans.add(Span.FootnoteRef(text.substring(i + 2, closeBracket)))
                    i = closeBracket + 1
                    continue
                }
            }

            // 检测上标 ^text^
            if (text[i] == '^') {
                val endIdx = text.indexOf('^', i + 1)
                if (endIdx > 0) {
                    spans.add(Span.Superscript(text.substring(i + 1, endIdx)))
                    i = endIdx + 1
                    continue
                }
            }

            // 检测下标 ~text~（排除 ~~ 删除线的情况）
            if (text[i] == '~' && (i + 1 >= len || text[i + 1] != '~')) {
                val endIdx = text.indexOf('~', i + 1)
                if (endIdx > 0) {
                    spans.add(Span.Subscript(text.substring(i + 1, endIdx)))
                    i = endIdx + 1
                    continue
                }
            }

            // 普通文本：收集直到下一个特殊字符
            val specialChars = setOf('*', '_', '`', '=', '~', '$', '[', ']', '^', '!', '(', '<', ':')
            var j = i
            while (j < len && text[j] !in specialChars) j++
            if (j > i) {
                spans.add(Span.PlainText(text.substring(i, j)))
                i = j
            } else {
                // 单个特殊字符作为普通文本
                spans.add(Span.PlainText(text[i].toString()))
                i++
            }
        }

        return spans
    }

    /**
     * 查找匹配的分隔符，考虑嵌套情况
     * 例如：在 "**==text==**" 中找到粗体的结束位置时，需要跳过内部的 ==
     */
    private fun findMatchingDelimiter(text: String, delimiter: String, startIndex: Int): Int {
        var i = startIndex
        val len = text.length
        val delimLen = delimiter.length

        while (i <= len - delimLen) {
            // 检查当前位置是否是分隔符
            if (text.substring(i, i + delimLen) == delimiter) {
                return i
            }
            i++
        }
        return -1
    }

    fun extractOutline(text: String): List<OutlineItem> {
        return text.lines().mapIndexedNotNull { idx, line ->
            HEADING_PATTERN.find(line)?.let {
                val rawText = it.groupValues[2].trim()
                // 去掉 Markdown 格式标记，显示纯文本
                val cleanText = stripMarkdownFormatting(rawText)
                OutlineItem(it.groupValues[1].length, cleanText, idx)
            }
        }
    }

    /**
     * 去掉 Markdown 格式标记，返回纯文本
     */
    private fun stripMarkdownFormatting(text: String): String {
        var result = text
        // 去掉粗斜体 ***text***
        result = result.replace(STRIP_BOLD_ITALIC_PATTERN, "$1")
        // 去掉粗体 **text**
        result = result.replace(STRIP_BOLD_ASTERISK_PATTERN, "$1")
        result = result.replace(STRIP_BOLD_UNDERSCORE_PATTERN, "$1")
        // 去掉斜体 *text* 或 _text_
        result = result.replace(STRIP_ITALIC_ASTERISK_PATTERN, "$1")
        result = result.replace(STRIP_ITALIC_UNDERSCORE_PATTERN, "$1")
        // 去掉高亮 ==text==
        result = result.replace(STRIP_HIGHLIGHT_PATTERN, "$1")
        // 去掉删除线 ~~text~~
        result = result.replace(STRIP_STRIKETHROUGH_PATTERN, "$1")
        // 去掉行内代码 `code`
        result = result.replace(STRIP_INLINE_CODE_PATTERN, "$1")
        // 去掉行内数学 $latex$
        result = result.replace(STRIP_INLINE_MATH_PATTERN, "$1")
        // 去掉 HTML 标签
        result = result.replace(STRIP_FONT_TAG_PATTERN, "$1")
        result = result.replace(STRIP_B_TAG_PATTERN, "$1")
        result = result.replace(STRIP_I_TAG_PATTERN, "$1")
        result = result.replace(STRIP_U_TAG_PATTERN, "$1")
        result = result.replace(STRIP_STRONG_TAG_PATTERN, "$1")
        result = result.replace(STRIP_EM_TAG_PATTERN, "$1")
        result = result.replace(STRIP_MARK_TAG_PATTERN, "$1")
        result = result.replace(STRIP_DEL_TAG_PATTERN, "$1")
        result = result.replace(STRIP_S_TAG_PATTERN, "$1")
        result = result.replace(STRIP_CODE_TAG_PATTERN, "$1")
        result = result.replace(STRIP_SUB_TAG_PATTERN, "$1")
        result = result.replace(STRIP_SUP_TAG_PATTERN, "$1")
        return result
    }
}
