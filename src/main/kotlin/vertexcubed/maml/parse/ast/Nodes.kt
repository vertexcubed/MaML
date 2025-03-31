package vertexcubed.maml.parse.ast

import vertexcubed.maml.eval.*

class UnitNode : AstNode() {
    override fun eval(env: Map<String, MValue>): MValue {
        return UnitValue
    }

    override fun toString(): String {
        return "Unit"
    }
}

class TrueNode : AstNode() {
    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(true)
    }

    override fun toString(): String {
        return "true"
    }
}

class FalseNode : AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(false)
    }

    override fun toString(): String {
        return "false"
    }
}

class StringNode(val text: String) : AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        return BooleanValue(false)
    }

    override fun toString(): String {
        return text
    }
}

class CharNode(val text: Char): AstNode() {
    override fun eval(env: Map<String, MValue>): MValue {
        return CharValue(text)
    }

    override fun toString(): String {
        return text.toString()
    }

}

/**
 * All integers in MaML are 64-bit.
 */
class IntegerNode(val number: Long) : AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        return IntegerValue(number)
    }

    override fun toString(): String {
        return "$number"
    }
}

class FloatNode(val number: Float): AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        return FloatValue(number)
    }

    override fun toString(): String {
        return "$number"
    }
}

class BinaryOpNode(val operation: Bop, val left: AstNode, val right: AstNode) : AstNode() {
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

    override fun toString(): String {
        return "Bop($operation, $left, $right)"
    }

}

class UnaryOpNode(val operation: Uop, val other: AstNode): AstNode() {
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

}



class AppNode(val func: AstNode, val arg: AstNode) : AstNode() {

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

    override fun toString(): String {
        return "App($func, $arg)"
    }
}

class IfNode(val condition: AstNode, val thenBranch: AstNode, val elseBranch: AstNode) : AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        val conditionValue = condition.eval(env)
        if(conditionValue !is BooleanValue) throw IfException()
        if(conditionValue.value) {
            return thenBranch.eval(env)
        }
        return elseBranch.eval(env)
    }

    override fun toString(): String {
        return "If($condition, $thenBranch, $elseBranch)"
    }

}

class LetNode(val name: String, val statement: AstNode, val expression: AstNode) : AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        val statementVal = statement.eval(env)
        val newEnv = env + (name to statementVal)
        return expression.eval(newEnv)
    }

    override fun toString(): String {
        return "Let($name, $statement, $expression)"
    }

}

class RecursiveFunctionNode(val name: String, val node: FunctionNode): AstNode() {
    override fun eval(env: Map<String, MValue>): MValue {
        val nodeVal = node.eval(env)
        return RecursiveFunctionValue(name, nodeVal)
    }

    override fun toString(): String {
        return "RecFun($name, $node)"
    }

}

class FunctionNode(val arg: String, val body: AstNode) : AstNode() {

    override fun eval(env: Map<String, MValue>): FunctionValue {
        return FunctionValue(arg, body, env)
    }

    override fun toString(): String {
        return "Fun($arg, $body)"
    }
}

class VariableNode(val name: String): AstNode() {

    override fun eval(env: Map<String, MValue>): MValue {
        return env.getOrElse(name, { throw UnboundVarException(name)})
    }

    override fun toString(): String {
        return "Var($name)"
    }
}