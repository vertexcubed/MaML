package vertexcubed.maml.type

import vertexcubed.maml.core.UnifyException
import java.util.*

sealed class MType() {

    /**
     * Check if some other type occurs in this type.
     */
    open fun occurs(other: MType): Boolean {
        return false
    }
    open fun find(): MType {
        return this
    }

    //Override for non primitives
    open fun unify(other: MType) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType)
            return
        }
        if(this != otherType) {
            throw UnifyException(myType, other)
        }
    }

    abstract fun substitute(from: MType, to: MType): MType

    open fun asString(env: TypeEnv): String {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val t = v.type.find()
            if(t is MTypeAlias && t.real == this.find()) return k
            if(t == this.find()) return k
        }
        return this.toString()
    }
}

open class MBasic: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return this
    }
}

data object MInt: MBasic() {
    override fun substitute(from: MType, to: MType): MType {
        return MInt
    }
}
data object MFloat: MBasic() {
    override fun substitute(from: MType, to: MType): MType {
        return MFloat
    }
}
data object MBool: MBasic() {
    override fun substitute(from: MType, to: MType): MType {
        return MBool
    }
}
data object MString: MBasic() {
    override fun substitute(from: MType, to: MType): MType {
        return MString
    }
}
data object MChar: MBasic() {
    override fun substitute(from: MType, to: MType): MType {
        return MChar
    }
}
data object MUnit: MBasic() {
    override fun substitute(from: MType, to: MType): MType {
        return MUnit
    }
}

data class MFunction(val arg: MType, val ret: MType): MType() {
    override fun toString(): String {
        val argStr: String
        if(arg.find() is MFunction) {
            argStr = "($arg)"
        }
        else argStr = arg.toString()

        return "$argStr -> $ret"
    }

    override fun occurs(other: MType): Boolean {
        return arg.occurs(other) || ret.occurs(other)
    }

    override fun unify(other: MType) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType)
            return
        }
        if(otherType !is MFunction) throw UnifyException(myType, otherType)
        arg.unify(otherType.arg)
        ret.unify(otherType.ret)
    }

    override fun substitute(from: MType, to: MType): MType {
        return MFunction(arg.substitute(from, to), ret.substitute(from, to))
    }

    override fun asString(env: TypeEnv): String {
        return "${arg.asString(env)} -> ${ret.asString(env)}"
    }

}

data class MTuple(val types: List<MType>): MType() {
    init {
        if(types.isEmpty()) throw IllegalArgumentException("Cannot create tuple of empty type")
    }
    override fun toString(): String {
        var str = types[0].toString()
        for(i in 1 until types.size) {
            str += " * " + types[i].toString()
        }
        return str
    }

    override fun occurs(other: MType): Boolean {
        for(type in types) {
            if (type.occurs(other)) return true
        }
        return false
    }

    override fun unify(other: MType) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType)
            return
        }
        if(otherType !is MTuple) throw UnifyException(myType, otherType)
        if(otherType.types.size != types.size) throw UnifyException(myType, otherType)
        for(i in types.indices) {
            types[i].unify(otherType.types[i])
        }
    }

    override fun substitute(from: MType, to: MType): MType {
        return MTuple(types.map { it.substitute(from, to) })
    }

    override fun asString(env: TypeEnv): String {
        return types.map { t -> t.asString(env) }.joinToString(" * ")
    }
}




data class MVariantType(val id: UUID, val args: List<Pair<String, MType>>): MType() {

    override fun occurs(other: MType): Boolean {
        for(arg in args) {
            if(arg.second.occurs(other)) return true
        }
        return false
    }


    override fun unify(other: MType) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            return otherType.unify(this)
        }
        if(otherType !is MVariantType) throw UnifyException(otherType, this)
        if(otherType.args.size != args.size) throw UnifyException(otherType, this)
        for(i in args.indices) {
            args[i].second.unify(otherType.args[i].second)
        }
    }

    override fun substitute(from: MType, to: MType): MType {
        return MVariantType(id, args.map { a -> Pair(a.first, a.second.substitute(from, to)) })
    }

    override fun asString(env: TypeEnv): String {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.type.find()
            if(otherType is MVariantType && otherType.id == this.id) {
                var str = ""
                if(args.isNotEmpty()) {
                    str += args.map { p -> p.second.asString(env) }.joinToString(" , ", "(" , ") ")
                }
                return str + k
            }
        }
        return this.toString()
    }


}

data class MTypeAlias(val id: UUID, val args: List<Pair<String, MType>>, val real: MType): MType() {
    override fun occurs(other: MType): Boolean {
        return real.occurs(other)
    }

    override fun substitute(from: MType, to: MType): MType {
        return MTypeAlias(id, args.map { p -> Pair(p.first, p.second.substitute(from, to)) }, real.substitute(from, to))
    }

//    override fun find(): MType {
//        return real.find()
//    }

    override fun unify(other: MType) {
        val otherType = other.find()
        when(otherType) {
            is MTypeVar -> {
                return otherType.unify(this)
            }
            is MTypeAlias -> {
                return real.unify(otherType.real)
            }
            else -> {
                return real.unify(other)
            }
        }
    }

    override fun asString(env: TypeEnv): String {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.type.find()
            if(otherType is MTypeAlias && otherType.id == this.id) {
                var str = ""
                if(args.isNotEmpty()) {
                    str += args.map { p -> p.second.asString(env) }.joinToString(" , ", "(" , ") ")
                }
                return str + k
            }
        }
        return this.toString()
    }

}







/**
 * Not *type constructors*, but rather a wrapped type for constructors themselves
 */
data class MConstr(val name: String, val type: MType, val argType: Optional<MType>): MType() {
    override fun substitute(from: MType, to: MType): MType {
        if(argType.isEmpty) return MConstr(name, type.substitute(from, to), Optional.empty())
        return MConstr(name, type.substitute(from, to), Optional.of(argType.get().substitute(from, to)))
    }

    override fun unify(other: MType) {
        type.unify(other)
    }

    override fun occurs(other: MType): Boolean {
        return type.occurs(other)
    }

    override fun find(): MType {
        return type.find()
    }
}

/**
 * Not reeally a type? Hence why you cannot unify or substitute or do occurs checks
 */
data class ModuleType(val name: String, val types: TypeEnv): MType() {
    override fun substitute(from: MType, to: MType): MType {
        return this
    }

    override fun occurs(other: MType): Boolean {
        return false
    }

    override fun unify(other: MType) {
        throw UnifyException(this, other)
    }
}