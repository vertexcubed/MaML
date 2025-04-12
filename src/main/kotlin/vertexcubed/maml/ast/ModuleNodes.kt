package vertexcubed.maml.ast

import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnboundTypeLabelException
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.eval.ModuleValue
import vertexcubed.maml.type.*
import java.util.*


/**
 * module m = struct ... end
 */
class ModuleStructNode(val name: String, val nodes: List<AstNode>, line: Int): AstNode(line) {

    private fun typeVariant(node: VariantTypeNode, typeEnv: TypeEnv, toWrite: TypeEnv) {
        val newEnv = typeEnv.copy()

        val nodeType = node.inferType(newEnv)
        val nodeCons = node.cons

        val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
        typeEnv.addType(node.name to scheme)
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
        println("Type of $node : ${nodeType.asString(newEnv)}")
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


    override fun eval(env: Map<String, MValue>): ModuleValue {
        //TODO: MAKE SURE TO ALWAYS UPDATE THIS!!!! IT SHOULD MATCH PROGRAM LOOP
        val newBindings = mutableMapOf<String, MValue>()
        val newEnv = env.toMutableMap()
        for(node in nodes) {
            when(node) {
                is TopLetNode -> {
                    val nodeVal = node.eval(newEnv)
                    println(nodeVal)
                    if(node.name.binding != "_") {
                        newEnv += (node.name.binding to nodeVal)
                        newBindings += (node.name.binding to nodeVal)
                    }
                }
                is VariantTypeNode, is TypeAliasNode -> {
                    //Don't try to "evaluate" datatype defs i guess?
                    continue
                }

                is ModuleStructNode -> {
                    val nodeVal = node.eval(newEnv)
                    println(nodeVal)
                    newEnv += (node.name to nodeVal)
                    newBindings += (node.name to nodeVal)
                }
                else -> {
                    println(node.eval(newEnv))
                }
            }
        }
        newBindings.keys.map { k -> "$name.$k" }
        return ModuleValue(name, newBindings, newEnv)
    }

    override fun inferType(env: TypeEnv): MType {
        //TODO: MAKE SURE TO ALWAYS UPDATE THIS!!!! IT SHOULD MATCH PROGRAM LOOP
        val newEnv = env.copy()
        val moduleTypes = TypeEnv(env.typeSystem)
        for(node in nodes) {
            when (node) {
                is TopLetNode -> {
                    val n = node.inferType(newEnv)
                    val scheme = ForAll.generalize(n, newEnv.typeSystem)
                    if (node.name.binding != "_") {
                        newEnv.addBinding(node.name.binding to scheme)
                        moduleTypes.addBinding(node.name.binding to scheme)
                    }
                    println("Type of $node : ${n.asString(newEnv)}")

                }

                is VariantTypeNode -> {
                    typeVariant(node, newEnv, moduleTypes)
                }

                is TypeAliasNode -> {
                    typeAlias(node, newEnv, moduleTypes)
                }

                is ModuleStructNode -> {
                    val n = node.inferType(newEnv)
                    newEnv.addBinding(node.name to ForAll.empty(n))
                    moduleTypes.addBinding(node.name to ForAll.empty(n))
                    println("Type of $node : ${n.asString(newEnv)}")

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