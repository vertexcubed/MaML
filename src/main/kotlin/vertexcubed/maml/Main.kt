package vertexcubed.maml

import vertexcubed.maml.core.Interpreter
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.eval.ConValue
import vertexcubed.maml.eval.IntegerValue
import vertexcubed.maml.eval.TupleValue
import vertexcubed.maml.eval.UnitValue
import vertexcubed.maml.type.MUnit
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*
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
    val interp = Interpreter()

    interp.registerExternal("maml_core_print") { arg ->
        print(arg)
        UnitValue
    }

    interp.registerExternal("maml_core_println") { arg ->
        println(arg)
        UnitValue
    }

    interp.run(code)

}