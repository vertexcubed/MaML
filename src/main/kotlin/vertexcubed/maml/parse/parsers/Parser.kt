package vertexcubed.maml.parse.parsers

import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.Token
import vertexcubed.maml.parse.result.ParseResult

abstract class Parser<T>() {
    fun parse(tokens: List<Token>, env: ParseEnv): ParseResult<T> {
        return parse(tokens, 0, env)
    }

    abstract fun parse(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<T>

    fun <V> rCompose(other: Parser<V>): Parser<V> {
        return RComposeParser<T, V>(this, other)
    }

    fun <V> lCompose(other: Parser<V>): Parser<T> {
        return LComposeParser<T, V>(this, other)
    }

    fun disjoint(other: Parser<T>): Parser<T> {
        return DisjointParser(this, other)
    }

    fun <V> map(function: (T) -> V): Parser<V> {
        return MapParser(this, function)
    }

    fun <V> bind(function: (T) -> Parser<V>): Parser<V> {
        return staticBind(this, function)
    }

    companion object {

        fun <T, V> staticBind(first: Parser<T>, function: (T) -> Parser<V>): Parser<V> {
            return FuncParser(fun(tokens: List<Token>, index: Int, env: ParseEnv): ParseResult<V> {
                return when(val result = first.parse(tokens, index, env)) {
                    is ParseResult.Success -> {
                        function(result.result).parse(tokens, result.newIndex, env)
                    }
                    is ParseResult.Failure -> {
                        result.newResult()
                    }
                }
            })
        }
    }
}

class FuncParser<T>(val func: (tokens: List<Token>, index: Int, env: ParseEnv) -> ParseResult<T>) : Parser<T>() {
    override fun parse(
        tokens: List<Token>,
        index: Int,
        env: ParseEnv
    ): ParseResult<T> {
        return func(tokens, index, env)
    }

}

class PureParser<T>(val data: T) : Parser<T>() {
    override fun parse(
        tokens: List<Token>,
        index: Int,
        env: ParseEnv
    ): ParseResult<T> {
        return ParseResult.Success(data, index)
    }
}

class FailParser(): Parser<Unit>() {
    override fun parse(
        tokens: List<Token>,
        index: Int,
        env: ParseEnv): ParseResult<Unit> {
        return ParseResult.Failure(index, if(index >= tokens.size) tokens.last() else tokens[index], "Always fail parser.")
    }
}