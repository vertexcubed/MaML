package vertexcubed.maml

import vertexcubed.maml.core.Interpreter
import vertexcubed.maml.eval.TupleValue
import vertexcubed.maml.eval.UnitValue
import vertexcubed.maml.parse.Lexer
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.parse.parsers.PrecedenceParsers
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
    val interp = Interpreter()

    interp.registerExternal("maml_core_print", { arg ->
        print(arg)
        UnitValue
    })

    interp.registerExternal("maml_core_println", { arg ->
        println(arg)
        UnitValue
    })

    interp.registerExternal("maml_8queens_fst") {arg ->
        (arg as TupleValue).values[0]
    }

    interp.registerExternal("maml_8queens_snd") {arg ->
        (arg as TupleValue).values[1]
    }

    interp.run(code)

//    val names1 = listOf("*", "/")
//    val names2 = listOf("+", "-")
//
//    val name1Parser = ChoiceParser(names1.map { name ->
//        CompoundSpecialCharParser(name).disjoint(SpecificIdentifierParser(name))
//    })
//    val name2Parser = ChoiceParser(names2.map { name ->
//        CompoundSpecialCharParser(name).disjoint(SpecificIdentifierParser(name))
//    })
//    val env = ParseEnv()
//    val parserList = arrayListOf<Parser<AstNode>>(PrecedenceParsers.UnaryLevel())
//    parserList.add(parserList[0].bind { first ->
//        ZeroOrMore(AndParser(name1Parser, parserList[0])).map { secondList ->
//            if(secondList.isEmpty()) return@map first
//            env.leftAssocParser(first, secondList)
//        }
//    })
//
//    parserList.add(parserList[1].bind { first ->
//        ZeroOrMore(AndParser(name2Parser, parserList[1])).map { secondList ->
//            if(secondList.isEmpty()) return@map first
//            env.leftAssocParser(first, secondList)
//        }
//    })
//
//    val str = "1 + 2 * 3 + 4"
//    val res = parserList[parserList.lastIndex].parse(Lexer(str).read(), env)
//    println(res)
//    env.init()
//    val otherRes = env.infixParser(PrecedenceParsers.UnaryLevel()).parse(Lexer(str).read(), env)
//    println(otherRes)

}