package vertexcubed.maml.type

import vertexcubed.maml.core.BindException
import vertexcubed.maml.core.UnifyException
import java.util.*

class MTypeVar(val id: Int): MType() {

    private var substitute = Optional.empty<MType>()

    override fun occurs(other: MType): Boolean {
        if(substitute.isPresent) return substitute.get().occurs(other)
        if(other is MTypeVar) {
            return id == other.id
        }
        return false
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
        val myType = find()
        val otherType = other.find()
        if(otherType.occurs(myType)) throw UnifyException(myType, otherType)

        if(myType !is MTypeVar && otherType is MTypeVar) {
            otherType.unify(myType)
            return
        }

        bind(otherType)
    }

    override fun substitute(from: MType, to: MType): MType {
        val me = find()
        if(me is MTypeVar) {
            val first = from.find()
            if(first is MTypeVar && me.id == first.id) {
                return to
            }
        }
        return this
    }

    override fun toString(): String {
        if(substitute.isPresent) return substitute.get().toString()
        return "'${idToStr()}"
    }

    private fun idToStr(): String {
        var num = id + 1
        var str = ""
        while(num > 0) {
            val digit = num % 26
            //ASCII a is 96
            val letter = (digit + 96).toChar()
            str = letter + str
            num /= 26
        }
        return str
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