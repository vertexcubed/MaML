package vertexcubed.maml.ast

import vertexcubed.maml.compile.CompEnv
import vertexcubed.maml.compile.lambda.LambdaNode
import vertexcubed.maml.eval.DynEnv
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

abstract class AstNode(val loc: NodeLoc) {

    /**
     * Walks the AST directly. Slower, but simpler. The first thing that was implemented.
     */
    abstract fun eval(env: DynEnv): MValue

    /**
     * Type checks a node. Catches most syntax errors
     * @param env   the current type environment. Make sure to always create
     *              a new environment before mutating with env.copy()
     */
    abstract fun inferType(env: TypeEnv): MType

    /**
     * Compiles a node down to the enriched lambda calculus. The first stage of compilation.
     */
    abstract fun compile(env: CompEnv): LambdaNode

    open fun pretty(): String {
        return toString()
    }
}

/**
 * Represents a location of a node in code, including the file its apart of, and the line #
 * TODO: include character columns? Maybe?
 */
data class NodeLoc(val file: String, val line: Int) {
    override fun toString(): String {
        return "$file, line $line"
    }
}