package vertexcubed.maml.ast

import vertexcubed.maml.core.ApplicationException
import vertexcubed.maml.core.IfException
import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnboundVarException
import vertexcubed.maml.eval.BooleanValue
import vertexcubed.maml.eval.FunctionValue
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.RecursiveFunctionValue
import vertexcubed.maml.type.*


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
        val elseType = elseBranch.inferType(env, types)
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

//TODO: explicit type for let not actually used!
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
        val myRetType = types.newTypeVar()
        if(name.type.isPresent) {
            myRetType.unify(name.type.get())
        }
        if(name.binding == "_") throw TypeCheckException(line, this, "Only variables are allowed as left-hand side of let rec")

        val argType = types.newTypeVar()
        if(node.arg.type.isPresent) {
            argType.unify(node.arg.type.get())
        }
        val myType = MFunction(argType, myRetType)
        var newEnv = env + (name.binding to ForAll.empty(myType))

        if(node.arg.binding == "_") {
            val bodyType = node.body.inferType(newEnv, types)
            return MFunction(argType, bodyType)
        }

        newEnv = newEnv + (node.arg.binding to ForAll.empty(argType))
        val bodyType = node.body.inferType(newEnv, types)

        myRetType.unify(bodyType)

        return MFunction(argType, bodyType)


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




class MatchCaseNode(val expr: AstNode, val nodes: List<MatchNode>, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        TODO("Not yet implemented")
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        TODO("Not yet implemented")
    }
}

class MatchNode(val pattern: AstNode, val expr: AstNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        TODO("Not yet implemented")
    }

    override fun inferType(env: Map<String, ForAll>, types: TypeVarEnv): MType {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        TODO("Not yet implemented")
    }
}