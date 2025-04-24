package vertexcubed.maml.parse

import vertexcubed.maml.core.*
import vertexcubed.maml.type.*

/**
 * Represents a type that may or may not exist. Used at parse time only.
 */
sealed class DummyType {
    abstract fun lookup(env: TypeEnv): MType

    abstract fun getAllLabels(): List<String>

}

data class SingleDummy(val name: MIdentifier): DummyType() {
    constructor(name: String): this(MIdentifier(name))

    override fun lookup(env: TypeEnv): MType {


        val type = env.lookupType(name).instantiate(env.typeSystem).find()


        when(type) {
            is MVariantType -> {
                var expected = 0
                for(arg in type.args) {
                    val t = arg.second.find()
                    if(t is MTypeVar) {
                        expected++
                    }
                }
                if(expected > 0) {
                    throw TypeConException(env, type, expected, 0)
                }
                return type
            }
            else -> {
                return type
            }
        }
    }

    override fun getAllLabels(): List<String> {
        return emptyList()
    }

    override fun toString(): String {
        return name.toString()
    }
}

data class TypeVarDummy(val name: String): DummyType() {
    override fun lookup(env: TypeEnv): MType {
        return env.lookupVarLabel(name, {
            throw UnboundTypeLabelException(this)
        })
    }

    override fun getAllLabels(): List<String> {
        return listOf(name)
    }


    override fun toString(): String {
        return "'$name"
    }

}

data class TypeConDummy(val name: MIdentifier, val args: List<DummyType>): DummyType() {
    constructor(name: String, args: List<DummyType>): this(MIdentifier(name), args)

    override fun lookup(env: TypeEnv): MType {
        val type = env.lookupType(name).instantiate(env.typeSystem)
        if(type is MTypeAlias) {
            if(unboundArgs(type) != args.size) throw UnboundTyConException(this.toString())
            for(i in args.indices) {
                val argType = args[i].lookup(env)
                type.args[i].second.unify(argType, env.typeSystem)
            }
            return type
        }


        if(type !is MVariantType) throw TypeConException(env, type, 0, args.size)
        if(unboundArgs(type) != args.size) throw UnboundTyConException(this.toString())
        for(i in args.indices) {
            val argType = args[i].lookup(env)
            type.args[i].second.unify(argType, env.typeSystem)
        }
        return type
    }

    override fun getAllLabels(): List<String> {
        return args.flatMap { it.getAllLabels() }
    }

    private fun unboundArgs(type: MVariantType): Int {
        var i = 0
        for(arg in type.args) {
            if(arg.second is MTypeVar) {
                i++
            }
        }
        return i
    }

    private fun unboundArgs(type: MTypeAlias): Int {
        var i = 0
        for(arg in type.args) {
            if(arg.second is MTypeVar) {
                i++
            }
        }
        return i
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

    override fun getAllLabels(): List<String> {
        return first.getAllLabels() + second.getAllLabels()
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

    override fun getAllLabels(): List<String> {
        return types.flatMap { it.getAllLabels() }
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

data class StaticRecordDummy(val types: List<Pair<String, DummyType>>): DummyType() {
    override fun lookup(env: TypeEnv): MType {
        val map = mutableMapOf<String, MType>()
        for((k, v) in types) {
            if(k in map) throw BadRecordException(k)
            map[k] = v.lookup(env)
        }
        return MRecord(map, MEmptyRow)
    }

    override fun getAllLabels(): List<String> {
        return types.flatMap { (_, v) -> v.getAllLabels() }
    }

    override fun toString(): String {
        return types.joinToString("; ", "{ ", " }") { (k, v) -> "$k: $v" }
    }
}