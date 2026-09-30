package io.github.basil_as.basillines.engine

/** A small strict JSON reader and writer, so the backup format needs no library and the engine module stays pure Kotlin. */
object Json {
    const val MAX_DEPTH = 32

    class ParseError(message: String) : Exception(message)

    /** Parsed values: Map<String, Any?>, List<Any?>, String, Double, Boolean or null. */
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skipWs()
        val value = p.value(0)
        p.skipWs()
        if (!p.atEnd()) throw ParseError("trailing data at ${p.pos}")
        return value
    }

    fun parseOrNull(text: String): Any? = try { parse(text) } catch (_: ParseError) { null }

    private class Parser(val s: String) {
        var pos = 0
        fun atEnd() = pos >= s.length
        fun skipWs() { while (pos < s.length && s[pos] in " \t\n\r") pos++ }

        fun value(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw ParseError("too deep")
            if (atEnd()) throw ParseError("unexpected end")
            return when (val c = s[pos]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c in '0'..'9') num() else throw ParseError("unexpected '$c' at $pos")
            }
        }

        fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, pos)) throw ParseError("bad literal at $pos")
            pos += word.length
            return v
        }

        fun num(): Double {
            val start = pos
            if (s[pos] == '-') pos++
            while (pos < s.length && (s[pos] in '0'..'9' || s[pos] in ".eE+-")) pos++
            return s.substring(start, pos).toDoubleOrNull()?.takeIf { it.isFinite() } ?: throw ParseError("bad number at $start")
        }

        fun str(): String {
            pos++
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw ParseError("unterminated string")
                val c = s[pos++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (atEnd()) throw ParseError("bad escape")
                        when (val e = s[pos++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > s.length) throw ParseError("bad unicode escape")
                                sb.append(s.substring(pos, pos + 4).toIntOrNull(16)?.toChar() ?: throw ParseError("bad unicode escape"))
                                pos += 4
                            }
                            else -> throw ParseError("bad escape")
                        }
                    }
                    c < ' ' -> throw ParseError("control character in string")
                    else -> sb.append(c)
                }
            }
        }

        fun arr(depth: Int): List<Any?> {
            pos++
            val out = mutableListOf<Any?>()
            skipWs()
            if (!atEnd() && s[pos] == ']') { pos++; return out }
            while (true) {
                skipWs()
                out += value(depth + 1)
                skipWs()
                if (atEnd()) throw ParseError("unterminated array")
                when (s[pos++]) { ',' -> continue; ']' -> return out; else -> throw ParseError("bad array at $pos") }
            }
        }

        fun obj(depth: Int): Map<String, Any?> {
            pos++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (!atEnd() && s[pos] == '}') { pos++; return out }
            while (true) {
                skipWs()
                if (atEnd() || s[pos] != '"') throw ParseError("expected key at $pos")
                val key = str()
                skipWs()
                if (atEnd() || s[pos++] != ':') throw ParseError("expected ':' at $pos")
                skipWs()
                out[key] = value(depth + 1)
                skipWs()
                if (atEnd()) throw ParseError("unterminated object")
                when (s[pos++]) { ',' -> continue; '}' -> return out; else -> throw ParseError("bad object at $pos") }
            }
        }
    }

    fun write(value: Any?): String = StringBuilder().also { write(value, it) }.toString()

    private fun write(v: Any?, out: StringBuilder) {
        when (v) {
            null -> out.append("null")
            is Boolean -> out.append(v)
            is Int, is Long -> out.append(v)
            is Double -> out.append(if (v == Math.floor(v) && Math.abs(v) < 1e15) v.toLong().toString() else v.toString())
            is String -> quote(v, out)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((k, x) in v) { if (!first) out.append(','); first = false; quote(k.toString(), out); out.append(':'); write(x, out) }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                var first = true
                for (x in v) { if (!first) out.append(','); first = false; write(x, out) }
                out.append(']')
            }
            else -> throw IllegalArgumentException("cannot write ${v::class}")
        }
    }

    private fun quote(s: String, out: StringBuilder) {
        out.append('"')
        for (c in s) when {
            c == '"' -> out.append("\\\""); c == '\\' -> out.append("\\\\")
            c == '\n' -> out.append("\\n"); c == '\r' -> out.append("\\r"); c == '\t' -> out.append("\\t")
            c < ' ' -> out.append("\\u%04x".format(c.code))
            else -> out.append(c)
        }
        out.append('"')
    }
}
