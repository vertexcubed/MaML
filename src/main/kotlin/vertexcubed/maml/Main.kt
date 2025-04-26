package vertexcubed.maml

import vertexcubed.maml.bytecode.MVirtualMachine
import vertexcubed.maml.core.Interpreter
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.MaMLException
import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.eval.ConValue
import vertexcubed.maml.eval.IntegerValue
import vertexcubed.maml.eval.TupleValue
import vertexcubed.maml.eval.UnitValue
import vertexcubed.maml.type.MUnit
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.io.path.Path


fun main(args: Array<String>) {


    val vm = MVirtualMachine()

    vm.run()


//    if(true) return

    val interp = Interpreter()

    println("Loading Stdlib...")
    try {
        interp.loadStdLib()
    }
    catch(e: TypeCheckException) {
        println("Error in ${e.loc}\n${e.log}")
        return
    }
    catch(e: MaMLException) {
        println("Exception: ${e.exn}")
        return
    }
    println("Stdlib loaded.")

    for(arg in args) {
        interp.runFile(arg)
    }

    println("Starting REPL...")
    val repl = Scanner(System.`in`)
    repl.useDelimiter(";;")

    //TODO: update to be more flexible: account for wildcard lets, etc.
    println("Type #quit;; to quit")
    while(repl.hasNext()) {
        val text = repl.next()
        if(text == "#quit") {
            break
        }
        else {
            interp.run(text)
            println()
        }
    }
}