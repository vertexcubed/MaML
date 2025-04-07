package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.result.ParseResult

@Suppress("UNCHECKED_CAST")
class PatternPrecedence {
    class Sub(): Parser<PatternNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<PatternNode> {
            return ChoiceParser(listOf(
                IdentifierPatternParser(),
                ConstantPatternParser() as Parser<PatternNode>,
                TuplePatternParser(),
            )).parse(tokens, index, env)
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

class ConstrPatternParser(): Parser<ConstructorPatternNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ConstructorPatternNode> {
        return ConstructorParser().bind { constr ->
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