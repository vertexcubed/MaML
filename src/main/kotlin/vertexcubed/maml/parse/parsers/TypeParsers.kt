package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.*
import vertexcubed.maml.parse.result.ParseResult


class SingleTypeParser(): Parser<SingleDummy>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<SingleDummy> {
        if(index < tokens.size) {
            val first = tokens[index]
            if(first.type == TokenType.IDENTIFIER) {
                return ParseResult.Success(SingleDummy(first.lexeme), index + 1)
            }
            return ParseResult.Failure(index, first, "Expected token of type ${TokenType.IDENTIFIER} but found ${first.type} (${first.lexeme})")
        }
        return ParseResult.Failure(index, tokens.last(), "Expected token of type ${TokenType.IDENTIFIER}, but End of File reached.")
    }
}

class TyConTypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return ZeroOrMore(TypeVarTypeParser()).bind { typeVars ->
            SingleTypeParser().map { single ->
                if(typeVars.isEmpty()) {
                    single
                }
                else {
                    TypeConDummy(single.name, typeVars)
                }
            }
        }.disjoint(TypeVarTypeParser() as Parser<DummyType>).parse(tokens, index, env)
    }

}


class FunTypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return TyConTypeParser().bind{ firstType ->
            ZeroOrMore(
                SpecialCharParser("-").rCompose(SpecialCharParser(">")).rCompose(FunTypeParser())
            ).map { moreTypes ->
                moreTypes.fold(firstType, {acc, rest -> FunctionDummy(rest, acc) })
            }
        }.parse(tokens, index, env)
    }
}

class TypeParser(): Parser<DummyType>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<DummyType> {
        return FunTypeParser().bind { first ->
            ZeroOrMore(SpecialCharParser("*").rCompose(FunTypeParser())).map { rest: List<DummyType> ->
                if (rest.isEmpty()) {
                    first
                }
                else {
                    val list = ArrayList<DummyType>()
                    list.add(first)
                    list.addAll(rest)
                    TupleDummy(list)
                }
            }
        }.parse(tokens, index, env)
    }
}

class TypeVarTypeParser(): Parser<TypeVarDummy>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<TypeVarDummy> {
        return SpecialCharParser("'").rCompose(IdentifierParser()).map { iden ->
            TypeVarDummy(iden)
        }.parse(tokens, index, env)
    }
}