package vertexcubed.maml.parse

import vertexcubed.maml.type.MFunction
import vertexcubed.maml.type.MTuple
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

/**
 * Represents a type that may or may not exist. Used at parse time only.
 */
sealed class DummyType {
    abstract fun lookup(env: TypeEnv): MType
}

data class SingleDummy(val name: String): DummyType() {
    override fun lookup(env: TypeEnv): MType {
        return env.lookupType(name).instantiate(env.typeSystem)
    }

    override fun toString(): String {
        return name
    }
}

data class TypeVarDummy(val name: String): DummyType() {
    override fun lookup(env: TypeEnv): MType {
        return env.typeSystem.newTypeVar()
    }

    override fun toString(): String {
        return "'$name"
    }

}

data class TypeConDummy(val name: String, val args: List<DummyType>): DummyType() {
    override fun lookup(env: TypeEnv): MType {
        return env.lookupType(name).instantiate(env.typeSystem)
    }

    override fun toString(): String {
        var str = ""
        for(arg in args) {
            str += "$arg "
        }
        return str + name
    }
}

data class FunctionDummy(val first: DummyType, val second: DummyType) : DummyType() {
    override fun lookup(env: TypeEnv): MType {
        return MFunction(first.lookup(env), second.lookup(env))
    }

    override fun toString(): String {
        var firStr = first.toString()
        if(first is FunctionDummy) {
            firStr = "($firStr)"
        }
        return "$firStr -> $second"
    }
}

data class TupleDummy(val types: List<DummyType>): DummyType() {
    override fun lookup(env: TypeEnv): MType {
        return MTuple(types.map { t -> t.lookup(env) })
    }

    override fun toString(): String {
        var str = ""
        for(i in types.indices) {
            if(types[i] is TupleDummy) {
                str += "(${types[i]})"
            }
            else {
                str += types[i]
            }
            if(i != types.lastIndex) {
                str += " * "
            }
        }
        return str
    }
}
