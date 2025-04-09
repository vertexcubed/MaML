package vertexcubed.maml.core

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.ModuleValue

/**
 * Represents a "long" identifier, to handle modules and such.
 */
data class MIdentifier(val path: List<String>) {
    constructor(path: String): this(path.split("."))

    init {
        if(path.isEmpty()) throw IllegalArgumentException("Cannot have empty path for identifier!")
    }

    fun lookupEvalBinding(env: Map<String, MValue>): MValue {
        var lastEnv = env
        for(i in path.indices) {
            val cur = lastEnv.getOrElse(path[i], { throw UnboundVarException(path[i]) })
            if(cur is ModuleValue) {
                lastEnv = cur.bindings
            }
            else {
                if(i != path.lastIndex) {
                    throw UnboundVarException(path[i])
                }
                return cur
            }
        }
        throw AssertionError("Should not happen!")
    }

    fun isWildCard(): Boolean {
        return path.size == 1 && path[0] == "_"
    }

    override fun toString(): String {
        return path.joinToString(".")
    }
}