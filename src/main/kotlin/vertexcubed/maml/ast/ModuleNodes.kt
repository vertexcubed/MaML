package vertexcubed.maml.ast

import vertexcubed.maml.core.*
import vertexcubed.maml.eval.*
import vertexcubed.maml.parse.ParseEnv
import vertexcubed.maml.type.*
import java.util.*


/**
 * module m = struct ... end
 */
//TODO: THIS NEEDS ONLY NEW STUFF, NOT THE WHOLE ENV
class ModuleStructNode(val name: String, val nodes: List<AstNode>, val parseEnv: ParseEnv, line: Int): AstNode(line) {

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

                    val conType = conDummy.lookupOrMutate(newEnv, false)
//                    conType = conType.substitute(conType, )

                    typeEnv.addBinding(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                    )
                    toWrite.addBinding(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                    )
                }
                catch(e: UnboundTypeLabelException) {
                    throw TypeCheckException(node.line, node, newEnv, "The type of variable ${e.type} is unbound in this type declaration.")
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


    override fun eval(env: DynEnv): ModuleValue {
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
                is VariantTypeNode, is TypeAliasNode -> {
                    //Don't try to "evaluate" datatype defs i guess?
                    continue
                }

                is ModuleStructNode -> {
                    val nodeVal = node.eval(newEnv)
                    println(nodeVal)
                    newEnv.addBinding(node.name to nodeVal)
                    newBindings.addBinding(node.name to nodeVal)
                }

                is TopOpenNode -> {
                    try {
                        val module = newEnv.lookupBinding(node.name)
                        if(module !is ModuleValue) throw UnboundModuleException(node.name.toString())
                        newEnv.addAllBindings(module.bindings.bindings)
                    }
                    catch(e: UnboundVarException) {
                        throw UnboundModuleException(e.name)
                    }
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
        return ModuleValue(name, newBindings)
    }

    override fun inferType(env: TypeEnv): MType {
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

                is TypeAliasNode -> {
                    typeAlias(node, newEnv, moduleTypes)
                }

                is ModuleStructNode -> {
                    val nodeType = node.inferType(newEnv)
                    newEnv.addBinding(node.name to ForAll.empty(nodeType))
                    newEnv.addType(node.name to ForAll.empty(nodeType))
                    moduleTypes.addBinding(node.name to ForAll.empty(nodeType))
                    moduleTypes.addType(node.name to ForAll.empty(nodeType))
                    println("Type of $node : ${nodeType.asString(newEnv)}")

                }

                is TopOpenNode -> {
                    try {
                        val module = newEnv.lookupBinding(node.name).instantiate(newEnv.typeSystem)
                        if(module !is ModuleType) throw UnboundModuleException(node.name.toString())
                        newEnv.addAllBindings(module.types.bindingTypes)
                        newEnv.addAllTypes(module.types.typeDefs)

                    }
                    catch(e: UnboundVarException) {
                        throw UnboundModuleException(e.name)
                    }

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
        return ModuleType(name, moduleTypes)
    }



    override fun toString(): String {
        return "Module($name, $nodes)"
    }

}

class ModuleSigNode(val name: String, val node: List<AstNode>, val parseEnv: ParseEnv, line: Int): AstNode(line) {
    override fun eval(env: DynEnv): MValue {
        TODO("Not yet implemented")
    }

    override fun inferType(env: TypeEnv): MType {
        TODO("Not yet implemented")
    }

}