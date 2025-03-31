package vertexcubed.maml.parse.parsers

import vertexcubed.maml.eval.ParseException
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.ast.*
import vertexcubed.maml.parse.result.ParseResult
import kotlin.math.pow

//Parse an expression.
class ExprParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
        return PrecedenceParsers.LetLevel().parse(tokens, index)
    }
}

class LetParser(): Parser<LetNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<LetNode> {
        val parser = KeywordParser("let").rCompose(OptionalParser(KeywordParser("rec"))).bind { rec ->
            IdentifierParser().bind { first ->
                    ZeroOrMore(IdentifierParser()).bind { arguments ->
                    SpecialCharParser("=").rCompose(ExprParser()).bind { second ->
                        KeywordParser("in").rCompose(ExprParser()).map { third ->
                            if(rec.isPresent() && arguments.isEmpty()) throw ParseException(tokens[index].line, "Only functions can be recursive, not values.")
                            val node = arguments.foldRightIndexed(second, { index, str, exist ->
                                if(rec.isPresent() && index == 0) {
                                    RecursiveFunctionNode(first, FunctionNode(str, exist))
                                }
                                else FunctionNode(str, exist)})
                            LetNode(first, node, third)
                        }
                    }
                }
            }

        }
        return parser.parse(tokens, index)
    }
}

class IfParser(): Parser<IfNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<IfNode> {
        val parser = KeywordParser("if").rCompose(ExprParser()).bind { first ->
            KeywordParser("then").rCompose(ExprParser()).bind { second ->
                KeywordParser("else").rCompose(ExprParser()).map { third ->
                    IfNode(
                        first,
                        second,
                        third
                    )
                }
            }
        }
        return parser.parse(tokens, index)
    }
}

class IntegerParser(): Parser<IntegerNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<IntegerNode> {
        val parser =DecimalNumberParser().disjoint(HexNumberParser()).map { second ->
            IntegerNode(second)
        }
        return parser.parse(tokens, index)
    }
}

class StringParser(): Parser<StringNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<StringNode> {
        return StringLitParser().map { str -> StringNode(str) }.parse(tokens, index)
    }
}

class CharParser(): Parser<CharNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<CharNode> {
        return CharLitParser().map { c -> CharNode(c) }.parse(tokens, index)
    }

}

class TrueParser(): Parser<TrueNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<TrueNode> {
        return KeywordParser("true").map {_ -> TrueNode() }.parse(tokens, index)
    }
}

class FalseParser(): Parser<FalseNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<FalseNode> {
        return KeywordParser("false").map {_ -> FalseNode() }.parse(tokens, index)
    }
}

class FloatParser(): Parser<FloatNode>() {
    companion object {
        fun toDec(dec: Long): Float {
            return dec.toFloat() / 10.0.pow(dec.toString().length.toDouble()).toFloat()
        }
    }

    override fun parse(tokens: List<Token>, index: Int): ParseResult<FloatNode> {
        val parser = DecimalNumberParser().lCompose(SpecialCharParser(".")).bind { second ->
            DecimalNumberParser().map { third ->
                FloatNode(second + toDec(third))
            }
        }
        return parser.parse(tokens, index)
    }
}

class UnitParser(): Parser<UnitNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<UnitNode> {
        return LParenParser().rCompose(RParenParser()).map { _ -> UnitNode() }.parse(tokens, index)
    }
}

class ParenthesesExprParser(): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
        return LParenParser().rCompose(ExprParser()).lCompose(RParenParser()).parse(tokens, index)
    }
}

class ApplicationParser(): Parser<AppNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<AppNode> {
        return PrecedenceParsers.ConstLevel().bind {first ->
            OneOrMore(PrecedenceParsers.ConstLevel()).map(fun(second: List<AstNode>): AppNode {
                var app = AppNode(first, second[0])
                for(i in 1..<second.size) {
                    app = AppNode(app, second[i])
                }
                return app
            })
        }.parse(tokens, index)
    }
}

class FunctionParser(): Parser<FunctionNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<FunctionNode> {
        return KeywordParser("fun").rCompose(IdentifierParser()).bind { first ->
            SpecialCharParser("-").rCompose(SpecialCharParser(">")).rCompose(ExprParser()).map { second ->
                FunctionNode(first, second)
            }
        }.parse(tokens, index)
    }
}

class LeftAssocBopParser(val parser: Parser<AstNode>, val bopList: List<Bop>): Parser<AstNode>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
        val parser = BinaryOpParser(parser, bopList).map(
            fun(data: BinaryOpParser.BopData<AstNode>): AstNode {
                val first = data.first
                if(data.rest.isEmpty()) return first
                var node = BinaryOpNode(data.rest[0].first, first, data.rest[0].second)
                for(i in 1..<data.rest.size) {
                    val pair = data.rest[i]
                    node = BinaryOpNode(pair.first, node, pair.second)
                }
                return node
            })
        return parser.parse(tokens, index)
    }
}

class RightAssocBopParser(val parser: Parser<AstNode>, val bopList: List<Bop>): Parser<AstNode>() {

    //TODO: test test test this is very spaghetti
    override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
        val parser = BinaryOpParser(parser, bopList).map(
            fun(data: BinaryOpParser.BopData<AstNode>): AstNode {

                //Convert 2, [(+ 5), (+ 3), (+ 7)] into [(2 +), (5 +), (3 +)], 7
                val first = data.first
                if(data.rest.isEmpty()) return first
                val newList = ArrayList<Pair<AstNode, Bop>>()
                newList.add(Pair(data.first, data.rest[0].first))
                for(i in 1..<data.rest.size) {
                    newList.add(Pair(data.rest[i-1].second, data.rest[i].first))
                }


                //Do same thing as left associative
                val lastIdx = data.rest.size-1
                val last = data.rest[lastIdx].second
                var node = BinaryOpNode(newList[lastIdx].second, newList[lastIdx].first, last)
                for(i in 1..<newList.size) {
                    val pair = newList[i]
                    node = BinaryOpNode(pair.second, pair.first, node)
                }

                return node
            })
        return parser.parse(tokens, index)
    }
}