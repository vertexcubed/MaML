package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.*
import vertexcubed.maml.parse.preprocess.Associativity
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.core.MBinding
import java.util.*
import kotlin.jvm.optionals.getOrDefault

@Suppress("UNCHECKED_CAST")
class ProgramParser(val terminator: Parser<Any>): Parser<List<AstNode>>() {
    constructor(): this(EOFParser() as Parser<Any>)

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<AstNode>> {
        val parser = ChoiceParser(listOf(
            TopLetParser() as Parser<AstNode>,
            DataTypeDefParser() as Parser<AstNode>,
            StructParser() as Parser<AstNode>,
            TypeAliasParser() as Parser<AstNode>
        ))
        var workingIndex = index
        val output = ArrayList<AstNode>()
        while(workingIndex < tokens.size) {
            if(terminator.parse(tokens, workingIndex, env) is ParseResult.Success) {
                workingIndex--
                break
            }

            val infixRes = InfixParser().parse(tokens, workingIndex, env)
            when(infixRes) {
                is ParseResult.Success -> {
                    try {
                        env.addInfixRule(infixRes.result)
                    }
                    catch(e: IllegalArgumentException) {
                        val precedence = infixRes.result.precedence
                        val m = env.infixMap[precedence]
                            ?: return ParseResult.Failure(workingIndex, tokens[workingIndex], "Catastrophic failure registering infix rule ${infixRes.result}")
                        val expected = m.first
                        val actual = infixRes.result.assoc
                        if(expected == Associativity.NONE) {
                            return ParseResult.Failure(workingIndex, tokens[workingIndex], "Precedence $precedence is non-associative, cannot add additional infix operator!")
                        }
                        return ParseResult.Failure(workingIndex, tokens[workingIndex], "Incompatible associativities for precedence $precedence: Expected $expected, Found: $actual ")
                    }
                    workingIndex = infixRes.newIndex
                    continue
                }
                is ParseResult.Failure -> {
                    //Void on failure lol
                }
            }


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
        return ParseResult.Success(output, workingIndex + 1)
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


class DataTypeDefParser(): Parser<VariantTypeNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<VariantTypeNode> {
        val parser = KeywordParser("type").rCompose(
            idenParser()).lCompose(SpecialCharParser("=")).bind { iden ->
            ConDefParser().bind { first ->
                ZeroOrMore(SpecialCharParser("|").rCompose(ConDefParser())).map { second ->
                    val list = ArrayList<ConDefNode>()
                    list.add(first)
                    list.addAll(second)
                    VariantTypeNode(iden.first, iden.second, list, tokens[index].line)
                }
            }
        }
        return parser.parse(tokens, index, env)
    }

    private fun idenParser(): Parser<Pair<String, List<TypeVarDummy>>> {
        return OptionalParser(MultiTypeVarTypeParser()).bind { args ->
            IdentifierParser().map { name ->
                Pair(name, args.getOrDefault(emptyList()))
            }
        }
    }
}

class TypeAliasParser(): Parser<TypeAliasNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TypeAliasNode> {
        val parser = KeywordParser("type").rCompose(idenParser()).lCompose(SpecialCharParser("=")).bind { iden ->
            TypeParser().map { type ->
                TypeAliasNode(iden.first, iden.second, type, index)
            }
        }
        return parser.parse(tokens, index, env)
    }

    private fun idenParser(): Parser<Pair<String, List<TypeVarDummy>>> {
        return OptionalParser(MultiTypeVarTypeParser()).bind { args ->
            IdentifierParser().map { name ->
                Pair(name, args.getOrDefault(emptyList()))
            }
        }
    }
}


@Suppress("UNCHECKED_CAST")
class StructParser(): Parser<ModuleStructNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ModuleStructNode> {
        return KeywordParser("module").rCompose(ConstructorParser()).lCompose(SpecialCharParser("=")).bind { name ->
            KeywordParser("struct").rCompose(ProgramParser(KeywordParser("end") as Parser<Any>)).lCompose(KeywordParser("end")).map { nodes ->
                ModuleStructNode(name, nodes, tokens[index].line)
            }
        }.parse(tokens, index, env)
    }
}