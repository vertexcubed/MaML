package vertexcubed.maml

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.TypeException
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
    val lexer = Lexer(code)

    val emptyEnv = emptyMap<String, MValue>()
    val emptyTypeEnv = emptyMap<String, MType>()


    val builtinTest = BuiltinNode(MBinding("print_builtin", MUnit), MString, 1, {
            mArg ->
        println(mArg)
        UnitValue
    })

    val wrapBuiltin = FunctionNode(MBinding("p0", MString), AppNode(builtinTest, VariableNode("p0", 1), 1), 1)

    var realTypeEnv = emptyTypeEnv + ("print" to wrapBuiltin.type(emptyTypeEnv))
    var realEnv = emptyEnv + ("print" to wrapBuiltin.eval(emptyEnv))


    val strList = lexer.toStringList()


    val parser = ExprParser()
    val result = parser.parse(lexer.read())
    if(result is ParseResult.Failure) {
        val line = result.token.line
        println(strList[line - 1].trim())
        println("Syntax Error: ${result.logMessage}")
        return
    }
    if(result !is ParseResult.Success) {
        println("Catastrophic failure: parse result isn't a success OR failure.")
        return
    }
    println("Parse successful. Type checking...")
    val type: MType
    try {
        type = result.result.type(realTypeEnv)
    }
    catch(e: TypeException) {
        println(strList[e.line - 1].trim())
        println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
        return
    }
    println("Type is $type. Evaluating...")
    try {
        println(result.result.eval(realEnv))
    }
    catch(e: Exception) {
        println("Runtime Error: $e")
    }

}