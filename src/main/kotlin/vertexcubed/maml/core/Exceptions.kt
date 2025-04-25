package vertexcubed.maml.core

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.ast.NodeLoc
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

/**
 * General Type check failures.
 */
open class TypeCheckException(val loc: NodeLoc, val node: AstNode, val log: String): Exception("$loc: $log)") {
    constructor(loc: NodeLoc, node: AstNode, env: TypeEnv, actualType: MType, expectedType: MType) : this(loc, node,
        "This expression has type ${actualType.asString(env)} but an expression was expected of type ${expectedType.asString(env)}")
}


class MissingSigFieldException(loc: NodeLoc, node: AstNode, log: String): TypeCheckException(loc, node, log) {
    constructor(loc: NodeLoc, node: AstNode, field: String, mod: String, sig: MIdentifier)
            : this(loc, node, "Signature $sig contains field $field\n" +
            "however it was not found in module $mod")
}

class MissingSigTypeException(loc: NodeLoc, node: AstNode, log: String): TypeCheckException(loc, node, log) {
    constructor(loc: NodeLoc, node: AstNode, ty: MType, env: TypeEnv, mod: String, sig: MIdentifier)
            : this(loc, node, "Signature $sig contains type ${ty.asString(env)}\n" +
            "however it was not found in module $mod")
}


class BadRecordException(val label: String): Exception("The record field label $label is defined several times")


/**
 * Thrown when parsing, usually in the case of illegal literals
 */
class ParseException(val loc: NodeLoc, val log: String)
    : Exception("$loc: $log") {
    constructor(lineText: String, loc: NodeLoc, log: String): this(loc, "$lineText\n$log")
}


/**
 * Thrown when an exception is raised within the runtime.
 */
class MaMLException(val exn: MValue): Exception(exn.toString())