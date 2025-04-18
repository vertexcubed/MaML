package vertexcubed.maml.parse.parsers

import vertexcubed.maml.core.MBinding
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.parse.*
import vertexcubed.maml.parse.result.ParseResult


private fun simple(tokens: List<Token>, index: Int, type: TokenType): ParseResult<String> {
    if(index < tokens.size) {
        val first = tokens[index]
        if(first.type == type) {
    return ParseResult.Success(first.lexeme, index + 1)
        }
        return ParseResult.Failure(index, first, "Expected token of type $type but found ${first.type} (${first.lexeme})")
    }
    return ParseResult.Failure(index, tokens.last(), "Expected token of type $type, but End of File reached.")
}

private fun simple(tokens: List<Token>, index: Int, type: TokenType, word: String): ParseResult<String> {
    if(index < tokens.size) {
        val first = tokens[index]
        if(first.type == type && first.lexeme == word) {
    return ParseResult.Success(first.lexeme, index + 1)
        }
        return ParseResult.Failure(index, first, "Expected: ${word}, Found: ${first.lexeme}")
    }
    return ParseResult.Failure(index, tokens.last(), "Expected: ${word}, but End of File reached.")
}

class SimpleParser(val type: TokenType): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, type)
    }
}

class EOFParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.EOF)
    }
}

class StringLitParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.STRING_LITERAL)
    }
}

@OptIn(ExperimentalStdlibApi::class)
class CharLitParser(): Parser<Char>() {

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Char> {
        if(index < tokens.size) {
            val first = tokens[index]
            if(first.type == TokenType.CHAR_LITERAL) {
                val text = first.lexeme
                val char = when(text) {
                    "\\n" -> '\n'
                    "\\t" -> '\t'
                    "\\r" -> '\r'
                    "\\b" -> '\b'
                    "\\\"" -> '\"'
                    "\\\\" -> '\\'
                    "\\space" -> ' '
                    else -> {
                        if(text[0] == '\\') {
                            if(text[1] == 'x' && text.length == 4) {
                                try {
                                    val hex = text.substring(2, 4).hexToByte().toInt().toChar()
                                    return ParseResult.Success(hex, index + 1)
                                }
                                catch(e: IllegalArgumentException) {
                                    return ParseResult.Failure(index, first, "Failed against token type ${first.type} with lexeme ${first.lexeme}")
                                }
                            }
                            if(text[1] == 'o') {
                                try {
                                    val octal = Integer.parseInt(text.substring(2, 5), 8).toChar()
                                    return ParseResult.Success(octal, index + 1)
                                }
                                catch(e: NumberFormatException) {
                                    return ParseResult.Failure(index, first, "Failed against token type ${first.type} with lexeme ${first.lexeme}")
                                }
                            }
                            if(text.length == 4) {
                                try {
                                    val decimal = Integer.parseInt(text.substring(1, 4)).toChar()
                                    return ParseResult.Success(decimal, index + 1)
                                }
                                catch(e: NumberFormatException) {
                                    return ParseResult.Failure(index, first, "Failed against token type ${first.type} with lexeme ${first.lexeme}")
                                }
                            }
                        }

                        if(text.length > 1) return ParseResult.Failure(index, first, "Failed against token type ${first.type} with lexeme ${first.lexeme}")
                        return ParseResult.Success(text[0], index + 1)
                    }
                }
                return ParseResult.Success(char, index + 1)
            }
            else ParseResult.Failure<Char>(index, first, "Failed against token type ${first.type} with lexeme ${first.lexeme}")
        }
        return ParseResult.Failure(index, tokens.last(), "End of file reached.")
    }

}

class KeywordParser(private val word: String): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.KEYWORD, word)
    }
}

class NonInfixIdentifierParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        val base = IdentifierParser().parse(tokens, index, env)
        when(base) {
            is ParseResult.Success -> {
                val text = base.result
                if(text in env.allStrings()) {
                    return ParseResult.Failure(index, tokens[index], "Expected identifier, but found infix operator instead")
                }
                return base
            }
            is ParseResult.Failure -> {
                return base
            }
        }
    }
}

class LongIdentifierParser(): Parser<MIdentifier>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<MIdentifier> {
        val parser = ZeroOrMore(ConstructorParser().lCompose(SpecialCharParser("."))).bind { first ->
            NonInfixIdentifierParser().map { i ->
                MIdentifier(first + i)
            }
        }
        return parser.parse(tokens, index, env)
    }
}

class LongConstructorParser(): Parser<MIdentifier>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<MIdentifier> {
        val parser = ZeroOrMore(ConstructorParser().lCompose(SpecialCharParser("."))).bind { first ->
            ConstructorParser().map { i ->
                MIdentifier(first + i)
            }
        }
        return parser.parse(tokens, index, env)
    }

}


class IdentifierParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return SimpleParser(TokenType.IDENTIFIER).parse(tokens, index, env)
    }
}

class CompoundSpecialCharParser(val name: String): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        if(name.isEmpty()) throw IllegalArgumentException("Cannot make compound special char parser on empty string")
        return SeriesParser(name.map { c -> SpecialCharParser("$c") }).map { results ->
            results.joinToString(separator = "")
        }.parse(tokens, index, env)
    }

}

class SpecificIdentifierParser(val name: String): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.IDENTIFIER, name)
    }

}

class ConstructorParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return SimpleParser(TokenType.CONSTRUCTOR).parse(tokens, index, env)
    }

}

class TypedIdentifierParser(): Parser<MBinding>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<MBinding> {
        val parser = IdentifierParser().bind { iden ->
            OptionalParser(SpecialCharParser(":").rCompose(TypeParser())).map {
                    type -> MBinding(iden, type)
            }
        }
        return parser.disjoint(LParenParser().rCompose(parser).lCompose(RParenParser())).parse(tokens, index, env)
    }
}



class SpecialCharParser(private val char: String): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.SPECIAL_CHAR, char)
    }
}

class LParenParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
//        print("$index ")
        return simple(tokens, index, TokenType.LPAREN)
    }
}

class RParenParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.RPAREN)
    }
}

class LCurlParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
//        print("$index ")
        return simple(tokens, index, TokenType.LCURL)
    }
}

class RCurlParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<String> {
        return simple(tokens, index, TokenType.RCURL)
    }
}

class PositiveDecimalNumberParser(): Parser<Long>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Long> {
        if(index < tokens.size) {
            val first = tokens[index]
            if(first.type == TokenType.NUMBER_LITERAL) {
                return try {
                    ParseResult.Success(first.lexeme.toLong(), index + 1)
                } catch(e: NumberFormatException) {
                    ParseResult.Failure(index, first,"${first.lexeme} is not a number.")
                }
            }
            return ParseResult.Failure(index, first,"Expected token of type ${TokenType.NUMBER_LITERAL} but found ${first.type} (${first.lexeme})")
        }
        return ParseResult.Failure(index, tokens.last(),"Expected token of type ${TokenType.NUMBER_LITERAL}, but End of File reached.")
    }
}

class DecimalNumberParser(): Parser<Long>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Long> {
        if(index < tokens.size) {
            val first = tokens[index]
            if(first.type == TokenType.NUMBER_LITERAL) {
                var lex = first.lexeme
                var mul = 1
                if(lex.isNotEmpty() && lex[0] == '-') {
                    lex = first.lexeme.substring(1)
                    mul = -1
                }
                return try {
                    ParseResult.Success(lex.toLong() * mul, index + 1)
                } catch(e: NumberFormatException) {
                    ParseResult.Failure(index, first,"${first.lexeme} is not a number.")
                }
            }
            return ParseResult.Failure(index, first,"Expected token of type ${TokenType.NUMBER_LITERAL} but found ${first.type} (${first.lexeme})")
        }
        return ParseResult.Failure(index, tokens.last(),"Expected token of type ${TokenType.NUMBER_LITERAL}, but End of File reached.")
    }
}

@OptIn(ExperimentalStdlibApi::class)
class HexNumberParser(): Parser<Long>() {
    companion object {
        private val hexFormat = HexFormat {
            number.prefix = "0x"
        }
    }
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Long> {
        if(index < tokens.size) {
            val first = tokens[index]
            if(first.type == TokenType.HEX_LITERAL) {
                return try {
                    ParseResult.Success(first.lexeme.hexToLong(hexFormat), index + 1)
                } catch(e: IllegalArgumentException) {
                    ParseResult.Failure(index, first,"${first.lexeme} is not a hex number.")
                }
            }
            return ParseResult.Failure(index, first,"Expected token of type ${TokenType.HEX_LITERAL} but found ${first.type} (${first.lexeme})")
        }
        return ParseResult.Failure(index, tokens.last(),"Expected token of type ${TokenType.HEX_LITERAL}, but End of File reached.")
    }
}