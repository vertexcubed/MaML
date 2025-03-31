package vertexcubed.maml.parse.result

import vertexcubed.maml.parse.Token

sealed class ParseResult<T> {

    data class Success<T>(val result: T, val newIndex: Int) : ParseResult<T>() {}

    data class Failure<T>(val index: Int, val token: Token, val logMessage: String) : ParseResult<T>() {
        fun <V> newResult(): Failure<V> {
            return Failure(index, token, logMessage)
        }
    }
}

