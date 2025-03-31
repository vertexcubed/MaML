package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.ast.*
import vertexcubed.maml.parse.result.ParseResult

//All the precedence levels.

class PrecedenceParsers {

    //Constants.
    @Suppress("UNCHECKED_CAST")
    class ConstLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return ChoiceParser(listOf(
                IntegerParser() as Parser<AstNode>,
                FloatParser() as Parser<AstNode>,
                CharParser() as Parser<AstNode>,
                StringParser() as Parser<AstNode>,
                TrueParser() as Parser<AstNode>,
                FalseParser() as Parser<AstNode>,
                UnitParser() as Parser<AstNode>,
                ParenthesesExprParser() as Parser<AstNode>,
                IdentifierParser().map { r -> VariableNode(r, tokens[index].line) }
            )).parse(tokens, index)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class AppLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return (ApplicationParser() as Parser<AstNode>).disjoint(ConstLevel()).parse(tokens, index)
        }
    }

    class UnaryLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
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
            }).parse(tokens, index)
        }

    }


    //Multiplication, Division, Modulo. Left Associative.
    class MultLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return LeftAssocBopParser(UnaryLevel(), listOf(Bop.MUL, Bop.DIV, Bop.MOD)).parse(tokens, index)
        }
    }

    //Addition, subtraction. Left Associative.
    class AddLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return LeftAssocBopParser(MultLevel(), listOf(Bop.ADD,Bop.SUB)).parse(tokens, index)
        }
    }

    //Addition, subtraction. Left Associative.
    class EqualityLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return LeftAssocBopParser(AddLevel(), listOf(Bop.LTE, Bop.GTE, Bop.NEQ, Bop.EQ, Bop.LT, Bop.GT)).parse(tokens, index)
        }
    }

    //And operator. Right associative.
    class AndLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return RightAssocBopParser(EqualityLevel(), listOf(Bop.AND)).parse(tokens, index)
        }
    }

    //Or operator. Right associative.
    class OrLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return RightAssocBopParser(AndLevel(), listOf(Bop.OR)).parse(tokens, index)
        }
    }

    //if statements.
    @Suppress("UNCHECKED_CAST")
    class IfLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return OrLevel().disjoint(IfParser() as Parser<AstNode>).parse(tokens, index)
        }
    }

    //Function definitions.
    @Suppress("UNCHECKED_CAST")
    class FunctionLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return IfLevel().disjoint(FunctionParser() as Parser<AstNode>).parse(tokens, index)
        }
    }

    //Let statements.
    @Suppress("UNCHECKED_CAST")
    class LetLevel(): Parser<AstNode>() {
        override fun parse(tokens: List<Token>, index: Int): ParseResult<AstNode> {
            return FunctionLevel().disjoint(LetParser() as Parser<AstNode>).parse(tokens, index)
        }
    }












}