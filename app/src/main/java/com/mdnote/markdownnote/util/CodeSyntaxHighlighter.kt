package com.mdnote.markdownnote.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mdnote.markdownnote.ui.theme.*

/**
 * 代码语法高亮器
 * 支持多种编程语言的语法高亮
 */
object CodeSyntaxHighlighter {

    // 语言关键词映射
    private val languageKeywords = mapOf(
        "kotlin" to listOf(
            "package", "import", "class", "interface", "object", "fun", "val", "var",
            "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
            "return", "break", "continue", "is", "as", "in", "out", "typeof",
            "private", "public", "protected", "internal", "open", "final", "abstract",
            "override", "lateinit", "lazy", "companion", "data", "enum", "sealed",
            "annotation", "suspend", "inline", "noinline", "crossinline", "reified",
            "true", "false", "null", "this", "super", "it", "by", "where", "init",
            "constructor", "get", "set", "field", "typealias", "value", "tailrec"
        ),
        "java" to listOf(
            "package", "import", "class", "interface", "enum", "extends", "implements",
            "public", "private", "protected", "static", "final", "abstract", "native",
            "synchronized", "volatile", "transient", "strictfp", "void", "return",
            "if", "else", "switch", "case", "default", "for", "while", "do", "break",
            "continue", "try", "catch", "finally", "throw", "throws", "new", "this",
            "super", "instanceof", "true", "false", "null", "const", "goto", "assert",
            "record", "sealed", "permits", "non-sealed", "var", "yield"
        ),
        "python" to listOf(
            "import", "from", "as", "class", "def", "lambda", "pass", "return",
            "if", "elif", "else", "for", "while", "break", "continue", "try",
            "except", "finally", "raise", "with", "yield", "global", "nonlocal",
            "assert", "async", "await", "True", "False", "None", "and", "or",
            "not", "in", "is", "del", "print", "exec", "property", "staticmethod",
            "classmethod", "super", "self", "cls", "match", "case"
        ),
        "javascript" to listOf(
            "var", "let", "const", "function", "class", "extends", "constructor",
            "return", "if", "else", "switch", "case", "default", "for", "while",
            "do", "break", "continue", "try", "catch", "finally", "throw", "new",
            "this", "super", "typeof", "instanceof", "in", "of", "void", "delete",
            "async", "await", "yield", "import", "export", "from", "as", "default",
            "static", "get", "set", "true", "false", "null", "undefined", "NaN",
            "Infinity", "debugger", "with"
        ),
        "typescript" to listOf(
            "var", "let", "const", "function", "class", "extends", "constructor",
            "return", "if", "else", "switch", "case", "default", "for", "while",
            "do", "break", "continue", "try", "catch", "finally", "throw", "new",
            "this", "super", "typeof", "instanceof", "in", "of", "void", "delete",
            "async", "await", "yield", "import", "export", "from", "as", "default",
            "static", "get", "set", "true", "false", "null", "undefined", "NaN",
            "Infinity", "debugger", "type", "interface", "enum", "namespace", "module",
            "declare", "abstract", "implements", "public", "private", "protected",
            "readonly", "keyof", "infer", "never", "unknown", "any", "void", "is",
            "unique", "symbol", "bigint", "require", "satisfies"
        ),
        "c" to listOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if", "int",
            "long", "register", "return", "short", "signed", "sizeof", "static",
            "struct", "switch", "typedef", "union", "unsigned", "void", "volatile",
            "while", "inline", "restrict", "_Bool", "_Complex", "_Imaginary",
            "_Alignas", "_Alignof", "_Atomic", "_Generic", "_Noreturn", "_Static_assert",
            "_Thread_local"
        ),
        "cpp" to listOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if", "int",
            "long", "register", "return", "short", "signed", "sizeof", "static",
            "struct", "switch", "typedef", "union", "unsigned", "void", "volatile",
            "while", "inline", "restrict", "class", "public", "private", "protected",
            "virtual", "friend", "operator", "template", "typename", "new", "delete",
            "this", "namespace", "using", "try", "catch", "throw", "const_cast",
            "dynamic_cast", "static_cast", "reinterpret_cast", "typeid", "explicit",
            "export", "mutable", "nullptr", "override", "final", "constexpr",
            "noexcept", "thread_local", "alignas", "alignof", "decltype", "auto",
            "concept", "requires", "co_await", "co_return", "co_yield"
        ),
        "go" to listOf(
            "package", "import", "func", "return", "var", "const", "type", "struct",
            "interface", "map", "chan", "if", "else", "switch", "case", "default",
            "for", "range", "break", "continue", "goto", "fallthrough", "defer",
            "select", "go", "true", "false", "nil", "iota", "append", "cap", "close",
            "complex", "copy", "delete", "imag", "len", "make", "new", "panic",
            "print", "println", "real", "recover"
        ),
        "rust" to listOf(
            "fn", "let", "mut", "const", "static", "pub", "mod", "use", "crate",
            "self", "super", "struct", "enum", "trait", "impl", "type", "where",
            "if", "else", "match", "for", "while", "loop", "break", "continue",
            "return", "move", "ref", "as", "in", "unsafe", "extern", "async",
            "await", "dyn", "true", "false", "Self", "self", "super", "box",
            "macro_rules", "union", "default"
        ),
        "swift" to listOf(
            "class", "struct", "enum", "protocol", "extension", "func", "var", "let",
            "import", "return", "if", "else", "switch", "case", "default", "for",
            "while", "do", "break", "continue", "fallthrough", "return", "throw",
            "try", "catch", "defer", "guard", "where", "in", "as", "is", "private",
            "fileprivate", "internal", "public", "open", "static", "final", "override",
            "mutating", "nonmutating", "lazy", "weak", "unowned", "true", "false",
            "nil", "self", "Self", "super", "inout", "typealias", "associatedtype",
            "precedencegroup", "operator", "infix", "prefix", "postfix", "async",
            "await", "actor", "some", "any", "rethrows", "throws", "indirect",
            "convenience", "required", "dynamic", "objc", "nonisolated", "distributed"
        ),
        "sql" to listOf(
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "LIKE", "BETWEEN",
            "IS", "NULL", "AS", "ORDER", "BY", "ASC", "DESC", "LIMIT", "OFFSET",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "FULL", "ON", "GROUP", "HAVING",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE",
            "DATABASE", "INDEX", "VIEW", "DROP", "ALTER", "ADD", "COLUMN", "PRIMARY",
            "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT", "AUTO_INCREMENT",
            "UNION", "ALL", "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX", "CASE",
            "WHEN", "THEN", "ELSE", "END", "IF", "EXISTS", "TRUNCATE", "GRANT", "REVOKE",
            "COMMIT", "ROLLBACK", "TRANSACTION", "BEGIN", "DECLARE", "CURSOR", "FETCH",
            "OPEN", "CLOSE", "EXEC", "EXECUTE", "PROCEDURE", "FUNCTION", "TRIGGER"
        ),
        "json" to listOf(
            "true", "false", "null"
        ),
        "yaml" to listOf(
            "true", "false", "null", "yes", "no", "on", "off", "~"
        ),
        "xml" to listOf(
            "xml", "version", "encoding", "standalone", "DOCTYPE", "ENTITY", "ELEMENT",
            "ATTLIST", "NOTATION", "PUBLIC", "SYSTEM", "CDATA", "INCLUDE", "IGNORE"
        ),
        "html" to listOf(
            "html", "head", "body", "title", "meta", "link", "script", "style",
            "div", "span", "p", "a", "img", "ul", "ol", "li", "table", "tr", "td",
            "th", "form", "input", "button", "select", "option", "textarea", "label",
            "header", "footer", "nav", "main", "section", "article", "aside", "figure",
            "figcaption", "video", "audio", "canvas", "svg", "iframe", "embed", "object",
            "param", "source", "track", "br", "hr", "pre", "code", "blockquote", "cite",
            "strong", "em", "b", "i", "u", "s", "sub", "sup", "mark", "small", "del",
            "ins", "abbr", "address", "time", "data", "meter", "progress", "details",
            "summary", "dialog", "menu", "menuitem", "template", "slot", "noscript",
            "base", "basefont", "frame", "frameset", "noframes", "applet", "isindex"
        ),
        "css" to listOf(
            "important", "inherit", "initial", "unset", "revert", "none", "auto",
            "block", "inline", "inline-block", "flex", "grid", "table", "absolute",
            "relative", "fixed", "sticky", "static", "hidden", "visible", "scroll",
            "solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset",
            "center", "left", "right", "top", "bottom", "middle", "baseline", "sub",
            "super", "text-top", "text-bottom", "nowrap", "pre", "pre-wrap", "pre-line",
            "uppercase", "lowercase", "capitalize", "bold", "bolder", "lighter", "normal",
            "italic", "oblique", "underline", "overline", "line-through", "blink",
            "transparent", "currentColor", "@media", "@keyframes", "@import", "@font-face",
            "@supports", "@page", "@namespace", "@counter-style", "@viewport", "@-webkit-keyframes"
        ),
        "shell" to listOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "do",
            "done", "in", "function", "return", "exit", "break", "continue", "local",
            "export", "readonly", "declare", "typeset", "unset", "shift", "set", "unset",
            "echo", "printf", "read", "test", "true", "false", "exit", "source", "alias",
            "unalias", "eval", "exec", "trap", "wait", "kill", "ps", "grep", "sed", "awk",
            "cut", "sort", "uniq", "wc", "head", "tail", "cat", "more", "less", "find",
            "xargs", "chmod", "chown", "mkdir", "rmdir", "rm", "cp", "mv", "ln", "touch",
            "date", "sleep", "time", "pwd", "cd", "ls", "which", "whereis", "type", "man"
        ),
        "bash" to listOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "do",
            "done", "in", "function", "return", "exit", "break", "continue", "local",
            "export", "readonly", "declare", "typeset", "unset", "shift", "set", "unset",
            "echo", "printf", "read", "test", "true", "false", "exit", "source", "alias",
            "unalias", "eval", "exec", "trap", "wait", "kill", "ps", "grep", "sed", "awk",
            "cut", "sort", "uniq", "wc", "head", "tail", "cat", "more", "less", "find",
            "xargs", "chmod", "chown", "mkdir", "rmdir", "rm", "cp", "mv", "ln", "touch",
            "date", "sleep", "time", "pwd", "cd", "ls", "which", "whereis", "type", "man"
        ),
        "markdown" to listOf(
            "TODO", "FIXME", "NOTE", "WARNING", "XXX", "HACK"
        )
    )

    // 语言别名映射
    private val languageAliases = mapOf(
        "js" to "javascript",
        "ts" to "typescript",
        "py" to "python",
        "rb" to "ruby",
        "sh" to "shell",
        "zsh" to "shell",
        "kt" to "kotlin",
        "h" to "c",
        "hpp" to "cpp",
        "hxx" to "cpp",
        "cc" to "cpp",
        "cxx" to "cpp",
        "rs" to "rust",
        "go" to "go",
        "swift" to "swift",
        "yml" to "yaml"
    )

    /**
     * 获取实际语言名称
     */
    private fun getActualLanguage(language: String): String {
        return languageAliases[language.lowercase()] ?: language.lowercase()
    }

    /**
     * 高亮代码
     * @param code 代码内容
     * @param language 语言
     * @param isDarkTheme 是否深色主题
     * @return 带语法高亮的AnnotatedString
     */
    fun highlight(code: String, language: String, isDarkTheme: Boolean): AnnotatedString {
        val actualLanguage = getActualLanguage(language)
        val keywords = languageKeywords[actualLanguage] ?: emptyList()

        return buildAnnotatedString {
            // 将 Tab 字符替换为 4 个空格，避免显示为方框
            val normalizedCode = code.replace("\t", "    ")
            val lines = normalizedCode.lines()
            lines.forEachIndexed { index, line ->
                if (index > 0) append("\n")
                highlightLine(line, keywords, isDarkTheme)
            }
        }
    }
    private val CODE_PATTERNS = listOf(
        // 多行字符串和单行字符串
        Regex("\"\"\"[\\s\\S]*?\"\"\"") to "multilineString",
        Regex("'''[\\s\\S]*?'''") to "multilineString",
        Regex("\"([^\"\\\\]|\\\\.)*\"") to "string",
        Regex("'([^'\\\\]|\\\\.)*'") to "string",
        Regex("`[^`]*`") to "string",
        // 注释
        Regex("//.*$") to "comment",
        Regex("#.*$") to "comment",
        Regex("--.*$") to "comment",
        Regex(";.*$") to "comment",
        Regex("/\\*[\\s\\S]*?\\*/") to "comment",
        Regex("<!--.*?-->") to "comment",
        // 数字
        Regex("\\b(0x[0-9a-fA-F]+|0b[01]+|0o[0-7]+|\\d+\\.?\\d*([eE][+-]?\\d+)?[fFdDlL]?)\\b") to "number",
        // 注解/装饰器
        Regex("@[a-zA-Z_][a-zA-Z0-9_]*") to "annotation",
        // 函数调用
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()") to "function",
        // 关键词和标识符
        Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b") to "identifier"
    )

    /**
     * 高亮单行代码
     */
    private fun AnnotatedString.Builder.highlightLine(
        line: String,
        keywords: List<String>,
        isDarkTheme: Boolean
    ) {
        var remaining = line

        while (remaining.isNotEmpty()) {
            var earliestMatch: MatchResult? = null
            var earliestType: String? = null
            var earliestIndex = remaining.length

            for ((pattern, type) in CODE_PATTERNS) {
                val match = pattern.find(remaining)
                if (match != null && match.range.first < earliestIndex) {
                    earliestMatch = match
                    earliestType = type
                    earliestIndex = match.range.first
                }
            }

            if (earliestMatch != null && earliestType != null) {
                // 添加匹配前的普通文本
                if (earliestIndex > 0) {
                    append(remaining.substring(0, earliestIndex))
                }

                val match = earliestMatch
                when (earliestType) {
                    "string", "multilineString" -> {
                        withStyle(SpanStyle(color = if (isDarkTheme) DarkCodeString else LightCodeString)) {
                            append(match.value)
                        }
                    }
                    "comment" -> {
                        withStyle(SpanStyle(
                            color = if (isDarkTheme) DarkCodeComment else LightCodeComment,
                            fontStyle = FontStyle.Italic
                        )) {
                            append(match.value)
                        }
                    }
                    "number" -> {
                        withStyle(SpanStyle(color = if (isDarkTheme) DarkCodeNumber else LightCodeNumber)) {
                            append(match.value)
                        }
                    }
                    "annotation" -> {
                        withStyle(SpanStyle(color = if (isDarkTheme) DarkCodeFunction else LightCodeFunction)) {
                            append(match.value)
                        }
                    }
                    "function" -> {
                        withStyle(SpanStyle(color = if (isDarkTheme) DarkCodeFunction else LightCodeFunction)) {
                            append(match.groupValues[1])
                        }
                    }
                    "identifier" -> {
                        val word = match.groupValues[1]
                        if (word in keywords) {
                            withStyle(SpanStyle(
                                color = if (isDarkTheme) DarkCodeKeyword else LightCodeKeyword,
                                fontWeight = FontWeight.Bold
                            )) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                    else -> append(match.value)
                }

                remaining = remaining.substring(match.range.last + 1)
            } else {
                // 没有更多匹配，添加剩余文本
                append(remaining)
                break
            }
        }
    }
}
