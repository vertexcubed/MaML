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


/**
 * module m = struct ... end
 */
open class ModuleStructNode(val name: String, val nodes: List<AstNode>, val sig: Optional<MIdentifier>, val parseEnv: ParseEnv, loc: NodeLoc): AstNode(loc) {

    private fun typeVariant(node: VariantTypeNode, typeEnv: TypeEnv, toWrite: TypeEnv, debug: Boolean) {
        val newEnv = typeEnv.copy()

        val nodeType = node.inferType(newEnv)
        val nodeCons = node.cons

        val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
        typeEnv.addType(node.name to scheme)
        newEnv.addType(node.name to scheme)
        toWrite.addType(node.name to scheme)

        for(arg in (scheme.type as MVariantType).args) {
            newEnv.addVarLabel(arg)
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
        if(nodeType !is MExtensibleVariantType) {
            throw TypeCheckException(node.loc, node, "Type ${nodeType.asString(typeEnv)} is not extensible.")
        }
        if(node.arguments.size != nodeType.args.size) {
            throw TypeCheckException(node.loc, node, "This extension does not match type ${nodeType.asString(typeEnv)}\n" +
                    "Expected arity ${nodeType.args.size}, but found arity ${node.arguments.size}")
        }
        for(i in node.arguments.indices) {
            // We don't care if the labels are different
            labelEnv.addVarLabel(node.arguments[i].name to nodeType.args[i].second)
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
            val sigTypes = sig.types

            for((k, v) in sigTypes.typeDefs) {
                val sigVal = v.instantiate(newEnv.typeSystem) as MDummyCons
                val modVal: MType
                try {
                    modVal = moduleTypes.lookupType(k).instantiate(newEnv.typeSystem)
                }
                catch (e: UnboundTyConException) {
                    throw MissingSigTypeException(loc, this, sigVal, sigTypes, name, this.sig.get())
                }

                if(modVal is MVariantType && modVal.args.size == sigVal.args.size
                    || modVal is MTypeAlias && modVal.args.size == sigVal.args.size) {
                    newOut.addType(k to moduleTypes.lookupType(k))
                }
                else {
                    throw TypeCheckException(
                        loc, this,
                        "Signature ${this.sig.get()} contains type ${sigVal.asString(sigTypes)} " +
                        "which is incompatible with type ${modVal.asString(newEnv)}")
                }
            }


            for((k, v) in sigTypes.bindingTypes) {
                val modVal: MType
                try {
                    modVal = moduleTypes.lookupBinding(k).instantiate(newEnv.typeSystem)
                }
                catch(e: UnboundVarException) {
                    throw MissingSigFieldException(loc, this, k, name, this.sig.get())
                }
                var sigVal = v.instantiateBase(newEnv.typeSystem)
                for((tk, tv) in sigTypes.typeDefs) {
                    sigVal = sigVal.substitute(tv.instantiateBase(newEnv.typeSystem), moduleTypes.lookupType(tk).instantiateBase(newEnv.typeSystem))
                }


                //this shoooould work?
                //TODO: BUGTEST BUGTEST BUGTEST

                try {
                    modVal.unify(sigVal, newEnv.typeSystem, true)
                }
                catch(e: UnifyException) {
                    throw TypeCheckException(
                        loc, this,
                        "Expression $k in module $name has type ${modVal.asString(newEnv)} \n" +
                        "which is incompatible with type ${v.instantiate(newEnv.typeSystem)} in signature ${this.sig.get()}")
                }

                //TODO: this is inefficient.
                var out = v.instantiate(newEnv.typeSystem)
                for((tk, tv) in sigTypes.typeDefs) {
                    out = out.substitute(tv.instantiate(newEnv.typeSystem), moduleTypes.lookupType(tk).instantiate(newEnv.typeSystem))
                }

                newOut.addBinding(k to ForAll.generalize(out, newEnv.typeSystem))
            }
            return StructType(name, Optional.of(sig), newOut)
        }


        return StructType(name, Optional.empty(), moduleTypes)
    }



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

                    fun createFunction(type: DummyType, argCount: Int): AstNode {
                        return when(type) {
                            is FunctionDummy -> {
                                FunctionNode(MBinding("p$argCount", Optional.empty()), createFunction(type.second,argCount + 1), node.loc)

                            }
                            else -> {
                                ExternalCallNode(node.javaFunc, argCount, node.loc)
                            }
                        }
                    }

                    val n = createFunction(node.type, 0)

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