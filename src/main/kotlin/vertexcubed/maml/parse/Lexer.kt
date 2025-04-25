package vertexcubed.maml.parse

import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.TokenType.*


private val WHITESPACE_REG = Regex("[ \t\n\r]+")

private val IDENTIFIER_REG = Regex("[a-zA-Z0-9_']")
private val DIGIT_REG = Regex("[0-9]")
private val HEX_DIGIT_REG = Regex("[0-9a-fA-F]")



class Lexer(val source: String) {

    companion object {
        private val keywords = HashSet<String>()

        init {
            keywords.addAll(listOf(
                "and",
//                "as",
                "assert",
//                "do",
                "else",
                "end",
                "exception",
                "external",
                "fn",
                "fun",
//                "handle",
                "if",
                "in",
                "include",
                "infix",
                "infixr",
                "let",
//                "local",
                "match",
                "module",
                "nonfix",
                "of",
                "op",
                "open",
//                "orelse",
                "rec",
                "sig",
                "struct",
                "then",
                "try",
                "type",
                "val",
                "with",
//                "withtype",
//                "while",
                "true",
                "false",
            ))
        }
    }


    val data = ArrayList<Token>()
    var lineIdx = 1
    var current = 0

    fun toStringList(): List<String> {
        return source.split('\n')
    }


    fun read(): List<Token> {

        if(source.isEmpty()) {
            data.add(Token(EOF, "", lineIdx))
            return data
        }


        while(hasNext()) {
            val token = token(poll())
            if(token.type == COMMENT) continue //ignore comments
            data.add(token)
        }
        if(data.last().type != EOF) {
            data.add(Token(EOF, "", lineIdx))
        }

        return data
    }

    private fun hasNext(): Boolean {
        return current < source.length
    }

    private fun peek(): Char {
        return source[current]
    }


    private fun poll(): Char {
        return source[current++]
    }



    private fun token(c: Char): Token {
        return when(c) {
            '!', '%', '&', '$', '#', '+', '/',
            ':', '<', '=', '>', '?', '@', '\\', '~',
            '`', '^', '|', '*', '.', ',', ';', -> Token(SPECIAL_CHAR, "$c", lineIdx)
            '-' -> {
                if(hasNext() && DIGIT_REG.matches(peek().toString())) {
                    return numberLit(c)
                }
                Token(SPECIAL_CHAR, "$c", lineIdx)
            }
            '{' -> Token(LCURL, "$c", lineIdx)
            '}' -> Token(RCURL, "$c", lineIdx)
            '[' -> Token(LBRACKET, "$c", lineIdx)
            ']' -> Token(RBRACKET, "$c", lineIdx)
            '(' -> {
                if(hasNext()) {
                    val next = peek()
                    if(next == '*') {
                        return comment(c)
                    }
                }
                return Token(LPAREN, "$c", lineIdx)
            }
            ')' -> Token(RPAREN, "$c", lineIdx)
            //Skip whitespace
            ' ', '\r', '\t' -> if(hasNext()) token(poll()) else Token(EOF, "", lineIdx)
            //newline, skip
            '\n' -> {
                lineIdx++
                return if(hasNext()) token(poll()) else Token(EOF, "", lineIdx)
            }
            '"' -> stringLit()
            '\'' -> {
                if(hasNext() && peek() != '\\') {
                    if(current + 1 < source.length) {
                        val peekTwice = source[current + 1]
                        if(peekTwice == '\'') {
                            val actualChar = poll()
                            poll() //consumes the closing apostrophe
                            return Token(CHAR_LITERAL, "$actualChar", lineIdx)
                        }
                    }
                }
                else if(hasNext() && peek() == '\\') {
                    return escapeSequence()
                }
                return Token(SPECIAL_CHAR, "$c", lineIdx)
            }
            else -> {
                if(DIGIT_REG.matches("$c")) {
                    return numberLit(c)
                }
                else {
                    return literal(c)
                }
            }

        }
    }

    private fun numberLit(currentChar: Char): Token {
        var c = currentChar
        val builder = StringBuilder().append(c)
        if(c == '-') {
            c = poll()
            builder.append(c)
        }
        var hex = false
        if(c == '0' && hasNext() && peek() == 'x') {
            builder.append(poll())
            hex = true
        }
        while(hasNext() && (if(hex) HEX_DIGIT_REG else DIGIT_REG).matches("${peek()}")) {
            builder.append(poll())
        }
        if(hex && builder.toString().length <= 2) {
            throw ParseException(lineIdx, "Invalid hex literal: $builder")
        }
        return Token(if(hex) HEX_LITERAL else NUMBER_LITERAL, builder.toString(), lineIdx)
    }

    private fun literal(currentChar: Char): Token {
        val iden = if(currentChar.isUpperCase()) CONSTRUCTOR else IDENTIFIER
        val builder = StringBuilder().append(currentChar)
        while(hasNext() && IDENTIFIER_REG.matches("${peek()}")) {
            builder.append(poll())
        }
        val type = if(builder.toString() in keywords) KEYWORD else iden

        return Token(type, builder.toString(), lineIdx)
    }

    private fun escapeSequence(): Token {
        val builder = StringBuilder().append(poll())
        //State is either: 0 (regular escape), 1 (Ascii decimal), 2 (Ascii hexadecimal), or 3 (Ascii octal)
        var state = 0
        val nextChar = poll()
        when(nextChar) {
            'x' -> {
                state = 2
                builder.append(nextChar)
            }
            'o' -> {
                state = 3
                builder.append(nextChar)
            }
            '\'', '\"', '\\', 'n', 't', 'b', 'r' -> {
                builder.append(nextChar)
                if(hasNext() && peek() == '\'') {
                    poll() //discard closing char
                    return Token(CHAR_LITERAL, builder.toString(), lineIdx)
                }
                else throw ParseException(lineIdx, "Illegal char literal: $nextChar")
            }
            's' -> {
                try {
                        builder.append(nextChar)
                            .append(source[current])
                            .append(source[current + 1])
                            .append(source[current + 2])
                            .append(source[current + 3])
                        if(builder.toString().equals("\\space") && source[current + 4] == '\'') {
                            for(i in 0..4) {
                                poll()
                            }
                            return Token(CHAR_LITERAL, builder.toString(), lineIdx)
                        }
                        else throw ParseException(lineIdx, "Illegal char literal: $nextChar")
                    }
                catch (e: IndexOutOfBoundsException) {
                    throw ParseException(lineIdx, "Illegal char literal: $nextChar")
                }
            }
            else -> {
                if(DIGIT_REG.matches("$nextChar")) {
                    state = 1
                    builder.append(nextChar)
                    for(i in 0..1) {
                        if(!hasNext() || !DIGIT_REG.matches("${peek()}")) throw ParseException(lineIdx, "Illegal char literal: $nextChar")
                        builder.append(poll())
                    }
                    if(hasNext() && peek() == '\'') {
                        poll()
                        return Token(CHAR_LITERAL, builder.toString(), lineIdx)
                    }
                    else throw ParseException(lineIdx, "Missing closing apostrophe in char literal: $nextChar")
                }
                else {
                    throw ParseException(lineIdx, "Illegal char literal: $nextChar")
                }
            }
        }

        when(state) {
            2 -> {
                for (i in 0..1) {
                    if (!hasNext() || !HEX_DIGIT_REG.matches("${peek()}")) throw ParseException(
                        lineIdx,
                        "Illegal char literal: $nextChar"
                    )
                    builder.append(poll())
                }
                if (hasNext() && peek() == '\'') {
                    poll()
                    return Token(CHAR_LITERAL, builder.toString(), lineIdx)
                } else throw ParseException(lineIdx, "Missing closing apostrophe in char literal: $nextChar")
            }

            3 -> {
                try {
                    val octal = Regex("[0-3][0-7][0-7]")
                    builder.append(poll()).append(poll()).append(poll())
                    val toTest = builder.toString().substring(2, 5)
                    if(!octal.matches(toTest)) throw ParseException(lineIdx, "Illegal char literal: $nextChar")
                }
                catch(e: IndexOutOfBoundsException) {
                    throw ParseException(lineIdx, "Illegal char literal: $nextChar")
                }
                if (hasNext() && peek() == '\'') {
                    poll()
                    return Token(CHAR_LITERAL, builder.toString(), lineIdx)
                } else throw ParseException(lineIdx, "Missing closing apostrophe in char literal: $nextChar")
            }

            else -> throw IllegalStateException("Illegal state when building char literal: $state")
        }
    }

    private fun comment(first: Char): Token {
        val beginningLine = lineIdx
        val builder = StringBuilder()
        builder.append(first).append(poll())
        while(hasNext()) {
            //nested comments
            if(peek() == '(') {
                val temp = poll()
                if(hasNext() && peek() == '*') {
                    comment(temp)
                }
                else {
                    builder.append(temp)
                }
                continue
            }



            if(peek() == '*') {
                builder.append(poll())
                if(hasNext() && peek() == ')') {
                    return Token(COMMENT, builder.append(poll()).toString(), lineIdx)
                }
            }
            if(peek() == '\n') {
                lineIdx++
            }
            builder.append(poll())
        }
        throw ParseException(beginningLine, "No closing comment.")
    }

    private fun stringLit(): Token {
        val beginningLine = lineIdx
        val builder = StringBuilder()
        while(hasNext()) {
            val next = poll()
            if(next == '\\' && peek() == '"') {
                builder.append(next).append(poll())
                continue
            }
            if(next == '"') {
                return Token(STRING_LITERAL, builder.toString(), lineIdx)
            }
            if(next == '\n') {
                lineIdx++
            }
            if(next == '\\') {
                if(peek() == 'n') {
                    builder.append('\n')
                    poll()
                    continue
                }
            }
            builder.append(next)
        }
        throw ParseException(beginningLine, "String literal not terminated.")
    }

}

class Token(val type: TokenType, val lexeme: String, val line: Int) {

    override fun toString(): String {
        return "Token($type, $lexeme, $line)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (type != other.type) return false
        if (lexeme != other.lexeme) return false
        if (line != other.line) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + lexeme.hashCode()
        result = 31 * result + line
        return result
    }
}

enum class TokenType {
    LPAREN, RPAREN,
    LCURL, RCURL,
    LBRACKET, RBRACKET,

    NUMBER_LITERAL,
    HEX_LITERAL,
    SPECIAL_CHAR,
    STRING_LITERAL,
    CHAR_LITERAL,
    IDENTIFIER,
    CONSTRUCTOR,
    KEYWORD,

    EOF,

    COMMENT
}