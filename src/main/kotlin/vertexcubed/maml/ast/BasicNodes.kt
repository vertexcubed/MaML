package vertexcubed.maml.ast

import vertexcubed.maml.core.MBinding
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.RecordException
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*

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
        return nodes.joinToString(", ", "(", ")") { n -> n.pretty() }
    }

    override fun toString(): String {
        return "Tuple($nodes)"
    }
}

class RecordLiteralNode(val fields: Map<String, AstNode>, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return RecordValue(fields.mapValues { (_, v) -> v.eval(env) })
    }

    override fun inferType(env: TypeEnv): MType {
        return MStaticRecord(fields.mapValues { (_, v) -> v.inferType(env) })
    }

    override fun pretty(): String {
        return fields.toList().joinToString("; ", "{ ", " }") { (k, v) -> "$k=${v.pretty()}" }
    }

    override fun toString(): String {
        return "Record($fields)"
    }

}

class RecordLookupNode(val record: AstNode, val field: String, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        val recordVal = record.eval(env)
        if(recordVal !is RecordValue) {
            throw RecordException("Cannot access record field of non-record value!")
        }
        return recordVal.values.getOrElse(field, { throw RecordException("Record ${record.pretty()} does not contain field $field!")})
    }

    override fun inferType(env: TypeEnv): MType {
        val recordType = record.inferType(env)
        val retType = env.typeSystem.newTypeVar()
        val polyRecord = MPolyRecord(mapOf(field to retType), env.typeSystem.newTypeVar())
        recordType.unify(polyRecord)
        return retType

    }

    override fun toString(): String {
        return "Lookup($record, $field)"
    }
}


class VariableNode(val name: MIdentifier, line: Int): AstNode(line) {
    constructor(name: String, line: Int): this(MIdentifier(name), line)

    override fun eval(env: Map<String, MValue>): MValue {
        return name.lookupEvalBinding(env)
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

class ConDefNode(val name: MBinding, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        if(name.type.isPresent) {
            //purposely discard return type?
            name.type.get().lookupOrMutate(newEnv, true)
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