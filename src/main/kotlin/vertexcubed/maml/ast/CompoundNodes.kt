package vertexcubed.maml.ast

import vertexcubed.maml.core.*
import vertexcubed.maml.eval.*
import vertexcubed.maml.type.*
import java.util.*


class AppNode(val func: AstNode, val arg: AstNode, loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        val argEval = arg.eval(env)

        val funcVal = func.eval(env)
        when(funcVal) {
            is FunctionValue -> {
                val newEnv = funcVal.env.copy()
                newEnv.addBinding(funcVal.arg to argEval)
                return funcVal.expr.eval(newEnv)
            }
            is RecursiveFunctionValue -> {
                val newEnv = funcVal.func.env.copy()
                newEnv.addBinding(funcVal.func.arg to argEval)
                newEnv.addBinding(funcVal.name to funcVal)
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
            funcType.unify(other, env.typeSystem)
        }
        catch(e: UnifyException) {
            throw TypeCheckException(loc, this, env, e.t2, e.t1)
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

class IfNode(val condition: AstNode, val thenBranch: AstNode, val elseBranch: AstNode, loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
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
            condType.unify(MBool, env.typeSystem)
        }
        catch(e: UnifyException) {
            throw TypeCheckException(condition.loc, this, env, condType, MBool)
        }
        val thenType = thenBranch.inferType(env)
        val elseType = elseBranch.inferType(env)
        try {
            thenType.unify(elseType, env.typeSystem)
        }
        catch(e: UnifyException) {
            throw TypeCheckException(elseBranch.loc, this, env, elseType, thenType)
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

class RecordExpandNode(val original: AstNode, val newPairs: Map<String, AstNode>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        val oldRecord = original.eval(env)
        if(oldRecord !is RecordValue) throw RecordException("Cannot expand non-record value!")
        val map = mutableMapOf<String, MValue>()
        map += oldRecord.values
        map += newPairs.mapValues { (_, v) -> v.eval(env) }
        return RecordValue(map)

    }

    override fun inferType(env: TypeEnv): MType {
        var originalType = original.inferType(env)

        val empty = MRecord(emptyMap(), env.typeSystem.newTypeVar())


        var ogFind = originalType.find()
        if(ogFind is MTypeVar) {
            try {
                ogFind.unify(empty, env.typeSystem)
            }
            catch(e: UnifyException) {
                throw TypeCheckException(loc, this, env, ogFind, empty)
            }
            ogFind = ogFind.find()
        }
        if(ogFind !is MRecord) {
            throw TypeCheckException(
                loc, this, "An expression was expected of type record\n" +
                    "but type ${ogFind.asString(env)} was found.")
        }
        val (ogFields, ogRest) = ogFind.flatten()

        originalType = MRecord(ogFields + newPairs.mapValues { (_, v) -> v.inferType(env) }, ogRest)

        return originalType
    }

    override fun toString(): String {
        return "Expand($original, $newPairs)"
    }
}

class RecordLookupNode(val record: AstNode, val field: String, loc: NodeLoc): AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        val recordVal = record.eval(env)
        if(recordVal !is RecordValue) {
            throw RecordException("Cannot access record field of non-record value!")
        }
        return recordVal.values.getOrElse(field, { throw RecordException("Record ${record.pretty()} does not contain field $field!")})
    }

    override fun inferType(env: TypeEnv): MType {
        val recordType = record.inferType(env)
        val retType = env.typeSystem.newTypeVar()
        val polyRecord = MRecord(mapOf(field to retType), env.typeSystem.newTypeVar())
        recordType.unify(polyRecord, env.typeSystem)
        return retType

    }

    override fun toString(): String {
        return "Lookup($record, $field)"
    }
}




//TODO: explicit type for let not actually used!
class LetNode(val name: MBinding, val statement: AstNode, val expression: AstNode, loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        val statementVal = statement.eval(env)
        if(name.binding == "_") return expression.eval(env)
        val newEnv = env.copy()
        newEnv.addBinding(name.binding to statementVal)
        return expression.eval(newEnv)
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
//        val statementType = statement.type(env)
//        val newEnv = env + (name.binding to statementType)
//        return expression.type(newEnv)
        val statementType = statement.inferType(newEnv)

        if(name.type.isPresent) {
            val labels = name.type.get().getAllLabels()
            for(l in labels) {
                newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
            }
            val nameType: MType
            try {
                nameType = name.type.get().lookup(newEnv)
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }

            var lastType = statementType
            while(true) {
                if(lastType is MFunction) {
                    lastType = lastType.ret
                }
                else {
                    break
                }
            }
            nameType.unify(lastType, env.typeSystem)
        }

        if(name.binding == "_") return expression.inferType(env)
        val scheme = ForAll.generalize(statementType, env.typeSystem)

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

class RecursiveFunctionNode(val name: MBinding, val node: FunctionNode, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        val nodeVal = node.eval(env)
        return RecursiveFunctionValue(name.binding, nodeVal)
    }

    override fun inferType(env: TypeEnv): MType {
        var newEnv = env.copy()
        val myRetType = newEnv.typeSystem.newTypeVar()
        if(name.type.isPresent) {
            val labels = name.type.get().getAllLabels()
            for(l in labels) {
                newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
            }
            val expectedType: MType
            try {
                expectedType = name.type.get().lookup(newEnv)
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }

            //This should never throw an exception
            myRetType.unify(expectedType, env.typeSystem)

        }
        if(name.binding == "_") throw TypeCheckException(loc, this, "Only variables are allowed as left-hand side of let rec")

        val argType = newEnv.typeSystem.newTypeVar()
        if(node.arg.type.isPresent) {
            val labels = node.arg.type.get().getAllLabels()
            for(l in labels) {
                newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
            }
            val expectedType: MType
            try {
                expectedType = node.arg.type.get().lookup(newEnv)
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }

            //This should never throw an exception
            argType.unify(expectedType, env.typeSystem)

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
        myRetType.unify(bodyType, env.typeSystem)

        return MFunction(argType, bodyType)


    }

    override fun pretty(): String {
        return "rec fun $name -> $node"
    }

    override fun toString(): String {
        return "RecFun($name, $node)"
    }

}

class FunctionNode(val arg: MBinding, val body: AstNode, loc: NodeLoc) : AstNode(loc) {

    override fun eval(env: DynEnv): FunctionValue {
        return FunctionValue(arg.binding, body, env)
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val argType = newEnv.typeSystem.newTypeVar()

        if(arg.type.isPresent) {
            val labels = arg.type.get().getAllLabels()
            for(l in labels) {
                newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
            }
            val expectedType: MType
            try {
                expectedType = arg.type.get().lookup(newEnv)
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }

            //This should never throw an exception
            argType.unify(expectedType, env.typeSystem)
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

class ConNode(val name: MIdentifier, val value: Optional<AstNode>, loc: NodeLoc): AstNode(loc) {
    constructor(name: String, value: Optional<AstNode>, loc: NodeLoc): this(MIdentifier(name), value, loc)

    override fun eval(env: DynEnv): MValue {
        if(value.isPresent) {
            return ConValue(name, Optional.of(value.get().eval(env)))
        }
        return ConValue(name, Optional.empty())
    }

    override fun inferType(env: TypeEnv): MType {
        val myType: MType
        try {
            myType = env.lookupConstructor(name).instantiate(env.typeSystem)

        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }

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
            expectedType.unify(valueType, env.typeSystem)
        }
        catch(e: UnifyException) {
            //Both are tuples, aka multi arg constructors
            if(expectedType is MTuple && valueType is MTuple) {
                //Different sizes
                if(expectedType.types.size != valueType.types.size)
                    throw conException(env, getArgSize(expectedType), getArgSize(valueType))

                //Technically i should do per tuple type checking but idc lmao
                throw TypeCheckException(loc, this, env, valueType, expectedType)
            }
            //Only one of them are tuples, aka different size args
            if(expectedType is MTuple || valueType is MTuple) {
                throw conException(env, getArgSize(expectedType), getArgSize(valueType))
            }
            throw TypeCheckException(loc, this, env, valueType, expectedType)
        }


        return myType.type
    }

    private fun conException(env: TypeEnv, expectedSize: Int, actualSize: Int): TypeCheckException {
        return TypeCheckException(
            loc, this, "The constructor $name expects $expectedSize argument(s),\n" +
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

class ExternalCallNode(val javaFunc: String, val argCount: Int, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        val args = arrayListOf<MValue>()
        for(i in 0 until argCount) {
            args.add(env.lookupBinding("p${i}"))
        }
        return env.callJavaFunc(javaFunc, args.toTypedArray())
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not type check external calls!")
    }

    override fun toString(): String {
        return "External($javaFunc)"
    }

}




class MatchCaseNode(val expr: AstNode, val nodes: List<Pair<PatternNode, AstNode>>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        val exprVal = expr.eval(env)
        for((pat, newExpr) in nodes) {
            val patBindings = pat.unify(exprVal)
            if(patBindings.isPresent) {
                val newEnv = env.copy()
                newEnv.addAllBindings(patBindings.get())
                return newExpr.eval(newEnv)
            }
        }
        //TODO: include file name.
        val exn = ConValue(MIdentifier("Match_failure"), Optional.of(TupleValue(listOf(StringValue(loc.file), IntegerValue(
            loc.line.toLong())))))
        throw MaMLException(exn)
    }

    override fun inferType(env: TypeEnv): MType {
        val exprType = expr.inferType(env)
        val retType = env.typeSystem.newTypeVar()
        for((p, e) in nodes) {
            val (pType, pBindings) = p.inferPatternType(env)
            try {
                exprType.unify(pType, env.typeSystem)
            }
            catch(e: UnifyException) {
                throw patException(env, pType, exprType)
            }
            val newEnv = env.copy()
            newEnv.addAllBindings(pBindings.mapValues { t -> ForAll.empty(t.value) })
            val eType = e.inferType(newEnv)
            try {
                retType.unify(eType, env.typeSystem)
            }
            catch(e: UnifyException) {
                throw TypeCheckException(loc, this, env, eType, retType)
            }
        }
        return retType
    }

    fun patException(env: TypeEnv, actualType: MType, expectedType: MType): TypeCheckException {
        return TypeCheckException(
            loc, this, "This pattern matches values of type ${actualType.asString(env)}\n" +
                "but a pattern was expected which matches values of type ${expectedType.asString(env)}")
    }

    override fun pretty(): String {
        return "match ${expr.pretty()} with ${nodes.map { p -> "${p.first} -> ${p.second}" }.joinToString(separator = " | ")}"
    }

    override fun toString(): String {
        return "MatchCase($expr, ${nodes.joinToString(separator = " | ")})"
    }
}

class TryWithNode(val expr: AstNode, val exceptions: List<Pair<PatternNode, AstNode>>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        try {
            return expr.eval(env)
        }
        catch(e: MaMLException) {
            val exn = e.exn
            for((pat, newExpr) in exceptions) {
                val patBindings = pat.unify(exn)
                if(patBindings.isPresent) {
                    val newEnv = env.copy()
                    newEnv.addAllBindings(patBindings.get())
                    return newExpr.eval(newEnv)
                }
            }
            throw e
        }
    }

    override fun inferType(env: TypeEnv): MType {
        val exprType = expr.inferType(env)

        val exnType = env.lookupType("exn").instantiate(env.typeSystem)
        for((p, e) in exceptions) {
            val (pType, pBindings) = p.inferPatternType(env)
            try {
                exnType.unify(pType, env.typeSystem)
            }
            catch(e: UnifyException) {
                throw patException(env, pType, exnType)
            }
            val newEnv = env.copy()
            newEnv.addAllBindings(pBindings.mapValues { t -> ForAll.empty(t.value) })
            val eType = e.inferType(newEnv)
            try {
                exprType.unify(eType, env.typeSystem)
            }
            catch(e: UnifyException) {
                throw TypeCheckException(loc, this, env, eType, exprType)
            }
        }
        return exprType
    }

    fun patException(env: TypeEnv, actualType: MType, expectedType: MType): TypeCheckException {
        return TypeCheckException(
            loc, this, "This pattern matches values of type ${actualType.asString(env)}\n" +
                "but a pattern was expected which matches values of type ${expectedType.asString(env)}")
    }

}





class LocalOpenNode(val name: MIdentifier, val body: AstNode, loc: NodeLoc): AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        try {
            val module = env.lookupModule(name)
            val newEnv = env.copy()
            newEnv.addAllBindings(module.bindings.bindings)
            return body.eval(newEnv)
        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }
    }

    override fun inferType(env: TypeEnv): MType {
        try {
            val module = env.lookupModule(name)
            val newEnv = env.copy()
            newEnv.addAllBindings(module.types.bindingTypes)
            newEnv.addAllTypes(module.types.typeDefs)
            return body.inferType(newEnv)
        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }
    }

}

class AssertNode(val check: AstNode, loc: NodeLoc): AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        val checkVal = check.eval(env)
        if(!boolOrThrow(checkVal)) {
            //TODO: throw assertion failure
            return UnitValue
        }
        return UnitValue
    }

    override fun inferType(env: TypeEnv): MType {

        if(check is FalseNode) {
            //TODO: actually decompose
            return env.typeSystem.newTypeVar()
        }

        val checkType = check.inferType(env)
        checkType.unify(MBool, env.typeSystem)
        return MUnit
    }

    override fun pretty(): String {
        return "assert ${check.pretty()}"
    }

    override fun toString(): String {
        return "Assert($check)"
    }


}