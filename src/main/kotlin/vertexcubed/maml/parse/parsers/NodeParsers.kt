package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.BadRecordException
import vertexcubed.maml.core.MBinding
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.result.ParseResult
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
            LetBindingParser().bind { first ->
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

class VariableParser(): Parser<VariableNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<VariableNode> {
        return LongIdentifierParser().map { r -> VariableNode(r, tokens[index].line) }.parse(tokens, index, env)
    }
}

class RecordLookupParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
        return VariableParser().bind { first ->
            ZeroOrMore(SpecialCharParser(".").rCompose(IdentifierParser())).map { rest ->
                if(rest.isEmpty()) {
                    first
                }
                else {
                    rest.fold(first as AstNode) { acc, field -> RecordLookupNode(acc, field, acc.line) }
                }
            }
        }.parse(tokens, index, env)
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

class RecordExpandParser(): Parser<RecordExpandNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<RecordExpandNode> {
        val entry = AndParser(IdentifierParser().lCompose(SpecialCharParser("=")), PrecedenceParsers.ConstLevel())

        return LCurlParser().rCompose(PrecedenceParsers.ConstLevel()).bind { original ->
            KeywordParser("with").rCompose(entry).bind { first ->
                ZeroOrMore(SpecialCharParser(";").rCompose(entry))
                    .lCompose(OptionalParser(SpecialCharParser(";"))).lCompose(RCurlParser())
                    .map { rest ->
                        val list = listOf(first) + rest
                        val map = mutableMapOf<String, AstNode>()
                        for((k, v) in list) {
                            if(k in map) throw BadRecordException(k)
                            map.put(k, v)
                        }
                        RecordExpandNode(original, map, tokens[index].line)
                    }
            }
        }.parse(tokens, index, env)
    }

}

class RecordLiteralParser(): Parser<RecordLiteralNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<RecordLiteralNode> {
        val entry = AndParser(IdentifierParser().lCompose(SpecialCharParser("=")), PrecedenceParsers.ConstLevel())
        return LCurlParser().rCompose(entry).bind { first ->
            ZeroOrMore(SpecialCharParser(";").rCompose(entry))
                .lCompose(OptionalParser(SpecialCharParser(";"))).lCompose(RCurlParser())
                .map { rest ->
                    val list = listOf(first) + rest
                    val map = mutableMapOf<String, AstNode>()
                    for((k, v) in list) {
                        if(k in map) throw BadRecordException(k)
                        map.put(k, v)
                    }

                    RecordLiteralNode(map, tokens[index].line)
                }
        }.parse(tokens, index, env)
    }

}

class ApplicationParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
        return PrecedenceParsers.ConstLevel().bind {first ->
            ZeroOrMore(PrecedenceParsers.ConstLevel()).map { second: List<AstNode> ->
                if (second.isEmpty()) return@map first

                var app = AppNode(first, second[0], tokens[index].line)
                for (i in 1..<second.size) {
                    app = AppNode(app, second[i], tokens[index].line)
                }
                app
            }
        }.parse(tokens, index, env)
    }
}

class AssertionParser(): Parser<AssertNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AssertNode> {
        return KeywordParser("assert")
            .rCompose(PrecedenceParsers.ConstLevel()).map { AssertNode(it, tokens[index].line) }
            .parse(tokens, index, env)
    }

}

class FunctionParser(): Parser<FunctionNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<FunctionNode> {
        return KeywordParser("fun").rCompose(TypedIdentifierParser()).bind { first ->
            ZeroOrMore(TypedIdentifierParser()).bind { others ->
                SpecialCharParser("-").rCompose(SpecialCharParser(">")).rCompose(ExprParser()).map { second ->
                    val secondFunc = others.foldRight(second, {cur, acc -> FunctionNode(cur, acc, acc.line)})
                    FunctionNode(first, secondFunc, tokens[index].line)
                }
            }
        }.parse(tokens, index, env)
    }
}

class ConNodeParser(): Parser<ConNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConNode> {
        return LongConstructorParser().bind { cons ->
            OptionalParser(PrecedenceParsers.ConstLevel()).map { value ->
                ConNode(cons, value, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}

class ConDefParser(): Parser<ConDefNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConDefNode> {

        val listCons = LParenParser()
            .rCompose(AndParser(SpecialCharParser(":"), SpecialCharParser(":")).map { "${it.first}${it.second}" })
            .lCompose(RParenParser())


        val name = ConstructorParser()
            .disjoint(AndParser(LBracketParser(), RBracketParser()).map { "${it.first}${it.second}"})
            .disjoint(listCons)


        return name.bind { cons ->
            OptionalParser(KeywordParser("of").rCompose(TypeParser())).map { typ ->
                ConDefNode(MBinding(cons, typ), index)
            }
        }.parse(tokens, index, env)
    }

}

class MatchCaseParser(): Parser<MatchCaseNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<MatchCaseNode> {
        val parser = KeywordParser("match").rCompose(ExprParser()).lCompose(KeywordParser("with")).bind { expr ->
            OptionalParser(SpecialCharParser("|")).rCompose(MatchParser()).bind { first ->
                ZeroOrMore(SpecialCharParser("|").rCompose(MatchParser())).lCompose(KeywordParser("end")).map { rest ->
                    val list = ArrayList<Pair<PatternNode, AstNode>>()
                    list.add(first)
                    list.addAll(rest)
                    MatchCaseNode(expr, list, tokens[index].line)
                }
            }
        }
        return parser.parse(tokens, index, env)
    }
}

class TryWithParser(): Parser<TryWithNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TryWithNode> {
        val parser = KeywordParser("try").rCompose(ExprParser()).lCompose(KeywordParser("with")).bind { expr ->
            OptionalParser(SpecialCharParser("|")).rCompose(MatchParser()).bind { first ->
                ZeroOrMore(SpecialCharParser("|").rCompose(MatchParser())).lCompose(KeywordParser("end")).map { rest ->
                    val list = ArrayList<Pair<PatternNode, AstNode>>()
                    list.add(first)
                    list.addAll(rest)
                    TryWithNode(expr, list, tokens[index].line)
                }
            }
        }
        return parser.parse(tokens, index, env)
    }
}


class MatchParser(): Parser<Pair<PatternNode, AstNode>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Pair<PatternNode, AstNode>> {
        //TODO: implement patterns
        val parser = PatternPrecedence.Main().lCompose(SpecialCharParser("-")).lCompose(SpecialCharParser(">")).bind { pattern ->
            ExprParser().map { expr ->
                Pair(pattern, expr)
            }
        }
        return parser.parse(tokens, index, env)
    }
}

class LocalOpenParser(): Parser<LocalOpenNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<LocalOpenNode> {
        return KeywordParser("let").rCompose(KeywordParser("open")).rCompose(LongConstructorParser()).bind { iden ->
            KeywordParser("in").rCompose(ExprParser()).map { expr ->
                LocalOpenNode(iden, expr, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }

}