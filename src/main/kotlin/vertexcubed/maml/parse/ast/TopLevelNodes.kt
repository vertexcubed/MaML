package vertexcubed.maml.parse.ast

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