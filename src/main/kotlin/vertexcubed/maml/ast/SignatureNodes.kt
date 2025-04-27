package vertexcubed.maml.ast

import vertexcubed.maml.compile.CompEnv
import vertexcubed.maml.compile.lambda.LambdaNode
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnboundException
import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.MDummyCons
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv
import java.util.*

sealed class SigNode(loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not evaluate module signature nodes!")
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }
}

class ValSigNode(val name: String, val type: DummyType, loc: NodeLoc): SigNode(loc) {

    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        val labels = type.getAllLabels()
        for(l in labels) {
            newEnv.addVarLabel(l to newEnv.typeSystem.newTypeVar())
        }
        return try {
            type.lookup(newEnv)
        }
        catch(e: UnboundException) {
            throw TypeCheckException(loc, this, e.log)
        }
    }

    override fun pretty(): String {
        return "val $name: $type"
    }

    override fun toString(): String {
        return "Val($name, $type)"
    }
}

class TypeSigNode(val name: String, val args: List<TypeVarDummy>, loc: NodeLoc): SigNode(loc) {
    override fun inferType(env: TypeEnv): MType {
        val newEnv = env.copy()
        for(arg in args) {
            newEnv.addVarLabel(arg.name to newEnv.typeSystem.newTypeVar())
        }
        return MDummyCons(UUID.randomUUID(), args.map { a -> a.name to a.lookup(newEnv) })
    }

    override fun pretty(): String {
        var argStr = ""
        if(args.size > 1) {
            argStr = args.joinToString(", ", "(", ")") + " "
        }
        else if(args.isNotEmpty()) {
            argStr = args[0].toString() + " "
        }
        return "$argStr$name"
    }

    override fun toString(): String {
        return "Type($name, $args)"
    }
}

class IncludeSigNode(val name: MIdentifier, loc: NodeLoc): SigNode(loc) {

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not infer include node!")
    }

    override fun pretty(): String {
        return "include $name"
    }

    override fun toString(): String {
        return "Include($name)"
    }
}

class OpenSigNode(val name: MIdentifier, loc: NodeLoc): SigNode(loc) {
    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not infer open node!")
    }

    override fun pretty(): String {
        return "open $name"
    }

    override fun toString(): String {
        return "Open($name)"
    }
}

class ExternalSigNode(val name: String, val type: DummyType, loc: NodeLoc): SigNode(loc) {
    override fun inferType(env: TypeEnv): MType {
        TODO("Not yet implemented")
    }
}