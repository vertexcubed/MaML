package vertexcubed.maml.ast

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.ForAll
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeVarEnv

abstract class AstNode(val line: Int) {

    abstract fun eval(env: Map<String, MValue>): MValue

    abstract fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType


    abstract fun pretty(): String
}

