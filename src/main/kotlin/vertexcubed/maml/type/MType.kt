package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import vertexcubed.maml.core.UnifyException
import java.util.*
import kotlin.jvm.optionals.getOrElse
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

    fun unify(other: MType, typeSystem: TypeSystem) {
        return unify(other, typeSystem, false)
    }

    /**
     *
     */
    open fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType, typeSystem, looser)
            return
        }
        if(this != otherType) {
            throw UnifyException(myType, other)
        }
    }

    abstract fun substitute(from: MType, to: MType): MType

    open fun asString(env: TypeEnv): String {
        return stringOpt(env).getOrElse { toString() }
    }

    fun stringOpt(env: TypeEnv): Optional<String> {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val t = v.type.find()
//            if(t is ModuleType) {
//                val modString = stringOpt(t.types)
//                if(modString.isPresent) return Optional.of("${t.name}.${modString.get()}")
//            }

            if(t is MTypeAlias && t.real.isSame(this.find())) return Optional.of(k)
            if(t.isSame(this.find())) {
                return Optional.of(k)
            }
        }
        return Optional.empty()
    }

    open fun isSame(other: MType): Boolean {
        return this.find() == other.find()
    }
}

data object MInt: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MInt
    }
}
data object MFloat: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MFloat
    }
}
data object MBool: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MBool
    }
}
data object MString: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MString
    }
}
data object MChar: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MChar
    }
}
data object MUnit: MType() {
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

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            if(looser) throw BindException(myType, otherType)

            otherType.unify(myType, typeSystem, looser)
            return
        }
        if(otherType !is MFunction) throw UnifyException(myType, otherType)
        arg.unify(otherType.arg, typeSystem, looser)
        ret.unify(otherType.ret, typeSystem, looser)
    }

    override fun substitute(from: MType, to: MType): MType {
        return MFunction(arg.substitute(from, to), ret.substitute(from, to))
    }

    override fun asString(env: TypeEnv): String {
        val opt = stringOpt(env)
        if(opt.isPresent) {
            return opt.get()
        }
        return "${arg.asString(env)} -> ${ret.asString(env)}"
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MFunction) return false
        return arg.isSame(otherType.arg) && ret.isSame(otherType.ret)
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

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            if(looser) throw BindException(myType, otherType)

            otherType.unify(myType, typeSystem, looser)
            return
        }
        if(otherType !is MTuple) throw UnifyException(myType, otherType)
        if(otherType.types.size != types.size) throw UnifyException(myType, otherType)
        for(i in types.indices) {
            types[i].unify(otherType.types[i], typeSystem, looser)
        }
    }

    override fun substitute(from: MType, to: MType): MType {
        return MTuple(types.map { it.substitute(from, to) })
    }

    override fun asString(env: TypeEnv): String {
        val opt = stringOpt(env)
        if(opt.isPresent) {
            return opt.get()
        }
        return types.map { t -> t.asString(env) }.joinToString(" * ")
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MTuple) return false
        if(otherType.types.size != this.types.size) return false
        for(i in types.indices) {
            if(!types[i].isSame(otherType.types[i])) return false
        }
        return true
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

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        type.unify(other, typeSystem, looser)
    }

    override fun occurs(other: MType): Boolean {
        return type.occurs(other)
    }

    override fun find(): MType {
        return type.find()
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MConstr) return false
        if(argType.isPresent) {
            val at = argType.get()
            if(otherType.argType.isEmpty) return false
            if(!at.isSame(otherType.argType.get())) return false
        }
        else if(otherType.argType.isPresent) {
            return false
        }
        return name == otherType.name && type.isSame(otherType.type)
    }
}