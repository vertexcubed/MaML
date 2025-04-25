package vertexcubed.maml.parse.parsers

import vertexcubed.maml.ast.NodeLoc
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.*
import vertexcubed.maml.parse.result.ParseResult


//class SingleTypeParser(): Parser<SingleDummy>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<SingleDummy> {
//        if(index < tokens.size) {
//            val first = tokens[index]
//            if(first.type == TokenType.IDENTIFIER) {
//                return ParseResult.Success(SingleDummy(first.lexeme), index + 1)
//            }
//            return ParseResult.Failure(index, first, "Expected token of type ${TokenType.IDENTIFIER} but found ${first.type} (${first.lexeme})")
//        }
//        return ParseResult.Failure(index, tokens.last(), "Expected token of type ${TokenType.IDENTIFIER}, but End of File reached.")
//    }
//}
//
//class TyConTypeParser(): Parser<DummyType>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
//        return OptionalParser(MultiTypeParser()).bind { typeVars ->
//            SingleTypeParser().map { single ->
//                if(typeVars.isEmpty()) {
//                    single
//                }
//                else {
//                    TypeConDummy(single.name, typeVars.get())
//                }
//            }
//        }.disjoint(TypeVarTypeParser() as Parser<DummyType>).parse(tokens, index, env)
//    }
//}
//
//
//class FunTypeParser(): Parser<DummyType>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
//        return TyConTypeParser().bind{ firstType ->
//            ZeroOrMore(
//                SpecialCharParser("-").rCompose(SpecialCharParser(">")).rCompose(FunTypeParser())
//            ).map { moreTypes ->
//                moreTypes.fold(firstType, {acc, rest -> FunctionDummy(rest, acc) })
//            }
//        }.parse(tokens, index, env)
//    }
//}
//
//class TypeParser(): Parser<DummyType>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
//        return FunTypeParser().bind { first ->
//            ZeroOrMore(SpecialCharParser("*").rCompose(FunTypeParser())).map { rest: List<DummyType> ->
//                if (rest.isEmpty()) {
//                    first
//                }
//                else {
//                    val list = ArrayList<DummyType>()
//                    list.add(first)
//                    list.addAll(rest)
//                    TupleDummy(list)
//                }
//            }
//        }.parse(tokens, index, env)
//    }
//}
//
//class MultiTypeParser(): Parser<List<DummyType>>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<DummyType>> {
//        val multi = LParenParser().rCompose((TypeParser()))
//    }
//
//}
//
//
//
//class TypeVarTypeParser(): Parser<TypeVarDummy>() {
//    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TypeVarDummy> {
//        return SpecialCharParser("'").rCompose(IdentifierParser()).map { iden ->
//            TypeVarDummy(iden)
//        }.parse(tokens, index, env)
//    }
//}

//Only used for datatype decl
class MultiTypeVarTypeParser(): Parser<List<TypeVarDummy>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<TypeVarDummy>> {
        val multi = LParenParser().rCompose(TypeVarParser()).bind { first ->
            ZeroOrMore(SpecialCharParser(",").rCompose(TypeVarParser())).lCompose(RParenParser()).map { rest ->
                if(rest.isEmpty()) {
                    listOf(first)
                }
                val a = arrayListOf(first)
                a.addAll(rest)
                //Shut up IntelliJ this is actually needed
                a as List<TypeVarDummy>
            }
        }
        return multi.disjoint(TypeVarParser().map { t -> listOf(t) }).parse(tokens, index, env)
    }

}


class TypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return FunctionTypeParser().parse(tokens, index, env)
    }

}

class FunctionTypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return TupleTypeParser().bind { first ->
            ZeroOrMore(
                CompoundSpecialCharParser("->").rCompose(FunctionTypeParser())
            ).map { moreTypes ->
                moreTypes.foldRight(first, {acc, rest -> FunctionDummy(rest, acc) })
            }
        }.parse(tokens, index, env)
    }
}

class TupleTypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return TyConParser().bind { first ->
            ZeroOrMore(SpecialCharParser("*").rCompose(TyConParser())).map { second ->
                if(second.isEmpty()) {
                    first
                }
                else {
                    val a = arrayListOf(first)
                    a.addAll(second)
                    TupleDummy(a)
                }
            }
        }.parse(tokens, index, env)
    }
}


class TyConParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        val parser = MultiTypeParser().bind { args ->
            OneOrMore(RealTypeParser()).map { rest ->
                rest.reversed().foldRight(args)
                { cur: SingleDummy, acc: List<DummyType> -> listOf(TypeConDummy(cur.name, acc)) }[0]
            }
        }
        return parser.disjoint(SingleTypeParser()).parse(tokens, index, env)
    }
}


class MultiTypeParser(): Parser<List<DummyType>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<DummyType>> {
        return SingleTypeParser().map { t -> listOf(t) }
            .disjoint(
                LParenParser().rCompose(FunctionTypeParser()).bind { first ->
                    ZeroOrMore(SpecialCharParser(",").rCompose(FunctionTypeParser())).lCompose(RParenParser()).map { second ->
                        val a = arrayListOf(first)
                        a.addAll(second)
                        a
                    }
                }
            ).parse(tokens, index, env)
    }

}


@Suppress("UNCHECKED_CAST")
class SingleTypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return ChoiceParser(listOf(
            TypeVarParser() as Parser<DummyType>,
            RealTypeParser() as Parser<DummyType>,
            RecordTypeParser() as Parser<DummyType>,
            LParenParser().rCompose(FunctionTypeParser()).lCompose(RParenParser())
        )).parse(tokens, index, env)
    }
}

class TypeVarParser(): Parser<TypeVarDummy>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TypeVarDummy> {
        return SpecialCharParser("'").rCompose(IdentifierParser()).map { iden ->
            if(iden.indexOf('\'') != -1) {
                throw ParseException(NodeLoc(env.file, tokens[index].line), "Illegal type var label: $iden")
            }
            else {
                TypeVarDummy(iden)
            }
        }.parse(tokens, index, env)
    }
}

class RealTypeParser(): Parser<SingleDummy>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<SingleDummy> {
        return LongIdentifierParser().map { iden -> SingleDummy(iden) }.parse(tokens, index, env)
    }
}

class RecordTypeParser(): Parser<StaticRecordDummy>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<StaticRecordDummy> {
        val entry = AndParser(IdentifierParser().lCompose(SpecialCharParser(":")), FunctionTypeParser())
        return LCurlParser().rCompose(entry).bind { first ->
            ZeroOrMore(SpecialCharParser(";").rCompose(entry))
                .lCompose(OptionalParser(SpecialCharParser(";"))).lCompose(RCurlParser())
                .map { rest ->
                    val list = arrayListOf(first)
                    list.addAll(rest)
                    StaticRecordDummy(list)
                }
        }.parse(tokens, index, env)
    }

}