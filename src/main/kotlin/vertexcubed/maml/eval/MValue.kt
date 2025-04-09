package vertexcubed.maml.eval

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.ast.ConNode
import vertexcubed.maml.core.MIdentifier
import java.util.*

sealed class MValue() {

}

data class CharValue(val value: Char): MValue() {
    override fun toString(): String {
        return value.toString()
    }
}

data class IntegerValue(val value: Long) : MValue() {
    override fun toString(): String {
        return value.toString()
    }
}

data class FloatValue(val value: Float): MValue() {
    override fun toString(): String {
        return value.toString()
    }
}

data object UnitValue : MValue() {
    override fun toString(): String {
        return "()"
    }
}

data class FunctionValue(val arg: String, val expr: AstNode, val env: Map<String, MValue>) : MValue() {
    override fun toString(): String {
        return "<fun>"
    }
}

data class RecursiveFunctionValue(val name: String, val func: FunctionValue): MValue() {
    override fun toString(): String {
        return "<fun>"
    }
}

data class BooleanValue(val value: Boolean) : MValue() {
    override fun toString(): String {
        return value.toString()
    }
}

data class StringValue(val value: String): MValue() {
    override fun toString(): String {
        return value
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
}

data class ConValue(val name: MIdentifier, val value: Optional<MValue>): MValue() {
    override fun toString(): String {
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

/**
 * Module "values." The bindings map is all of the new bindings, while the env map is the environment this module was evaluated in (closure)
 */
data class ModuleValue(val name: String, val bindings: Map<String, MValue>, val env: Map<String, MValue>): MValue() {
    override fun toString(): String {
        return super.toString()
    }
}