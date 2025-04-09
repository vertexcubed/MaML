package vertexcubed.maml.ast

import vertexcubed.maml.core.MBinding
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.ModuleValue
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.*


class TopLetNode(val name: MBinding, val statement: AstNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return statement.eval(env)
    }

    override fun inferType(env: TypeEnv): MType {
        val statementType =  statement.inferType(env)
        if(name.type.isPresent) {
            val nameType = name.type.get().lookup(env)
            var lastType = statementType
            while(true) {
                if(lastType is MFunction) {
                    lastType = lastType.ret
                }
                else {
                    break
                }
            }
            nameType.unify(lastType)
        }



        return statementType
    }

    override fun pretty(): String {
        return "let $name = $statement"
    }

    override fun toString(): String {
        return "TopLet($name, $statement)"
    }

}

/**
 * Represents a variant type: eg. type a = B | C | D
 */
class VariantTypeNode(val name: String, val arguments: List<TypeVarDummy>, val cons: List<ConDefNode>, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MVariantType {
        val myType = MVariantType(name, arguments.map { a -> Pair(a.name, a.lookup(env)) })
        val newEnv = env.copy()
        newEnv.addType(name to ForAll.generalize(myType, env.typeSystem))
        for(con in cons) {
            //purposely discard return type?
            con.inferType(newEnv)
        }
        return myType
    }

    override fun pretty(): String {
        var str = ""
        for(i in cons.indices) {
            str += cons[i].toString() + " "
            if(i != cons.size - 1) {
                str += "| "
            }
        }
        return "type $name = "
    }

    override fun toString(): String {
        return "Type($name, $arguments, $cons)"
    }
}




/**
 * Represents an extensible variant type, such as exn
 */
class ExtensibleVariantTypeNode(val name: String, val arguments: List<TypeVarDummy>, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated")
    }

    override fun inferType(env: TypeEnv): MType {
        val myType = MVariantType(name, arguments.map { a -> Pair(a.name, a.lookup(env)) })
        return myType
    }
}



class TypeAliasNode(val name: String, val type: MType, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        TODO("NYI")
    }

    override fun inferType(env: TypeEnv): MType {
        TODO("NYI")
    }

    override fun pretty(): String {
        return "type $name = $type"
    }

}

/**
 * module m = struct ... end
 */
class ModuleStructNode(val name: String, val nodes: List<AstNode>, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): ModuleValue {
        //TODO: MAKE SURE TO ALWAYS UPDATE THIS!!!! IT SHOULD MATCH PROGRAM LOOP
        val newBindings = mutableMapOf<String, MValue>()
        val newEnv = env.toMutableMap()
        for(node in nodes) {
            when(node) {
                is TopLetNode -> {
                    val nodeVal = node.eval(newEnv)
                    println(nodeVal)
                    if(node.name.binding != "_") {
                        newEnv += (node.name.binding to nodeVal)
                        newBindings += (node.name.binding to nodeVal)
                    }
                }
                is VariantTypeNode -> {
                    //Don't try to "evaluate" datatype defs i guess?
                    continue
                }
                else -> {
                    println(node.eval(newEnv))
                }
            }
        }
        newBindings.keys.map { k -> "$name.$k" }
        return ModuleValue(name, newBindings, newEnv)
    }

    override fun inferType(env: TypeEnv): MType {
        TODO("Module infer not yet implemented")
    }

}