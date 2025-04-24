package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import vertexcubed.maml.core.UnifyException
import java.util.*
import kotlin.jvm.optionals.getOrElse

sealed class MTypeCon(open val args: List<Pair<String, MType>>): MType()


data class MDummyCons(val id: UUID, override val args: List<Pair<String, MType>>): MTypeCon(args) {

    override fun occurs(other: MType): Boolean {
        for(arg in args) {
            if(arg.second.occurs(other)) return true
        }
        return false
    }

    override fun substitute(from: MType, to: MType): MType {
        if(from is MDummyCons && from.id == this.id) {
            return to
        }
        return MDummyCons(id, args.map { (k, v) -> k to v.substitute(from, to) })
    }

    override fun asString(env: TypeEnv): String {
        return stringOpt(env, env, "").getOrElse { this.toString() }
    }

    private fun stringOpt(env: TypeEnv, parentEnv: TypeEnv, module: String): Optional<String> {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.type.find()
//            if(otherType is ModuleType) {
//                val modString = stringOpt(otherType.types, parentEnv, "$module${otherType.name}.")
//                if(modString.isPresent) return modString
//            }
            if(otherType is MDummyCons && otherType.id == this.id) {
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

}


data class MVariantType(val id: UUID, override val args: List<Pair<String, MType>>): MTypeCon(args) {

    override fun occurs(other: MType): Boolean {
        for(arg in args) {
            if(arg.second.occurs(other)) return true
        }
        return false
    }


    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            if(looser) throw UnifyException(this, otherType)

            return otherType.unify(this, typeSystem, looser)
        }
        if(otherType !is MVariantType) throw UnifyException(this, otherType)
        if(otherType.id != this.id) throw UnifyException(this, otherType)
        if(otherType.args.size != args.size) throw UnifyException(this, otherType)
        for(i in args.indices) {
            args[i].second.unify(otherType.args[i].second, typeSystem, looser)
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
//            if(otherType is ModuleType) {
//                val modString = stringOpt(otherType.types, parentEnv, "$module${otherType.name}.")
//                if(modString.isPresent) return modString
//            }
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

data class MTypeAlias(val id: UUID, override val args: List<Pair<String, MType>>, val real: MType): MTypeCon(args) {
    override fun occurs(other: MType): Boolean {
        return real.occurs(other)
    }

    override fun substitute(from: MType, to: MType): MType {
        return MTypeAlias(id, args.map { p -> Pair(p.first, p.second.substitute(from, to)) }, real.substitute(from, to))
    }

//    override fun find(): MType {
//        return real.find()
//    }

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        val otherType = other.find()
        when(otherType) {
            is MTypeVar -> {
                if(looser) throw BindException(this, otherType)

                return otherType.unify(this, typeSystem, looser)
            }
            is MTypeAlias -> {
                return real.unify(otherType.real, typeSystem, looser)
            }
            else -> {
                return real.unify(other, typeSystem, looser)
            }
        }
    }

    override fun asString(env: TypeEnv): String {
        return stringOpt(env, env, "").getOrElse { toString() }
    }

    private fun stringOpt(env: TypeEnv, parentEnv: TypeEnv, module: String): Optional<String> {
        for((k, v) in env.typeDefs.entries.reversed()) {
            val otherType = v.type.find()
//            if(otherType is ModuleType) {
//                val modString = stringOpt(otherType.types, parentEnv, "$module${otherType.name}.")
//                if(modString.isPresent) return modString
//            }
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
