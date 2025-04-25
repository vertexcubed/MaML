package vertexcubed.maml.core

import vertexcubed.maml.ast.*
import vertexcubed.maml.eval.*
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.parsers.InterfaceParser
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
    private val fileModules = mutableListOf<ModuleWrapper>()
    private val interfaces = mutableListOf<InterfaceWrapper>()

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
        registerExternal("maml_core_div") { x, y ->
            val x = longOrThrow(x)
            val y = longOrThrow(y)
            if(y == 0L) {
                raise("Division_by_zero")
            }
            return@registerExternal IntegerValue(x / y)
        }
        registerExternal("maml_core_mod") { x, y ->
            val x = longOrThrow(x)
            val y = longOrThrow(y)
            if(y == 0L) {
                raise("Division_by_zero")
            }
            return@registerExternal IntegerValue(x % y)
        }

        registerExternal("maml_core_addf") { x, y -> FloatValue(floatOrThrow(x) + floatOrThrow(y))}
        registerExternal("maml_core_subf") { x, y -> FloatValue(floatOrThrow(x) - floatOrThrow(y))}
        registerExternal("maml_core_mulf") { x, y -> FloatValue(floatOrThrow(x) * floatOrThrow(y))}
        registerExternal("maml_core_divf") { x, y ->
            val x = floatOrThrow(x)
            val y = floatOrThrow(y)
            if(y == 0.0f) {
                raise("Division_by_zero")
            }
            return@registerExternal FloatValue(x / y)
        }
        registerExternal("maml_core_modf") { x, y ->
            val x = floatOrThrow(x)
            val y = floatOrThrow(y)
            if(y == 0.0f) {
                raise("Division_by_zero")
            }
            return@registerExternal FloatValue(x % y)
        }
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

        registerExternal("maml_core_raise") { x ->
            throw MaMLException(x)
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


    /**
     * Returns the value of some named value, or null if it was not found.
     * Register named values with Callback.register
     */
    fun namedValue(name: String): MValue? {
        return namedValues[name]
    }

    /**
     * Returns the result of some function call, or null if it failed.
     */
    fun callback(vfunc: MValue, vararg args: MValue): MValue? {
        if(args.isEmpty()) return null

        var last = toFunction(vfunc)?.invoke(args[0])
        for(i in 1 until args.size) {
            if(last == null) return null
            last = toFunction(last)?.invoke(args[i])
        }
        return last
    }

    /**
     * Returns the result of some named function call, or null if it failed or if the function was not found.
     * Register named functions with Callback.register
     */
    fun callback(name: String, vararg args: MValue): MValue? {
        val func = namedValue(name) ?: return null
        return callback(func, *args)
    }


    /**
     * Raises a MaML exception from java.
     * Throws IllegalArgumentException if name isn't a registered exception
     */
    fun raise(name: String, vararg args: MValue) {
        val iden = MIdentifier(name)
        var con: MType
        try {
            con = typeEnv.lookupConstructor(iden).instantiate(typeSystem)

        }
        catch(e: UnboundVarException) {
            try {
                con = typeEnv.lookupConstructor(MIdentifier("Core.$name")).instantiate(typeSystem)
            }
            catch(e: UnboundVarException) {
                throw IllegalArgumentException("MaML exception $name does not exist")
            }
        }

        if(con !is MConstr) throw IllegalArgumentException("Cannot raise non-exception $name")
        val conType = con.type
        if(conType !is MExtensibleVariantType) throw IllegalArgumentException("Cannot raise non-exception $name")
        val (exn, _) = typeEnv.lookupType(conType.id)
        if(exn != "Core.exn") throw IllegalArgumentException("Cannot raise non-exception $name")

        val tupleOpt: Optional<MValue> =
            if(args.isEmpty()) Optional.empty<MValue>()
            else Optional.of(TupleValue(args.toList()))
        val ret = ConValue(MIdentifier(name), tupleOpt)
        throw MaMLException(ret)
    }

    fun failWith(name: String) {
        return raise("Failure", StringValue(name))
    }

    fun invalidArg(name: String) {
        return raise("Invalid_argument", StringValue(name))
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


    fun run(code: String) {

        val classLoader = Thread.currentThread().contextClassLoader

        val parseEnv = ParseEnv()
        parseEnv.init()

        val coreFile = File(classLoader.getResource("core/core.ml")?.toURI() ?: throw FileNotFoundException("Could not find file core/core.ml"))

        val core = ModuleWrapper(coreFile.name, fileToString(coreFile), "Core", false, parseEnv, false)

        parseEnv.addAllFrom(core.node.parseEnv)

        val stdlibEnum = classLoader.getResources("stdlib")


        val moduleMap = mutableMapOf<String, Pair<Pair<String, String>?, Pair<String, String>?>>()

        if(stdlibEnum.hasMoreElements()) {
            val folderURL = stdlibEnum.nextElement()
            val files = File(folderURL.toURI()).listFiles()


            if(files != null) {


                for(f in files.filterNotNull()) {
                    val str = fileToString(f)

                    var moduleName = f.nameWithoutExtension
                    moduleName = moduleName.replaceFirstChar { it.uppercase() }
                    if(f.extension == "mli") {
                        if(moduleName in moduleMap) {
                            moduleMap[moduleName] = (f.name to str) to (moduleMap[moduleName]!!.second)
                        }
                        else {
                            moduleMap[moduleName] = (f.name to str) to (null)
                        }
                    }
                    else if(f.extension == "ml") {
                        if(moduleName in moduleMap) {
                            moduleMap[moduleName] = (moduleMap[moduleName]!!.first) to (f.name to str)
                        }
                        else {
                            moduleMap[moduleName] = (null) to (f.name to str)
                        }
                    }
                }

//                globalModules.add(ModuleWrapper(str, moduleName, parseEnv))
            }
        }
        for ((k, v) in moduleMap) {
            val (sig, struct) = v
            if(sig != null) {
                val (file, s) = sig
                interfaces.add(InterfaceWrapper(file, s, k, parseEnv))
            }
            if(struct != null) {
                val (file, s) = struct
                fileModules.add(ModuleWrapper(file, s, k, sig != null, parseEnv))
            }
        }



        val program = ModuleWrapper("toplevel", code, "Program", false, parseEnv)

        println(program.node)

        core.typeCheck(typeEnv)

        for(sig in interfaces) {
            try {
                sig.typeCheck(typeEnv)
            }
            catch(e: TypeCheckException) {
                println(sig.strList[e.loc.line - 1].trim())
                println("Error in ${e.loc}\n${e.log}")
                return
            }
        }

        for(mod in fileModules) {
            try {
                mod.typeCheck(typeEnv)
            }
            catch(e: TypeCheckException) {
                println(mod.strList[e.loc.line - 1].trim())
                println("Error in ${e.loc}\n${e.log}")
                return
            }
        }

        try {
            program.node.exportTypes(typeEnv, true)
        }
        catch(e: TypeCheckException) {
            println(program.strList[e.loc.line - 1].trim())
            println("Error in ${e.loc}\n${e.log}")
            return
        }
        //TODO: this is broken. Fix.
        typeSystem.normalizeTypeNames()

        println("Type checked. Evaluating...")

        core.evaluate(dynEnv)

        for(mod in fileModules) {
            try {
                mod.evaluate(dynEnv)
            }
            catch(e: MaMLException) {
                println("Exception: ${e.exn}")
                return
            }
        }

        try {
            program.node.exportValues(dynEnv, true)
        }
        catch(e: MaMLException) {
            println("Exception: ${e.exn}")
        }
        catch(e: MaMLException) {
            println("Exception: ${e.exn}")
        }
    }
}


class ModuleWrapper(val fileName: String, code: String, moduleName: String, val hasInterface: Boolean, val parseEnv: ParseEnv, val openCore: Boolean) {
    constructor(fileName: String, code: String, moduleName: String, hasInterface: Boolean, parseEnv: ParseEnv): this(fileName, code, moduleName, hasInterface, parseEnv, true)

    val strList: List<String>
    val node: ModuleStructNode

    init {
        strList = code.split('\n')
        node = parseFile(code, moduleName)
    }

    private fun parseFile(code: String, moduleName: String): ModuleStructNode {
        val lexer = Lexer(code, fileName)

        val parser = ProgramParser()
        val tokens = lexer.read()
        println(tokens)
        val newEnv = parseEnv.copy()
        newEnv.file = fileName
        val result = parser.parse(tokens, newEnv)

        if(result is ParseResult.Failure) {
            val line = result.token.line
            println(result)
            throw ParseException(strList[line - 1].trim(), NodeLoc(fileName, line), "Module $moduleName: ${result.logMessage}")
        }
        if(result !is ParseResult.Success) {
            println("Catastrophic failure: parse result isn't a success OR failure (This will never happen).")
            throw AssertionError()
        }
        var list = result.result.first
        if(openCore) {
           list = listOf(TopOpenNode(MIdentifier("Core"), NodeLoc(fileName, 1))) + list
        }
        val sig = if(hasInterface) Optional.of(MIdentifier(moduleName)) else Optional.empty()
        return ModuleStructNode(moduleName, list, sig, result.result.second, NodeLoc(fileName, 1))
    }

    fun typeCheck(env: TypeEnv) {
        val module = node.exportTypes(env, false)
        env.addModule(module)
    }

    fun evaluate(env: DynEnv) {
        val module = node.exportValues(env, false)
        env.addModule(module)
    }
}

class InterfaceWrapper(val fileName: String, code: String, interfaceName: String, val parseEnv: ParseEnv, val openCore: Boolean) {
    constructor(fileName: String, code: String, interfaceName: String, parseEnv: ParseEnv): this(fileName, code, interfaceName, parseEnv, true)

    val strList: List<String>
    val node: ModuleSigNode

    init {
        strList = code.split('\n')
        node = parseFile(code, interfaceName)
    }

    private fun parseFile(code: String, moduleName: String): ModuleSigNode {
        val lexer = Lexer(code, fileName)
        val newEnv = parseEnv.copy()
        newEnv.file = fileName

        val parser = InterfaceParser()
        val tokens = lexer.read()

        val result = parser.parse(tokens, newEnv)

        if(result is ParseResult.Failure) {
            val line = result.token.line
            throw ParseException(strList[line - 1].trim(), NodeLoc(fileName, line), result.logMessage)
        }
        if(result !is ParseResult.Success) {
            println("Catastrophic failure: parse result isn't a success OR failure (This will never happen).")
            throw AssertionError()
        }
        var list = result.result.first
        if(openCore) {
            list = listOf(OpenSigNode(MIdentifier("Core"), NodeLoc(fileName, 1))) + list
        }
        return ModuleSigNode(moduleName, list, result.result.second, NodeLoc(fileName, 1))
    }

    fun typeCheck(env: TypeEnv) {
        val sig = node.exportTypes(env)
        env.addSignature(sig)
    }
}

