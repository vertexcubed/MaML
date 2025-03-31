package vertexcubed.maml.eval

import vertexcubed.maml.parse.ast.AstNode
import vertexcubed.maml.type.MType


class ParseException(line: Int, message: String) : Exception("Error on line $line: $message")
class BinaryOpException(message: String) : Exception(message)
class UnaryOpException(message: String) : Exception(message)
class UnboundVarException(name: String) : Exception("Unbound Variable: $name")
class ApplicationException(message: String) : Exception(message)
class IfException() : Exception("Cannot use non-boolean as condition")
class TypeException(val line: Int, val node: AstNode, val log: String): Exception("Line $line: $log)") {
        constructor(line: Int, node: AstNode, actualType: MType, expectedType: MType) : this(line, node,
            "This expression has type $actualType but an expression was expected of type $expectedType")
}