package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.ast.*
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.MBinding
import java.util.*

//All the precedence levels.

class PrecedenceParsers {

    //Constants.
    @Suppress("UNCHECKED_CAST")
    class ConstLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return ChoiceParser(listOf(
                ConNodeParser() as Parser<AstNode>,
                IntegerParser() as Parser<AstNode>,
                FloatParser() as Parser<AstNode>,
                CharParser() as Parser<AstNode>,
                StringParser() as Parser<AstNode>,
                TrueParser() as Parser<AstNode>,
                FalseParser() as Parser<AstNode>,
                UnitParser() as Parser<AstNode>,
                ParenthesesParser() as Parser<AstNode>,
                IdentifierParser().map { r -> VariableNode(r, tokens[index].line) }
            )).parse(tokens, index, env)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class AppLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return (ApplicationParser() as Parser<AstNode>).parse(tokens, index, env)
        }
    }

    class UnaryLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            val opParser = SpecialCharParser("-").disjoint(SpecialCharParser("!")).disjoint(KeywordParser("not"))
            return (AppLevel() as Parser<AstNode>).disjoint(opParser.bind { op ->
                AppLevel().map { second ->
                    val uop = when(op) {
                        "-" -> Uop.NEGATE
                        "!" -> Uop.NOT
                        "not" -> Uop.NOT
                        else -> throw AssertionError()
                    }
                    UnaryOpNode(uop, second, tokens[index].line)
                }
            }).parse(tokens, index, env)
        }

    }


    //Multiplication, Division, Modulo. Left Associative.
//    class MultLevel(): Parser<AstNode>() {
//        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
//            return LeftAssocBopParser(UnaryLevel(), listOf(Bop.MUL, Bop.DIV, Bop.MOD)).parse(tokens, index, env)
//        }
//    }

    //Addition, subtraction. Left Associative.
//    class AddLevel(): Parser<AstNode>() {
//        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
//            return LeftAssocBopParser(MultLevel(), listOf(Bop.ADD,Bop.SUB)).parse(tokens, index, env)
//        }
//    }

    //Addition, subtraction. Left Associative.
//    class EqualityLevel(): Parser<AstNode>() {
//        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
//            return LeftAssocBopParser(AddLevel(), listOf(Bop.LTE, Bop.GTE, Bop.NEQ, Bop.EQ, Bop.LT, Bop.GT)).parse(tokens, index, env)
//        }
//    }

    //And operator. Right associative.
//    class AndLevel(): Parser<AstNode>() {
//        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
//            return RightAssocBopParser(EqualityLevel(), listOf(Bop.AND)).parse(tokens, index, env)
//        }
//    }

    //Or operator. Right associative.
//    class OrLevel(): Parser<AstNode>() {
//        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
//            return RightAssocBopParser(AndLevel(), listOf(Bop.OR)).parse(tokens, index, env)
//        }
//    }

    class InfixLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return env.infixParser(UnaryLevel()).parse(tokens, index, env)
        }

    }

    class SequenceLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            val parser: Parser<AstNode> = InfixLevel().bind { first ->
                OptionalParser(SpecialCharParser(";").rCompose(ExprParser())).map { second ->
                    if(second.isPresent) LetNode(MBinding("_", Optional.empty()), first, second.get(), tokens[index].line)
                else first
                }
            }
            return parser.parse(tokens, index, env)
        }
    }

    //if statements.
    @Suppress("UNCHECKED_CAST")
    class IfLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return SequenceLevel().disjoint(IfParser() as Parser<AstNode>).parse(tokens, index, env)
        }
    }

    //Function definitions.
    @Suppress("UNCHECKED_CAST")
    class FunctionLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return IfLevel().disjoint(FunctionParser() as Parser<AstNode>).parse(tokens, index, env)
        }
    }

    //Let statements.
    @Suppress("UNCHECKED_CAST")
    class LetLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return FunctionLevel().disjoint(LetParser() as Parser<AstNode>).parse(tokens, index, env)
        }
    }
}