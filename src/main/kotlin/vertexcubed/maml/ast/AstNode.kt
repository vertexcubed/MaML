package vertexcubed.maml.ast

import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

abstract class AstNode(val loc: NodeLoc) {

    abstract fun eval(env: DynEnv): MValue

    abstract fun inferType(env: TypeEnv): MType


    open fun pretty(): String {
        return toString()
    }
}

data class NodeLoc(val file: String, val line: Int) {
    override fun toString(): String {
        return "$file, line $line"
    }
}