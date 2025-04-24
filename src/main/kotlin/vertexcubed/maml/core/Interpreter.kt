package vertexcubed.maml.core

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.ast.FunctionNode
import vertexcubed.maml.ast.ModuleStructNode
import vertexcubed.maml.eval.*
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.parsers.ProgramParser
import vertexcubed.maml.parse.result.ParseResult
import vertexcubed.maml.type.*
import java.util.*

class Interpreter {

    private val namedValues: MutableMap<String, MValue>
    var dynEnv: DynEnv
    var typeEnv: TypeEnv

    val typeSystem: TypeSystem = TypeSystem()


    //TODO: IMPLEMENT AND/OR SHORT CIRCUITING
    init {
        namedValues = mutableMapOf()

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
        dynEnv = DynEnv()
        dynEnv.addAllBindings(mapOf(
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
        ))




        registerExternal("maml_register_named_value") { vname, value ->
            val name = stringOf(vname)!!
            namedValues[name] = value
            UnitValue
        }
    }


    private fun infixType(arg: MType, ret: MType): ForAll {
        return ForAll.empty(MFunction(arg, MFunction(arg, ret)))
    }

    private fun genericType(ret: MType): ForAll {
        val newType = typeSystem.newTypeVar()
        val rawType = MFunction(newType, MFunction(newType, ret))
        return ForAll.generalize(rawType, typeSystem)
    }

    fun namedValue(name: String): MValue? {
        return namedValues[name]
    }

    fun callback(vfunc: MValue, vararg args: MValue): MValue? {
        if(args.isEmpty()) return null

        var last = toFunction(vfunc)?.invoke(args[0])
        for(i in 1 until args.size) {
            if(last == null) return null
            last = toFunction(last)?.invoke(args[i])
        }
        return last
    }

    fun callback(name: String, vararg args: MValue): MValue? {
        val func = namedValue(name) ?: return null
        return callback(func, *args)
    }

    fun registerExternal(name: String, function: () -> MValue) {
        registerExternalArr(name) { args ->
            function()
        }
    }

    fun registerExternal(name: String, function: (MValue) -> MValue) {
        registerExternalArr(name) { args: Array<MValue> ->
            function(args[0])
        }
    }
    fun registerExternal(name: String, function: (MValue, MValue) -> MValue) {
        registerExternalArr(name) { args: Array<MValue> ->
            function(args[0], args[1])
        }
    }

    fun registerExternal(name: String, function: (MValue, MValue, MValue) -> MValue) {
        registerExternalArr(name) { args: Array<MValue> ->
            function(args[0], args[1], args[2])
        }
    }

    fun registerExternal(name: String, function: (MValue, MValue, MValue, MValue) -> MValue) {
        registerExternalArr(name) { args: Array<MValue> ->
            function(args[0], args[1], args[2], args[3])
        }
    }

    fun registerExternal(name: String, function: (MValue, MValue, MValue, MValue, MValue) -> MValue) {
        registerExternalArr(name) { args: Array<MValue> ->
            function(args[0], args[1], args[2], args[3], args[4])
        }
    }

    fun registerExternal(name: String, function: (MValue, MValue, MValue, MValue, MValue, MValue) -> MValue) {
        registerExternalArr(name) { args: Array<MValue> ->
            function(args[0], args[1], args[2], args[3], args[4], args[5])
        }
    }

    fun registerExternalArr(name: String, function: (Array<MValue>) -> MValue) {
        dynEnv.addJavaFunc(name, function)
    }




    fun run(code: String) {
        val lexer = Lexer(code)

        val strList = lexer.toStringList()


        val parser = ProgramParser()
        val tokens = lexer.read()
        println(tokens)
        val parseEnv = ParseEnv()
        parseEnv.init()
        var result = parser.parse(tokens, parseEnv)

        if(result is ParseResult.Failure) {
            val line = result.token.line
            println(strList[line - 1].trim())
            println(result)
            return
        }
        if(result !is ParseResult.Success) {
            println("Catastrophic failure: parse result isn't a success OR failure (This will never happen).")
            return
        }

        val program = ModuleStructNode("Program", result.result, Optional.empty(), parseEnv, 1)
        println(program.nodes)
        println("Parse successful. Type checking...")
        try {
            program.exportTypes(typeEnv)
        }
        catch(e: TypeCheckException) {
            println(strList[e.line - 1].trim())
            println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
            return
        }
        catch(e: MissingSigFieldException) {
            println(strList[e.line - 1].trim())
            println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
            return
        }
        //TODO: this is broken. Fix.
        typeSystem.normalizeTypeNames()

        println("Type checked. Evaluating...")
        try {
            program.exportValues(dynEnv)
        }
        catch(e: Exception) {
            println("Runtime Error: $e")
        }
    }
}

class BuiltinOperators {
    companion object {

        fun eq(env: DynEnv): MValue {
            return bop(env, { x, y -> BooleanValue(x == y)})
        }

        fun neq(env: DynEnv): MValue {
            return bop(env, { x, y -> BooleanValue(x != y)})
        }

        fun lt(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value < y.value)
            })
        }

        fun lte(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value <= y.value)
            })
        }

        fun gt(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value > y.value)
            })
        }

        fun gte(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                BooleanValue(x.value >= y.value)
            })
        }

        fun add(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value + y.value)
            })
        }

        fun sub(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value - y.value)
            })
        }

        fun mul(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value * y.value)
            })
        }

        fun div(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value / y.value)
            })
        }

        fun mod(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                if(y !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(x.value % y.value)
            })
        }

        fun negate(env: DynEnv): MValue {
            return uop(env, { x ->
                if(x !is IntegerValue) throw AssertionError("Should not happen")
                IntegerValue(-x.value)
            })
        }

        fun addf(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value + y.value)
            })
        }

        fun subf(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value - y.value)
            })
        }

        fun mulf(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value * y.value)
            })
        }

        fun divf(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value / y.value)
            })
        }

        fun modf(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                if(y !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(x.value % y.value)
            })
        }

        fun negatef(env: DynEnv): MValue {
            return uop(env, { x ->
                if(x !is FloatValue) throw AssertionError("Should not happen")
                FloatValue(-x.value)
            })
        }

        fun and(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                if(y !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(x.value && y.value)
            })
        }

        fun or(env: DynEnv): MValue {
            return bop(env, { x, y ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                if(y !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(x.value || y.value)
            })
        }

        fun not(env: DynEnv): MValue {
            return uop(env, { x ->
                if(x !is BooleanValue) throw AssertionError("Should not happen")
                BooleanValue(!x.value)
            })
        }

        private fun uop(env: DynEnv, func: (MValue) -> MValue): MValue {
            return FunctionValue("x", BuiltinOpNode({ e ->
                val x = e.lookupBinding("x")
                func(x)
            }, 1), env)
        }



        private fun bop(env: DynEnv, func: (MValue, MValue) -> MValue): MValue {
            return FunctionValue("x", FunctionNode(MBinding("y"), BuiltinOpNode({ e ->
                val x = e.lookupBinding("x")
                val y = e.lookupBinding("y")
                func(x, y)
            }, 1), 1), env)
        }
    }


    //TODO: this is very fragile and unsafe. Replace with much more stable version.
    class BuiltinOpNode(val evalFunc: (DynEnv) -> MValue, line: Int): AstNode(line) {
        override fun eval(env: DynEnv): MValue {
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