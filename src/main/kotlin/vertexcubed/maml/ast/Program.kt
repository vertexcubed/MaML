package vertexcubed.maml.ast

import vertexcubed.maml.core.TypeCheckException
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.TypeVarDummy
import vertexcubed.maml.type.*
import java.util.*

class Program(val nodes: List<AstNode>, var evalMap: Map<String, MValue>, val typeEnv: TypeEnv) {

    companion object {
        fun typeVariant(node: VariantTypeNode, typeEnv: TypeEnv, toWrite: Optional<TypeEnv>): MType {
            val nodeType = node.inferType(typeEnv)
            val nodeCons = node.cons
            //TODO: change to generalize instead
            val scheme = ForAll.generalize(nodeType, typeEnv.typeSystem)
            typeEnv.addType(node.name to scheme)
            if(toWrite.isPresent) {
                toWrite.get().addType(node.name to scheme)
            }
            for(con in nodeCons) {
                if(con.name.type.isPresent) {
                    val conDummy = con.name.type.get()
                    //temporary init to get the compiler to shut up
                    var conType: MType = MUnit
                    if(conDummy is TypeVarDummy) {
                        val conDummyName = conDummy.name

                        var found = false
                        //this is a safe cast
                        for(arg in (scheme.type as MVariantType).args) {
                            if(conDummyName == arg.first) {
                                conType = arg.second
                                found = true
                                break
                            }
                        }
                        if(!found) {
                            throw TypeCheckException(node.line, node, "The type of variable $conDummy is unbound in this type declaration.")
                        }
                    }
                    else {
                        conType = conDummy.lookup(typeEnv)
                    }

                    typeEnv.addBinding(con.name.binding to
                            ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                    )
                    if(toWrite.isPresent) {
                        toWrite.get().addBinding(con.name.binding to
                                ForAll(scheme.typeVars, MConstr(con.name.binding, scheme.type, Optional.of(conType)))
                        )
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