package vertexcubed.maml.eval

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.type.MTuple
import java.util.*

sealed class MValue() {
    operator abstract fun compareTo(y: MValue): Int

}

data class CharValue(val value: Char): MValue() {
    override fun compareTo(y: MValue): Int {
        if(y !is CharValue) throw IllegalArgumentException("Cannot compare against $y: not a char")
        return value.compareTo(y.value)
    }

    override fun toString(): String {
        return value.toString()
    }
}

data class IntegerValue(val value: Long) : MValue() {
    override fun compareTo(y: MValue): Int {
        if(y !is IntegerValue) throw IllegalArgumentException("Cannot compare against $y: not an int")
        return value.compareTo(y.value)
    }

    override fun toString(): String {
        return value.toString()
    }
}

data class FloatValue(val value: Float): MValue() {
    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(y: MValue): Int {
        if(y !is FloatValue) throw IllegalArgumentException("Cannot compare against $y: not a float")
        return value.compareTo(y.value)
    }
}

data object UnitValue : MValue() {
    override fun compareTo(y: MValue): Int {
        if(y !is UnitValue) throw IllegalArgumentException("Cannot compare against $y: not unit")
        return 0
    }

    override fun toString(): String {
        return "()"
    }
}

data class BooleanValue(val value: Boolean) : MValue() {
    override fun toString(): String {
        return value.toString()
    }
    override fun compareTo(y: MValue): Int {
        if(y !is BooleanValue) throw IllegalArgumentException("Cannot compare against $y: not a bool")
        return value.compareTo(y.value)
    }
}

data class StringValue(val value: String): MValue() {
    override fun toString(): String {
        return value
    }

    override fun compareTo(y: MValue): Int {
        if(y !is StringValue) throw IllegalArgumentException("Cannot compare against $y: not a string")
        return value.compareTo(y.value)
    }
}

data class FunctionValue(val arg: String, val expr: AstNode, val env: DynEnv) : MValue() {
    override fun toString(): String {
        return "<fun>"
    }

    override fun compareTo(y: MValue): Int {
        throw IllegalArgumentException("Cannot compare function values!")
    }
}

data class RecursiveFunctionValue(val name: String, val func: FunctionValue): MValue() {
    override fun toString(): String {
        return "<fun>"
    }
    override fun compareTo(y: MValue): Int {
        throw IllegalArgumentException("Cannot compare function values!")
    }
}

data class TupleValue(val values: List<MValue>): MValue() {
    override fun toString(): String {
        var str = "("
        for(i in values.indices) {
            str += values[i].toString()
            if(i < values.size - 1) str += ", "
        }
        return "$str)"
    }
    override fun compareTo(y: MValue): Int {
        if(y !is TupleValue) throw IllegalArgumentException("Cannot compare against $y: not a tuple")
        if(values.size != y.values.size) {
            values.size - y.values.size
        }
        for(i in values.indices) {
            val comp = values[i].compareTo(y.values[i])
            if(comp != 0) return comp
        }
        return 0
    }
}

data class RecordValue(val values: Map<String, MValue>): MValue() {
    override fun compareTo(y: MValue): Int {
        throw IllegalArgumentException("Cannot compare tuples!")
    }

    override fun toString(): String {
        return values.toList().map { (k, v) -> "$k=$v" }.joinToString("; ", "{ ", " }")
    }
}

data class ExternalValue(val javaFunc: String): MValue() {
    override fun toString(): String {
        return "<extern-fun>"
    }

    override fun compareTo(y: MValue): Int {
        throw IllegalArgumentException("Cannot compare external values!")
    }
}

data class ConValue(val name: MIdentifier, val value: Optional<MValue>): MValue() {
    override fun compareTo(y: MValue): Int {
        throw IllegalArgumentException("Cannot compare type constructors!")
    }

    override fun toString(): String {
        if(name == MIdentifier("::")) {
            var str = "["
            var trav = this
            var isList = false
            while(true) {
                if(trav.value.isEmpty) {
                    if(trav.name == MIdentifier("[]")) {
                        isList = true
                        str += "]"
                        break
                    }
                    else break
                }

                val value = trav.value.get()
                if(value is TupleValue && value.values.size == 2) {
                    val left = value.values[0]
                    val right = value.values[1]
                    str += left.toString()
                    if(right is ConValue) {
                        if(right.name != MIdentifier("[]")) {
                            str += "; "
                        }
                        trav = right
                    }
                }
                else {
                    break
                }
            }

            if(isList) return str
        }



        var str = name.toString()
        if(value.isPresent) {
            var toAdd = value.get().toString()
            if(value.get() is ConValue) {
                toAdd = "($toAdd)"
            }
            str += " $toAdd"
        }
        return str;
    }
}