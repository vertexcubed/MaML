package vertexcubed.maml.core

import vertexcubed.maml.ast.*
import vertexcubed.maml.eval.*
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.parsers.EOFParser
import vertexcubed.maml.parse.parsers.ProgramParser
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.*
import java.util.*

class Interpreter {

    var dynEnv: Map<String, MValue>
    var typeEnv: TypeEnv
    val typeSystem: TypeSystem = TypeSystem()

    init {
        typeEnv = TypeEnv(typeSystem)
        typeEnv.addAllBindings(mapOf(
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
            "+." to infixType(MFloat, MFloat),
            "-." to infixType(MFloat, MFloat),
            "*." to infixType(MFloat, MFloat),
            "/." to infixType(MFloat, MFloat),
            "%." to infixType(MFloat, MFloat),

            //Unary prefix operators
            "~-" to ForAll.empty(MFunction(MInt, MInt)),
            "~-." to ForAll.empty(MFunction(MFloat, MFloat)),
            "!" to ForAll.empty(MFunction(MBool, MBool)),
        ))
        typeEnv.addAllTypes(mapOf(
            "int" to ForAll.empty(MInt),
            "float" to ForAll.empty(MFloat),
            "unit" to ForAll.empty(MUnit),
            "bool" to ForAll.empty(MBool),
            "char" to ForAll.empty(MChar),
            "string" to ForAll.empty(MString),
        ))
        dynEnv = emptyMap()
        dynEnv = mapOf(
            "||" to BuiltinOperators.or(dynEnv),
            "&&" to BuiltinOperators.and(dynEnv),
            "=" to BuiltinOperators.eq(dynEnv),
            "!=" to BuiltinOperators.neq(dynEnv),
            "<" to BuiltinOperators.lt(dynEnv),
            "<=" to BuiltinOperators.lte(dynEnv),
            ">" to BuiltinOperators.gt(dynEnv),
            ">=" to BuiltinOperators.gte(dynEnv),
            "+" to BuiltinOperators.add(dynEnv),
            "-" to BuiltinOperators.sub(dynEnv),
            "*" to BuiltinOperators.mul(dynEnv),
            "/" to BuiltinOperators.div(dynEnv),
            "%" to BuiltinOperators.mod(dynEnv),
            "+." to BuiltinOperators.addf(dynEnv),
            "-." to BuiltinOperators.subf(dynEnv),
            "*." to BuiltinOperators.mulf(dynEnv),
            "/." to BuiltinOperators.divf(dynEnv),
            "%." to BuiltinOperators.modf(dynEnv),

            //Unary prefix operators
            "~-" to BuiltinOperators.negate(dynEnv),
            "~-." to BuiltinOperators.negatef(dynEnv),
            "!" to BuiltinOperators.not(dynEnv),
        )

    }


    private fun infixType(arg: MType, ret: MType): ForAll {
        return ForAll.empty(MFunction(arg, MFunction(arg, ret)))
    }

    private fun genericType(ret: MType): ForAll {
        val newType = typeSystem.newTypeVar()
        val rawType = MFunction(newType, MFunction(newType, ret))
        return ForAll.generalize(rawType, typeSystem)
    }


    fun registerBuiltin(name: String, function: (MValue) -> MValue) {
        val builtin = BuiltinNode(MBinding(name + "_builtin", Optional.empty()), 1, function)
        val wrapBuiltin = FunctionNode(MBinding("p0", Optional.empty()), AppNode(builtin, VariableNode("p0", 1), 1), 1)

        val wrapType = wrapBuiltin.inferType(typeEnv)
        val scheme = ForAll.generalize(wrapType, typeSystem)

        typeEnv = typeEnv.copy()
        typeEnv.addBinding(name to scheme)
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


        val program = Program(result.result, dynEnv, typeEnv)
        println(program.nodes)
        println("Parse successful. Type checking...")
        try {
            program.inferTypes()
        }
        catch(e: TypeCheckException) {
            println(strList[e.line - 1].trim())
            println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
            return
        }
        typeSystem.normalizeTypeNames()

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
            return bop(env, { x, y -> BooleanValue(x == y)})
        }

        fun neq(env: Map<String, MValue>): MValue {
            return bop(env, { x, y -> BooleanValue(x != y)})
        }

        fun lt(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value < y.value)
            })
        }

        fun lte(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value <= y.value)
            })
        }

        fun gt(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value > y.value)
            })
        }

        fun gte(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value >= y.value)
            })
        }

        fun add(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value + y.value)
            })
        }

        fun sub(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value - y.value)
            })
        }

        fun mul(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value * y.value)
            })
        }

        fun div(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value / y.value)
            })
        }

        fun mod(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value % y.value)
            })
        }

        fun negate(env: Map<String, MValue>): MValue {
            return uop(env, { x ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(-x.value)
            })
        }

        fun addf(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value + y.value)
            })
        }

        fun subf(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value - y.value)
            })
        }

        fun mulf(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value * y.value)
            })
        }

        fun divf(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value / y.value)
            })
        }

        fun modf(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value % y.value)
            })
        }

        fun negatef(env: Map<String, MValue>): MValue {
            return uop(env, { x ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(-x.value)
            })
        }

        fun and(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                if(y !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(x.value && y.value)
            })
        }

        fun or(env: Map<String, MValue>): MValue {
            return bop(env, { x, y ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                if(y !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(x.value || y.value)
            })
        }

        fun not(env: Map<String, MValue>): MValue {
            return uop(env, { x ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(!x.value)
            })
        }

        private fun uop(env: Map<String, MValue>, func: (MValue) -> MValue): MValue {
            return FunctionValue("x", BuiltinOpNode({ e ->
                val x = e.getOrElse("x", {throw UnboundVarException("x")})
                func(x)
            }, 1), env)
        }



        private fun bop(env: Map<String, MValue>, func: (MValue, MValue) -> MValue): MValue {
            return FunctionValue("x", FunctionNode(MBinding("y"), BuiltinOpNode({ e ->
                val x = e.getOrElse("x", {throw UnboundVarException("x")})
                val y = e.getOrElse("y", {throw UnboundVarException("y")})
                func(x, y)
            }, 1), 1), env)
        }
    }


    //TODO: this is very fragile and unsafe. Replace with much more stable version.
    class BuiltinOpNode(val evalFunc: (Map<String, MValue>) -> MValue, line: Int): AstNode(line) {
        override fun eval(env: Map<String, MValue>): MValue {
            return evalFunc(env)
        }

        override fun inferType(env: TypeEnv): MType {
            throw AssertionError("This should never be callled...")
        }

        override fun pretty(): String {
            return "<fun>"
        }

    }
}