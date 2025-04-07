package vertexcubed.maml.ast

import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnboundVarException
import vertexcubed.maml.core.UnifyException
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*
import java.util.*
import kotlin.jvm.optionals.getOrElse

class UnitNode(line: Int) : AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return UnitValue
    }

    override fun inferType(env: TypeEnv): MType {
        return MUnit
    }

    override fun pretty(): String {
        return "unit"
    }

    override fun toString(): String {
        return "Unit"
    }
}

class TrueNode(line: Int) : AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(true)
    }

    override fun inferType(env: TypeEnv): MType {
        return MBool
    }

    override fun pretty(): String {
        return "true"
    }

    override fun toString(): String {
        return "true"
    }
}

class FalseNode(line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(false)
    }

    override fun inferType(env: TypeEnv): MType {
        return MBool
    }

    override fun pretty(): String {
        return "false"
    }

    override fun toString(): String {
        return "false"
    }
}

class StringNode(val text: String, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return StringValue(text)
    }

    override fun inferType(env: TypeEnv): MType {
        return MString
    }

    override fun pretty(): String {
        return "\"$text\""
    }

    override fun toString(): String {
        return "\"$text\""
    }
}

class CharNode(val text: Char, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return CharValue(text)
    }

    override fun inferType(env: TypeEnv): MType {
        return MChar
    }

    override fun pretty(): String {
        return "\"${text}\""
    }

    override fun toString(): String {
        return "\"${text}\""
    }

}

/**
 * All integers in MaML are 64-bit.
 */
class IntegerNode(val number: Long, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return IntegerValue(number)
    }

    override fun inferType(env: TypeEnv): MType {
        return MInt
    }

    override fun pretty(): String {
        return "$number"
    }

    override fun toString(): String {
        return "$number"
    }
}

class FloatNode(val number: Float, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return FloatValue(number)
    }

    override fun inferType(env: TypeEnv): MType {
        return MFloat
    }

    override fun pretty(): String {
        return "$number"
    }

    override fun toString(): String {
        return "$number"
    }
}

class TupleNode(val nodes: List<AstNode>, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return TupleValue(nodes.map { node -> node.eval(env) })
    }

    override fun inferType(env: TypeEnv): MType {
        return MTuple(nodes.map { node -> node.inferType(env) })
    }

    override fun pretty(): String {
        return "(${nodes.joinToString()})"
    }

    override fun toString(): String {
        return "Tuple($nodes)"
    }
}

class VariableNode(val name: String, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return env.getOrElse(name, { throw UnboundVarException(name) })
    }

    override fun inferType(env: TypeEnv): MType {
        return env.lookupBinding(name).instantiate(env.typeSystem)
    }

    override fun pretty(): String {
        return name
    }

    override fun toString(): String {
        return "Var($name)"
    }
}

class ConDefNode(val name: MBinding, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        if(name.type.isPresent) {
            //purposely discard return type?
            name.type.get().lookup(env)
        }
        //Uhhhh figure out what to do here cuz this is definitely wrong
        return MUnit
    }

    override fun pretty(): String {
        var out = name.binding
        if(name.type.isPresent) {
            out += " of ${name.type.get()}"
        }
        return out
    }

    override fun toString(): String {
        return "ConDef(${pretty()})"
    }

}