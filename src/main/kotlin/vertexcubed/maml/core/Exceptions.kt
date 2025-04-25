package vertexcubed.maml.core

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.type.MType
import vertexcubed.maml.type.TypeEnv

/**
 * General Type check failures.
 */
open class TypeCheckException(val line: Int, val node: AstNode, val log: String): Exception("Line $line: $log)") {
    constructor(line: Int, node: AstNode, env: TypeEnv, actualType: MType, expectedType: MType) : this(line, node,
        "This expression has type ${actualType.asString(env)} but an expression was expected of type ${expectedType.asString(env)}")
}


class MissingSigFieldException(line: Int, node: AstNode, log: String): TypeCheckException(line, node, log) {
    constructor(line: Int, node: AstNode, field: String, mod: String, sig: MIdentifier)
            : this(line, node, "Signature $sig contains field $field\n" +
            "however it was not found in module $mod")
}

class MissingSigTypeException(line: Int, node: AstNode, log: String): TypeCheckException(line, node, log) {
    constructor(line: Int, node: AstNode, ty: MType, env: TypeEnv, mod: String, sig: MIdentifier)
            : this(line, node, "Signature $sig contains type ${ty.asString(env)}\n" +
            "however it was not found in module $mod")
}


class BadRecordException(val label: String): Exception("The record field label $label is defined several times")


/**
 * Thrown when parsing, usually in the case of illegal literals
 */
class ParseException(val line: Int, val log: String)
    : Exception("Error on line $line: $log") {
    constructor(lineText: String, line: Int, log: String): this(line, "$lineText\n$log")
}


/**
 * Thrown when an exception is raised within the runtime.
 */
class MaMLException(val exn: MValue): Exception(exn.toString())