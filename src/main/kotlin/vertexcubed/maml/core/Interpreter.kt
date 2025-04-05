package vertexcubed.maml.core

import vertexcubed.maml.eval.BooleanValue
import vertexcubed.maml.eval.FunctionValue
import vertexcubed.maml.eval.IntegerValue
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.ast.*
import vertexcubed.maml.parse.parsers.ProgramParser
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.*
import java.util.*

class Interpreter {

    var dynEnv: Map<String, MValue>
    var typeEnv: Map<String, ForAll>
    val varTypeEnv: TypeVarEnv

    init {
        varTypeEnv = TypeVarEnv()
        typeEnv = mapOf(
            "||" to infixType(MBool, MBool),
            "&&" to infixType(MBool, MBool),
            "=" to genericType(MBool),
            "!=" to genericType(MBool),
            "<" to infixType(MInt, MBool),
            "<=" to infixType(MInt, MBool),
            ">" to infixType(MInt, MBool),
            ">=" to infixType(MInt, MBool),
            "+" to infixType(MInt, MInt),
            "-" to infixType(MInt, MInt),
            "*" to infixType(MInt, MInt),
            "/" to infixType(MInt, MInt),
            "%" to infixType(MInt, MInt),
            "mod" to infixType(MInt, MInt),
        )
        dynEnv = emptyMap()
        dynEnv += ("||" to BuiltinOperators.or(dynEnv))
        dynEnv += ("&&" to BuiltinOperators.and(dynEnv))
        dynEnv += ("=" to BuiltinOperators.eq(dynEnv))
        dynEnv += ("!=" to BuiltinOperators.neq(dynEnv))
        dynEnv += ("<" to BuiltinOperators.lt(dynEnv))
        dynEnv += ("<=" to BuiltinOperators.lte(dynEnv))
        dynEnv += (">" to BuiltinOperators.gt(dynEnv))
        dynEnv += (">=" to BuiltinOperators.gte(dynEnv))
        dynEnv += ("+" to BuiltinOperators.add(dynEnv))
        dynEnv += ("-" to BuiltinOperators.sub(dynEnv))
        dynEnv += ("*" to BuiltinOperators.mul(dynEnv))
        dynEnv += ("/" to BuiltinOperators.div(dynEnv))
        dynEnv += ("%" to BuiltinOperators.mod(dynEnv))
        dynEnv += ("mod" to BuiltinOperators.mod(dynEnv))
    }


    private fun infixType(arg: MType, ret: MType): ForAll {
        return ForAll.empty(MFunction(arg, MFunction(arg, ret)))
    }

    private fun genericType(ret: MType): ForAll {
        val newType = varTypeEnv.newTypeVar()
        val rawType = MFunction(newType, MFunction(newType, ret))
        return ForAll.generalize(rawType, varTypeEnv)
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
        val parseEnv = ParseEnv()
        parseEnv.init()
        val result = parser.parse(tokens, parseEnv)
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

class BuiltinOperators {
    companion object {

        fun eq(env: Map<String, MValue>): MValue {
            return basic(env, {x, y -> BooleanValue(x == y)})
        }

        fun neq(env: Map<String, MValue>): MValue {
            return basic(env, {x, y -> BooleanValue(x != y)})
        }

        fun lt(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value < y.value)
            })
        }

        fun lte(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value <= y.value)
            })
        }

        fun gt(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value > y.value)
            })
        }

        fun gte(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value >= y.value)
            })
        }

        fun add(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value + y.value)
            })
        }

        fun sub(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value - y.value)
            })
        }

        fun mul(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value * y.value)
            })
        }

        fun div(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value / y.value)
            })
        }

        fun mod(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value % y.value)
            })
        }

        fun and(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                if(y !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(x.value && y.value)
            })
        }

        fun or(env: Map<String, MValue>): MValue {
            return basic(env, {x, y ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                if(y !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(x.value || y.value)
            })
        }




        private fun basic(env: Map<String, MValue>, func: (MValue, MValue) -> MValue): MValue {
            return FunctionValue("x", FunctionNode(MBinding("y"), BuiltinOpNode({e ->
                val x = e.getOrElse("x", {throw UnboundVarException("x")})
                val y = e.getOrElse("y", {throw UnboundVarException("y")})
                func(x, y)
            }, 1), 1), env)
        }
    }

    class BuiltinOpNode(val evalFunc: (Map<String, MValue>) -> MValue, line: Int): AstNode(line) {
        override fun eval(env: Map<String, MValue>): MValue {
            return evalFunc(env)
        }

        override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
            throw AssertionError("This should never be callled...")
        }

        override fun pretty(): String {
            return "<fun>"
        }

    }
}