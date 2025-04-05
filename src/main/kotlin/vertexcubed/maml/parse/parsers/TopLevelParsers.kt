package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.TokenType
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.MBinding
import java.util.*

@Suppress("UNCHECKED_CAST")
class ProgramParser(): Parser<Program>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Program> {
        val parser = ChoiceParser(listOf(
            TopLetParser() as Parser<AstNode>,
            DataTypeDefParser() as Parser<AstNode>,

        ))
        var workingIndex = index
        val output = ArrayList<AstNode>()
        while(workingIndex < tokens.size && tokens[workingIndex].type != TokenType.EOF) {
            val res = parser.parse(tokens, workingIndex, env)
            when(res) {
                is ParseResult.Success -> {
                    workingIndex = res.newIndex
                    output.add(res.result)
                }
                is ParseResult.Failure -> {
                    return res.newResult()
                }
            }
        }
        return ParseResult.Success(Program(output), index + 1)
    }
}








class TopLetParser(): Parser<TopLetNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TopLetNode> {
        val parser = KeywordParser("let").rCompose(OptionalParser(KeywordParser("rec"))).bind { rec ->
            IdentifierParser().bind { first ->
                ZeroOrMore(TypedIdentifierParser()).bind { arguments ->
                    OptionalParser(SpecialCharParser(":").rCompose(TypeParser())).bind { type ->
                        SpecialCharParser("=").rCompose(ExprParser()).map { second ->
                            if(rec.isPresent() && arguments.isEmpty()) throw ParseException(tokens[index].line, "Only functions can be recursive, not values.")

                            val node = arguments.foldRightIndexed(second, { index, str, exist ->
                                if(rec.isPresent() && index == 0) {
                                    RecursiveFunctionNode(MBinding(first, Optional.empty()), FunctionNode(str, exist, tokens[index].line), tokens[index].line)
                                }
                                else FunctionNode(str, exist, tokens[index].line)
                            })
                            TopLetNode(MBinding(first, type), node, tokens[index].line)
                        }
                    }
                }
            }
        }
        return parser.parse(tokens, index, env)
    }
}


class DataTypeDefParser(): Parser<DataTypeNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DataTypeNode> {
        val parser = KeywordParser("type").rCompose(IdentifierParser()).lCompose(SpecialCharParser("=")).bind { iden ->
            ConDefParser().bind { first ->
                ZeroOrMore(SpecialCharParser("|").rCompose(ConDefParser())).map { second ->
                    val list = ArrayList<ConDefNode>()
                    list.add(first)
                    list.addAll(second)
                    DataTypeNode(iden, list, index)
                }
            }
        }
        return parser.parse(tokens, index, env)
    }
}

class TypeAliasParser(): Parser<TypeAliasNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TypeAliasNode> {
        val parser = KeywordParser("type").rCompose(IdentifierParser()).lCompose(SpecialCharParser("=")).bind { iden ->
            TypeParser().map { type ->
                TypeAliasNode(iden, type, index)
            }
        }
        return parser.parse(tokens, index, env)
    }
}