package vertexcubed.maml.ast

import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.MDummyCons
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv
import java.util.*

sealed class SigNode(line: Int): AstNode(line) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not evaluate module signature nodes!")
    }
}

class ValSigNode(val name: String, val type: DummyType, line: Int): SigNode(line) {

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val labels = type.getAllLabels()
        for(l in labels) {
            newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
        }
        return type.lookup(newEnv)
    }

}

class TypeSigNode(val name: String, val args: List<TypeVarDummy>, line: Int): SigNode(line) {
    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        for(arg in args) {
            newEnv.addVarLabel(arg.name to newEnv.typeSystem.newTypeVar())
        }
        return MDummyCons(UUID.randomUUID(), args.map { a -> a.name to a.lookup(newEnv) })
    }
}

class IncludeSigNode(val name: MIdentifier, line: Int): SigNode(line) {

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not infer include node!")
    }

}

class OpenSigNode(val name: MIdentifier, line: Int): SigNode(line) {
    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not infer open node!")
    }
}

class ExternalSigNode(val name: String, val type: DummyType, line: Int): SigNode(line) {
    override fun inferType(env: TypeEnv): MType {
        TODO("Not yet implemented")
    }
}