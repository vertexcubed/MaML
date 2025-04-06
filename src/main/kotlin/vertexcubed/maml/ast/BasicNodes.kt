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

class ConNode(val name: String, val value: Optional<AstNode>, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        if(value.isPresent) {
            return ConValue(name, Optional.of(value.get().eval(env)))
        }
        return ConValue(name, Optional.empty())
    }

    override fun inferType(env: TypeEnv): MType {
        val myType = env.lookupBinding(name).instantiate(env.typeSystem)
        if(myType !is MConstr) throw IllegalArgumentException("This should never happen?")
        if(value.isEmpty) {
            if(myType.argType.isPresent)
                throw conException(getArgSize(myType.argType), 0)
            return myType.type
        }
        val valueType = value.get().inferType(env)
        if(myType.argType.isEmpty)
            throw conException(0, getArgSize(valueType))
        val expectedType = myType.argType.get()
        try {
            expectedType.unify(valueType)
        }
        catch(e: UnifyException) {
            //Both are tuples, aka multi arg constructors
            if(expectedType is MTuple && valueType is MTuple) {
                //Different sizes
                if(expectedType.types.size != valueType.types.size)
                    throw conException(getArgSize(expectedType), getArgSize(valueType))

                //Technically i should do per tuple type checking but idc lmao
                throw TypeCheckException(line, this, valueType, expectedType)
            }
            //Only one of them are tuples, aka different size args
            if(expectedType is MTuple || valueType is MTuple) {
                throw conException(getArgSize(expectedType), getArgSize(valueType))
            }
            throw TypeCheckException(line, this, valueType, expectedType)
        }


        return myType.type
    }

    private fun conException(expectedSize: Int, actualSize: Int): TypeCheckException {
        return TypeCheckException(line, this, "The constructor $name expects $expectedSize argument(s),\n" +
                "but is applied here to $actualSize argument(s)")
    }

    private fun getArgSize(type: MType): Int {
        return getArgSize(Optional.of(type))
    }

    private fun getArgSize(type: Optional<MType>): Int {
        if(type.isEmpty) return 0
        val t = type.get()
        if(t is MTuple) return t.types.size
        return 1
    }

    override fun pretty(): String {
        var str = name
        if(value.isPresent) {
            var toAdd = value.get().pretty()
            if(value.get() is ConNode) {
                toAdd = "($toAdd)"
            }
            str += " $toAdd"
        }
        return str;
    }

    override fun toString(): String {
        return "Con $name($value)"
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