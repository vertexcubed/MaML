package vertexcubed.maml

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.parsers.*
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.MType
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import kotlin.io.path.Path


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        error("Invalid number of args!")
    }

    val path = Path(args[0])


    val reader = BufferedReader(FileReader(path.toString()))
    val lines = StringBuilder()
    try {
        var line = reader.readLine()
        while(line != null) {
            lines.append(line)
            lines.append('\n')
            line = reader.readLine()
        }
    }
    catch (e: IOException) {
        e.printStackTrace()
    }
    finally {
        reader.close()
    }
    val code = lines.toString()


    println("Starting execution of file ${args[0]}")
    val lexer = Lexer(code)

    val emptyEnv = emptyMap<String, MValue>()
    val emptyTypeEnv = emptyMap<String, MType>()

    val parser = ExprParser()
    when(val result = parser.parse(lexer.read())) {
        is ParseResult.Success -> {
            println(result.result)
            println("Parse successful. Type checking: ")
            println(result.result.type(emptyTypeEnv))
            println("Type check successful. Evaluating: ")
            println(result.result.eval(emptyEnv))
        }
        is ParseResult.Failure -> {
            println("Syntax Error: ${result.logMessage}")
        }
    }
}