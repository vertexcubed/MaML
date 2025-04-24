package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.parse.result.ParseResult
import kotlin.jvm.optionals.getOrDefault

class ValParser(): Parser<ValSigNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ValSigNode> {
        return KeywordParser("val").rCompose(IdentifierParser()).bind { iden ->
            SpecialCharParser(":").rCompose(TypeParser()).map { type ->
                ValSigNode(iden, type, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}

class AbstractTypeParser(): Parser<TypeSigNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TypeSigNode> {
        return KeywordParser("type").rCompose(idenParser()).map { (name, args) ->
            TypeSigNode(name, args, tokens[index].line)
        }.parse(tokens, index, env)
    }

    private fun idenParser(): Parser<Pair<String, List<TypeVarDummy>>> {
        return OptionalParser(MultiTypeVarTypeParser()).bind { args ->
            IdentifierParser().map { name ->
                Pair(name, args.getOrDefault(emptyList()))
            }
        }
    }

}

class OpenSigParser(): Parser<OpenSigNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<OpenSigNode> {
        val parseRes = KeywordParser("open").rCompose(LongConstructorParser()).map { iden ->
            OpenSigNode(iden, tokens[index].line)
        }
        return parseRes.parse(tokens, index, env)
    }
}

class IncludeSigParser(): Parser<IncludeSigNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<IncludeSigNode> {
        val parseRes = KeywordParser("include").rCompose(LongConstructorParser()).map { iden ->
            IncludeSigNode(iden, tokens[index].line)
        }
        return parseRes.parse(tokens, index, env)
    }
}