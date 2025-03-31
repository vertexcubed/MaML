package vertexcubed.maml

import vertexcubed.maml.core.Interpreter
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.core.TypeException
import vertexcubed.maml.eval.UnitValue
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ast.AppNode
import vertexcubed.maml.parse.ast.BuiltinNode
import vertexcubed.maml.parse.ast.FunctionNode
import vertexcubed.maml.parse.ast.VariableNode
import vertexcubed.maml.parse.parsers.*
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.MBinding
import vertexcubed.maml.type.MString
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.MUnit
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
//    val lexer = Lexer("if true else 2")
    val interp = Interpreter()

    interp.registerBuiltin("print", MString, MUnit, { arg ->
        println(arg)
        UnitValue
    })

    interp.run(code)
}