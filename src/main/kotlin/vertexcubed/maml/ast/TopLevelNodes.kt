package vertexcubed.maml.ast

import vertexcubed.maml.compile.CompEnv
import vertexcubed.maml.compile.lambda.LambdaNode
import vertexcubed.maml.core.*
import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.*
import java.util.*


class TopLetNode(val name: MBinding, val statement: AstNode, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        return statement.eval(env)
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val statementType =  statement.inferType(newEnv)
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
            nameType.unify(lastType, newEnv.typeSystem)
        }



        return statementType
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        return "let $name = $statement"
    }

    override fun toString(): String {
        return "TopLet($name, $statement)"
    }

}

/**
 * Represents a variant type: eg. type a = B | C | D
 */
class VariantTypeNode(val name: String, val arguments: List<TypeVarDummy>, val cons: List<ConDefNode>, loc: NodeLoc): AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MVariantType {
        val myType = MVariantType(UUID.randomUUID(), arguments.map { a -> Pair(a.name, env.typeSystem.newTypeVar()) })
        val newEnv = env.copy()
        newEnv.addType(name to ForAll.generalize(myType, env.typeSystem))
        for(con in cons) {
            //purposely discard return type?
            con.inferType(newEnv)
        }
        return myType
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        var str = ""
        for(i in cons.indices) {
            str += cons[i].toString() + " "
            if(i != cons.size - 1) {
                str += "| "
            }
        }
        return "type $name = "
    }

    override fun toString(): String {
        return "Type($name, $arguments, $cons)"
    }
}

/**
 * Represents an extensible variant type, such as exn
 */
class ExtensibleVariantTypeNode(val name: String, val arguments: List<TypeVarDummy>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated")
    }

    override fun inferType(env: TypeEnv): MExtensibleVariantType {
        val myType = MExtensibleVariantType(UUID.randomUUID(), arguments.map { a -> Pair(a.name, env.typeSystem.newTypeVar()) })
        val newEnv = env.copy()
        newEnv.addType(name to ForAll.generalize(myType, env.typeSystem))
        return myType
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }
}

class VariantExtendNode(val name: String, val arguments: List<TypeVarDummy>, val cons: List<ConDefNode>, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not type check extend node")
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        var str = ""
        for(i in cons.indices) {
            str += cons[i].toString() + " "
            if(i != cons.size - 1) {
                str += "| "
            }
        }
        return "type $name += "
    }

    override fun toString(): String {
        return "TypeExtend($name, $arguments, $cons)"
    }

}


class TypeAliasNode(val name: String, val args: List<TypeVarDummy>, val type: DummyType, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        try {
            val original = type.lookup(env)
            val id = UUID.randomUUID()
            return MTypeAlias(id, args.map{ a -> Pair(a.name, a.lookup(env))}, original)
        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "TypeAlias($name, $args, $type)"
    }

}

class TopOpenNode(val name: MIdentifier, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not eval open nodes!")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not typecheck open nodes!")
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun pretty(): String {
        return "open $name"
    }

    override fun toString(): String {
        return "Open($name)"
    }
}

class TopIncludeNode(val name: MIdentifier, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not eval open nodes!")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not typecheck open nodes!")
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "Include($name)"
    }

    override fun pretty(): String {
        return "include $name"
    }
}

class ExternalDefNode(val name: String, val type: DummyType, val javaFunc: String, loc: NodeLoc): AstNode(loc) {

    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Probably shouldn't be evaluated")
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val labels = type.getAllLabels()
        for(l in labels) {
            newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
        }
        try {
            return type.lookup(newEnv)
        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "External($name, $type, $javaFunc)"
    }

    override fun pretty(): String {
        return "external $name: $type = $javaFunc"
    }
}