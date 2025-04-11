package vertexcubed.maml.ast

import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.core.UnboundTypeLabelException
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.*
import java.util.*

class Program(val nodes: List<AstNode>, var evalMap: Map<String, MValue>, val typeEnv: TypeEnv) {

    companion object {
        fun typeVariant(node: VariantTypeNode, typeEnv: TypeEnv, toWrite: Optional<TypeEnv>): MType {


            val nodeType = node.inferType(typeEnv)
            val nodeCons = node.cons

            val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
            typeEnv.addType(node.name to scheme)
            if(toWrite.isPresent) {
                toWrite.get().addType(node.name to scheme)
            }
            val newEnv = typeEnv.copy()
            for(arg in (scheme.type as MVariantType).args) {
                newEnv.addVarLabel(arg.first to arg.second)
            }
            for(con in nodeCons) {
                if(con.name.type.isPresent) {
                    val conDummy = con.name.type.get()

                    try {

                        val conType = conDummy.lookupOrMutate(newEnv, false)

                        typeEnv.addBinding(con.name.binding to
                                ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                        )
                        if(toWrite.isPresent) {
                            toWrite.get().addBinding(con.name.binding to
                                    ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                            )
                        }
                    }
                    catch(e: UnboundTypeLabelException) {
                        throw TypeCheckException(node.line, node, "The type of variable ${e.type} is unbound in this type declaration.")
                    }
                }
                else {
                    typeEnv.addBinding(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.empty())))

                    if(toWrite.isPresent) {
                        toWrite.get().addBinding(con.name.binding to
                                ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.empty()))
                        )
                    }
                }
            }
            return nodeType
        }
    }



    fun eval() {
        for(node in nodes) {
            when(node) {
                is TopLetNode -> {
                    val nodeVal = node.eval(evalMap)
                    println(nodeVal)
                    if(node.name.binding != "_") {
                        evalMap += (node.name.binding to nodeVal)
                    }
                }
                is VariantTypeNode -> {
                    //Don't try to "evaluate" datatype defs i guess?
                    continue
                }
                is ModuleStructNode -> {
                    val nodeVal = node.eval(evalMap)
                    println(nodeVal)
                    evalMap += (node.name to nodeVal)
                }
                else -> {
                    println(node.eval(evalMap))
                }
            }
        }
    }

    fun inferTypes() {
        for(node in nodes) {
            val nodeType: MType = when(node) {
                is TopLetNode -> {
                    val n = node.inferType(typeEnv)
                    val scheme = ForAll.generalize(n, typeEnv.typeSystem)
                    if(node.name.binding != "_") {
                        typeEnv.addBinding(node.name.binding to scheme)
                    }

                    n
                }
                is VariantTypeNode -> {
                    typeVariant(node, typeEnv, Optional.empty())
                }
                is ModuleStructNode -> {
                    val n = node.inferType(typeEnv)
                    typeEnv.addBinding(node.name to ForAll.empty(n))
                    n
                }
                else -> {
                    node.inferType(typeEnv)
                }
            }
            println("Type of $node : $nodeType")
        }
    }
}