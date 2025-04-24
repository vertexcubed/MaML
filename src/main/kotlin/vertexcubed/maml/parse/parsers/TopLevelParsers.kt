package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.MBinding
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.parse.preprocess.Associativity
import vertexcubed.maml.parse.result.ParseResult
import java.util.*
import kotlin.jvm.optionals.getOrDefault

@Suppress("UNCHECKED_CAST")
class ProgramParser(val terminator: Parser<Any>): Parser<List<AstNode>>() {
    constructor(): this(EOFParser() as Parser<Any>)

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<AstNode>> {
        val parser = ChoiceParser(listOf(
            TopLetParser() as Parser<AstNode>,
            TypeAliasParser() as Parser<AstNode>,
            DataTypeDefParser() as Parser<AstNode>,
            StructParser() as Parser<AstNode>,
            SigParser() as Parser<AstNode>,
            TopOpenParser() as Parser<AstNode>,
            TopIncludeParser() as Parser<AstNode>,
            ExternalDefParser() as Parser<AstNode>,
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

                    if(res.result is TopOpenNode) {
                        val module = env.lookupModule(res.result.name)
                        if(module.isPresent) {
                            env.addAllFrom(module.get().parseEnv)
                        }
                    }

                    if(res.result is TopIncludeNode) {
                        val module = env.lookupModule(res.result.name)
                        if(module.isPresent) {
                            env.addAllFrom(module.get().parseEnv)
                        }
                    }

                    if(res.result is ModuleStructNode) {
                        env.addModule(res.result)
                    }


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
            LetBindingParser().bind { first ->
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

class TopOpenParser(): Parser<TopOpenNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TopOpenNode> {
        val parseRes = KeywordParser("open").rCompose(LongConstructorParser()).map { iden ->
            TopOpenNode(iden, tokens[index].line)
        }
        return parseRes.parse(tokens, index, env)
    }
}

class TopIncludeParser(): Parser<TopIncludeNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TopIncludeNode> {
        val parseRes = KeywordParser("include").rCompose(LongConstructorParser()).map { iden ->
            TopIncludeNode(iden, tokens[index].line)
        }
        return parseRes.parse(tokens, index, env)
    }
}

class ExternalDefParser(): Parser<ExternalDefNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ExternalDefNode> {
        return KeywordParser("external").rCompose(LetBindingParser()).bind { name ->
            SpecialCharParser(":").rCompose(TypeParser()).bind { type ->
                SpecialCharParser("=").rCompose(StringLitParser()).map { lit ->
                    ExternalDefNode(name, type, lit, tokens[index].line)
                }
            }
        }.parse(tokens, index, env)
    }

}

@Suppress("UNCHECKED_CAST")
class StructParser(): Parser<ModuleStructNode>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ModuleStructNode> {
        val newEnv = env.copy()
        return KeywordParser("module").rCompose(ConstructorParser()).bind { name ->
            OptionalParser(SpecialCharParser(":").rCompose(LongConstructorParser())).lCompose(SpecialCharParser("=")).bind { sig ->
                KeywordParser("struct").rCompose(ProgramParser(KeywordParser("end") as Parser<Any>)).lCompose(KeywordParser("end")).map { nodes ->
                    ModuleStructNode(name, nodes, sig, newEnv, tokens[index].line)
                }
            }
        }.parse(tokens, index, newEnv)
    }
}


class SigParser(): Parser<ModuleSigNode>() {

    @Suppress("UNCHECKED_CAST")
    class SigNodes(): Parser<List<SigNode>>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<SigNode>> {
            val parser = ChoiceParser(listOf(
                ValParser() as Parser<SigNode>,
                AbstractTypeParser() as Parser<SigNode>,
                OpenSigParser() as Parser<SigNode>,
                IncludeSigParser() as Parser<SigNode>,
            ))

            var workingIndex = index
            val output = arrayListOf<SigNode>()
            while(workingIndex < tokens.size) {
                if(KeywordParser("end").parse(tokens, workingIndex, env) is ParseResult.Success) {
                    workingIndex--
                    break
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

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<ModuleSigNode> {
        val newEnv = env.copy()
        return KeywordParser("module").rCompose(KeywordParser("type"))
            .rCompose(ConstructorParser()).lCompose(SpecialCharParser("=")).bind { name ->
            KeywordParser("sig").rCompose(SigNodes()).lCompose(KeywordParser("end")).map {nodes ->
                ModuleSigNode(name, nodes, newEnv, tokens[index].line)
            }
        }.parse(tokens, index, newEnv)
    }

}