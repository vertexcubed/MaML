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

class BinaryOpNode(val operation: Bop, val left: AstNode, val right: AstNode, line: Int) : AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        val datatype = operation.dataType
        val leftVal = left.eval(env)
        val rightVal = right.eval(env)
        when(datatype) {
            Bop.DataType.INT -> {
                if(leftVal !is IntegerValue || rightVal !is IntegerValue) throw BinaryOpException("Cannot perform operation on non-integer values")
                return when(operation) {
                    Bop.ADD -> IntegerValue(leftVal.value + rightVal.value)
                    Bop.SUB -> IntegerValue(leftVal.value - rightVal.value)
                    Bop.MUL -> IntegerValue(leftVal.value * rightVal.value)
                    Bop.DIV -> IntegerValue(leftVal.value / rightVal.value)
                    Bop.MOD -> IntegerValue(leftVal.value % rightVal.value)
                    Bop.LT -> BooleanValue(leftVal.value < rightVal.value)
                    Bop.LTE -> BooleanValue(leftVal.value <= rightVal.value)
                    Bop.GT -> BooleanValue(leftVal.value > rightVal.value)
                    Bop.GTE -> BooleanValue(leftVal.value >= rightVal.value)
                    Bop.EQ -> BooleanValue(leftVal.value == rightVal.value)
                    Bop.NEQ -> BooleanValue(leftVal.value != rightVal.value)
                    else -> throw AssertionError()
                }
            }
            Bop.DataType.BOOL -> {
                if(leftVal !is BooleanValue || rightVal !is BooleanValue) throw BinaryOpException("Cannot perform operation on non-bool values")
                return when(operation) {
                    Bop.AND -> BooleanValue(leftVal.value && rightVal.value)
                    Bop.OR -> BooleanValue(leftVal.value || rightVal.value)
                    else -> throw AssertionError()
                }
            }
        }

    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        val myType: MType
        val expectedType: MType
        when(operation) {

            Bop.ADD, Bop.SUB, Bop.MUL, Bop.DIV, Bop.MOD -> {
                myType = MInt
                expectedType = MInt
            }
            Bop.LT, Bop.LTE, Bop.GT, Bop.GTE, Bop.EQ, Bop.NEQ -> {
                myType = MBool
                expectedType = MInt
            }
            Bop.AND, Bop.OR -> {
                myType = MBool
                expectedType = MBool
            }
        }
        val leftType = left.inferType(env, types)
        val rightType = right.inferType(env, types)
        leftType.unify(expectedType)
        rightType.unify(expectedType)
        return myType
    }

    override fun pretty(): String {
        return "$left ${operation.display} $right"
    }

    override fun toString(): String {
        return "Bop($operation, $left, $right)"
    }

}

class UnaryOpNode(val operation: Uop, val other: AstNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        val otherVal = other.eval(env)
        return when(operation) {
            Uop.NEGATE -> {
                when(otherVal) {
                    is FloatValue -> FloatValue(otherVal.value * -1)
                    is IntegerValue -> IntegerValue(otherVal.value * -1)
                    else -> throw UnaryOpException("Cannot negate non-number values")
                }
            }
            Uop.NOT -> {
                if(otherVal !is BooleanValue) throw UnaryOpException("Cannot perform not on non-boolean value")
                else BooleanValue(!otherVal.value)
            }
        }
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        val validTypes = when(operation) {
            Uop.NEGATE -> MInt
            Uop.NOT -> MBool
        }
        val otherType = other.inferType(env, types)

        otherType.unify(validTypes)
        return otherType
    }

    override fun pretty(): String {
        return "${operation.display} $other"
    }

    override fun toString(): String {
        return "Uop($operation, $other)"
    }

}



class AppNode(val func: AstNode, val arg: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {


        //Special logic for builtins: The actual builtin takes one argument, but they can get combined into a tuple ig
        if(func is BuiltinNode) {
            val argEval = arg.eval(env)
            val newEnv = env + (BuiltinNode.argBinding to argEval)
            return func.eval(newEnv)
        }



        val funcVal = func.eval(env)
        when(funcVal) {
            is FunctionValue -> {
                val argEval = arg.eval(env)
                val newEnv = funcVal.env + (funcVal.arg to argEval)
                return funcVal.expr.eval(newEnv)
            }
            is RecursiveFunctionValue -> {
                val argEval = arg.eval(env)
                val newEnv = funcVal.func.env + (funcVal.func.arg to argEval) + (funcVal.name to funcVal)
                return funcVal.func.expr.eval(newEnv)
            }
            else -> throw ApplicationException("Cannot apply non-function value")
        }
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {

        val funcType = func.inferType(env, types)

        val argType = arg.inferType(env, types)

        val myType = types.newTypeVar()

        funcType.unify(MFunction(argType, myType))

        return myType

//        val funcType = func.type(env, types)
//        if(funcType !is MFunction) throw TypeException(line, func, "This expression has type $funcType\nThis is not a function; it cannot be applied")
//
//        val argType = arg.type(env, types)
//
//        if(argType != funcType.arg) throw TypeException(line, arg, argType, funcType.arg)
//        return funcType.ret
    }

    override fun pretty(): String {
        return "$func $arg"
    }

    override fun toString(): String {
        return "App($func, $arg)"
    }
}

class IfNode(val condition: AstNode, val thenBranch: AstNode, val elseBranch: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        val conditionValue = condition.eval(env)
        if(conditionValue !is BooleanValue) throw IfException()
        if(conditionValue.value) {
            return thenBranch.eval(env)
        }
        return elseBranch.eval(env)
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {

        val condType = condition.inferType(env, types)
        condType.unify(MBool)
        val thenType = thenBranch.inferType(env, types)
        val elseType = thenBranch.inferType(env, types)
        thenType.unify(elseType)
        return thenType
    }

    override fun pretty(): String {
        return "If $condition then $thenBranch else $elseBranch"
    }

    override fun toString(): String {
        return "If($condition, $thenBranch, $elseBranch)"
    }

}

class LetNode(val name: MBinding, val statement: AstNode, val expression: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        val statementVal = statement.eval(env)
        if(name.binding == "_") return expression.eval(env)
        val newEnv = env + (name.binding to statementVal)
        return expression.eval(newEnv)
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
//        val statementType = statement.type(env)
//        val newEnv = env + (name.binding to statementType)
//        return expression.type(newEnv)
        val statementType = statement.inferType(env, types)
        val scheme = ForAll.generalize(statementType, types)
        if(name.binding == "_") return expression.inferType(env, types)

        val newEnv = env + (name.binding to scheme)
        return expression.inferType(newEnv, types)

    }

    override fun pretty(): String {
        return "let $name = $statement in $expression"
    }

    override fun toString(): String {
        return "Let($name, $statement, $expression)"
    }

}

class RecursiveFunctionNode(val name: MBinding, val node: FunctionNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        val nodeVal = node.eval(env)
        return RecursiveFunctionValue(name.binding, nodeVal)
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        val myType = types.newTypeVar()
        if(name.type.isPresent) {
            myType.unify(name.type.get())
        }
        if(name.binding == "_") throw TypeCheckException(line, this, "Only variables are allowed as left-hand side of let rec")

        val newEnv = env + (name.binding to ForAll.empty(myType))
        return node.inferType(newEnv, types)
    }

    override fun pretty(): String {
        return "rec fun $name -> $node"
    }

    override fun toString(): String {
        return "RecFun($name, $node)"
    }

}

class FunctionNode(val arg: MBinding, val body: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): FunctionValue {
        return FunctionValue(arg.binding, body, env)
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        val argType = types.newTypeVar()
        if(arg.type.isPresent) {
            argType.unify(arg.type.get())
        }
        if(arg.binding == "_") {
            val bodyType = body.inferType(env, types)
            return MFunction(argType, bodyType)
        }

        val newEnv = env + (arg.binding to ForAll.empty(argType))
        val bodyType = body.inferType(newEnv, types)


        return MFunction(argType, bodyType)
    }



    override fun pretty(): String {
        return "fun ${arg.binding} -> $body"
    }

    override fun toString(): String {
        return "Fun($arg, $body)"
    }
}

class BuiltinNode(val name: MBinding, line: Int, val function: (MValue) -> MValue): AstNode(line) {

    companion object {
        const val argBinding = "b0"
    }

    override fun eval(env: Map<String, MValue>): MValue {
        val argVal = env.getOrElse(argBinding, { throw UnboundVarException(argBinding) })
        return function(argVal)
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        val argType = types.newTypeVar()
        val myType = types.newTypeVar()
        return MFunction(argType, myType)
    }

    override fun pretty(): String {
        return name.binding
    }

    override fun toString(): String {
        return "Builtin(${name.binding})"
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