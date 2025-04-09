package vertexcubed.maml.parse

import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.UnboundTyConException
import vertexcubed.maml.type.*

/**
 * Represents a type that may or may not exist. Used at parse time only.
 */
sealed class DummyType {
    abstract fun lookup(env: TypeEnv): MType
}

data class SingleDummy(val name: MIdentifier): DummyType() {
    constructor(name: String): this(MIdentifier(name))

    override fun lookup(env: TypeEnv): MType {
        return env.lookupType(name).instantiate(env.typeSystem)
    }

    override fun toString(): String {
        return name.toString()
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

data class TypeConDummy(val name: MIdentifier, val args: List<DummyType>): DummyType() {
    constructor(name: String, args: List<DummyType>): this(MIdentifier(name), args)

    override fun lookup(env: TypeEnv): MType {
        val type = env.lookupType(name).instantiate(env.typeSystem)
        if(type !is MVariantType) throw AssertionError("what.")
        if(type.args.size != args.size) throw UnboundTyConException(this.toString())
        for(i in args.indices) {
            val argType = args[i].lookup(env)
            type.args[i].second.unify(argType)
        }
        return type
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
