package vertexcubed.maml

import vertexcubed.maml.core.Interpreter
import vertexcubed.maml.eval.TupleValue
import vertexcubed.maml.eval.UnitValue
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

    interp.registerBuiltin("print", { arg ->
        print(arg)
        UnitValue
    })

    interp.registerBuiltin("println", { arg ->
        println(arg)
        UnitValue
    })

    interp.registerBuiltin("fst", { arg ->
        (arg as TupleValue).values[0]
    })

    interp.registerBuiltin("snd", { arg ->
        (arg as TupleValue).values[1]
    })

    interp.run(code)
}