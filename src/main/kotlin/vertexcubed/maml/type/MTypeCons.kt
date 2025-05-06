package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import java.util.*
import kotlin.jvm.optionals.getOrElse

data object MInt: MTypeCon(INT_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MInt
    }
}

data object MFloat: MTypeCon(FLOAT_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MFloat
    }
}

data object MBool: MTypeCon(BOOL_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MBool
    }
}

data object MString: MTypeCon(STRING_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MString
    }
}

data object MChar: MTypeCon(CHAR_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MChar
    }
}

data object MUnit: MTypeCon(UNIT_ID, emptyList()) {
    override fun substitute(from: MType, to: MType): MType {
        return MUnit
    }
}

data class MFunction(val arg: MType, val ret: MType): MTypeCon(FUNCTION_ID, listOf(arg, ret)) {

    override fun toString(): String {
        val argStr: String
        if(arg.find() is MFunction) {
            argStr = "($arg)"
        }
        else argStr = arg.toString()

        return "$argStr -> $ret"
    }

    override fun asString(env: TypeEnv): String {
        val opt = stringOpt(env)
        if(opt.isPresent) {
            return opt.get()
        }
        return "${arg.asString(env)} -> ${ret.asString(env)}"
    }

    override fun substitute(from: MType, to: MType): MType {
        return MFunction(arg.substitute(from, to), ret.substitute(from, to))
    }
}

data class MTuple(val types: List<MType>): MTypeCon(TUPLE_ID, types) {
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

    override fun asString(env: TypeEnv): String {
        val opt = stringOpt(env)
        if(opt.isPresent) {
            return opt.get()
        }
        return types.map { t -> t.asString(env) }.joinToString(" * ")
    }

    override fun substitute(from: MType, to: MType): MType {
        return MTuple(types.map { t -> t.substitute(from, to) })
    }
}

/**
 * Not *type constructors*, but rather a wrapped type for constructors themselves
 */
data class MConstr(val name: String, val conId: Int, val type: MType, val argType: Optional<MType>): MType() {
    override fun substitute(from: MType, to: MType): MType {
        if(argType.isEmpty) return MConstr(name, conId, type.substitute(from, to), Optional.empty())
        return MConstr(name, conId, type.substitute(from, to), Optional.of(argType.get().substitute(from, to)))
    }

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        type.unify(other, typeSystem, looser)
    }

    override fun contains(other: MType): Boolean {
        return type.contains(other)
    }

    override fun find(): MType {
        return type.find()
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType is MTypeVar) return otherType.isSame(this)
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

data class MVariant(override val id: Int, override val args: List<MType>, val numConstrs: Int): MTypeCon(id, args) {
    override fun substitute(from: MType, to: MType): MType {
        return MVariant(id, args.map { a -> a.substitute(from, to) }, numConstrs)
    }
}

data class MExtensibleVariant(override val id: Int, override val args: List<MType>): MTypeCon(id, args) {
    override fun substitute(from: MType, to: MType): MType {
        return MExtensibleVariant(id, args.map { a -> a.substitute(from, to) })
    }
}

data class MAlias(override val id: Int, val realArgs: List<MType>, override val args: List<MType>): MTypeCon(id, args) {
    override fun substitute(from: MType, to: MType): MType {
        return MAlias(id, realArgs.map { it.substitute(from, to) }, args.map {it.substitute(from, to)})
    }
}