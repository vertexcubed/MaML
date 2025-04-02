package vertexcubed.maml.parse.parsers

import com.sun.tools.example.debug.expr.ExpressionParser
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.TokenType
import vertexcubed.maml.parse.ast.*
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.MBinding
import vertexcubed.maml.type.MType
import java.util.*

@Suppress("UNCHECKED_CAST")
class ProgramParser(): Parser<Program>() {
    override fun parse(tokens: List<Token>, index: Int): ParseResult<Program> {
        val parser = ChoiceParser(listOf(
            TopLetParser() as Parser<AstNode>,

        ))
        var workingIndex = index
        val output = ArrayList<AstNode>()
        while(workingIndex < tokens.size && tokens[workingIndex].type != TokenType.EOF) {
            val res = parser.parse(tokens, workingIndex)
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
    override fun parse(tokens: List<Token>, index: Int): ParseResult<TopLetNode> {
        val parser = KeywordParser("let").rCompose(OptionalParser(KeywordParser("rec"))).bind { rec ->
            IdentifierParser().bind { first ->
                ZeroOrMore(TupleIdentifierParser().disjoint(TypedIdentifierParser())).bind { arguments ->
                    OptionalParser(SpecialCharParser(":").rCompose((TupleTypeParser() as Parser<MType>).disjoint(TypeParser()))).bind { type ->
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
        return parser.parse(tokens, index)
    }
}