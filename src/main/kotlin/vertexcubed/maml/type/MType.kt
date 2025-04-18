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


data class MStaticRecord(val fields: Map<String, MType>): MType() {

    override fun occurs(other: MType): Boolean {
        for(f in fields.values) {
            if(f.occurs(other)) return true
        }
        return false
    }

    override fun toString(): String {
        return fields.toList().joinToString("; ", "{", "}") { (k, v) -> "$k:$v" }
    }

    override fun asString(env: TypeEnv): String {
        val opt = stringOpt(env)
        if(opt.isPresent) {
            return opt.get()
        }
        return fields.toList().joinToString("; ", "{", "}") { (k, v) -> "$k:${v.asString(env)}" }
    }

    override fun unify(other: MType) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            return otherType.unify(this)
        }
        if(otherType is MPolyRecord) {
            return otherType.unify(this)
        }
        if(otherType !is MStaticRecord) {
            throw UnifyException(this, otherType)
        }

        for((k, v) in fields) {
            //TODO: more detailed error?
            v.unify(otherType.fields.getOrElse(k, {throw UnifyException(this, otherType)}))
        }
        for(k in fields.keys) {
            if(k !in fields) {
                throw UnifyException(this, otherType)
            }
        }

    }

    override fun substitute(from: MType, to: MType): MType {
        return MStaticRecord(fields.mapValues { (_, v) -> v.substitute(from, to) })
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MStaticRecord) return false
        for((k, v) in fields) {
            if(k !in otherType.fields) return false
            if(!v.isSame(otherType.fields[k]!!)) return false
        }
        for(k in otherType.fields.keys) {
            if(k !in fields) return false
        }
        return true
    }
}

/**
 * Polymorphic record types.
 * These are MUTABLE internally, meaning a lot of special logic needs to be done to prevent unecessary cloning! See substitute
 */
data class MPolyRecord(val fields: Map<String, MType>, val rowVar: MType): MType() {

    private var binding = Optional.empty<MType>()

    init {
        if(fields.isEmpty()) throw IllegalArgumentException("Cannot create record with no fields!")
    }

    override fun find(): MType {
        return if(binding.isPresent) binding.get().find() else this
    }

    fun isBound(): Boolean {
        return binding.isPresent
    }

    fun bind(other: MType) {
        val last = other.find()
        if(binding.isPresent) {
            val sub = binding.get()
            if(sub is MPolyRecord) {
                sub.bind(last)
                return
            }
            if(sub == last) {
                return
            }
            else throw BindException(this, sub)
        }
        if(other is MStaticRecord || other is MPolyRecord) {
            binding = Optional.of(last)
        }
        else throw BindException(this, last)
    }


    override fun occurs(other: MType): Boolean {
        if(binding.isPresent) {
            return binding.get().occurs(other)
        }
        for(f in fields.values) {
            if(f.occurs(other)) return true
        }
        return false
    }

    override fun unify(other: MType) {
        if(binding.isPresent) {
            return binding.get().unify(other)
        }

        val otherType = other.find()
        when(otherType) {
            is MTypeVar -> {
                return otherType.unify(this)
            }
            is MStaticRecord -> {
                val map = mutableMapOf<String, MType>()
                for((k, v) in fields) {
                    //If I contain explicit fields that are not in static record
                    v.unify(otherType.fields.getOrElse(k, {throw UnifyException(this, otherType)}))

                    map += k to v
                }

                for((k, v) in otherType.fields) {
                    //TODO: more detailed errors?
                    if(k !in fields) {
                        map += k to v
                    }
                }
                this.bind(MStaticRecord(map))

            }
            is MPolyRecord -> {

                if(rowVar is MTypeVar &&
                    otherType.rowVar is MTypeVar &&
                    rowVar.id == otherType.rowVar.id) return


                val myMap = mutableMapOf<String, MType>()
                val otherMap = mutableMapOf<String, MType>()

                for((k, v) in fields) {
                    if(k in otherType.fields) {
                        v.unify(otherType.fields[k]!!)
                    }
                    myMap += k to v
                    otherMap += k to v

                }
                for((k, v) in otherType.fields) {
                    if(k in fields) {
                        continue
                    }
                    myMap += k to v
                    otherMap += k to v
                }

                //Uh this should be fine? I can't make a new typevar but yeagh
                this.rowVar.unify(otherType.rowVar)

                this.bind(MPolyRecord(myMap, rowVar))
                otherType.bind(MPolyRecord(myMap, otherType.rowVar))

            }
            else -> {
                throw UnifyException(this, otherType)
            }
        }
    }

    override fun asString(env: TypeEnv): String {
        if(binding.isPresent) return binding.get().asString(env)
        if(fields.isEmpty()) {
            return "{..}"
        }
        return fields.toList().joinToString("; ", "{", "; ..${rowVar.asString(env)}}") { (k, v) -> "$k: ${v.asString(env)}" }
    }

    override fun toString(): String {
        if(binding.isPresent) return binding.get().toString()
        return fields.toList().joinToString("; ", "{", "; ..$rowVar}") { (k, v) -> "$k: $v}" }
    }

    override fun substitute(from: MType, to: MType): MType {
        if(isSame(from)) return to


        if(binding.isPresent) {
            return binding.get().substitute(from, to)
        }

        //Since this is kind of mutable, we should check if we need to actually do any substitutions first to avoid the constructor call. Fixes issues in ForAll#substitute
        var makeNew = false
        val map = mutableMapOf<String, MType>()
        for((k, v) in fields) {
            val nv = v.substitute(from, to)
            if(!v.isSame(nv)) makeNew = true
            map += k to v
        }

        if(makeNew) {
            return MPolyRecord(map, rowVar)
        }
        return this
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MPolyRecord) return false
        if(!rowVar.isSame(otherType.rowVar)) return false

        for((k, v) in fields) {
            if(k !in otherType.fields) return false
            if(!v.isSame(otherType.fields[k]!!)) return false
        }
        for(k in otherType.fields.keys) {
            if(k !in fields) return false
        }
        return true
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


    override fun unify(other: MType) {
        val otherType = other.find()
        if(otherType is MTypeVar) {
            return otherType.unify(this)
        }
        if(otherType !is MVariantType) throw UnifyException(this, otherType)
        if(otherType.id != this.id) throw UnifyException(this, otherType)
        if(otherType.args.size != args.size) throw UnifyException(this, otherType)
        for(i in args.indices) {
            args[i].second.unify(otherType.args[i].second)
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

    override fun unify(other: MType) {
        type.unify(other)
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

    override fun unify(other: MType) {
        throw UnifyException(this, other)
    }
}