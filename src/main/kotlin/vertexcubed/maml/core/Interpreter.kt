package vertexcubed.maml.core

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.parsers.ExprParser
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.ForAll
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

class Interpreter {

    var dynEnv: Map<String, MValue>
    var typeEnv: Map<String, ForAll>
    val varTypeEnv: TypeEnv

    init {
        dynEnv = emptyMap()
        typeEnv = emptyMap()
        varTypeEnv = TypeEnv()
    }

    fun registerBuiltin(name: String, argType: MType, retTye: MType, function: (MValue) -> MValue) {
//        val builtin = BuiltinNode(MBinding(name + "_builtin", retTye), argType, 1, function)
//        val wrapBuiltin = FunctionNode(MBinding("p0", argType), AppNode(builtin, VariableNode("p0", 1), 1), 1)
//
//        typeEnv = typeEnv + (name to wrapBuiltin.type(typeEnv, varTypeEnv))
//        dynEnv = dynEnv + (name to wrapBuiltin.eval(dynEnv))
        TODO("Not Yet Implemented")
    }




    fun run(code: String) {
        val lexer = Lexer(code)

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


        println(result.result)
        println("Parse successful. Type checking...")
        val type: MType
        try {
            type = result.result.inferType(typeEnv, varTypeEnv)
        }
        catch(e: TypeException) {
            println(strList[e.line - 1].trim())
            println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
            return
        }

        println(varTypeEnv.types)

        println("Type is $type. Evaluating...")
        try {
            println(result.result.eval(dynEnv))
        }
        catch(e: Exception) {
            println("Runtime Error: $e")
        }
    }
}