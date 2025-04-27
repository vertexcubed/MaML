package vertexcubed.maml.ast

import vertexcubed.maml.compile.CompEnv
import vertexcubed.maml.compile.bytecode.ZChar
import vertexcubed.maml.compile.bytecode.ZFloat
import vertexcubed.maml.compile.bytecode.ZInt
import vertexcubed.maml.compile.bytecode.ZString
import vertexcubed.maml.compile.lambda.LConst
import vertexcubed.maml.compile.lambda.LambdaNode
import vertexcubed.maml.core.MBinding
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnboundException
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*

class UnitNode(loc: NodeLoc) : AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        return UnitValue
    }

    override fun inferType(env: TypeEnv): MType {
        return MUnit
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZInt(0), loc)
    }

    override fun pretty(): String {
        return "unit"
    }

    override fun toString(): String {
        return "Unit"
    }
}

class TrueNode(loc: NodeLoc) : AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        return BooleanValue(true)
    }

    override fun inferType(env: TypeEnv): MType {
        return MBool
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZInt(1), loc)
    }

    override fun pretty(): String {
        return "true"
    }

    override fun toString(): String {
        return "true"
    }
}

class FalseNode(loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        return BooleanValue(false)
    }

    override fun inferType(env: TypeEnv): MType {
        return MBool
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZInt(0), loc)
    }

    override fun pretty(): String {
        return "false"
    }

    override fun toString(): String {
        return "false"
    }
}

class StringNode(val text: String, loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        return StringValue(text)
    }

    override fun inferType(env: TypeEnv): MType {
        return MString
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZString(text), loc)
    }

    override fun pretty(): String {
        return "\"$text\""
    }

    override fun toString(): String {
        return "\"$text\""
    }
}

class CharNode(val text: Char, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        return CharValue(text)
    }

    override fun inferType(env: TypeEnv): MType {
        return MChar
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZChar(text), loc)
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
class IntegerNode(val number: Long, loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        return IntegerValue(number)
    }

    override fun inferType(env: TypeEnv): MType {
        return MInt
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZInt(number), loc)
    }

    override fun pretty(): String {
        return "$number"
    }

    override fun toString(): String {
        return "$number"
    }
}

class FloatNode(val number: Float, loc: NodeLoc): AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        return FloatValue(number)
    }

    override fun inferType(env: TypeEnv): MType {
        return MFloat
    }

    override fun compile(env: CompEnv): LambdaNode {
        return LConst(ZFloat(number), loc)
    }

    override fun pretty(): String {
        return "$number"
    }

    override fun toString(): String {
        return "$number"
    }
}

class TupleNode(val nodes: List<AstNode>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        return TupleValue(nodes.map { node -> node.eval(env) })
    }

    override fun inferType(env: TypeEnv): MType {
        return MTuple(nodes.map { node -> node.inferType(env) })
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        return nodes.joinToString(", ", "(", ")") { n -> n.pretty() }
    }

    override fun toString(): String {
        return "Tuple($nodes)"
    }
}

class RecordLiteralNode(val fields: Map<String, AstNode>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        return RecordValue(fields.mapValues { (_, v) -> v.eval(env) })
    }

    override fun inferType(env: TypeEnv): MType {
        return MRecord(fields.mapValues { (_, v) -> v.inferType(env) }, MEmptyRow)
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        return fields.toList().joinToString("; ", "{ ", " }") { (k, v) -> "$k=${v.pretty()}" }
    }

    override fun toString(): String {
        return "Record($fields)"
    }

}

class VariableNode(val name: MIdentifier, loc: NodeLoc): AstNode(loc) {
    constructor(name: String, loc: NodeLoc): this(MIdentifier(name), loc)

    override fun eval(env: DynEnv): MValue {
        return env.lookupBinding(name)
    }

    override fun inferType(env: TypeEnv): MType {
        try {
            return env.lookupBinding(name).instantiate(env.typeSystem)
        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        return name.toString()
    }

    override fun toString(): String {
        return "Var($name)"
    }
}
//TODO: refactor so you don't actually call inferType
class ConDefNode(val name: MBinding, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        if(name.type.isPresent) {
            //purposely discard return type?
            val labels = name.type.get().getAllLabels()
            for(l in labels) {
                newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
            }
            try {
                val expectedType = name.type.get().lookup(newEnv)
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }


        }
        //TODO: Uhhhh figure out what to do here cuz this is definitely wrong
        return MUnit
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
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