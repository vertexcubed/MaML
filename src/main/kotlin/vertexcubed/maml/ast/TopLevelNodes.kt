package vertexcubed.maml.ast

import vertexcubed.maml.core.MBinding
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.ModuleValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.*
import java.util.*


class TopLetNode(val name: MBinding, val statement: AstNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return statement.eval(env)
    }

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val statementType =  statement.inferType(newEnv)
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



        return statementType
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
class VariantTypeNode(val name: String, val arguments: List<TypeVarDummy>, val cons: List<ConDefNode>, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
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

class TypeAliasNode(val name: String, val args: List<TypeVarDummy>, val type: DummyType, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MType {
        val original = type.lookupOrMutate(env, false)
        val id = UUID.randomUUID()
        return MTypeAlias(id, args.map{ a -> Pair(a.name, a.lookupOrMutate(env, false))}, original)
    }

    override fun toString(): String {
        return "TypeAlias($name, $args, $type)"
    }

}

/**
 * Represents an extensible variant type, such as exn
 */
class ExtensibleVariantTypeNode(val name: String, val arguments: List<TypeVarDummy>, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated")
    }
    override fun inferType(env: TypeEnv): MType {
        val myType = MVariantType(UUID.randomUUID(), arguments.map { a -> Pair(a.name, a.lookupOrMutate(env, false)) })
        return myType
    }
}

