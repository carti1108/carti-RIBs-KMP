package com.carti.ribs

/**
 * [RibState]를 JSON 문자열로 바꾸고 되살린다. 외부 의존성 없이 허용 타입만 다룬다(ADR-0008).
 * 정수는 `Long`, 소수점이나 지수가 있는 수는 `Double`로 읽는다.
 */
internal object RibStateCodec {
    fun encode(values: Map<String, Any?>): String = buildString { writeObject(values) }

    fun decode(encoded: String): Map<String, Any?> {
        val reader = Reader(encoded)
        val result = reader.readObject()
        reader.skipWhitespace()
        require(reader.isAtEnd) { "Unexpected trailing characters at ${reader.position}" }
        return result
    }

    private fun StringBuilder.writeObject(values: Map<String, Any?>) {
        append('{')
        values.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) append(',')
            writeString(key)
            append(':')
            writeValue(value)
        }
        append('}')
    }

    private fun StringBuilder.writeValue(value: Any?) {
        when (value) {
            null -> append("null")
            is String -> writeString(value)
            is Boolean, is Long -> append(value.toString())
            is Double -> append(encodeDouble(value))
            is RibState -> writeObject(value.keys.associateWith { value[it] })
            is List<*> -> {
                append('[')
                value.forEachIndexed { index, item -> if (index > 0) append(','); writeValue(item) }
                append(']')
            }
            else -> error("Unsupported RibState value: $value")
        }
    }

    /** JS는 `2.0.toString()`이 `"2"`라서, 다시 읽을 때 `Double`이 되도록 소수점을 붙인다. */
    private fun encodeDouble(value: Double): String {
        val text = value.toString()
        return if (text.any { it == '.' || it == 'e' || it == 'E' }) text else "$text.0"
    }

    private fun StringBuilder.writeString(text: String) {
        append('"')
        text.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ') append("\\u" + char.code.toString(HEX).padStart(UNICODE_DIGITS, '0')) else append(char)
            }
        }
        append('"')
    }

    private class Reader(private val text: String) {
        var position = 0
            private set
        val isAtEnd: Boolean get() = position >= text.length

        fun readObject(): Map<String, Any?> {
            expect('{')
            val result = linkedMapOf<String, Any?>()
            if (consumeIf('}')) return result
            do {
                val key = readString()
                expect(':')
                result[key] = readValue()
            } while (consumeIf(','))
            expect('}')
            return result
        }

        private fun readValue(): Any? {
            skipWhitespace()
            require(!isAtEnd) { "Unexpected end of RibState input" }
            return when (val char = text[position]) {
                '{' -> RibState(readObject())
                '[' -> readList()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> if (char == '-' || char.isDigit()) readNumber() else fail("Unexpected '$char'")
            }
        }

        private fun readList(): List<Any?> {
            expect('[')
            val result = mutableListOf<Any?>()
            if (consumeIf(']')) return result
            do result += readValue() while (consumeIf(','))
            expect(']')
            return result
        }

        fun readString(): String {
            expect('"')
            val result = StringBuilder()
            while (true) {
                require(!isAtEnd) { "Unterminated string in RibState input" }
                val char = text[position++]
                when (char) {
                    '"' -> return result.toString()
                    '\\' -> result.append(readEscape())
                    else -> result.append(char)
                }
            }
        }

        private fun readEscape(): Char {
            require(!isAtEnd) { "Unterminated escape in RibState input" }
            return when (val char = text[position++]) {
                '"', '\\', '/' -> char
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    require(position + UNICODE_DIGITS <= text.length) { "Invalid unicode escape at $position" }
                    val code = text.substring(position, position + UNICODE_DIGITS).toIntOrNull(HEX)
                        ?: fail("Invalid unicode escape")
                    position += UNICODE_DIGITS
                    code.toChar()
                }
                else -> fail("Invalid escape '\\$char'")
            }
        }

        private fun readNumber(): Any {
            val start = position
            while (!isAtEnd && text[position] in NUMBER_CHARS) position++
            val token = text.substring(start, position)
            val isDecimal = token.any { it == '.' || it == 'e' || it == 'E' }
            return (if (isDecimal) token.toDoubleOrNull() else token.toLongOrNull()) ?: fail("Invalid number '$token'")
        }

        private fun readLiteral(literal: String, value: Any?): Any? {
            require(text.startsWith(literal, position)) { "Invalid literal at $position" }
            position += literal.length
            return value
        }

        fun skipWhitespace() {
            while (!isAtEnd && text[position].isWhitespace()) position++
        }

        private fun consumeIf(char: Char): Boolean {
            skipWhitespace()
            if (isAtEnd || text[position] != char) return false
            position++
            return true
        }

        private fun expect(char: Char) {
            require(consumeIf(char)) { "Expected '$char' at $position in RibState input" }
        }

        private fun fail(message: String): Nothing = throw IllegalArgumentException("$message at $position in RibState input")
    }

    private const val HEX = 16
    private const val UNICODE_DIGITS = 4
    private const val NUMBER_CHARS = "+-0123456789.eE"
}
