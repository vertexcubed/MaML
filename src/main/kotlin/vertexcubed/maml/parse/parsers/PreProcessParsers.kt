package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.preprocess.Associativity
import vertexcubed.maml.parse.preprocess.InfixRule
import vertexcubed.maml.parse.result.ParseResult

/**
 * Infix operators
 */

class InfixParser(): Parser<InfixRule>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<InfixRule> {
        return (KeywordParser("infix").disjoint(KeywordParser("infixr")).disjoint(KeywordParser("nonfix"))).bind { assoc ->
            DecimalNumberParser().bind { prec ->
                IdentifierParser().map { iden ->
                    val associativity = when(assoc) {
                        "infix" -> Associativity.LEFT
                        "infixr" -> Associativity.RIGHT
                        "nonfix" -> Associativity.NONE
                        else -> throw AssertionError("Should not happen")
                    }
                    InfixRule(iden, prec.toInt(), associativity)
                }
            }
        }.parse(tokens, index, env)
    }
}