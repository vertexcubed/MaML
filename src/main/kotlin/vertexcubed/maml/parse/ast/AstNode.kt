package vertexcubed.maml.parse.ast

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.MType

abstract class AstNode(val line: Int) {

    abstract fun eval(env: Map<String, MValue>): MValue

    abstract fun type(env: Map<String, MType>): MType

    abstract fun pretty(): String
}

