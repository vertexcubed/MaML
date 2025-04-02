package vertexcubed.maml.core

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ast.AppNode
import vertexcubed.maml.parse.ast.BuiltinNode
import vertexcubed.maml.parse.ast.FunctionNode
import vertexcubed.maml.parse.ast.VariableNode
import vertexcubed.maml.parse.parsers.ExprParser
import vertexcubed.maml.parse.parsers.ProgramParser
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.ForAll
import vertexcubed.maml.type.MBinding
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeVarEnv
import java.util.*

class Interpreter {

    var dynEnv: Map<String, MValue>
    var typeEnv: Map<String, ForAll>
    val varTypeEnv: TypeVarEnv

    init {
        dynEnv = emptyMap()
        typeEnv = emptyMap()
        varTypeEnv = TypeVarEnv()
    }

    fun registerBuiltin(name: String, function: (MValue) -> MValue) {
        val builtin = BuiltinNode(MBinding(name + "_builtin", Optional.empty()), 1, function)
        val wrapBuiltin = FunctionNode(MBinding("p0", Optional.empty()), AppNode(builtin, VariableNode("p0", 1), 1), 1)

        val wrapType = wrapBuiltin.inferType(typeEnv, varTypeEnv)
        val scheme = ForAll.generalize(wrapType, varTypeEnv)

        typeEnv = typeEnv + (name to scheme)
        dynEnv = dynEnv + (name to wrapBuiltin.eval(dynEnv))
    }




    fun run(code: String) {
        val lexer = Lexer(code)

        val strList = lexer.toStringList()


        val parser = ProgramParser()
        val tokens = lexer.read()
        println(tokens)
        val result = parser.parse(tokens)
        if(result is ParseResult.Failure) {
            val line = result.token.line
            println(strList[line - 1].trim())
//            println("Syntax Error: ${result.logMessage}")
            println(result)
            return
        }
        if(result !is ParseResult.Success) {
            println("Catastrophic failure: parse result isn't a success OR failure.")
            return
        }


        val program = result.result
        program.init(dynEnv, typeEnv)
        println(program.nodes)
        println("Parse successful. Type checking...")
        try {
            program.inferTypes(varTypeEnv)
        }
        catch(e: TypeCheckException) {
            println(strList[e.line - 1].trim())
            println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
            return
        }
        varTypeEnv.normalizeTypeNames()

        println("Type checked. Evaluating...")
        try {
            program.eval()
        }
        catch(e: Exception) {
            println("Runtime Error: $e")
        }
    }
}