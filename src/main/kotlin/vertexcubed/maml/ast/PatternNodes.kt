package vertexcubed.maml.ast

import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnifyException
import vertexcubed.maml.eval.ConValue
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.TupleValue
import vertexcubed.maml.type.MConstr
import vertexcubed.maml.type.MTuple
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv
import java.util.*
import kotlin.collections.ArrayList

sealed class PatternNode(line: Int): AstNode(line) {
    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not inferType of a pattern, use inferPatternType instead.")
    }

    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Patterns should never be evaluated!")
    }

    abstract fun inferPatternType(emv: TypeEnv): Pair<MType, Map<String, MType>>

    abstract fun unify(expr: MValue): Optional<Map<String, MValue>>

    fun patException(actualType: MType, expectedType: MType): TypeCheckException {
        return TypeCheckException(line, this, "This pattern matches values of type $actualType\n" +
                "but a pattern was expected which matches values of type $expectedType")
    }
}

class ConstantPatternNode(val value: AstNode, line: Int): PatternNode(line) {

    override fun inferPatternType(env: TypeEnv): Pair<MType, Map<String, MType>> {
        return Pair(value.inferType(env), emptyMap())
    }

    override fun unify(expr: MValue): Optional<Map<String, MValue>> {
        val myVal = value.eval(emptyMap())
        if(myVal == expr) {
            return Optional.of(emptyMap())
        }
        return Optional.empty()
    }

    override fun pretty(): String {
        return value.pretty()
    }

    override fun toString(): String {
        return "Pat($value)"
    }

}

class OrPatternNode(val nodes: List<PatternNode>, line: Int): PatternNode(line) {
    init {
        if(nodes.isEmpty()) throw IllegalArgumentException("Cannot have | pattern with 0 patterns!")
    }

    override fun inferPatternType(env: TypeEnv): Pair<MType, Map<String, MType>> {
        var (lastType, lastBindings) = nodes[0].inferPatternType(env)
        for(i in 1 until nodes.size) {
            val (curType, curBindings) = nodes[i].inferPatternType(env)
            try {
                lastType.unify(curType)
            }
            catch(e: UnifyException) {
                throw patException(curType, lastType)
            }
            lastType = curType

            for((b, bType) in lastBindings) {
                val cType = curBindings.getOrElse(b, { throw sideException(b) })
                bType.unify(cType)
            }
            for((c, _) in curBindings) {
                if(!lastBindings.containsKey(c)) {
                    throw sideException(c)
                }
            }

            lastBindings = curBindings
        }

        return lastType to lastBindings
    }

    override fun unify(expr: MValue): Optional<Map<String, MValue>> {
        for(node in nodes) {
            val bindings = node.unify(expr)
            if(bindings.isPresent) return bindings
        }
        return Optional.empty()
    }

    private fun sideException(binding: String): TypeCheckException {
        return TypeCheckException(line, this, "Variable $binding must occur on all sides of this | pattern")
    }

    override fun pretty(): String {
        return nodes.map { p -> p.pretty() }.joinToString(separator = " | ")
    }

}

class VariablePatternNode(val name: String, line: Int): PatternNode(line) {
    override fun inferPatternType(env: TypeEnv): Pair<MType, Map<String, MType>> {
        val varType = env.typeSystem.newTypeVar()
        return Pair(varType, mapOf(name to varType))
    }

    override fun unify(expr: MValue): Optional<Map<String, MValue>> {
        return Optional.of(mapOf(name to expr))
    }

    override fun pretty(): String {
        return name
    }

    override fun toString(): String {
        return "VarPat($name)"
    }

}


class WildcardPatternNode(line: Int): PatternNode(line) {
    override fun inferPatternType(env: TypeEnv): Pair<MType, Map<String, MType>> {
        return Pair(env.typeSystem.newTypeVar(), emptyMap())
    }

    override fun unify(expr: MValue): Optional<Map<String, MValue>> {
        return Optional.of(emptyMap())
    }

    override fun pretty(): String {
        return "_"
    }

    override fun toString(): String {
        return "WildcardPat"
    }

}


class TuplePatternNode(val nodes: List<PatternNode>, line: Int): PatternNode(line) {
    override fun inferPatternType(env: TypeEnv): Pair<MType, Map<String, MType>> {
        val types = ArrayList<MType>()
        var bindings = emptyMap<String, MType>()
        for(node in nodes) {
            val (nType, nBindings) = node.inferPatternType(env)
            for(n in nBindings.keys) {
                if(n in bindings) {
                    throw multibound(n)
                }
            }
            types.add(nType)
            bindings += nBindings
        }
        return MTuple(types) to bindings
    }

    override fun unify(expr: MValue): Optional<Map<String, MValue>> {
        if(expr !is TupleValue) return Optional.empty()
        if(nodes.size != expr.values.size) return Optional.empty()
        var bindings = emptyMap<String, MValue>()
        for(i in nodes.indices) {
            val nodeBindings = nodes[i].unify(expr.values[i])
            if(nodeBindings.isEmpty) return Optional.empty()
            bindings += nodeBindings.get()
        }
        return Optional.of(bindings)
    }

    private fun multibound(binding: String): TypeCheckException {
        return TypeCheckException(line, this, "Variable $binding is bound several times in this matching")
    }

    override fun pretty(): String {
        return "(${nodes.map {p -> p.pretty()}.joinToString(separator = ", ")})"
    }

    override fun toString(): String {
        return super.toString()
    }

}


class ConstructorPatternNode(val constr: String, val expr: Optional<PatternNode>, line: Int): PatternNode(line) {

    override fun inferPatternType(env: TypeEnv): Pair<MType, Map<String, MType>> {
        val constrType = env.lookupBinding(constr).instantiate(env.typeSystem)

        if(constrType !is MConstr) throw IllegalArgumentException("This should never happen?")
        if(expr.isEmpty) {
            if(constrType.argType.isPresent)
                throw conException(getArgSize(constrType.argType), 0)
            return Pair(constrType.type, emptyMap())
        }
        val patType = expr.get().inferPatternType(env)
        val valueType = patType.first
        if(constrType.argType.isEmpty)
            throw conException(0, getArgSize(valueType))
        val expectedType = constrType.argType.get()
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
            throw patException(valueType, expectedType)
        }


        return Pair(constrType.type, patType.second)
    }

    override fun unify(expr: MValue): Optional<Map<String, MValue>> {
        if(expr !is ConValue) return Optional.empty()
        if(expr.name != constr) return Optional.empty()
        if(expr.value.isPresent) {
            if(this.expr.isEmpty) return Optional.empty()
            return this.expr.get().unify(expr)
        }
        if(this.expr.isPresent) return Optional.empty()
        //Both 0 argument constructors
        return Optional.of(emptyMap())
    }

    private fun conException(expectedSize: Int, actualSize: Int): TypeCheckException {
        return TypeCheckException(line, this, "The constructor $constr expects $expectedSize argument(s),\n" +
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
        var str = constr
        if(expr.isPresent) {
            var toAdd = expr.get().pretty()
            if(expr.get() is ConstructorPatternNode) {
                toAdd = "($toAdd)"
            }
            str += " $toAdd"
        }
        return str;
    }

    override fun toString(): String {
        return "ConPat $constr($expr)"
    }

}

