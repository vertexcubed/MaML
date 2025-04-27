package vertexcubed.maml.compile.lambda

import vertexcubed.maml.ast.NodeLoc
import vertexcubed.maml.compile.bytecode.ZValue

sealed class LambdaNode(val loc: NodeLoc) {

}

// Variables, with De Brujin index
class LVar(val index: Int, loc: NodeLoc): LambdaNode(loc)

// Constants
class LConst(val value: ZValue, loc: NodeLoc): LambdaNode(loc)

// Multi application
class LApply(val func: LambdaNode, val args: List<LambdaNode>, loc: NodeLoc): LambdaNode(loc)

// Curried function
class LFunction(val arity: Int, val body: LambdaNode, loc: NodeLoc): LambdaNode(loc)

// Local definitions.
class LLet(val bindings: List<LambdaNode>, val body: LambdaNode, loc: NodeLoc): LambdaNode(loc)

// Recursive local definitions
class LLetRec(val bindings: List<LambdaNode>, val body: LambdaNode, loc: NodeLoc): LambdaNode(loc)

// Primitive operations. TODO: add primitive operations lol
class LPrim(val args: List<LambdaNode>, loc: NodeLoc): LambdaNode(loc)