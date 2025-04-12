package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import vertexcubed.maml.core.UnifyException
import java.util.*

class MTypeVar(val id: Int): MType() {

    var display = "'$id"

    private var substitute = Optional.empty<MType>()

    override fun occurs(other: MType): Boolean {
        if(substitute.isPresent) return substitute.get().occurs(other)
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

    override fun unify(other: MType) {
        if(substitute.isPresent) {
            substitute.get().unify(other)
            return
        }
        val otherType = other.find()
        if(otherType is MTypeVar && this.id == otherType.id) return
        if(otherType.occurs(this)) throw UnifyException(this, otherType)
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

}

data class MGeneralTypeVar(val id: Int): MType() {

    override fun occurs(other: MType): Boolean {
        return this == other
    }

    override fun unify(other: MType) {
        throw UnifyException(this, other)
    }

    override fun substitute(from: MType, to: MType): MType {
        val first = from.find()
        if(first is MGeneralTypeVar && id == first.id) {
            return to
        }
        return this
    }

    override fun toString(): String {
        return "'t$id"
    }
}