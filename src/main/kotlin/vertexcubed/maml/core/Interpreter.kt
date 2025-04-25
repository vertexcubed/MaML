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
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.math.*

class Interpreter {

    private val namedValues: MutableMap<String, MValue>
    private var dynEnv: DynEnv
    private var typeEnv: TypeEnv
    private val typeSystem: TypeSystem = TypeSystem()
    private val globalModules = mutableListOf<ModuleWrapper>()

    //TODO: IMPLEMENT AND/OR SHORT CIRCUITING
    init {
        namedValues = mutableMapOf()

        typeEnv = TypeEnv(typeSystem)
        typeEnv.addAllBindings(mapOf(

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




        registerExternal("maml_register_named_value") { vname, value ->
            val name = stringOf(vname)!!
            namedValues[name] = value
            UnitValue
        }

        registerExternal("maml_core_add") { x, y -> IntegerValue(longOrThrow(x) + longOrThrow(y))}
        registerExternal("maml_core_sub") { x, y -> IntegerValue(longOrThrow(x) - longOrThrow(y))}
        registerExternal("maml_core_mul") { x, y -> IntegerValue(longOrThrow(x) * longOrThrow(y))}
        registerExternal("maml_core_div") { x, y -> IntegerValue(longOrThrow(x) / longOrThrow(y))}
        registerExternal("maml_core_mod") { x, y -> IntegerValue(longOrThrow(x) % longOrThrow(y))}
        registerExternal("maml_core_addf") { x, y -> FloatValue(floatOrThrow(x) + floatOrThrow(y))}
        registerExternal("maml_core_subf") { x, y -> FloatValue(floatOrThrow(x) - floatOrThrow(y))}
        registerExternal("maml_core_mulf") { x, y -> FloatValue(floatOrThrow(x) * floatOrThrow(y))}
        registerExternal("maml_core_divf") { x, y -> FloatValue(floatOrThrow(x) / floatOrThrow(y))}
        registerExternal("maml_core_modf") { x, y -> FloatValue(floatOrThrow(x) % floatOrThrow(y))}
        registerExternal("maml_core_eq") { x, y -> BooleanValue(x == y)}
        registerExternal("maml_core_neq") { x, y -> BooleanValue(x != y)}
        registerExternal("maml_core_lt") { x, y -> BooleanValue(x < y)}
        registerExternal("maml_core_lte") { x, y -> BooleanValue(x <= y)}
        registerExternal("maml_core_gt") { x, y -> BooleanValue(x > y)}
        registerExternal("maml_core_gte") { x, y -> BooleanValue(x >= y)}
        registerExternal("maml_core_not") { x -> BooleanValue(!boolOrThrow(x))}
        registerExternal("maml_core_and") { x, y -> BooleanValue(boolOrThrow(x) && boolOrThrow(y))}
        registerExternal("maml_core_or") { x, y -> BooleanValue(boolOrThrow(x) || boolOrThrow(y))}
        registerExternal("maml_core_negate") { x -> IntegerValue(-longOrThrow(x))}
        registerExternal("maml_core_negatef") { x -> FloatValue(-floatOrThrow(x))}

        registerExternal("maml_list_cons") { x, xs ->
            ConValue(MIdentifier("::"), Optional.of(TupleValue(listOf(x, xs))))
        }

        registerExternal("maml_core_int_of_float") { x -> IntegerValue(floatOrThrow(x).toLong())}
        registerExternal("maml_core_float_of_int") { x -> FloatValue(longOrThrow(x).toFloat())}
        registerExternal("maml_core_string_of_int") { x -> StringValue(longOrThrow(x).toString())}
        registerExternal("maml_core_string_of_float") { x -> StringValue(floatOrThrow(x).toString())}

//        I could not be bothered to actually implement these so we're using externals
        registerExternal("maml_core_sqrt") { x -> FloatValue(sqrt(floatOrThrow(x)))}
        registerExternal("maml_core_pow") { x, y -> FloatValue((floatOrThrow(x).pow(floatOrThrow(y)))) }
        registerExternal("maml_core_log") { x -> FloatValue(ln(floatOrThrow(x))) }
        registerExternal("maml_core_log10") { x -> FloatValue(log10(floatOrThrow(x))) }

        registerExternal("maml_core_sin") { x -> FloatValue(sin(floatOrThrow(x))) }
        registerExternal("maml_core_cos") { x -> FloatValue(cos(floatOrThrow(x))) }
        registerExternal("maml_core_tan") { x -> FloatValue(tan(floatOrThrow(x))) }
        registerExternal("maml_core_asin") { x -> FloatValue(asin(floatOrThrow(x))) }
        registerExternal("maml_core_acos") { x -> FloatValue(acos(floatOrThrow(x))) }
        registerExternal("maml_core_atan") { x -> FloatValue(atan(floatOrThrow(x))) }
        registerExternal("maml_core_atan2") { y, x -> FloatValue(atan2(floatOrThrow(y), floatOrThrow(x))) }


        registerExternal("maml_core_floor") { x -> FloatValue(floor(floatOrThrow(x))) }
        registerExternal("maml_core_ceil") { x -> FloatValue(ceil(floatOrThrow(x))) }
        registerExternal("maml_core_round") { x -> FloatValue(round(floatOrThrow(x))) }
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


    private fun fileToString(file: File): String {
        val lines = StringBuilder()
        val reader = Scanner(file)
        try {
            while(reader.hasNextLine()) {
                lines.append(reader.nextLine())
                lines.append('\n')
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            reader.close()
        }
        return lines.toString()
    }

    private fun parseFile(code: String, moduleName: String): ModuleStructNode? {
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
            println(result)
            return null
        }
        if(result !is ParseResult.Success) {
            println("Catastrophic failure: parse result isn't a success OR failure (This will never happen).")
            return null
        }
        return ModuleStructNode(moduleName, result.result.first, Optional.empty(), result.result.second, 1)
    }


    fun run(code: String) {

        val classLoader = Thread.currentThread().contextClassLoader

        val parseEnv = ParseEnv()
        parseEnv.init()

        val core = File(classLoader.getResource("core/core.ml")?.toURI() ?: throw FileNotFoundException("Could not find file core/core.ml"))

        globalModules.add(ModuleWrapper(fileToString(core), "Core", parseEnv))

        val stdlibEnum = classLoader.getResources("stdlib")

        if(stdlibEnum.hasMoreElements()) {
            val folderURL = stdlibEnum.nextElement()
            val files = File(folderURL.toURI()).listFiles()

            if(files != null) {
                for(f in files.filterNotNull()) {
                    val str = fileToString(f)
                    var moduleName = f.name.replaceFirstChar { it.uppercase() }
                    moduleName = moduleName.substring(0, moduleName.indexOf(".ml"))
                    println(moduleName)
                    globalModules.add(ModuleWrapper(str, moduleName, parseEnv))
                }
            }
        }

        val program = ModuleWrapper(code, "Program", parseEnv)

        println(program.node)

        for(mod in globalModules) {
            try {
                mod.typeCheck(typeEnv)
            }
            catch(e: TypeCheckException) {
                println(mod.strList[e.line - 1].trim())
                println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
                return
            }
        }

        try {
            program.node.exportTypes(typeEnv, true)
        }
        catch(e: TypeCheckException) {
            println(program.strList[e.line - 1].trim())
            println("Error on line ${e.line} (${e.node.pretty()})\n${e.log}")
            return
        }
        //TODO: this is broken. Fix.
        typeSystem.normalizeTypeNames()

        println("Type checked. Evaluating...")

        for(mod in globalModules) {
            try {
                mod.evaluate(dynEnv)
            }
            catch(e: Exception) {
                println("Runtime Error: $e")
                return
            }
        }

        try {
            program.node.exportValues(dynEnv)
        }
        catch(e: Exception) {
            println("Runtime Error: $e")
        }
    }
}


class ModuleWrapper(code: String, moduleName: String, val parseEnv: ParseEnv) {


    val strList: List<String>
    val node: ModuleStructNode

    init {
        strList = code.split('\n')
        node = parseFile(code, moduleName)
    }

    private fun parseFile(code: String, moduleName: String): ModuleStructNode {
        val lexer = Lexer(code)

        val parser = ProgramParser()
        val tokens = lexer.read()
        println(tokens)
        val result = parser.parse(tokens, parseEnv)

        if(result is ParseResult.Failure) {
            val line = result.token.line
            throw ParseException(strList[line - 1].trim(), line, result.logMessage)
        }
        if(result !is ParseResult.Success) {
            println("Catastrophic failure: parse result isn't a success OR failure (This will never happen).")
            throw AssertionError()
        }
        return ModuleStructNode(moduleName, result.result.first, Optional.empty(), result.result.second, 1)
    }

    fun typeCheck(env: TypeEnv) {
        val module = node.exportTypes(env, false)
        env.addModule(module)
    }

    fun evaluate(env: DynEnv) {
        val module = node.exportValues(env)
        env.addModule(module)
    }
}


