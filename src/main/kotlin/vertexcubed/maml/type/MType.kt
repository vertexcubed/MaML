package vertexcubed.maml.type

import vertexcubed.maml.core.UnifyException

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

/**
 * Polymorphic types, e.g. 'a list or 'a option
 */
class MTypeCon(val name: String, val arg: MType): MType() {

    override fun substitute(from: MType, to: MType): MType {
        return this
    }

    //TODO: maybe rewrite
    override fun occurs(other: MType): Boolean {
        val otherType = other.find()
        val myType = find()
        if(myType is MTypeVar && otherType is MTypeVar && myType.id == otherType.id) {
            return true
        }
        return false
    }

    override fun unify(other: MType) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            otherType.unify(this)
            return
        }
        if(otherType is MTypeCon) {
            val myArg = arg.find()
            val otherArg = otherType.arg.find()
            myArg.unify(otherArg)
            return
        }
        throw UnifyException(this, otherType)
    }

    override fun toString(): String {
        return "$name of $arg"
    }
}
