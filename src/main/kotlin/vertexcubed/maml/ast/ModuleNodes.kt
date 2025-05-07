package vertexcubed.maml.ast

import vertexcubed.maml.compile.CompEnv
import vertexcubed.maml.compile.lambda.LambdaNode
import vertexcubed.maml.core.*
import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.parse.FunctionDummy
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.type.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * module m = struct ... end
 */
open class ModuleStructNode(val name: String, val nodes: List<AstNode>, val sig: Optional<MIdentifier>, val parseEnv: ParseEnv, loc: NodeLoc): AstNode(loc) {

    private fun typeVariant(node: VariantTypeNode, typeEnv: TypeEnv, toWrite: TypeEnv, debug: Boolean) {
        val newEnv = typeEnv.copy()

        var nodeType = node.inferType(newEnv)
//        if(sig.isPresent) {
//            val sigType = newEnv.lookupSignature(sig.get())
//            try {
//                val abstype = sigType.types.lookupType(node.name).type as MTypeCon
//                if(abstype.args.size != nodeType.args.size) {
//                    throw TypeCheckException(
//                        loc, this,
//                        "Signature ${this.sig.get()} contains type ${abstype.asString(sigType.types)} " +
//                                "which is incompatible with type ${nodeType.asString(newEnv)}")
//                }
//                nodeType = MTypeCon(abstype.id, nodeType.args)
//            }
//            catch(_: UnboundException) {
//
//            }
//        }
        val nodeCons = node.cons

        val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
        typeEnv.addType(node.name to scheme)
        newEnv.addType(node.name to scheme)
        toWrite.addType(node.name to scheme)

        for((i, arg) in node.arguments.withIndex()) {
            newEnv.addVarLabel(arg.name to (scheme.type as MTypeCon).args[i])
        }

        for((i, con) in nodeCons.withIndex()) {
            if(con.name.type.isPresent) {
                val conDummy = con.name.type.get()

                try {

                    val conType = conDummy.lookup(newEnv)
//                    conType = conType.substitute(conType, )

                    typeEnv.addConstructor(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.of(conType)))
                    )
                    toWrite.addConstructor(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.of(conType)))
                    )
                }
                catch(e: UnboundTypeLabelException) {
                    throw TypeCheckException(node.loc, node, "The type of variable ${e.type} is unbound in this type declaration.")
                }
            }
            else {
                typeEnv.addConstructor(con.name.binding to
                        ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.empty())))

                toWrite.addConstructor(con.name.binding to
                        ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.empty()))
                )
            }
        }
        if(debug) {
            println("Type of $node : ${nodeType.asString(typeEnv)}")
        }
    }

    private fun typeExtend(node: VariantExtendNode, typeEnv: TypeEnv, toWrite: TypeEnv, debug: Boolean) {
        val labelEnv = typeEnv.copy()

        val scheme = typeEnv.lookupType(node.name)
        val nodeType = scheme.instantiate(typeEnv.typeSystem)
        if(nodeType !is MExtensibleVariant) {
            throw TypeCheckException(node.loc, node, "Type ${nodeType.asString(typeEnv)} is not extensible.")
        }
        if(node.arguments.size != nodeType.args.size) {
            throw TypeCheckException(node.loc, node, "This extension does not match type ${nodeType.asString(typeEnv)}\n" +
                    "Expected arity ${nodeType.args.size}, but found arity ${node.arguments.size}")
        }
        for(i in node.arguments.indices) {
            // We don't care if the labels are different
            labelEnv.addVarLabel(node.arguments[i].name to nodeType.args[i])
        }

        for((i, con) in node.cons.withIndex()) {
            if(con.name.type.isPresent) {
                val conDummy = con.name.type.get()

                try {

                    val conType = conDummy.lookup(labelEnv)

                    typeEnv.addConstructor(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.of(conType)))
                    )
                    toWrite.addConstructor(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.of(conType)))
                    )
                }
                catch(e: UnboundTypeLabelException) {
                    throw TypeCheckException(node.loc, node, "The type of variable ${e.type} is unbound in this type declaration.")
                }
            }
            else {
                typeEnv.addConstructor(con.name.binding to
                        ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.empty())))

                toWrite.addConstructor(con.name.binding to
                        ForAll(scheme.typeVars, MConstr(con.name.binding, i, scheme.type, Optional.empty()))
                )
            }
            if(debug) {
                println("Type of $node : ${nodeType.asString(typeEnv)}")
            }
        }
    }



    private fun typeAlias(node: TypeAliasNode, typeEnv: TypeEnv, toWrite: TypeEnv, debug: Boolean) {
        val newEnv = typeEnv.copy()
        for(arg in node.args) {
            newEnv.addVarLabel(arg.name to newEnv.typeSystem.newTypeVar())
        }

        val nodeType = node.inferType(newEnv)
        //type 'a test = int is LEGAL!


        val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
        typeEnv.addType(node.name to scheme)
        toWrite.addType(node.name to scheme)
        if(debug) {
            println("Type of $node : ${nodeType.asString(typeEnv)}")
        }
    }


    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not evaluate modules normally!")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not infer types of modules normally!")
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }


    fun exportTypes(env: TypeEnv, debug: Boolean): StructType {
        val newEnv = env.copy()
        val moduleTypes = TypeEnv(env.typeSystem)
        for(node in nodes) {
            try {
                when (node) {
                    is TopLetNode -> {
                        val nodeType = node.inferType(newEnv)
                        val scheme = ForAll.generalize(nodeType, newEnv.typeSystem)
                        if (node.name.binding != "_") {
                            newEnv.addBinding(node.name.binding to scheme)
                            moduleTypes.addBinding(node.name.binding to scheme)
                        }
                        if(debug) {
                            println("Type of $node : ${nodeType.asString(newEnv)}")
                        }

                    }

                    is VariantTypeNode -> {
                        typeVariant(node, newEnv, moduleTypes, debug)
                    }

                    is VariantExtendNode -> {
                        typeExtend(node, newEnv, moduleTypes, debug)
                    }

                    is ExtensibleVariantTypeNode -> {

                        val nodeType = node.inferType(newEnv)
                        val scheme = ForAll.generalize(nodeType, newEnv.typeSystem)
                        newEnv.addType(node.name to scheme)
                        moduleTypes.addType(node.name to scheme)
                    }

                    is TypeAliasNode -> {
                        typeAlias(node, newEnv, moduleTypes, debug)
                    }

                    is ModuleStructNode -> {
                        val module = node.exportTypes(newEnv, debug)
                        newEnv.addModule(module)
                        moduleTypes.addModule(module)

                    }

                    is ModuleSigNode -> {
                        val sig = node.exportTypes(newEnv)
                        newEnv.addSignature(sig)
                        moduleTypes.addSignature(sig)
                    }

                    is TopOpenNode -> {
                        val module = newEnv.lookupModule(node.name)
                        newEnv.addAllFrom(module.types)
                    }

                    is TopIncludeNode -> {
                        val module = newEnv.lookupModule(node.name)
                        newEnv.addAllFrom(module.types)
                        moduleTypes.addAllFrom(module.types)
                    }

                    is ExternalDefNode -> {
                        val t = node.inferType(newEnv)
                        val scheme = ForAll.generalize(t, newEnv.typeSystem)
                        newEnv.addBinding(node.name to scheme)
                        moduleTypes.addBinding(node.name to scheme)
                        if(debug) {
                            println("Type of $node : ${t.asString(newEnv)}")
                        }

                    }

                    else -> {
                        val t = node.inferType(newEnv)
                        if(debug) {
                            println("Type of $node : ${t.asString(newEnv)}")
                        }
                    }
                }
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }
        }
        if(sig.isPresent) {
            val newOut = TypeEnv(env.typeSystem)
            val sig = env.lookupSignature(sig.get())
            val sigTypes = sig.types.copy()

            for((k, v) in sigTypes.typeDefs) {
                val sigVal = v.instantiate(newEnv.typeSystem) as MTypeCon
                val modVal: MType
                try {
                    modVal = moduleTypes.lookupType(k).instantiate(newEnv.typeSystem) as MTypeCon
                }
                catch (e: UnboundTyConException) {
                    throw MissingSigTypeException(loc, this, sigVal, sigTypes, name, this.sig.get())
                }

                if(modVal.args.size == sigVal.args.size) {
                    newOut.addType(k to v)
                }
                else {

                    val errEnv = newEnv.copy()
                    errEnv.addAllFrom(sigTypes)
                    throw TypeCheckException(
                        loc, this,
                        "Signature ${this.sig.get()} contains type ${sigVal.asString(errEnv)} " +
                        "which is incompatible with type ${modVal.asString(errEnv)}")
                }
            }


            for((k, v) in sigTypes.bindingTypes) {
                if(name == "Mod") {
                    0
                }


                val modVal: MType
                try {
                    modVal = moduleTypes.lookupBinding(k).instantiate(newEnv.typeSystem)
                }
                catch(e: UnboundVarException) {
                    throw MissingSigFieldException(loc, this, k, name, this.sig.get())
                }
                val baseVars = arrayListOf<MType>()
                for(i in v.typeVars.indices) {
                    baseVars.add(newEnv.typeSystem.newBaseType())
                }


                var sigVal = v.instantiate(baseVars)
                for((tk, tv) in sigTypes.typeDefs) {
                    sigVal = sigVal.substitute(tv.instantiate(baseVars), moduleTypes.lookupType(tk).instantiate(baseVars))
                }


                //this shoooould work?
                //TODO: BUGTEST BUGTEST BUGTEST

                try {
                    modVal.unify(sigVal, newEnv.typeSystem, true)
                }
                catch(e: UnifyException) {
                    val errEnv = newEnv.copy()
                    errEnv.addAllFrom(sigTypes)
                    throw TypeCheckException(
                        loc, this,
                        "Expression $k in module $name has type ${modVal.asString(errEnv)} \n" +
                        "which is incompatible with type ${v.instantiate(errEnv.typeSystem).asString(errEnv)} in signature ${this.sig.get()}")
                }

                //TODO: this is inefficient.
//                var out = v.instantiate(newEnv.typeSystem)
//                for((tk, tv) in sigTypes.typeDefs) {
//                    out = out.substitute(tv.instantiate(newEnv.typeSystem), moduleTypes.lookupType(tk).instantiate(newEnv.typeSystem))
//                }

                newOut.addBinding(k to v)
            }
            return StructType(name, Optional.of(sig), newOut)
        }


        return StructType(name, Optional.empty(), moduleTypes)
    }

//    private fun replaceAbstract(type: MType, absId: Int, concreteId: Int): MType {
//        when(type) {
//            is MFunction -> {
//                return MFunction(replaceAbstract(type.arg, absId, concreteId), replaceAbstract(type.ret, absId, concreteId))
//            }
//            is MGeneralTypeVar -> return type
//            is MBaseTypeVar -> return type
//            is MTuple -> {
//                return MTuple(type.types.map { replaceAbstract(it, absId, concreteId) })
//            }
//            is MTypeVar -> return type
//
//            MBool, MChar, MFloat, MInt, MString, MUnit, MEmptyRow -> return type
//
//            is MConstr -> {
//                if(type.argType.isPresent) {
//                    return MConstr(type.name, type.conId, replaceAbstract(type.type, absId, concreteId), Optional.of(replaceAbstract(type.argType.get(), absId, concreteId)))
//                }
//                return MConstr(type.name, type.conId, replaceAbstract(type.type, absId, concreteId), Optional.empty())
//            }
//            is MRecord -> {
//                return MRecord(type.fields.mapValues { (_, v) -> replaceAbstract(v, absId, concreteId) }, replaceAbstract(type.rest, absId, concreteId))
//            }
//            is MTypeCon -> {
//                if(type.id == absId) {
//                    return MTypeCon(concreteId, type.args.map { replaceAbstract(it, absId, concreteId) })
//                }
//                return MTypeCon(absId, type.args.map { replaceAbstract(it, absId, concreteId) })
//            }
//        }
//    }
//

    fun exportValues(env: DynEnv, debug: Boolean): StructEval {
        val newBindings = DynEnv()
        val newEnv = env.copy()
        for(node in nodes) {
            when(node) {
                is TopLetNode -> {
                    val nodeVal = node.eval(newEnv)
                    if(debug) {
                        println(nodeVal)
                    }
                    if(node.name.binding != "_") {
                        newEnv.addBinding(node.name.binding to nodeVal)
                        newBindings.addBinding(node.name.binding to nodeVal)
                    }
                }
                is VariantTypeNode, is VariantExtendNode, is ExtensibleVariantTypeNode, is TypeAliasNode, is ModuleSigNode -> {
                    //These are only used for type checking
                    continue
                }

                is ModuleStructNode -> {
                    val module = node.exportValues(newEnv, debug)
                    newEnv.addModule(module)
                    newBindings.addModule(module)
                }

                is TopOpenNode -> {
                    val module = newEnv.lookupModule(node.name)
                    newEnv.addAllBindings(module.bindings.bindings)
                }

                is TopIncludeNode -> {
                    val module = newEnv.lookupModule(node.name)
                    newEnv.addAllBindings(module.bindings.bindings)
                    newBindings.addAllBindings(module.bindings.bindings)
                }

                is ExternalDefNode -> {

                    fun createFunction(type: DummyType): AstNode {
                        return when(type) {
                            is FunctionDummy -> {
//                                FunctionNode(MBinding("p$argCount", Optional.empty()), createFunction(type.second,argCount + 1), node.loc)
                                val argList = ArrayList<MBinding>()
                                var trav = type
                                var argCount = 0
                                while(trav is FunctionDummy) {
                                    argList.add(MBinding("p$argCount", Optional.of(trav.first)))
                                    trav = trav.second
                                    argCount++
                                }
                                //TODO: return type is yeeted
                                FunctionNode(argList, ExternalCallNode(node.javaFunc, argCount, node.loc), node.loc)
                            }
                            else -> {
                                ExternalCallNode(node.javaFunc, 0, node.loc)
                            }
                        }
                    }

                    val n = createFunction(node.type)

                    val value = n.eval(newEnv)

                    newEnv.addBinding(node.name to value)
                    newBindings.addBinding(node.name to value)
                }
                else -> {
                    if(debug) {
                        println(node.eval(newEnv))
                    }
                }
            }
        }
        newBindings.bindings.keys.map { k -> "$name.$k" }
        return StructEval(name, newBindings)
    }



    override fun toString(): String {
        return "Module($name, $nodes)"
    }

}

class ModuleSigNode(val name: String, val nodes: List<SigNode>, val parseEnv: ParseEnv, loc: NodeLoc): AstNode(loc) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not evaluate sig nodes!")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not type check sig nodes!")
    }

    override fun compile(env: CompEnv): LambdaNode {
        TODO("Not yet implemented")
    }

    fun exportTypes(env: TypeEnv): SigType {
        val newEnv = env.copy()
        val sigTypes = TypeEnv(env.typeSystem)
        for(node in nodes) {
            try {
                when(node) {
                    is ExternalSigNode -> TODO()
                    is IncludeSigNode -> {
                        val sig = newEnv.lookupSignature(node.name)
                        newEnv.addAllFrom(sig.types)
                        //Export everything too
                        sigTypes.addAllFrom(sig.types)
                    }
                    is OpenSigNode -> {
                        val module = newEnv.lookupModule(node.name)
                        newEnv.addAllFrom(module.types)
                    }
                    is TypeSigNode -> {
                        val type = node.inferType(newEnv)
                        val scheme = ForAll.generalize(type, newEnv.typeSystem)
                        newEnv.addType(node.name to scheme)
                        sigTypes.addType(node.name to scheme)
                    }
                    is ValSigNode -> {
                        val type = ForAll.generalize(node.inferType(newEnv), newEnv.typeSystem)
                        sigTypes.addBinding(node.name to type)
                    }
                }
            }
            catch(e: UnboundException) {
                throw TypeCheckException(loc, this, e.log)
            }
        }
        return SigType(name, sigTypes)
    }

    override fun toString(): String {
        return "ModuleSig($name, $nodes)"
    }
}


data class StructEval(val name: String, val bindings: DynEnv)

data class StructType(val name: String, val sig: Optional<SigType>, val types: TypeEnv)

data class SigType(val name: String, val types: TypeEnv)


// TODO: fix open not working in toplevel
class TopLevel(nodes: List<AstNode>, parseEnv: ParseEnv, val isExpr: Boolean): ModuleStructNode("//toplevel//", nodes, Optional.empty(), parseEnv, NodeLoc("//toplevel//", 0)) {

    fun runAll(typeEnv: TypeEnv, dynEnv: DynEnv): Pair<TypeEnv, DynEnv> {
        if(isExpr) {
            val t = nodes[0].inferType(typeEnv)
            val v = nodes[0].eval(dynEnv)
            println("- : ${t.asString(typeEnv)} = $v")
            val outT = TypeEnv(typeEnv.typeSystem)
            val outV = DynEnv()
            return outT to outV
        }







        val types = exportTypes(typeEnv, false).types
        val valueTypes = mutableMapOf<String, MType>()
        val allTypeEnv = types.copy()
        allTypeEnv.addAllFrom(typeEnv)

        for((_, v) in types.typeDefs) {
            val t = v.instantiate(types.typeSystem)
            println("type ${t.asString(allTypeEnv)}")
        }
        for((k, v) in types.bindingTypes) {
            val t = v.instantiate(types.typeSystem)
            valueTypes[k] = t
        }

        val values = exportValues(dynEnv, false).bindings



        for((k, v) in values.bindings) {
            val t = valueTypes[k]!!
            println("val $k : ${t.asString(allTypeEnv)} = $v")
        }

        return types to values
    }
}