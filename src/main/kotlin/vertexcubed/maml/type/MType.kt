package vertexcubed.maml.type

import vertexcubed.maml.core.UnifyException
import java.util.*
import kotlin.jvm.optionals.getOrElse


sealed class MType() {

    companion object {
//        val ABSTRACT_ID = 0
        val INT_ID = 1
        val BOOL_ID = 2
        val FLOAT_ID = 3
        val CHAR_ID = 4
        val STRING_ID = 5
        val UNIT_ID = 6
        val FUNCTION_ID = 7
        val TUPLE_ID = 8
        val RECORD_ID = 9
        val EMPTY_ROW_ID = 10
        val NEW_BASE = 11
    }


    /**
     * Check if some other type occurs in this type.
     */
    abstract fun contains(other: MType): Boolean
    open fun find(): MType {
        return this
    }

    fun unify(other: MType, typeSystem: TypeSystem) {
        return unify(other, typeSystem, false)
    }

    /**
     * Unifies this type with another type.
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

            if(t.isSame(this.find())) {
                return Optional.of(k)
            }
        }
        return Optional.empty()
    }

    abstract fun isSame(other: MType): Boolean
}

open class MTypeCon(open val id: Int, open val args: List<MType>): MType() {


    override fun substitute(from: MType, to: MType): MType {
        return MTypeCon(id, args.map { it.substitute(from, to) })
    }

    override fun contains(other: MType): Boolean {
        for(a in args) {
            if(a.contains(other)) return true
        }
        return false
    }


    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            if(looser) throw UnifyException(this, otherType)
            return otherType.unify(this, typeSystem, looser)
        }

        if(otherType !is MTypeCon) throw UnifyException(this, otherType)
        if(otherType.args.size != this.args.size || otherType.id != this.id) throw UnifyException(this, otherType)
        for(i in args.indices) {
            args[i].unify(otherType.args[i], typeSystem, looser)
        }
    }

    override fun asString(env: TypeEnv): String {
        val pair = stringOpt(env, env, "").getOrElse {
            return "TyCon($id, ${args.joinToString(", ") { it.asString(env) }})"
        }
        return "${pair.first}${pair.second}"
    }

    private fun stringOpt(env: TypeEnv, parentEnv: TypeEnv, module: String): Optional<Pair<String, String>> {
        if(id == 15) {
            0
        }
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.instantiate(env.typeSystem).find()
//            if(otherType is ModuleType) {
//                val modString = stringOpt(otherType.types, parentEnv, "$module${otherType.name}.")
//                if(modString.isPresent) return modString
//            }
            if(otherType is MAlias && isSame(otherType)) {
                var str = ""
                if(otherType.realArgs.size == 1) {
                    str += otherType.realArgs[0].asString(parentEnv) + " "
                }
                else if(otherType.realArgs.isNotEmpty()) {
                    str += otherType.realArgs.map { p -> p.asString(parentEnv) }.joinToString(", ", "(" , ") ")
                }
                var modOut = module
                if(modOut.isNotEmpty()) {
                    modOut += "."
                }
                return Optional.of(str to modOut + k)
            }
            if(otherType is MTypeCon && isSame(otherType)) {
                var str = ""
                if(args.size == 1) {
                    str += args[0].asString(parentEnv) + " "
                }
                else if(args.isNotEmpty()) {
                    str += args.map { p -> p.asString(parentEnv) }.joinToString(", ", "(" , ") ")
                }
                var modOut = module
                if(modOut.isNotEmpty()) {
                    modOut += "."
                }
                return Optional.of(str to modOut + k)
            }
        }

        for((k, v) in env.modules.entries.reversed()) {
            val opt = stringOpt(v.types, parentEnv, k)
            if(opt.isPresent) {
                if(module.isNotEmpty()) {
                    return Optional.of(opt.get().first to "$module.${opt.get().second}");
                }
                return opt
            }
        }

        return Optional.empty()
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType is MTypeVar) return otherType.isSame(this)
        if(otherType !is MTypeCon) return false
        if(otherType.id != this.id || otherType.args.size != this.args.size) return false
        for(i in args.indices) {
            if(!otherType.args[i].isSame(this.args[i])) return false
        }
        return true
    }

    override fun toString(): String {
        return "TyCon($id, $args)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MTypeCon

        if (id != other.id) return false
        if (args != other.args) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + args.hashCode()
        return result
    }
}