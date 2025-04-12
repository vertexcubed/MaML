package vertexcubed.maml.ast

import vertexcubed.maml.core.*
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*
import java.util.*


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

    override fun inferType(env: TypeEnv): MType {

        val funcType = func.inferType(env)

        val argType = arg.inferType(env)

        val myType = env.typeSystem.newTypeVar()

        val other = MFunction(argType, myType)
        try {
            funcType.unify(other)
        }
        catch(e: UnifyException) {
            throw TypeCheckException(line, this, env, e.t2, e.t1)
        }


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
        return "${func.pretty()} ${arg.pretty()}"
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

    override fun inferType(env: TypeEnv): MType {

        val condType = condition.inferType(env)
        try {
            condType.unify(MBool)
        }
        catch(e: UnifyException) {
            throw TypeCheckException(condition.line, this, env, condType, MBool)
        }
        val thenType = thenBranch.inferType(env)
        val elseType = elseBranch.inferType(env)
        try {
            thenType.unify(elseType)
        }
        catch(e: UnifyException) {
            throw TypeCheckException(elseBranch.line, this, env, elseType, thenType)
        }
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

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
//        val statementType = statement.type(env)
//        val newEnv = env + (name.binding to statementType)
//        return expression.type(newEnv)
        val statementType = statement.inferType(newEnv)

        if(name.type.isPresent) {
            val nameType = name.type.get().lookupOrMutate(newEnv, true)
            var lastType = statementType
            while(true) {
                if(lastType is MFunction) {
                    lastType = lastType.ret
                }
                else {
                    break
                }
            }
            nameType.unify(lastType)
        }

        val scheme = ForAll.generalize(statementType, env.typeSystem)
        if(name.binding == "_") return expression.inferType(env)

        newEnv.addBinding(name.binding, scheme)

//        val newEnv = env + (name.binding to scheme)

        return expression.inferType(newEnv)

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

    override fun inferType(env: TypeEnv): MType {
        var newEnv = env.copy()
        val myRetType = newEnv.typeSystem.newTypeVar()
        if(name.type.isPresent) {
            val expectedType = name.type.get().lookupOrMutate(newEnv, true)
            //This should never throw an exception
            myRetType.unify(expectedType)

        }
        if(name.binding == "_") throw TypeCheckException(line, this, env, "Only variables are allowed as left-hand side of let rec")

        val argType = newEnv.typeSystem.newTypeVar()
        if(node.arg.type.isPresent) {
            val expectedType = node.arg.type.get().lookupOrMutate(newEnv, true)
            //This should never throw an exception
            argType.unify(expectedType)

        }
        val myType = MFunction(argType, myRetType)
        newEnv.addBinding(name.binding to ForAll.empty(myType))

        if(node.arg.binding == "_") {
            val bodyType = node.body.inferType(newEnv)
            return MFunction(argType, bodyType)
        }
        newEnv = newEnv.copy()
        newEnv.addBinding(node.arg.binding to ForAll.empty(argType))
        val bodyType = node.body.inferType(newEnv)

        //This should never throw an exception?
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

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val argType = newEnv.typeSystem.newTypeVar()

        if(arg.type.isPresent) {
            val expectedType = arg.type.get().lookupOrMutate(newEnv, true)
            //This should never throw an exception
            argType.unify(expectedType)
        }
        if(arg.binding == "_") {
            val bodyType = body.inferType(newEnv)
            return MFunction(argType, bodyType)
        }

        newEnv.addBinding(arg.binding to ForAll.empty(argType))
        val bodyType = body.inferType(newEnv)


        return MFunction(argType, bodyType)
    }



    override fun pretty(): String {
        return "fun ${arg.binding} -> $body"
    }

    override fun toString(): String {
        return "Fun($arg, $body)"
    }
}

class ConNode(val name: MIdentifier, val value: Optional<AstNode>, line: Int): AstNode(line) {
    constructor(name: String, value: Optional<AstNode>, line: Int): this(MIdentifier(name), value, line)

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
                throw conException(env, getArgSize(myType.argType), 0)
            return myType.type
        }
        val valueType = value.get().inferType(env)
        if(myType.argType.isEmpty)
            throw conException(env, 0, getArgSize(valueType))
        val expectedType = myType.argType.get()
        try {
            expectedType.unify(valueType)
        }
        catch(e: UnifyException) {
            //Both are tuples, aka multi arg constructors
            if(expectedType is MTuple && valueType is MTuple) {
                //Different sizes
                if(expectedType.types.size != valueType.types.size)
                    throw conException(env, getArgSize(expectedType), getArgSize(valueType))

                //Technically i should do per tuple type checking but idc lmao
                throw TypeCheckException(line, this, env, valueType, expectedType)
            }
            //Only one of them are tuples, aka different size args
            if(expectedType is MTuple || valueType is MTuple) {
                throw conException(env, getArgSize(expectedType), getArgSize(valueType))
            }
            throw TypeCheckException(line, this, env, valueType, expectedType)
        }


        return myType.type
    }

    private fun conException(env: TypeEnv, expectedSize: Int, actualSize: Int): TypeCheckException {
        return TypeCheckException(line, this, env,"The constructor $name expects $expectedSize argument(s),\n" +
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
        var str = name.toString()
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

class BuiltinNode(val name: MBinding, line: Int, val function: (MValue) -> MValue): AstNode(line) {

    companion object {
        const val argBinding = "b0"
    }

    override fun eval(env: Map<String, MValue>): MValue {
        val argVal = env.getOrElse(argBinding, { throw UnboundVarException(argBinding) })
        return function(argVal)
    }

    override fun inferType(env: TypeEnv): MType {
        val argType = env.typeSystem.newTypeVar()
        val myType = env.typeSystem.newTypeVar()
        return MFunction(argType, myType)
    }

    override fun pretty(): String {
        return name.binding
    }

    override fun toString(): String {
        return "Builtin(${name.binding})"
    }
}




class MatchCaseNode(val expr: AstNode, val nodes: List<Pair<PatternNode, AstNode>>, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        val exprVal = expr.eval(env)
        for((pat, newExpr) in nodes) {
            val patBindings = pat.unify(exprVal)
            if(patBindings.isPresent) {
                val newEnv = env + patBindings.get()
                return newExpr.eval(newEnv)
            }
        }
        throw MatchException(exprVal)
    }

    override fun inferType(env: TypeEnv): MType {
        val exprType = expr.inferType(env)
        val retType = env.typeSystem.newTypeVar()
        for((p, e) in nodes) {
            val (pType, pBindings) = p.inferPatternType(env)
            try {
                exprType.unify(pType)
            }
            catch(e: UnifyException) {
                throw patException(env, pType, exprType)
            }
            val newEnv = env.copy()
            newEnv.addAllBindings(pBindings.mapValues { t -> ForAll.empty(t.value) })
            val eType = e.inferType(newEnv)
            try {
                retType.unify(eType)
            }
            catch(e: UnifyException) {
                throw TypeCheckException(line, this, env, eType, retType)
            }
        }
        return retType
    }

    fun patException(env: TypeEnv, actualType: MType, expectedType: MType): TypeCheckException {
        return TypeCheckException(line, this, env, "This pattern matches values of type ${actualType.asString(env)}\n" +
                "but a pattern was expected which matches values of type ${expectedType.asString(env)}")
    }

    override fun pretty(): String {
        return "match ${expr.pretty()} with ${nodes.map { p -> "${p.first} -> ${p.second}" }.joinToString(separator = " | ")}"
    }

    override fun toString(): String {
        return "MatchCase($expr, ${nodes.joinToString(separator = " | ")})"
    }
}