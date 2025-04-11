package vertexcubed.maml.core

import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.eval.MValue
import vertexcubed.maml.parse.DummyType
import vertexcubed.maml.type.MType


class ParseException(line: Int, message: String) : Exception("Error on line $line: $message")
class UnboundVarException(name: String) : Exception("Unbound Variable: $name")
class UnboundTypeLabelException(val type: DummyType): Exception("The type of variable $type is unbound.")
class UnboundTyConException(name: String) : Exception("Unbound Type Constructor: $name")
class ApplicationException(message: String) : Exception(message)
class MatchException(value: MValue): Exception("Failed to match against $value")
class IfException() : Exception("Cannot use non-boolean as condition")
class TypeCheckException(val line: Int, val node: AstNode, val log: String): Exception("Line $line: $log)") {
        constructor(line: Int, node: AstNode, actualType: MType, expectedType: MType) : this(line, node,
            "This expression has type $actualType but an expression was expected of type $expectedType")
}

class UnifyException(val t1: MType, val t2: MType): Exception("Cannot unify type $t1 with type $t2")
class BindException(t1: MType, boundType: MType): Exception("Type $t1 already bound to type $boundType")