package vertexcubed.maml.core

import vertexcubed.maml.parse.Lexer

class Interpreter {



    fun run(code: String) {
        run(code.split("\n"))
    }

    fun run(code: List<String>) {
//        val lexer = Lexer(code)
//        println(lexer.read())
    }
}