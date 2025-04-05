package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.result.ParseResult
import java.util.*

class RComposeParser<T, V>(val first: Parser<T>, val second: Parser<V>) : Parser<V>() {

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<V> {

        return staticBind(first, fun(_: T): Parser<V> {
            return staticBind(second, { second -> PureParser(second)})
        }).parse(tokens, index, env)

    }
}

class LComposeParser<T, V>(val first: Parser<T>, val second: Parser<V>) : Parser<T>() {

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<T> {

        return staticBind(first, fun(firstResult: T): Parser<T> {
            return staticBind(second, fun(_: V): Parser<T> {
                return PureParser(firstResult)
            })
        }).parse(tokens, index, env)
    }
}

class MapParser<T, V>(val first: Parser<T>, val mapFunction: (T) -> V) : Parser<V>() {

    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<V> {
        val firstResult = first.parse(tokens, index, env)
        return when(firstResult) {
            is ParseResult.Success -> ParseResult.Success(mapFunction(firstResult.result), firstResult.newIndex)
            is ParseResult.Failure -> firstResult.newResult()
        }
    }
}

class AndParser<T, V>(val first: Parser<T>, val second: Parser<V>): Parser<Pair<T, V>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Pair<T, V>> {
        val parser = staticBind(first, {firstResult ->
            second.map { secondResult ->
                Pair(firstResult, secondResult)
            }
        })
        return parser.parse(tokens, index, env)
    }
}

class SeriesParser<T>(val list: List<Parser<T>>): Parser<List<T>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<T>> {
        val allResults = ArrayList<T>()
        var newIndex = index
        for(parser in list) {
            val result = parser.parse(tokens, newIndex, env)
            when(result) {
                is ParseResult.Success -> {
                    allResults.add(result.result)
                    newIndex = result.newIndex
                }
                is ParseResult.Failure -> {
                    return result.newResult()
                }
            }
        }
        return ParseResult.Success(allResults, newIndex)
    }

}

class DisjointParser<T>(val first: Parser<T>, val second: Parser<T>): Parser<T>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<T> {
        val firstRes = first.parse(tokens, index, env)
        when (firstRes) {
            is ParseResult.Success -> {
                return firstRes
            }
            is ParseResult.Failure -> {
                val secondRes = second.parse(tokens, index, env)
                when(secondRes) {
                    is ParseResult.Success -> {
                        return secondRes
                    }
                    is ParseResult.Failure -> {
                        if(firstRes.index == secondRes.index) {
                            return ParseResult.Failure(index, secondRes.token, "${firstRes.logMessage} | ${secondRes.logMessage}")
                        }
                        else if(firstRes.index > secondRes.index)
                            return firstRes
                        else return secondRes
                    }
                }
            }
        }
    }
}

class ZeroOrMore<T>(val parser: Parser<T>): Parser<List<T>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<T>> {
        val ret = ArrayList<T>()
        var idx = index
        while(idx < tokens.size) {
            val result = parser.parse(tokens, idx, env)
            when(result) {
                is ParseResult.Success -> {
                    ret.add(result.result)
                    idx = result.newIndex
                }
                is ParseResult.Failure -> {
                    break
                }
            }
        }
        return ParseResult.Success(ret, idx)
    }
}

class OneOrMore<T>(val parser: Parser<T>): Parser<List<T>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<List<T>> {
        val ret = ArrayList<T>()
        var idx = index

        val first = parser.parse(tokens, idx, env)
        when(first) {
            is ParseResult.Success -> {
                ret.add(first.result)
                idx = first.newIndex
            }
            is ParseResult.Failure -> {
                return first.newResult()
            }
        }

        while(idx < tokens.size) {
            val result = parser.parse(tokens, idx, env)
            when(result) {
                is ParseResult.Success -> {
                    ret.add(result.result)
                    idx = result.newIndex
                }
                is ParseResult.Failure -> {
                    break
                }
            }
        }
        return ParseResult.Success(ret, idx)
    }
}

class ChoiceParser<T>(val parsers: List<Parser<T>>): Parser<T>() {
    init {
        if(parsers.size == 0) throw IllegalArgumentException("Cannot make Choice parser wtih no parsers!")
    }


    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<T> {
        var ret = parsers[0]
        for(i in 1..<parsers.size) {
            ret = ret.disjoint(parsers[i])
        }
        return ret.parse(tokens, index, env)
    }

}



class OptionalParser<T : Any>(val parser: Parser<T>): Parser<Optional<T>>() {
    override fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<Optional<T>> {
        val parseResult = parser.parse(tokens, index, env)
        return when(parseResult) {
            is ParseResult.Success -> ParseResult.Success(Optional.of(parseResult.result), parseResult.newIndex)
            is ParseResult.Failure -> ParseResult.Success(Optional.empty(), index)
        }
    }
}
