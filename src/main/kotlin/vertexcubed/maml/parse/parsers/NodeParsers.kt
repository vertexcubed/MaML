package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.MBinding
import java.util.*
import kotlin.math.pow

//Parse an expression.
class ExprParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
        return PrecedenceParsers.LetLevel().parse(tokens, index, env)
    }
}

//class SequenceParser(): Parser<LetNode>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<LetNode> {
//        return ExprParser().lCompose(SpecialCharParser(";").bind { expr ->
//            ExprParser().bind { rest ->
//                LetNode(MBinding("_"))
//            }
//        })
//    }
//}




class LetParser(): Parser<LetNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<LetNode> {
        val parser = KeywordParser("let").rCompose(OptionalParser(KeywordParser("rec"))).bind { rec ->
            IdentifierParser().bind { first ->
                    ZeroOrMore(TypedIdentifierParser()).bind { arguments ->
                    OptionalParser(SpecialCharParser(":").rCompose(TypeParser())).bind { type ->
                        SpecialCharParser("=").rCompose(ExprParser()).bind { second ->
                            KeywordParser("in").rCompose(ExprParser()).map { third ->
                                if(rec.isPresent() && arguments.isEmpty()) throw ParseException(tokens[index].line, "Only functions can be recursive, not values.")

                                val node = arguments.foldRightIndexed(second, { index, str, exist ->
                                    if(rec.isPresent() && index == 0) {
                                        RecursiveFunctionNode(MBinding(first, Optional.empty()), FunctionNode(str, exist, tokens[index].line), tokens[index].line)
                                    }
                                    else FunctionNode(str, exist, tokens[index].line)
                                })
                                LetNode(MBinding(first, type), node, third, tokens[index].line)
                            }
                        }
                    }
                }
            }

        }
        return parser.parse(tokens, index, env)
    }
}

class IfParser(): Parser<IfNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<IfNode> {
        val parser = KeywordParser("if").rCompose(ExprParser()).bind { first ->
            KeywordParser("then").rCompose(ExprParser()).bind { second ->
                KeywordParser("else").rCompose(ExprParser()).map { third ->
                    IfNode(
                        first,
                        second,
                        third,
                        tokens[index].line
                    )
                }
            }
        }
        return parser.parse(tokens, index, env)
    }
}

class IntegerParser(): Parser<IntegerNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<IntegerNode> {
        val parser = DecimalNumberParser().disjoint(HexNumberParser()).map { second ->
            IntegerNode(second, tokens[index].line)
        }
        return parser.parse(tokens, index, env)
    }
}

class StringParser(): Parser<StringNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<StringNode> {
        return StringLitParser().map { str -> StringNode(str, tokens[index].line) }.parse(tokens, index, env)
    }
}

class CharParser(): Parser<CharNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<CharNode> {
        return CharLitParser().map { c -> CharNode(c, tokens[index].line) }.parse(tokens, index, env)
    }

}

class TrueParser(): Parser<TrueNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TrueNode> {
        return KeywordParser("true").map {_ -> TrueNode(tokens[index].line) }.parse(tokens, index, env)
    }
}

class FalseParser(): Parser<FalseNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<FalseNode> {
        return KeywordParser("false").map {_ -> FalseNode(tokens[index].line) }.parse(tokens, index, env)
    }
}

class FloatParser(): Parser<FloatNode>() {
    companion object {
        fun toDec(dec: Long): Float {
            return dec.toFloat() / 10.0.pow(dec.toString().length.toDouble()).toFloat()
        }
    }

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<FloatNode> {
        val parser = DecimalNumberParser().lCompose(SpecialCharParser(".")).bind { second ->
            PositiveDecimalNumberParser().map { third ->
                FloatNode(second + toDec(third), tokens[index].line)
            }
        }
        return parser.parse(tokens, index, env)
    }
}

class UnitParser(): Parser<UnitNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<UnitNode> {
        return LParenParser().rCompose(RParenParser()).map { _ -> UnitNode(tokens[index].line) }.parse(tokens, index, env)
    }
}

class ParenthesesParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
        return LParenParser().rCompose(ExprParser()).bind { first ->
            ZeroOrMore(SpecialCharParser(",").rCompose(ExprParser())).lCompose(RParenParser()).map { second ->
                if(second.isEmpty()) {
                    first
                }
                else {
                    val list = ArrayList<AstNode>()
                    list.add(first)
                    list.addAll(second)
                    TupleNode(list, first.line) as AstNode
                }
            }
        }.parse(tokens, index, env)
    }
}

class ApplicationParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
        return PrecedenceParsers.ConstLevel().bind {first ->
            ZeroOrMore(PrecedenceParsers.ConstLevel()).map(fun(second: List<AstNode>): AstNode {
                if(second.isEmpty()) return first

                var app = AppNode(first, second[0], tokens[index].line)
                for(i in 1..<second.size) {
                    app = AppNode(app, second[i], tokens[index].line)
                }
                return app
            })
        }.parse(tokens, index, env)
    }
}

class FunctionParser(): Parser<FunctionNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<FunctionNode> {
        return KeywordParser("fun").rCompose(TypedIdentifierParser()).bind { first ->
            SpecialCharParser("-").rCompose(SpecialCharParser(">")).rCompose(ExprParser()).map { second ->
                FunctionNode(first, second, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}

class ConNodeParser(): Parser<ConNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConNode> {
        return ConstructorParser().bind { cons ->
            OptionalParser(PrecedenceParsers.ConstLevel()).map { value ->
                ConNode(cons, value, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}

class ConDefParser(): Parser<ConDefNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConDefNode> {
        return ConstructorParser().bind { cons ->
            OptionalParser(KeywordParser("of").rCompose(TypeParser())).map { typ ->
                ConDefNode(MBinding(cons, typ), index)
            }
        }.parse(tokens, index, env)
    }

}