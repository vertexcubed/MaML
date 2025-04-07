package vertexcubed.maml.ast

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

abstract class AstNode(val line: Int) {

    abstract fun eval(env: Map<String, MValue>): MValue

    abstract fun inferType(env: TypeEnv): MType


    open fun pretty(): String {
        return toString()
    }
}

