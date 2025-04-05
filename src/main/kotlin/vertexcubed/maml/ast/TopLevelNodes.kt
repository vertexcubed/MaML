package vertexcubed.maml.ast

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.ForAll
import vertexcubed.maml.type.MBinding
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeVarEnv


class Program(val nodes: List<AstNode>) {

    private val evalMap = mutableMapOf<String, MValue>()
    private val typeMap = mutableMapOf<String, ForAll>()


    fun init(evalMap: Map<String, MValue>, typeMap: Map<String, ForAll>) {
        this.evalMap += evalMap
        this.typeMap += typeMap
    }


    fun eval() {
        for(node in nodes) {
            val nodeVal = node.eval(evalMap)
            when(node) {
                is TopLetNode -> {
                    if(node.name.binding != "_") {
                        evalMap += (node.name.binding to nodeVal)
                    }
                }
            }
        }
    }

    fun inferTypes(types: TypeVarEnv) {
        for(node in nodes) {
            val nodeType = node.inferType(typeMap, types)
            if(node is TopLetNode) {
                val scheme = ForAll.generalize(nodeType, types)
                if(node.name.binding != "_") {
                    typeMap += (node.name.binding to scheme)
                }
            }
        }
    }

}




class TopLetNode(val name: MBinding, val statement: AstNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return statement.eval(env)
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        return statement.inferType(env, types)
    }

    override fun pretty(): String {
        return "let $name = $statement"
    }

    override fun toString(): String {
        return "TopLet($name, $statement)"
    }

}

/**
 * Represents an ADT
 */
class DataTypeNode(val name: String, val types: List<ConDefNode>, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        TODO("Not yet implemented")
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        var str = ""
        for(i in types.indices) {
            str += types[i].toString() + " "
            if(i != types.size - 1) {
                str += "| "
            }
        }
        return "type $name = "
    }

    override fun toString(): String {
        return "Type($name, $types)"
    }
}



class TypeAliasNode(val name: String, val type: MType, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        TODO("Not yet implemented")
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        return type
    }

    override fun pretty(): String {
        return "type $name = $type"
    }

}