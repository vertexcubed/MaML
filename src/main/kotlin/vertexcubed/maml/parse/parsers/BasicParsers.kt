package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.TokenType
import vertexcubed.maml.parse.ast.Bop
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.*
import java.util.*
import kotlin.collections.ArrayList


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
    override fun parse(tokens: List<Token>, index: Int): ParseResult<String> {
        return simple(tokens, index, type)
    }

}

class StringLitParser(): Parser<String>() {
    override fun parse(
        tokens: List<Token>,
        index: Int
    ): ParseResult<String> {
        return simple(tokens, index, TokenType.STRING_LITERAL)
    }
}

@OptIn(ExperimentalStdlibApi::class)
class CharLitParser(): Parser<Char>() {

    override fun parse(tokens: List<Token>, index: Int): ParseResult<Char> {
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
    override fun parse(tokens: List<Token>, index: Int): ParseResult<String> {
        return simple(tokens, index, TokenType.KEYWORD, word)
    }
}

class IdentifierParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<String> {
        return SimpleParser(TokenType.IDENTIFIER).disjoint(
            SimpleParser(TokenType.IDENTIFIER)
        ).parse(tokens, index)
    }
}

class TypeParser(): Parser<MType>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<MType> {
        return SingleTypeParser().bind{ firstType ->
            ZeroOrMore(
                SpecialCharParser("-").rCompose(SpecialCharParser(">")).rCompose(TypeParser())
            ).map { moreTypes ->
                moreTypes.fold(firstType, {acc, rest -> MFunction(rest, acc)})
            }
        }.parse(tokens, index)
    }

}

class SingleTypeParser(): Parser<MType>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<MType> {
        if(index < tokens.size) {
            val first = tokens[index]
            if(first.type == TokenType.PRIMITIVE_TYPE) {
                val type = when(first.lexeme) {
                    "int" -> MInt
                    "bool" -> MBool
                    "float" -> MFloat
                    "char" -> MChar
                    "string" -> MString
                    "unit" -> MUnit
                    else -> return ParseResult.Failure(index, first, "Catastrophic failure against token type ${first.type} ($index) with lexeme ${first.lexeme}")
                }
                return ParseResult.Success(type, index + 1)
            }
            return ParseResult.Failure(index, first, "Expected token of type ${TokenType.PRIMITIVE_TYPE} but found ${first.type} (${first.lexeme})")
        }
        return ParseResult.Failure(index, tokens.last(), "Expected token of type ${TokenType.PRIMITIVE_TYPE}, but End of File reached.")
    }

}

class TupleTypeParser(): Parser<MTuple>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<MTuple> {
        return TypeParser().bind { first ->
            OneOrMore(SpecialCharParser("*").rCompose(TypeParser())).map { rest ->
                val list = ArrayList<MType>()
                list.add(first)
                list.addAll(rest)
                MTuple(list)
            }
        }.parse(tokens, index)
    }
}

//TODO: Make just One TypedIdentifierParser
class TupleIdentifierParser(): Parser<MBinding>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<MBinding> {
        val parser = IdentifierParser().bind { iden ->
            OptionalParser(SpecialCharParser(":").rCompose(TupleTypeParser())).map {
                type -> MBinding(iden, type as Optional<MType>)}
        }
        return parser.parse(tokens, index)
    }
}

class TypedIdentifierParser(): Parser<MBinding>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<MBinding> {
        val parser = IdentifierParser().bind { iden ->
            OptionalParser(SpecialCharParser(":").rCompose(TypeParser())).map {
                    type -> MBinding(iden, type)}
        }
        return parser.disjoint(LParenParser().rCompose(parser).lCompose(RParenParser())).parse(tokens, index)
    }
}



class SpecialCharParser(private val char: String): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<String> {
        return simple(tokens, index, TokenType.SPECIAL_CHAR, char)
    }
}

class LParenParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<String> {
//        print("$index ")
        return simple(tokens, index, TokenType.LPAREN)
    }
}

class RParenParser(): Parser<String>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<String> {
        return simple(tokens, index, TokenType.RPAREN)
    }
}

class DecimalNumberParser(): Parser<Long>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<Long> {
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

@OptIn(ExperimentalStdlibApi::class)
class HexNumberParser(): Parser<Long>() {
    companion object {
        private val hexFormat = HexFormat {
            number.prefix = "0x"
        }
    }
    override fun parse(tokens: List<Token>, index: Int): ParseResult<Long> {
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

class OpParser(val op: Bop): Parser<Bop>() {

    companion object {
        fun toStringParser(op: Bop): Parser<String> {
            return when(op) {
                Bop.ADD -> SpecialCharParser("+")
                Bop.SUB -> SpecialCharParser("-")
                Bop.MUL -> SpecialCharParser("*")
                Bop.DIV -> SpecialCharParser("/")
                Bop.MOD -> SpecialCharParser("%")
                Bop.LT -> SpecialCharParser("<")
                Bop.LTE -> AndParser(SpecialCharParser("<"), SpecialCharParser("="))
                    .map { pair -> pair.first + pair.second }
                Bop.GT -> SpecialCharParser(">")
                Bop.GTE -> AndParser(SpecialCharParser(">"), SpecialCharParser("="))
                    .map { pair -> pair.first + pair.second }
                Bop.EQ -> SpecialCharParser("=")
                Bop.NEQ -> AndParser(SpecialCharParser("!"), SpecialCharParser("="))
                    .map { pair -> pair.first + pair.second }
                    .disjoint(AndParser(SpecialCharParser("<"), SpecialCharParser(">"))
                        .map { pair -> pair.first + pair.second })
                Bop.AND -> AndParser(SpecialCharParser("&"), SpecialCharParser("&"))
                    .map { pair -> pair.first + pair.second }
                Bop.OR -> AndParser(SpecialCharParser("|"), SpecialCharParser("|"))
                    .map { pair -> pair.first + pair.second }
            }
        }
    }



    override fun parse(tokens: List<Token>, index: Int): ParseResult<Bop> {
        return toStringParser(op).map {_ -> op}.parse(tokens, index)
    }

}