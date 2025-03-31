package vertexcubed.maml.parse.ast

import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*

class UnitNode(line: Int) : AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return UnitValue
    }

    override fun type(env: Map<String, MType>): MType {
        return MUnit
    }

    override fun toString(): String {
        return "Unit"
    }
}

class TrueNode(line: Int) : AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(true)
    }

    override fun type(env: Map<String, MType>): MType {
        return MBool
    }

    override fun toString(): String {
        return "true"
    }
}

class FalseNode(line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(false)
    }

    override fun type(env: Map<String, MType>): MType {
        return MBool
    }

    override fun toString(): String {
        return "false"
    }
}

class StringNode(val text: String, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(false)
    }

    override fun type(env: Map<String, MType>): MType {
        return MString
    }

    override fun toString(): String {
        return text
    }
}

class CharNode(val text: Char, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return CharValue(text)
    }

    override fun type(env: Map<String, MType>): MType {
        return MChar
    }

    override fun toString(): String {
        return text.toString()
    }

}

/**
 * All integers in MaML are 64-bit.
 */
class IntegerNode(val number: Long, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return IntegerValue(number)
    }

    override fun type(env: Map<String, MType>): MType {
        return MInt
    }

    override fun toString(): String {
        return "$number"
    }
}

class FloatNode(val number: Float, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return FloatValue(number)
    }

    override fun type(env: Map<String, MType>): MType {
        return MFloat
    }

    override fun toString(): String {
        return "$number"
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

    override fun type(env: Map<String, MType>): MType {
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
        val leftType = left.type(env)
        val rightType = right.type(env)
        if(leftType != expectedType) {
            throw TypeException(line, left, leftType, expectedType)
        }
        if(rightType != expectedType) {
            throw TypeException(line, right, rightType, expectedType)
        }
        return myType
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

    override fun type(env: Map<String, MType>): MType {
        val types = when(operation) {
            Uop.NEGATE -> listOf(MInt, MFloat)
            Uop.NOT -> listOf(MBool)
        }
        val otherType = other.type(env)
        if(otherType !in types) {
            throw TypeException(line, other, otherType, types[0])
        }
        return otherType
    }

}



class AppNode(val func: AstNode, val arg: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
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

    override fun type(env: Map<String, MType>): MType {
        val funcType = func.type(env)
        if(funcType !is MFunction) throw TypeException(line, func, "This expression has type $funcType\nThis is not a function; it cannot be applied")

        val argType = arg.type(env)

        if(argType != funcType.arg) throw TypeException(line, arg, argType, funcType.arg)
        return funcType.ret
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

    override fun type(env: Map<String, MType>): MType {
        val condType = condition.type(env)
        if(condType !is MBool) throw TypeException(line, condition, condType, MBool)
        val thenType = thenBranch.type(env)
        val elseType = elseBranch.type(env)
        if(thenType != elseType) throw TypeException(line, condition, elseType, thenType)
        return thenType
    }

    override fun toString(): String {
        return "If($condition, $thenBranch, $elseBranch)"
    }

}

class LetNode(val name: MBinding, val statement: AstNode, val expression: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        val statementVal = statement.eval(env)
        val newEnv = env + (name.binding to statementVal)
        return expression.eval(newEnv)
    }

    override fun type(env: Map<String, MType>): MType {
        val statementType = statement.type(env)
        val newEnv = env + (name.binding to statementType)
        return expression.type(newEnv)
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

    override fun type(env: Map<String, MType>): MType {
        val newEnv = env + (name.binding to name.type)
        return node.type(newEnv)
    }

    override fun toString(): String {
        return "RecFun($name, $node)"
    }

}

class FunctionNode(val arg: MBinding, val body: AstNode, line: Int) : AstNode(line) {

    override fun eval(env: Map<String, MValue>): FunctionValue {
        return FunctionValue(arg.binding, body, env)
    }

    override fun type(env: Map<String, MType>): MType {
        val newEnv = env + (arg.binding to arg.type)
        return MFunction(arg.type, body.type(newEnv))
    }

    override fun toString(): String {
        return "Fun($arg, $body)"
    }
}

class VariableNode(val name: String, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        return env.getOrElse(name, { throw UnboundVarException(name) })
    }

    override fun type(env: Map<String, MType>): MType {
        return env.getOrElse(name, { throw UnboundVarException(name) })
    }

    override fun toString(): String {
        return "Var($name)"
    }
}