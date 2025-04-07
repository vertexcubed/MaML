package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.*
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
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
                FloatParser() as Parser<AstNode>,
                IntegerParser() as Parser<AstNode>,
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


            val opParser =
                    CompoundSpecialCharParser("-.")
                    .disjoint(SpecialCharParser("-"))
                    .disjoint(SpecialCharParser("!"))
                    .disjoint(CompoundSpecialCharParser("~-."))
                    .disjoint(CompoundSpecialCharParser("~-"))
            return (AppLevel() as Parser<AstNode>).disjoint(opParser.bind { op ->
                AppLevel().map { second ->
                    var oper = op
                    //Syntax sugar: - (float) -> ~-. float, - -> ~-, -. -> ~-.
                    if(second is FloatNode && op == "-") {
                        oper = "~-."
                    }
                    else if(op == "-") {
                        oper = "~-"
                    }
                    else if(op == "-.") {
                        oper = "~-."
                    }
                    AppNode(VariableNode(oper, second.line), second, second.line)
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

    class MatchLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            return InfixLevel().disjoint(MatchCaseParser() as Parser<AstNode>).parse(tokens, index, env)
        }

    }



    class SequenceLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<AstNode> {
            val parser: Parser<AstNode> = MatchLevel().bind { first ->
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