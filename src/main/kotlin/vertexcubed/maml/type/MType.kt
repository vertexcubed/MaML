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
}

data object MInt: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MInt
    }

    override fun toString(): String {
        return "int"
    }
}
data object MFloat: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MFloat
    }

    override fun toString(): String {
        return "float"
    }
}
data object MBool: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MBool
    }

    override fun toString(): String {
        return "bool"
    }
}
data object MString: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MString
    }

    override fun toString(): String {
        return "string"
    }
}
data object MChar: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MChar
    }

    override fun toString(): String {
        return "int"
    }
}
data object MUnit: MType() {
    override fun substitute(from: MType, to: MType): MType {
        return MUnit
    }

    override fun toString(): String {
        return "unit"
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
}

data class MVariantType(val name: String, val args: List<Pair<String, MType>>): MType() {

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
        return MVariantType(name, args.map { a -> Pair(a.first, a.second.substitute(from, to)) })
    }

    override fun toString(): String {
        var str = ""
        if(args.size > 1) {
            str += args.map { p -> p.second.toString() }.joinToString(separator = ", ", prefix = "(", postfix = ")") + " "
        }
        else {
            if(args.isNotEmpty()) str += "${args[0].second} "
        }
        return str + name
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