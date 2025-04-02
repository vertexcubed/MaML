package vertexcubed.maml.parse.ast

import vertexcubed.maml.core.*
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*

class UnitNode(line: Int) : AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return UnitValue
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
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

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        return MTuple(nodes.map { node -> node.inferType(env, types) })
    }

    override fun pretty(): String {
        return "($nodes)"
    }

    override fun toString(): String {
        return "Tuple($nodes)"
    }
}

class VariableNode(val name: String, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return env.getOrElse(name, { throw UnboundVarException(name) })
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        return env.getOrElse(name, { throw UnboundVarException(name) }).instantiate(types)
    }

    override fun pretty(): String {
        return name
    }

    override fun toString(): String {
        return "Var($name)"
    }
}