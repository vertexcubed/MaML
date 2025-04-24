package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.BadRecordException
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.result.ParseResult
import java.util.*

@Suppress("UNCHECKED_CAST")
class PatternPrecedence {
    class Sub(): Parser<PatternNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<PatternNode> {
            val each = ChoiceParser(listOf(
                IdentifierPatternParser(),
                ConstantPatternParser() as Parser<PatternNode>,
                TuplePatternParser(),
                RecordPatternParser() as Parser<PatternNode>,
                ListNilPatternParser() as Parser<PatternNode>,
            ))

            val parser = each.bind { first ->
                ZeroOrMore(CompoundSpecialCharParser("::").rCompose(each)).map { rest ->
                    rest.foldRight(first) { n, acc ->
                        ConstructorPatternNode(MIdentifier("::"), Optional.of(TuplePatternNode(listOf(n, acc), n.line)), n.line)
                    }
                }
            }


            return parser.parse(tokens, index, env)
        }
    }

    class Main(): Parser<PatternNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<PatternNode> {
            return (ConstrPatternParser() as Parser<PatternNode>).disjoint(OrPatternParser()).parse(tokens, index, env)
        }

    }
}

@Suppress("UNCHECKED_CAST")
class ConstantPatternParser(): Parser<ConstantPatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConstantPatternNode> {
        return ChoiceParser(listOf(
            FloatParser() as Parser<AstNode>,
            IntegerParser() as Parser<AstNode>,
            CharParser() as Parser<AstNode>,
            StringParser() as Parser<AstNode>,
            TrueParser() as Parser<AstNode>,
            FalseParser() as Parser<AstNode>,
            UnitParser() as Parser<AstNode>,
        )).map { node -> ConstantPatternNode(node, node.line) }.parse(tokens, index, env)
    }
}



class ListNilPatternParser(): Parser<ConstructorPatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConstructorPatternNode> {
        return LBracketParser().rCompose(RBracketParser()).map {
            ConstructorPatternNode(MIdentifier("[]"), Optional.empty(), tokens[index].line)
        }.parse(tokens, index, env)
    }
}


class ConstrPatternParser(): Parser<ConstructorPatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConstructorPatternNode> {
        return LongConstructorParser().bind { constr ->
            OptionalParser(PatternPrecedence.Main()).map { pattern ->
                ConstructorPatternNode(constr, pattern, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}

class IdentifierPatternParser(): Parser<PatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<PatternNode> {
        return IdentifierParser().map { iden ->
            if(iden == "_") {
                WildcardPatternNode(tokens[index].line)
            }
            else {
                VariablePatternNode(iden, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}

class OrPatternParser(): Parser<PatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<PatternNode> {
        return PatternPrecedence.Sub().bind { first ->
            ZeroOrMore(SpecialCharParser("|").rCompose(PatternPrecedence.Sub())).map { rest ->
                if(rest.isEmpty()) {
                    first
                }
                else {
                    val list = arrayListOf(first)
                    list.addAll(rest)
                    OrPatternNode(list, first.line)
                }
            }
        }.parse(tokens, index, env)
    }
}

class TuplePatternParser(): Parser<PatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<PatternNode> {
        return LParenParser().rCompose(PatternPrecedence.Main()).bind { first ->
            ZeroOrMore(SpecialCharParser(",").rCompose(PatternPrecedence.Main())).lCompose(RParenParser()).map { rest ->
                if(rest.isEmpty()) {
                    first
                }
                else {
                    val list = arrayListOf(first)
                    list.addAll(rest)
                    TuplePatternNode(list, first.line)
                }
            }
        }.parse(tokens, index, env)
    }
}


class RecordPatternParser(): Parser<RecordPatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<RecordPatternNode> {
        val entry = AndParser(IdentifierParser().lCompose(SpecialCharParser("=")), PatternPrecedence.Main())
        val parser = LCurlParser().rCompose(entry).bind { first ->
            ZeroOrMore(SpecialCharParser(";").rCompose(entry)).bind { rest ->
                SpecialCharParser(";").rCompose(CompoundSpecialCharParser("..")).map { _ ->
                    val list = listOf(first) + rest
                    val map = mutableMapOf<String, PatternNode>()
                    for((k, v) in list) {
                        if(k in map) throw BadRecordException(k)
                        map.put(k, v)
                    }
                    RecordPatternNode(map, true, tokens[index].line)
                }.disjoint(
                    OptionalParser(SpecialCharParser(";")).map { _ ->
                        val list = listOf(first) + rest
                        val map = mutableMapOf<String, PatternNode>()
                        for((k, v) in list) {
                            if(k in map) throw BadRecordException(k)
                            map.put(k, v)
                        }
                        RecordPatternNode(map, false, tokens[index].line)
                    }
                ).lCompose(RCurlParser())
            }
        }
        return parser.parse(tokens, index, env)
    }

}