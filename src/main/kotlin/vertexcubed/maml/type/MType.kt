package vertexcubed.maml.type

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

    //Override for non primitives
    open fun unify(other: MType, typeSystem: TypeSystem) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType, typeSystem)
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
            if(t is ModuleType) {
                val modString = stringOpt(t.types)
                if(modString.isPresent) return Optional.of("${t.name}.${modString.get()}")
            }

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

    override fun unify(other: MType, typeSystem: TypeSystem) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType, typeSystem)
            return
        }
        if(otherType !is MFunction) throw UnifyException(myType, otherType)
        arg.unify(otherType.arg, typeSystem)
        ret.unify(otherType.ret, typeSystem)
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

    override fun unify(other: MType, typeSystem: TypeSystem) {
        val myType = find()
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(myType, typeSystem)
            return
        }
        if(otherType !is MTuple) throw UnifyException(myType, otherType)
        if(otherType.types.size != types.size) throw UnifyException(myType, otherType)
        for(i in types.indices) {
            types[i].unify(otherType.types[i], typeSystem)
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




data class MVariantType(val id: UUID, val args: List<Pair<String, MType>>): MType() {

    override fun occurs(other: MType): Boolean {
        for(arg in args) {
            if(arg.second.occurs(other)) return true
        }
        return false
    }


    override fun unify(other: MType, typeSystem: TypeSystem) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            return otherType.unify(this, typeSystem)
        }
        if(otherType !is MVariantType) throw UnifyException(this, otherType)
        if(otherType.id != this.id) throw UnifyException(this, otherType)
        if(otherType.args.size != args.size) throw UnifyException(this, otherType)
        for(i in args.indices) {
            args[i].second.unify(otherType.args[i].second, typeSystem)
        }
    }

    override fun substitute(from: MType, to: MType): MType {
        return MVariantType(id, args.map { a -> Pair(a.first, a.second.substitute(from, to)) })
    }

    override fun asString(env: TypeEnv): String {
        return stringOpt(env, env, "").getOrElse { this.toString() }
    }

    private fun stringOpt(env: TypeEnv, parentEnv: TypeEnv, module: String): Optional<String> {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.type.find()
            if(otherType is ModuleType) {
                val modString = stringOpt(otherType.types, parentEnv, "$module${otherType.name}.")
                if(modString.isPresent) return modString
            }
            if(otherType is MVariantType && otherType.id == this.id) {
                var str = ""
                if(args.size == 1) {
                    str += args[0].second.asString(parentEnv) + " "
                }
                else if(args.isNotEmpty()) {
                    str += args.map { p -> p.second.asString(parentEnv) }.joinToString(", ", "(" , ") ")
                }
                return Optional.of(str + module + k)
            }
        }
        return Optional.empty()
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MVariantType) return false
        return id == otherType.id
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

    override fun unify(other: MType, typeSystem: TypeSystem) {
        val otherType = other.find()
        when(otherType) {
            is MTypeVar -> {
                return otherType.unify(this, typeSystem)
            }
            is MTypeAlias -> {
                return real.unify(otherType.real, typeSystem)
            }
            else -> {
                return real.unify(other, typeSystem)
            }
        }
    }

    override fun asString(env: TypeEnv): String {
        return stringOpt(env, env, "").getOrElse { toString() }
    }

    private fun stringOpt(env: TypeEnv, parentEnv: TypeEnv, module: String): Optional<String> {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.type.find()
            if(otherType is ModuleType) {
                val modString = stringOpt(otherType.types, parentEnv, "$module${otherType.name}.")
                if(modString.isPresent) return modString
            }
            if(otherType is MTypeAlias && otherType.id == this.id) {
                var str = ""
                if(args.size == 1) {
                    str += args[0].second.asString(parentEnv) + " "
                }
                else if(args.isNotEmpty()) {
                    str += args.map { p -> p.second.asString(parentEnv) }.joinToString(", ", "(" , ") ")
                }
                return Optional.of(str + module + k)
            }
        }
        return Optional.empty()
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType is MTypeAlias) {
            return real.isSame(otherType.real)
        }
        return real.isSame(otherType)
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

    override fun unify(other: MType, typeSystem: TypeSystem) {
        type.unify(other, typeSystem)
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

    override fun unify(other: MType, typeSystem: TypeSystem) {
        throw UnifyException(this, other)
    }
}