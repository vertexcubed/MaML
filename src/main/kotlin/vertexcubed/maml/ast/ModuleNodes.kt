package vertexcubed.maml.ast

import vertexcubed.maml.core.*
import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.ExternalValue
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.type.*
import java.util.*


/**
 * module m = struct ... end
 */
//TODO: THIS NEEDS ONLY NEW STUFF, NOT THE WHOLE PARSEENV
class ModuleStructNode(val name: String, val nodes: List<AstNode>, val sig: Optional<MIdentifier>, val parseEnv: ParseEnv, line: Int): AstNode(line) {

    private fun typeVariant(node: VariantTypeNode, typeEnv: TypeEnv, toWrite: TypeEnv) {
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

        for(con in nodeCons) {
            if(con.name.type.isPresent) {
                val conDummy = con.name.type.get()

                try {

                    val conType = conDummy.lookup(newEnv)
//                    conType = conType.substitute(conType, )

                    typeEnv.addBinding(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                    )
                    toWrite.addBinding(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                    )
                }
                catch(e: UnboundTypeLabelException) {
                    throw TypeCheckException(node.line, node, "The type of variable ${e.type} is unbound in this type declaration.")
                }
            }
            else {
                typeEnv.addBinding(con.name.binding to
                        ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.empty())))

                toWrite.addBinding(con.name.binding to
                        ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.empty()))
                )
            }
        }
        println("Type of $node : ${nodeType.asString(typeEnv)}")
    }

    private fun typeAlias(node: TypeAliasNode, typeEnv: TypeEnv, toWrite: TypeEnv) {
        val newEnv = typeEnv.copy()
        for(arg in node.args) {
            newEnv.addVarLabel(arg.name to newEnv.typeSystem.newTypeVar())
        }

        val nodeType = node.inferType(newEnv)
        //type 'a test = int is LEGAL!


        val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
        typeEnv.addType(node.name to scheme)
        toWrite.addType(node.name to scheme)
        println("Type of $node : ${nodeType.asString(typeEnv)}")
    }


    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not evaluate modules normally!")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not infer types of modules normally!")
    }


    fun exportValues(env: DynEnv): StructEval {
        val newBindings = DynEnv()
        val newEnv = env.copy()
        for(node in nodes) {
            when(node) {
                is TopLetNode -> {
                    val nodeVal = node.eval(newEnv)
                    println(nodeVal)
                    if(node.name.binding != "_") {
                        newEnv.addBinding(node.name.binding to nodeVal)
                        newBindings.addBinding(node.name.binding to nodeVal)
                    }
                }
                is VariantTypeNode, is TypeAliasNode, is ModuleSigNode -> {
                    //These are only used for type checking
                    continue
                }

                is ModuleStructNode -> {
                    val module = node.exportValues(newEnv)
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
                    newEnv.addBinding(node.name to ExternalValue(node.javaFunc))
                    newBindings.addBinding(node.name to ExternalValue(node.javaFunc))
                }
                else -> {
                    println(node.eval(newEnv))
                }
            }
        }
        newBindings.bindings.keys.map { k -> "$name.$k" }
        return StructEval(name, newBindings)
    }



    fun exportTypes(env: TypeEnv): StructType {
        val newEnv = env.copy()
        val moduleTypes = TypeEnv(env.typeSystem)
        for(node in nodes) {
            when (node) {
                is TopLetNode -> {
                    val nodeType = node.inferType(newEnv)
                    val scheme = ForAll.generalize(nodeType, newEnv.typeSystem)
                    if (node.name.binding != "_") {
                        newEnv.addBinding(node.name.binding to scheme)
                        moduleTypes.addBinding(node.name.binding to scheme)
                    }
                    println("Type of $node : ${nodeType.asString(newEnv)}")

                }

                is VariantTypeNode -> {
                    typeVariant(node, newEnv, moduleTypes)
                }

                is ExtensibleVariantTypeNode -> {

                }

                is TypeAliasNode -> {
                    typeAlias(node, newEnv, moduleTypes)
                }

                is ModuleStructNode -> {
                    val module = node.exportTypes(newEnv)
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
                    println("Type of $node : ${t.asString(newEnv)}")

                }

                else -> {
                    val t = node.inferType(newEnv)
                    println("Type of $node : ${t.asString(newEnv)}")
                }
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
                    throw MissingSigTypeException(line, this, sigVal, sigTypes, name, this.sig.get())
                }

                if(modVal is MVariantType && modVal.args.size == sigVal.args.size
                    || modVal is MTypeAlias && modVal.args.size == sigVal.args.size) {
                    newOut.addType(k to moduleTypes.lookupType(k))
                }
                else {
                    throw TypeCheckException(line, this,
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
                    throw MissingSigFieldException(line, this, k, name, this.sig.get())
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
                    throw TypeCheckException(line, this,
                        "Expression $k in module $name has type ${modVal.asString(newEnv)} \n" +
                        "which is incompatible with type ${v.instantiate(newEnv.typeSystem)} in signature ${this.sig.get()}")
                }
                newOut.addBinding(k to v)
            }
            return StructType(name, Optional.of(sig), newOut)
        }


        return StructType(name, Optional.empty(), moduleTypes)
    }



    override fun toString(): String {
        return "Module($name, $nodes)"
    }

}

class ModuleSigNode(val name: String, val nodes: List<SigNode>, val parseEnv: ParseEnv, line: Int): AstNode(line) {
    override fun eval(env: DynEnv): MValue {
        throw AssertionError("Do not evaluate sig nodes!")
    }

    override fun inferType(env: TypeEnv): MType {
        throw AssertionError("Do not type check sig nodes!")
    }

    fun exportTypes(env: TypeEnv): SigType {
        val newEnv = env.copy()
        val sigTypes = TypeEnv(env.typeSystem)
        for(node in nodes) {
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
        return SigType(name, sigTypes)
    }

    override fun toString(): String {
        return "ModuleSig($name, $nodes)"
    }
}


data class StructEval(val name: String, val bindings: DynEnv)

data class StructType(val name: String, val sig: Optional<SigType>, val types: TypeEnv)

data class SigType(val name: String, val types: TypeEnv)