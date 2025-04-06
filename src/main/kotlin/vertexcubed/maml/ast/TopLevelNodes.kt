package vertexcubed.maml.ast

import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.*


class TopLetNode(val name: MBinding, val statement: AstNode, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        return statement.eval(env)
    }

    override fun inferType(env: TypeEnv): MType {
        return statement.inferType(env)
    }

    override fun pretty(): String {
        return "let $name = $statement"
    }

    override fun toString(): String {
        return "TopLet($name, $statement)"
    }

}

/**
 * Represents an ADT
 */
class DataTypeNode(val name: String, val arguments: List<TypeVarDummy>, val cons: List<ConDefNode>, line: Int): AstNode(line) {

    override fun eval(env: Map<String, MValue>): MValue {
        throw AssertionError("Probably shouldn't be evaluated?")
    }

    override fun inferType(env: TypeEnv): MDataType {
        val myType = MDataType(name, arguments.map { a -> Pair(a.name, a.lookup(env)) })
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



class TypeAliasNode(val name: String, val type: MType, line: Int): AstNode(line) {
    override fun eval(env: Map<String, MValue>): MValue {
        TODO("NYI")
    }

    override fun inferType(env: TypeEnv): MType {
        TODO("NYI")
    }

    override fun pretty(): String {
        return "type $name = $type"
    }

}