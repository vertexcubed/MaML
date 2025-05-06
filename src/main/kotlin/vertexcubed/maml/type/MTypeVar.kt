package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import vertexcubed.maml.core.UnifyException
import java.util.*

data class MTypeVar(val id: Int): MType() {

    var display = "'$id"

    private var substitute = Optional.empty<MType>()

    override fun contains(other: MType): Boolean {
        if(substitute.isPresent) return substitute.get().contains(other)
        if(other is MTypeVar) {
            return id == other.id
        }
        return false
    }

    fun isBound(): Boolean {
        return substitute.isPresent
    }

    override fun find(): MType {
        if(substitute.isPresent) {
            return substitute.get().find()
        }
        return this
    }

    fun bind(other: MType) {
        val last = other.find()
        if(substitute.isPresent) {
            val sub = substitute.get()
            if(sub is MTypeVar) {
                sub.bind(last)
                return
            }
            if(sub == last) {
                return
            }
            else throw BindException(this, sub)
        }
        substitute = Optional.of(last)
    }

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        if(substitute.isPresent) {
            substitute.get().unify(other, typeSystem, looser)
            return
        }
        val otherType = other.find()
        if(otherType is MTypeVar && this.id == otherType.id) return
        if(otherType.contains(this)) throw UnifyException(this, otherType)
        bind(otherType)
    }

    override fun substitute(from: MType, to: MType): MType {
        if(substitute.isPresent) {
            return substitute.get().substitute(from, to)
        }
        val first = from.find()
        if(first is MTypeVar && id == first.id) {
            return to
        }
        return this
    }

    override fun toString(): String {
        if(substitute.isPresent) return substitute.get().toString()
        return display
    }


    override fun asString(env: TypeEnv): String {
        val end = find()
        if(end is MTypeVar) {
            return end.toString()
        }
        return end.asString(env)
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(substitute.isPresent) {
            return substitute.get().isSame(otherType)
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MTypeVar

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

}

data class MGeneralTypeVar(val id: Int): MType() {

    override fun contains(other: MType): Boolean {
        return this == other
    }

    override fun unify(other: MType, typeSystem: TypeSystem, looser: Boolean) {
        throw UnifyException(this, other)
    }

    override fun substitute(from: MType, to: MType): MType {
        val first = from.find()
        if(first is MGeneralTypeVar && id == first.id) {
            return to
        }
        return this
    }

    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MGeneralTypeVar) return false
        return id == otherType.id
    }

    override fun toString(): String {
        return "'t$id"
    }
}


/**
 * Special type variables that behave like "base" types (int, bool, etc.).
 * For use in signature enrichment
 */
data class MBaseTypeVar(val id: Int): MType() {
    override fun substitute(from: MType, to: MType): MType {
        val first = from.find()
        if(first is MBaseTypeVar && first.id == id) {
            return to
        }
        return this
    }

    override fun contains(other: MType): Boolean {
        return isSame(other)
    }


    override fun isSame(other: MType): Boolean {
        val otherType = other.find()
        if(otherType !is MBaseTypeVar) return false
        return id == otherType.id
    }

    override fun toString(): String {
        return "b$id"
    }

}