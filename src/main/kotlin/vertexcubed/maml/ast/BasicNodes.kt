package vertexcubed.maml.ast

import vertexcubed.maml.core.MBinding
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.UnboundTypeLabelException
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*

class UnitNode(line: Int) : AstNode(line) {
    override fun eval(env: DynEnv): MValue {
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
    override fun eval(env: DynEnv): MValue {
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

    override fun eval(env: DynEnv): MValue {
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

    override fun eval(env: DynEnv): MValue {
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
    override fun eval(env: DynEnv): MValue {
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

    override fun eval(env: DynEnv): MValue {
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

    override fun eval(env: DynEnv): MValue {
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
    override fun eval(env: DynEnv): MValue {
        return TupleValue(nodes.map { node -> node.eval(env) })
    }

    override fun inferType(env: TypeEnv): MType {
        return MTuple(nodes.map { node -> node.inferType(env) })
    }

    override fun pretty(): String {
        return nodes.joinToString(", ", "(", ")") { n -> n.pretty() }
    }

    override fun toString(): String {
        return "Tuple($nodes)"
    }
}

class RecordLiteralNode(val fields: Map<String, AstNode>, line: Int): AstNode(line) {
    override fun eval(env: DynEnv): MValue {
        return RecordValue(fields.mapValues { (_, v) -> v.eval(env) })
    }

    override fun inferType(env: TypeEnv): MType {
        return MRecord(fields.mapValues { (_, v) -> v.inferType(env) }, MEmptyRow)
    }

    override fun pretty(): String {
        return fields.toList().joinToString("; ", "{ ", " }") { (k, v) -> "$k=${v.pretty()}" }
    }

    override fun toString(): String {
        return "Record($fields)"
    }

}

class VariableNode(val name: MIdentifier, line: Int): AstNode(line) {
    constructor(name: String, line: Int): this(MIdentifier(name), line)

    override fun eval(env: DynEnv): MValue {
        return env.lookupBinding(name)
    }

    override fun inferType(env: TypeEnv): MType {
        return env.lookupBinding(name).instantiate(env.typeSystem)
    }

    override fun pretty(): String {
        return name.toString()
    }

    override fun toString(): String {
        return "Var($name)"
    }
}
//TODO: refactor so you don't actually call inferType
class ConDefNode(val name: MBinding, line: Int): AstNode(line) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        if(name.type.isPresent) {
            //purposely discard return type?
            var expectedType: MType
            try {
                expectedType = name.type.get().lookup(newEnv)
            }
            catch(e: UnboundTypeLabelException) {
                expectedType = newEnv.typeSystem.newTypeVar()
                newEnv.addVarLabel(e.type.name to expectedType)
            }

        }
        //Uhhhh figure out what to do here cuz this is definitely wrong
        return MUnit
    }

    override fun pretty(): String {
        var out = name.binding.toString()
        if(name.type.isPresent) {
            out += " of ${name.type.get()}"
        }
        return out
    }

    override fun toString(): String {
        return "ConDef(${pretty()})"
    }

}